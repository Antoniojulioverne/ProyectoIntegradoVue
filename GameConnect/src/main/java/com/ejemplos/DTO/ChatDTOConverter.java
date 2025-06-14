// ChatDTOConverter.java - CORREGIDO
package com.ejemplos.DTO;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ejemplos.modelo.*;
import com.ejemplos.servicios.MensajeEncriptarService; // *** AGREGAR IMPORT ***

@Component
public class ChatDTOConverter {
    
    @Autowired
    private UsuarioDTOConverter usuarioDTOConverter;
    
    @Autowired
    private MensajeDTOConverter mensajeDTOConverter;
    
    @Autowired
    private MensajeRepositorio mensajeRepositorio;
    
    // *** AGREGAR ESTA LÍNEA ***
    @Autowired
    private MensajeEncriptarService mensajeEncriptarService;
    
    public ChatDTO convertirADto(Chat chat, Long usuarioActual) {
        List<UsuarioDTO> participantes = chat.getParticipantes().stream()
            .map(p -> usuarioDTOConverter.convertirADto(p.getUsuario()))
            .collect(Collectors.toList());
        
        // Obtener último mensaje
        MensajeDTO ultimoMensaje = null;
        if (!chat.getMensajes().isEmpty()) {
            Mensaje ultimo = chat.getMensajes().get(chat.getMensajes().size() - 1);
            ultimoMensaje = mensajeDTOConverter.convertirADto(ultimo);
            
            // *** AGREGAR ESTA LÍNEA - DESCIFRAR EL ÚLTIMO MENSAJE ***
            ultimoMensaje.setContenido(mensajeEncriptarService.descifrarMensaje(ultimo.getContenido()));
        }
        
        // Contar mensajes no leídos
        Long mensajesNoLeidos = mensajeRepositorio.countMensajesNoLeidosEnChat(
            chat.getChatId(), usuarioActual);
        
        return ChatDTO.builder()
            .chatId(chat.getChatId())
            .nombreChat(chat.getNombreChat())
            .tipo(chat.getTipo().toString())
            .fechaCreacion(chat.getFechaCreacion())
            .participantes(participantes)
            .ultimoMensaje(ultimoMensaje)
            .mensajesNoLeidos(mensajesNoLeidos)
            .build();
    }
}

// TAMBIÉN CORREGIR MensajeDTOConverter.java si es necesario
// (Pero probablemente no sea necesario ya que este se usa después del descifrado)