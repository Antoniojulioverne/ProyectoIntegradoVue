package com.ejemplos.modelo;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "mensaje")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Mensaje {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long mensajeId;
    
    @ManyToOne
    @JoinColumn(name = "chat_id", nullable = false)
    private Chat chat;
    
    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String contenido;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoMensaje tipo;
    
    @Column(name = "fecha_envio")
    private LocalDateTime fechaEnvio;
    
    @Column(name = "es_leido")
    private Boolean esLeido = false;
    
    @Column(name = "fecha_lectura")
    private LocalDateTime fechaLectura;
    
    @PrePersist
    protected void onCreate() {
        fechaEnvio = LocalDateTime.now();
        if (esLeido == null) {
            esLeido = false;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        if (esLeido != null && esLeido && fechaLectura == null) {
            fechaLectura = LocalDateTime.now();
        }
    }
}