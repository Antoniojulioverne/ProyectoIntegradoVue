<template>
  <ion-page class="gs-chat-list-page">
    <ion-header class="gs-header">
      <ion-toolbar>
        <ion-buttons slot="start"> 
          <ion-menu-button :auto-hide="false"></ion-menu-button>
        </ion-buttons>
        <ion-title class="gs-page-title">
          <div class="gs-title-content">
            <ion-icon name="chatbubbles" class="gs-title-icon"></ion-icon>
            <span>Mis Chats</span>
            <div class="gs-connection-indicator" :class="connectionStatusClass">
              <ion-icon :name="connectionIcon"></ion-icon>
            </div>
          </div>
        </ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content class="gs-content">
      <!-- *** AGREGAR ION-REFRESHER *** -->
      <ion-refresher slot="fixed" @ionRefresh="handleRefresh($event)">
        <ion-refresher-content
          pulling-icon="chatbubbles-outline"
          pulling-text="Desliza para actualizar chats"
          refreshing-spinner="circular"
          refreshing-text="Actualizando..."
        ></ion-refresher-content>
      </ion-refresher>
      <div class="gs-chat-container">
        <!-- Loading state -->
        <div v-if="isLoading" class="gs-loading-state">
          <div class="gs-skeleton-container">
            <div v-for="n in 3" :key="n" class="gs-skeleton-chat-item">
              <div class="gs-skeleton gs-skeleton-avatar"></div>
              <div class="gs-skeleton-content">
                <div class="gs-skeleton gs-skeleton-line gs-skeleton-title"></div>
                <div class="gs-skeleton gs-skeleton-line gs-skeleton-subtitle"></div>
              </div>
            </div>
          </div>
        </div>

        <!-- Chat list -->
        <div v-else-if="chats.length > 0" class="gs-chat-list">
          <transition-group name="gs-chat-item" tag="div">
            <div
              v-for="chat in chats"
              :key="chat.chatId"
              class="gs-chat-item"
              :class="{ 'gs-has-unread': chat.mensajesNoLeidos > 0 }"
              @click="openChat(chat)"
            >
              <div class="gs-chat-avatar">
                <div class="gs-avatar-circle">
                  <!-- Avatar para chat grupal -->
                  <div v-if="isGroupChat(chat)" class="gs-group-avatar">
                    <ion-icon name="people" class="gs-group-icon"></ion-icon>
                  </div>
                  
                  <!-- Avatar para chat privado -->
                  <div v-else class="gs-private-avatar">
                    <!-- Mostrar foto de perfil del otro usuario -->
                    <img 
                      v-if="getOtherUserPhoto(chat)" 
                      :src="getProfileImageSrc(getOtherUserPhoto(chat))" 
                      :alt="getChatDisplayName(chat)"
                      class="gs-avatar-image"
                      @error="handleImageError"
                    />
                    <!-- Fallback a iniciales si no hay foto -->
                    <div 
                      v-else 
                      class="gs-avatar-initials"
                      :style="{ display: getOtherUserPhoto(chat) ? 'none' : 'flex' }"
                    >
                      {{ getInitials(getChatDisplayName(chat)) }}
                    </div>
                  </div>
                </div>
                <div v-if="chat.mensajesNoLeidos > 0" class="gs-unread-dot"></div>
              </div>
              
              <div class="gs-chat-info">
                <div class="gs-chat-header">
                  <h3 class="gs-chat-title">{{ getChatDisplayName(chat) }}</h3>
                  <div class="gs-chat-meta">
                    <span v-if="chat.ultimoMensaje" class="gs-chat-time">
                      {{ formatMessageTime(chat.ultimoMensaje.fechaEnvio) }}
                    </span>
                  </div>
                </div>
                
                <div class="gs-chat-preview">
                  <p v-if="chat.ultimoMensaje" class="gs-last-message">
                    <span v-if="chat.ultimoMensaje.usuarioId === currentUserId" class="gs-message-prefix">
                      Tú: 
                    </span>
                    <span v-else-if="isGroupChat(chat)" class="gs-message-prefix">
                      {{ getUsernameById(chat.ultimoMensaje.usuarioId, chat) }}: 
                    </span>
                    {{ chat.ultimoMensaje.contenido }}
                  </p>
                  <p v-else class="gs-no-messages">
                    No hay mensajes aún
                  </p>
                </div>
              </div>
              
              <div class="gs-chat-actions">
                <div 
                  v-if="chat.mensajesNoLeidos > 0" 
                  class="gs-unread-badge"
                >
                  {{ formatUnreadCount(chat.mensajesNoLeidos) }}
                </div>
                <!-- Botón de opciones para grupos -->
                <ion-button 
                  v-if="isGroupChat(chat)"
                  @click.stop="openGroupOptions(chat)"
                  fill="clear" 
                  size="small"
                  class="group-options-btn"
                >
                  <ion-icon name="ellipsis-vertical"></ion-icon>
                </ion-button>
                <ion-icon name="chevron-forward" class="gs-arrow-icon"></ion-icon>
              </div>
            </div>
          </transition-group>
        </div>

        <!-- Empty state -->
        <div v-else class="gs-empty-state">
          <div class="gs-empty-content">
            <ion-icon name="chatbubbles-outline" class="gs-empty-icon"></ion-icon>
            <h3 class="gs-empty-title">No tienes chats</h3>
            <p class="gs-empty-description">
              Cuando tengas conversaciones, aparecerán aquí
            </p>
            <button 
              @click="showCreateChatModal = true"
              class="gs-button gs-button-primary"
            >
              <ion-icon name="add" slot="start"></ion-icon>
              Crear tu primer chat
            </button>
          </div>
        </div>

        <!-- Connection status banner -->
        <div 
          v-if="connectionStatus.error" 
          class="gs-connection-banner gs-connection-error"
        >
          <ion-icon name="wifi-outline"></ion-icon>
          <span>{{ connectionStatus.error }}</span>
          <button @click="handleReconnect" class="gs-button gs-button-sm gs-button-ghost">
            Reconectar
          </button>
        </div>

        <div 
          v-else-if="!connectionStatus.isConnected && !connectionStatus.isConnecting" 
          class="gs-connection-banner gs-connection-warning"
        >
          <ion-icon name="cloud-offline-outline"></ion-icon>
          <span>Sin conexión en tiempo real</span>
          <button @click="handleReconnect" class="gs-button gs-button-sm gs-button-ghost">
            Conectar
          </button>
        </div>
      </div>

      <!-- Floating Action Button -->
      <ion-fab vertical="bottom" horizontal="end" slot="fixed">
        <ion-fab-button 
          color="primary" 
          @click="showCreateChatModal = true"
          class="gs-fab"
        >
          <ion-icon name="add"></ion-icon>
        </ion-fab-button>
      </ion-fab>
    </ion-content>

    <!-- Create Chat Modal -->
    <CreateChatModal
      :is-open="showCreateChatModal"
      @close="handleCloseCreateChatModal"
      @chat-created="handleChatCreated"
    />

    <!-- Group Options Modal -->
    <GroupOptionsModal
      :is-open="showGroupOptionsModal"
      :chat="selectedChat"
      :current-user-id="currentUserId"
      @close="closeGroupOptions"
      @leave-group="handleLeaveGroup"
      @update-group="handleUpdateGroup"
    />

    <!-- Toast notifications -->
    <ion-toast
      :is-open="showToast"
      :message="toastMessage"
      :duration="3000"
      :color="toastColor"
      @did-dismiss="showToast = false"
    ></ion-toast>
  </ion-page>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, onActivated } from 'vue';
import { useRouter } from 'vue-router';
import {
  IonPage, IonHeader, IonToolbar, IonTitle, 
  IonContent, IonIcon, IonButtons, IonMenuButton, IonButton,
  IonFab, IonFabButton, IonToast,toastController,IonRefresher,IonRefresherContent
} from '@ionic/vue';
import { useWebSocket, type ChatMessage, type ChatNotification } from '@/services/websocket/WebSocketService';
import { useAuth } from '@/composables/useAuth';
import { useApi } from '@/composables/useApi';
import config from '@/config/config';
import CreateChatModal from '@/views/chat/CreateChatModal.vue';
import GroupOptionsModal from '@/views/chat/GroupOptionsModal.vue';

// Types
interface Chat {
  chatId: number;
  nombreChat?: string;
  tipo: string;
  ultimoMensaje?: {
    mensajeId: number;
    usuarioId: number;
    contenido: string;
    fechaEnvio: string;
  };
  mensajesNoLeidos: number;
  participantes: Array<{
    usuarioId: number;
    username: string;
    skin?: string;
    fotoPerfil?: string;
  }>;
}

// Composables
const router = useRouter();
const { usuario } = useAuth();
const { showToast: apiShowToast } = useApi();
const {
  connectionStatus,
  connect,
  disconnect,
  reconnect,
  isConnected
} = useWebSocket();

// Reactive data
const chats = ref<Chat[]>([]);
const isLoading = ref(true);
const showCreateChatModal = ref(false);
const showGroupOptionsModal = ref(false);
const selectedChat = ref<Chat | null>(null);
const showToast = ref(false);
const toastMessage = ref('');
const toastColor = ref('success');

// Computed
const currentUserId = computed((): number => {
  return usuario.value?.usuarioId || 0;
});

const connectionStatusClass = computed(() => {
  if (connectionStatus.isConnected) return 'gs-connected';
  if (connectionStatus.isConnecting) return 'gs-connecting';
  return 'gs-disconnected';
});

const connectionIcon = computed(() => {
  if (connectionStatus.isConnected) return 'wifi';
  if (connectionStatus.isConnecting) return 'sync';
  return 'wifi-outline';
});

// Methods

const handleRefresh = async (event: any) => {
  try {
    console.log('🔄 Actualizando Lista de Chats...')
    
    // Refrescar la lista de chats
    await refreshChatList()
    
    // Mostrar toast de confirmación
    const toast = await toastController.create({
      message: '✅ Chats actualizados',
      duration: 2000,
      position: 'top',
      color: 'success'
    })
    await toast.present()
    
  } catch (error) {
    console.error('❌ Error actualizando chats:', error)
    
    const toast = await toastController.create({
      message: '❌ Error al actualizar chats',
      duration: 3000,
      position: 'top',
      color: 'danger'
    })
    await toast.present()
    
  } finally {
    event.target.complete()
  }
}
const getProfileImageSrc = (fotoPerfil: string | null | undefined): string | undefined => {
  if (!fotoPerfil) return undefined; // Cambiar de null a undefined
  
  // Si ya es una URL completa
  if (fotoPerfil.startsWith('http://') || fotoPerfil.startsWith('https://')) {
    return fotoPerfil;
  }
  
  // Si es base64 sin prefijo, añadir el prefijo
  if (!fotoPerfil.startsWith('data:image/')) {
    return `data:image/jpeg;base64,${fotoPerfil}`;
  }
  
  // Si ya tiene el prefijo, devolverlo tal como está
  return fotoPerfil;
};

const handleImageError = (event: Event) => {
  console.error('Error cargando imagen de perfil:', event);
  // Ocultar la imagen y mostrar iniciales como fallback
  const imgElement = event.target as HTMLImageElement;
  imgElement.style.display = 'none';
  
  // Buscar el elemento de iniciales y mostrarlo
  const avatarContainer = imgElement.closest('.gs-private-avatar');
  const initialsElement = avatarContainer?.querySelector('.gs-avatar-initials') as HTMLElement;
  if (initialsElement) {
    initialsElement.style.display = 'flex';
  }
};

const getInitials = (name: string): string => {
  if (!name) return '?';
  
  const words = name.trim().split(' ');
  if (words.length >= 2) {
    return (words[0][0] + words[1][0]).toUpperCase();
  }
  return name.charAt(0).toUpperCase();
};

const getChatDisplayName = (chat: Chat): string => {
  // Si es un grupo (tipo GRUPO), SIEMPRE mostrar nombreChat
  if (chat.tipo === 'GRUPO') {
    return chat.nombreChat?.trim() || `Grupo #${chat.chatId}`;
  }
  
  // Si es privado (tipo PRIVADO), mostrar el otro usuario
  if (chat.tipo === 'PRIVADO' && chat.participantes && chat.participantes.length === 2) {
    const currentUsername = usuario.value?.username;
    const otherUser = chat.participantes.find(p => p.username !== currentUsername);
    
    if (otherUser && otherUser.username) {
      return otherUser.username;
    }
  }
  
  // Fallback
  return chat.nombreChat?.trim() || `Chat #${chat.chatId}`;
};

const getOtherUserPhoto = (chat: Chat): string | null => {
  // Solo para chats privados
  if (chat.tipo !== 'PRIVADO' || !chat.participantes || chat.participantes.length !== 2) {
    return null;
  }
  
  const currentUsername = usuario.value?.username;
  const otherUser = chat.participantes.find(p => p.username !== currentUsername);
  
  return otherUser?.fotoPerfil || null;
};

const getUsernameById = (userId: number, chat: Chat): string => {
  const user = chat.participantes?.find(p => p.usuarioId === userId);
  return user?.username || 'Usuario';
};

const isGroupChat = (chat: Chat): boolean => {
  return chat.tipo === 'GRUPO';
};

const formatMessageTime = (dateString: string): string => {
  const date = new Date(dateString);
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffHours = diffMs / (1000 * 60 * 60);
  const diffDays = diffMs / (1000 * 60 * 60 * 24);
  
  if (diffHours < 24) {
    return date.toLocaleTimeString('es-ES', { 
      hour: '2-digit', 
      minute: '2-digit' 
    });
  } else if (diffDays < 7) {
    return date.toLocaleDateString('es-ES', { 
      weekday: 'short'
    });
  } else {
    return date.toLocaleDateString('es-ES', { 
      day: '2-digit', 
      month: '2-digit' 
    });
  }
};

const formatUnreadCount = (count: number): string => {
  return count > 99 ? '99+' : count.toString();
};

const openChat = (chat: Chat) => {
  router.push({
    name: 'Chat',
    params: { chatId: chat.chatId.toString() }
  });
};

const openGroupOptions = (chat: Chat) => {
  selectedChat.value = chat;
  showGroupOptionsModal.value = true;
};

const closeGroupOptions = () => {
  showGroupOptionsModal.value = false;
  selectedChat.value = null;
};

const handleLeaveGroup = async (chatId: number) => {
  try {
    const response = await fetch(
      `${config.api.fullApiUrl}/chat/${chatId}/participantes/${currentUserId.value}`,
      {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${localStorage.getItem(config.storage.token)}`,
          'Content-Type': 'application/json'
        }
      }
    );

    if (response.ok) {
      showToastMessage('Has salido del grupo', 'success');
      // Recargar lista de chats
      await refreshChatList();
    } else {
      showToastMessage('Error al salir del grupo', 'danger');
    }
  } catch (error) {
    console.error('Error saliendo del grupo:', error);
    showToastMessage('Error de conexión', 'danger');
  } finally {
    closeGroupOptions();
  }
};

const handleUpdateGroup = () => {
  // Recargar lista después de actualizar grupo
  refreshChatList();
  closeGroupOptions();
};

const handleReconnect = async () => {
  if (!currentUserId.value) return;
  
  try {
    await reconnect();
    showToastMessage('Reconectando...', 'warning');
  } catch (error) {
    showToastMessage('Error al reconectar', 'danger');
  }
};

// Refrescar lista de chats
const refreshChatList = async () => {
  if (!currentUserId.value) return;
  
  try {
    console.log('🔄 Refrescando lista de chats...');
    const response = await fetch(`${config.api.fullApiUrl}/chat/usuario/${currentUserId.value}`, {
      headers: {
        'Authorization': `Bearer ${localStorage.getItem(config.storage.token)}`,
        'Content-Type': 'application/json'
      }
    });
    
    if (response.ok) {
      const chatsData = await response.json();
      chats.value = chatsData;
      console.log('✅ Lista de chats actualizada desde API:', chatsData.length);
    } else {
      console.error('❌ Error actualizando chats desde API');
    }
  } catch (error) {
    console.error('❌ Error refrescando chats:', error);
  }
};

const loadChatsFromAPI = async () => {
  if (!currentUserId.value) return;
  
  try {
    const response = await fetch(`${config.api.fullApiUrl}/chat/usuario/${currentUserId.value}`, {
      headers: {
        'Authorization': `Bearer ${localStorage.getItem(config.storage.token)}`,
        'Content-Type': 'application/json'
      }
    });
    
    if (response.ok) {
      const chatsData = await response.json();
      chats.value = chatsData;
      console.log('📋 Chats cargados desde API:', chatsData.length);
    }
  } catch (error) {
    console.error('❌ Error cargando chats desde API:', error);
    showToastMessage('Error cargando chats', 'danger');
  }
};

const setupWebSocketConnection = async () => {
  if (!currentUserId.value) return;

  try {
    console.log('🔌 Configurando conexión WebSocket para lista de chats...');
    
    const success = await connect(currentUserId.value, {
      onMessage: handleGlobalMessage,
      onChatNotification: handleChatNotification,
      onChatListUpdate: handleChatListUpdate,
      onConnectionChange: (status) => {
        console.log('🔄 Estado de conexión:', status);
      }
    });
    
    if (success) {
      console.log('✅ WebSocket conectado para lista de chats');
    } else {
      console.warn('⚠️ No se pudo conectar WebSocket, usando API REST');
      await loadChatsFromAPI();
    }
  } catch (error) {
    console.error('❌ Error conectando WebSocket:', error);
    await loadChatsFromAPI();
  } finally {
    isLoading.value = false;
  }
};

// WebSocket Event Handlers
const handleGlobalMessage = (message: ChatMessage) => {
  console.log('📨 Mensaje global recibido:', message);
  
  // Actualizar último mensaje en la lista de chats
  const chatIndex = chats.value.findIndex(chat => chat.chatId === message.chatId);
  if (chatIndex !== -1) {
    const chat = chats.value[chatIndex];
    
    // Actualizar último mensaje
    chat.ultimoMensaje = {
      mensajeId: message.mensajeId,
      usuarioId: message.usuarioId,
      contenido: message.contenido,
      fechaEnvio: message.fechaEnvio
    };
    
    // Incrementar contador de no leídos si el mensaje no es del usuario actual
    if (message.usuarioId !== currentUserId.value) {
      chat.mensajesNoLeidos++;
    }
    
    // Mover chat al principio de la lista
    const updatedChat = chats.value.splice(chatIndex, 1)[0];
    chats.value.unshift(updatedChat);
  }
};

const handleChatNotification = (notification: ChatNotification) => {
  console.log('🔔 Notificación de chat:', notification);
  
  // Mostrar notificación toast si la app no está en primer plano
  if (document.hidden) {
    showToastMessage(
      `${notification.remitenteNombre}: ${notification.contenidoPreview}`,
      'primary'
    );
  }
};

const handleChatListUpdate = (updatedChats: Chat[]) => {
  console.log('📋 Lista de chats actualizada:', updatedChats.length);
  chats.value = updatedChats;
};

// Modal Event Handlers
const handleCloseCreateChatModal = () => {
  showCreateChatModal.value = false;
};

const handleChatCreated = async (newChat: any) => {
  console.log('✅ Chat creado desde modal:', newChat);
  showToastMessage('Chat creado exitosamente', 'success');
  
  // Actualizar la lista de chats
  if (!isConnected()) {
    await loadChatsFromAPI();
  }
  
  // Navegar al nuevo chat si es privado
  if (newChat.chatId) {
    router.push({
      name: 'Chat',
      params: { chatId: newChat.chatId.toString() }
    });
  }
};

const showToastMessage = (message: string, color: string = 'success') => {
  toastMessage.value = message;
  toastColor.value = color;
  showToast.value = true;
};

// Lifecycle
onMounted(async () => {
  console.log('📱 ChatListView montado');
  
  if (!currentUserId.value) {
    showToastMessage('Usuario no autenticado', 'danger');
    router.push('/');
    return;
  }
  
  await setupWebSocketConnection();
});

onActivated(async () => {
  console.log('📱 ChatListView activado - refrescando chats...');
  
  // Refrescar la lista al regresar de un chat
  if (currentUserId.value) {
    await refreshChatList();
  }
});

onUnmounted(() => {
  console.log('📱 ChatListView desmontado');
  // No desconectamos el WebSocket aquí porque puede ser usado por otros componentes
});
</script>

<style scoped>
/* Reutilizar los estilos existentes + nuevos para grupo */

.group-options-btn {
  --color: var(--gs-text-tertiary);
  margin-right: var(--gs-space-xs);
}

.group-options-btn:hover {
  --color: var(--gs-primary-500);
}

.gs-chat-list-page {
  --ion-background-color: var(--gs-bg-primary);
}

.gs-header {
  --background: var(--gs-bg-secondary);
  border-bottom: 1px solid var(--gs-border-primary);
}

.gs-page-title {
  display: flex;
  align-items: center;
  font-weight: var(--gs-font-semibold);
  color: var(--gs-text-primary);
}

.gs-title-content {
  display: flex;
  align-items: center;
  gap: var(--gs-space-sm);
  width: 100%;
}

.gs-title-icon {
  font-size: 1.25rem;
  color: var(--gs-primary-500);
}

.gs-connection-indicator {
  margin-left: auto;
  padding: var(--gs-space-xs);
  border-radius: var(--gs-radius-full);
  transition: all var(--gs-transition-fast);
}

.gs-connection-indicator.gs-connected {
  color: var(--gs-secondary-500);
}

.gs-connection-indicator.gs-connecting {
  color: var(--gs-warning-500);
  animation: gs-pulse 1s infinite;
}

.gs-connection-indicator.gs-disconnected {
  color: var(--gs-error-500);
}

@keyframes gs-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.gs-content {
  --background: var(--gs-bg-primary);
  --padding-bottom: 80px;
}

.gs-chat-container {
  padding: var(--gs-space-md);
  min-height: 100%;
}

/* === LOADING STATES === */

.gs-loading-state {
  padding: var(--gs-space-lg) 0;
}

.gs-skeleton-container {
  display: flex;
  flex-direction: column;
  gap: var(--gs-space-md);
}

.gs-skeleton-chat-item {
  display: flex;
  align-items: center;
  gap: var(--gs-space-md);
  padding: var(--gs-space-md);
  background: var(--gs-bg-secondary);
  border-radius: var(--gs-radius-lg);
}

.gs-skeleton-avatar {
  width: 48px;
  height: 48px;
  border-radius: var(--gs-radius-full);
}

.gs-skeleton-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: var(--gs-space-sm);
}

.gs-skeleton-line {
  height: 16px;
  border-radius: var(--gs-radius-sm);
}

.gs-skeleton-title {
  width: 60%;
}

.gs-skeleton-subtitle {
  width: 80%;
}

/* === CHAT LIST === */

.gs-chat-list {
  display: flex;
  flex-direction: column;
  gap: var(--gs-space-sm);
}

.gs-chat-item {
  display: flex;
  align-items: center;
  gap: var(--gs-space-md);
  padding: var(--gs-space-md);
  background: var(--gs-bg-secondary);
  border: 1px solid var(--gs-border-primary);
  border-radius: var(--gs-radius-lg);
  cursor: pointer;
  transition: all var(--gs-transition-normal);
  position: relative;
  overflow: hidden;
}

.gs-chat-item:hover {
  transform: translateY(-2px);
  box-shadow: var(--gs-shadow-md);
  border-color: var(--gs-primary-200);
}

.gs-chat-item.gs-has-unread {
  border-color: var(--gs-primary-300);
  background: linear-gradient(135deg, var(--gs-bg-secondary) 0%, var(--gs-primary-50) 100%);
}

.gs-chat-item::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  background: var(--gs-primary-500);
  transform: scaleY(0);
  transition: transform var(--gs-transition-normal);
  transform-origin: bottom;
}

.gs-chat-item:hover::before {
  transform: scaleY(1);
}

.gs-chat-item.gs-has-unread::before {
  transform: scaleY(1);
  background: var(--gs-primary-600);
}

/* === CHAT AVATAR === */

.gs-chat-avatar {
  position: relative;
  flex-shrink: 0;
}

.gs-avatar-circle {
  width: 48px;
  height: 48px;
  border-radius: var(--gs-radius-full);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  position: relative;
}

/* Avatar para grupos */
.gs-group-avatar {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, var(--gs-secondary-500), var(--gs-secondary-600));
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--gs-radius-full);
}

.gs-group-icon {
  color: white;
  font-size: 20px;
}

/* Avatar para chats privados */
.gs-private-avatar {
  width: 100%;
  height: 100%;
  border-radius: var(--gs-radius-full);
}

.gs-avatar-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: var(--gs-radius-full);
  border: 2px solid var(--gs-primary-200);
}

.gs-avatar-initials {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, var(--gs-primary-500), var(--gs-primary-600));
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 16px;
  font-weight: var(--gs-font-semibold);
  border-radius: var(--gs-radius-full);
}

.gs-unread-dot {
  position: absolute;
  top: -2px;
  right: -2px;
  width: 12px;
  height: 12px;
  background: var(--gs-error-500);
  border-radius: var(--gs-radius-full);
  border: 2px solid var(--gs-bg-secondary);
  animation: gs-pulse 2s infinite;
}

/* === CHAT INFO === */

.gs-chat-info {
  flex: 1;
  min-width: 0;
}

.gs-chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--gs-space-xs);
}

.gs-chat-title {
  font-size: var(--gs-text-lg);
  font-weight: var(--gs-font-semibold);
  color: var(--gs-text-primary);
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  line-height: 1.4;
}

.gs-no-messages {
  font-style: italic;
  color: var(--gs-text-tertiary);
}

.gs-message-prefix {
  font-weight: var(--gs-font-medium);
  color: var(--gs-text-tertiary);
}

/* === CHAT ACTIONS === */

.gs-chat-actions {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: var(--gs-space-sm);
  flex-shrink: 0;
}

.gs-unread-badge {
  background: var(--gs-primary-500);
  color: white;
  font-size: var(--gs-text-xs);
  font-weight: var(--gs-font-semibold);
  padding: var(--gs-space-xs) var(--gs-space-sm);
  border-radius: var(--gs-radius-full);
  min-width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  animation: gs-pulse 2s infinite;
}

.gs-arrow-icon {
  color: var(--gs-text-tertiary);
  font-size: 16px;
  transition: all var(--gs-transition-fast);
}

.gs-chat-item:hover .gs-arrow-icon {
  color: var(--gs-primary-500);
  transform: translateX(4px);
}

/* === EMPTY STATE === */

.gs-empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
  text-align: center;
  padding: var(--gs-space-2xl);
}

.gs-empty-content {
  max-width: 300px;
}

.gs-empty-icon {
  font-size: 4rem;
  color: var(--gs-text-tertiary);
  margin-bottom: var(--gs-space-lg);
  opacity: 0.6;
}

.gs-empty-title {
  font-size: var(--gs-text-xl);
  font-weight: var(--gs-font-semibold);
  color: var(--gs-text-primary);
  margin: 0 0 var(--gs-space-sm) 0;
}

.gs-empty-description {
  font-size: var(--gs-text-base);
  color: var(--gs-text-secondary);
  margin: 0 0 var(--gs-space-xl) 0;
  line-height: var(--gs-leading-relaxed);
}

/* === CONNECTION BANNER === */

.gs-connection-banner {
  position: sticky;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  gap: var(--gs-space-sm);
  padding: var(--gs-space-sm) var(--gs-space-md);
  margin-top: var(--gs-space-md);
  border-radius: var(--gs-radius-lg);
  font-size: var(--gs-text-sm);
  z-index: 10;
}

.gs-connection-error {
  background: var(--gs-error-50);
  color: var(--gs-error-700);
  border: 1px solid var(--gs-error-200);
}

.gs-connection-warning {
  background: var(--gs-warning-50);
  color: var(--gs-warning-700);
  border: 1px solid var(--gs-warning-200);
}

/* === BUTTONS === */

.gs-button {
  display: inline-flex;
  align-items: center;
  gap: var(--gs-space-sm);
  padding: var(--gs-space-sm) var(--gs-space-md);
  border: none;
  border-radius: var(--gs-radius-md);
  font-size: var(--gs-text-sm);
  font-weight: var(--gs-font-medium);
  cursor: pointer;
  transition: all var(--gs-transition-fast);
  text-decoration: none;
}

.gs-button-primary {
  background: var(--gs-primary-500);
  color: white;
}

.gs-button-primary:hover {
  background: var(--gs-primary-600);
  transform: translateY(-1px);
}

.gs-button-ghost {
  background: transparent;
  color: inherit;
  padding: var(--gs-space-xs) var(--gs-space-sm);
}

.gs-button-ghost:hover {
  background: rgba(255, 255, 255, 0.1);
}

.gs-button-sm {
  padding: var(--gs-space-xs) var(--gs-space-sm);
  font-size: var(--gs-text-xs);
}

/* === FAB === */

.gs-fab {
  --background: var(--gs-primary-500);
  --box-shadow: var(--gs-shadow-lg);
  transition: all var(--gs-transition-normal);
}

.gs-fab:hover {
  --background: var(--gs-primary-600);
  transform: translateY(-2px);
  --box-shadow: var(--gs-shadow-xl);
}

/* === RESPONSIVE === */

@media (max-width: 768px) {
  .gs-chat-container {
    padding: var(--gs-space-sm);
  }
  
  .gs-chat-item {
    padding: var(--gs-space-sm);
  }
  
  .gs-avatar-circle {
    width: 44px;
    height: 44px;
  }
  
  .gs-chat-title {
    font-size: var(--gs-text-base);
  }
  
  .gs-last-message {
    font-size: var(--gs-text-xs);
  }
}

/* === DARK MODE === */

@media (prefers-color-scheme: dark) {
  .gs-avatar-image {
    border-color: var(--gs-primary-700);
  }
  
  .gs-connection-error {
    background: var(--gs-error-900);
    color: var(--gs-error-300);
    border-color: var(--gs-error-800);
  }
  
  .gs-connection-warning {
    background: var(--gs-warning-900);
    color: var(--gs-warning-300);
    border-color: var(--gs-warning-800);
  }
}

/* === ANIMATIONS === */

@keyframes gs-skeleton {
  0% {
    background-color: var(--gs-gray-200);
  }
  50% {
    background-color: var(--gs-gray-300);
  }
  100% {
    background-color: var(--gs-gray-200);
  }
}

.gs-skeleton {
  animation: gs-skeleton 1.5s ease-in-out infinite;
  background-color: var(--gs-gray-200);
}

/* Transiciones para la lista de chats */
.gs-chat-item-enter-active,
.gs-chat-item-leave-active {
  transition: all 0.3s ease;
}

.gs-chat-item-enter-from {
  opacity: 0;
  transform: translateX(-20px);
}

.gs-chat-item-leave-to {
  opacity: 0;
  transform: translateX(20px);
}

.gs-chat-item-move {
  transition: transform 0.3s ease;
}


.gs-last-message,
.gs-no-messages {
  font-size: var(--gs-text-sm);
  color: var(--gs-text-secondary);
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
  margin-right: var(--gs-space-sm);
}

.gs-chat-meta {
  display: flex;
  align-items: center;
  gap: var(--gs-space-xs);
}

.gs-chat-time {
  font-size: var(--gs-text-xs);
  color: var(--gs-text-tertiary);
  white-space: nowrap;
}

.gs-chat-preview {
  display: flex;
  align-items: center;
  gap: var(--gs-space-xs);
}
</style>