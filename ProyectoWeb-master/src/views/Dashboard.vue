<template>
  <ion-page>
    <ion-header>
      <ion-toolbar color="primary" class="custom-toolbar">
        <ion-title>Estadísticas de Juego</ion-title>
        <ion-buttons slot="end">
          <ion-button @click="toggleDarkMode" color="light">
            <ion-icon :icon="isDarkMode ? sunnyOutline : moonOutline"></ion-icon>
          </ion-button>
          <ion-button @click="logout" color="light">
            <ion-icon :icon="logOutOutline"></ion-icon>
            <span class="logout-text">Cerrar Sesión</span>
          </ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>

    <ion-content class="ion-padding" :class="{ 'dark-theme': isDarkMode }">
      <div v-if="isLoading" class="loading-container">
        <ion-spinner name="crescent"></ion-spinner>
        <p>Cargando estadísticas...</p>
      </div>

      <div v-else>
        <div class="welcome-container">
          <div class="logo-container">
            <div class="app-logo">GS</div>
          </div>
          <h1>Bienvenido, {{ userStats.username }}</h1>
          <p class="subtitle">Consulta tus estadísticas y compáralas con tus amigos</p>
        </div>
        
        <ion-segment v-model="selectedTab" class="tab-segment">
          <ion-segment-button value="personal">
            <ion-label >Mis Estadísticas</ion-label>
          </ion-segment-button>
          <ion-segment-button value="friends">
            <ion-label>Ranking Amigos</ion-label>
          </ion-segment-button>
          <ion-segment-button value="skins">
            <ion-label>Skins</ion-label>
          </ion-segment-button>
        </ion-segment>

        <!-- Tab de selección de skin -->
        <div v-if="selectedTab === 'skins'" class="skin-selection-container">
          <h2 class="section-title">Selecciona tu Skin</h2>

          <!-- Grupo de radios -->
          <ion-radio-group v-model="selectedSkin" class="skin-radio-group">

            <div class="skin-options">
              <!-- Opción Azul -->
              <div class="skin-option">
                <div class="skin-rectangle skin-blue" :class="{ 'selected': selectedSkin === 'blue' }">
                  <img src="/src/assets/Sonic.gif" alt="Skin Azul" class="skin-image">
                </div>
                <ion-item lines="none" class="radio-item">
                  <ion-radio value="blue" class="skin-radio">Azul</ion-radio>
                </ion-item>
              </div>

              <!-- Opción Roja -->
              <div class="skin-option">
                <div class="skin-rectangle skin-red" :class="{ 'selected': selectedSkin === 'red' }">
                  <img src="/src/assets/Mario.gif" alt="Skin Rojo" class="skin-image">
                </div>
                <ion-item lines="none" class="radio-item">
                  <ion-radio value="red" class="skin-radio">Rojo</ion-radio>
                </ion-item>
              </div>
            </div>

          </ion-radio-group>

          <ion-button expand="block" class="apply-skin-button" @click="applySkinChange">
            Aplicar Cambios
          </ion-button>
        </div>

        <!-- Tab de estadísticas personales -->
        <div v-if="selectedTab === 'personal'" class="stats-container">
          <ion-card class="highlight-card">
            <ion-card-header>
              <ion-card-subtitle>Tu mejor registro</ion-card-subtitle>
              <ion-card-title class="max-score">{{ userStats.maxScore }}</ion-card-title>
            </ion-card-header>
            <ion-card-content>
              <div class="score-details">
                <div class="score-item">
                  <ion-icon :icon="trophyOutline" class="score-icon"></ion-icon>
                  <div class="score-text">
                    <span class="score-label">Puntuación máxima</span>
                    <span class="score-date">{{ formatDate(userStats.maxScoreDate) }}</span>
                  </div>
                </div>
              </div>
            </ion-card-content>
          </ion-card>

          <h2 class="section-title">Últimas partidas</h2>

          <ion-card v-for="(game, index) in userStats.recentGames" :key="index" class="game-card">
            <ion-card-content>
              <div class="game-info">
                <div class="game-date">{{ formatDate(game.date) }}</div>
                <div class="game-score">{{ game.score }} pts</div>
                <div class="game-time">{{ formatTime(game.time) }}</div>
              </div>
            </ion-card-content>
          </ion-card>

          <div class="stats-summary">
            <ion-card class="summary-card">
              <ion-card-content>
                <div class="summary-item">
                  <div class="summary-value">{{ userStats.totalGames }}</div>
                  <div class="summary-label">Partidas jugadas</div>
                </div>
              </ion-card-content>
            </ion-card>

            <ion-card class="summary-card">
              <ion-card-content>
                <div class="summary-item">
                  <div class="summary-value">{{ userStats.averageScore }}</div>
                  <div class="summary-label">Puntuación media</div>
                </div>
              </ion-card-content>
            </ion-card>

            <ion-card class="summary-card">
              <ion-card-content>
                <div class="summary-item">
                  <div class="summary-value">{{ userStats.rank }}</div>
                  <div class="summary-label">Ranking global</div>
                </div>
              </ion-card-content>
            </ion-card>
          </div>
        </div>

        <!-- Tab de ranking de amigos -->
        <div v-if="selectedTab === 'friends'" class="friends-container">
          <div v-if="isLoadingFriends" class="loading-container">
            <ion-spinner name="crescent"></ion-spinner>
            <p>Cargando amigos...</p>
          </div>

          <div v-else-if="friendsError" class="error-container">
            <p class="error-message">{{ friendsError }}</p>
            <ion-button @click="fetchFriendsData" color="primary">
              Reintentar
            </ion-button>
          </div>

          <ion-card v-else class="ranking-card">
            <ion-card-header>
              <ion-card-title>Ranking de Amigos</ion-card-title>
              <ion-card-subtitle>Lista de amigos ({{ friendsRanking.length }} amigos encontrados)</ion-card-subtitle>
            </ion-card-header>

            <ion-card-content>
              <div v-if="friendsRanking.length === 0" class="no-friends-message">
                <p>No tienes amigos agregados aún.</p>
              </div>

              <div v-else>
                <!-- Versión para pantallas grandes (tablas) -->
                <table class="ranking-table desktop-only">
                  <thead>
                    <tr>
                      <th>Posición</th>
                      <th>Jugador</th>
                      <th>Skin</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="(friend, index) in friendsRanking" :key="index">
                      <td class="rank-cell">
                        <span class="rank-number" :class="getRankClass(index)">{{ index + 1 }}</span>
                      </td>
                      <td class="player-cell">
                        <div class="player-info">
                          <div class="player-avatar">
                            {{ getInitials(friend.username) }}
                          </div>
                          <span class="player-name">{{ friend.username }}</span>
                        </div>
                      </td>
                      <td class="skin-cell">{{ friend.skin || 'Sin skin' }}</td>
                    </tr>
                  </tbody>
                </table>

                <!-- Versión para móviles (tarjetas) -->
                <div class="mobile-only">
                  <div class="mobile-ranking-item" v-for="(friend, index) in friendsRanking" :key="index">
                    <div class="mobile-ranking-header">
                      <div class="mobile-rank">
                        <span class="rank-number" :class="getRankClass(index)">{{ index + 1 }}</span>
                      </div>
                      <div class="mobile-player-info">
                        <div class="player-avatar">
                          {{ getInitials(friend.username) }}
                        </div>
                        <div class="mobile-player-details">
                          <span class="player-name">{{ friend.username }}</span>
                          <span class="mobile-skin">{{ friend.skin || 'Sin skin' }}</span>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </ion-card-content>
          </ion-card>
        </div>
      </div>
    </ion-content>
  </ion-page>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import axios from 'axios';
import {
  IonPage,
  IonHeader,
  IonToolbar,
  IonTitle,
  IonButtons,
  IonButton,
  IonIcon,
  IonContent,
  IonCard,
  IonCardHeader,
  IonCardTitle,
  IonCardSubtitle,
  IonCardContent,
  IonSpinner,
  IonSegment,
  IonSegmentButton,
  IonLabel,
  IonRadioGroup,
  IonRadio,
  IonItem,
  toastController
} from '@ionic/vue';
import { logOutOutline, trophyOutline, sunnyOutline, moonOutline } from 'ionicons/icons';

// Configuración de la API
const API_BASE_URL = 'http://192.168.1.234:8090/GameConnect';
const usuarioStr = localStorage.getItem('usuario'); // obtiene el string JSON
let usuario = null;

if (usuarioStr) {
  usuario = JSON.parse(usuarioStr); // convierte a objeto JS
}

// Ahora puedes acceder:
let USER_ID = null;
let USERNAME = null;


// Estado reactivo
const router = useRouter();
const isLoading = ref(true);
const isLoadingFriends = ref(false);
const error = ref(null);
const friendsError = ref(null);
const isDarkMode = ref(window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches);
const selectedSkin = ref('blue');

// Datos del usuario actual
const userStats = ref(null);

// Datos de amigos (ahora se cargarán desde la API)
const friendsRanking = ref([]);

const selectedTab = ref('personal');

// Función para obtener amigos desde la API
const fetchFriendsData = async () => {
  isLoadingFriends.value = true;
  friendsError.value = null;

  try {
    const token = localStorage.getItem('token');
    if (!token) throw new Error('Token de sesión no encontrado');

    console.log(`Intentando cargar amigos desde: ${API_BASE_URL}/usuario/${USER_ID}/amigos`);

    const response = await axios.get(`${API_BASE_URL}/usuario/${USER_ID}/amigos`, {
      timeout: 10000, // 10 segundos de timeout
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}` // ← Aquí se añade el token
      }
    });

    console.log('Respuesta de la API:', response.data);

    if (Array.isArray(response.data)) {
      friendsRanking.value = response.data;

      const toast = await toastController.create({
        message: `Se cargaron ${response.data.length} amigos correctamente`,
        duration: 2000,
        position: 'bottom',
        color: 'success'
      });
      await toast.present();
    } else {
      throw new Error('La respuesta no es un array válido');
    }

  } catch (err) {
    console.error('Error al cargar amigos:', err);

    let errorMessage = 'Error desconocido';

    if (err.code === 'ECONNREFUSED' || err.code === 'ERR_NETWORK') {
      errorMessage = 'No se pudo conectar con el servidor. Verifica que esté funcionando.';
    } else if (err.code === 'ECONNABORTED') {
      errorMessage = 'La conexión tardó demasiado. Intenta de nuevo.';
    } else if (err.response) {
      errorMessage = `Error del servidor: ${err.response.status} - ${err.response.statusText}`;
    } else if (err.request) {
      errorMessage = 'No se recibió respuesta del servidor.';
    } else {
      errorMessage = err.message || 'Error al procesar la solicitud';
    }

    friendsError.value = errorMessage;

    const toast = await toastController.create({
      message: errorMessage,
      duration: 4000,
      position: 'bottom',
      color: 'danger'
    });
    await toast.present();

  } finally {
    isLoadingFriends.value = false;
  }
};


// Función para alternar modo oscuro
const toggleDarkMode = () => {
  isDarkMode.value = !isDarkMode.value;
};

// Detectar cambios en el modo oscuro/claro del sistema
const setupDarkModeListener = () => {
  if (window.matchMedia) {
    const darkModeQuery = window.matchMedia('(prefers-color-scheme: dark)');

    const updateDarkMode = (event) => {
      isDarkMode.value = event.matches;
    };

    if (darkModeQuery.addEventListener) {
      darkModeQuery.addEventListener('change', updateDarkMode);
    } else if (darkModeQuery.addListener) {
      darkModeQuery.addListener(updateDarkMode);
    }
  }
};

// Función para aplicar cambios de skin
const applySkinChange = async () => {
  try {
    const toast = await toastController.create({
      message: `Skin ${selectedSkin.value === 'blue' ? 'Azul' : 'Rojo'} aplicado correctamente`,
      duration: 2000,
      position: 'bottom',
      color: 'success'
    });
    await toast.present();
    
    console.log(`Skin seleccionado: ${selectedSkin.value}`);
  } catch (error) {
    console.error('Error al aplicar skin:', error);
  }
};

// Función para formatear fechas
const formatDate = (dateString) => {
  if (!dateString) return 'N/A';
  const date = new Date(dateString);
  return date.toLocaleDateString('es-ES', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric'
  });
};

// Función para formatear tiempos
const formatTime = (seconds) => {
  if (!seconds && seconds !== 0) return 'N/A';

  const mins = Math.floor(seconds / 60);
  const secs = seconds % 60;

  return `${mins}:${secs.toString().padStart(2, '0')}`;
};

// Obtener iniciales del nombre de usuario
const getInitials = (username) => {
  if (!username) return '?';
  return username.substring(0, 2).toUpperCase();
};

// Obtener clase CSS según posición en el ranking
const getRankClass = (index) => {
  if (index === 0) return 'rank-first';
  if (index === 1) return 'rank-second';
  if (index === 2) return 'rank-third';
  return '';
};

// Cargar estadísticas del usuario
const fetchUserStats = async () => {
  isLoading.value = true;
  error.value = null;

  try {
    const sessionId = localStorage.getItem('token');

    if (!sessionId) {
      console.warn('No hay sesión activa, usando datos de demostración');
    }

    setTimeout(() => {
      isLoading.value = false;
    }, 1000);

  } catch (err) {
    console.error('Error inesperado:', err);
    isLoading.value = false;
  }
};

// Función para cerrar sesión
const logout = async () => {
  try {
    localStorage.removeItem('token');
    localStorage.removeItem('usuario');

    delete axios.defaults.headers.common['Authorization'];

    const toast = await toastController.create({
      message: 'Sesión cerrada correctamente',
      duration: 2000,
      position: 'bottom',
      color: 'success'
    });
    await toast.present();

    router.push('/');
  } catch (error) {
    console.error('Error al cerrar sesión:', error);

    localStorage.clear();
    delete axios.defaults.headers.common['Authorization'];
    router.push('/');
  }
};

// Cargar los datos cuando el componente se monte
onMounted(() => {
   // Lee usuario aquí, cuando ya está montado
  const usuarioStr = localStorage.getItem('usuario');

  if (usuarioStr) {
    const usuario = JSON.parse(usuarioStr);
    USER_ID = usuario.usuarioId;
    USERNAME = usuario.username;

    userStats.value = {
      id: usuario.usuarioId,
      username: usuario.username,
      maxScore: 0,
      totalGames: 0,
      rank: 0,
      recentGames: []
    };

    fetchFriendsData(); // ahora sí puede usar USER_ID
  } else {
    console.warn('No se encontró usuario en localStorage');
    router.push('/'); // o redirige a login
  }

  setupDarkModeListener();
  fetchUserStats(); // si depende del token, asegúrate de que también se lea aquí si hace falta
});
</script>

<style>
/* Variables CSS globales para toda la aplicación */
:root {
  /* Colores primarios del gradiente */
  --primary-dark: #1a2a6c;
  --primary-medium: #b21f1f;
  --primary-light: #fdbb2d;
  --accent-color: #ec8c3d;
  
  /* Colores para el esquema visual */
  --primary-color: #1a2a6c;
  --secondary-color: #b21f1f;
  
  /* Colores de texto */
  --text-primary: #333;
  --text-secondary: #666;
  --text-light: #f5f5f5;
  
  /* Colores de fondo */
  --bg-primary: #f5f5f5;
  --bg-secondary: #e9e9e9;
  --highlight-bg: rgba(26, 42, 108, 0.05);
  --card-bg: #ffffff;
}

/* Estilos para modo oscuro */
.dark-theme {
  --primary-color: #3a4db0;
  --secondary-color: #e04141;
  --accent-color: #f5a045;

  --text-primary: #f5f5f5;
  --text-secondary: #cccccc;
  --text-light: #f5f5f5;

  --bg-primary: #1e1e1e;
  --bg-secondary: #2d2d2d;
  --card-bg: #2d2d2d;
  --highlight-bg: rgba(58, 77, 176, 0.15);
}

/* Estilos generales */
ion-content {
  --background: var(--bg-primary);
  --color: var(--text-primary);
}

.custom-toolbar {
  --background: linear-gradient(90deg, var(--primary-dark), var(--primary-medium), var(--accent-color));
  --color: white;
}

.logout-text {
  margin-left: 5px;
}

@media (max-width: 576px) {
  .logout-text {
    display: none;
  }
}

ion-card {
  background-color: var(--card-bg);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  border-radius: 10px;
  overflow: hidden;
  color: var(--text-primary);
}

h1 {
  font-size: 24px;
  font-weight: 700;
  margin-bottom: 8px;
  color: var(--text-primary);
}

.subtitle {
  color: var(--text-secondary);
  margin-top: 0;
  margin-bottom: 24px;
}

.section-title {
  font-size: 20px;
  font-weight: 600;
  margin: 20px 0 12px;
  color: var(--text-primary);
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 300px;
  color: var(--text-secondary);
}

.welcome-container {
  text-align: center;
  margin-bottom: 24px;
}

.logo-container {
  display: flex;
  justify-content: center;
  margin-bottom: 12px;
}

.app-logo {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--primary-dark), var(--primary-medium));
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: 800;
}

/* Estilos de las pestañas */
.tab-segment {
  margin-bottom: 24px;
  --background: var(--bg-secondary);
  border-radius: 8px;
  overflow: hidden;
}

/* Asegurar que el texto de las pestañas sea visible */
.segment-label {
  color: var(--text-primary);
  font-weight: 500;
}

ion-segment-button {
  --color-checked: var(--text-light);
  --color: var(--text-primary);
  --background-checked: var(--primary-color);
}

/* Estilos de las tarjetas */
.highlight-card {
  background: linear-gradient(135deg, var(--primary-dark), var(--primary-medium), var(--primary-light));
  color: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.15);
}

.highlight-card ion-card-title {
  font-size: 36px;
  font-weight: 700;
  color: white;
}

.highlight-card ion-card-subtitle {
  color: rgba(255, 255, 255, 0.8);
  font-size: 16px;
}

.max-score {
  font-size: 48px !important;
  font-weight: 800 !important;
  color: white;
}

.score-details {
  margin-top: 12px;
}

.score-item {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}

.score-icon {
  font-size: 24px;
  margin-right: 12px;
}

.score-text {
  display: flex;
  flex-direction: column;
}

.score-label {
  font-size: 14px;
  font-weight: 500;
}

.score-date {
  font-size: 12px;
  opacity: 0.8;
}

.game-card {
  border-radius: 10px;
  margin-bottom: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  background-color: var(--card-bg);
}

.game-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.game-date {
  font-size: 14px;
  color: var(--text-secondary);
}

.game-score {
  font-size: 18px;
  font-weight: 600;
  color: var(--primary-color);
}

.game-time {
  font-size: 14px;
  background-color: var(--bg-secondary);
  padding: 4px 8px;
  border-radius: 12px;
  color: var(--text-secondary);
}

.stats-summary {
  display: flex;
  justify-content: space-between;
  margin-top: 24px;
  gap: 12px;
}

@media (max-width: 576px) {
  .stats-summary {
    flex-direction: column;
  }
}

.summary-card {
  flex: 1;
  text-align: center;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  background-color: var(--card-bg);
}

.summary-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.summary-value {
  font-size: 24px;
  font-weight: 700;
  color: var(--primary-color);
}

.summary-label {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 4px;
}

/* Estilos para el ranking de amigos */
.ranking-card {
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  background-color: var(--card-bg);
}

.ranking-table {
  width: 100%;
  border-collapse: collapse;
  color: var(--text-primary);
}

.ranking-table th {
  text-align: left;
  padding: 12px;
  border-bottom: 1px solid var(--bg-secondary);
  font-size: 14px;
  color: var(--text-secondary);
}

.ranking-table td {
  padding: 12px;
  border-bottom: 1px solid var(--bg-secondary);
  color: var(--text-primary);
}

.rank-cell {
  width: 70px;
}

.rank-number {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  background-color: var(--bg-secondary);
  border-radius: 50%;
  font-weight: 600;
  color: var(--text-primary);
}

/* Colores de medal para ranking */
.rank-first {
  background-color: #ffd700;
  color: #333 !important;
  font-weight: 700;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.rank-second {
  background-color: #c0c0c0;
  color: #333 !important;
  font-weight: 700;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.rank-third {
  background-color: #cd7f32;
  color: #fff !important;
  font-weight: 700;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.player-cell {
  width: 40%;
}

.player-info {
  display: flex;
  align-items: center;
}

.player-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background-color: var(--primary-color);
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  margin-right: 12px;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.current-user-avatar {
  background: linear-gradient(135deg, var(--secondary-color), var(--accent-color));
  color: #ffffff;
}

.player-name {
  font-weight: 500;
  color: var(--text-primary);
  display: flex;
  align-items: center;
}

.current-user-badge {
  background-color: var(--accent-color);
  color: white;
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 10px;
  margin-left: 8px;
}

.score-cell {
  font-weight: 600;
  color: var(--primary-color);
  font-size: 16px;
}

.date-cell {
  color: var(--text-secondary);
  font-size: 14px;
}

.current-user {
  background-color: var(--highlight-bg);
  font-weight: 600;
}

/* Distribución diferente para móviles y desktop */
.mobile-only {
  display: none;
}

.desktop-only {
  display: table;
}

/* Media query para cambiar entre vistas móvil y desktop */
@media (max-width: 768px) {
  .mobile-only {
    display: block;
  }
  
  .desktop-only {
    display: none;
  }
}

/* Estilos específicos para vista móvil de ranking */
.mobile-ranking-item {
  border-bottom: 1px solid var(--bg-secondary);
  padding: 12px 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: var(--text-primary);
}

.mobile-ranking-header {
  display: flex;
  align-items: center;
}

.mobile-rank {
  margin-right: 12px;
}

.mobile-player-info {
  display: flex;
  align-items: center;
}

.mobile-player-details {
  display: flex;
  flex-direction: column;
}

.mobile-date {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 4px;
}

.mobile-score {
  font-weight: 600;
  font-size: 16px;
  color: var(--primary-color);
}

/* Estilos para la selección de skin */
.skin-selection-container {
  text-align: center;
  margin: 24px 0;
  padding: 16px;
  background-color: var(--card-bg);
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.skin-options {
  display: flex;
  justify-content: center;
  gap: 30px;
  margin-top: 20px;
  flex-wrap: wrap;
}

.skin-option {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.skin-rectangle {
  width: 100px;
  height: 120px;
  border-radius: 10px;
  margin-bottom: 10px;
  box-shadow: 0 0 10px rgba(0, 0, 0, 0.2);
  transition: transform 0.3s, box-shadow 0.3s;
  overflow: hidden; /* Para contener la imagen dentro del rectángulo */
  display: flex;
  justify-content: center;
  align-items: center;
}

.skin-blue {
  background-color: #1a2a6c;
}

.skin-red {
  background-color: #b21f1f;
}

.skin-rectangle.selected {
  outline: 3px solid #ffc107;
  transform: scale(1.05);
  box-shadow: 0 0 15px rgba(255, 193, 7, 0.6);
}

.skin-radio {
  --color: var(--text-primary);
}

.radio-item {
  --background: transparent;
  --color: var(--text-primary);
}

.skin-image {
  width: 80%; /* Ajustar imagen al contenedor */
  height: auto;
  object-fit: contain;
  max-height: 110px;
}

.apply-skin-button {
  margin-top: 16px;
  --background: var(--primary-color);
  --color: var(--text-light);
  --border-radius: 8px;
  --padding-top: 10px;
  --padding-bottom: 10px;
  --padding-start: 20px;
  --padding-end: 20px;
  font-weight: 600;
}
.dark-theme .rank-first {
  background-color: #ffd700 !important;
  color: #333 !important;
}

.dark-theme .rank-second {
  background-color: #c0c0c0 !important;
  color: #333 !important;
}

.dark-theme .rank-third {
  background-color: #cd7f32 !important;
  color: #fff !important;
}

.dark-theme .score-cell {
  color: var(--accent-color);
}

.dark-theme .rank-number {
  background-color: #444;
}

.dark-theme .highlight-card {
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.3);
}
</style>