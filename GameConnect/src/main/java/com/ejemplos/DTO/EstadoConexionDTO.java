package com.ejemplos.DTO;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EstadoConexionDTO {
    private Long usuarioId;
    private String usuarioNombre;
    private boolean conectado;
    private Long chatId; // opcional, para presencia en chat específico
}