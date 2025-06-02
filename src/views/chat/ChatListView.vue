<template>
  <ion-page class="chat-page">
    <ion-header class="chat-header">
      <ion-toolbar>
        <ion-buttons slot="start"> 
           <ion-menu-button :auto-hide="false"></ion-menu-button>
        </ion-buttons>
        <ion-title class="page-title">
          <ion-icon name="chatbubbles" class="title-icon"></ion-icon>
          Mis Chats
          <div class="connection-status" :class="{ 'connected': isConnected, 'disconnected': !isConnected }">
            <ion-icon :name="isConnected ? 'wifi' : 'wifi-outline'"></ion-icon>
          </div>
        </ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content class="chat-content">
      <div class="chat-container">
        <!-- Loading state -->
        <div v-if="loading" class="loading-state">
          <ion-spinner name="dots"></ion-spinner>
          <p>Cargando chats...</p>
        </div>

        <!-- Chat list -->
        <div v-else-if="chats.length > 0" class="chat-list-wrapper">
          <div
            v-for="chat in chats"
            :key="chat.chatId"
            class="chat-item"
            @click="abrirChat(chat)"
          >
            <div class="chat-avatar">
              <ion-icon name="chatbubble-ellipses"></ion-icon>
            </div>
            
            <div class="chat-info">
              <h2 class="chat-title">{{ getOtherUserName(chat) }}</h2>
              <p class="last-message" v-if="chat.ultimoMensaje">
                {{ chat.ultimoMensaje.contenido }}
              </p>
              <p class="last-message-time" v-if="chat.ultimoMensaje">
                {{ formatTime(chat.ultimoMensaje.fechaEnvio) }}
              </p>
              <p v-if="chat.mensajesNoLeidos > 0" class="unread-info">
                {{ chat.mensajesNoLeidos }} mensajes no leídos
              </p>
            </div>
            
            <div class="chat-meta">
              <div 
                v-if="chat.mensajesNoLeidos > 0" 
                class="unread-badge"
              >
                {{ chat.mensajesNoLeidos }}
              </div>
              <ion-icon name="chevron-forward" class="arrow-icon"></ion-icon>
            </div>
          </div>
        </div>

        <!-- Empty state -->
        <div class="empty-state" v-else>
          <ion-icon name="chatbubbles-outline" class="empty-icon"></ion-icon>
          <h3 class="empty-title">No tienes chats</h3>
          <p class="empty-description">Cuando tengas conversaciones, aparecerán aquí</p>
          <ion-button 
            fill="outline" 
            color="primary" 
            @click="mostrarModalCrearChat"
            class="create-first-chat-btn"
          >
            <ion-icon name="add" slot="start"></ion-icon>
            Crear tu primer chat
          </ion-button>
        </div>
      </div>

      <!-- Floating Action Button -->
      <ion-fab vertical="bottom" horizontal="end" slot="fixed">
        <ion-fab-button 
          color="primary" 
          @click="mostrarModalCrearChat"
          class="fab-button"
        >
          <ion-icon name="add"></ion-icon>
        </ion-fab-button>
      </ion-fab>
    </ion-content>

    <!-- Modal para crear chat -->
    <ion-modal :is-open="showCreateChatModal" @did-dismiss="cerrarModalCrearChat">
      <ion-header>
        <ion-toolbar>
          <ion-title>Crear Nuevo Chat</ion-title>
          <ion-buttons slot="end">
            <ion-button @click="cerrarModalCrearChat">
              <ion-icon name="close"></ion-icon>
            </ion-button>
          </ion-buttons>
        </ion-toolbar>
      </ion-header>
      
      <ion-content class="modal-content">
        <div class="modal-container">
          <!-- Tipo de chat -->
          <div class="chat-type-selector">
            <ion-segment v-model="chatType" @ionChange="onChatTypeChange">
              <ion-segment-button value="privado">
                <ion-icon name="person"></ion-icon>
                <ion-label>Chat Privado</ion-label>
              </ion-segment-button>
              <ion-segment-button value="grupal">
                <ion-icon name="people"></ion-icon>
                <ion-label>Chat Grupal</ion-label>
              </ion-segment-button>
            </ion-segment>
          </div>

          <!-- Chat Privado -->
          <div v-if="chatType === 'privado'" class="chat-form">
            <ion-item>
              <ion-label position="stacked">Usuario</ion-label>
              <ion-input
                v-model="selectedUserId"
                placeholder="ID del usuario"
                type="number"
                :disabled="creatingChat"
              ></ion-input>
            </ion-item>
          </div>

          <!-- Chat Grupal -->
          <div v-if="chatType === 'grupal'" class="chat-form">
            <ion-item>
              <ion-label position="stacked">Nombre del grupo</ion-label>
              <ion-input
                v-model="groupName"
                placeholder="Ingresa el nombre del grupo"
                :disabled="creatingChat"
              ></ion-input>
            </ion-item>
            
            <ion-item>
              <ion-label position="stacked">IDs de participantes (separados por comas)</ion-label>
              <ion-input
                v-model="participantIds"
                placeholder="1, 2, 3"
                :disabled="creatingChat"
              ></ion-input>
            </ion-item>
          </div>

          <!-- Botones de acción -->
          <div class="modal-actions">
            <ion-button 
              expand="full" 
              @click="crearChat"
              :disabled="!canCreateChat || creatingChat"
            >
              <ion-spinner v-if="creatingChat" name="dots"></ion-spinner>
              <span v-else>Crear Chat</span>
            </ion-button>
          </div>
        </div>
      </ion-content>
    </ion-modal>

    <!-- Toast para notificaciones -->
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
import { ref, onMounted, onUnmounted, computed } from "vue";
import { useRouter } from "vue-router";
import { obtenerChatsDeUsuario } from "@/services/chatApi";
import {
  connectGlobal,
  disconnect,
  isConnected as checkConnection,
  crearChatPrivado,
  crearChatGrupal,
  obtenerChatsUsuario,
  addStateChangeListener,
  removeStateChangeListener
} from "@/services/chatWebSocket";
import {
  IonPage, IonHeader, IonToolbar, IonTitle, 
  IonContent, IonIcon, IonButtons, IonMenuButton,
  IonFab, IonFabButton, IonModal, IonButton,
  IonSegment, IonSegmentButton, IonLabel,
  IonItem, IonInput, IonSpinner, IonToast
} from '@ionic/vue';

interface ChatData {
  chatId: number;
  nombreChat?: string;
  ultimoMensaje?: {
    contenido: string;
    fechaEnvio: string;
  };
  mensajesNoLeidos: number;
  participantes: Array<{
    username: string;
    skin?: string | null;
  }>;
}

const storedUser = localStorage.getItem("usuario");
const usuarioId = storedUser ? JSON.parse(storedUser).usuarioId : null;
const currentUsername = storedUser ? JSON.parse(storedUser).username : null;

// Estados reactivos
const chats = ref<ChatData[]>([]);
const loading = ref(true);
const isConnected = ref(false);
const router = useRouter();

// Estados del modal
const showCreateChatModal = ref(false);
const chatType = ref('privado');
const selectedUserId = ref<number | null>(null);
const groupName = ref('');
const participantIds = ref('');
const creatingChat = ref(false);

// Estados del toast
const showToast = ref(false);
const toastMessage = ref('');
const toastColor = ref('success');

// Computed
const canCreateChat = computed(() => {
  if (chatType.value === 'privado') {
    return selectedUserId.value && selectedUserId.value > 0;
  } else {
    return groupName.value.trim() && participantIds.value.trim();
  }
});

// Función para actualizar el estado de conexión
function updateConnectionStatus() {
  isConnected.value = checkConnection();
}

// Listener para cambios de estado de WebSocket
const stateChangeListener = () => {
  updateConnectionStatus();
};

function getOtherUserName(chat: ChatData): string {
  // Para chats privados (2 participantes), siempre mostrar el otro usuario
  if (chat.participantes && chat.participantes.length === 2) {
    const otherUser = chat.participantes.find(p => p.username !== currentUsername);
    if (otherUser && otherUser.username) {
      return otherUser.username;
    }
  }
  
  // Para chats grupales (más de 2 participantes), usar nombreChat si existe
  if (chat.nombreChat && chat.nombreChat.trim()) {
    return chat.nombreChat;
  }
  
  // Fallback
  return `Chat #${chat.chatId}`;
}

function formatTime(dateString: string): string {
  const date = new Date(dateString);
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffHours = diffMs / (1000 * 60 * 60);
  
  if (diffHours < 24) {
    return date.toLocaleTimeString('es-ES', { 
      hour: '2-digit', 
      minute: '2-digit' 
    });
  } else {
    return date.toLocaleDateString('es-ES', { 
      day: '2-digit', 
      month: '2-digit' 
    });
  }
}

async function cargarChats() {
  try {
    loading.value = true;
    console.log(`Cargando chats para usuario ${usuarioId}`);
    
    // Intentar cargar desde WebSocket si está conectado
    if (isConnected.value) {
      try {
        const chatsData = await obtenerChatsUsuario(usuarioId);
        if (Array.isArray(chatsData)) {
          chats.value = chatsData;
          console.log(`Cargados ${chatsData.length} chats desde WebSocket`);
          return;
        }
      } catch (wsError) {
        console.warn("Error cargando desde WebSocket, usando API REST:", wsError);
      }
    }
    
    // Fallback a API REST
    const chatsData = await obtenerChatsDeUsuario(usuarioId);
    if (Array.isArray(chatsData)) {
      chats.value = chatsData;
      console.log(`Cargados ${chatsData.length} chats desde API REST`);
    } else {
      console.warn("Los datos de chats no son un array:", chatsData);
      chats.value = [];
    }
  } catch (error) {
    console.error("Error cargando chats:", error);
    chats.value = [];
    mostrarToast("Error cargando chats", "danger");
  } finally {
    loading.value = false;
  }
}

async function conectarWebSocket() {
  try {
    console.log("Iniciando conexión WebSocket...");
    
    await connectGlobal(
      // Callback para chat creado
      (chatInfo: any) => {
        console.log("Nuevo chat creado:", chatInfo);
        cargarChats(); // Recargar lista
        mostrarToast("Nuevo chat creado", "success");
      },
      // Callback para lista actualizada
      (chatsData: any[]) => {
        console.log("Lista de chats actualizada:", chatsData);
        if (Array.isArray(chatsData)) {
          chats.value = chatsData;
        }
      }
    );
    
    updateConnectionStatus();
    console.log("WebSocket conectado exitosamente");
    
    // Cargar chats iniciales
    await cargarChats();
    
  } catch (error) {
    console.error("Error conectando WebSocket:", error);
    updateConnectionStatus();
    mostrarToast("Error de conexión", "warning");
    
    // Cargar chats usando API REST como fallback
    await cargarChats();
  }
}

function abrirChat(chat: ChatData) {
  router.push({ 
    name: "Chat", 
    params: { 
      chatId: chat.chatId.toString() 
    }
  });
}

// Funciones del modal
function mostrarModalCrearChat() {
  showCreateChatModal.value = true;
}

function cerrarModalCrearChat() {
  showCreateChatModal.value = false;
  resetFormulario();
}

function resetFormulario() {
  chatType.value = 'privado';
  selectedUserId.value = null;
  groupName.value = '';
  participantIds.value = '';
  creatingChat.value = false;
}

function onChatTypeChange() {
  resetFormulario();
  chatType.value = chatType.value; // Mantener el tipo seleccionado
}

async function crearChat() {
  if (!canCreateChat.value || creatingChat.value) return;
  
  creatingChat.value = true;
  
  try {
    if (chatType.value === 'privado') {
      const chatData = await crearChatPrivado(usuarioId, selectedUserId.value!);
      console.log("Chat privado creado:", chatData);
      mostrarToast("Chat privado creado exitosamente", "success");
    } else {
      const participantsList = participantIds.value
        .split(',')
        .map(id => parseInt(id.trim()))
        .filter(id => !isNaN(id));
      
      if (participantsList.length === 0) {
        mostrarToast("IDs de participantes inválidos", "danger");
        return;
      }
      
      const chatData = await crearChatGrupal(groupName.value.trim(), participantsList);
      console.log("Chat grupal creado:", chatData);
      mostrarToast("Chat grupal creado exitosamente", "success");
    }
    
    cerrarModalCrearChat();
    // Los chats se actualizarán automáticamente vía WebSocket
    
  } catch (error) {
    console.error("Error creando chat:", error);
    mostrarToast("Error creando el chat", "danger");
  } finally {
    creatingChat.value = false;
  }
}

function mostrarToast(mensaje: string, color: string = 'success') {
  toastMessage.value = mensaje;
  toastColor.value = color;
  showToast.value = true;
}

onMounted(async () => {
  console.log("Componente ChatList montado");
  
  // Agregar listener para cambios de estado
  addStateChangeListener(stateChangeListener);
  
  // Conectar WebSocket
  await conectarWebSocket();
});

onUnmounted(() => {
  console.log("Componente ChatList desmontado");
  
  // Remover listener
  removeStateChangeListener(stateChangeListener);
  
  // No desconectar WebSocket aquí ya que puede ser usado por otros componentes
  // disconnect();
});
</script>

<style scoped>
/* FORZAR el fondo en todos los niveles */
.chat-page {
  --ion-background-color: #1a1a2e !important;
  background: #1a1a2e !important;
}

.chat-header {
  --ion-background-color: #16213e !important;
  background: #16213e !important;
  border-bottom: 1px solid #2a3441;
}

.chat-content {
  --ion-background-color: #1a1a2e !important;
  background: #1a1a2e !important;
}

.chat-container {
  background: #1a1a2e !important;
  min-height: 100vh;
  padding: 16px;
  padding-bottom: 80px; /* Espacio para el FAB */
}

.page-title {
  display: flex;
  align-items: center;
  font-weight: 600;
  color: #4facfe;
  position: relative;
}

.title-icon {
  margin-right: 8px;
  font-size: 1.2rem;
}

.connection-status {
  position: absolute;
  right: -40px;
  top: 50%;
  transform: translateY(-50%);
  transition: all 0.3s ease;
}

.connection-status.connected {
  color: #4caf50;
}

.connection-status.disconnected {
  color: #f44336;
  animation: blink 1s infinite;
}

@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0.3; }
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 50vh;
  color: #8a8a8a;
}

.loading-state ion-spinner {
  margin-bottom: 16px;
}

.chat-list-wrapper {
  background: transparent;
}

.chat-item {
  background: #16213e;
  border: 1px solid #2a3441;
  border-radius: 16px;
  padding: 16px;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.chat-item::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  background: linear-gradient(to bottom, #4facfe, #00f2fe);
  transform: scaleY(0);
  transition: transform 0.3s ease;
  transform-origin: bottom;
}

.chat-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(79, 172, 254, 0.2);
  border-color: rgba(79, 172, 254, 0.4);
}

.chat-item:hover::before {
  transform: scaleY(1);
}

.chat-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: linear-gradient(135deg, #4facfe, #00f2fe);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16px;
  flex-shrink: 0;
}

.chat-avatar ion-icon {
  color: white;
  font-size: 24px;
}

.chat-info {
  flex: 1;
  min-width: 0;
}

.chat-title {
  font-size: 1.1rem;
  font-weight: 600;
  color: #e4e6ea;
  margin: 0 0 4px 0;
  line-height: 1.3;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.last-message {
  font-size: 0.9rem;
  color: #8a8a8a;
  margin: 0 0 2px 0;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.last-message-time {
  font-size: 0.75rem;
  color: #666;
  margin: 0 0 4px 0;
}

.unread-info {
  font-size: 0.8rem;
  color: #4facfe;
  margin: 0;
  font-weight: 500;
}

.chat-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
}

.unread-badge {
  background: #4facfe;
  color: white;
  font-size: 0.75rem;
  font-weight: 600;
  padding: 4px 8px;
  border-radius: 12px;
  min-width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.7; }
}

.arrow-icon {
  color: #8a8a8a;
  font-size: 16px;
  transition: all 0.3s ease;
}

.chat-item:hover .arrow-icon {
  color: #4facfe;
  transform: translateX(4px);
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 60vh;
  text-align: center;
}

.empty-icon {
  font-size: 64px;
  color: #8a8a8a;
  margin-bottom: 16px;
  opacity: 0.6;
}

.empty-title {
  font-size: 1.3rem;
  font-weight: 600;
  color: #e4e6ea;
  margin: 0 0 8px 0;
}

.empty-description {
  font-size: 1rem;
  color: #8a8a8a;
  margin: 0 0 24px 0;
  line-height: 1.5;
}

.create-first-chat-btn {
  --color: #4facfe;
  --border-color: #4facfe;
}

.fab-button {
  --background: linear-gradient(135deg, #4facfe, #00f2fe);
  --box-shadow: 0 4px 16px rgba(79, 172, 254, 0.4);
  transition: all 0.3s ease;
}

.fab-button:hover {
  --box-shadow: 0 6px 20px rgba(79, 172, 254, 0.6);
  transform: translateY(-2px);
}

/* Modal styles */
.modal-content {
  --ion-background-color: #1a1a2e;
}

.modal-container {
  padding: 20px;
}

.chat-type-selector {
  margin-bottom: 24px;
}

.chat-type-selector ion-segment {
  --background: #16213e;
}

.chat-type-selector ion-segment-button {
  --color: #8a8a8a;
  --color-checked: #4facfe;
  --indicator-color: #4facfe;
}

.chat-form {
  margin-bottom: 24px;
}

.chat-form ion-item {
  --background: #16213e;
  --color: #e4e6ea;
  margin-bottom: 16px;
  border-radius: 8px;
}

.chat-form ion-label {
  --color: #4facfe !important;
}

.chat-form ion-input {
  --color: #e4e6ea;
  --placeholder-color: #8a8a8a;
}

.modal-actions {
  margin-top: 32px;
}

.modal-actions ion-button {
  --background: linear-gradient(135deg, #4facfe, #00f2fe);
  height: 48px;
}

.modal-actions ion-button[disabled] {
  --background: #2a3441;
  --color: #666;
}

/* Animaciones de entrada */
@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.chat-item {
  animation: fadeInUp 0.4s ease forwards;
}

.chat-item:nth-child(1) { animation-delay: 0.1s; opacity: 0; }
.chat-item:nth-child(2) { animation-delay: 0.2s; opacity: 0; }
.chat-item:nth-child(3) { animation-delay: 0.3s; opacity: 0; }
.chat-item:nth-child(4) { animation-delay: 0.4s; opacity: 0; }
.chat-item:nth-child(5) { animation-delay: 0.5s; opacity: 0; }

/* Responsive */
@media (max-width: 768px) {
  .chat-container {
    padding: 12px;
    padding-bottom: 80px;
  }
  
  .chat-item {
    padding: 12px;
  }
  
  .chat-avatar {
    width: 40px;
    height: 40px;
  }
  
  .chat-avatar ion-icon {
    font-size: 20px;
  }
  
  .chat-title {
    font-size: 1rem;
  }
  
  .last-message {
    font-size: 0.85rem;
  }
}
</style>