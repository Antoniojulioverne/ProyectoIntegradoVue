<template>
  <ion-page class="verification-page">
    <div class="verification-container">
      <div class="verification-card">
        <div class="logo-container">
          <div class="game-logo-placeholder">
            <span>GS</span>
          </div>
          <h1 class="app-title">Game Stats</h1>
        </div>

        <form @submit.prevent="verificarCodigo" class="verification-form">
          <div class="form-header">
            <h2>Verificar Email</h2>
            <p>Ingresa el código de 6 dígitos que enviamos a tu email</p>
          </div>

          <div class="form-group">
            <ion-item class="custom-item">
              <ion-label position="floating" class="custom-label">Código de Verificación</ion-label>
              <ion-input 
                v-model="codigo" 
                type="text" 
                required 
                maxlength="6"
                pattern="[0-9]{6}"
                placeholder="123456"
                class="custom-input"
              ></ion-input>
            </ion-item>
            <div class="input-helper">
              <small>Código de 6 dígitos</small>
            </div>
          </div>

          <div class="form-actions">
            <ion-button 
              type="submit" 
              expand="block" 
              :disabled="isLoading || codigo.length !== 6" 
              class="verify-button"
            >
              <span v-if="!isLoading">Verificar</span>
              <ion-spinner v-else name="circles" class="login-spinner"></ion-spinner>
            </ion-button>
          </div>

          <div class="form-footer">
            <p class="resend-text">¿No recibiste el código?</p>
            <ion-button 
              fill="clear" 
              :disabled="isResendDisabled || isLoading"
              @click="reenviarCodigo"
              class="resend-button"
            >
              <span v-if="!isResendDisabled">Reenviar código</span>
              <span v-else>Reenviar en {{ countdown }}s</span>
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
import { ref, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { arrowBack } from 'ionicons/icons';
import {
  IonPage, IonItem, IonLabel, IonInput, IonButton, IonText, IonSpinner, IonIcon,
  loadingController
} from '@ionic/vue';

import { useAuthExtended } from '@/composables/useAuth';

const codigo = ref('');
const errorMessage = ref('');
const successMessage = ref('');
const isResendDisabled = ref(false);
const countdown = ref(60);
let countdownInterval = null;

const router = useRouter();
const { isLoading, verificarEmail, reenviarCodigo: reenviarCodigoAPI } = useAuthExtended();

const userEmail = ref('');

onMounted(() => {
  // Obtener email del usuario desde localStorage o route params
  const userData = localStorage.getItem('tempUserData');
  if (userData) {
    const user = JSON.parse(userData);
    userEmail.value = user.email;
  }
});

onUnmounted(() => {
 
  if (countdownInterval) {
    clearInterval(countdownInterval);
  }
});

const verificarCodigo = async () => {
  if (codigo.value.length !== 6) {
    errorMessage.value = 'El código debe tener 6 dígitos';
    return;
  }

  try {
    errorMessage.value = '';
    successMessage.value = '';

    const loading = await loadingController.create({
      message: 'Verificando código...',
      duration: 3000
    });

    await loading.present();

    const result = await verificarEmail(codigo.value);

    if (result.success) {
      successMessage.value = 'Email verificado correctamente';
      
      // Limpiar datos temporales
      localStorage.removeItem('tempUserData');
      
      // Redirigir al login después de 2 segundos
      setTimeout(() => {
        router.push('/');
      }, 1000);
    } else {
      errorMessage.value = result.message;
    }

  } catch (error) {
    console.error('Error al verificar código:', error);
    errorMessage.value = 'Error al verificar el código';
  } finally {
    const loading = await loadingController.getTop();
    if (loading) await loadingController.dismiss();
  }
};

const reenviarCodigo = async () => {
  if (!userEmail.value) {
    errorMessage.value = 'No se pudo obtener el email del usuario';
    return;
  }

  try {
    errorMessage.value = '';
    
    const result = await reenviarCodigoAPI(userEmail.value);
    
    if (result.success) {
      // Iniciar countdown
      isResendDisabled.value = true;
      countdown.value = 60;
      
      countdownInterval = setInterval(() => {
        countdown.value--;
        if (countdown.value <= 0) {
          clearInterval(countdownInterval);
          isResendDisabled.value = false;
          countdown.value = 60;
        }
      }, 1000);
    } else {
      errorMessage.value = result.message;
    }
  } catch (error) {
    console.error('Error al reenviar código:', error);
    errorMessage.value = 'Error al reenviar el código';
  }
};

const volverAlLogin = () => {
  router.push('/');
};
</script>

<style scoped>
.verification-page {
  background: linear-gradient(135deg, #1a2a6c, #b21f1f, #fdbb2d);
  min-height: 100vh;
  width: 100%;
  position: relative;
}

.verification-container {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  overflow-y: auto;
  box-sizing: border-box;
}

.verification-card {
  background-color: rgba(255, 255, 255, 0.9);
  border-radius: 12px;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.2);
  width: 100%;
  max-width: 450px;
  padding: 30px;
  transition: all 0.3s ease;
  margin: 10px auto;
}

.verification-card:hover {
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

.custom-label {
  color: #333 !important;
  font-weight: 500;
  margin-bottom: 0;
  opacity: 1 !important;
  transform-origin: left top;
  transition: transform 0.15s ease-in-out, opacity 0.15s ease-in-out;
  position: relative;
  top: -8px;
  margin-left: 0;
}

.custom-input {
  color: #000 !important;
  border-radius: 8px;
  padding: 8px;
  width: 100%;
  box-sizing: border-box;
  margin-top: 0;
  --padding-top: 0;
  --padding-bottom: 0;
  --padding-start: 0;
  height: 48px;
  font-size: 18px;
  text-align: center;
  letter-spacing: 4px;
  font-weight: bold;
}

.input-helper {
  margin-top: 5px;
  text-align: center;
}

.input-helper small {
  color: #666;
  font-size: 12px;
}

ion-item.item-has-focus .custom-label,
ion-item.item-has-value .custom-label {
  transform: translateY(-22px) scale(0.85) !important;
  color: #1a2a6c !important;
}

.form-actions {
  margin-top: 30px;
}

.verify-button {
  --background: #1a2a6c;
  --border-radius: 8px;
  --box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
  height: 50px;
  font-size: 18px;
  font-weight: 600;
  text-transform: none;
}

.verify-button:disabled {
  --background: #ccc;
}

.login-spinner {
  color: #fff;
}

.form-footer {
  margin-top: 20px;
  text-align: center;
}

.resend-text {
  color: #666;
  font-size: 14px;
  margin: 10px 0 5px;
}

.resend-button {
  --color: #1a2a6c;
  font-size: 14px;
  text-transform: none;
  font-weight: 500;
}

.resend-button:hover {
  --color: #b21f1f;
}

.back-to-login {
  margin-top: 20px;
  text-align: center;
}

.back-button {
  --color: #666;
  font-size: 14px;
  text-transform: none;
}

.back-button:hover {
  --color: #1a2a6c;
}

.error-message {
  text-align: center;
  margin-top: 20px;
  font-size: 14px;
}

.success-message {
  text-align: center;
  margin-top: 20px;
  font-size: 14px;
}

@media screen and (max-width: 768px) {
  .verification-card {
    padding: 20px;
    margin: 0 auto;
    max-width: 90%;
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
  
  .verify-button {
    height: 48px;
    font-size: 16px;
  }
}
</style>