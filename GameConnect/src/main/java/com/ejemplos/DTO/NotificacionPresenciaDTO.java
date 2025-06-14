package com.ejemplos.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificacionPresenciaDTO {
    private Long chatId;
    private Long usuarioId;
    private String usuarioNombre;
    private boolean activo;
}