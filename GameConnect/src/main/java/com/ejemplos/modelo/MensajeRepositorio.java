package com.ejemplos.modelo;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MensajeRepositorio extends JpaRepository<Mensaje, Long> {
    
    List<Mensaje> findByChatChatIdOrderByFechaEnvioAsc(Long chatId);
    
    List<Mensaje> findByChatChatIdOrderByFechaEnvioDesc(Long chatId);
    
    // Nuevo método para contar mensajes no leídos en un chat específico
    @Query("SELECT COUNT(m) FROM Mensaje m WHERE m.chat.chatId = :chatId " +
           "AND m.usuario.usuarioId != :usuarioId " +
           "AND (m.esLeido IS NULL OR m.esLeido = false)")
    Long countMensajesNoLeidosEnChat(@Param("chatId") Long chatId, 
                                    @Param("usuarioId") Long usuarioId);
    
    // Método para obtener mensajes no leídos de un usuario en un chat específico
    @Query("SELECT m FROM Mensaje m WHERE m.chat.chatId = :chatId " +
           "AND m.usuario.usuarioId != :usuarioId " +
           "AND (m.esLeido IS NULL OR m.esLeido = false) " +
           "ORDER BY m.fechaEnvio ASC")
    List<Mensaje> findMensajesNoLeidosEnChat(@Param("chatId") Long chatId, 
                                           @Param("usuarioId") Long usuarioId);
    
    // Método para obtener todos los mensajes no leídos de un usuario
    @Query("SELECT m FROM Mensaje m " +
           "JOIN ChatParticipante cp ON cp.chat = m.chat " +
           "WHERE cp.usuario.usuarioId = :usuarioId " +
           "AND m.usuario.usuarioId != :usuarioId " +
           "AND (m.esLeido IS NULL OR m.esLeido = false) " +
           "ORDER BY m.fechaEnvio DESC")
    List<Mensaje> findAllMensajesNoLeidosDeUsuario(@Param("usuarioId") Long usuarioId);
}