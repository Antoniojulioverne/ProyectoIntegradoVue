<template>
  <ion-modal :is-open="isOpen" @did-dismiss="$emit('close')" class="create-chat-modal">
    <ion-header class="gs-header">
      <ion-toolbar>
        <ion-title class="gs-heading-3">
          {{ chatType === 'PRIVADO' ? 'Nuevo Chat' : 'Nuevo Grupo' }}
        </ion-title>
        <ion-buttons slot="end">
          <ion-button @click="$emit('close')" fill="clear" class="gs-button gs-button-ghost">
            <ion-icon name="close"></ion-icon>
          </ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>

    <ion-content class="gs-content">
      <div class="gs-container">
        <!-- Selector de tipo de chat -->
        <div class="form-section">
          <label class="gs-label">Tipo de chat</label>
          <ion-segment v-model="chatType" class="chat-type-segment">
            <ion-segment-button value="PRIVADO" class="segment-button">
              <ion-icon name="person" class="segment-icon"></ion-icon>
              <ion-label>Chat Privado</ion-label>
            </ion-segment-button>
            <ion-segment-button value="GRUPO" class="segment-button">
              <ion-icon name="people" class="segment-icon"></ion-icon>
              <ion-label>Grupo</ion-label>
            </ion-segment-button>
          </ion-segment>
        </div>

        <!-- Nombre del grupo (solo para grupos) -->
        <div v-if="chatType === 'GRUPO'" class="form-section">
          <label class="gs-label">Nombre del grupo</label>
          <input
            v-model="groupName"
            type="text"
            class="gs-input"
            placeholder="Ingresa el nombre del grupo"
            :disabled="isCreating"
            maxlength="50"
          />
          <div class="input-helper">
            <span class="character-count">{{ groupName.length }}/50</span>
          </div>
        </div>

        <!-- Buscador de usuarios -->
        <div class="form-section">
          <label class="gs-label">
            {{ chatType === 'PRIVADO' ? 'Buscar usuario' : 'Agregar participantes' }}
          </label>
          <div class="search-container">
            <input
              v-model="searchTerm"
              type="text"
              class="gs-input search-input"
              placeholder="Buscar por nombre o email..."
              :disabled="isCreating"
              @input="debouncedSearch"
            />
            <ion-icon name="search-outline" class="search-icon"></ion-icon>
          </div>
        </div>

        <!-- Usuarios seleccionados -->
        <div v-if="selectedUsers.length > 0" class="form-section">
          <label class="gs-label">
            {{ chatType === 'PRIVADO' ? 'Usuario seleccionado' : 'Participantes seleccionados' }}
            ({{ selectedUsers.length }})
          </label>
          <div class="selected-users">
            <div
              v-for="user in selectedUsers"
              :key="user.usuarioId"
              class="selected-user-chip"
            >
              <div class="user-avatar">
                <img v-if="user.skin" :src="user.skin" :alt="user.username" />
                <ion-icon v-else name="person-circle" class="avatar-icon"></ion-icon>
              </div>
              <div class="user-info">
                <span class="username">{{ user.username }}</span>
                <span class="email">{{ user.email }}</span>
              </div>
              <button 
                @click="removeUser(user)"
                class="remove-btn"
                :disabled="isCreating"
              >
                <ion-icon name="close"></ion-icon>
              </button>
            </div>
          </div>
        </div>

        <!-- Resultados de búsqueda -->
        <div v-if="searchTerm && !isSearching" class="form-section">
          <label class="gs-label">Resultados</label>
          
          <div v-if="searchResults.length === 0" class="no-results">
            <ion-icon name="search-outline" class="no-results-icon"></ion-icon>
            <p>No se encontraron usuarios</p>
          </div>
          
          <div v-else class="search-results">
            <div
              v-for="user in availableUsers"
              :key="user.usuarioId"
              class="user-result"
              @click="selectUser(user)"
            >
              <div class="user-avatar">
                <img v-if="user.skin" :src="user.skin" :alt="user.username" />
                <ion-icon v-else name="person-circle" class="avatar-icon"></ion-icon>
              </div>
              <div class="user-details">
                <h4 class="username">{{ user.username }}</h4>
                <p class="email">{{ user.email }}</p>
                <span 
                  v-if="user.estadoRelacion && user.estadoRelacion !== 'SIN_RELACION'"
                  class="relation-badge"
                  :class="relationClass(user.estadoRelacion)"
                >
                  {{ relationText(user.estadoRelacion) }}
                </span>
              </div>
              <div class="user-actions">
                <ion-icon 
                  :name="isUserSelected(user) ? 'checkmark-circle' : 'add-circle-outline'"
                  class="action-icon"
                  :class="{ 'selected': isUserSelected(user) }"
                ></ion-icon>
              </div>
            </div>
          </div>
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
  border-radius: var(--gs-radius-md);
  min-height: 48px;
}

.segment-icon {
  font-size: 1.25rem;
  margin-bottom: var(--gs-space-xs);
}

/* Search Container */
.search-container {
  position: relative;
}

.search-input {
  padding-right: var(--gs-space-3xl);
}

.search-icon {
  position: absolute;
  right: var(--gs-space-md);
  top: 50%;
  transform: translateY(-50%);
  color: var(--gs-text-tertiary);
  font-size: 1.25rem;
  pointer-events: none;
}

/* Selected Users */
.selected-users {
  display: flex;
  flex-direction: column;
  gap: var(--gs-space-sm);
  max-height: 200px;
  overflow-y: auto;
}

.selected-user-chip {
  display: flex;
  align-items: center;
  gap: var(--gs-space-sm);
  padding: var(--gs-space-sm);
  background: var(--gs-bg-secondary);
  border: 1px solid var(--gs-border-primary);
  border-radius: var(--gs-radius-lg);
  transition: all var(--gs-transition-fast);
}

.selected-user-chip:hover {
  border-color: var(--gs-primary-300);
  box-shadow: var(--gs-shadow-sm);
}

/* Search Results */
.search-results {
  display: flex;
  flex-direction: column;
  gap: var(--gs-space-xs);
  max-height: 300px;
  overflow-y: auto;
}

.user-result {
  display: flex;
  align-items: center;
  gap: var(--gs-space-md);
  padding: var(--gs-space-md);
  background: var(--gs-bg-secondary);
  border: 1px solid var(--gs-border-primary);
  border-radius: var(--gs-radius-lg);
  cursor: pointer;
  transition: all var(--gs-transition-fast);
}

.user-result:hover {
  border-color: var(--gs-primary-300);
  box-shadow: var(--gs-shadow-sm);
  transform: translateY(-1px);
}

/* User Avatar */
.user-avatar {
  width: 40px;
  height: 40px;
  border-radius: var(--gs-radius-full);
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--gs-primary-100);
  flex-shrink: 0;
}

.user-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-icon {
  font-size: 1.5rem;
  color: var(--gs-primary-500);
}

/* User Info */
.user-info,
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
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--gs-space-md);
  padding: var(--gs-space-2xl);
  text-align: center;
  color: var(--gs-text-tertiary);
}

.no-results-icon {
  font-size: 2rem;
  opacity: 0.6;
}

/* Search Loading */
.search-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--gs-space-md);
  padding: var(--gs-space-xl);
  color: var(--gs-text-secondary);
}

/* Form Actions */
.form-actions {
  margin-top: var(--gs-space-2xl);
  padding-top: var(--gs-space-lg);
  border-top: 1px solid var(--gs-border-primary);
}

.create-btn {
  width: 100%;
  min-height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--gs-space-sm);
  font-weight: var(--gs-font-semibold);
}

.create-btn.loading {
  opacity: 0.8;
  pointer-events: none;
}

.button-spinner,
.button-icon {
  font-size: 1.25rem;
}

/* Scrollbars */
.selected-users::-webkit-scrollbar,
.search-results::-webkit-scrollbar {
  width: 6px;
}

.selected-users::-webkit-scrollbar-track,
.search-results::-webkit-scrollbar-track {
  background: var(--gs-bg-tertiary);
  border-radius: var(--gs-radius-sm);
}

.selected-users::-webkit-scrollbar-thumb,
.search-results::-webkit-scrollbar-thumb {
  background: var(--gs-border-secondary);
  border-radius: var(--gs-radius-sm);
}

.selected-users::-webkit-scrollbar-thumb:hover,
.search-results::-webkit-scrollbar-thumb:hover {
  background: var(--gs-text-tertiary);
}

/* Responsive */
@media (max-width: 768px) {
  .create-chat-modal {
    --width: 100vw;
    --height: 100vh;
  }
  
  .gs-container {
    padding: var(--gs-space-md);
  }
  
  .user-result {
    padding: var(--gs-space-sm);
  }
  
  .user-avatar {
    width: 36px;
    height: 36px;
  }
  
  .avatar-icon {
    font-size: 1.25rem;
  }
}
</style>