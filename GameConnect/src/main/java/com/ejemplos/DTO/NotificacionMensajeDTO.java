package com.ejemplos.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificacionMensajeDTO {
    private Long chatId;
    private Long remitenteId;
    private String remitenteNombre;
    private String contenidoPreview;
    private String tipoMensaje;
    private LocalDateTime fechaEnvio;
}