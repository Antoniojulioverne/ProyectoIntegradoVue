package com.ejemplos.DTO;

import org.springframework.stereotype.Component;
import com.ejemplos.modelo.Mensaje;

@Component
public class MensajeDTOConverter {
    
    public MensajeDTO convertirADto(Mensaje mensaje) {
        return MensajeDTO.builder()
            .mensajeId(mensaje.getMensajeId())
            .chatId(mensaje.getChat().getChatId())
            .usuarioId(mensaje.getUsuario().getUsuarioId())
            .username(mensaje.getUsuario().getUsername())
            .contenido(mensaje.getContenido())
            .fechaEnvio(mensaje.getFechaEnvio())
            .tipo(mensaje.getTipo().toString())
            .esLeido(mensaje.getEsLeido())
            .build();
    }
}