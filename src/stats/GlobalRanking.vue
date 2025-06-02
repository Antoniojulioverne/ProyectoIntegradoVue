<template>
  <div class="ranking-container">
    <LoadingSpinner 
      v-if="isLoading"
      message="Cargando ranking..."
    />
    
    <ErrorCard 
      v-else-if="error"
      :message="error"
      @retry="$emit('retry')"
    />
    
    <ion-card v-else class="ranking-card">
      <ion-card-header>
        <ion-card-title>Ranking Global</ion-card-title>
        <ion-card-subtitle>
          Top jugadores ({{ ranking.length }} jugadores)
        </ion-card-subtitle>
      </ion-card-header>
      
      <ion-card-content>
        <EmptyState 
          v-if="ranking.length === 0"
          title="No hay datos de ranking disponibles."
        />
        
        <div v-else>
          <!-- Desktop Table -->
          <div class="table-container">
            <table class="ranking-table">
              <thead>
                <tr>
                  <th class="position-header">Posición</th>
                  <th class="player-header">Jugador</th>
                  <th class="score-header">Puntos Máximos</th>
                  <th class="date-header">Fecha</th>
                </tr>
              </thead>
              <tbody>
                <RankingRow
                  v-for="(player, index) in ranking"
                  :key="`${player.username}-${index}`"
                  :player="player"
                  :position="index + 1"
                  :is-current-user="player.username === currentUsername"
                />
              </tbody>
            </table>
          </div>
          
        </div>
      </ion-card-content>
    </ion-card>
  </div>
</template>

<script setup>
import { 
  IonCard, IonCardHeader, IonCardTitle,
  IonCardSubtitle, IonCardContent 
} from '@ionic/vue';
import LoadingSpinner from '../ui/LoadingSpinner.vue';
import ErrorCard from '../ui/ErrorCard.vue';
import EmptyState from '../ui/EmptyState.vue';
import RankingRow from './RankingRow.vue';


defineProps({
  ranking: {
    type: Array,
    default: () => []
  },
  currentUsername: {
    type: String,
    default: ''
  },
  isLoading: {
    type: Boolean,
    default: false
  },
  error: {
    type: String,
    default: null
  }
});

defineEmits(['retry']);
</script>

<style scoped>
/* Estilos base para la tabla */
.table-container {
  overflow-x: auto;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.ranking-table {
  width: 100%;
  border-collapse: collapse;
  background-color: var(--ion-color-light);
  min-width: 600px;
}

/* Responsive adjustments para móviles */
@media (max-width: 768px) {
  .table-container {
    overflow-x: visible;
    border-radius: 0;
    box-shadow: none;
  }
  
  .ranking-table {
    min-width: 100%;
    display: block;
    overflow: hidden;
  }
  
  .ranking-table thead {
    display: block;
  }
  
  .ranking-table tbody {
    display: block;
  }
  
  .ranking-table tr {
    display: block;
    margin-bottom: 8px;
    border-radius: 8px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
    overflow: hidden;
  }
  
  .ranking-table th,
  .ranking-table td {
    display: block;
    text-align: left !important;
    border: none;
    padding: 8px 12px;
  }
  
  /* Header mobile - más compacto */
  .ranking-table thead tr {
    position: absolute;
    left: -9999px;
    opacity: 0;
  }
  
  /* Cada celda como fila con etiqueta */
  .ranking-table td {
    position: relative;
    padding-left: 25% !important;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
  
  .ranking-table td:before {
    content: attr(data-label);
    position: absolute;
    left: 12px;
    width: 20%;
    padding-right: 10px;
    white-space: nowrap;
    font-weight: 600;
    color: var(--ion-color-medium);
    font-size: 12px;
    text-transform: uppercase;
  }
  
  /* Ajustes específicos para cada celda */
  .position-cell {
    width: 100% !important;
    min-width: auto;
    padding: 12px !important;
    background: linear-gradient(90deg, 
      var(--ion-color-primary-tint) 0%, 
      var(--ion-color-light) 100%);
  }
  
  .position-cell:before {
    display: none;
  }
  
  .position-wrapper {
    justify-content: flex-start;
    flex-wrap: nowrap;
  }
  
  .player-cell {
    width: 100% !important;
    min-width: auto;
    padding: 12px !important;
  }
  
  .player-cell:before {
    display: none;
  }
  
  .score-cell {
    width: 100% !important;
    min-width: auto;
    text-align: left !important;
  }
  
  .score-cell:before {
    content: "Puntos: ";
  }
  
  .score-wrapper {
    justify-content: flex-start;
  }
  
  .date-cell {
    width: 100% !important;
    min-width: auto;
    text-align: left !important;
  }
  
  .date-cell:before {
    content: "Fecha: ";
  }
  
  /* Ajustes adicionales para móvil */
  .trophy-icon {
    font-size: 18px;
  }
  
  .position-number {
    font-size: 16px;
  }
  
  .current-badge {
    font-size: 9px;
    padding: 2px 6px;
  }
  
  .avatar-circle {
    width: 36px;
    height: 36px;
    font-size: 13px;
  }
  
  .username {
    font-size: 15px;
  }
  
  .player-meta {
    font-size: 11px;
  }
  
  .score {
    font-size: 16px;
  }
  
  .date {
    font-size: 13px;
  }
  
  /* Estilos especiales para filas destacadas */
  .current-user {
    border-left: none;
    border-top: 4px solid var(--ion-color-primary);
  }
  
  .top-three {
    border-left: none;
    border-top: 4px solid var(--ion-color-warning);
  }
}

/* Ajustes para pantallas muy pequeñas */
@media (max-width: 480px) {
  .ranking-table td {
    padding-left: 30% !important;
  }
  
  .ranking-table td:before {
    width: 25%;
  }
  
  .position-cell,
  .player-cell,
  .score-cell,
  .date-cell {
    padding: 10px !important;
  }
  
  .avatar-circle {
    width: 32px;
    height: 32px;
    font-size: 12px;
  }
  
  .username {
    font-size: 14px;
  }
  
  .score {
    font-size: 15px;
  }
}

/* Dark theme adjustments para móvil */
@media (max-width: 768px) {
  .dark-theme .ranking-table td:before {
    color: var(--ion-color-medium-tint);
  }
  
  .dark-theme .position-cell {
    background: linear-gradient(90deg, 
      var(--ion-color-primary-shade) 0%, 
      var(--ion-color-dark-tint) 100%);
  }
  
  .dark-theme .current-user {
    border-top-color: var(--ion-color-primary-shade);
  }
  
  .dark-theme .top-three {
    border-top-color: var(--ion-color-warning-shade);
  }
}
</style>