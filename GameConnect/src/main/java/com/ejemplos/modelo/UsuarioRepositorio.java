package com.ejemplos.modelo;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
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
}
