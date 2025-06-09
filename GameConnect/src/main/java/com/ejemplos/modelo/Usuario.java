package com.ejemplos.modelo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@JsonIgnoreProperties({"amistades","partidas","chatsParticipante","mensajes"})
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long usuarioId;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;
    
    private String skin;

    // NUEVO CAMPO: Foto de perfil (URL o path del archivo)
    @Column(name = "foto_perfil", columnDefinition = "LONGTEXT")
    private String fotoPerfil;

    // NUEVO CAMPO: Fecha de creación de la cuenta (cuando se verifica)
    @Column(name = "fecha_creacion")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime fechaCreacion;

    // CAMPOS para verificación de email
    @Column(name = "codigo_verificacion")
    private String codigoVerificacion;

    @Column(name = "fecha_expiracion_codigo")
    private LocalDateTime fechaExpiracionCodigo;

    @Column(name = "email_verificado", nullable = false)
    private boolean emailVerificado = false;

    // CAMPOS para restablecimiento de contraseña
    private String tokenRecuperacion;
    private LocalDateTime fechaExpiracionToken;

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL)
    private List<Amistad> amistades = new ArrayList<>();

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL)
    private List<Partida> partidas = new ArrayList<>();

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL)
    private List<ChatParticipante> chatsParticipante = new ArrayList<>();

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL)
    private List<Mensaje> mensajes = new ArrayList<>();
}