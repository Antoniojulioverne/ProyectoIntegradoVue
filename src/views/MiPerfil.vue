<template>
  <ion-page class="gs-profile-page" :class="{ 'gs-dark': isDarkMode }">
    <ion-header class="custom-header">
      <ion-toolbar class="custom-toolbar">
        <ion-buttons slot="start">
          <ion-menu-button :auto-hide="false"></ion-menu-button>
        </ion-buttons>
        <ion-title class="page-title">
          <div class="title-container">
            <span>Cuenta</span>
          </div>
        </ion-title>
      </ion-toolbar>
    </ion-header>
    
    <ion-content :fullscreen="true" class="gs-content">
      <!-- *** AGREGAR ION-REFRESHER *** -->
      <ion-refresher slot="fixed" @ionRefresh="handleRefresh($event)">
        <ion-refresher-content
          pulling-icon="person-circle-outline"
          pulling-text="Desliza para actualizar perfil"
          refreshing-spinner="circular"
          refreshing-text="Actualizando..."
        ></ion-refresher-content>
      </ion-refresher>
      <!-- Loading State -->
      <div v-if="isLoading && !hasValidData" class="gs-loading-container">
        <div class="gs-loading-spinner">
          <ion-spinner name="crescent"></ion-spinner>
        </div>
        <p class="gs-loading-text">Cargando perfil...</p>
      </div>

      <!-- Error State -->
      <div v-else-if="hasError" class="gs-error-container">
        <div class="gs-error-card">
          <ion-icon name="alert-circle-outline" class="gs-error-icon"></ion-icon>
          <h3 class="gs-error-title">Error al cargar el perfil</h3>
          <p class="gs-error-message">{{ error?.message || 'Ha ocurrido un error inesperado' }}</p>
          <button class="gs-button gs-button-primary" @click="loadUserProfile">
            <ion-icon name="refresh-outline"></ion-icon>
            Reintentar
          </button>
        </div>
      </div>

      <!-- Profile Content -->
      <div v-else-if="hasValidData" class="gs-profile-container">
        
        <!-- Avatar Section Card -->
        <div class="gs-section-card">
          <div class="gs-avatar-section">
            <div class="gs-avatar-container">
              <div class="gs-avatar-circle" @click="triggerFileInput()">
                <!-- Foto de perfil o iniciales -->
                <img 
                  v-if="userProfile.fotoPerfil" 
                  :src="userProfile.fotoPerfil" 
                  :alt="userProfile.username"
                  class="gs-avatar-image"
                />
                <div v-else class="gs-avatar-initials">
                  {{ getInitials(userProfile.username) }}
                </div>
                
                <!-- Overlay para edición -->
                <div class="gs-avatar-overlay">
                  <ion-icon name="camera-outline" class="gs-camera-icon"></ion-icon>
                  <span class="gs-overlay-text">Cambiar foto</span>
                </div>
              </div>
              
              <!-- Input oculto para foto -->
              <input 
                ref="fileInput"
                type="file"
                accept="image/*"
                @change="onFileSelected"
                style="display: none;"
              />
              
              <div 
                v-if="userProfile.emailVerificado" 
                class="gs-verified-badge"
              >
                <ion-icon name="checkmark-circle"></ion-icon>
              </div>
            </div>
            
            <!-- User Info -->
            <div class="gs-user-info">
              <h2 class="gs-username">{{ userProfile.username }}</h2>
              <p v-if="userProfile.fechaCreacion" class="gs-member-since">
                Miembro desde {{ formatDateForDisplay(userProfile.fechaCreacion) }}
              </p>
              <p v-else class="gs-member-since">
                Cuenta en proceso de verificación
              </p>
            </div>
          </div>
        </div>

        <!-- Photo Upload Progress Card -->
        <div v-if="isUploadingPhoto" class="gs-section-card">
          <div class="gs-upload-progress">
            <div class="gs-progress-content">
              <ion-progress-bar type="indeterminate" class="gs-progress-bar"></ion-progress-bar>
              <div class="gs-progress-text">
                <p class="gs-progress-title">Optimizando imagen...</p>
                <p class="gs-progress-subtitle">Reduciendo tamaño para carga más rápida</p>
              </div>
            </div>
          </div>
        </div>

        <!-- Profile Form Card -->
        <div class="gs-section-card">
          <div class="gs-form-card">
            <form @submit.prevent="saveProfile">
              
              <!-- Username Input -->
              <div class="gs-input-group">
                <label class="gs-input-label">Nombre de Usuario</label>
                <input
                  v-model="editForm.username"
                  :placeholder="userProfile.username"
                  maxlength="50"
                  class="gs-input gs-input-username"
                />
                <span class="gs-input-counter">{{ editForm.username.length }}/50</span>
              </div>

              <!-- Email (Solo lectura) -->
              <div class="gs-input-group">
                <label class="gs-input-label">Correo Electrónico</label>
                <div class="gs-input-with-badge">
                  <input
                    :value="userProfile.email"
                    readonly
                    class="gs-input gs-input-readonly"
                  />
                  <span 
                    v-if="userProfile.emailVerificado" 
                    class="gs-status-badge gs-status-verified"
                  >
                    <ion-icon name="checkmark-circle"></ion-icon>
                    Verificado
                  </span>
                  <span 
                    v-else 
                    class="gs-status-badge gs-status-pending"
                  >
                    <ion-icon name="time-outline"></ion-icon>
                    Sin verificar
                  </span>
                </div>
              </div>

              <!-- Navigation Tabs -->
              <div class="gs-tabs-container">
                <div class="gs-tabs">
                  <button 
                    type="button"
                    class="gs-tab"
                    :class="{ 'gs-tab-active': selectedSegment === 'info' }"
                    @click="selectedSegment = 'info'"
                  >
                    <ion-icon name="information-circle-outline"></ion-icon>
                    <span>Información</span>
                  </button>
                  <button 
                    type="button"
                    class="gs-tab"
                    :class="{ 'gs-tab-active': selectedSegment === 'skin' }"
                    @click="selectedSegment = 'skin'"
                  >
                    <ion-icon name="color-palette-outline"></ion-icon>
                    <span>Personalización</span>
                  </button>
                </div>
              </div>

              <!-- Tab Content -->
              <div class="gs-tab-content">
                
                <!-- Información General -->
                <div v-if="selectedSegment === 'info'" class="gs-tab-panel gs-fade-in">
                  <div class="gs-info-grid">
                    
                    <div class="gs-info-item">
                      <div class="gs-info-icon">
                        <ion-icon name="shield-checkmark-outline"></ion-icon>
                      </div>
                      <div class="gs-info-content">
                        <h4 class="gs-info-title">Estado de la cuenta</h4>
                        <p class="gs-info-description">
                          {{ userProfile.emailVerificado ? 'Cuenta verificada y activa' : 'Verificación de email pendiente' }}
                        </p>
                        <span 
                          class="gs-info-badge"
                          :class="userProfile.emailVerificado ? 'gs-info-badge-success' : 'gs-info-badge-warning'"
                        >
                          {{ userProfile.emailVerificado ? 'Verificada' : 'Pendiente' }}
                        </span>
                      </div>
                    </div>

                    <div v-if="userProfile.fechaCreacion" class="gs-info-item">
                      <div class="gs-info-icon">
                        <ion-icon name="calendar-outline"></ion-icon>
                      </div>
                      <div class="gs-info-content">
                        <h4 class="gs-info-title">Fecha de registro</h4>
                        <p class="gs-info-description">{{ formatDateForDisplay(userProfile.fechaCreacion) }}</p>
                      </div>
                    </div>

                    <div class="gs-info-item">
                      <div class="gs-info-icon">
                        <ion-icon name="color-palette-outline"></ion-icon>
                      </div>
                      <div class="gs-info-content">
                        <h4 class="gs-info-title">Skin Actual</h4>
                        <p class="gs-info-description">{{ getSkinDisplayName(userProfile.skin) || 'Sin skin seleccionada' }}</p>
                        <span 
                          v-if="userProfile.skin"
                          class="gs-skin-badge"
                          :class="`gs-skin-${userProfile.skin}`"
                        >
                          {{ userProfile.skin }}
                        </span>
                      </div>
                    </div>
                  </div>
                </div>

                <!-- Selector de Skin -->
                <div v-if="selectedSegment === 'skin'" class="gs-tab-panel gs-fade-in">
                  <div class="gs-skin-section">
                    <h3 class="gs-section-title">Personaliza tu Skin</h3>
                    <p class="gs-section-description">Elige el skin que mejor represente tu estilo de juego</p>
                    
                    <SkinSelection
                      v-model="editForm.skin"
                      @apply="applySkinChange"
                      class="gs-skin-selector"
                    />
                  </div>
                </div>

              </div>

            </form>
          </div>
        </div>

      </div>

      <!-- Empty state si no hay datos -->
      <div v-else class="gs-empty-state">
        <ion-icon name="person-circle-outline" class="gs-empty-icon"></ion-icon>
        <h3 class="gs-empty-title">No se pudo cargar el perfil</h3>
        <p class="gs-empty-description">Intenta recargar la página o verifica tu conexión</p>
        <button class="gs-button gs-button-primary" @click="loadUserProfile">
          <ion-icon name="refresh-outline"></ion-icon>
          Reintentar
        </button>
      </div>
    </ion-content>

    <!-- Floating Save Button -->
    <div class="gs-save-fab" :class="{ 'gs-save-fab-visible': hasChanges }">
      <button 
        class="gs-fab-button"
        @click="saveProfile"
        :disabled="!hasChanges || isSaving"
      >
        <ion-icon v-if="!isSaving" name="checkmark-outline" class="gs-fab-icon"></ion-icon>
        <ion-spinner v-else name="crescent" class="gs-fab-spinner"></ion-spinner>
        <span class="gs-fab-text">{{ isSaving ? 'Guardando...' : 'Guardar' }}</span>
      </button>
      
      <!-- Changes Indicator -->
      <div v-if="hasChanges && !isSaving" class="gs-changes-indicator">
        <ion-icon name="alert-circle-outline"></ion-icon>
        <span>Cambios pendientes</span>
      </div>
    </div>
  </ion-page>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, reactive } from 'vue';
import { useRouter } from 'vue-router';
import { useTheme } from '@/composables/useTheme';
import {
  IonPage, IonHeader, IonToolbar, IonTitle, IonContent,
  IonButtons, IonMenuButton, IonProgressBar, IonIcon, IonSpinner,toastController,IonRefresher,IonRefresherContent
} from '@ionic/vue';

// Componentes reutilizables
import SkinSelection from '@/views/SkinSelection.vue';

// Composables y utilidades
import { useApi } from '@/composables/useApi';
import { getInitials } from '@/utils/userUtils';
import { validateToken } from '@/utils/validation';
import config from '@/config/config';

// Interfaces
interface UserProfile {
  usuarioId: number;
  username: string;
  email: string;
  skin: string;
  fotoPerfil?: string;
  emailVerificado: boolean;
  fechaCreacion?: string | null;
}

interface EditForm {
  username: string;
  skin: string;
}
const handleRefresh = async (event: any) => {
  try {
    console.log('🔄 Actualizando Perfil...')
    
    // Recargar datos del perfil
    await loadUserProfile()
    
    const toast = await toastController.create({
      message: '✅ Perfil actualizado',
      duration: 2000,
      position: 'top',
      color: 'success'
    })
    await toast.present()
    
  } catch (error) {
    console.error('❌ Error actualizando perfil:', error)
    
    const toast = await toastController.create({
      message: '❌ Error al actualizar perfil',
      duration: 3000,
      position: 'top',
      color: 'danger'
    })
    await toast.present()
    
  } finally {
    event.target.complete()
  }
}


// Composables
const router = useRouter();
const { isDarkMode } = useTheme();
const { 
  makeRequest, 
  isLoading, 
  error, 
  hasError, 
  showToast 
} = useApi();

// Reactive State
const isSaving = ref(false);
const isUploadingPhoto = ref(false);
const selectedPhotoBase64 = ref<string | null>(null);
const fileInput = ref<HTMLInputElement>();
const selectedSegment = ref('info');

// Valores originales para comparar cambios
const originalValues = ref<EditForm>({
  username: '',
  skin: ''
});

const userProfile = ref<UserProfile>({
  usuarioId: 0,
  username: '',
  email: '',
  skin: '',
  fotoPerfil: '',
  emailVerificado: false,
  fechaCreacion: ''
});

const editForm = reactive<EditForm>({
  username: '',
  skin: ''
});

// Computed Properties
const hasValidData = computed(() => {
  const profile = userProfile.value;
  
  if (!profile || !profile.username) {
    console.log('❌ ProfilePage: No hay username');
    return false;
  }
  
  console.log('✅ ProfilePage: Datos válidos del perfil');
  return true;
});

const hasChanges = computed(() => {
  const usernameChanged = editForm.username.trim() !== originalValues.value.username && editForm.username.trim().length >= 3;
  const skinChanged = editForm.skin !== originalValues.value.skin && editForm.skin !== '';
  const photoChanged = selectedPhotoBase64.value !== null;
  
  return usernameChanged || skinChanged || photoChanged;
});

// Methods
const getCurrentUser = () => {
  const userData = localStorage.getItem(config.storage.user);
  return userData ? JSON.parse(userData) : null;
};

const formatDateForDisplay = (dateString: string): string => {
  if (!dateString) return '';
  
  try {
    // Limpiar microsegundos si existen (ej: 2025-05-30 21:14:18.684143)
    let cleanDateString = dateString;
    if (dateString.includes('.')) {
      cleanDateString = dateString.split('.')[0]; // Quitar microsegundos
    }
    
    // Reemplazar espacio con T para ISO format si es necesario
    if (cleanDateString.includes(' ')) {
      cleanDateString = cleanDateString.replace(' ', 'T');
    }
    
    const date = new Date(cleanDateString);
    
    // Verificar si la fecha es válida
    if (isNaN(date.getTime())) {
      console.warn('Fecha inválida:', dateString);
      return 'Fecha no disponible';
    }
    
    return date.toLocaleDateString('es-ES', {
      day: '2-digit',
      month: 'long',
      year: 'numeric'
    });
  } catch (error) {
    console.error('Error formateando fecha:', error);
    return 'Fecha no disponible';
  }
};

const triggerFileInput = () => {
  if (fileInput.value) {
    fileInput.value.click();
  }
};

const onFileSelected = async (event: Event) => {
  const target = event.target as HTMLInputElement;
  const file = target.files?.[0];
  
  if (!file) return;
  
  // Validar archivo
  if (!file.type.startsWith('image/')) {
    await showToast('Solo se permiten archivos de imagen', 'warning');
    return;
  }
  
  if (file.size > 5 * 1024 * 1024) { // 5MB
    await showToast('El archivo no debe superar 5MB', 'warning');
    return;
  }
  
  await convertToBase64(file);
};

const convertToBase64 = (file: File): Promise<void> => {
  return new Promise((resolve, reject) => {
    isUploadingPhoto.value = true;
    
    // Crear elemento de imagen para redimensionar
    const img = new Image();
    const canvas = document.createElement('canvas');
    const ctx = canvas.getContext('2d');
    
    img.onload = () => {
      // Calcular nuevas dimensiones basadas en el tamaño del archivo
      let maxSize = 400;
      let quality = 0.8;
      
      // Para archivos muy grandes (como fotos de iPhone), usar compresión más agresiva
      if (file.size > 3 * 1024 * 1024) { // Archivos > 3MB
        maxSize = 300;
        quality = 0.6; // Menor calidad para reducir tamaño
      } else if (file.size > 1.5 * 1024 * 1024) { // Archivos > 1.5MB
        maxSize = 350;
        quality = 0.7;
      }
      
      let { width, height } = img;
      
      // Redimensionar manteniendo proporción
      if (width > height) {
        if (width > maxSize) {
          height = (height * maxSize) / width;
          width = maxSize;
        }
      } else {
        if (height > maxSize) {
          width = (width * maxSize) / height;
          height = maxSize;
        }
      }
      
      // Redimensionar imagen
      canvas.width = width;
      canvas.height = height;
      ctx?.drawImage(img, 0, 0, width, height);
      
      // Convertir a Base64 con calidad ajustada
      const base64 = canvas.toDataURL('image/jpeg', quality);
      
      // Verificar que el resultado no sea demasiado grande
      if (base64.length > 6 * 1024 * 1024) { // Si el Base64 > 6MB
        // Intentar con menor calidad
        const smallerBase64 = canvas.toDataURL('image/jpeg', 0.5);
        selectedPhotoBase64.value = smallerBase64;
        userProfile.value.fotoPerfil = smallerBase64;
      } else {
        selectedPhotoBase64.value = base64;
        userProfile.value.fotoPerfil = base64;
      }
      
      isUploadingPhoto.value = false;
      console.log(`📸 Imagen procesada: ${Math.round(base64.length / 1024)}KB`);
      resolve();
    };
    
    img.onerror = () => {
      isUploadingPhoto.value = false;
      showToast('Error al procesar la imagen', 'danger');
      reject();
    };
    
    // Cargar imagen desde el archivo
    const reader = new FileReader();
    reader.onload = (e) => {
      img.src = e.target?.result as string;
    };
    reader.readAsDataURL(file);
  });
};

const loadUserProfile = async () => {
  try {
    if (!validateToken()) {
      await showToast('Sesión expirada. Por favor, inicia sesión nuevamente', 'danger');
      router.push('/login');
      return;
    }

    const currentUser = getCurrentUser();
    if (!currentUser?.usuarioId) {
      await showToast('No se encontró información del usuario', 'danger');
      router.push('/login');
      return;
    }

    console.log('🔄 Cargando perfil del usuario:', currentUser.usuarioId);

    // Cargar perfil del usuario
    const profile = await makeRequest<UserProfile>('GET', `/usuario/${currentUser.usuarioId}`);
    
    userProfile.value = {
      ...profile,
      fechaCreacion: profile.fechaCreacion || null
    };

    // Inicializar formulario con datos actuales
    editForm.username = profile.username;
    editForm.skin = profile.skin || '';

    // Guardar valores originales para comparar cambios
    originalValues.value = {
      username: profile.username,
      skin: profile.skin || ''
    };

    console.log('✅ Perfil cargado exitosamente:', userProfile.value);

  } catch (err: any) {
    console.error('Error cargando perfil:', err);
    await showToast('Error al cargar el perfil del usuario', 'danger');
  }
};

const saveProfile = async () => {
  if (!hasChanges.value) return;

  isSaving.value = true;
  
  try {
    const currentUser = getCurrentUser();
    const updateData: any = {};
    
    // Solo incluir campos que han cambiado
    if (editForm.username.trim() !== originalValues.value.username && editForm.username.trim().length >= 3) {
      updateData.username = editForm.username.trim();
    }
    
    if (editForm.skin !== originalValues.value.skin && editForm.skin) {
      updateData.skin = editForm.skin;
    }

    // Incluir foto si se ha seleccionado una nueva
    if (selectedPhotoBase64.value) {
      updateData.fotoPerfil = selectedPhotoBase64.value;
    }

    // Solo hacer request si hay cambios
    if (Object.keys(updateData).length > 0) {
      console.log('💾 Guardando cambios:', Object.keys(updateData));
      
      await makeRequest('PUT', `/usuario/${currentUser.usuarioId}`, updateData);
      
      // Actualizar estado local
      if (updateData.username) {
        userProfile.value.username = updateData.username;
        originalValues.value.username = updateData.username;
      }
      if (updateData.skin) {
        userProfile.value.skin = updateData.skin;
        originalValues.value.skin = updateData.skin;
      }
      if (updateData.fotoPerfil) {
        userProfile.value.fotoPerfil = updateData.fotoPerfil;
      }

      // Actualizar localStorage con los nuevos datos
      const updatedUser = { ...currentUser, ...updateData };
      localStorage.setItem(config.storage.user, JSON.stringify(updatedUser));
    }

    await showToast('Perfil actualizado correctamente', 'success');
    selectedPhotoBase64.value = null; // Limpiar foto seleccionada

  } catch (err: any) {
    console.error('Error guardando perfil:', err);
    await showToast(err.message || 'Error al guardar los cambios', 'danger');
  } finally {
    isSaving.value = false;
  }
};

const applySkinChange = async () => {
  console.log('Skin seleccionada:', editForm.skin);
};

const getSkinDisplayName = (skinValue: string): string => {
  const skinMap: Record<string, string> = {
    'blue': 'Azul (Sonic)',
    'red': 'Rojo (Mario)'
  };
  return skinMap[skinValue] || skinValue;
};

// Lifecycle
onMounted(() => {
  loadUserProfile();
});
</script>

<style scoped>
/* Importar Design System desde styles/ */
 @Import '../styles/design-system.css';

/* === PROFILE PAGE CUSTOM STYLES === */
/* Solo agregamos estilos específicos que no están en el design system */

.gs-profile-page {
  background: var(--gs-bg-primary);
}

.gs-content {
  --background: var(--gs-bg-primary);
}

/* === HEADER STYLES === */
.gs-header {
  position: relative;
  z-index: var(--gs-z-header, 50);
}

.gs-toolbar {
  --background: var(--gs-bg-secondary, #ffffff);
  --border-color: var(--gs-border-primary, #e5e7eb);
  --color: var(--gs-text-primary, #111827);
  border-bottom: 1px solid var(--border-color);
  min-height: 56px;
}

.gs-title {
  font-size: var(--gs-text-lg, 1.125rem);
  font-weight: var(--gs-font-semibold, 600);
  color: var(--gs-text-primary, #111827);
}

.gs-menu-button {
  color: var(--gs-primary-500, #3b82f6);
  --color: var(--gs-primary-500, #3b82f6);
}

/* === LOADING STATES === */
.gs-loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--gs-space-3xl);
  text-align: center;
}

.gs-loading-spinner {
  margin-bottom: var(--gs-space-lg);
  color: var(--gs-primary-500);
  font-size: 2rem;
}

.gs-loading-text {
  color: var(--gs-text-secondary);
  font-size: var(--gs-text-base);
}

/* === ERROR STATES === */
.gs-error-container {
  padding: var(--gs-space-xl);
  display: flex;
  justify-content: center;
}

.gs-error-card {
  @apply gs-card;
  padding: var(--gs-space-2xl);
  text-align: center;
  max-width: 400px;
}

.gs-error-icon {
  font-size: 3rem;
  color: var(--gs-error-500);
  margin-bottom: var(--gs-space-lg);
}

.gs-error-title {
  font-size: var(--gs-text-xl);
  font-weight: var(--gs-font-semibold);
  color: var(--gs-text-primary);
  margin-bottom: var(--gs-space-sm);
}

.gs-error-message {
  color: var(--gs-text-secondary);
  margin-bottom: var(--gs-space-lg);
}

/* === PROFILE CONTAINER === */
.gs-profile-container {
  max-width: 600px;
  margin: 0 auto;
  padding: var(--gs-space-md);
  padding-bottom: 120px; /* Espacio para FAB */
}

/* === AVATAR SECTION === */
.gs-section-card {
  @apply gs-card;
  border-radius: var(--gs-radius-2xl);
  margin-bottom: var(--gs-space-xl);
  padding: var(--gs-space-xl);
  background: var(--gs-bg-secondary);
  border: 1px solid var(--gs-border-primary);
  box-shadow: var(--gs-shadow-sm);
  transition: all var(--gs-transition-fast);
}

.gs-section-card:hover {
  box-shadow: var(--gs-shadow-md);
  transform: translateY(-1px);
}

.gs-avatar-section {
  text-align: center;
}

.gs-avatar-container {
  position: relative;
  display: inline-block;
  margin-bottom: var(--gs-space-lg);
}

.gs-avatar-circle {
  width: 120px;
  height: 120px;
  border-radius: var(--gs-radius-full);
  border: 4px solid var(--gs-primary-500);
  background: var(--gs-primary-50);
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  cursor: pointer;
  transition: all var(--gs-transition-normal);
}

.gs-avatar-circle:hover {
  transform: scale(1.05);
  box-shadow: var(--gs-shadow-lg);
}

.gs-avatar-circle:hover .gs-avatar-overlay {
  opacity: 1;
}

.gs-avatar-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.gs-avatar-initials {
  font-size: var(--gs-text-3xl);
  font-weight: var(--gs-font-bold);
  color: var(--gs-primary-600);
}

.gs-avatar-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border-radius: var(--gs-radius-full);
  opacity: 0;
  transition: opacity var(--gs-transition-normal);
}

.gs-camera-icon {
  font-size: var(--gs-text-2xl);
  color: white;
  margin-bottom: var(--gs-space-xs);
}

.gs-overlay-text {
  color: white;
  font-size: var(--gs-text-sm);
  font-weight: var(--gs-font-medium);
}

.gs-verified-badge {
  position: absolute;
  bottom: 8px;
  right: 8px;
  background: var(--gs-secondary-500);
  color: white;
  border-radius: var(--gs-radius-full);
  padding: var(--gs-space-xs);
  border: 2px solid var(--gs-bg-secondary);
  font-size: var(--gs-text-lg);
}

/* === USER INFO === */
.gs-user-info {
  text-align: center;
}

.gs-username {
  font-size: var(--gs-text-2xl);
  font-weight: var(--gs-font-bold);
  color: var(--gs-text-primary);
  margin-bottom: var(--gs-space-xs);
}

.gs-member-since {
  color: var(--gs-text-secondary);
  font-size: var(--gs-text-sm);
  margin: 0;
}

/* === UPLOAD PROGRESS === */
.gs-upload-progress {
  margin-bottom: var(--gs-space-xl);
}

.gs-progress-content {
  @apply gs-card;
  padding: var(--gs-space-lg);
  text-align: center;
}

.gs-progress-bar {
  margin-bottom: var(--gs-space-md);
  --color: var(--gs-primary-500);
}

.gs-progress-title {
  font-size: var(--gs-text-base);
  font-weight: var(--gs-font-medium);
  color: var(--gs-text-primary);
  margin-bottom: var(--gs-space-xs);
}

.gs-progress-subtitle {
  font-size: var(--gs-text-sm);
  color: var(--gs-text-secondary);
  margin: 0;
}

/* === FORM CARD === */
.gs-form-card {
  @apply gs-card;
  border-radius: var(--gs-radius-xl);
  padding: var(--gs-space-xl);
}

/* === INPUT GROUPS === */
.gs-input-group {
  margin-bottom: var(--gs-space-lg);
}

.gs-input-label {
  display: block;
  font-size: var(--gs-text-sm);
  font-weight: var(--gs-font-medium);
  color: var(--gs-text-primary);
  margin-bottom: var(--gs-space-sm);
}

.gs-input {
  @apply gs-input;
}

.gs-input-username {
  background: var(--gs-bg-primary);
}

.gs-input-readonly {
  background: var(--gs-gray-50);
  cursor: not-allowed;
}

.gs-input-counter {
  display: block;
  font-size: var(--gs-text-xs);
  color: var(--gs-text-tertiary);
  text-align: right;
  margin-top: var(--gs-space-xs);
}

.gs-input-with-badge {
  position: relative;
}

.gs-input-with-badge .gs-input {
  padding-right: 120px;
}

.gs-status-badge {
  position: absolute;
  right: var(--gs-space-sm);
  top: 50%;
  transform: translateY(-50%);
  display: flex;
  align-items: center;
  gap: var(--gs-space-xs);
  padding: var(--gs-space-xs) var(--gs-space-sm);
  border-radius: var(--gs-radius-md);
  font-size: var(--gs-text-xs);
  font-weight: var(--gs-font-medium);
}

.gs-status-verified {
  background: var(--gs-secondary-50);
  color: var(--gs-secondary-700);
}

.gs-status-pending {
  background: var(--gs-warning-50);
  color: var(--gs-warning-700);
}

/* === TABS === */
.gs-tabs-container {
  margin: var(--gs-space-xl) 0;
}

.gs-tabs {
  display: flex;
  background: var(--gs-bg-primary);
  border-radius: var(--gs-radius-lg);
  padding: var(--gs-space-xs);
  border: 1px solid var(--gs-border-primary);
}

.gs-tab {
  @apply gs-button;
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--gs-space-sm);
  padding: var(--gs-space-md);
  background: transparent;
  border: none;
  border-radius: var(--gs-radius-md);
  color: var(--gs-text-secondary);
}

.gs-tab:hover {
  background: var(--gs-bg-secondary);
  color: var(--gs-text-primary);
}

.gs-tab-active {
  @apply gs-button-primary;
}

/* === TAB CONTENT === */
.gs-tab-content {
  margin-top: var(--gs-space-lg);
}

.gs-tab-panel {
  @apply gs-fade-in;
}

/* === INFO GRID === */
.gs-info-grid {
  display: flex;
  flex-direction: column;
  gap: var(--gs-space-lg);
}

.gs-info-item {
  @apply gs-card;
  display: flex;
  align-items: flex-start;
  gap: var(--gs-space-md);
  padding: var(--gs-space-lg);
  transition: all var(--gs-transition-fast);
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
.gs-info-item:hover {
  transform: translateY(-1px);
}

.gs-info-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  background: var(--gs-primary-50);
  color: var(--gs-primary-500);
  border-radius: var(--gs-radius-lg);
  font-size: var(--gs-text-xl);
}

.gs-info-content {
  flex: 1;
}

.gs-info-title {
  font-size: var(--gs-text-base);
  font-weight: var(--gs-font-semibold);
  color: var(--gs-text-primary);
  margin-bottom: var(--gs-space-xs);
}

.gs-info-description {
  font-size: var(--gs-text-sm);
  color: var(--gs-text-secondary);
  margin-bottom: var(--gs-space-sm);
}

.gs-info-badge {
  display: inline-flex;
  align-items: center;
  padding: var(--gs-space-xs) var(--gs-space-sm);
  border-radius: var(--gs-radius-md);
  font-size: var(--gs-text-xs);
  font-weight: var(--gs-font-medium);
}

.gs-info-badge-success {
  background: var(--gs-secondary-50);
  color: var(--gs-secondary-700);
}

.gs-info-badge-warning {
  background: var(--gs-warning-50);
  color: var(--gs-warning-700);
}


.custom-toolbar {
  --background: rgba(0, 0, 0);
  --color: white;
  --border-color: rgba(255, 255, 255, 1);
}
.gs-skin-badge {
  display: inline-flex;
  align-items: center;
  padding: var(--gs-space-xs) var(--gs-space-sm);
  border-radius: var(--gs-radius-md);
  font-size: var(--gs-text-xs);
  font-weight: var(--gs-font-medium);
  text-transform: uppercase;
}

.gs-skin-blue {
  background: var(--gs-primary-50);
  color: var(--gs-primary-700);
}

.gs-skin-red {
  background: var(--gs-error-50);
  color: var(--gs-error-700);
}

/* === SKIN SECTION === */
.gs-skin-section {
  text-align: center;
}

.gs-section-title {
  font-size: var(--gs-text-xl);
  font-weight: var(--gs-font-semibold);
  color: var(--gs-text-primary);
  margin-bottom: var(--gs-space-sm);
}

.gs-section-description {
  font-size: var(--gs-text-sm);
  color: var(--gs-text-secondary);
  margin-bottom: var(--gs-space-xl);
}

.gs-skin-selector {
  margin: 0;
}

/* === FLOATING ACTION BUTTON === */
.gs-save-fab {
  position: fixed;
  bottom: var(--gs-space-xl);
  left: 50%;
  transform: translateX(-50%) translateY(100px);
  opacity: 0;
  visibility: hidden;
  transition: all var(--gs-transition-normal);
  z-index: var(--gs-z-fab);
}

.gs-save-fab-visible {
  transform: translateX(-50%) translateY(0);
  opacity: 1;
  visibility: visible;
}

.gs-fab-button {
  @apply gs-button gs-button-primary;
  display: flex;
  align-items: center;
  gap: var(--gs-space-sm);
  padding: var(--gs-space-md) var(--gs-space-xl);
  border-radius: var(--gs-radius-full);
  font-size: var(--gs-text-base);
  font-weight: var(--gs-font-semibold);
  box-shadow: var(--gs-shadow-xl);
  min-width: 140px;
  justify-content: center;
}

.gs-fab-button:hover:not(:disabled) {
  transform: translateY(-2px);
}

.gs-fab-icon,
.gs-fab-spinner {
  font-size: var(--gs-text-lg);
}

.gs-fab-text {
  font-size: var(--gs-text-sm);
}

.gs-changes-indicator {
  position: absolute;
  top: -45px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: var(--gs-space-xs);
  padding: var(--gs-space-sm) var(--gs-space-md);
  background: var(--gs-warning-500);
  color: white;
  border-radius: var(--gs-radius-lg);
  font-size: var(--gs-text-xs);
  font-weight: var(--gs-font-medium);
  white-space: nowrap;
  box-shadow: var(--gs-shadow-md);
}

.gs-changes-indicator::after {
  content: '';
  position: absolute;
  top: 100%;
  left: 50%;
  transform: translateX(-50%);
  width: 0;
  height: 0;
  border-left: 6px solid transparent;
  border-right: 6px solid transparent;
  border-top: 6px solid var(--gs-warning-500);
}

/* === EMPTY STATE === */
.gs-empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--gs-space-3xl);
  text-align: center;
}

.gs-empty-icon {
  font-size: 4rem;
  color: var(--gs-text-tertiary);
  margin-bottom: var(--gs-space-xl);
}

.gs-empty-title {
  font-size: var(--gs-text-xl);
  font-weight: var(--gs-font-semibold);
  color: var(--gs-text-primary);
  margin-bottom: var(--gs-space-sm);
}

.gs-empty-description {
  color: var(--gs-text-secondary);
  margin-bottom: var(--gs-space-xl);
}

/* === RESPONSIVE === */
@media (max-width: 768px) {
  .gs-profile-container {
    padding: var(--gs-space-sm);
    padding-bottom: 140px;
  }
  
  .gs-avatar-circle {
    width: 100px;
    height: 100px;
  }
  
  .gs-form-card {
    padding: var(--gs-space-lg);
  }
  
  .gs-fab-button {
    padding: var(--gs-space-sm) var(--gs-space-lg);
    font-size: var(--gs-text-sm);
  }
  
  .gs-info-item {
    padding: var(--gs-space-md);
  }
  
  .gs-tabs {
    padding: 2px;
  }
  
  .gs-tab {
    padding: var(--gs-space-sm);
    font-size: var(--gs-text-xs);
  }
}

/* === DARK MODE OVERRIDES === */
.gs-dark .gs-input-readonly {
  background: var(--gs-gray-800);
}

.gs-dark .gs-info-icon {
  background: var(--gs-primary-900);
  color: var(--gs-primary-400);
}

.gs-dark .gs-toolbar {
  --background: var(--gs-bg-secondary);
  --border-color: var(--gs-border-primary);
}

.gs-dark .gs-title {
  color: var(--gs-text-primary);
}

/* === IONIC COMPONENT OVERRIDES === */
ion-header.gs-header {
  position: relative !important;
  z-index: 50 !important;
}

ion-toolbar.gs-toolbar {
  --background: var(--gs-bg-secondary) !important;
  --border-color: var(--gs-border-primary) !important;
  --color: var(--gs-text-primary) !important;
}

ion-title.gs-title {
  color: var(--gs-text-primary) !important;
  font-size: var(--gs-text-lg) !important;
  font-weight: var(--gs-font-semibold) !important;

  font-size: var(--gs-text-xs);
  color: var(--gs-text-tertiary);
  text-align: right;
  margin-top: var(--gs-space-xs);
}

.gs-input-with-badge {
  position: relative;
}

.gs-input-with-badge .gs-input {
  padding-right: 120px;
}

.gs-status-badge {
  position: absolute;
  right: var(--gs-space-sm);
  top: 50%;
  transform: translateY(-50%);
  display: flex;
  align-items: center;
  gap: var(--gs-space-xs);
  padding: var(--gs-space-xs) var(--gs-space-sm);
  border-radius: var(--gs-radius-md);
  font-size: var(--gs-text-xs);
  font-weight: var(--gs-font-medium);
}

.gs-status-verified {
  background: var(--gs-secondary-50);
  color: var(--gs-secondary-700);
}

.gs-status-pending {
  background: var(--gs-warning-50);
  color: var(--gs-warning-700);
}

/* === TABS === */
.gs-tabs-container {
  margin: var(--gs-space-xl) 0;
}

.gs-tabs {
  display: flex;
  background: var(--gs-bg-primary);
  border-radius: var(--gs-radius-lg);
  padding: var(--gs-space-xs);
  border: 1px solid var(--gs-border-primary);
}

.gs-tab {
  @apply gs-button;
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--gs-space-sm);
  padding: var(--gs-space-md);
  background: transparent;
  border: none;
  border-radius: var(--gs-radius-md);
  color: var(--gs-text-secondary);
}

.gs-tab:hover {
  background: var(--gs-bg-secondary);
  color: var(--gs-text-primary);
}

.gs-tab-active {
  @apply gs-button-primary;
}

/* === TAB CONTENT === */
.gs-tab-content {
  margin-top: var(--gs-space-lg);
}

.gs-tab-panel {
  @apply gs-fade-in;
}

/* === INFO GRID === */
.gs-info-grid {
  display: flex;
  flex-direction: column;
  gap: var(--gs-space-lg);
}

.gs-info-item {
  @apply gs-card;
  display: flex;
  align-items: flex-start;
  gap: var(--gs-space-md);
  padding: var(--gs-space-lg);
  transition: all var(--gs-transition-fast);
}

.gs-info-item:hover {
  transform: translateY(-1px);
}

.gs-info-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  background: var(--gs-primary-50);
  color: var(--gs-primary-500);
  border-radius: var(--gs-radius-lg);
  font-size: var(--gs-text-xl);
}

.gs-info-content {
  flex: 1;
}

.gs-info-title {
  font-size: var(--gs-text-base);
  font-weight: var(--gs-font-semibold);
  color: var(--gs-text-primary);
  margin-bottom: var(--gs-space-xs);
}

.gs-info-description {
  font-size: var(--gs-text-sm);
  color: var(--gs-text-secondary);
  margin-bottom: var(--gs-space-sm);
}

.gs-info-badge {
  display: inline-flex;
  align-items: center;
  padding: var(--gs-space-xs) var(--gs-space-sm);
  border-radius: var(--gs-radius-md);
  font-size: var(--gs-text-xs);
  font-weight: var(--gs-font-medium);
}

.gs-info-badge-success {
  background: var(--gs-secondary-50);
  color: var(--gs-secondary-700);
}

.gs-info-badge-warning {
  background: var(--gs-warning-50);
  color: var(--gs-warning-700);
}

.gs-skin-badge {
  display: inline-flex;
  align-items: center;
  padding: var(--gs-space-xs) var(--gs-space-sm);
  border-radius: var(--gs-radius-md);
  font-size: var(--gs-text-xs);
  font-weight: var(--gs-font-medium);
  text-transform: uppercase;
}

.gs-skin-blue {
  background: var(--gs-primary-50);
  color: var(--gs-primary-700);
}

.gs-skin-red {
  background: var(--gs-error-50);
  color: var(--gs-error-700);
}

/* === SKIN SECTION === */
.gs-skin-section {
  text-align: center;
}

.gs-section-title {
  font-size: var(--gs-text-xl);
  font-weight: var(--gs-font-semibold);
  color: var(--gs-text-primary);
  margin-bottom: var(--gs-space-sm);
}

.gs-section-description {
  font-size: var(--gs-text-sm);
  color: var(--gs-text-secondary);
  margin-bottom: var(--gs-space-xl);
}

.gs-skin-selector {
  margin: 0;
}

/* === FLOATING ACTION BUTTON === */
.gs-save-fab {
  position: fixed;
  bottom: var(--gs-space-xl);
  left: 50%;
  transform: translateX(-50%) translateY(100px);
  opacity: 0;
  visibility: hidden;
  transition: all var(--gs-transition-normal);
  z-index: var(--gs-z-fab);
}

.gs-save-fab-visible {
  transform: translateX(-50%) translateY(0);
  opacity: 1;
  visibility: visible;
}

.gs-fab-button {
  @apply gs-button gs-button-primary;
  display: flex;
  align-items: center;
  gap: var(--gs-space-sm);
  padding: var(--gs-space-md) var(--gs-space-xl);
  border-radius: var(--gs-radius-full);
  font-size: var(--gs-text-base);
  font-weight: var(--gs-font-semibold);
  box-shadow: var(--gs-shadow-xl);
  min-width: 140px;
  justify-content: center;
}

.gs-fab-button:hover:not(:disabled) {
  transform: translateY(-2px);
}

.gs-fab-icon,
.gs-fab-spinner {
  font-size: var(--gs-text-lg);
}

.gs-fab-text {
  font-size: var(--gs-text-sm);
}

.gs-changes-indicator {
  position: absolute;
  top: -45px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: var(--gs-space-xs);
  padding: var(--gs-space-sm) var(--gs-space-md);
  background: var(--gs-warning-500);
  color: white;
  border-radius: var(--gs-radius-lg);
  font-size: var(--gs-text-xs);
  font-weight: var(--gs-font-medium);
  white-space: nowrap;
  box-shadow: var(--gs-shadow-md);
}

.gs-changes-indicator::after {
  content: '';
  position: absolute;
  top: 100%;
  left: 50%;
  transform: translateX(-50%);
  width: 0;
  height: 0;
  border-left: 6px solid transparent;
  border-right: 6px solid transparent;
  border-top: 6px solid var(--gs-warning-500);
}

/* === EMPTY STATE === */
.gs-empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--gs-space-3xl);
  text-align: center;
}

.gs-empty-icon {
  font-size: 4rem;
  color: var(--gs-text-tertiary);
  margin-bottom: var(--gs-space-xl);
}

.gs-empty-title {
  font-size: var(--gs-text-xl);
  font-weight: var(--gs-font-semibold);
  color: var(--gs-text-primary);
  margin-bottom: var(--gs-space-sm);
}

.gs-empty-description {
  color: var(--gs-text-secondary);
  margin-bottom: var(--gs-space-xl);
}

/* === RESPONSIVE === */
@media (max-width: 768px) {
  .gs-profile-container {
    padding: var(--gs-space-sm);
    padding-bottom: 140px;
  }
  
  .gs-avatar-circle {
    width: 100px;
    height: 100px;
  }
  
  .gs-form-card {
    padding: var(--gs-space-lg);
  }
  
  .gs-fab-button {
    padding: var(--gs-space-sm) var(--gs-space-lg);
    font-size: var(--gs-text-sm);
  }
  
  .gs-info-item {
    padding: var(--gs-space-md);
  }
  
  .gs-tabs {
    padding: 2px;
  }
  
  .gs-tab {
    padding: var(--gs-space-sm);
    font-size: var(--gs-text-xs);
  }
}

/* === DARK MODE OVERRIDES === */
.gs-dark .gs-input-readonly {
  background: var(--gs-gray-800);
}

.gs-dark .gs-info-icon {
  background: var(--gs-primary-900);
  color: var(--gs-primary-400);
}
</style>