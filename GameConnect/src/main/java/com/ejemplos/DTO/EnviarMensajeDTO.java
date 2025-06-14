package com.ejemplos.DTO;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnviarMensajeDTO {
    private Long chatId;
    private Long usuarioId;
    private String contenido;
    private String tipo = "TEXTO";
}