package com.ejemplos.modelo;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatRepositorio extends JpaRepository<Chat, Long> {
    
    @Query("SELECT c FROM Chat c JOIN c.participantes p WHERE p.usuario.usuarioId = :usuarioId")
    List<Chat> findChatsByUsuarioId(@Param("usuarioId") Long usuarioId);
    
    @Query("SELECT c FROM Chat c JOIN ChatParticipante cp1 ON cp1.chat = c JOIN ChatParticipante cp2 ON cp2.chat = c " +
    	       "WHERE c.tipo = 'PRIVADO' AND " +
    	       "((cp1.usuario.usuarioId = :usuario1Id AND cp2.usuario.usuarioId = :usuario2Id) " +
    	       "OR (cp1.usuario.usuarioId = :usuario2Id AND cp2.usuario.usuarioId = :usuario1Id))")
    	Optional<Chat> findChatPrivadoEntreUsuarios(@Param("usuario1Id") Long usuario1Id,
    	                                            @Param("usuario2Id") Long usuario2Id);
}