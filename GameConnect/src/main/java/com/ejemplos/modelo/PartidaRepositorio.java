package com.ejemplos.modelo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio mejorado para entidad Partida
 * Incluye consultas optimizadas y métodos para estadísticas
 */
@Repository
public interface PartidaRepositorio extends JpaRepository<Partida, Long> {
    
    // === CONSULTAS BÁSICAS ===
    
    /**
     * Encuentra todas las partidas de un usuario ordenadas por fecha descendente
     */
    List<Partida> findByUsuarioOrderByFechaDesc(Usuario usuario);
    
    /**
     * Encuentra partidas de un usuario con paginación
     */
    Page<Partida> findByUsuarioOrderByFechaDesc(Usuario usuario, Pageable pageable);
    
    /**
     * Cuenta el total de partidas de un usuario
     */
    long countByUsuario(Usuario usuario);
    
    // === CONSULTAS DE ESTADÍSTICAS ===
    
    /**
     * Encuentra la partida con mayor puntaje de un usuario
     */
    Optional<Partida> findTopByUsuarioOrderByPuntosDesc(Usuario usuario);
    
    /**
     * Suma total de puntos de un usuario
     */
    @Query("SELECT COALESCE(SUM(p.puntos), 0) FROM Partida p WHERE p.usuario = :usuario")
    Integer sumPuntosByUsuario(@Param("usuario") Usuario usuario);
    
    /**
     * Suma total de monedas de un usuario
     */
    @Query("SELECT COALESCE(SUM(p.monedas), 0) FROM Partida p WHERE p.usuario = :usuario")
    Integer sumMonedasByUsuario(@Param("usuario") Usuario usuario);
    
    /**
     * Calcula el promedio de puntos de un usuario
     */
    @Query("SELECT AVG(p.puntos) FROM Partida p WHERE p.usuario = :usuario")
    Double avgPuntosByUsuario(@Param("usuario") Usuario usuario);
    
    // === CONSULTAS TEMPORALES ===
    
    /**
     * Encuentra partidas de un usuario en un rango de fechas
     */
    @Query("SELECT p FROM Partida p WHERE p.usuario = :usuario AND p.fecha BETWEEN :fechaInicio AND :fechaFin ORDER BY p.fecha DESC")
    List<Partida> findByUsuarioAndFechaBetweenOrderByFechaDesc(
        @Param("usuario") Usuario usuario,
        @Param("fechaInicio") LocalDateTime fechaInicio,
        @Param("fechaFin") LocalDateTime fechaFin
    );
    
    /**
     * Encuentra partidas de los últimos N días
     */
    @Query("SELECT p FROM Partida p WHERE p.usuario = :usuario AND p.fecha >= :fechaLimite ORDER BY p.fecha DESC")
    List<Partida> findByUsuarioAndFechaAfterOrderByFechaDesc(
        @Param("usuario") Usuario usuario,
        @Param("fechaLimite") LocalDateTime fechaLimite
    );
    
    /**
     * Cuenta partidas de un usuario en los últimos N días
     */
    @Query("SELECT COUNT(p) FROM Partida p WHERE p.usuario = :usuario AND p.fecha >= :fechaLimite")
    long countByUsuarioAndFechaAfter(@Param("usuario") Usuario usuario, @Param("fechaLimite") LocalDateTime fechaLimite);
    
    // === CONSULTAS DE RANKING ===
    
    /**
     * Obtiene el mejor puntaje por usuario para ranking
     */
    @Query("SELECT p.usuario, MAX(p.puntos) as maxPuntos FROM Partida p GROUP BY p.usuario ORDER BY maxPuntos DESC")
    List<Object[]> findMaxPuntosByUsuario();
    
    /**
     * Obtiene estadísticas completas por usuario
     */
    @Query("""
        SELECT p.usuario, 
               MAX(p.puntos) as maxPuntos, 
               SUM(p.puntos) as totalPuntos, 
               COUNT(p) as totalPartidas,
               AVG(p.puntos) as promedioPuntos,
               MAX(p.fecha) as ultimaPartida
        FROM Partida p 
        GROUP BY p.usuario 
        ORDER BY maxPuntos DESC
        """)
    List<Object[]> findEstadisticasCompletas();
    
    // === CONSULTAS DE ANÁLISIS ===
    
    /**
     * Encuentra las mejores partidas del sistema
     */
    @Query("SELECT p FROM Partida p ORDER BY p.puntos DESC")
    Page<Partida> findTopPartidas(Pageable pageable);
    
    /**
     * Encuentra partidas por rango de puntos
     */
    @Query("SELECT p FROM Partida p WHERE p.puntos BETWEEN :minPuntos AND :maxPuntos ORDER BY p.fecha DESC")
    List<Partida> findByPuntosBetweenOrderByFechaDesc(
        @Param("minPuntos") Integer minPuntos, 
        @Param("maxPuntos") Integer maxPuntos
    );
    
    /**
     * Obtiene estadísticas globales del sistema
     */
    @Query("""
        SELECT 
            COUNT(p) as totalPartidas,
            AVG(p.puntos) as promedioPuntos,
            MAX(p.puntos) as maxPuntos,
            MIN(p.puntos) as minPuntos,
            SUM(p.puntos) as totalPuntos
        FROM Partida p
        """)
    List<Object[]> findEstadisticasGlobales();
    
    // === CONSULTAS DE TENDENCIAS ===
    
    /**
     * Obtiene partidas agrupadas por día para gráficos de tendencias
     */
    @Query("""
        SELECT DATE(p.fecha) as fecha, 
               COUNT(p) as partidasJugadas,
               AVG(p.puntos) as promedioPuntos,
               MAX(p.puntos) as mejorPuntaje
        FROM Partida p 
        WHERE p.usuario = :usuario 
        AND p.fecha >= :fechaInicio
        GROUP BY DATE(p.fecha)
        ORDER BY fecha DESC
        """)
    List<Object[]> findTendenciasDiarias(
        @Param("usuario") Usuario usuario,
        @Param("fechaInicio") LocalDateTime fechaInicio
    );
    
    /**
     * Encuentra la racha más larga de mejoras de un usuario
     */
    @Query("""
        SELECT p FROM Partida p 
        WHERE p.usuario = :usuario 
        ORDER BY p.fecha ASC
        """)
    List<Partida> findPartidasParaCalcularRachas(@Param("usuario") Usuario usuario);
    
    // === CONSULTAS DE COMPARACIÓN ===
    
    /**
     * Compara estadísticas de un usuario con el promedio global
     */
    @Query("""
        SELECT 
            (SELECT AVG(p1.puntos) FROM Partida p1 WHERE p1.usuario = :usuario) as promedioUsuario,
            (SELECT AVG(p2.puntos) FROM Partida p2) as promedioGlobal,
            (SELECT MAX(p3.puntos) FROM Partida p3 WHERE p3.usuario = :usuario) as maxUsuario,
            (SELECT MAX(p4.puntos) FROM Partida p4) as maxGlobal
        """)
    List<Object[]> findComparacionConGlobal(@Param("usuario") Usuario usuario);
    
    /**
     * Encuentra usuarios con mejor rendimiento que el usuario dado
     */
    @Query("""
        SELECT COUNT(DISTINCT p.usuario) 
        FROM Partida p 
        WHERE p.usuario != :usuario
        AND (SELECT MAX(p2.puntos) FROM Partida p2 WHERE p2.usuario = p.usuario) > 
            (SELECT MAX(p3.puntos) FROM Partida p3 WHERE p3.usuario = :usuario)
        """)
    long countUsuariosConMejorRendimiento(@Param("usuario") Usuario usuario);
    
    // === CONSULTAS DE VALIDACIÓN ===
    
    /**
     * Verifica si existe una partida con puntaje sospechosamente alto
     */
    @Query("SELECT COUNT(p) FROM Partida p WHERE p.puntos > :limite")
    long countPartidasConPuntajeSospechoso(@Param("limite") Integer limite);
    
    /**
     * Encuentra partidas duplicadas (mismo usuario, mismos puntos, fecha muy cercana)
     */
    @Query("""
        SELECT p1 FROM Partida p1, Partida p2 
        WHERE p1.usuario = p2.usuario 
        AND p1.puntos = p2.puntos 
        AND p1.partidaId != p2.partidaId
        AND ABS(TIMESTAMPDIFF(MINUTE, p1.fecha, p2.fecha)) < 5
        """)
    List<Partida> findPosiblesDuplicados();
    
    // === CONSULTAS DE LIMPIEZA ===
    
    /**
     * Elimina partidas antiguas (para mantenimiento)
     */
    @Query("DELETE FROM Partida p WHERE p.fecha < :fechaLimite")
    void deletePartidasAnterioresA(@Param("fechaLimite") LocalDateTime fechaLimite);
    
    /**
     * Encuentra partidas huérfanas (sin usuario válido)
     */
    @Query("SELECT p FROM Partida p WHERE p.usuario IS NULL")
    List<Partida> findPartidasHuerfanas();
    
    // === MÉTODOS DE CONVENIENCIA ===
    
    /**
     * Verifica si un usuario tiene partidas
     */
    boolean existsByUsuario(Usuario usuario);
    
    /**
     * Encuentra la última partida de un usuario
     */
    Optional<Partida> findTopByUsuarioOrderByFechaDesc(Usuario usuario);
    
    /**
     * Encuentra la primera partida de un usuario
     */
    Optional<Partida> findTopByUsuarioOrderByFechaAsc(Usuario usuario);
    
    /**
     * Cuenta partidas con puntuación específica
     */
    long countByPuntos(Integer puntos);
    
    /**
     * Encuentra partidas de un usuario con puntuación mínima
     */
    @Query("SELECT p FROM Partida p WHERE p.usuario = :usuario AND p.puntos >= :minPuntos ORDER BY p.fecha DESC")
    List<Partida> findByUsuarioAndPuntosGreaterThanEqualOrderByFechaDesc(
        @Param("usuario") Usuario usuario,
        @Param("minPuntos") Integer minPuntos
    );
}