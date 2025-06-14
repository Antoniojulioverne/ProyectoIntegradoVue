package com.ejemplos.modelo;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "peticion_amistad", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"usuario_solicitante_id", "usuario_destinatario_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PeticionAmistad {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "peticion_id")
    private Long peticionId;
    
    @ManyToOne
    @JoinColumn(name = "usuario_solicitante_id", nullable = false)
    private Usuario usuarioSolicitante;
    
    @ManyToOne
    @JoinColumn(name = "usuario_destinatario_id", nullable = false)
    private Usuario usuarioDestinatario;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoPeticion estado;
    
    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;
    
    @Column(name = "fecha_respuesta")
    private LocalDateTime fechaRespuesta;
    
    public enum EstadoPeticion {
        PENDIENTE,
        ACEPTADA,
        RECHAZADA
    }
    
    @PrePersist
    public void prePersist() {
        this.fechaCreacion = LocalDateTime.now();
        if (this.estado == null) {
            this.estado = EstadoPeticion.PENDIENTE;
        }
    }
}