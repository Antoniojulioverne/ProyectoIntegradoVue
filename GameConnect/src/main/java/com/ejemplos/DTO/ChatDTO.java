package com.ejemplos.DTO;

import java.time.LocalDateTime;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatDTO {
    private Long chatId;
    private String nombreChat;
    private String tipo;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime fechaCreacion;
    private List<UsuarioDTO> participantes;
    private MensajeDTO ultimoMensaje;
    private Long mensajesNoLeidos;
}