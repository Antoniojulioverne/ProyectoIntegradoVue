package com.ejemplos.servicios.base;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Servicio base que proporciona operaciones CRUD comunes
 * Implementa patrones de diseño y buenas prácticas para servicios
 * 
 * @param <T> Tipo de entidad
 * @param <ID> Tipo del identificador
 */
@Slf4j
@Transactional
public abstract class BaseService<T, ID> {

    /**
     * Método abstracto que debe ser implementado por los servicios hijos
     * para proporcionar el repositorio específico
     */
    protected abstract JpaRepository<T, ID> getRepository();

    // === OPERACIONES CRUD BÁSICAS ===

    @Transactional(readOnly = true)
    public List<T> findAll() {
        log.debug("Obteniendo todas las entidades de tipo: {}", getEntityClass().getSimpleName());
        return getRepository().findAll();
    }

    @Transactional(readOnly = true)
    public Optional<T> findById(ID id) {
        log.debug("Buscando entidad por ID: {} (tipo: {})", id, getEntityClass().getSimpleName());
        return getRepository().findById(id);
    }

    @Transactional(readOnly = true)
    public boolean existsById(ID id) {
        log.debug("Verificando existencia de entidad por ID: {} (tipo: {})", id, getEntityClass().getSimpleName());
        return getRepository().existsById(id);
    }

    @Transactional(readOnly = true)
    public long count() {
        log.debug("Contando entidades de tipo: {}", getEntityClass().getSimpleName());
        return getRepository().count();
    }

    public T save(T entity) {
        log.debug("Guardando entidad de tipo: {}", getEntityClass().getSimpleName());
        validateEntity(entity);
        T savedEntity = getRepository().save(entity);
        log.info("Entidad guardada exitosamente: {}", savedEntity);
        return savedEntity;
    }

    public List<T> saveAll(Iterable<T> entities) {
        log.debug("Guardando múltiples entidades de tipo: {}", getEntityClass().getSimpleName());
        entities.forEach(this::validateEntity);
        List<T> savedEntities = getRepository().saveAll(entities);
        log.info("Guardadas {} entidades de tipo: {}", savedEntities.size(), getEntityClass().getSimpleName());
        return savedEntities;
    }

    public void deleteById(ID id) {
        log.debug("Eliminando entidad por ID: {} (tipo: {})", id, getEntityClass().getSimpleName());
        
        if (!existsById(id)) {
            throw new EntityNotFoundException(
                String.format("No se encontró entidad con ID: %s (tipo: %s)", 
                id, getEntityClass().getSimpleName())
            );
        }
        
        getRepository().deleteById(id);
        log.info("Entidad eliminada exitosamente: ID {}", id);
    }

    public void delete(T entity) {
        log.debug("Eliminando entidad: {}", entity);
        getRepository().delete(entity);
        log.info("Entidad eliminada exitosamente");
    }

    public void deleteAll() {
        log.warn("Eliminando todas las entidades de tipo: {}", getEntityClass().getSimpleName());
        getRepository().deleteAll();
        log.info("Todas las entidades eliminadas");
    }

    // === MÉTODOS DE UTILIDAD ===

    /**
     * Busca una entidad por ID y lanza excepción si no existe
     */
    public T findByIdOrThrow(ID id, String errorMessage) {
        return findById(id)
                .orElseThrow(() -> new EntityNotFoundException(errorMessage));
    }

    /**
     * Busca una entidad por ID con mensaje de error predeterminado
     */
    public T findByIdOrThrow(ID id) {
        return findByIdOrThrow(id, 
            String.format("No se encontró entidad con ID: %s (tipo: %s)", 
            id, getEntityClass().getSimpleName())
        );
    }

    /**
     * Actualiza una entidad existente
     */
    public T update(ID id, T entity) {
        log.debug("Actualizando entidad con ID: {} (tipo: {})", id, getEntityClass().getSimpleName());
        
        if (!existsById(id)) {
            throw new EntityNotFoundException(
                String.format("No se puede actualizar. Entidad no encontrada con ID: %s", id)
            );
        }
        
        validateEntity(entity);
        T updatedEntity = save(entity);
        log.info("Entidad actualizada exitosamente: {}", updatedEntity);
        return updatedEntity;
    }

    /**
     * Guarda una entidad solo si no existe
     */
    public T saveIfNotExists(T entity, ID id) {
        if (existsById(id)) {
            log.debug("Entidad ya existe con ID: {}, omitiendo guardado", id);
            return findById(id).get();
        }
        return save(entity);
    }

    // === MÉTODOS DE VALIDACIÓN ===

    /**
     * Valida una entidad antes de guardarla
     * Puede ser sobrescrito por servicios hijos para validaciones específicas
     */
    protected void validateEntity(T entity) {
        if (entity == null) {
            throw new IllegalArgumentException("La entidad no puede ser null");
        }
        // Validaciones adicionales pueden ser implementadas por servicios hijos
    }

    /**
     * Valida que una lista de IDs no esté vacía ni contenga nulos
     */
    protected void validateIds(List<ID> ids, String fieldName) {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " no puede estar vacío");
        }
        
        if (ids.contains(null)) {
            throw new IllegalArgumentException(fieldName + " no puede contener valores nulos");
        }
    }

    /**
     * Valida que un string no esté vacío
     */
    protected void validateNotBlank(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " no puede estar vacío");
        }
    }

    /**
     * Valida que un valor no sea nulo
     */
    protected void validateNotNull(Object value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " no puede ser null");
        }
    }

    // === MÉTODOS DE REFLEXIÓN ===

    /**
     * Obtiene la clase de la entidad usando reflexión
     */
    @SuppressWarnings("unchecked")
    protected Class<T> getEntityClass() {
        return (Class<T>) ((java.lang.reflect.ParameterizedType) 
                getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    }

    // === EXCEPCIONES PERSONALIZADAS ===

    public static class EntityNotFoundException extends RuntimeException {
        public EntityNotFoundException(String message) {
            super(message);
        }
    }

    public static class EntityValidationException extends RuntimeException {
        public EntityValidationException(String message) {
            super(message);
        }
    }

    // === MÉTODOS DE LOGGING ===

    protected void logOperation(String operation, Object... params) {
        log.info("Ejecutando operación: {} en {} con parámetros: {}", 
                operation, getEntityClass().getSimpleName(), params);
    }

    protected void logError(String operation, Exception e) {
        log.error("Error en operación: {} en {}: {}", 
                operation, getEntityClass().getSimpleName(), e.getMessage(), e);
    }

    // === MÉTODOS DE BATCH ===

    /**
     * Procesa entidades en lotes para mejorar rendimiento
     */
    public void processBatch(List<T> entities, int batchSize) {
        log.debug("Procesando {} entidades en lotes de {}", entities.size(), batchSize);
        
        for (int i = 0; i < entities.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, entities.size());
            List<T> batch = entities.subList(i, endIndex);
            
            log.debug("Procesando lote {}/{}", (i / batchSize) + 1, 
                    (int) Math.ceil((double) entities.size() / batchSize));
            
            saveAll(batch);
        }
        
        log.info("Procesamiento en lotes completado para {} entidades", entities.size());
    }
}