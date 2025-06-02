<template>
  <ion-page class="reset-page">
    <div class="reset-container">
      <div class="reset-card">
        <div class="logo-container">
          <div class="game-logo-placeholder">
            <span>GS</span>
          </div>
          <h1 class="app-title">Game Stats</h1>
        </div>

        <!-- Estado: Validando token -->
        <div v-if="validandoToken" class="loading-state">
          <ion-spinner name="circles"></ion-spinner>
          <p>Validando enlace...</p>
        </div>

        <!-- Estado: Token inválido -->
        <div v-else-if="tokenInvalido" class="error-state">
          <ion-icon :icon="alertCircle" class="error-icon"></ion-icon>
          <h2>Enlace inválido</h2>
          <p>El enlace de recuperación es inválido o ha expirado.</p>
          <ion-button @click="volverAlLogin" fill="outline" class="back-button">
            Volver al login
          </ion-button>
        </div>

        <!-- Estado: Formulario para nueva contraseña -->
        <form v-else @submit.prevent="restablecerContrasena" class="reset-form">
          <div class="form-header">
            <h2>Nueva Contraseña</h2>
            <p>Ingresa tu nueva contraseña</p>
          </div>

          <div class="form-group">
            <ion-item class="custom-item">
              <ion-label position="floating" class="custom-label">Nueva Contraseña</ion-label>
              <ion-input 
                v-model="nuevaContrasena" 
                :type="mostrarContrasena ? 'text' : 'password'"
                required 
                class="custom-input"
                minlength="6"
              ></ion-input>
              <ion-button 
                fill="clear" 
                slot="end" 
                @click="mostrarContrasena = !mostrarContrasena"
                class="toggle-password"
              >
                <ion-icon :icon="mostrarContrasena ? eyeOff : eye"></ion-icon>
              </ion-button>
            </ion-item>
          </div>

          <div class="form-group">
            <ion-item class="custom-item">
              <ion-label position="floating" class="custom-label">Confirmar Contraseña</ion-label>
              <ion-input 
                v-model="confirmarContrasena" 
                :type="mostrarConfirmarContrasena ? 'text' : 'password'"
                required 
                class="custom-input"
                minlength="6"
              ></ion-input>
              <ion-button 
                fill="clear" 
                slot="end" 
                @click="mostrarConfirmarContrasena = !mostrarConfirmarContrasena"
                class="toggle-password"
              >
                <ion-icon :icon="mostrarConfirmarContrasena ? eyeOff : eye"></ion-icon>
              </ion-button>
            </ion-item>
          </div>

          <div class="form-actions">
            <ion-button 
              type="submit" 
              expand="block" 
              :disabled="isLoading || !formularioValido" 
              class="reset-button"
            >
              <span v-if="!isLoading">Restablecer Contraseña</span>
              <ion-spinner v-else name="circles" class="reset-spinner"></ion-spinner>
            </ion-button>
          </div>

          <ion-text color="danger" v-if="errorMessage" class="error-message">
            <p>{{ errorMessage }}</p>
          </ion-text>

          <ion-text color="success" v-if="successMessage" class="success-message">
            <p>{{ successMessage }}</p>
          </ion-text>
        </form>

        <div class="back-to-login" v-if="!validandoToken && !tokenInvalido">
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
import { ref, computed, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { arrowBack, eye, eyeOff, alertCircle } from 'ionicons/icons';
import {
  IonPage, IonItem, IonLabel, IonInput, IonButton, IonText, IonSpinner, IonIcon,
  loadingController, toastController
} from '@ionic/vue';

import { useAuthExtended } from '@/composables/useAuth';

const nuevaContrasena = ref('');
const confirmarContrasena = ref('');
const mostrarContrasena = ref(false);
const mostrarConfirmarContrasena = ref(false);
const errorMessage = ref('');
const successMessage = ref('');
const validandoToken = ref(true);
const tokenInvalido = ref(false);
const token = ref('');

const router = useRouter();
const route = useRoute();
const { isLoading, validarTokenRecuperacion, restablecerContrasena: restablecerContrasenaAPI } = useAuthExtended();

const formularioValido = computed(() => {
  return nuevaContrasena.value.length >= 6 && 
         confirmarContrasena.value.length >= 6 && 
         nuevaContrasena.value === confirmarContrasena.value;
});

onMounted(async () => {
  // Obtener token de la URL
  token.value = route.query.token || route.params.token || '';
  
  if (!token.value) {
    tokenInvalido.value = true;
    validandoToken.value = false;
    return;
  }

  try {
    // Validar el token con el backend
    const result = await validarTokenRecuperacion(token.value);
    
    if (result.success) {
      validandoToken.value = false;
    } else {
      tokenInvalido.value = true;
      validandoToken.value = false;
    }
  } catch (error) {
    console.error('Error validando token:', error);
    tokenInvalido.value = true;
    validandoToken.value = false;
  }
});

const restablecerContrasena = async () => {
  if (!formularioValido.value) {
    errorMessage.value = 'Las contraseñas no coinciden o son muy cortas (mínimo 6 caracteres)';
    return;
  }

  try {
    errorMessage.value = '';
    successMessage.value = '';

    const loading = await loadingController.create({
      message: 'Restableciendo contraseña...',
      duration: 30000
    });

    await loading.present();

    const result = await restablecerContrasenaAPI(token.value, nuevaContrasena.value);

    if (result.success) {
      successMessage.value = 'Contraseña restablecida correctamente';
      
      // Mostrar toast de éxito
      const toast = await toastController.create({
        message: 'Contraseña actualizada. Puedes iniciar sesión ahora.',
        duration: 3000,
        color: 'success',
        position: 'top'
      });
      await toast.present();

      // Redirigir al login después de un breve delay
      setTimeout(() => {
        router.push('/');
      }, 2000);
    } else {
      errorMessage.value = result.message || 'Error al restablecer la contraseña';
    }

  } catch (error) {
    console.error('Error al restablecer contraseña:', error);
    errorMessage.value = 'Error al restablecer la contraseña';
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
.reset-page {
  --background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.reset-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  padding: 20px;
}

.reset-card {
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

.loading-state,
.error-state {
  text-align: center;
  padding: 40px 20px;
}

.loading-state ion-spinner {
  --color: #667eea;
  margin-bottom: 20px;
}

.error-state .error-icon {
  font-size: 64px;
  color: #e74c3c;
  margin-bottom: 20px;
}

.error-state h2 {
  color: #333;
  margin-bottom: 15px;
}

.error-state p {
  color: #666;
  margin-bottom: 30px;
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

.toggle-password {
  --color: #666;
  margin: 0;
}

.form-actions {
  margin: 40px 0 20px 0;
}

.reset-button {
  --background: linear-gradient(135deg, #667eea, #764ba2);
  --color: white;
  --border-radius: 12px;
  height: 50px;
  font-size: 16px;
  font-weight: 600;
  text-transform: none;
}

.reset-button:hover {
  --background: linear-gradient(135deg, #5a6fd8, #6a4190);
}

.reset-spinner {
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
  .reset-card {
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