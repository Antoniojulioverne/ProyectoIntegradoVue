<template>
  <ion-page class="friends-page">
    <ion-header class="custom-header">
      <ion-toolbar class="custom-toolbar">
        <ion-buttons slot="start">
          <ion-back-button class="custom-back-btn"></ion-back-button>
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
      <!-- Contenido principal cuando hay usuario autenticado -->
      <div v-if="usuario?.usuarioId" class="main-content">
        <div class="welcome-section">
          <div class="user-greeting">
            <h2>¡Hola, {{ usuario.username || 'Usuario' }}!</h2>
            <p>Gestiona tu lista de amigos</p>
          </div>
        </div>
        
        <div class="component-container">
          <FriendManagement 
            :usuario-actual-id="usuario.usuarioId"
            :api-base-url="'http://192.168.1.234:8090/GameConnect'"
          />
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
  IonBackButton,
  IonSpinner,
  IonButton,
  IonIcon 
} from '@ionic/vue'
import { ref, onMounted, computed } from 'vue'
import { alertCircle, refresh, people } from 'ionicons/icons'
import { useAuth } from '@/composables/useAuth'
import FriendManagement from '@/views/GestionAmigos.vue'

// Usar el composable de autenticación
const { usuario, cargarDatosAuth } = useAuth()

// Estado para controlar si ya intentamos cargar los datos
const dataLoaded = ref(false)

// Función para recargar datos
const recargarDatos = () => {
  cargarDatosAuth()
  dataLoaded.value = true
}

// Cargar datos al montar el componente
onMounted(() => {
  cargarDatosAuth()
  dataLoaded.value = true
})
</script>

<style scoped>
/* Estilos globales de la página */
.friends-page {
  --background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

/* Header personalizado */
.custom-header {
  --background: transparent;
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
}

.custom-toolbar {
  --background: rgba(255, 255, 255, 0.1);
  --color: white;
  --border-color: rgba(255, 255, 255, 0.2);
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

/* Contenido principal */
.custom-content {
  --background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  --padding-top: 0;
  --padding-bottom: 0;
}

.main-content {
  min-height: 100%;
  padding: 0;
}

/* Sección de bienvenida */
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

/* Container del componente */
.component-container {
  background: white;
  margin: -16px 16px 16px;
  border-radius: 20px 20px 8px 8px;
  box-shadow: 
    0 -4px 20px rgba(0, 0, 0, 0.08),
    0 4px 25px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  min-height: 60vh;
}

/* Estados de carga y error */
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

/* Estado de carga */
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

/* Estado de error */
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

/* Contenido de estados */
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

/* Botones de acción */
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

.component-container {
  animation: fadeInUp 0.8s ease;
}

/* Responsive */
@media (max-width: 480px) {
  .welcome-section {
    padding: 24px 16px 16px;
  }
  
  .user-greeting h2 {
    font-size: 1.5rem;
  }
  
  .component-container {
    margin: -12px 12px 12px;
    border-radius: 16px 16px 8px 8px;
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

/* Modo oscuro */
@media (prefers-color-scheme: dark) {
  .friends-page {
    --background: linear-gradient(135deg, #2d3748 0%, #4a5568 100%);
  }
  
  .custom-content {
    --background: linear-gradient(135deg, #1a202c 0%, #2d3748 100%);
  }
  
  .status-card {
    background: #2d3748;
    border-color: rgba(255, 255, 255, 0.1);
  }
  
  .component-container {
    background: #2d3748;
  }
  
  .status-content h3 {
    color: #f7fafc;
  }
  
  .status-content p {
    color: #a0aec0;
  }
}
</style>