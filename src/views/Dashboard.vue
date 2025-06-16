<template>
  <ion-page>
    <ion-header class="custom-header">
      <ion-toolbar class="custom-toolbar">
        <ion-buttons slot="start">
          <ion-menu-button auto-hide="false"></ion-menu-button>
        </ion-buttons>
        <ion-title class="page-title">
          <div class="title-container">
            <span>Estadísticas</span>
          </div>
        </ion-title>
      </ion-toolbar>
    </ion-header>
    
    <ion-content class="ion-padding" :class="{ 'dark-theme': isDarkMode }">
      <!-- *** AGREGAR ION-REFRESHER *** -->
      <ion-refresher slot="fixed" @ionRefresh="handleRefresh($event)">
        <ion-refresher-content
          pulling-icon="stats-chart-outline"
          pulling-text="Desliza para actualizar estadísticas"
          refreshing-spinner="circular"
          refreshing-text="Actualizando..."
        ></ion-refresher-content>
      </ion-refresher>
      <!-- Loading State -->
      <div v-if="isLoading" class="loading-container">
        <ion-spinner name="crescent"></ion-spinner>
        <p>Cargando estadísticas...</p>
      </div>

      <div v-else>
        <!-- Header Section -->
        <div class="welcome-container">
          <div class="profile-header">
            <div class="profile-avatar">
              <!-- Foto de perfil -->
               <ProfileAvatar
              :profile-image="userStats.fotoPerfil"
              :username="userStats.username"
              :size="80"
             
            />
            </div>
            
            <div class="profile-info">
              <h1>Bienvenido, {{ userStats.username }}</h1>
              <p class="subtitle">Consulta tus estadísticas y compáralas con otros jugadores</p>
              
              <!-- Stats rápidas -->
              <div class="quick-stats">
                <div class="stat-item">
                  <ion-icon name="medal" class="stat-icon"></ion-icon>
                  <span class="stat-value">#{{ userStats.rank || '?' }}</span>
                  <span class="stat-label">Ranking</span>
                </div>
                <div class="stat-item">
                  <ion-icon name="trophy" class="stat-icon"></ion-icon>
                  <span class="stat-value">{{ userStats.maxScore || 0 }}</span>
                  <span class="stat-label">Puntuacion Maxima</span>
                </div>
              
                <div class="stat-item">
                  <ion-icon name="medal" class="stat-icon"></ion-icon>
                  <span class="stat-value">{{ userStats.averageScore || '?' }}</span>
                  <span class="stat-label">Puntuacion Media</span>
                </div>
                <div class="stat-item">
                  <ion-icon name="medal" class="stat-icon"></ion-icon>
                  <span class="stat-value">{{ userStats.totalGames || '?' }}</span>
                  <span class="stat-label">Partidas Jugadas</span>
                </div>
              </div>
            </div>
          </div>
        </div>
        
        <!-- Tab Navigation -->
        <ion-segment v-model="selectedTab" class="tab-segment">
          <ion-segment-button value="personal">
            <ion-label>Mis Estadísticas</ion-label>
          </ion-segment-button>
          <ion-segment-button value="ranking">
            <ion-label>Ranking Global</ion-label>
          </ion-segment-button>
        </ion-segment>

        <!-- Personal Stats Tab -->
        <PersonalStats
          v-if="selectedTab === 'personal'"
          :key="userStats.username || 'loading'"
          :user-stats="userStats"
          :is-loading-games="isLoadingGames"
          :games-error="gamesError"
          @retry-games="fetchUserGames"
        />

        <!-- Ranking Tab -->
        <GlobalRanking
          v-if="selectedTab === 'ranking'"
          :ranking="globalRanking"
          :current-username="userStats.username"
          :is-loading="isLoadingRanking"
          :error="rankingError"
          @retry="fetchRankingData"
        />
      </div>
    </ion-content>
  </ion-page>
</template>

<script setup>

import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import {
  IonPage, IonHeader, IonToolbar, IonTitle, IonButtons, IonButton,
  IonIcon, IonContent, IonSpinner, IonSegment, IonSegmentButton,
  IonLabel, toastController,IonMenuButton,IonRefresher,IonRefresherContent
} from '@ionic/vue';
import ProfileAvatar from '../ui/ProfileAvatar.vue';
import { useApi } from '../composables/useApi';
import { useAuth } from '../composables/useAuth';
import { useTheme } from '../composables/useTheme';
import PersonalStats from './PersonalStats.vue';
import GlobalRanking from '../stats/GlobalRanking.vue';

// Composables
const router = useRouter();
const { fetchUserGames, fetchUserStats, fetchRankingData
 } = useApi();
const { userStats, isAuthenticated, initializeUser, updateUserStats, updateUserGames, updateUserRank } = useAuth();
const { isDarkMode } = useTheme();

const handleRefresh = async (event) => {
  try {
    console.log('🔄 Actualizando Dashboard...')
    
    // Recargar estadísticas del usuario
    await loadUserStats()
    
    // Mostrar toast de confirmación
    const toast = await toastController.create({
      message: '✅ Estadísticas actualizadas',
      duration: 2000,
      position: 'top',
      color: 'success'
    })
    await toast.present()
    
  } catch (error) {
    console.error('❌ Error actualizando Dashboard:', error)
    
    const toast = await toastController.create({
      message: '❌ Error al actualizar estadísticas',
      duration: 3000,
      position: 'top',
      color: 'danger'
    })
    await toast.present()
    
  } finally {
    // *** IMPORTANTE: Completar el refresher ***
    event.target.complete()
  }
}



// State
const isLoading = ref(true);
const isLoadingRanking = ref(false);
const isLoadingGames = ref(false);
const rankingError = ref(null);
const gamesError = ref(null);
const globalRanking = ref([]);
const selectedTab = ref('personal');

const clearLocalState = () => {
  isLoading.value = true;
  isLoadingRanking.value = false;
  isLoadingGames.value = false;
  rankingError.value = null;
  gamesError.value = null;
  globalRanking.value = [];
  selectedTab.value = 'personal';
};

const loadUserStats = async () => {
  isLoading.value = true;
  
  try {
    // 1. Verificar autenticación
    if (!isAuthenticated.value) {
      console.error('Usuario no autenticado');
      router.push('/');
      return;
    }

      // Limpiar el estado local ANTES del logout
    clearLocalState();

    // 2. Inicializar usuario si no está inicializado
    const usuario = initializeUser();
    if (!usuario || !usuario.usuarioId) {
      console.error('No se pudo obtener los datos del usuario del localStorage');
      
      const toast = await toastController.create({
        message: 'Error: No se encontraron datos de usuario. Inicia sesión nuevamente.',
        duration: 3000,
        position: 'bottom',
        color: 'danger'
      });
      await toast.present();
      
      router.push('/');
      return;
    }

    console.log('Usuario inicializado:', usuario);

    const userId = usuario.usuarioId;

    // 4. Cargar datos de forma secuencial para mejor control de errores
    console.log('Cargando datos para usuario ID:', userId);
 console.log('🔍 Usuario actual:', {
    username: userStats.value.username,
    fotoPerfil: userStats.value.fotoPerfil ? 'SÍ ✅' : 'NO ❌',
    fotoLength: userStats.value.fotoPerfil?.length || 0,
    emailVerificado: userStats.value.emailVerificado
  });
    // Cargar puntos y monedas
    try {
      const { totalPoints, totalCoins } = await fetchUserStats(userId);
      updateUserStats({ totalPoints, totalCoins });
      console.log('Puntos y monedas cargados:', { totalPoints, totalCoins });
    } catch (error) {
      console.error('Error cargando puntos y monedas:', error);
      // Continuar con los otros datos
    }

    // Cargar juegos
    try {
      isLoadingGames.value = true;
      const games = await fetchUserGames(userId);
      updateUserGames(games);
      gamesError.value = null;
      console.log('Juegos cargados:', games.length);
    } catch (error) {
      console.error('Error cargando juegos:', error);
      gamesError.value = error.message;
    } finally {
      isLoadingGames.value = false;
    }

    // Cargar ranking
    try {
      isLoadingRanking.value = true;
      const ranking = await fetchRankingData();
      globalRanking.value = ranking;
      updateUserRank(ranking, usuario.username);
      rankingError.value = null;
      console.log('Ranking cargado:', ranking.length, 'jugadores');
    } catch (error) {
      console.error('Error cargando ranking:', error);
      rankingError.value = error.message;
    } finally {
      isLoadingRanking.value = false;
    }

  } catch (error) {
    console.error('Error general loading stats:', error);
    
    // Solo redirigir si es un error de autenticación
    if (error.message && 
        (error.message.includes('Token') || 
         error.message.includes('401') || 
         error.message.includes('sesión'))) {
      
      const toast = await toastController.create({
        message: 'Tu sesión ha expirado. Por favor, inicia sesión nuevamente.',
        duration: 3000,
        position: 'bottom',
        color: 'warning'
      });
      await toast.present();
      
      router.push('/');
    } else {
      // Para otros errores, mostrar mensaje pero no redirigir
      const toast = await toastController.create({
        message: 'Hubo un problema cargando algunos datos. Intenta refrescar la página.',
        duration: 4000,
        position: 'bottom',
        color: 'warning'
      });
      await toast.present();
    }
  } finally {
    isLoading.value = false;
  }
};

// Lifecycle
onMounted(async () => {
  console.log('Dashboard mounted, checking authentication...');
  console.log('LocalStorage token exists:', !!localStorage.getItem('token'));
  console.log('LocalStorage usuario exists:', !!localStorage.getItem('usuario'));
  
  if (!isAuthenticated.value) {
    console.log('Not authenticated, redirecting to login');
    router.push('/');
    return;
  }
  
  await loadUserStats();
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
  --background: rgba(0, 0, 0);
  --color: white;
  --border-color: rgba(255, 255, 255, 0.2);
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
.page-title {
  font-weight: 600;
  font-size: 1.2rem;
}

.title-container {
  display: flex;
  align-items: center;
  gap: 8px;
}

.subtitle {
  color: var(--text-secondary);
  margin-top: 0;
  margin-bottom: 16px;
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
  margin-bottom: 24px;
}

/* === PROFILE HEADER === */
.profile-header {
  display: flex;
  align-items: center;
  gap: 20px;
  background: var(--card-bg);
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  border: 1px solid var(--bg-secondary);
}

.profile-avatar {
  position: relative;
  flex-shrink: 0;
}

.profile-avatar .avatar-image,
.profile-avatar .avatar-initials {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.profile-avatar .avatar-image {
  object-fit: cover;
  border: 3px solid var(--primary-color);
}

.profile-avatar .avatar-initials {
  background: linear-gradient(135deg, var(--primary-dark), var(--primary-medium));
  color: white;
  font-size: 28px;
  font-weight: 700;
  border: 3px solid var(--primary-color);
}

.verified-badge {
  position: absolute;
  bottom: 4px;
  right: 4px;
  background: var(--secondary-color);
  color: white;
  border-radius: 50%;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid var(--card-bg);
  font-size: 14px;
}

.profile-info {
  flex: 1;
  min-width: 0;
}

.profile-info h1 {
  margin-bottom: 8px;
  font-size: 28px;
  font-weight: 700;
}

.quick-stats {
  display: flex;
  gap: 24px;
  margin-top: 16px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 12px 16px;
  background: var(--bg-secondary);
  border-radius: 12px;
  min-width: 80px;
}

.stat-icon {
  font-size: 24px;
  color: var(--primary-color);
  margin-bottom: 4px;
}

.stat-value {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
}

.stat-label {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 2px;
}

/* Responsive para móviles */
@media (max-width: 768px) {
  .profile-header {
    flex-direction: column;
    text-align: center;
    gap: 16px;
  }
  
  .profile-info h1 {
    font-size: 24px;
  }
  
  .quick-stats {
    justify-content: center;
    gap: 16px;
  }
  
  .stat-item {
    min-width: 70px;
    padding: 10px 12px;
  }
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