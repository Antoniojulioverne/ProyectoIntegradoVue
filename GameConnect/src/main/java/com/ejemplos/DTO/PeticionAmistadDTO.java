package com.ejemplos.DTO;

import lombok.Data;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

@Data
public class PeticionAmistadDTO {
    private Long peticionId;
    private Long usuarioSolicitanteId;
    private String usernameRemitente;
    private String emailRemitente;
    private String skinRemitente;
  private String fotopersonaRemitente; 
    private Long usuarioDestinatarioId;
    private String usernameDestinatario;
    private String emailDestinatario;
    private String skinDestinatario;
    private String fotopersonaDestinatario;
    
    private String estado;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime fechaCreacion;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime fechaRespuesta;
}
