package com.ejemplos.DTO;

import lombok.Data;

@Data
public class UsuarioBusquedaDTO {
    private Long usuarioId;
    private String username;
    private String email;
    private String skin;
    private String fotoPerfil;
    private String estadoRelacion; // "SIN_RELACION", "AMISTAD_EXISTENTE", "PETICION_ENVIADA", "PETICION_RECIBIDA"
    private Long peticionId; // ID de la petición si existe una relación de petición recibida
    private boolean emailVerificado; // Añadido para indicar si el usuario ha verificado su email
}