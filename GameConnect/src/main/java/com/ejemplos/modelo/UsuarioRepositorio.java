package com.ejemplos.modelo;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface UsuarioRepositorio extends JpaRepository<Usuario, Long> {
Usuario findByEmail(String email);
    
    Usuario findByUsername(String username);
    // NUEVOS MÉTODOS para la verificación
    Usuario findByCodigoVerificacion(String codigoVerificacion);
    Usuario findByTokenRecuperacion(String token);
 boolean existsByEmail(String email);
    
    boolean existsByUsername(String username);
    @Query("UPDATE Usuario u SET u.tokenRecuperacion = null, u.fechaExpiracionToken = null WHERE u.fechaExpiracionToken < :now")
    @Modifying
    @Transactional
    void limpiarTokensExpirados(LocalDateTime now);
    
    
 // Método para buscar usuarios por nombre o email (más eficiente que filtrar en memoria)
    @Query("SELECT u FROM Usuario u WHERE " +
           "(LOWER(u.username) LIKE LOWER(CONCAT('%', :termino, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :termino, '%'))) " +
           "AND u.emailVerificado = true AND u.usuarioId <> :usuarioActualId")
    List<Usuario> buscarPorUsernameOEmail(
            @Param("termino") String termino, 
            @Param("usuarioActualId") Long usuarioActualId);
}

