package com.ejemplos.modelo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface PeticionAmistadRepositorio extends JpaRepository<PeticionAmistad, Long> {
    
    // Encontrar peticiones pendientes recibidas por un usuario
    @Query("SELECT p FROM PeticionAmistad p WHERE p.usuarioDestinatario.usuarioId = :usuarioId AND p.estado = 'PENDIENTE'")
    List<PeticionAmistad> findPeticionesPendientesRecibidas(@Param("usuarioId") Long usuarioId);
    
    // Encontrar peticiones enviadas por un usuario
    @Query("SELECT p FROM PeticionAmistad p WHERE p.usuarioSolicitante.usuarioId = :usuarioId")
    List<PeticionAmistad> findPeticionesEnviadas(@Param("usuarioId") Long usuarioId);
    
    // Verificar si ya existe una petición entre dos usuarios (en cualquier dirección)
    @Query("SELECT p FROM PeticionAmistad p WHERE " +
           "(p.usuarioSolicitante.usuarioId = :usuario1Id AND p.usuarioDestinatario.usuarioId = :usuario2Id) OR " +
           "(p.usuarioSolicitante.usuarioId = :usuario2Id AND p.usuarioDestinatario.usuarioId = :usuario1Id)")
    Optional<PeticionAmistad> findPeticionEntreUsuarios(@Param("usuario1Id") Long usuario1Id, @Param("usuario2Id") Long usuario2Id);
    
    // Verificar si existe petición pendiente entre usuarios
    @Query("SELECT COUNT(p) > 0 FROM PeticionAmistad p WHERE " +
           "((p.usuarioSolicitante.usuarioId = :usuario1Id AND p.usuarioDestinatario.usuarioId = :usuario2Id) OR " +
           "(p.usuarioSolicitante.usuarioId = :usuario2Id AND p.usuarioDestinatario.usuarioId = :usuario1Id)) " +
           "AND p.estado = 'PENDIENTE'")
    boolean existePeticionPendienteEntreUsuarios(@Param("usuario1Id") Long usuario1Id, @Param("usuario2Id") Long usuario2Id);
}