package com.ejemplos.DTO;

import lombok.Data;

@Data
public class UsuarioBusquedaDTO {
    private Long usuarioId;
    private String username;
    private String email;
    private String skin;
    private String estadoRelacion; // "SIN_RELACION", "AMISTAD_EXISTENTE", "PETICION_ENVIADA", "PETICION_RECIBIDA"
}