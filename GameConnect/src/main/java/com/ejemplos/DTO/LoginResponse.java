package com.ejemplos.DTO;

import com.ejemplos.modelo.Usuario;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private Usuario usuario;
}