<template>
  <ion-modal
    :is-open="isOpen"
    @will-dismiss="$emit('close')"
    class="group-options-modal"
  >
    <ion-header>
      <ion-toolbar>
        <ion-title>Opciones del Grupo</ion-title>
        <ion-buttons slot="end">
          <ion-button @click="$emit('close')">
            <ion-icon slot="icon-only" name="close-outline"></ion-icon>
          </ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>
    
    <ion-content class="ion-padding">
      <div class="gs-container">
        <!-- Información del grupo -->
        <div v-if="chat" class="group-info-section">
          <div class="group-avatar">
            <ion-icon name="people" class="group-icon"></ion-icon>
          </div>
          <div class="group-details">
            <h2 class="group-name">{{ chat.nombreChat || 'Grupo sin nombre' }}</h2>
            <p class="group-participants">{{ participantCount }} participantes</p>
          </div>
        </div>

        <!-- Acciones del grupo -->
        <div v-if="chat" class="action-section">
          <h3 class="section-title">Participantes</h3>
          
          <!-- Botón para mostrar/ocultar lista de participantes -->
          <div class="action-item" @click="showParticipants = !showParticipants">
            <div class="action-content">
              <ion-icon name="people-outline" class="action-icon"></ion-icon>
              <div class="action-text">
                <div class="action-title">Ver participantes</div>
                <div class="action-subtitle">{{ participantCount }} miembros</div>
              </div>
            </div>
            <ion-icon 
              :name="showParticipants ? 'chevron-up' : 'chevron-down'" 
              class="action-arrow"
            ></ion-icon>
          </div>

          <!-- Lista de participantes (expandible) -->
          <div v-if="showParticipants && chat?.participantes" class="participants-section">
            <div class="participants-list">
              <div
                v-for="participant in (chat?.participantes || [])"
                :key="participant.usuarioId"
                class="participant-item"
              >
                <!-- Usar ProfileAvatar para mostrar foto de perfil -->
                <ProfileAvatar
                  :profile-image="participant.fotoPerfil"
                  :username="participant.username"
                  :size="40"
                  :is-verified="participant.emailVerificado"
                  :show-verification="true"
                />
                
                <div class="participant-info">
                  <div class="participant-name">{{ participant.username }}</div>
                  <div class="participant-email">{{ participant.email }}</div>
                  <div v-if="isParticipantAdmin(participant.usuarioId)" class="admin-badge">
                    <ion-icon name="shield-checkmark" class="badge-icon"></ion-icon>
                    Administrador
                  </div>
                </div>
                
                <!-- Acciones solo para admin -->
                <div v-if="isAdmin && participant.usuarioId !== currentUserId" class="participant-actions">
                  <ion-button 
                    v-if="!isParticipantAdmin(participant.usuarioId)"
                    @click="makeAdmin(participant.usuarioId)"
                    fill="clear" 
                    size="small"
                    class="admin-btn"
                    title="Hacer administrador"
                  >
                    <ion-icon name="shield-checkmark" slot="icon-only"></ion-icon>
                  </ion-button>
                  <ion-button 
                    @click="removeParticipant(participant.usuarioId)"
                    fill="clear" 
                    size="small"
                    color="danger"
                    class="remove-btn"
                    title="Remover del grupo"
                  >
                    <ion-icon name="person-remove" slot="icon-only"></ion-icon>
                  </ion-button>
                </div>
              </div>
            </div>

            <!-- Agregar participante (solo admin) -->
            <div v-if="isAdmin" class="add-participant-section">
              <h4 class="subsection-title">Agregar participante</h4>
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

              <!-- Resultados de búsqueda -->
              <div v-if="searchResults.length > 0" class="search-results">
                <div
                  v-for="user in availableUsers"
                  :key="user.usuarioId"
                  class="user-result"
                  @click="addParticipant(user.usuarioId)"
                >
                  <!-- Usar ProfileAvatar para usuarios encontrados -->
                  <ProfileAvatar
                    :profile-image="user.fotoPerfil"
                    :username="user.username"
                    :size="32"
                    :is-verified="user.emailVerificado"
                    :show-verification="true"
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
                  
                  <div class="user-actions">
                    <ion-icon name="add-circle-outline" class="add-icon"></ion-icon>
                  </div>
                </div>
              </div>

              <!-- Sin resultados -->
              <div v-else-if="searchTerm && searchResults.length === 0" class="no-results">
                <ion-icon name="search" class="no-results-icon"></ion-icon>
                <p>No se encontraron usuarios para "{{ searchTerm }}"</p>
              </div>
            </div>
          </div>
        </div>

        <!-- Otras acciones del grupo -->
        <div v-if="chat" class="action-section">
          <h3 class="section-title">Opciones</h3>
          
          <div v-if="isAdmin" class="action-item" @click="showEditName = true">
            <div class="action-content">
              <ion-icon name="create-outline" class="action-icon"></ion-icon>
              <div class="action-text">
                <div class="action-title">Editar nombre del grupo</div>
              </div>
            </div>
            <ion-icon name="chevron-forward" class="action-arrow"></ion-icon>
          </div>

          <div class="action-item danger-action" @click="$emit('leaveGroup', chat?.chatId)">
            <div class="action-content">
              <ion-icon name="exit-outline" class="action-icon danger-icon"></ion-icon>
              <div class="action-text">
                <div class="action-title danger-text">Salir del grupo</div>
                <div class="action-subtitle">Ya no recibirás mensajes</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </ion-content>

    <!-- Modal para editar nombre -->
    <ion-modal :is-open="showEditName" @will-dismiss="showEditName = false">
      <ion-header>
        <ion-toolbar>
          <ion-title>Editar nombre del grupo</ion-title>
          <ion-buttons slot="end">
            <ion-button @click="showEditName = false">Cancelar</ion-button>
            <ion-button @click="updateGroupName" :disabled="!newGroupName.trim()">Guardar</ion-button>
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
              placeholder="Nombre del grupo"
              maxlength="50"
            />
            <div class="input-helper">
              <span class="character-count">{{ newGroupName.length }}/50</span>
            </div>
          </div>
        </div>
      </ion-content>
    </ion-modal>

    <!-- Toast notifications -->
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
  IonContent, IonIcon, IonToast
} from '@ionic/vue';
import config from '@/config/config';
import ProfileAvatar from '@/ui/ProfileAvatar.vue'; // Importar ProfileAvatar

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

// Estado reactivo
const showParticipants = ref(false);
const showEditName = ref(false);
const newGroupName = ref('');
const searchTerm = ref('');
const searchResults = ref<any[]>([]);
const adminParticipants = ref<number[]>([]);
const showToast = ref(false);
const toastMessage = ref('');
const toastColor = ref('success');

// Computed
const participantCount = computed(() => props.chat?.participantes?.length || 0);

const isAdmin = computed(() => {
  return adminParticipants.value.includes(props.currentUserId);
});

const availableUsers = computed(() => {
  if (!props.chat?.participantes) return searchResults.value;
  
  const participantIds = props.chat.participantes.map((p: any) => p.usuarioId);
  return searchResults.value.filter((user: any) => !participantIds.includes(user.usuarioId));
});

// Métodos
const isParticipantAdmin = (userId: number) => {
  return adminParticipants.value.includes(userId);
};

// Métodos para relaciones (reutilizados del CreateChatModal)
const relationClass = (relation: string) => {
  const classes: Record<string, string> = {
    'AMIGOS': 'friends',
    'PENDIENTE_ENVIADA': 'pending-sent',
    'PENDIENTE_RECIBIDA': 'pending-received',
    'BLOQUEADO': 'blocked'
  };
  return classes[relation] || '';
};

const relationText = (relation: string) => {
  const texts: Record<string, string> = {
    'AMIGOS': 'Amigos',
    'PENDIENTE_ENVIADA': 'Solicitud enviada',
    'PENDIENTE_RECIBIDA': 'Solicitud recibida',
    'BLOQUEADO': 'Bloqueado'
  };
  return texts[relation] || '';
};

const makeAdmin = async (userId: number) => {
  try {
    const response = await fetch(
      `${config.api.fullApiUrl}/chat/${props.chat.chatId}/administrador?usuarioId=${userId}&adminId=${props.currentUserId}`,
      {
        method: 'POST',
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
    console.error('Error promoviendo usuario:', error);
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

const updateGroupName = async () => {
  if (!newGroupName.value.trim()) return;

  try {
    const response = await fetch(
      `${config.api.fullApiUrl}/chat/${props.chat.chatId}/nombre`,
      {
        method: 'PUT',
        headers: {
          'Authorization': `Bearer ${localStorage.getItem(config.storage.token)}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ 
          nombreChat: newGroupName.value.trim(),
          adminId: props.currentUserId 
        })
      }
    );

    if (response.ok) {
      showToastMessage('Nombre del grupo actualizado', 'success');
      showEditName.value = false;
      emit('updateGroup');
    } else {
      showToastMessage('Error al actualizar nombre', 'danger');
    }
  } catch (error) {
    console.error('Error actualizando nombre:', error);
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
                newGroupName.value = props.chat?.nombreChat || '';
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

.subsection-title {
  font-size: var(--gs-text-base);
  font-weight: var(--gs-font-medium);
  color: var(--gs-text-primary);
  margin: var(--gs-space-lg) 0 var(--gs-space-sm) 0;
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

.action-item.danger-action {
  border-color: var(--gs-error-200);
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
  margin-top: var(--gs-space-lg);
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
  transition: background-color var(--gs-transition-fast);
}

.participant-item:last-child {
  border-bottom: none;
}

.participant-item:hover {
  background: var(--gs-bg-tertiary);
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

.participant-email {
  font-size: var(--gs-text-sm);
  color: var(--gs-text-secondary);
}

.admin-badge {
  display: flex;
  align-items: center;
  gap: var(--gs-space-xs);
  font-size: var(--gs-text-xs);
  color: var(--gs-warning-600);
  background: var(--gs-warning-100);
  padding: var(--gs-space-xs) var(--gs-space-sm);
  border-radius: var(--gs-radius-md);
  width: fit-content;
}

.badge-icon {
  font-size: 0.875rem;
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
  border-top: 1px solid var(--gs-border-primary);
  padding-top: var(--gs-space-lg);
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

.relation-badge {
  font-size: var(--gs-text-xs);
  padding: var(--gs-space-xs) var(--gs-space-sm);
  border-radius: var(--gs-radius-sm);
  font-weight: var(--gs-font-medium);
  width: fit-content;
  margin-top: var(--gs-space-xs);
}

.relation-badge.friends {
  background: var(--gs-success-100);
  color: var(--gs-success-600);
}

.relation-badge.pending-sent {
  background: var(--gs-warning-100);
  color: var(--gs-warning-600);
}

.relation-badge.pending-received {
  background: var(--gs-info-100);
  color: var(--gs-info-600);
}

.relation-badge.blocked {
  background: var(--gs-error-100);
  color: var(--gs-error-600);
}

.user-actions {
  flex-shrink: 0;
}

.add-icon {
  font-size: 1.25rem;
  color: var(--gs-secondary-500);
}

.no-results {
  text-align: center;
  padding: var(--gs-space-xl);
  color: var(--gs-text-secondary);
}

.no-results-icon {
  font-size: 2rem;
  margin-bottom: var(--gs-space-sm);
  opacity: 0.5;
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