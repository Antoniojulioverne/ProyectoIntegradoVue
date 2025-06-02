package com.ejemplos.DTO;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

@Data
public class UsuarioRankingDTO {

    private String username;
    private int puntosMaximos;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private String fechaPartida; // <-- String en vez de LocalDateTime

    public UsuarioRankingDTO(String username, int puntosMaximos, LocalDateTime fechaPartida) {
        this.username = username;
        this.puntosMaximos = puntosMaximos;
        this.fechaPartida = (fechaPartida != null)
            ? fechaPartida.toString() // o usar .format(...)
            : "No disponible";
    }
}

