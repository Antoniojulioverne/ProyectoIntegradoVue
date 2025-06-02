package com.ejemplos.DTO;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MensajeDTO {
    private Long mensajeId;
    private Long chatId;
    private Long usuarioId;
    private String username;
    private String contenido;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime fechaEnvio;
    private String tipo;
    private Boolean esLeido;
}