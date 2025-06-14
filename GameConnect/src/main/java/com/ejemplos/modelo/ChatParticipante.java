package com.ejemplos.modelo;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "chat_participante")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatParticipante {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "chat_id", nullable = false)
    private Chat chat;
    
    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;
    
    @Column(name = "fecha_union")
    private LocalDateTime fechaUnion;
    
    @Column(name = "es_admin")
    private Boolean esAdmin = false;
    
    @PrePersist
    protected void onCreate() {
        fechaUnion = LocalDateTime.now();
    }
}