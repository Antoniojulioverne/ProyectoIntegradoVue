package com.ejemplos.modelo;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "partida")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Partida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "partida_id")
    private Long partidaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "puntos", nullable = false)
    private int puntos;

    @Column(name = "monedas", nullable = false)
    private int monedas;
    
    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;
}
