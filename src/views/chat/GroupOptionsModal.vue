<template>
  <ion-modal :is-open="isOpen" @did-dismiss="$emit('close')" class="group-options-modal">
    <ion-header class="gs-header">
      <ion-toolbar>
        <ion-title class="gs-heading-3">
          Opciones del Grupo
        </ion-title>
        <ion-buttons slot="end">
          <ion-button @click="$emit('close')" fill="clear" class="gs-button gs-button-ghost">
            <ion-icon name="close"></ion-icon>
          </ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>

    <ion-content class="gs-content" v-if="chat">
      <div class="gs-container">
        <!-- Información del grupo -->
        <div class="group-info-section">
          <div class="group-avatar">
            <ion-icon name="people" class="group-icon"></ion-icon>
          </div>
          <div class="group-details">
            <h2 class="group-name">{{ chat.nombreChat || `Grupo #${chat.chatId}` }}</h2>
            <p class="group-participants">{{ chat.participantes?.length || 0 }} participantes</p>
          </div>
        </div>

        <!-- Acciones del grupo -->
        <div class="group-actions">
          <!-- Editar nombre del grupo (solo admin) -->
          <div v-if="isAdmin" class="action-section">
            <h3 class="section-title">Administración</h3>
            
            <div class="action-item" @click="toggleEditName">
              <div class="action-content">
                <ion-icon name="create" class="action-icon"></ion-icon>
                <div class="action-text">
                  <span class="action-title">Editar nombre del grupo</span>
                  <span class="action-subtitle">Cambiar el nombre visible</span>
                </div>
              </div>
              <ion-icon name="chevron-forward" class="action-arrow"></ion-icon>
            </div>

            <div class="action-item" @click="showParticipants = !showParticipants">
              <div class="action-content">
                <ion-icon name="people" class="action-icon"></ion-icon>
                <div class="action-text">
                  <span class="action-title">Gestionar participantes</span>
                  <span class="action-subtitle">Ver y administrar miembros</span>
                </div>
              </div>
              <ion-icon 
                :name="showParticipants ? 'chevron-up' : 'chevron-down'" 
                class="action-arrow"
              ></ion-icon>
            </div>
          </div>

          <!-- Lista de participantes (expandible) -->
          <div v-if="showParticipants" class="participants-section">
            <div class="participants-list">
              <div
                v-for="participant in chat.participantes"
                :key="participant.usuarioId"
                class="participant-item"
              >
                <div class="participant-avatar">
                  <img v-if="participant.skin" :src="participant.skin" :alt="participant.username" />
                  <ion-icon v-else name="person-circle" class="avatar-icon"></ion-icon>
                </div>
                <div class="participant-info">
                  <span class="participant-name">{{ participant.username }}</span>
                  <span v-if="isParticipantAdmin(participant.usuarioId)" class="admin-badge">
                    Administrador
                  </span>
                </div>
                <div v-if="isAdmin && participant.usuarioId !== currentUserId" class="participant-actions">
                  <ion-button 
                    v-if="!isParticipantAdmin(participant.usuarioId)"
                    @click="makeAdmin(participant.usuarioId)"
                    fill="clear" 
                    size="small"
                    class="admin-btn"
                  >
                    <ion-icon name="shield-checkmark" slot="icon-only"></ion-icon>
                  </ion-button>
                  <ion-button 
                    @click="removeParticipant(participant.usuarioId)"
                    fill="clear" 
                    size="small"
                    color="danger"
                    class="remove-btn"
                  >
                    <ion-icon name="person-remove" slot="icon-only"></ion-icon>
                  </ion-button>
                </div>
              </div>
            </div>

            <!-- Agregar participante (solo admin) -->
            <div v-if="isAdmin" class="add-participant-section">
              <div class="search-container">
                <input
                  v-model="searchTerm"
                  type="text"
                  class="gs-input search-input"
                  placeholder="Buscar usuarios para agregar..."
                  @input="debouncedSearch"
                />
                <ion-icon name="search-outline" class="search-icon"></ion-icon>
              </div>

              <div v-if="searchResults.length > 0" class="search-results">
                <div
                  v-for="user in availableUsers"
                  :key="user.usuarioId"
                  class="user-result"
                  @click="addParticipant(user.usuarioId)"
                >
                  <div class="user-avatar">
                    <img v-if="user.skin" :src="user.skin" :alt="user.username" />
                    <ion-icon v-else name="person-circle" class="avatar-icon"></ion-icon>
                  </div>
                  <div class="user-details">
                    <h4 class="username">{{ user.username }}</h4>
                    <p class="email">{{ user.email }}</p>
                  </div>
                  <div class="user-actions">
                    <ion-icon name="add-circle" class="add-icon"></ion-icon>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Acciones generales -->
          <div class="action-section">
            <h3 class="section-title">Acciones</h3>
            
            <div class="action-item danger-action" @click="confirmLeaveGroup">
              <div class="action-content">
                <ion-icon name="exit" class="action-icon danger-icon"></ion-icon>
                <div class="action-text">
                  <span class="action-title danger-text">Salir del grupo</span>
                  <span class="action-subtitle">Abandonar este grupo</span>
                </div>
              </div>
              <ion-icon name="chevron-forward" class="action-arrow"></ion-icon>
            </div>
          </div>
        </div>
      </div>
    </ion-content>

    <!-- Modal de editar nombre -->
    <ion-modal :is-open="showEditName" @did-dismiss="showEditName = false">
      <ion-header>
        <ion-toolbar>
          <ion-title>Editar nombre del grupo</ion-title>
          <ion-buttons slot="end">
            <ion-button @click="showEditName = false" fill="clear">
              <ion-icon name="close"></ion-icon>
            </ion-button>
          </ion-buttons>
        </ion-toolbar>
      </ion-header>
      <ion-content>
        <div class="edit-name-container">
          <div class="form-section">
            <label class="gs-label">Nuevo nombre</label>
            <input
              v-model="newGroupName"
              type="text"
              class="gs-input"
              placeholder="Ingresa el nuevo nombre"
              maxlength="50"
              @keyup.enter="updateGroupName"
            />
            <div class="input-helper">
              <span class="character-count">{{ newGroupName.length }}/50</span>
            </div>
          </div>
          <div class="form-actions">
            <button
              @click="updateGroupName"
              :disabled="!newGroupName.trim() || isUpdating"
              class="gs-button gs-button-primary"
            >
              <ion-spinner v-if="isUpdating" name="dots"></ion-spinner>
              <span v-else>Guardar cambios</span>
            </button>
          </div>
        </div>
      </ion-content>
    </ion-modal>

    <!-- Alert de confirmación -->
    <ion-alert
      :is-open="showConfirmLeave"
      header="Salir del grupo"
      :message="`¿Estás seguro de que quieres salir de '${chat?.nombreChat}'?`"
      :buttons="[
        { text: 'Cancelar', role: 'cancel' },
        { text: 'Salir', handler: () => handleLeaveGroup() }
      ]"
      @did-dismiss="showConfirmLeave = false"
    ></ion-alert>

    <!-- Toast -->
    <ion-toast
      :is-open="showToast"
      :message="toastMessage"
      :duration="3000"
      :color="toastColor"
      @did-dismiss="showToast = false"
    ></ion-toast>
  </ion-modal>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue';
import {
  IonModal, IonHeader, IonToolbar, IonTitle, IonButtons, IonButton,
  IonContent, IonIcon, IonSpinner, IonAlert, IonToast
} from '@ionic/vue';
import { useApi } from '@/composables/useApi';
import config from '@/config/config';

// Props & Emits
const props = defineProps<{
  isOpen: boolean;
  chat: any;
  currentUserId: number;
}>();

const emit = defineEmits<{
  close: [];
  leaveGroup: [chatId: number];
  updateGroup: [];
}>();

// Composables
const { showToast: apiShowToast } = useApi();

// Estado reactivo
const showParticipants = ref(false);
const showEditName = ref(false);
const showConfirmLeave = ref(false);
const newGroupName = ref('');
const isUpdating = ref(false);
const searchTerm = ref('');
const searchResults = ref<any[]>([]);
const showToast = ref(false);
const toastMessage = ref('');
const toastColor = ref('success');

// Datos de participantes admin (simulado - en una app real vendría del backend)
const adminParticipants = ref<number[]>([]);

// Computed
const isAdmin = computed(() => {
  // En una app real, esto vendría del backend
  // Por ahora, asumimos que el creador es admin
  return adminParticipants.value.includes(props.currentUserId);
});

const availableUsers = computed(() => {
  return searchResults.value.filter(user => 
    !props.chat?.participantes?.some((p: any) => p.usuarioId === user.usuarioId)
  );
});

// Métodos
const isParticipantAdmin = (userId: number): boolean => {
  return adminParticipants.value.includes(userId);
};

const toggleEditName = () => {
  newGroupName.value = props.chat?.nombreChat || '';
  showEditName.value = true;
};

const updateGroupName = async () => {
  if (!newGroupName.value.trim() || isUpdating.value) return;

  isUpdating.value = true;
  try {
    const response = await fetch(
      `${config.api.fullApiUrl}/chat/${props.chat.chatId}/nombre`,
      {
        method: 'PUT',
        headers: {
          'Authorization': `Bearer ${localStorage.getItem(config.storage.token)}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ nombre: newGroupName.value.trim() })
      }
    );

    if (response.ok) {
      showToastMessage('Nombre del grupo actualizado', 'success');
      showEditName.value = false;
      emit('updateGroup');
    } else {
      showToastMessage('Error al actualizar el nombre', 'danger');
    }
  } catch (error) {
    console.error('Error actualizando nombre:', error);
    showToastMessage('Error de conexión', 'danger');
  } finally {
    isUpdating.value = false;
  }
};

const confirmLeaveGroup = () => {
  showConfirmLeave.value = true;
};

const handleLeaveGroup = () => {
  emit('leaveGroup', props.chat.chatId);
  showConfirmLeave.value = false;
};

const makeAdmin = async (userId: number) => {
  try {
    const response = await fetch(
      `${config.api.fullApiUrl}/chat/${props.chat.chatId}/admin/${userId}`,
      {
        method: 'PUT',
        headers: {
          'Authorization': `Bearer ${localStorage.getItem(config.storage.token)}`,
          'Content-Type': 'application/json'
        }
      }
    );

    if (response.ok) {
      adminParticipants.value.push(userId);
      showToastMessage('Usuario promovido a administrador', 'success');
    } else {
      showToastMessage('Error al promover usuario', 'danger');
    }
  } catch (error) {
    console.error('Error promoviendo admin:', error);
    showToastMessage('Error de conexión', 'danger');
  }
};

const removeParticipant = async (userId: number) => {
  try {
    const response = await fetch(
      `${config.api.fullApiUrl}/chat/${props.chat.chatId}/participantes/${userId}?adminId=${props.currentUserId}`,
      {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${localStorage.getItem(config.storage.token)}`,
          'Content-Type': 'application/json'
        }
      }
    );

    if (response.ok) {
      showToastMessage('Participante removido del grupo', 'success');
      emit('updateGroup');
    } else {
      showToastMessage('Error al remover participante', 'danger');
    }
  } catch (error) {
    console.error('Error removiendo participante:', error);
    showToastMessage('Error de conexión', 'danger');
  }
};

const addParticipant = async (userId: number) => {
  try {
    const response = await fetch(
      `${config.api.fullApiUrl}/chat/${props.chat.chatId}/participantes?usuarioId=${userId}&adminId=${props.currentUserId}`,
      {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${localStorage.getItem(config.storage.token)}`,
          'Content-Type': 'application/json'
        }
      }
    );

    if (response.ok) {
      showToastMessage('Participante agregado al grupo', 'success');
      searchTerm.value = '';
      searchResults.value = [];
      emit('updateGroup');
    } else {
      showToastMessage('Error al agregar participante', 'danger');
    }
  } catch (error) {
    console.error('Error agregando participante:', error);
    showToastMessage('Error de conexión', 'danger');
  }
};

// Búsqueda de usuarios
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

const searchUsers = async () => {
  if (!searchTerm.value.trim()) return;

  try {
    const response = await fetch(
      `${config.api.fullApiUrl}/usuario/buscar?termino=${encodeURIComponent(searchTerm.value)}&usuarioActualId=${props.currentUserId}`,
      {
        headers: {
          'Authorization': `Bearer ${localStorage.getItem(config.storage.token)}`,
          'Content-Type': 'application/json'
        }
      }
    );

    if (response.ok) {
      const users = await response.json();
      searchResults.value = users.filter((user: any) => user.usuarioId !== props.currentUserId);
    } else {
      searchResults.value = [];
    }
  } catch (error) {
    console.error('Error buscando usuarios:', error);
    searchResults.value = [];
  }
};

const showToastMessage = (message: string, color: string = 'success') => {
  toastMessage.value = message;
  toastColor.value = color;
  showToast.value = true;
};

const loadGroupAdmins = async () => {
  // En una app real, cargarías esta info del backend
  // Por ahora, simulamos que el primer participante es admin
  if (props.chat?.participantes?.length > 0) {
    adminParticipants.value = [props.chat.participantes[0].usuarioId];
  }
};

// Watchers
watch(() => props.isOpen, (isOpen) => {
  if (isOpen && props.chat) {
    loadGroupAdmins();
    showParticipants.value = false;
    showEditName.value = false;
    searchTerm.value = '';
    searchResults.value = [];
  }
});
</script>

<style scoped>
.group-options-modal {
  --width: min(90vw, 500px);
  --height: 90vh;
}

.gs-container {
  padding: var(--gs-space-lg);
}

/* Información del grupo */
.group-info-section {
  display: flex;
  align-items: center;
  gap: var(--gs-space-md);
  padding: var(--gs-space-lg);
  background: var(--gs-bg-secondary);
  border-radius: var(--gs-radius-lg);
  margin-bottom: var(--gs-space-xl);
  border: 1px solid var(--gs-border-primary);
}

.group-avatar {
  width: 64px;
  height: 64px;
  border-radius: var(--gs-radius-full);
  background: linear-gradient(135deg, var(--gs-primary-500), var(--gs-primary-600));
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.group-icon {
  font-size: 2rem;
  color: white;
}

.group-details {
  flex: 1;
}

.group-name {
  font-size: var(--gs-text-xl);
  font-weight: var(--gs-font-semibold);
  color: var(--gs-text-primary);
  margin: 0 0 var(--gs-space-xs) 0;
}

.group-participants {
  font-size: var(--gs-text-sm);
  color: var(--gs-text-secondary);
  margin: 0;
}

/* Secciones de acciones */
.action-section {
  margin-bottom: var(--gs-space-xl);
}

.section-title {
  font-size: var(--gs-text-lg);
  font-weight: var(--gs-font-semibold);
  color: var(--gs-text-primary);
  margin: 0 0 var(--gs-space-md) 0;
}

.action-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--gs-space-md);
  background: var(--gs-bg-secondary);
  border: 1px solid var(--gs-border-primary);
  border-radius: var(--gs-radius-lg);
  cursor: pointer;
  transition: all var(--gs-transition-fast);
  margin-bottom: var(--gs-space-sm);
}

.action-item:hover {
  border-color: var(--gs-primary-300);
  box-shadow: var(--gs-shadow-sm);
}

.action-item.danger-action:hover {
  border-color: var(--gs-error-300);
  background: var(--gs-error-50);
}

.action-content {
  display: flex;
  align-items: center;
  gap: var(--gs-space-md);
  flex: 1;
}

.action-icon {
  font-size: 1.25rem;
  color: var(--gs-primary-500);
}

.action-icon.danger-icon {
  color: var(--gs-error-500);
}

.action-text {
  display: flex;
  flex-direction: column;
  gap: var(--gs-space-xs);
}

.action-title {
  font-size: var(--gs-text-base);
  font-weight: var(--gs-font-medium);
  color: var(--gs-text-primary);
}

.action-title.danger-text {
  color: var(--gs-error-600);
}

.action-subtitle {
  font-size: var(--gs-text-sm);
  color: var(--gs-text-secondary);
}

.action-arrow {
  color: var(--gs-text-tertiary);
  font-size: 1rem;
}

/* Lista de participantes */
.participants-section {
  margin-bottom: var(--gs-space-xl);
}

.participants-list {
  background: var(--gs-bg-secondary);
  border-radius: var(--gs-radius-lg);
  border: 1px solid var(--gs-border-primary);
  overflow: hidden;
  margin-bottom: var(--gs-space-lg);
}

.participant-item {
  display: flex;
  align-items: center;
  gap: var(--gs-space-md);
  padding: var(--gs-space-md);
  border-bottom: 1px solid var(--gs-border-primary);
}

.participant-item:last-child {
  border-bottom: none;
}

.participant-avatar {
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

.participant-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-icon {
  font-size: 1.5rem;
  color: var(--gs-primary-500);
}

.participant-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: var(--gs-space-xs);
}

.participant-name {
  font-size: var(--gs-text-base);
  font-weight: var(--gs-font-medium);
  color: var(--gs-text-primary);
}

.admin-badge {
  font-size: var(--gs-text-xs);
  color: var(--gs-warning-600);
  background: var(--gs-warning-100);
  padding: var(--gs-space-xs) var(--gs-space-sm);
  border-radius: var(--gs-radius-md);
  align-self: flex-start;
}

.participant-actions {
  display: flex;
  gap: var(--gs-space-sm);
}

.admin-btn,
.remove-btn {
  --color: var(--gs-text-tertiary);
  transition: all var(--gs-transition-fast);
}

.admin-btn:hover {
  --color: var(--gs-warning-500);
}

.remove-btn:hover {
  --color: var(--gs-error-500);
}

/* Buscar usuarios */
.add-participant-section {
  margin-top: var(--gs-space-lg);
}

.search-container {
  position: relative;
  margin-bottom: var(--gs-space-md);
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

.search-results {
  display: flex;
  flex-direction: column;
  gap: var(--gs-space-xs);
  max-height: 200px;
  overflow-y: auto;
}

.user-result {
  display: flex;
  align-items: center;
  gap: var(--gs-space-md);
  padding: var(--gs-space-sm);
  background: var(--gs-bg-secondary);
  border: 1px solid var(--gs-border-primary);
  border-radius: var(--gs-radius-md);
  cursor: pointer;
  transition: all var(--gs-transition-fast);
}

.user-result:hover {
  border-color: var(--gs-primary-300);
  box-shadow: var(--gs-shadow-sm);
}

.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: var(--gs-radius-full);
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--gs-primary-100);
  flex-shrink: 0;
}

.user-details {
  flex: 1;
  min-width: 0;
}

.username {
  font-size: var(--gs-text-sm);
  font-weight: var(--gs-font-medium);
  color: var(--gs-text-primary);
  margin: 0 0 var(--gs-space-xs) 0;
}

.email {
  font-size: var(--gs-text-xs);
  color: var(--gs-text-secondary);
  margin: 0;
}

.user-actions {
  flex-shrink: 0;
}

.add-icon {
  font-size: 1.25rem;
  color: var(--gs-secondary-500);
}

/* Modal de editar nombre */
.edit-name-container {
  padding: var(--gs-space-lg);
}

.form-section {
  margin-bottom: var(--gs-space-lg);
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

.form-actions {
  padding-top: var(--gs-space-lg);
  border-top: 1px solid var(--gs-border-primary);
}

/* Responsive */
@media (max-width: 768px) {
  .group-options-modal {
    --width: 100vw;
    --height: 100vh;
  }
  
  .gs-container {
    padding: var(--gs-space-md);
  }
  
  .group-info-section {
    padding: var(--gs-space-md);
  }
  
  .group-avatar {
    width: 48px;
    height: 48px;
  }
  
  .group-icon {
    font-size: 1.5rem;
  }
}
</style>