package com.ejemplos.DTO;

import com.ejemplos.modelo.PeticionAmistad;
import org.springframework.stereotype.Component;

@Component
public class PeticionAmistadDTOConverter {
    
    public PeticionAmistadDTO convertirADto(PeticionAmistad peticion) {
        PeticionAmistadDTO dto = new PeticionAmistadDTO();
        dto.setPeticionId(peticion.getPeticionId());
        dto.setUsuarioSolicitanteId(peticion.getUsuarioSolicitante().getUsuarioId());
        dto.setUsernameRemitente(peticion.getUsuarioSolicitante().getUsername());
        dto.setEmailRemitente(peticion.getUsuarioSolicitante().getEmail());
        dto.setSkinRemitente(peticion.getUsuarioSolicitante().getSkin());
        dto.setEstado(peticion.getEstado().toString());
        dto.setFechaCreacion(peticion.getFechaCreacion());
        dto.setFechaRespuesta(peticion.getFechaRespuesta());
        return dto;
    }
}