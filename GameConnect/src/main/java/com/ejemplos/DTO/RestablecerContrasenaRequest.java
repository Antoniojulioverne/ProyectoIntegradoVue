package com.ejemplos.DTO;

import lombok.Data;

@Data
public class RestablecerContrasenaRequest {
    private String token;
    private String nuevaContrasena;
}