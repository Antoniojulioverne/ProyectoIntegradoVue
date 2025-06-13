// chatWebSocket.ts - Versión corregida con soporte para onGlobalMessage
import { Client } from '@stomp/stompjs';

// Estado del WebSocket
let stompClient: Client | null = null;
let currentChatId: number | null = null;
let isConnectedVar: boolean = false;
let connectionPromise: Promise<void> | null = null;

// Callbacks para diferentes tipos de mensajes
let onMessageCallback: ((message: any) => void) | null = null;
let onReadCallback: ((readInfo: any) => void) | null = null;
let onChatCreatedCallback: ((chatInfo: any) => void) | null = null;
let onChatListUpdatedCallback: ((chats: any[]) => void) | null = null;
let onGlobalMessageCallback: ((messageData: any) => void) | null = null; // 🆕 NUEVO CALLBACK

// Listeners para cambios de estado (para reactividad de Vue)
let stateChangeListeners: (() => void)[] = [];

// Función para registrar listeners de cambio de estado
export function addStateChangeListener(listener: () => void) {
  stateChangeListeners.push(listener);
}

// Función para eliminar listeners
export function removeStateChangeListener(listener: () => void) {
  const index = stateChangeListeners.indexOf(listener);
  if (index > -1) {
    stateChangeListeners.splice(index, 1);
  }
}

// Función para notificar cambios de estado
function notifyStateChange() {
  stateChangeListeners.forEach(listener => {
    try {
      listener();
    } catch (error) {
      console.warn('Error en state change listener:', error);
    }
  });
}

// Función para actualizar el estado de conexión
function updateConnectionState(newState: boolean) {
  const oldState = isConnectedVar;
  isConnectedVar = newState;
  
  console.log(`🔄 Estado de conexión: ${oldState} -> ${newState}`);
  
  // Notificar a los listeners si cambió el estado
  if (oldState !== newState) {
    setTimeout(() => notifyStateChange(), 0);
  }
}

// Función para obtener el estado de conexión
export function isConnected(): boolean {
  const clientConnected = stompClient?.connected === true;
  const finalState = isConnectedVar && clientConnected;
  
  if (process.env.NODE_ENV === 'development') {
    console.log(`🔍 isConnected check: internal=${isConnectedVar}, client=${clientConnected}, final=${finalState}`);
  }
  
  return finalState;
}

// Función para obtener el token
function getAuthToken(): string | null {
  return localStorage.getItem("token");
}

// Construir URL con token como query parameter
function buildWebSocketURL(): string {
  const baseURL = 'wss://gameconnect-latest.onrender.com/ws';
  const token = getAuthToken();
  
  if (!token) {
    throw new Error("No hay token de autenticación");
  }
  
  const url = `${baseURL}?token=${encodeURIComponent(token)}`;
  console.log('🔗 URL WebSocket construida (token oculto por seguridad)');
  return url;
}

// ===== CONEXIÓN PRINCIPAL =====

// 🔧 CORREGIDO: Conectar globalmente con soporte para onGlobalMessage
export async function connectGlobal(
  onChatCreated?: (chatInfo: any) => void,
  onChatListUpdated?: (chats: any[]) => void,
  onGlobalMessage?: (messageData: any) => void // 🆕 NUEVO PARÁMETRO
): Promise<void> {
  console.log('🔧 Conectando globalmente...');
  
  if (connectionPromise) {
    console.log("⏳ Conexión en proceso, esperando...");
    await connectionPromise;
    
    if (isConnected()) {
      console.log("✅ Reutilizando conexión global existente");
      onChatCreatedCallback = onChatCreated || null;
      onChatListUpdatedCallback = onChatListUpdated || null;
      onGlobalMessageCallback = onGlobalMessage || null; // 🆕 ASIGNAR CALLBACK
      return;
    }
  }

  connectionPromise = createGlobalConnection(onChatCreated, onChatListUpdated, onGlobalMessage);
  await connectionPromise;
}

// Conectar al chat específico
export async function connectToChat(
  chatId: number,
  onMessage: (message: any) => void,
  onRead: (readInfo: any) => void
): Promise<void> {
  console.log(`🔧 Conectando al chat ${chatId}...`);
  
  // Primero conectar globalmente si no está conectado
  if (!isConnected()) {
    await connectGlobal();
  }
  
  // Actualizar callbacks y chat actual
  currentChatId = chatId;
  onMessageCallback = onMessage;
  onReadCallback = onRead;
  
  // Suscribirse al chat específico
  await subscribeToChat(chatId);
}

// 🔧 CORREGIDO: Función interna con soporte para onGlobalMessage
async function createGlobalConnection(
  onChatCreated?: (chatInfo: any) => void,
  onChatListUpdated?: (chats: any[]) => void,
  onGlobalMessage?: (messageData: any) => void // 🆕 NUEVO PARÁMETRO
): Promise<void> {
  return new Promise((resolve, reject) => {
    try {
      // Desconectar conexión anterior si existe
      if (stompClient && stompClient.connected) {
        console.log("🔧 Desconectando conexión anterior...");
        try {
          stompClient.deactivate();
        } catch (error) {
          console.warn("Advertencia al desconectar:", error);
        }
      }

      // Resetear estado
      updateConnectionState(false);
      currentChatId = null;

      // Construir URL con token
      const webSocketURL = buildWebSocketURL();

      // Crear nuevo cliente STOMP
      stompClient = new Client({
        brokerURL: webSocketURL,
        connectHeaders: {},
        debug: (str) => {
          console.log('🔍 STOMP Debug:', str);
        },
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
        webSocketFactory: () => {
          console.log('🔗 Creando WebSocket con token en URL...');
          const ws = new WebSocket(webSocketURL);
          return ws;
        }
      });

      // Configurar callbacks
      stompClient.onConnect = (frame) => {
        console.log('✅ WebSocket connected successfully:', frame);
        
        updateConnectionState(true);
        onChatCreatedCallback = onChatCreated || null;
        onChatListUpdatedCallback = onChatListUpdated || null;
        onGlobalMessageCallback = onGlobalMessage || null; // 🆕 ASIGNAR CALLBACK

        // Suscribirse a notificaciones globales
        subscribeToGlobalNotifications()
          .then(() => {
            console.log('✅ Conexión global inicializada completamente');
            resolve();
          })
          .catch((error) => {
            console.error('❌ Error en suscripciones globales:', error);
            updateConnectionState(false);
            reject(error);
          });
      };

      stompClient.onStompError = (frame) => {
        console.error('❌ STOMP Error:', frame);
        updateConnectionState(false);
        reject(new Error(`STOMP Error: ${frame.body}`));
      };

      stompClient.onWebSocketError = (error) => {
        console.error('❌ WebSocket Error:', error);
        updateConnectionState(false);
        reject(error);
      };

      stompClient.onDisconnect = (frame) => {
        console.log('🔌 WebSocket disconnected:', frame);
        updateConnectionState(false);
        currentChatId = null;
      };

      stompClient.onWebSocketClose = (event) => {
        console.log('🔌 WebSocket closed:', event);
        console.log('Code:', event.code, 'Reason:', event.reason);
        updateConnectionState(false);
        
        if (event.code === 1006) {
          console.warn('⚠️ Conexión cerrada de manera anormal - posible problema de red o autenticación');
        }
      };

      // Activar el cliente
      console.log('🔧 Activando cliente STOMP...');
      stompClient.activate();

    } catch (error) {
      console.error('❌ Error creando conexión:', error);
      updateConnectionState(false);
      reject(error);
    }
  });
}

// ===== SUSCRIPCIONES =====

// 🔧 CORREGIDO: Suscribirse a notificaciones globales con soporte para mensajes globales
async function subscribeToGlobalNotifications(): Promise<void> {
  if (!stompClient?.connected) {
    throw new Error("Cliente STOMP no conectado");
  }

  console.log('🔧 Subscribing to global notifications...');

  try {
    const userStr = localStorage.getItem('usuario');
    const user = userStr ? JSON.parse(userStr) : null;
    const userId = user?.usuarioId;

    if (!userId) {
      throw new Error("No se encontró ID de usuario");
    }

    // Suscripción a chats nuevos (usando la ruta correcta del backend)
    stompClient.subscribe(`/user/queue/chat.nuevo`, (message) => {
      console.log('🆕 Nuevo chat creado:', message.body);
      try {
        const chatData = JSON.parse(message.body);
        onChatCreatedCallback?.(chatData);
      } catch (error) {
        console.error('❌ Error parseando notificación de chat creado:', error);
      }
    });

    // Suscripción a lista de chats (usando la ruta correcta del backend)
    stompClient.subscribe(`/user/queue/chats`, (message) => {
      console.log('📋 Lista de chats recibida:', message.body);
      try {
        const chatsData = JSON.parse(message.body);
        onChatListUpdatedCallback?.(chatsData);
      } catch (error) {
        console.error('❌ Error parseando lista de chats:', error);
      }
    });

    // 🆕 NUEVA SUSCRIPCIÓN: Mensajes globales para actualizar último mensaje
    // Esto podría ser una suscripción general a todos los mensajes del usuario
    stompClient.subscribe(`/user/queue/mensajes.globales`, (message) => {
      console.log('🌐 Mensaje global recibido:', message.body);
      try {
        const messageData = JSON.parse(message.body);
        onGlobalMessageCallback?.(messageData);
      } catch (error) {
        console.error('❌ Error parseando mensaje global:', error);
      }
    });

    console.log('✅ Suscripciones globales configuradas');
  } catch (error) {
    console.error('❌ Error en suscripciones globales:', error);
    throw error;
  }
}

// Suscribirse a los temas del chat específico
async function subscribeToChat(chatId: number): Promise<void> {
  if (!stompClient?.connected) {
    throw new Error("Cliente STOMP no conectado");
  }

  console.log(`🔧 Subscribing to chat ${chatId} topics...`);

  try {
    // Suscripción a mensajes del chat
    const messageSubscription = stompClient.subscribe(`/topic/chat.${chatId}.mensajes`, (message) => {
      console.log('📨 Mensaje recibido:', message.body);
      try {
        const messageData = JSON.parse(message.body);
        onMessageCallback?.(messageData);
        
        // 🆕 TAMBIÉN NOTIFICAR AL CALLBACK GLOBAL SI EXISTE
        onGlobalMessageCallback?.(messageData);
      } catch (error) {
        console.error('❌ Error parseando mensaje:', error);
      }
    });

    // Suscripción a notificaciones de lectura
    const readSubscription = stompClient.subscribe(`/topic/chat.${chatId}.lectura`, (message) => {
      console.log('👁️ Notificación de lectura:', message.body);
      try {
        const readData = JSON.parse(message.body);
        onReadCallback?.(readData);
      } catch (error) {
        console.error('❌ Error parseando notificación de lectura:', error);
      }
    });

    // Suscripción al historial de mensajes
    stompClient.subscribe(`/user/queue/chat.${chatId}.mensajes.historial`, (message) => {
      console.log('📥 Historial de mensajes recibido:', message.body);
      try {
        const messagesData = JSON.parse(message.body);
        console.log(`Recibidos ${messagesData.length} mensajes del historial`);
      } catch (error) {
        console.error('❌ Error parseando historial de mensajes:', error);
      }
    });

    console.log(`✅ Subscribed to chat ${chatId} successfully`);
  } catch (error) {
    console.error(`❌ Error subscribing to chat ${chatId}:`, error);
    throw error;
  }
}

// ===== FUNCIONES DE ENVÍO =====

// Enviar mensaje
export function sendMessage(messageDTO: any): boolean {
  if (!stompClient?.connected || !isConnected()) {
    console.error('❌ No hay conexión WebSocket para enviar mensaje');
    return false;
  }

  try {
    console.log('📤 Enviando mensaje:', messageDTO);
    stompClient.publish({
      destination: '/app/chat.enviarMensaje',
      body: JSON.stringify(messageDTO)
    });
    console.log('✅ Mensaje enviado correctamente');
    return true;
  } catch (error) {
    console.error('❌ Error enviando mensaje:', error);
    return false;
  }
}

// Marcar mensajes como leídos
export function marcarMensajesLeidos(readDTO: any): boolean {
  if (!stompClient?.connected || !isConnected()) {
    console.error('❌ No hay conexión WebSocket para marcar como leído');
    return false;
  }

  try {
    console.log('👁️ Marcando mensajes como leídos:', readDTO);
    stompClient.publish({
      destination: '/app/chat.marcarLeido',
      body: JSON.stringify(readDTO)
    });
    return true;
  } catch (error) {
    console.error('❌ Error marcando como leído:', error);
    return false;
  }
}

// Crear chat privado con suscripción temporal correcta
export function crearChatPrivado(usuario1Id: number, usuario2Id: number): Promise<any> {
  return new Promise((resolve, reject) => {
    if (!stompClient?.connected || !isConnected()) {
      reject(new Error('No hay conexión WebSocket'));
      return;
    }

    try {
      console.log('🆕 Creando chat privado:', { usuario1Id, usuario2Id });
      
      // Configurar listener temporal para la respuesta (usando la ruta correcta)
      const tempSubscription = stompClient.subscribe('/user/queue/chat.nuevo', (message) => {
        try {
          const chatData = JSON.parse(message.body);
          console.log('✅ Chat privado creado:', chatData);
          tempSubscription.unsubscribe();
          resolve(chatData);
        } catch (error) {
          tempSubscription.unsubscribe();
          reject(error);
        }
      });

      // Enviar solicitud
      stompClient.publish({
        destination: '/app/chat.crearPrivado',
        body: JSON.stringify({ usuario1Id, usuario2Id })
      });

      // Timeout para evitar que quede colgado
      setTimeout(() => {
        tempSubscription.unsubscribe();
        reject(new Error('Timeout creando chat privado'));
      }, 10000);

    } catch (error) {
      console.error('❌ Error creando chat privado:', error);
      reject(error);
    }
  });
}

// Crear chat grupal
export function crearChatGrupal(nombreChat: string, participantesIds: number[]): Promise<any> {
  return new Promise((resolve, reject) => {
    if (!stompClient?.connected || !isConnected()) {
      reject(new Error('No hay conexión WebSocket'));
      return;
    }

    try {
      console.log('🆕 Creando chat grupal:', { nombreChat, participantesIds });
      
      // Configurar listener temporal para la respuesta
      const tempSubscription = stompClient.subscribe('/user/queue/chat.group.created', (message) => {
        try {
          const chatData = JSON.parse(message.body);
          console.log('✅ Chat grupal creado:', chatData);
          tempSubscription.unsubscribe();
          resolve(chatData);
        } catch (error) {
          tempSubscription.unsubscribe();
          reject(error);
        }
      });

      // Enviar solicitud
      stompClient.publish({
        destination: '/app/chat.crearGrupal',
        body: JSON.stringify({ nombreChat, participantesIds })
      });

      // Timeout
      setTimeout(() => {
        tempSubscription.unsubscribe();
        reject(new Error('Timeout creando chat grupal'));
      }, 10000);

    } catch (error) {
      console.error('❌ Error creando chat grupal:', error);
      reject(error);
    }
  });
}

// Obtener chats del usuario
export function obtenerChatsUsuario(usuarioId: number): Promise<any[]> {
  return new Promise((resolve, reject) => {
    if (!stompClient?.connected || !isConnected()) {
      reject(new Error('No hay conexión WebSocket'));
      return;
    }

    try {
      console.log('📋 Obteniendo chats del usuario:', usuarioId);
      
      // Suscripción temporal usando la ruta correcta del backend
      const tempSubscription = stompClient.subscribe('/user/queue/chats', (message) => {
        try {
          const chatsData = JSON.parse(message.body);
          console.log('✅ Chats obtenidos:', chatsData);
          tempSubscription.unsubscribe();
          resolve(chatsData);
        } catch (error) {
          tempSubscription.unsubscribe();
          reject(error);
        }
      });

      // Enviar solicitud
      stompClient.publish({
        destination: '/app/chat.obtenerChats',
        body: JSON.stringify({ usuarioId })
      });

      // Timeout
      setTimeout(() => {
        tempSubscription.unsubscribe();
        reject(new Error('Timeout obteniendo chats'));
      }, 10000);

    } catch (error) {
      console.error('❌ Error obteniendo chats:', error);
      reject(error);
    }
  });
}

// Obtener mensajes de un chat
export function obtenerMensajesChat(chatId: number): Promise<any[]> {
  return new Promise((resolve, reject) => {
    if (!stompClient?.connected || !isConnected()) {
      reject(new Error('No hay conexión WebSocket'));
      return;
    }

    try {
      console.log('📥 Obteniendo mensajes del chat:', chatId);
      
      // Suscripción temporal usando la ruta correcta del backend
      const tempSubscription = stompClient.subscribe(`/user/queue/chat.${chatId}.mensajes.historial`, (message) => {
        try {
          const messagesData = JSON.parse(message.body);
          console.log('✅ Mensajes obtenidos:', messagesData);
          tempSubscription.unsubscribe();
          resolve(messagesData);
        } catch (error) {
          tempSubscription.unsubscribe();
          reject(error);
        }
      });

      // Enviar solicitud
      stompClient.publish({
        destination: '/app/chat.obtenerMensajes',
        body: JSON.stringify({ chatId })
      });

      // Timeout
      setTimeout(() => {
        tempSubscription.unsubscribe();
        reject(new Error('Timeout obteniendo mensajes'));
      }, 10000);

    } catch (error) {
      console.error('❌ Error obteniendo mensajes:', error);
      reject(error);
    }
  });
}

// ===== FUNCIONES DE CONTROL =====

// 🔧 CORREGIDO: Reconectar con soporte para onGlobalMessage
export async function reconnect(): Promise<boolean> {
  try {
    console.log('🔄 Intentando reconectar...');
    
    if (onChatCreatedCallback || onChatListUpdatedCallback || onGlobalMessageCallback) {
      await connectGlobal(
        onChatCreatedCallback || undefined, 
        onChatListUpdatedCallback || undefined,
        onGlobalMessageCallback || undefined // 🆕 INCLUIR EN RECONEXIÓN
      );
      
      // Si había un chat específico conectado, reconectar a él
      if (currentChatId && onMessageCallback && onReadCallback) {
        await subscribeToChat(currentChatId);
      }
      
      return isConnected();
    } else {
      console.error('❌ No hay información suficiente para reconectar');
      return false;
    }
  } catch (error) {
    console.error('❌ Error en reconexión:', error);
    return false;
  }
}

// 🔧 CORREGIDO: Desconectar incluyendo onGlobalMessage
export function disconnect(): void {
  console.log('🔌 Desconectando WebSocket...');
  
  updateConnectionState(false);
  currentChatId = null;
  onMessageCallback = null;
  onReadCallback = null;
  onChatCreatedCallback = null;
  onChatListUpdatedCallback = null;
  onGlobalMessageCallback = null; // 🆕 RESETEAR CALLBACK GLOBAL
  connectionPromise = null;
  
  if (stompClient) {
    try {
      if (stompClient.connected) {
        console.log('🔧 Deactivando cliente STOMP...');
        stompClient.deactivate();
      }
    } catch (error) {
      console.warn('Advertencia al desconectar STOMP:', error);
    } finally {
      stompClient = null;
    }
  }
  
  console.log('✅ WebSocket desconectado');
}

// Validar token antes de conectar
export function validateTokenBeforeConnect(): boolean {
  const token = getAuthToken();
  if (!token) {
    console.error('❌ No se encontró token de autenticación');
    return false;
  }
  
  if (token.length < 10) {
    console.error('❌ Token parece inválido (muy corto)');
    return false;
  }
  
  console.log('✅ Token de autenticación encontrado');
  return true;
}

// 🔧 CORREGIDO: Export adicional para debugging incluyendo onGlobalMessage
export function getConnectionDebugInfo() {
  return {
    isConnectedVar: isConnectedVar,
    stompConnected: stompClient?.connected ?? false,
    currentChatId: currentChatId,
    hasCallbacks: !!(onMessageCallback && onReadCallback),
    hasGlobalCallbacks: !!(onChatCreatedCallback || onChatListUpdatedCallback || onGlobalMessageCallback),
    stompClient: !!stompClient,
    stompActive: stompClient?.active ?? false,
    hasToken: !!getAuthToken(),
    tokenLength: getAuthToken()?.length ?? 0
  };
}