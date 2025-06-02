package com.ejemplos.DTO;

import org.modelmapper.ModelMapper; 
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ejemplos.modelo.Partida;
import com.ejemplos.modelo.Usuario;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PartidaDTOConverter {
	@Autowired
	private ModelMapper modelMapper;
	
	
	public PartidaDTO convertirADto(Partida partida) {
		return modelMapper.map(partida, PartidaDTO.class);
	}
	
	
	public Partida convertirAPartida(CreatePartidaDTO createPartidaDTO) {
	    Partida partida = modelMapper.map(createPartidaDTO, Partida.class);
	    partida.setFecha(java.time.LocalDateTime.now());
	    return partida;
	}
}
