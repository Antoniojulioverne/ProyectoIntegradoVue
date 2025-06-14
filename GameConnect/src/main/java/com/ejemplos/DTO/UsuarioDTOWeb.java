package com.ejemplos.DTO;

 
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UsuarioDTOWeb {

	private Long usuarioId;
	private String username;
	private String skin;
	private String  email;
	private String  password;
	//nombre de entidad+nombreDelAtributo identi
}
