package com.ejemplos.modelo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AmistadRepositorio extends JpaRepository<Amistad, Long> {
	 // Tus métodos originales
    List<Amistad> findByUsuario(Usuario usuario);
    boolean existsByUsuarioAndAmigo(Usuario usuario, Usuario amigo);
    
    // Nuevos métodos para el sistema sin duplicados
    @Query("SELECT a FROM Amistad a WHERE " +
           "(a.usuario.usuarioId = :usuarioId1 AND a.amigo.usuarioId = :usuarioId2) OR " +
           "(a.usuario.usuarioId = :usuarioId2 AND a.amigo.usuarioId = :usuarioId1)")
    Optional<Amistad> findAmistadEntreUsuarios(@Param("usuarioId1") Long usuarioId1, 
                                               @Param("usuarioId2") Long usuarioId2);
    
    @Query("SELECT a FROM Amistad a WHERE a.usuario.usuarioId = :usuarioId OR a.amigo.usuarioId = :usuarioId")
    List<Amistad> findAmistadesByUsuarioId(@Param("usuarioId") Long usuarioId);
    
    @Query("SELECT COUNT(a) > 0 FROM Amistad a WHERE " +
           "(a.usuario.usuarioId = :usuarioId1 AND a.amigo.usuarioId = :usuarioId2) OR " +
           "(a.usuario.usuarioId = :usuarioId2 AND a.amigo.usuarioId = :usuarioId1)")
    boolean existeAmistadEntreUsuarios(@Param("usuarioId1") Long usuarioId1, 
                                       @Param("usuarioId2") Long usuarioId2);
}
