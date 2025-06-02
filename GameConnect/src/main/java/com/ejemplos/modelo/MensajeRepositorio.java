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
	    
	    @Query("SELECT m FROM Mensaje m WHERE m.chat.chatId = :chatId AND m.usuario.usuarioId != :usuarioId AND (m.esLeido = false OR m.esLeido IS NULL)")
	    List<Mensaje> findMensajesNoLeidosEnChat(@Param("chatId") Long chatId, @Param("usuarioId") Long usuarioId);
	    
	    @Query("SELECT COUNT(m) FROM Mensaje m WHERE m.chat.chatId = :chatId AND m.usuario.usuarioId != :usuarioId AND (m.esLeido = false OR m.esLeido IS NULL)")
	    Long countMensajesNoLeidosEnChat(@Param("chatId") Long chatId, @Param("usuarioId") Long usuarioId);
	}