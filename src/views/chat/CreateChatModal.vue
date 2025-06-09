<!-- Actualizado: Se ha añadido el componente ProfileAvatar para las fotos de perfil -->
<template>
  <ion-modal
    :is-open="isOpen"
    @will-dismiss="$emit('close')"
    class="create-chat-modal"
  >
    <ion-header>
      <ion-toolbar>
        <ion-title>{{ chatType === 'PRIVADO' ? 'Nuevo Chat' : 'Nuevo Grupo' }}</ion-title>
        <ion-buttons slot="end">
          <ion-button @click="$emit('close')">
            <ion-icon slot="icon-only" name="close-outline"></ion-icon>
          </ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>
    <ion-content class="ion-padding">
      <div class="gs-container">
        <!-- Chat Type Selector -->
        <div class="form-section">
          <ion-segment v-model="chatType" class="chat-type-segment">
            <ion-segment-button value="PRIVADO" class="segment-button">
              <ion-label>Chat Privado</ion-label>
            </ion-segment-button>
            <ion-segment-button value="GRUPO" class="segment-button">
              <ion-label>Grupo</ion-label>
            </ion-segment-button>
          </ion-segment>
        </div>

        <!-- Group Name Input -->
        <div v-if="chatType === 'GRUPO'" class="form-section">
          <label class="gs-label">Nombre del Grupo</label>
          <input
            v-model="groupName"
            type="text"
            class="gs-input"
            placeholder="Ingresa un nombre para el grupo"
            maxlength="50"
          />
          <div class="input-helper">
            <span class="character-count">{{ groupName.length }}/50</span>
          </div>
        </div>

        <!-- Selected Users -->
        <div v-if="selectedUsers.length > 0" class="form-section">
          <label class="gs-label">
            {{ chatType === 'PRIVADO' ? 'Contacto Seleccionado' : 'Participantes Seleccionados' }}
          </label>
          <div class="selected-users-list">
            <div 
              v-for="user in selectedUsers" 
              :key="user.usuarioId"
              class="selected-user"
            >
              <div class="user-info">
                <!-- Usar ProfileAvatar para mostrar foto de perfil -->
                <ProfileAvatar
                  :profile-image="user.fotoPerfil"
                  :username="user.username"
                  :size="40"
                  :is-verified="user.emailVerificado"
                  :show-verification="false"
                />
                <div class="user-details">
                  <h3 class="username">{{ user.username }}</h3>
                  <p class="email">{{ user.email }}</p>
                  <span 
                    v-if="user.estadoRelacion && user.estadoRelacion !== 'SIN_RELACION'" 
                    :class="['relation-badge', relationClass(user.estadoRelacion)]"
                  >
                    {{ relationText(user.estadoRelacion) }}
                  </span>
                </div>
              </div>
              <button 
                class="remove-btn" 
                @click="removeUser(user)"
                :disabled="isCreating"
              >
                <ion-icon name="close-outline"></ion-icon>
              </button>
            </div>
          </div>
        </div>

        <!-- Search Section -->
        <div class="form-section">
          <label class="gs-label">
            {{ selectedUsers.length > 0 ? 'Añadir más usuarios' : 'Buscar usuarios' }}
          </label>
          <div class="search-input-container">
            <input
              v-model="searchTerm"
              type="text"
              class="gs-input search-input"
              placeholder="Buscar por nombre o email"
              @input="debouncedSearch"
            />
            <ion-icon name="search" class="search-icon"></ion-icon>
          </div>
        </div>

        <!-- Search Results -->
        <div v-if="availableUsers.length > 0" class="form-section">
          <div class="search-results">
            <div 
              v-for="user in availableUsers" 
              :key="user.usuarioId"
              class="user-card"
              @click="selectUser(user)"
            >
              <div class="user-info">
                <!-- Usar ProfileAvatar para mostrar foto de perfil -->
                <ProfileAvatar
                  :profile-image="user.fotoPerfil"
                  :username="user.username"
                  :size="40"
                  :is-verified="user.emailVerificado"
                  :show-verification="false"
                />
                <div class="user-details">
                  <h3 class="username">{{ user.username }}</h3>
                  <p class="email">{{ user.email }}</p>
                  <span 
                    v-if="user.estadoRelacion && user.estadoRelacion !== 'SIN_RELACION'" 
                    :class="['relation-badge', relationClass(user.estadoRelacion)]"
                  >
                    {{ relationText(user.estadoRelacion) }}
                  </span>
                </div>
              </div>
              <div class="user-actions">
                <ion-icon 
                  :name="isUserSelected(user) ? 
                  'checkmark-circle' : 'add-circle-outline'"
                  class="action-icon"
                  :class="{ 'selected': isUserSelected(user) }"
                ></ion-icon>
              </div>
            </div>
          </div>
        </div>

        <!-- No Results -->
        <div v-else-if="searchTerm && !isSearching" class="no-results">
          <ion-icon name="search" class="no-results-icon"></ion-icon>
          <p>No se encontraron usuarios para "{{ searchTerm }}"</p>
        </div>

        <!-- Indicador de búsqueda -->
        <div v-if="isSearching" class="search-loading">
          <ion-spinner name="dots"></ion-spinner>
          <p>Buscando usuarios...</p>
        </div>

        <!-- Botones de acción -->
        <div class="form-actions">
          <button
            @click="createChat"
            :disabled="!canCreate || isCreating"
            class="gs-button gs-button-primary create-btn"
            :class="{ 'loading': isCreating }"
          >
            <ion-spinner v-if="isCreating" name="dots" class="button-spinner"></ion-spinner>
            <ion-icon v-else name="chatbubbles" class="button-icon"></ion-icon>
            <span>{{ chatType === 'PRIVADO' ? 'Crear Chat' : 'Crear Grupo' }}</span>
          </button>
        </div>
      </div>
    </ion-content>
  </ion-modal>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue';
import {
  IonModal, IonHeader, IonToolbar, IonTitle, IonButtons, IonButton,
  IonContent, IonIcon, IonSegment, IonSegmentButton, IonLabel,
  IonSpinner
} from '@ionic/vue';
import { useApi } from '@/composables/useApi';
import { useAuth } from '@/composables/useAuth';
import config from '@/config/config';
import ProfileAvatar from '@/ui/ProfileAvatar.vue'; // Importar el componente ProfileAvatar

// Props & Emits
const props = defineProps<{
  isOpen: boolean;
}>();

const emit = defineEmits<{
  close: [];
  chatCreated: [chat: any];
}>();

// Composables
const { showToast } = useApi();
const { usuario } = useAuth();

// Estado reactivo
const chatType = ref<'PRIVADO' | 'GRUPO'>('PRIVADO');
const groupName = ref('');
const searchTerm = ref('');
const searchResults = ref<any[]>([]);
const selectedUsers = ref<any[]>([]);
const isSearching = ref(false);
const isCreating = ref(false);

// Debounced search
let searchTimeout: NodeJS.Timeout | null = null;
const debouncedSearch = () => {
  if (searchTimeout) clearTimeout(searchTimeout);
  searchTimeout = setTimeout(() => {
    if (searchTerm.value.trim()) {
      searchUsers();
    } else {
      searchResults.value = [];
    }
  }, 300);
};

// Computed
const currentUserId = computed(() => usuario.value?.usuarioId);

const availableUsers = computed(() => {
  return searchResults.value.filter(user => 
    !selectedUsers.value.some(selected => selected.usuarioId === user.usuarioId)
  );
});

const canCreate = computed(() => {
  if (chatType.value === 'PRIVADO') {
    return selectedUsers.value.length === 1;
  } else {
    return groupName.value.trim().length > 0 && selectedUsers.value.length >= 1;
  }
});

// Métodos
const searchUsers = async () => {
  if (!searchTerm.value.trim() || !currentUserId.value) return;

  isSearching.value = true;
  try {
    // Asegúrate de que esta URL también sea correcta
    const response = await fetch(
      `${config.api.fullApiUrl}/usuario/buscar?termino=${encodeURIComponent(searchTerm.value)}&usuarioActualId=${currentUserId.value}`,
      {
        headers: {
          'Authorization': `Bearer ${localStorage.getItem(config.storage.token)}`,
          'Content-Type': 'application/json'
        }
      }
    );

    if (response.ok) {
      const users = await response.json();
      console.log('Usuarios encontrados:', users); // Para depuración
      // Filtrar usuarios que ya están verificados
      searchResults.value = users.filter((user: any) => user.usuarioId !== currentUserId.value);
    } else {
      searchResults.value = [];
      showToast('Error al buscar usuarios', 'danger');
    }
  } catch (error) {
    console.error('Error buscando usuarios:', error);
    searchResults.value = [];
    showToast('Error de conexión', 'danger');
  } finally {
    isSearching.value = false;
  }
};

const selectUser = (user: any) => {
  if (isUserSelected(user)) {
    removeUser(user);
    return;
  }

  if (chatType.value === 'PRIVADO') {
    selectedUsers.value = [user];
  } else {
    selectedUsers.value.push(user);
  }
};

const removeUser = (user: any) => {
  selectedUsers.value = selectedUsers.value.filter(u => u.usuarioId !== user.usuarioId);
};

const isUserSelected = (user: any) => {
  return selectedUsers.value.some(selected => selected.usuarioId === user.usuarioId);
};

const relationClass = (estado: string) => {
  switch (estado) {
    case 'AMISTAD_EXISTENTE': return 'friends';
    case 'PETICION_ENVIADA': return 'sent';
    case 'PETICION_RECIBIDA': return 'received';
    default: return '';
  }
};

const relationText = (estado: string) => {
  switch (estado) {
    case 'AMISTAD_EXISTENTE': return 'Amigo';
    case 'PETICION_ENVIADA': return 'Solicitud enviada';
    case 'PETICION_RECIBIDA': return 'Te envió solicitud';
    default: return '';
  }
};

const createChat = async () => {
  if (!canCreate.value || isCreating.value) return;

  isCreating.value = true;
  try {
    // Usar solo /chat ya que fullApiUrl incluye /GameConnect
    const endpoint = `${config.api.fullApiUrl}/chat?creadorId=${currentUserId.value}`;
    const headers = {
      'Authorization': `Bearer ${localStorage.getItem(config.storage.token)}`,
      'Content-Type': 'application/json'
    };

    // Preparar los participantes incluyendo al usuario actual
    const participantesIds = [currentUserId.value, ...selectedUsers.value.map(u => u.usuarioId)];

    const body = {
      nombreChat: chatType.value === 'GRUPO' ? groupName.value.trim() : null,
      tipo: chatType.value,
      participantesIds: participantesIds
    };

    const response = await fetch(endpoint, {
      method: 'POST',
      headers,
      body: JSON.stringify(body)
    });

    if (response.ok) {
      const newChat = await response.json();
      showToast(
        chatType.value === 'PRIVADO' ? 'Chat creado exitosamente' : 'Grupo creado exitosamente',
        'success'
      );
      emit('chatCreated', newChat);
      resetForm();
      emit('close');
    } else {
      const errorText = await response.text();
      showToast(errorText || 'Error creando el chat', 'danger');
    }
  } catch (error) {
    console.error('Error creando chat:', error);
    showToast('Error de conexión', 'danger');
  } finally {
    isCreating.value = false;
  }
};

const resetForm = () => {
  chatType.value = 'PRIVADO';
  groupName.value = '';
  searchTerm.value = '';
  searchResults.value = [];
  selectedUsers.value = [];
  isCreating.value = false;
  isSearching.value = false;
};

// Watchers
watch(() => props.isOpen, (isOpen) => {
  if (!isOpen) {
    resetForm();
  }
});

watch(chatType, () => {
  selectedUsers.value = [];
  if (chatType.value === 'PRIVADO') {
    groupName.value = '';
  }
});
</script>

<style scoped>
.create-chat-modal {
  --width: min(90vw, 600px);
  --height: 90vh;
}

.gs-container {
  padding: var(--gs-space-lg);
  max-width: 100%;
}

/* Form Sections */
.form-section {
  margin-bottom: var(--gs-space-xl);
}

.gs-label {
  display: block;
  font-size: var(--gs-text-sm);
  font-weight: var(--gs-font-semibold);
  color: var(--gs-text-primary);
  margin-bottom: var(--gs-space-sm);
}

.input-helper {
  display: flex;
  justify-content: flex-end;
  margin-top: var(--gs-space-xs);
}

.character-count {
  font-size: var(--gs-text-xs);
  color: var(--gs-text-tertiary);
}

/* Chat Type Segment */
.chat-type-segment {
  --background: var(--gs-bg-tertiary);
  border-radius: var(--gs-radius-lg);
  padding: var(--gs-space-xs);
  margin-bottom: var(--gs-space-md);
}

.segment-button {
  --color: var(--gs-text-secondary);
  --color-checked: var(--gs-text-inverse);
  --background-checked: var(--gs-primary-500);
  font-size: var(--gs-text-sm);
  font-weight: var(--gs-font-medium);
  text-transform: none;
  letter-spacing: normal;
}

/* Inputs */
.gs-input {
  width: 100%;
  padding: var(--gs-space-md);
  border: 1px solid var(--gs-border-primary);
  border-radius: var(--gs-radius-lg);
  background: var(--gs-bg-secondary);
  color: var(--gs-text-primary);
  font-size: var(--gs-text-base);
  transition: all var(--gs-transition-fast);
}

.gs-input:focus {
  outline: none;
  border-color: var(--gs-primary-500);
  box-shadow: 0 0 0 2px var(--gs-primary-200);
}

.gs-input::placeholder {
  color: var(--gs-text-tertiary);
}

/* Search Input */
.search-input-container {
  position: relative;
}

.search-input {
  padding-left: calc(var(--gs-space-lg) + 16px);
}

.search-icon {
  position: absolute;
  top: 50%;
  left: var(--gs-space-md);
  transform: translateY(-50%);
  color: var(--gs-text-tertiary);
  font-size: 18px;
}

/* Selected Users */
.selected-users-list {
  display: flex;
  flex-direction: column;
  gap: var(--gs-space-md);
}

.selected-user {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--gs-space-md);
  background: var(--gs-bg-secondary);
  border-radius: var(--gs-radius-lg);
  border: 1px solid var(--gs-border-primary);
}

.user-info {
  display: flex;
  align-items: center;
  gap: var(--gs-space-md);
}

/* User Avatar - Replaced with ProfileAvatar component */
.user-details {
  flex: 1;
  min-width: 0;
}

.username {
  font-size: var(--gs-text-base);
  font-weight: var(--gs-font-semibold);
  color: var(--gs-text-primary);
  margin: 0 0 var(--gs-space-xs) 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.email {
  font-size: var(--gs-text-sm);
  color: var(--gs-text-secondary);
  margin: 0;
}

/* Remove Button */
.remove-btn {
  background: var(--gs-error-500);
  color: white;
  border: none;
  border-radius: var(--gs-radius-full);
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all var(--gs-transition-fast);
  flex-shrink: 0;
}

.remove-btn:hover:not(:disabled) {
  background: var(--gs-error-600);
  transform: scale(1.1);
}

.remove-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* Relation Badge */
.relation-badge {
  display: inline-block;
  padding: var(--gs-space-xs) var(--gs-space-sm);
  border-radius: var(--gs-radius-md);
  font-size: var(--gs-text-xs);
  font-weight: var(--gs-font-medium);
  margin-top: var(--gs-space-xs);
}

.relation-badge.friends {
  background: var(--gs-secondary-100);
  color: var(--gs-secondary-700);
}

.relation-badge.sent {
  background: var(--gs-primary-100);
  color: var(--gs-primary-700);
}

.relation-badge.received {
  background: var(--gs-warning-100);
  color: var(--gs-warning-700);
}

/* User Actions */
.user-actions {
  flex-shrink: 0;
}

.action-icon {
  font-size: 1.5rem;
  color: var(--gs-text-tertiary);
  transition: all var(--gs-transition-fast);
}

.action-icon.selected {
  color: var(--gs-secondary-500);
}

/* No Results */
.no-results {
  padding: var(--gs-space-2xl);
  text-align: center;
  color: var(--gs-text-tertiary);
}

.no-results-icon {
  font-size: 2rem;
  margin-bottom: var(--gs-space-md);
}

/* Search Results */
.search-results {
  display: flex;
  flex-direction: column;
  gap: var(--gs-space-md);
  max-height: 300px;
  overflow-y: auto;
  padding-right: var(--gs-space-sm);
}

.user-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--gs-space-md);
  background: var(--gs-bg-secondary);
  border-radius: var(--gs-radius-lg);
  border: 1px solid var(--gs-border-primary);
  cursor: pointer;
  transition: all var(--gs-transition-fast);
}

.user-card:hover {
  background: var(--gs-bg-hover);
  transform: translateY(-1px);
  box-shadow: var(--gs-shadow-sm);
}

/* Search Loading */
.search-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--gs-space-xl);
  color: var(--gs-text-secondary);
}

.search-loading p {
  margin-top: var(--gs-space-md);
  font-size: var(--gs-text-sm);
}

/* Form Actions */
.form-actions {
  display: flex;
  justify-content: flex-end;
}

.create-btn {
  display: flex;
  align-items: center;
  gap: var(--gs-space-sm);
  padding: var(--gs-space-md) var(--gs-space-lg);
  background: var(--gs-primary-500);
  color: white;
  border: none;
  border-radius: var(--gs-radius-lg);
  font-weight: var(--gs-font-semibold);
  font-size: var(--gs-text-base);
  cursor: pointer;
  transition: all var(--gs-transition-fast);
}

.create-btn:hover:not(:disabled) {
  background: var(--gs-primary-600);
  transform: translateY(-1px);
  box-shadow: var(--gs-shadow-md);
}

.create-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.create-btn.loading {
  padding-right: var(--gs-space-xl);
}

.button-icon,
.button-spinner {
  margin-right: var(--gs-space-xs);
}
</style>