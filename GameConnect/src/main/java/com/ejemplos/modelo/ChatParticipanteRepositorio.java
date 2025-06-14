package com.ejemplos.modelo;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatParticipanteRepositorio extends JpaRepository<ChatParticipante, Long> {
    
    List<ChatParticipante> findByChatChatId(Long chatId);
    
    Optional<ChatParticipante> findByChatChatIdAndUsuarioUsuarioId(Long chatId, Long usuarioId);
    
    boolean existsByChatChatIdAndUsuarioUsuarioId(Long chatId, Long usuarioId);
}