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
                  <ion-icon name="chatbubble-ellipses"></ion-icon>
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

    <!-- Create Chat Modal Integrado -->
    <CreateChatModal
      :is-open="showCreateChatModal"
      @close="handleCloseCreateChatModal"
      @chat-created="handleChatCreated"
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
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import {
  IonPage, IonHeader, IonToolbar, IonTitle, 
  IonContent, IonIcon, IonButtons, IonMenuButton,
  IonFab, IonFabButton, IonToast
} from '@ionic/vue';
import { useWebSocket, type ChatMessage, type ChatNotification } from '@/services/websocket/WebSocketService';
import { useAuth } from '@/composables/useAuth';
import { useApi } from '@/composables/useApi';
import config from '@/config/config';
import CreateChatModal from '@/views/chat/CreateChatModal.vue';

// Types
interface Chat {
  chatId: number;
  nombreChat?: string;
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
const showToast = ref(false);
const toastMessage = ref('');
const toastColor = ref('success');

// Computed
const currentUserId = computed(() => usuario.value?.usuarioId);

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
const getChatDisplayName = (chat: Chat): string => {
  // Para chats grupales, usar nombreChat si existe
  if (chat.nombreChat?.trim() && chat.participantes && chat.participantes.length > 2) {
    return chat.nombreChat;
  }
  
  // Para chats privados (2 participantes), mostrar solo el otro usuario
  if (chat.participantes && chat.participantes.length === 2) {
    // Obtener el username del usuario actual
    const currentUsername = usuario.value?.username;
    
    // Encontrar el otro usuario
    const otherUser = chat.participantes.find(p => p.username !== currentUsername);
    
    if (otherUser && otherUser.username) {
      return otherUser.username;
    }
  }
  
  // Fallback para casos edge
  return chat.nombreChat?.trim() || `Chat #${chat.chatId}`;
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

const handleReconnect = async () => {
  if (!currentUserId.value) return;
  
  try {
    await reconnect();
    showToastMessage('Reconectando...', 'warning');
  } catch (error) {
    showToastMessage('Error al reconectar', 'danger');
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

onUnmounted(() => {
  console.log('📱 ChatListView desmontado');
  // No desconectamos el WebSocket aquí porque puede ser usado por otros componentes
});
</script>

<style scoped>
/* ===== CHAT LIST STYLES ===== */

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
  background: linear-gradient(135deg, var(--gs-primary-500), var(--gs-primary-600));
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 20px;
  box-shadow: var(--gs-shadow-sm);
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

.gs-last-message,
.gs-no-messages {
  font-size: var(--gs-text-sm);
  color: var(--gs-text-secondary);
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
  position: fixed;
  top: calc(var(--ion-safe-area-top) + 60px);
  left: var(--gs-space-md);
  right: var(--gs-space-md);
  display: flex;
  align-items: center;
  gap: var(--gs-space-sm);
  padding: var(--gs-space-sm) var(--gs-space-md);
  border-radius: var(--gs-radius-lg);
  font-size: var(--gs-text-sm);
  font-weight: var(--gs-font-medium);
  z-index: var(--gs-z-overlay);
  animation: gs-slide-down 0.3s ease-out;
  box-shadow: var(--gs-shadow-lg);
}

.gs-connection-banner.gs-connection-error {
  background: var(--gs-error-500);
  color: white;
}

.gs-connection-banner.gs-connection-warning {
  background: var(--gs-warning-500);
  color: white;
}

@keyframes gs-slide-down {
  from {
    opacity: 0;
    transform: translateY(-100%);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
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

/* === ANIMATIONS === */

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

/* === RESPONSIVE === */

@media (max-width: 768px) {
  .gs-chat-container {
    padding: var(--gs-space-sm);
  }
  
  .gs-chat-item {
    padding: var(--gs-space-sm);
  }
  
  .gs-avatar-circle {
    width: 40px;
    height: 40px;
    font-size: 18px;
  }
  
  .gs-chat-title {
    font-size: var(--gs-text-base);
  }
  
  .gs-last-message {
    font-size: var(--gs-text-xs);
  }
  
  .gs-connection-banner {
    left: var(--gs-space-sm);
    right: var(--gs-space-sm);
  }
}

/* === DARK MODE === */

@media (prefers-color-scheme: dark) {
  .gs-chat-item.gs-has-unread {
    background: linear-gradient(135deg, var(--gs-bg-secondary) 0%, var(--gs-primary-900) 100%);
  }
}
</style>