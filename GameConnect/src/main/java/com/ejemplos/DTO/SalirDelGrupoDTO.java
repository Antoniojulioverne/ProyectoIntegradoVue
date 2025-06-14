package com.ejemplos.DTO;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalirDelGrupoDTO {
    private Long chatId;
    private Long usuarioId;
}
