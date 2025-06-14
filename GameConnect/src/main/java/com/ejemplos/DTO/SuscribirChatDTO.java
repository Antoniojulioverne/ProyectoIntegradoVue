package com.ejemplos.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SuscribirChatDTO {
    private Long chatId;
    private Long usuarioId;
}