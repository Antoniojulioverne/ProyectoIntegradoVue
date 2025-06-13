<template>
  <ion-page :class="{ 'dark-mode': isDarkMode }">
    <ion-header class="chat-header" :class="{ 'dark-mode': isDarkMode }">
      <ion-toolbar>
        <ion-buttons slot="start">
          <ion-back-button default-href="/chatlist" class="back-btn"></ion-back-button>
        </ion-buttons>
        
        <div class="header-content">
          <div class="chat-info">
            <h3 class="chat-title">{{ otherUserName || `Chat #${chatId}` }}</h3>
            <p class="chat-status" :class="connectionStatus.color">
              {{ connectionStatus.text }}
            </p>
          </div>
        </div>
      </ion-toolbar>
    </ion-header>

    <ion-content 
      ref="contentRef" 
      class="chat-content" 
      :class="{ 'dark-mode': isDarkMode }"
    >
      <!-- Mensajes -->
      <div class="messages-container" :class="{ 'dark-mode': isDarkMode }" v-if="mensajes.length > 0">
        <div
          v-for="(mensaje, index) in mensajes"
          :key="mensaje.mensajeId"
          :class="[
            'message-wrapper',
            mensaje.usuarioId === usuarioId ? 'own-message' : 'other-message',
            { 'dark-mode': isDarkMode },
            {
              'grouped': isGroupedMessage(mensaje, index),
              'last-in-group': isLastInGroup(mensaje, index)
            }
          ]"
        >
          <!-- Avatar para mensajes de otros (solo si no está agrupado) -->
          <div 
            v-if="mensaje.usuarioId !== usuarioId && !isGroupedMessage(mensaje, index)" 
            class="message-avatar"
          >
            <div class="avatar-circle">
              {{ mensaje.username.charAt(0).toUpperCase() }}
            </div>
          </div>
          
          <!-- Spacer para mensajes agrupados de otros -->
          <div 
            v-else-if="mensaje.usuarioId !== usuarioId" 
            class="message-avatar-spacer"
          ></div>

          <!-- Contenido del mensaje -->
          <div class="message-content">
            <div 
              class="message-bubble" 
              :class="{ 
                'dark-mode': isDarkMode, 
                'grouped': isGroupedMessage(mensaje, index) 
              }"
            >
              <p class="message-text">{{ mensaje.contenido }}</p>
              <!-- Tiempo dentro del globo para cada mensaje -->
              <div class="message-time-inside">
                <span class="time">{{ formatTime(mensaje.fechaEnvio) }}</span>
                <div v-if="mensaje.usuarioId === usuarioId" class="read-status-container">
                  <ion-icon
                    :name="mensaje.esLeido ? 'checkmark-done' : 'checkmark'"
                    :class="['read-status', { 'read': mensaje.esLeido }]"
                  ></ion-icon>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
      
      <!-- Estado vacío -->
      <div v-else class="empty-state" :class="{ 'dark-mode': isDarkMode }">
        <div class="empty-icon">
          <ion-icon name="chatbubbles-outline"></ion-icon>
        </div>
        <h3>¡Comienza la conversación!</h3>
        <p>Envía un mensaje para iniciar el chat</p>
      </div>
      
      <!-- Warning de conexión -->
      <div 
        v-if="!connectionStatus.isConnected" 
        class="connection-warning"
        :class="{ 'dark-mode': isDarkMode }"
      >
        <ion-icon name="wifi-outline"></ion-icon>
        <span>{{ connectionStatus.warning }}</span>
      </div>
    </ion-content>

    <!-- Input de mensaje -->
    <ion-footer class="message-input-footer" :class="{ 'dark-mode': isDarkMode }">
      <div class="input-container">
        <div class="input-wrapper">
          <ion-input
            v-model="nuevoMensaje"
            placeholder="Mensaje..."
            @keyup.enter="enviarMensaje"
            class="message-input"
            :class="{ 'dark-mode': isDarkMode }"
          ></ion-input>
          
          <ion-button 
            fill="clear" 
            @click="enviarMensaje"
            :disabled="!canSendMessage || !nuevoMensaje.trim()"
            class="send-button"
            :class="{ 'active': nuevoMensaje.trim() }"
          >
            <ion-icon name="send" class="send-icon"></ion-icon>
          </ion-button>
        </div>
      </div>
    </ion-footer>
  </ion-page>
</template>

<script setup lang="ts">
import {
  IonPage, IonHeader, IonToolbar, IonTitle, IonButtons, IonButton,IonBackButton,
  IonItem, IonContent, IonFooter, IonList,
  IonLabel, IonChip, IonInput, IonIcon
} from '@ionic/vue';
import { ref, onMounted, onBeforeUnmount, nextTick, computed, watch } from "vue";
import { useRoute } from "vue-router";
import { useTheme } from "@/composables/useTheme"; // Asumiendo esta ruta
import { obtenerMensajesDeChat, obtenerChatsDeUsuario } from "@/services/chatApi";
import {
  connectToChat,
  disconnect,
  sendMessage,
  marcarMensajesLeidos,
  isConnected as checkConnection,
  reconnect,
  addStateChangeListener,
  removeStateChangeListener,
  getConnectionDebugInfo
} from "@/services/chatWebSocket";

// Composables
const { isDarkMode } = useTheme();

// Interfaces
interface MensajeData {
  mensajeId: number;
  chatId: number;
  usuarioId: number;
  username: string;
  contenido: string;
  fechaEnvio: string;
  tipo: string;
  esLeido: boolean;
}

interface ChatInfo {
  chatId: number;
  nombreChat?: string;
  participantes: Array<{
    username: string;
    skin?: string | null;
  }>;
}

const route = useRoute();
const chatId = Number(route.params.chatId);
const usuarioStr = localStorage.getItem('usuario')

const usuario = usuarioStr ? JSON.parse(usuarioStr) : null
const usuarioId = usuario?.usuarioId ?? null
const mensajes = ref<MensajeData[]>([]);
const nuevoMensaje = ref("");
const contentRef = ref();
const connectionRetries = ref(0);
const maxRetries = 5;
const isReconnecting = ref(false);
const showDebug = ref(false);
const chatInitialized = ref(false);

// Nueva variable para información del chat
const chatInfo = ref<ChatInfo | null>(null);
const otherUserName = ref<string>('');

// Estado reactivo para la conexión
const connectionState = ref(false);

// Listener para cambios de estado de conexión
const stateChangeListener = () => {
  const newState = checkConnection();
  if (connectionState.value !== newState) {
    connectionState.value = newState;
    console.log(`🔄 Vue state updated: ${newState}`);
  }
};

// Función para obtener el nombre del otro usuario
function getOtherUserName(chat: ChatInfo): string {
  // Obtener el username del usuario actual desde localStorage
  const storedUser = localStorage.getItem("usuario");
  const currentUsername = storedUser ? JSON.parse(storedUser).username : null;
  
  console.log('Debug - Usuario actual:', currentUsername);
  console.log('Debug - Participantes:', chat.participantes);
  
  // Para chats privados (2 participantes), siempre mostrar el otro usuario
  if (chat.participantes && chat.participantes.length === 2) {
    const otherUser = chat.participantes.find(p => p.username !== currentUsername);
    console.log('Debug - Otro usuario encontrado:', otherUser);
    
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

function getOtherUserNameAdvanced(chat: ChatInfo): string {
  const storedUser = localStorage.getItem("usuario");
  const currentUsername = storedUser ? JSON.parse(storedUser).username : null;
  
  const isPrivateChat = chat.participantes && chat.participantes.length === 2;
  
  if (isPrivateChat) {
    // Chat privado: siempre mostrar el otro usuario
    const otherUser = chat.participantes.find(p => p.username !== currentUsername);
    if (otherUser && otherUser.username) {
      return otherUser.username;
    }
  } else {
    // Chat grupal: usar nombreChat o crear nombre descriptivo
    if (chat.nombreChat && chat.nombreChat.trim()) {
      return chat.nombreChat;
    }
    
    // Si no hay nombreChat en grupo, crear uno descriptivo
    const otherParticipants = chat.participantes
      ?.filter(p => p.username !== currentUsername)
      ?.map(p => p.username)
      ?.slice(0, 2); // Máximo 2 nombres
    
    if (otherParticipants && otherParticipants.length > 0) {
      const additionalCount = chat.participantes.length - otherParticipants.length - 1; // -1 para ti
      return additionalCount > 0 
        ? `${otherParticipants.join(', ')} y ${additionalCount} más`
        : otherParticipants.join(', ');
    }
  }
  
  return `Chat #${chat.chatId}`;
}


// Función para cargar información del chat
async function cargarInfoChat() {
  try {
    console.log(`📋 Cargando información del chat ${chatId}`);
    
    // Obtener todos los chats del usuario y filtrar por el chatId actual
    const chatsData = await obtenerChatsDeUsuario(usuarioId);
    const currentChat = chatsData.find((chat: any) => chat.chatId === chatId);
    
    if (currentChat) {
      chatInfo.value = currentChat;
      otherUserName.value = getOtherUserName(currentChat);
      console.log(`✅ Nombre del chat: ${otherUserName.value}`);
    } else {
      console.warn("⚠️ No se encontró información del chat");
      otherUserName.value = `Chat #${chatId}`;
    }
  } catch (error) {
    console.error("❌ Error cargando información del chat:", error);
    otherUserName.value = `Chat #${chatId}`;
  }
}

// Computed para verificar token
const tokenExists = computed(() => {
  return !!localStorage.getItem("token");
});

// Computed para el estado de conexión con detalles
const connectionStatus = computed(() => {
  const isConnected = connectionState.value;
  
  if (isConnected) {
    return {
      isConnected: true,
      color: 'success',
      text: 'Activo ahora',
      warning: ''
    };
  } else if (isReconnecting.value) {
    return {
      isConnected: false,
      color: 'warning',
      text: 'Conectando...',
      warning: `Reconectando... (${connectionRetries.value}/${maxRetries})`
    };
  } else {
    return {
      isConnected: false,
      color: 'danger',
      text: 'Sin conexión',
      warning: 'Sin conexión a internet'
    };
  }
});

// Computed para verificar si se puede enviar mensajes
const canSendMessage = computed(() => {
  return connectionState.value && 
         chatInitialized.value && 
         tokenExists.value && 
         !isReconnecting.value;
});

// Watch para manejar cambios en el estado de conexión
watch(connectionState, (newValue, oldValue) => {
  console.log(`🔄 Connection state watcher: ${oldValue} -> ${newValue}`);
  
  if (!newValue && !isReconnecting.value && chatInitialized.value) {
    console.log('🔄 Iniciando reconexión automática...');
    attemptReconnection();
  }
});

// Carga mensajes iniciales vía REST
async function cargarMensajes() {
  try {
    console.log(`📥 Cargando mensajes para chat ${chatId}`);
    const mensajesData = await obtenerMensajesDeChat(chatId);
    
    if (Array.isArray(mensajesData)) {
      mensajes.value = mensajesData;
      console.log(`✅ Cargados ${mensajesData.length} mensajes`);
    } else {
      console.warn("⚠️ Los datos de mensajes no son un array:", mensajesData);
      mensajes.value = [];
    }
    
    // Scroll al final después de cargar mensajes
    await nextTick();
    scrollToBottom();
    
    // Marcar como leídos después de cargar
    setTimeout(() => marcarComoLeidos(), 1000);
  } catch (error) {
    console.error("❌ Error al cargar mensajes:", error);
    mensajes.value = [];
  }
}

// Enviar mensaje por WebSocket
function enviarMensaje() {
  const mensaje = nuevoMensaje.value.trim();
  if (!mensaje) {
    console.log("⚠️ Mensaje vacío, no se envía");
    return;
  }
  
  if (!canSendMessage.value) {
    console.error("❌ No se puede enviar mensaje - condiciones no cumplidas");
    console.log('Debug:', {
      connected: connectionState.value,
      initialized: chatInitialized.value,
      hasToken: tokenExists.value,
      reconnecting: isReconnecting.value
    });
    return;
  }

  console.log("📤 Enviando mensaje:", mensaje);
  
  const mensajeDTO = {
    chatId,
    usuarioId,
    contenido: mensaje,
    tipo: "TEXTO"
  };

  try {
    const success = sendMessage(mensajeDTO);
    if (success) {
      nuevoMensaje.value = "";
      console.log("✅ Mensaje enviado exitosamente");
    } else {
      console.error("❌ Error enviando mensaje - sendMessage returned false");
    }
  } catch (error) {
    console.error("❌ Error al enviar mensaje:", error);
  }
}

// Marca mensajes como leídos vía WebSocket
function marcarComoLeidos() {
  if (!canSendMessage.value) return;
  
  try {
    const dto = { chatId, usuarioId };
    marcarMensajesLeidos(dto);
    console.log("👁️ Marcando mensajes como leídos");
  } catch (error) {
    console.error("❌ Error marcando mensajes como leídos:", error);
  }
}

// Manejar mensajes recibidos por WebSocket
function onMessageReceived(mensaje: MensajeData) {
  console.log("📨 Nuevo mensaje recibido:", mensaje);
  
  // Evitar duplicados
  const exists = mensajes.value.some(m => m.mensajeId === mensaje.mensajeId);
  if (!exists) {
    mensajes.value.push(mensaje);
    
    // Scroll al final cuando llega un nuevo mensaje
    nextTick(() => {
      scrollToBottom();
    });
    
    // Si el mensaje no es mío, marcar como leído
    if (mensaje.usuarioId !== usuarioId) {
      setTimeout(() => marcarComoLeidos(), 500);
    }
  }
}

// Manejar notificación de mensajes leídos
function onReadReceived(readInfo: any) {
  console.log("👁️ Mensajes leídos:", readInfo);
  
  if (readInfo.usuarioId !== usuarioId) {
    mensajes.value.forEach(mensaje => {
      if (mensaje.usuarioId === usuarioId && mensaje.chatId === readInfo.chatId) {
        mensaje.esLeido = true;
      }
    });
  }
}

// Scroll al final del chat
function scrollToBottom() {
  if (contentRef.value && contentRef.value.$el) {
    contentRef.value.$el.scrollToBottom(300);
  }
}

// Formatear tiempo - mostrar hora exacta
function formatTime(fecha: string | undefined): string {
  if (!fecha) return '';
  return new Date(fecha).toLocaleTimeString('es-ES', {
    hour: '2-digit',
    minute: '2-digit'
  });
}

// Verificar si un mensaje está agrupado con lógica de tiempo
function isGroupedMessage(mensaje: MensajeData, index: number): boolean {
  if (index === 0) return false;
  const previousMessage = mensajes.value[index - 1];
  
  // Solo agrupar si es el mismo usuario
  if (previousMessage.usuarioId !== mensaje.usuarioId) return false;
  
  // Verificar diferencia de tiempo (máximo 5 minutos para agrupar)
  const prevTime = new Date(previousMessage.fechaEnvio).getTime();
  const currTime = new Date(mensaje.fechaEnvio).getTime();
  const timeDiff = (currTime - prevTime) / (1000 * 60); // diferencia en minutos
  
  return timeDiff <= 5; // Agrupar solo si la diferencia es menor a 5 minutos
}

// Verificar si es el último mensaje de un grupo
function isLastInGroup(mensaje: MensajeData, index: number): boolean {
  if (index === mensajes.value.length - 1) return true;
  const nextMessage = mensajes.value[index + 1];
  
  if (nextMessage.usuarioId !== mensaje.usuarioId) return true;
  
  // Verificar diferencia de tiempo con el siguiente mensaje
  const currTime = new Date(mensaje.fechaEnvio).getTime();
  const nextTime = new Date(nextMessage.fechaEnvio).getTime();
  const timeDiff = (nextTime - currTime) / (1000 * 60); // diferencia en minutos
  
  return timeDiff > 5; // Es último del grupo si la diferencia es mayor a 5 minutos
}

// Intentar reconexión
async function attemptReconnection() {
  if (connectionRetries.value >= maxRetries || isReconnecting.value) {
    console.log(`⚠️ Reconexión cancelada - reintentos: ${connectionRetries.value}/${maxRetries}, reconectando: ${isReconnecting.value}`);
    return;
  }
  
  isReconnecting.value = true;
  connectionRetries.value++;
  
  console.log(`🔄 Intentando reconexión ${connectionRetries.value}/${maxRetries}`);
  
  try {
    // Esperar antes de reconectar
    await new Promise(resolve => setTimeout(resolve, 2000 * connectionRetries.value));
    
    const success = await reconnect();
    
    if (success) {
      connectionRetries.value = 0;
      console.log("✅ Reconectado exitosamente");
    } else {
      console.error("❌ Reconexión falló");
    }
  } catch (error) {
    console.error("❌ Error en reconexión:", error);
  } finally {
    isReconnecting.value = false;
  }
}

// Configuración inicial
onMounted(async () => {
  console.log(`=== Iniciando chat ${chatId} para usuario ${usuarioId} ===`);
  
  // Registrar listener para cambios de estado
  addStateChangeListener(stateChangeListener);
  
  // Verificar parámetros
  if (!chatId || !usuarioId) {
    console.error("❌ Error: faltan parámetros chatId o usuarioId");
    return;
  }
  
  // Verificar que tenemos token
  const token = localStorage.getItem("token");
  if (!token) {
    console.error("❌ No hay token de autenticación disponible");
    return;
  }
  
  console.log("✅ Token encontrado, continuando...");
  
  try {
    // Cargar información del chat primero
    console.log("📋 Paso 1: Cargando información del chat...");
    await cargarInfoChat();
    
    // Cargar mensajes
    console.log("📥 Paso 2: Cargando mensajes...");
    await cargarMensajes();
    
    // Conectar WebSocket
    console.log("🔌 Paso 3: Conectando WebSocket...");
    await connectToChat(chatId, onMessageReceived, onReadReceived);
    
    // Actualizar estado inicial
    connectionState.value = checkConnection();
    
    // Marcar como inicializado
    chatInitialized.value = true;
    connectionRetries.value = 0;
    
    console.log("✅ Chat inicializado correctamente");
    console.log(`📊 Estado final - Conectado: ${connectionState.value}, Inicializado: ${chatInitialized.value}`);
    
  } catch (error) {
    console.error("❌ Error inicializando chat:", error);
    chatInitialized.value = false;
    connectionState.value = false;
    attemptReconnection();
  }
});

onBeforeUnmount(() => {
  console.log("🧹 Limpiando conexiones del chat");
  
  // Remover listener
  removeStateChangeListener(stateChangeListener);
  
  // Resetear estado
  chatInitialized.value = false;
  connectionState.value = false;
  
  // Desconectar
  disconnect();
});
</script>

<style scoped>
/* Variables CSS para modo oscuro */
:root {
  --ig-bg-primary: #ffffff;
  --ig-bg-secondary: #fafafa;
  --ig-text-primary: #262626;
  --ig-text-secondary: #8e8e8e;
  --ig-border: #dbdbdb;
  --ig-message-own: #3797f0; /* azul para mensajes propios */
  --ig-message-other: #d1f5d3; /* verde pastel para mensajes del otro usuario */
  --ig-message-text-own: #ffffff;
  --ig-message-text-other: #262626;
  --ig-time-own: rgba(255, 255, 255, 0.7);
  --ig-time-other: rgba(38, 38, 38, 0.6);
}

.dark-mode {
  --ig-bg-primary: #000000;
  --ig-bg-secondary: #121212;
  --ig-text-primary: #ffffff;
  --ig-text-secondary: #a8a8a8;
  --ig-border: #363636;
  --ig-message-own: #3797f0;
  --ig-message-other: #262626;
  --ig-message-text-own: #ffffff;
  --ig-message-text-other: #ffffff;
  --ig-time-own: rgba(255, 255, 255, 0.7);
  --ig-time-other: rgba(255, 255, 255, 0.6);
}

/* Header */
.chat-header {
  border-bottom: 1px solid var(--ig-border);
  background: var(--ig-bg-primary);
}

.chat-header ion-toolbar {
  --background: var(--ig-bg-primary);
  --color: var(--ig-text-primary);
  --border-width: 0;
}

.header-content {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.chat-info {
  text-align: center;
}

.chat-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--ig-text-primary);
  line-height: 1.2;
}

.chat-status {
  margin: 0;
  font-size: 12px;
  color: var(--ig-text-secondary);
  line-height: 1;
}

.chat-status.success {
  color: #00c851;
}

.chat-status.warning {
  color: #ffbb33;
}

.chat-status.danger {
  color: #ff4444;
}

.back-btn {
  --color: var(--ig-text-primary);
}

.header-btn {
  --color: var(--ig-text-primary);
  margin: 0 4px;
}

/* Content */
.chat-content {
  --background: var(--ig-bg-primary);
  --padding-bottom: 80px;
}

.messages-container {
  padding: 12px 16px;
  padding-bottom: 20px;
}

.message-wrapper {
  display: flex;
  align-items: flex-start;
  margin-bottom: 12px;
  gap: 8px;
}

.message-wrapper.grouped {
  margin-bottom: 2px;
}

.message-wrapper.last-in-group {
  margin-bottom: 12px;
}

.own-message {
  flex-direction: row-reverse;
  justify-content: flex-start;
}

.message-avatar {
  flex-shrink: 0;
  align-self: flex-start;
  margin-top: 2px;
}

.message-avatar-spacer {
  width: 28px;
  flex-shrink: 0;
}

.avatar-circle {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: linear-gradient(45deg, #f09433 0%, #e6683c 25%, #dc2743 50%, #cc2366 75%, #bc1888 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  font-weight: 600;
  color: white;
  text-transform: uppercase;
}

.message-content {
   display: inline-block;
  max-width: 100%;
  word-break: break-word;
}

/* MENSAJE BASE - Por defecto mensajes de otros usuarios */
.message-bubble {
  padding: 12px 16px 8px 16px;
  border-radius: 22px;
  position: relative;
  word-wrap: break-word;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
  /* MODO CLARO - Mensajes de otros (verde pastel) */
  background: #d1f5d3 !important;
  color: #262626 !important;
}

.message-bubble.grouped {
  margin-bottom: 2px;
}

/* MENSAJES PROPIOS - Azul siempre */
.message-wrapper.own-message .message-bubble,
.own-message .message-bubble {
  background: #84bef5 !important;
  color: #262626 !important;
}

/* MENSAJES DE OTROS - Verde pastel en modo claro */
.message-wrapper.other-message .message-bubble,
.other-message .message-bubble {
  background: #d1f5d3 !important;
  color: #262626 !important;
}

/* MODO OSCURO - Mensajes de otros (gris oscuro) */
.dark-mode .message-bubble,
.dark-mode .message-wrapper.other-message .message-bubble,
.dark-mode .other-message .message-bubble {
  background: #262626 !important;
  color: #ffffff !important;
}

/* MODO OSCURO - Mensajes propios (mantener azul) */
.dark-mode .message-wrapper.own-message .message-bubble,
.dark-mode .own-message .message-bubble {
  background: #3797f0 !important;
  color: #ffffff !important;
}

.message-text {
  margin: 0 0 6px 0;
  font-size: 14px;
  line-height: 1.4;
  font-weight: 400;
}

/* Tiempo dentro del globo */
.message-time-inside {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 6px;
  margin-top: 4px;
}

.own-message .message-time-inside {
  justify-content: flex-end;
}

.other-message .message-time-inside {
  justify-content: flex-end;
}

.message-time-inside .time {
  font-size: 11px;
  font-weight: 400;
  line-height: 1;
}

.own-message .message-time-inside .time {
  color: var(--ig-time-own);
}

.other-message .message-time-inside .time {
  color: var(--ig-time-other);
}

.read-status-container {
  display: flex;
  align-items: center;
}

.read-status {
  font-size: 12px;
  color: var(--ig-time-own);
  transition: color 0.2s ease;
}

.read-status.read {
  color: rgba(191, 223, 12, 0.9);
}

/* Estado vacío */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 40px;
  text-align: center;
  height: 60vh;
}

.empty-icon {
  margin-bottom: 16px;
}

.empty-icon ion-icon {
  font-size: 64px;
  color: var(--ig-text-secondary);
  opacity: 0.5;
}

.empty-state h3 {
  margin: 0 0 8px 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--ig-text-primary);
}

.empty-state p {
  margin: 0;
  font-size: 14px;
  color: var(--ig-text-secondary);
}

/* Warning de conexión */
.connection-warning {
  position: fixed;
  top: 60px;
  left: 50%;
  transform: translateX(-50%);
  background: var(--ig-message-other);
  color: var(--ig-text-primary);
  padding: 8px 16px;
  border-radius: 20px;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 6px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  z-index: 1000;
}

/* Footer input */
.message-input-footer {
  background: var(--ig-bg-primary);
  border-top: 1px solid var(--ig-border);
  padding: 0;
}

.input-container {
  padding: 12px 16px;
}

.input-wrapper {
  display: flex;
  align-items: center;
  background: var(--ig-bg-secondary);
  border: 1px solid var(--ig-border);
  border-radius: 22px;
  padding: 8px 12px;
  gap: 8px;
}

.message-input {
  --background: transparent;
  --color: var(--ig-text-primary);
  --placeholder-color: var(--ig-text-secondary);
  --padding-start: 0;
  --padding-end: 0;
  flex: 1;
  font-size: 14px;
}

.send-button {
  --color: var(--ig-text-secondary);
  --padding: 6px;
  --border-radius: 50%;
  margin: 0;
  width: 32px;
  height: 32px;
  flex-shrink: 0;
  transition: all 0.2s ease;
}

.send-button.active {
  --color: var(--ig-message-own);
}

.send-button:hover {
  --background: rgba(0, 0, 0, 0.05);
}

.dark-mode .send-button:hover {
  --background: rgba(255, 255, 255, 0.05);
}

.send-icon {
  font-size: 18px;
}

/* Animations */
.message-wrapper {
  animation: messageSlideIn 0.3s ease-out;
}

@keyframes messageSlideIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* Responsive */
@media (max-width: 480px) {
  .message-content {
    max-width: 85%;
  }
  
  .messages-container {
    padding: 8px 12px;
  }
  
  .input-container {
    padding: 8px 12px;
  }
}

/* Dark mode específico para elementos de Ionic */
.dark-mode ion-header {
  --background: var(--ig-bg-primary);
}

.dark-mode ion-content {
  --background: var(--ig-bg-primary);
}

.dark-mode ion-footer {
  --background: var(--ig-bg-primary);
}

.dark-mode .input-wrapper {
  background: var(--ig-bg-secondary);
  border-color: var(--ig-border);
}

.dark-mode .message-input {
  --color: var(--ig-text-primary);
  --placeholder-color: var(--ig-text-secondary);
}

.dark-mode .connection-warning {
  background: var(--ig-message-other);
  color: var(--ig-text-primary);
}
</style>