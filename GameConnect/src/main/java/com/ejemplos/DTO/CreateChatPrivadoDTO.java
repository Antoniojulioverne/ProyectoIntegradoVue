package com.ejemplos.DTO;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateChatPrivadoDTO {
    private Long usuario1Id;
    private Long usuario2Id;
}
