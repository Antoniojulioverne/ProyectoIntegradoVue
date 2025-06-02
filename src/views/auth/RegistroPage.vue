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

        <form @submit.prevent="register" class="login-form">
          <div class="form-header">
            <h2>Crear Cuenta</h2>
            <p>Regístrate para acceder a tus estadísticas</p>
          </div>

          <div class="form-group">
            <ion-item class="custom-item">
              <ion-label position="floating" class="custom-label">Nombre</ion-label>
              <ion-input v-model="userData.username" type="text" required class="custom-input"></ion-input>
            </ion-item>
          </div>

          <div class="form-group">
            <ion-item class="custom-item">
              <ion-label position="floating" class="custom-label">Email</ion-label>
              <ion-input v-model="userData.email" type="email" required class="custom-input"></ion-input>
            </ion-item>
          </div>

          <div class="form-group">
            <ion-item class="custom-item">
              <ion-label position="floating" class="custom-label">Contraseña</ion-label>
              <ion-input v-model="userData.password" type="password" required class="custom-input"></ion-input>
            </ion-item>
          </div>

          <div class="form-actions">
            <ion-button type="submit" expand="block" :disabled="isLoading" class="login-button">
              <span v-if="!isLoading">Registrarse</span>
              <ion-spinner v-else name="circles" class="login-spinner"></ion-spinner>
            </ion-button>
          </div>

          <ion-text color="danger" v-if="errorMessage" class="error-message">
            <p>{{ errorMessage }}</p>
          </ion-text>
        </form>
      </div>
    </div>
  </ion-page>
</template>

<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import axios from 'axios';
import {
  IonPage, IonItem, IonLabel, IonInput, IonButton, IonText, IonSpinner,
  toastController, loadingController
} from '@ionic/vue';

const REGISTER_ENDPOINT = 'http://192.168.1.234:8090/auth/registro';

const userData = ref({
  username: '',
  email: '',
  password: ''
});
const isLoading = ref(false);
const errorMessage = ref('');
const router = useRouter();

const register = async () => {
  try {
    errorMessage.value = '';
    isLoading.value = true;

    const loading = await loadingController.create({
      message: 'Registrando...',
      duration: 3000
    });

    await loading.present();

    console.log('Enviando datos:', userData.value);

    const response = await axios.post(REGISTER_ENDPOINT, userData.value, {
      headers: {
        'Content-Type': 'application/json'
      },
      timeout: 30000 // 10 segundos de timeout
    });

    console.log('Respuesta del servidor:', response);

    if (response.status === 201 || response.status === 200) {
      // Guardar datos temporales en localStorage ANTES de limpiar el formulario
      localStorage.setItem('tempUserData', JSON.stringify({
        email: userData.value.email,
        username: userData.value.username,
        timestamp: new Date().getTime() // Para limpiar datos viejos
      }));

      const toast = await toastController.create({
        message: '¡Registro exitoso! Revisa tu email para verificar tu cuenta.',
        duration: 2000,
        position: 'bottom',
        color: 'success'
      });
      await toast.present();

      // Limpiar formulario
      userData.value = {
        username: '',
        email: '',
        password: ''
      };

      // Navegar sin mostrar nada en la URL
      router.push('/verificar-email');
    } else {
      throw new Error('No se pudo completar el registro');
    }

  } catch (error) {
    console.error('Error de registro:', error);

    let errorMsg = 'Error al registrar usuario';

    if (error.response) {
      // El servidor respondió con un código de estado de error
      errorMsg = error.response.data?.message || error.response.data || 'Error del servidor';
    } else if (error.request) {
      // La petición se hizo pero no se recibió respuesta
      errorMsg = 'No se pudo conectar con el servidor';
    } else {
      // Error en la configuración de la petición
      errorMsg = error.message || 'Error desconocido';
    }

    errorMessage.value = errorMsg;

    const toast = await toastController.create({
      message: errorMsg,
      duration: 3000,
      position: 'bottom',
      color: 'danger'
    });
    await toast.present();
  } finally {
    isLoading.value = false;
    const loading = await loadingController.getTop();
    if (loading) await loadingController.dismiss();
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
  top: -8px;
  /* Subir el label hacia arriba */
  margin-left: 0;
  /* Alinear correctamente con el borde del input */
}

.custom-input {
  color: #000 !important;
  border-radius: 8px;
  padding: 8px;
  width: 100%;
  box-sizing: border-box;
  margin-top: 0;
  /* Ajustado para mejorar espaciado */
  --padding-top: 0;
  /* Reset padding */
  --padding-bottom: 0;
  /* Reset padding */
  --padding-start: 0;
  /* Reset padding lateral */
  height: 48px;
  /* Altura fija para el input */
  font-size: 16px;
}

/* Ajustado para subir el label cuando está activo/tiene valor */
ion-item.item-has-focus .custom-label,
ion-item.item-has-value .custom-label {
  transform: translateY(-22px) scale(0.85) !important;
  /* Más arriba */
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

.forgot-password {
  color: #1a2a6c;
  text-decoration: none;
  font-size: 14px;
  transition: color 0.3s ease;
}

.forgot-password:hover {
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
@media (-webkit-min-device-pixel-ratio: 2),
(min-resolution: 192dpi) {
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