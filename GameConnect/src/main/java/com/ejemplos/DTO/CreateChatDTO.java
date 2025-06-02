package com.ejemplos.DTO;

import java.util.List;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateChatDTO {
    private String nombreChat;
    private String tipo; // PRIVADO, GRUPO
    private List<Long> participantesIds;
}