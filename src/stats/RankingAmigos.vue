<script setup>
import { ref, onMounted, watch } from 'vue';
import axios from 'axios';
import { 
  IonCard, IonCardHeader, IonCardTitle,
  IonCardSubtitle, IonCardContent, IonButton, IonIcon, IonSpinner
} from '@ionic/vue';
import { trophy, refresh, alertCircle, peopleOutline } from 'ionicons/icons';
import RankingRow from './RankingRow.vue';
import { useAuth } from '@/composables/useAuth';

const props = defineProps({
  usuarioActualId: {
    type: Number,
    required: true
  },
  currentUsername: {
    type: String,
    default: ''
  },
  apiBaseUrl: {
    type: String,
    required: true
  }
});

const ranking = ref([]);
const isLoading = ref(false);
const error = ref(null);

// Usar el composable de autenticación
const { getHeaders, token, usuario, isAuthenticated } = useAuth();

const cargarRankingAmigos = async () => {
  if (!props.usuarioActualId) {
    error.value = 'ID de usuario no disponible';
    return;
  }

  // Verificar autenticación
  if (!isAuthenticated.value) {
    error.value = 'No hay sesión activa. Por favor, inicia sesión nuevamente.';
    return;
  }

  isLoading.value = true;
  error.value = null;
  
  try {
    // Usar axios que ya tiene los headers configurados automáticamente
    const response = await axios.get(
      `${props.apiBaseUrl}/usuario/${props.usuarioActualId}/ranking-amigos`,
      {
        timeout: 10000, // 10 segundos de timeout
        headers: getHeaders() // Headers adicionales si son necesarios
      }
    );
    
    // Si llega aquí, la petición fue exitosa (status 200-299)
    const data = response.data;
    ranking.value = Array.isArray(data) ? data : [];
    
  } catch (err) {
    console.error('Error al cargar ranking de amigos:', err);
    
    // Manejar diferentes tipos de errores
    if (err.code === 'ECONNREFUSED' || err.code === 'ERR_NETWORK') {
      error.value = 'No se pudo conectar con el servidor. Verifica que esté funcionando.';
    } else if (err.code === 'ECONNABORTED') {
      error.value = 'La conexión tardó demasiado. Intenta de nuevo.';
    } else if (err.response) {
      const status = err.response.status;
      const data = err.response.data;
      
      if (status === 401) {
        error.value = 'Sesión expirada. Por favor, inicia sesión nuevamente.';
      } else if (status === 403) {
        error.value = 'No tienes permisos para acceder a esta información';
      } else if (status === 404) {
        error.value = 'Usuario no encontrado';
      } else if (status === 204) {
        // Sin contenido, pero no es error
        ranking.value = [];
        return;
      } else {
        error.value = typeof data === 'string' ? data : `Error del servidor: ${status}`;
      }
    } else if (err.request) {
      error.value = 'No se recibió respuesta del servidor.';
    } else {
      error.value = err.message || 'Error al cargar el ranking de amigos';
    }
    
    ranking.value = [];
  } finally {
    isLoading.value = false;
  }
};

// Cargar datos al montar y cuando cambien las dependencias
onMounted(() => {
  if (isAuthenticated.value && props.usuarioActualId) {
    cargarRankingAmigos();
  }
});

// Observar cambios en el usuario, token y autenticación
watch([() => props.usuarioActualId, isAuthenticated], ([newUserId, newAuth]) => {
  if (newUserId && newAuth) {
    cargarRankingAmigos();
  }
});

// Exponer la función para uso externo
defineExpose({
  cargarRankingAmigos
});
</script>

<template>
  <div class="ranking-container">
    <!-- Loading state -->
    <div v-if="isLoading" class="status-container loading-state">
      <div class="status-card">
        <div class="spinner-container">
          <ion-spinner name="crescent" class="custom-spinner"></ion-spinner>
        </div>
        <div class="status-content">
          <h3>Cargando ranking</h3>
          <p>Obteniendo datos de tus amigos...</p>
        </div>
      </div>
    </div>
    
    <!-- Error state -->
    <div v-else-if="error" class="status-container error-state">
      <div class="status-card error-card">
        <div class="error-icon-container">
          <ion-icon :icon="alertCircle" class="error-icon"></ion-icon>
        </div>
        <div class="status-content">
          <h3>Error al cargar ranking</h3>
          <p>{{ error }}</p>
        </div>
        <div class="action-buttons">
          <ion-button 
            @click="cargarRankingAmigos" 
            fill="clear" 
            class="retry-button"
          >
            <ion-icon :icon="refresh" slot="start"></ion-icon>
            Reintentar
          </ion-button>
        </div>
      </div>
    </div>
    
    <!-- Ranking content -->
    <ion-card v-else class="ranking-card">
      <ion-card-header>
        <ion-card-title>
          <div class="title-container">
            <ion-icon :icon="trophy" class="trophy-title-icon"></ion-icon>
            <span>Ranking de Amigos</span>
          </div>
        </ion-card-title>
        <ion-card-subtitle>
          Tu posición entre amigos ({{ ranking.length }} {{ ranking.length === 1 ? 'jugador' : 'jugadores' }})
        </ion-card-subtitle>
      </ion-card-header>
      
      <ion-card-content>
        <!-- Empty state -->
        <div v-if="ranking.length === 0" class="empty-state">
          <div class="empty-icon">
            <ion-icon :icon="peopleOutline" class="empty-icon-svg"></ion-icon>
          </div>
          <h3>No hay datos de ranking</h3>
          <p>Agrega amigos y juega partidas para ver el ranking</p>
        </div>
        
        <!-- Ranking table -->
        <div v-else>
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
          
          <!-- Refresh button -->
          <div class="refresh-container">
            <ion-button 
              fill="clear" 
              size="small"
              @click="cargarRankingAmigos"
              class="refresh-button"
            >
              <ion-icon :icon="refresh" slot="start"></ion-icon>
              Actualizar ranking
            </ion-button>
          </div>
        </div>
      </ion-card-content>
    </ion-card>
  </div>
</template>

<style scoped>
.ranking-container {
  width: 100%;
}

.title-container {
  display: flex;
  align-items: center;
  gap: 8px;
}

.trophy-title-icon {
  font-size: 24px;
  color: var(--ion-color-warning);
}

.refresh-container {
  margin-top: 16px;
  text-align: center;
  padding-top: 16px;
  border-top: 1px solid var(--ion-color-light-shade);
}

.refresh-button {
  --color: var(--ion-color-medium);
  font-size: 0.9rem;
}

.refresh-button:hover {
  --color: var(--ion-color-primary);
}

.status-container {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 300px;
  padding: 24px;
}

.status-card {
  background: white;
  border-radius: 16px;
  padding: 32px 24px;
  text-align: center;
  max-width: 400px;
  width: 100%;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}

.loading-state .status-card {
  border: 2px solid rgba(102, 126, 234, 0.1);
}

.error-card {
  border: 2px solid rgba(244, 67, 54, 0.1);
}

.spinner-container {
  margin-bottom: 20px;
}

.custom-spinner {
  --color: #667eea;
  width: 40px;
  height: 40px;
}

.error-icon-container {
  margin-bottom: 20px;
}

.error-icon {
  font-size: 40px;
  color: #f44336;
}

.status-content h3 {
  margin: 0 0 8px 0;
  font-size: 1.2rem;
  font-weight: 600;
  color: #2c3e50;
}

.status-content p {
  margin: 0 0 20px 0;
  color: #718096;
  line-height: 1.4;
  font-size: 0.9rem;
}

.action-buttons {
  margin-top: 20px;
}

.retry-button {
  --background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  --color: white;
  --border-radius: 10px;
  --padding: 10px 20px;
  font-weight: 600;
  text-transform: none;
  font-size: 0.9rem;
}

.empty-state {
  text-align: center;
  padding: 40px 20px;
  color: var(--ion-color-medium);
}

.empty-icon {
  margin-bottom: 16px;
}

.empty-icon-svg {
  font-size: 48px;
  color: var(--ion-color-medium-tint);
}

.empty-state h3 {
  margin: 0 0 8px 0;
  font-size: 1.1rem;
  font-weight: 600;
  color: var(--ion-color-medium-shade);
}

.empty-state p {
  margin: 0;
  font-size: 0.9rem;
  line-height: 1.4;
}

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

.ranking-table th {
  padding: 14px 12px;
  font-size: 14px;
  font-weight: 600;
  color: var(--ion-color-medium-shade);
  text-align: left;
  border-bottom: 2px solid var(--ion-color-light-shade);
  background-color: var(--ion-color-light-shade);
}

.ranking-table th.position-header {
  text-align: center;
  width: 100px;
}

.ranking-table th.player-header {
  width: 50%;
}

.ranking-table th.score-header {
  width: 140px;
  text-align: center;
}

.ranking-table th.date-header {
  width: 130px;
  text-align: center;
}

/* Dark theme adjustments */
:deep(.dark-theme) .status-card {
  background: var(--ion-color-dark);
}

:deep(.dark-theme) .status-content h3 {
  color: var(--ion-color-light);
}

:deep(.dark-theme) .status-content p {
  color: var(--ion-color-medium-tint);
}

:deep(.dark-theme) .empty-state {
  color: var(--ion-color-medium-tint);
}

:deep(.dark-theme) .empty-state h3 {
  color: var(--ion-color-light);
}

:deep(.dark-theme) .ranking-table {
  background-color: var(--ion-color-dark);
}

:deep(.dark-theme) .ranking-table th {
  color: var(--ion-color-light);
  border-bottom-color: var(--ion-color-dark-shade);
  background-color: var(--ion-color-dark-shade);
}

/* Media queries for responsive design */
@media (max-width: 768px) {
  .ranking-table {
    min-width: auto;
  }
  
  .ranking-table thead {
    display: none;
  }
  
  .ranking-table tbody tr {
    display: block;
    margin-bottom: 16px;
    border-bottom: 2px solid var(--ion-color-light-shade);
  }
  
  .ranking-table td {
    display: block;
    text-align: right;
    padding-left: 50% !important;
    position: relative;
    border-bottom: 1px solid var(--ion-color-light-shade);
    padding: 12px !important;
  }
  
  .ranking-table td:before {
    content: attr(data-label);
    position: absolute;
    left: 12px;
    width: 45%;
    padding-right: 10px;
    text-align: left;
    font-weight: 600;
    color: var(--ion-color-medium);
  }
  
  .position-cell {
    width: 100% !important;
    min-width: auto;
    text-align: left !important;
  }
  
  .position-cell:before {
    content: "Posición: ";
  }
  
  .position-wrapper {
    justify-content: flex-start;
  }
  
  .player-cell {
    width: 100% !important;
    min-width: auto;
  }
  
  .player-cell:before {
    content: "Jugador: ";
  }
  
  .player-info {
    justify-content: flex-end;
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
  
  .player-info {
    gap: 8px;
  }
  
  .score {
    font-size: 15px;
  }
}

/* Dark theme adjustments para móvil */
@media (max-width: 768px) {
  :deep(.dark-theme) .ranking-table td:before {
    color: var(--ion-color-medium-tint);
  }
  
  :deep(.dark-theme) .ranking-table tbody tr {
    border-bottom-color: var(--ion-color-dark-shade);
  }
  
  :deep(.dark-theme) .ranking-table td {
    border-bottom-color: var(--ion-color-dark-shade);
  }
  
  :deep(.dark-theme) .current-user {
    border-top-color: var(--ion-color-primary-shade);
  }
  
  :deep(.dark-theme) .top-three {
    border-top-color: var(--ion-color-warning-shade);
  }
  }
  </style>