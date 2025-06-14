package com.ejemplos.DTO;




import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreatePartidaDTO {
    
    private Long usuarioId;
	private int puntos;
	private int monedas;

	}