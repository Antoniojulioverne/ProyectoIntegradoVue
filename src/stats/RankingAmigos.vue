<script setup>
import { ref, onMounted, watch } from 'vue';
import axios from 'axios';
import { 
  IonCard, IonCardHeader, IonCardTitle,
  IonCardSubtitle, IonCardContent, IonButton, IonIcon, IonSpinner
} from '@ionic/vue';
import { trophy, refresh, alertCircle, peopleOutline } from 'ionicons/icons';
import RankingRow from './RankingRow.vue'; // Ajusta la ruta según tu estructura
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

<!-- El template es exactamente el mismo que en la versión anterior -->
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

<!-- Los estilos son exactamente los mismos -->
<style scoped>
/* Todos los estilos anteriores... */
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

/* Resto de estilos iguales que en la versión anterior... */
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

/* El resto de los estilos responsive y de tabla son iguales... */
</style>