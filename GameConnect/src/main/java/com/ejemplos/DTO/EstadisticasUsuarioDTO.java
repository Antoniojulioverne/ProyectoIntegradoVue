package com.ejemplos.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * DTO para estadísticas completas de usuario
 * Contiene métricas calculadas y datos agregados
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EstadisticasUsuarioDTO {
    
    // Información básica del usuario
    private Long usuarioId;
    private String username;
    
    // Estadísticas de partidas
    private Integer totalPartidas;
    private Integer puntosMaximos;
    private Integer puntosTotales;
    private Integer monedasTotales;
    private Double promedioPartida;
    
    // Información temporal
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime fechaMejorPartida;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime fechaUltimaPartida;
    
    // Métricas adicionales
    private Integer rankingPosicion;
    private Double porcentajeMejoramiento; // Comparado con la media del usuario
    private String nivelJugador; // PRINCIPIANTE, INTERMEDIO, AVANZADO, EXPERTO
    
    // Estadísticas temporales (últimos 7 días, 30 días)
    private EstadisticasTemporales ultimos7Dias;
    private EstadisticasTemporales ultimos30Dias;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EstadisticasTemporales {
        private Integer partidasJugadas;
        private Integer puntosTotales;
        private Integer monedasTotales;
        private Double promedioPuntos;
        private Integer mejorPuntaje;
    }
    
    // Métodos de conveniencia
    public String getNivelJugador() {
        if (nivelJugador != null) {
            return nivelJugador;
        }
        
        // Calcular nivel basado en puntos máximos
        if (puntosMaximos == null || puntosMaximos == 0) {
            return "PRINCIPIANTE";
        } else if (puntosMaximos < 1000) {
            return "PRINCIPIANTE";
        } else if (puntosMaximos < 5000) {
            return "INTERMEDIO";
        } else if (puntosMaximos < 10000) {
            return "AVANZADO";
        } else {
            return "EXPERTO";
        }
    }
    
    public Double getPromedioPartida() {
        if (promedioPartida != null) {
            return Math.round(promedioPartida * 100.0) / 100.0;
        }
        
        if (totalPartidas != null && totalPartidas > 0 && puntosTotales != null) {
            return Math.round(((double) puntosTotales / totalPartidas) * 100.0) / 100.0;
        }
        
        return 0.0;
    }
}