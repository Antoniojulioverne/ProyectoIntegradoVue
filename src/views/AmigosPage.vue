<template>
  <ion-page class="friends-page">
    <ion-header class="custom-header">
      <ion-toolbar class="custom-toolbar">
        <ion-buttons slot="start">
          <ion-menu-button auto-hide="false"></ion-menu-button>
        </ion-buttons>
        <ion-title class="page-title">
          <div class="title-container">
            <ion-icon :icon="people" class="title-icon"></ion-icon>
            <span>Gestión de Amigos</span>
          </div>
        </ion-title>
      </ion-toolbar>
    </ion-header>
    
    <ion-content class="custom-content">
      <!-- *** AGREGAR ION-REFRESHER *** -->
      <ion-refresher slot="fixed" @ionRefresh="handleRefresh($event)">
        <ion-refresher-content
          pulling-icon="people-circle-outline"
          pulling-text="Desliza para actualizar amigos"
          refreshing-spinner="circular"
          refreshing-text="Actualizando..."
        ></ion-refresher-content>
      </ion-refresher>
      <!-- Contenido principal cuando hay usuario autenticado -->
      <div v-if="usuario?.usuarioId && isAuthenticated" class="main-content">
        <div class="welcome-section">
          <div class="user-greeting">
            <h2>¡Hola, {{ usuario.username || 'Usuario' }}!</h2>
            <p>Gestiona tu lista de amigos y ve tu ranking</p>
          </div>
        </div>
        
        <!-- Tabs para organizar el contenido -->
        <div class="content-tabs">
          <div class="tab-container">
            <div class="tab-buttons">
              <button 
                class="tab-button"
                :class="{ active: activeTab === 'friends' }"
                @click="activeTab = 'friends'"
              >
                <ion-icon :icon="people"></ion-icon>
                <span>Amigos</span>
              </button>
              <button 
                class="tab-button"
                :class="{ active: activeTab === 'ranking' }"
                @click="activeTab = 'ranking'"
              >
                <ion-icon :icon="trophy"></ion-icon>
                <span>Ranking</span>
              </button>
            </div>
          </div>
          
          <!-- Contenido de las tabs -->
          <div class="tab-content">
            <!-- Tab de gestión de amigos -->
            <div v-show="activeTab === 'friends'" class="tab-panel">
              <FriendManagement 
                v-if="usuario?.usuarioId"
                :usuario-actual-id="usuario.usuarioId"
                :api-base-url="'https://gameconnect-latest.onrender.com/GameConnect'"
              />
            </div>
            
            <!-- Tab de ranking de amigos -->
            <div v-show="activeTab === 'ranking'" class="tab-panel">
              <RankingAmigos
                v-if="usuario?.usuarioId && usuario?.username"
                :usuario-actual-id="usuario.usuarioId"
                :current-username="usuario.username"
                :api-base-url="'https://gameconnect-latest.onrender.com/GameConnect'"
              />
            </div>
          </div>
        </div>
      </div>
      
      <!-- Estado de carga mejorado -->
      <div v-else-if="!dataLoaded" class="status-container loading-state">
        <div class="status-card">
          <div class="spinner-container">
            <ion-spinner name="crescent" class="custom-spinner"></ion-spinner>
          </div>
          <div class="status-content">
            <h3>Cargando datos</h3>
            <p>Obteniendo información del usuario...</p>
          </div>
        </div>
      </div>
      
      <!-- Estado de error mejorado -->
      <div v-else class="status-container error-state">
        <div class="status-card error-card">
          <div class="error-icon-container">
            <ion-icon :icon="alertCircle" class="error-icon"></ion-icon>
          </div>
          <div class="status-content">
            <h3>Oops, algo salió mal</h3>
            <p>No se pudieron cargar los datos del usuario. Verifica tu conexión e inténtalo de nuevo.</p>
          </div>
          <div class="action-buttons">
            <ion-button 
              @click="recargarDatos" 
              fill="clear" 
              class="retry-button"
            >
              <ion-icon :icon="refresh" slot="start"></ion-icon>
              Reintentar
            </ion-button>
          </div>
        </div>
      </div>
    </ion-content>
  </ion-page>
</template>

<script setup>
import { 
  IonPage,
  IonHeader,
  IonToolbar,
  IonTitle,
  IonContent,
  IonButtons,
  IonMenuButton,
  IonSpinner,
  IonButton,
  IonIcon
  ,IonRefresherContent, IonRefresher,
  toastController
} from '@ionic/vue'
import { ref, onMounted} from 'vue'
import { alertCircle, refresh, people, trophy } from 'ionicons/icons'
import { useAuth } from '@/composables/useAuth'
import FriendManagement from '@/views/GestionAmigos.vue'
import RankingAmigos from '@/stats/RankingAmigos.vue' // Ajusta la ruta según tu estructura

// Usar el composable de autenticación
const { usuario, cargarDatosAuth, isAuthenticated, token } = useAuth()

// Estado para controlar las tabs
const activeTab = ref('friends')

// Estado para controlar si ya intentamos cargar los datos
const dataLoaded = ref(false)

// Función para recargar datos
const recargarDatos = async () => {
  try {
    await cargarDatosAuth()
    dataLoaded.value = true
  } catch (error) {
    console.error('Error al recargar datos:', error)
    dataLoaded.value = true
  }
}
// *** FUNCIÓN DE REFRESH CORREGIDA ***
const handleRefresh = async (event) => {
  try {
    console.log('🔄 Actualizando Gestión de Amigos...')
    
    // Solo recargar datos del usuario
    await cargarDatosAuth()
    
    const toast = await toastController.create({
      message: '✅ Datos actualizados',
      duration: 2000,
      position: 'top',
      color: 'success'
    })
    await toast.present()
    
  } catch (error) {
    console.error('❌ Error:', error)
    
    const toast = await toastController.create({
      message: '❌ Error al actualizar',
      duration: 3000,
      position: 'top',
      color: 'danger'
    })
    await toast.present()
    
  } finally {
    if (event?.target) {
      event.target.complete()
    }
  }
}


// Cargar datos al montar el componente
onMounted(async () => {
  await recargarDatos()
})
</script>

<style scoped>
/* Tus estilos existentes para la página */
.friends-page {
  --background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.custom-header {
  --background: transparent;
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
}

.custom-toolbar {
  --background: rgba(0, 0, 0);
  --color: white;
  --border-color: rgba(0, 0, 0);
}

.custom-back-btn {
  --color: white;
  --icon-font-size: 24px;
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

.title-icon {
  font-size: 24px;
  color: white;
}

.custom-content {
  --background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  --padding-top: 0;
  --padding-bottom: 0;
}

.main-content {
  min-height: 100%;
  padding: 0;
}

.welcome-section {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 32px 24px 24px;
  color: white;
  position: relative;
  overflow: hidden;
}

.welcome-section::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: url('data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><defs><pattern id="grain" width="100" height="100" patternUnits="userSpaceOnUse"><circle cx="25" cy="25" r="1" fill="rgba(255,255,255,0.1)"/><circle cx="75" cy="75" r="1" fill="rgba(255,255,255,0.1)"/><circle cx="50" cy="10" r="0.5" fill="rgba(255,255,255,0.05)"/></pattern></defs><rect width="100" height="100" fill="url(%23grain)"/></svg>');
  opacity: 0.3;
}

.user-greeting {
  position: relative;
  z-index: 1;
}

.user-greeting h2 {
  margin: 0 0 8px 0;
  font-size: 1.8rem;
  font-weight: 700;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.user-greeting p {
  margin: 0;
  font-size: 1rem;
  opacity: 0.9;
  font-weight: 400;
}

/* Nuevos estilos para las tabs */
.content-tabs {
  background: white;
  margin: -16px 16px 16px;
  border-radius: 20px 20px 8px 8px;
  box-shadow: 
    0 -4px 20px rgba(0, 0, 0, 0.08),
    0 4px 25px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  min-height: 60vh;
}

.tab-container {
  background: #f8f9fa;
  border-bottom: 1px solid rgba(0, 0, 0, 0.1);
  position: sticky;
  top: 0;
  z-index: 10;
}

.tab-buttons {
  display: flex;
  width: 100%;
}

.tab-button {
  flex: 1;
  padding: 16px 12px;
  background: transparent;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 0.9rem;
  font-weight: 500;
  color: var(--ion-color-medium);
  transition: all 0.3s ease;
  position: relative;
}

.tab-button.active {
  color: var(--ion-color-primary);
  background: white;
}

.tab-button.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: var(--ion-color-primary);
}

.tab-button:hover:not(.active) {
  color: var(--ion-color-primary-tint);
  background: rgba(102, 126, 234, 0.05);
}

.tab-button ion-icon {
  font-size: 20px;
}

.tab-content {
  background: white;
  min-height: 50vh;
}

.tab-panel {
  padding: 20px;
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

/* Estados de carga y error existentes */
.status-container {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 70vh;
  padding: 24px;
}

.status-card {
  background: white;
  border-radius: 24px;
  padding: 40px 32px;
  text-align: center;
  max-width: 400px;
  width: 100%;
  box-shadow: 
    0 10px 40px rgba(0, 0, 0, 0.1),
    0 4px 25px rgba(0, 0, 0, 0.05);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
}

.loading-state .status-card {
  border: 2px solid rgba(102, 126, 234, 0.1);
}

.spinner-container {
  margin-bottom: 24px;
}

.custom-spinner {
  --color: #667eea;
  width: 48px;
  height: 48px;
}

.error-card {
  border: 2px solid rgba(244, 67, 54, 0.1);
}

.error-icon-container {
  margin-bottom: 24px;
}

.error-icon {
  font-size: 48px;
  color: #f44336;
}

.status-content h3 {
  margin: 0 0 12px 0;
  font-size: 1.4rem;
  font-weight: 600;
  color: #2c3e50;
}

.status-content p {
  margin: 0 0 24px 0;
  color: #718096;
  line-height: 1.5;
  font-size: 0.95rem;
}

.action-buttons {
  margin-top: 24px;
}

.retry-button {
  --background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  --color: white;
  --border-radius: 12px;
  --padding: 12px 24px;
  font-weight: 600;
  text-transform: none;
  letter-spacing: 0.5px;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.3);
  transition: all 0.3s ease;
}

.retry-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
}

/* Responsive para las tabs */
@media (max-width: 480px) {
  .content-tabs {
    margin: -12px 12px 12px;
    border-radius: 16px 16px 8px 8px;
  }
  
  .tab-button {
    padding: 14px 8px;
    font-size: 0.85rem;
    gap: 6px;
  }
  
  .tab-button ion-icon {
    font-size: 18px;
  }
  
  .tab-panel {
    padding: 16px;
  }
  
  .welcome-section {
    padding: 24px 16px 16px;
  }
  
  .user-greeting h2 {
    font-size: 1.5rem;
  }
  
  .status-card {
    padding: 32px 24px;
    border-radius: 20px;
  }
  
  .custom-spinner {
    width: 40px;
    height: 40px;
  }
  
  .error-icon {
    font-size: 40px;
  }
}

/* Ajustes para pantallas muy pequeñas */
@media (max-width: 380px) {
  .tab-button span {
    display: none;
  }
  
  .tab-button {
    gap: 0;
  }
  
  .tab-button ion-icon {
    font-size: 22px;
  }
}

/* Modo oscuro */
@media (prefers-color-scheme: dark) {
  .friends-page {
    --background: linear-gradient(135deg, #2d3748 0%, #4a5568 100%);
  }
  
  .custom-content {
    --background: linear-gradient(135deg, #1a202c 0%, #2d3748 100%);
  }
  
  .content-tabs {
    background: #2d3748;
  }
  
  .tab-container {
    background: #1a202c;
    border-bottom-color: rgba(255, 255, 255, 0.1);
  }
  
  .tab-button.active {
    background: #2d3748;
  }
  
  .tab-content {
    background: #2d3748;
  }
  
  .tab-button:hover:not(.active) {
    background: rgba(102, 126, 234, 0.1);
  }
  
  .status-card {
    background: #2d3748;
    border-color: rgba(255, 255, 255, 0.1);
  }
  
  .status-content h3 {
    color: #f7fafc;
  }
  
  .status-content p {
    color: #a0aec0;
  }
}

/* Animaciones */
@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.status-card {
  animation: fadeInUp 0.6s ease;
}

.content-tabs {
  animation: fadeInUp 0.8s ease;
}
</style>