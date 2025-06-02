<template>
  <ion-page class="solicitar-page">
    <div class="solicitar-container">
      <div class="solicitar-card">
        <div class="logo-container">
          <div class="game-logo-placeholder">
            <span>GS</span>
          </div>
          <h1 class="app-title">Game Stats</h1>
        </div>

        <div class="form-header">
          <h2>Recuperar Contraseña</h2>
          <p>Ingresa tu email para recibir un enlace de recuperación</p>
        </div>

        <form @submit.prevent="solicitarRecuperacion" class="solicitar-form">
          <div class="form-group">
            <ion-item class="custom-item">
              <ion-label position="floating" class="custom-label">Email</ion-label>
              <ion-input 
                v-model="email" 
                type="email"
                required 
                class="custom-input"
                autocomplete="email"
              ></ion-input>
            </ion-item>
          </div>

          <div class="form-actions">
            <ion-button 
              type="submit" 
              expand="block" 
              :disabled="isLoading || !emailValido" 
              class="solicitar-button"
            >
              <span v-if="!isLoading">Enviar Enlace de Recuperación</span>
              <ion-spinner v-else name="circles" class="solicitar-spinner"></ion-spinner>
            </ion-button>
          </div>

          <ion-text color="danger" v-if="errorMessage" class="error-message">
            <p>{{ errorMessage }}</p>
          </ion-text>

          <ion-text color="success" v-if="successMessage" class="success-message">
            <p>{{ successMessage }}</p>
          </ion-text>
        </form>

        <div class="back-to-login">
          <ion-button fill="clear" @click="volverAlLogin" class="back-button">
            <ion-icon :icon="arrowBack" slot="start"></ion-icon>
            Volver al login
          </ion-button>
        </div>
      </div>
    </div>
  </ion-page>
</template>

<script setup>
import { ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import { arrowBack } from 'ionicons/icons';
import {
  IonPage, IonItem, IonLabel, IonInput, IonButton, IonText, IonSpinner, IonIcon,
  loadingController, toastController
} from '@ionic/vue';

import { useAuthExtended } from '@/composables/useAuth';

const email = ref('');
const errorMessage = ref('');
const successMessage = ref('');

const router = useRouter();
const { isLoading, solicitarRecuperacion: solicitarRecuperacionAPI } = useAuthExtended();

const emailValido = computed(() => {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return email.value.length > 0 && emailRegex.test(email.value);
});

const solicitarRecuperacion = async () => {
  if (!emailValido.value) {
    errorMessage.value = 'Por favor ingresa un email válido';
    return;
  }

  try {
    errorMessage.value = '';
    successMessage.value = '';

    const loading = await loadingController.create({
      message: 'Enviando enlace de recuperación...',
      duration: 30000
    });

    await loading.present();

    const result = await solicitarRecuperacionAPI(email.value);

    if (result.success) {
      successMessage.value = 'Se ha enviado un enlace de recuperación a tu email. Revisa tu bandeja de entrada.';
      
      // Mostrar toast de éxito
      const toast = await toastController.create({
        message: 'Enlace de recuperación enviado. Revisa tu email.',
        duration: 4000,
        color: 'success',
        position: 'top'
      });
      await toast.present();

      // Limpiar el formulario
      email.value = '';
      
    } else {
      errorMessage.value = result.message || 'Error al enviar el enlace de recuperación';
    }

  } catch (error) {
    console.error('Error al solicitar recuperación:', error);
    errorMessage.value = 'Error al solicitar recuperación de contraseña';
  } finally {
    const loading = await loadingController.getTop();
    if (loading) await loadingController.dismiss();
  }
};

const volverAlLogin = () => {
  router.push('/');
};
</script>

<style scoped>
.solicitar-page {
  --background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.solicitar-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  padding: 20px;
}

.solicitar-card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 20px;
  padding: 40px 30px;
  width: 100%;
  max-width: 400px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
}

.logo-container {
  text-align: center;
  margin-bottom: 40px;
}

.game-logo-placeholder {
  width: 80px;
  height: 80px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 15px;
  color: white;
  font-size: 32px;
  font-weight: bold;
}

.app-title {
  color: #333;
  font-size: 28px;
  font-weight: 600;
  margin: 0;
}

.form-header {
  text-align: center;
  margin-bottom: 30px;
}

.form-header h2 {
  color: #333;
  font-size: 24px;
  font-weight: 600;
  margin: 0 0 10px 0;
}

.form-header p {
  color: #666;
  font-size: 16px;
  margin: 0;
}

.form-group {
  margin-bottom: 20px;
}

.custom-item {
  --background: transparent;
  --border-color: #e0e0e0;
  --border-width: 2px;
  --border-radius: 12px;
  --padding-start: 15px;
  --padding-end: 15px;
  --min-height: 60px;
}

.custom-item.item-has-focus {
  --border-color: #667eea;
}

.custom-label {
  --color: #666;
  font-weight: 500;
}

.custom-input {
  --color: #333;
  font-size: 16px;
}

.form-actions {
  margin: 40px 0 20px 0;
}

.solicitar-button {
  --background: linear-gradient(135deg, #667eea, #764ba2);
  --color: white;
  --border-radius: 12px;
  height: 50px;
  font-size: 16px;
  font-weight: 600;
  text-transform: none;
}

.solicitar-button:hover {
  --background: linear-gradient(135deg, #5a6fd8, #6a4190);
}

.solicitar-spinner {
  --color: white;
}

.error-message,
.success-message {
  text-align: center;
  margin-top: 15px;
  font-size: 14px;
}

.back-to-login {
  text-align: center;
  margin-top: 30px;
}

.back-button {
  --color: #667eea;
  font-size: 14px;
  text-transform: none;
}

@media (max-width: 480px) {
  .solicitar-card {
    padding: 30px 20px;
    margin: 10px;
  }
  
  .game-logo-placeholder {
    width: 60px;
    height: 60px;
    font-size: 24px;
  }
  
  .app-title {
    font-size: 24px;
  }
}
</style>