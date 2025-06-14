package com.ejemplos.DTO;

import com.ejemplos.modelo.PeticionAmistad;
import org.springframework.stereotype.Component;

@Component
public class PeticionAmistadDTOConverter {
    
    public PeticionAmistadDTO convertirADto(PeticionAmistad peticion) {
        PeticionAmistadDTO dto = new PeticionAmistadDTO();
        dto.setPeticionId(peticion.getPeticionId());
        
        // Información del solicitante (quien envió la petición)
        dto.setUsuarioSolicitanteId(peticion.getUsuarioSolicitante().getUsuarioId());
        dto.setUsernameRemitente(peticion.getUsuarioSolicitante().getUsername());
        dto.setEmailRemitente(peticion.getUsuarioSolicitante().getEmail());
        dto.setSkinRemitente(peticion.getUsuarioSolicitante().getSkin());
        dto.setFotopersonaRemitente(peticion.getUsuarioSolicitante().getFotoPerfil());
        
        // Información del destinatario (quien recibe la petición) ***
        dto.setUsuarioDestinatarioId(peticion.getUsuarioDestinatario().getUsuarioId());
        dto.setUsernameDestinatario(peticion.getUsuarioDestinatario().getUsername());
        dto.setEmailDestinatario(peticion.getUsuarioDestinatario().getEmail());
        dto.setSkinDestinatario(peticion.getUsuarioDestinatario().getSkin());
        dto.setFotopersonaDestinatario(peticion.getUsuarioDestinatario().getFotoPerfil());
        
        dto.setEstado(peticion.getEstado().toString());
        dto.setFechaCreacion(peticion.getFechaCreacion());
        dto.setFechaRespuesta(peticion.getFechaRespuesta());
        
        return dto;
    }
}