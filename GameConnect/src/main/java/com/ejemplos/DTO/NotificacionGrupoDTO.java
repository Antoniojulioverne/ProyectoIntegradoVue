package com.ejemplos.DTO;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificacionGrupoDTO {
    private Long chatId;
    private Long usuarioId;
    private String usuarioNombre;
    private String accion; // "SALIR_GRUPO", "NUEVO_PARTICIPANTE", "PROMOVER_ADMIN", etc.
    private String mensaje;
    private LocalDateTime timestamp;
}