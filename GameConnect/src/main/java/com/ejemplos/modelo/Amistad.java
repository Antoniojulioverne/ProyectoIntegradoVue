package com.ejemplos.modelo;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "amistad", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"usuario_id", "amigo_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Amistad {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "amistad_id")
    private Long amistadId;
    
    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;
    
    @ManyToOne
    @JoinColumn(name = "amigo_id", nullable = false)
    private Usuario amigo;
    
    // Solo este constructor para ordenar los IDs
    public Amistad(Usuario usuario1, Usuario usuario2) {
        if (usuario1.getUsuarioId() < usuario2.getUsuarioId()) {
            this.usuario = usuario1;
            this.amigo = usuario2;
        } else {
            this.usuario = usuario2;
            this.amigo = usuario1;
        }
    }
}