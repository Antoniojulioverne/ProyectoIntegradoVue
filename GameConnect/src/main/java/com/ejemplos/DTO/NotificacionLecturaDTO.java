package com.ejemplos.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificacionLecturaDTO {
    private Long chatId;
    private Long usuarioId;
    private String usuarioNombre;
    private List<Long> mensajesLeidosIds;
    private LocalDateTime fechaMarcado;
}