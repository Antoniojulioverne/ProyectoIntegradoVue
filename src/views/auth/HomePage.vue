<template>
  <ion-page class="login-page">
    <div class="login-container">
      <div class="login-card">
        <div class="logo-container">
          <div class="game-logo-placeholder">
            <span>GS</span>
          </div>
          <h1 class="app-title">Game Stats</h1>
        </div>
        
        <form @submit.prevent="login" class="login-form">
          <div class="form-header">
            <h2>Iniciar Sesión</h2>
            <p>Accede para ver tus estadísticas de juego</p>
          </div>
          
          <div class="form-group">
            <ion-item class="custom-item">
              <ion-label position="floating" required class="custom-label">Email</ion-label>
              <ion-input v-model="credentials.email" type="email" required class="custom-input"></ion-input>
            </ion-item>
          </div>
          
          <div class="form-group">
            <ion-item class="custom-item">
              <ion-label position="floating" required class="custom-label">Contraseña</ion-label>
              <ion-input v-model="credentials.password" type="password" required class="custom-input"></ion-input>
            </ion-item>
          </div>
          
          <div class="form-actions">
            <ion-button type="submit" expand="block" :disabled="isLoading" class="login-button">
              <span v-if="!isLoading">Iniciar Sesión</span>
              <ion-spinner v-else name="circles" class="login-spinner"></ion-spinner>
            </ion-button>
          </div>
          
          <div class="form-footer">
            <a @click.prevent="goToRegister" class="create-account">¿No tienes cuenta? Crear una</a>
            <a @click.prevent="goToForgotPassword" class="forgot-password">¿Olvidaste tu contraseña?</a>
          </div>
        </form>
        
        <ion-text color="danger" v-if="errorMessage" class="error-message">
          <p>{{ errorMessage }}</p>
        </ion-text>

        <ion-text color="success" v-if="successMessage" class="success-message">
          <p>{{ successMessage }}</p>
        </ion-text>
      </div>
    </div>
  </ion-page>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import axios from 'axios';
import { 
  IonPage, 
  IonItem, 
  IonLabel, 
  IonInput, 
  IonButton, 
  IonText,
  IonSpinner,
  loadingController, 
  toastController 
} from '@ionic/vue';

import { useAuth } from '@/composables/useAuth';
import { useAuthExtended } from '@/composables/useAuth';

// Endpoint para el login
const LOGIN_ENDPOINT = 'https://gameconnect-latest.onrender.com/auth/login';

// Estado reactivo
const credentials = ref({
  email: '',
  password: ''
});
const isLoading = ref(false);
const errorMessage = ref('');
const successMessage = ref('');
const router = useRouter();
const route = useRoute();

const { initializeUser } = useAuth();
const { showToast } = useAuthExtended();

const goToRegister = () => {
  router.push('/registro');
};

const goToForgotPassword = () => {
  router.push('solicitar-recuperacion');
};

// Verificar si ya existe una sesión
const checkExistingSession = async () => {
  try {
    const existingToken = localStorage.getItem('token');
    
    if (existingToken) {
      isLoading.value = true;
      
      axios.defaults.headers.common['Authorization'] = `Bearer ${existingToken}`;

      // Inicializar datos del usuario si ya había token
      initializeUser();
      
      setTimeout(() => {
        router.push('/dashboard');
      }, 100);
    }
  } catch (error) {
    console.error('Error al verificar la sesión existente:', error);
    localStorage.removeItem('token');
    localStorage.removeItem('userData');
    localStorage.removeItem('usuario');
  } finally {
    isLoading.value = false;
  }
};

// Manejar errores de API con el sistema del useAuthExtended
const handleLoginError = (error) => {
  console.error('Error de inicio de sesión:', error);
  
  // Usar la misma lógica de manejo de errores que useAuthExtended
  if (error.code === 'ECONNREFUSED' || error.code === 'ERR_NETWORK') {
    return 'No se pudo conectar con el servidor. Verifica que esté funcionando.';
  }
  if (error.code === 'ECONNABORTED') {
    return 'La conexión tardó demasiado. Intenta de nuevo.';
  }
  if (error.response) {
    const status = error.response.status;
    const data = error.response.data;
    
    if (status === 400) {
      return typeof data === 'string' ? data : 'Datos inválidos';
    }
    if (status === 401) {
      return 'Credenciales incorrectas';
    }
    if (status === 403) {
      return 'Email no verificado. Revisa tu correo para verificar tu cuenta.';
    }
    if (status === 404) {
      return 'Usuario no encontrado';
    }
    if (status === 429) {
      return 'Demasiados intentos. Inténtalo más tarde';
    }
    if (status === 500) {
      return 'Error del servidor. Inténtalo más tarde';
    }
    
    return typeof data === 'string' ? data : `Error del servidor: ${status}`;
  }
  if (error.request) {
    return 'No se recibió respuesta del servidor.';
  }

  return error.message || 'Error desconocido';
};

// Verificar mensajes de URL (como confirmación de verificación de email)
const checkURLMessages = () => {
  const verified = route.query.verified;
  const message = route.query.message;
  
  if (verified === 'true') {
    successMessage.value = 'Email verificado correctamente. Ya puedes iniciar sesión.';
    showToast('Email verificado correctamente', 'success');
  } else if (message) {
    successMessage.value = decodeURIComponent(message);
  }
};

onMounted(() => {
  checkExistingSession();
  checkURLMessages();
});

// Función de inicio de sesión mejorada
const login = async () => {
  try {
    errorMessage.value = '';
    successMessage.value = '';
    isLoading.value = true;
    
    const loading = await loadingController.create({
      message: 'Iniciando sesión...',
      duration: 10000
    });
    
    await loading.present();
    
    const loginData = {
      email: credentials.value.email.trim(),
      password: credentials.value.password
    };
    
    const response = await axios.post(LOGIN_ENDPOINT, loginData, {
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json'
      },
      timeout: 10000
    });
    
    if (response.data && response.data.token && response.data.usuario) {
      const { token, usuario } = response.data;
      
      if (!token) {
        throw new Error('No se recibió el token de sesión');
      }
      
      localStorage.setItem('token', token);
      localStorage.setItem('usuario', JSON.stringify(usuario));
      
      axios.defaults.headers.common['Authorization'] = `Bearer ${token}`;

      initializeUser();
      
      await showToast('¡Bienvenido de nuevo!', 'success');
      
      router.push('/dashboard');
    } else {
      throw new Error('Respuesta de API inválida o login fallido');
    }
  } catch (error) {
    const errorMessage_local = handleLoginError(error);
    errorMessage.value = errorMessage_local;
    
    // Si el error es por email no verificado, dar opción de reenviar código
    if (error.response && error.response.status === 403) {
      // Guardar email temporalmente para posible reenvío de código
      localStorage.setItem('tempUserData', JSON.stringify({ email: credentials.value.email }));
      
      setTimeout(() => {
        if (confirm('¿Deseas ir a la página de verificación para reenviar el código?')) {
          router.push('/verificar-email');
        }
      }, 2000);
    }
    
    await showToast(errorMessage_local, 'danger', 3000);
  } finally {
    isLoading.value = false;
    const loadings = await loadingController.getTop();
    if (loadings) {
      await loadingController.dismiss();
    }
  }
};
</script>

<style scoped>
.login-page {
  background: linear-gradient(135deg, #1a2a6c, #b21f1f, #fdbb2d);
  min-height: 100vh;
  width: 100%;
  position: relative;
}

.login-container {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  overflow-y: auto;
  box-sizing: border-box;
}

.login-card {
  background-color: rgba(255, 255, 255, 0.9);
  border-radius: 12px;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.2);
  width: 100%;
  max-width: 450px;
  padding: 30px;
  transition: all 0.3s ease;
  margin: 10px auto;
}

.login-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 15px 30px rgba(0, 0, 0, 0.3);
}

.logo-container {
  text-align: center;
  margin-bottom: 30px;
}

.game-logo-placeholder {
  width: 80px;
  height: 80px;
  margin: 0 auto 10px;
  border-radius: 50%;
  background: linear-gradient(45deg, #1a2a6c, #b21f1f);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 0 10px rgba(0, 0, 0, 0.2);
}

.game-logo-placeholder span {
  color: white;
  font-size: 28px;
  font-weight: bold;
}

.app-title {
  font-size: 28px;
  font-weight: 700;
  margin: 0;
  color: #333;
}

.form-header {
  text-align: center;
  margin-bottom: 30px;
}

.form-header h2 {
  margin: 0;
  color: #333;
  font-size: 24px;
}

.form-header p {
  margin: 10px 0 0;
  color: #666;
  font-size: 16px;
}

.form-group {
  margin-bottom: 20px;
  position: relative;
}

/* Corrección para la posición vertical de los labels */
.custom-item {
  --background: transparent;
  --border-color: #ddd;
  --border-width: 1px;
  --border-radius: 8px;
  --padding-start: 12px;
  --padding-end: 12px;
  --padding-top: 0px; 
  --padding-bottom: 0px;
  --min-height: 48px;
  margin-bottom: 5px;
  position: relative;
}

/* Corregido: Posición del label más arriba con espacio */
.custom-label {
  color: #333 !important;
  font-weight: 500;
  margin-bottom: 0;
  opacity: 1 !important;
  transform-origin: left top;
  transition: transform 0.15s ease-in-out, opacity 0.15s ease-in-out;
  position: relative;
  top: -8px; /* Subir el label hacia arriba */
  margin-left: 0; /* Alinear correctamente con el borde del input */
}

.custom-input {
  color: #000 !important;
  border-radius: 8px;
  padding: 8px;
  width: 100%;
  box-sizing: border-box;
  margin-top: 0; /* Ajustado para mejorar espaciado */
  --padding-top: 0; /* Reset padding */
  --padding-bottom: 0; /* Reset padding */
  --padding-start: 0; /* Reset padding lateral */
  height: 48px; /* Altura fija para el input */
  font-size: 16px;
}

/* Ajustado para subir el label cuando está activo/tiene valor */
ion-item.item-has-focus .custom-label,
ion-item.item-has-value .custom-label {
  transform: translateY(-22px) scale(0.85) !important; /* Más arriba */
  color: #1a2a6c !important;
}

/* Override específico para Ionic */
:host(.item-interactive) {
  --padding-bottom: 0;
  --padding-top: 0;
}

:host(.item-label-floating) ion-input {
  --padding-top: 0;
  --padding-bottom: 0;
}

.custom-input::placeholder {
  color: #999 !important;
  opacity: 1 !important;
}

.custom-input::-webkit-input-placeholder {
  color: #999 !important;
}

.custom-input::-moz-placeholder {
  color: #999 !important;
}

.custom-input:-ms-input-placeholder {
  color: #999 !important;
}

/* Asegurar que el label se posiciona correctamente cuando hay valor */
.item-has-value.sc-ion-item-md-h .sc-ion-label-md-s,
.item-has-focus.sc-ion-item-md-h .sc-ion-label-md-s {
  transform: translateY(-22px) scale(0.85) !important;
}

.form-actions {
  margin-top: 30px;
}

.login-button {
  --background: #1a2a6c;
  --border-radius: 8px;
  --box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
  height: 50px;
  font-size: 18px;
  font-weight: 600;
  text-transform: none;
}

.login-spinner {
  color: #fff;
}

.form-footer {
  margin-top: 20px;
  text-align: center;
}
.create-account {
  color: #1a2a6c;
  text-decoration: none;
  font-size: 14px;
  transition: color 0.3s ease;
  cursor: pointer;
}

.create-account:hover {
  color: #b21f1f;
  text-decoration: underline;
}

.error-message {
  text-align: center;
  margin-top: 20px;
  font-size: 14px;
}

/* Media queries mejorados para dispositivos móviles */
@media screen and (max-width: 768px) {
  .login-card {
    padding: 20px;
    margin: 0 auto;
    max-width: 90%;
  }
  
  /* No cambiamos la altura del input en móvil, solo ajustamos label */
  .custom-label {
    top: -8px;
  }
  
  /* Asegurar que el label se posiciona correctamente al tener foco/valor */
  ion-item.item-has-focus .custom-label,
  ion-item.item-has-value .custom-label {
    transform: translateY(-22px) scale(0.85) !important;
  }
  
  .app-title {
    font-size: 24px;
  }
  
  .form-header h2 {
    font-size: 20px;
  }
  
  .form-header p {
    font-size: 14px;
  }
  
  .login-button {
    height: 48px;
    font-size: 16px;
  }
}

/* Media queries para dispositivos muy pequeños */
@media screen and (max-width: 320px) {
  .login-card {
    padding: 15px;
  }
  
  .game-logo-placeholder {
    width: 60px;
    height: 60px;
  }
  
  .game-logo-placeholder span {
    font-size: 22px;
  }
  
  .app-title {
    font-size: 22px;
  }
  
  .form-header h2 {
    font-size: 18px;
  }
  
  .form-header p {
    font-size: 13px;
  }
  
  .login-button {
    height: 45px;
    font-size: 15px;
  }
}

/* Fix para problemas de visualización en teclado virtual */
@media screen and (max-height: 600px) {
  .login-container {
    align-items: flex-start;
    padding-top: 20px;
  }
  
  .login-card {
    margin-top: 0;
  }
  
  .logo-container {
    margin-bottom: 20px;
  }
  
  .game-logo-placeholder {
    width: 60px;
    height: 60px;
  }
}

/* Orientación landscape en móviles */
@media (max-height: 500px) and (orientation: landscape) {
  .login-container {
    padding: 10px;
  }
  
  .login-card {
    padding: 15px;
  }
  
  .logo-container {
    margin-bottom: 15px;
  }
  
  .form-header {
    margin-bottom: 15px;
  }
  
  .form-group {
    margin-bottom: 10px;
  }
  
  .form-actions {
    margin-top: 15px;
  }
}

/* Soporte para pantallas de alta densidad */
@media (-webkit-min-device-pixel-ratio: 2), (min-resolution: 192dpi) {
  .login-card {
    box-shadow: 0 8px 20px rgba(0, 0, 0, 0.15);
  }
}

/* Soporte para navegación táctil */
@media (hover: none) {
  .login-card:hover {
    transform: none;
    box-shadow: 0 10px 25px rgba(0, 0, 0, 0.2);
  }
  
  .forgot-password:active {
    color: #b21f1f;
  }
}

/* Prevenir comportamientos extraños en inputs en iOS */
@supports (-webkit-touch-callout: none) {
  .custom-input {
    font-size: 16px !important;
  }
  
  .login-button {
    padding-top: 0;
    padding-bottom: 0;
  }
}
</style>