package com.ejemplos.DTO;

import lombok.*;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;



@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartidaDTO {
    private Long id;
    private int puntos;
    private int monedas;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime fecha;
}
