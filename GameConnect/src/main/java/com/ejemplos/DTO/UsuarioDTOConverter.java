package com.ejemplos.DTO;




import org.modelmapper.ModelMapper; 
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ejemplos.modelo.Usuario;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UsuarioDTOConverter {

	@Autowired
	private ModelMapper modelMapper;
	
	
	public UsuarioDTO convertirADto(Usuario usuario) {
		return modelMapper.map(usuario, UsuarioDTO.class);
	}
	
	
	public UsuarioDTOWeb convertirADtoWeb(Usuario usuario) {
		return modelMapper.map(usuario, UsuarioDTOWeb.class);
	}
	
	
	public Usuario convertirAUsuario(CreateUsuarioDTO createUsuarioDTO) {
		return modelMapper.map(createUsuarioDTO, Usuario.class);
	}
	
}
