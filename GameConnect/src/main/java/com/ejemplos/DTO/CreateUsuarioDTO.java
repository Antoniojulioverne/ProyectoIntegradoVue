package com.ejemplos.DTO;




import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateUsuarioDTO {

	private String username;
	private String skin;
	private String password;
	private String  email;
    private String fotoPerfil; // NUEVO: Base64 string de la imagen

}
