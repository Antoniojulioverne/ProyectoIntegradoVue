package com.ejemplos.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ObtenerMensajesDTO {
    private Long chatId;
    private Long usuarioId;
}