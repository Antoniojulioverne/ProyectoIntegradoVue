// WebSocket Service Mejorado con Conexión Continua
import { Client, Frame } from '@stomp/stompjs';
import { ref, reactive,readonly } from 'vue';
import config from '@/config/config';

// Tipos
export interface ChatMessage {
  mensajeId: number;
  chatId: number;
  usuarioId: number;
  username: string;
  contenido: string;
  fechaEnvio: string;
  tipo: string;
  esLeido: boolean;
}

export interface ChatNotification {
  chatId: number;
  remitenteId: number;
  remitenteNombre: string;
  contenidoPreview: string;
  tipoMensaje: string;
  fechaEnvio: string;
}

export interface ConnectionStatus {
  isConnected: boolean;
  isConnecting: boolean;
  error: string | null;
  reconnectAttempt: number;
}

// Estado reactivo global
const connectionStatus = reactive<ConnectionStatus>({
  isConnected: false,
  isConnecting: false,
  error: null,
  reconnectAttempt: 0
});

// Callbacks globales
interface GlobalCallbacks {
  onMessage?: (message: ChatMessage) => void;
  onChatNotification?: (notification: ChatNotification) => void;
  onChatListUpdate?: (chats: any[]) => void;
  onReadNotification?: (readInfo: any) => void;
  onConnectionChange?: (status: ConnectionStatus) => void;
}

class WebSocketService {
  private client: Client | null = null;
  private currentUserId: number | null = null;
  private currentUserEmail: string | null = null;
  private reconnectTimer: NodeJS.Timeout | null = null;
  private heartbeatTimer: NodeJS.Timeout | null = null;
  private globalCallbacks: GlobalCallbacks = {};
  private chatSpecificCallbacks: Map<number, any> = new Map();

  // Listeners para reactividad
  private statusListeners: Set<() => void> = new Set();

  constructor() {
    this.setupEventListeners();
  }

  // === CONFIGURACIÓN INICIAL ===
  
  private setupEventListeners() {
    // Escuchar cambios de visibilidad para reconectar
    document.addEventListener('visibilitychange', () => {
      if (!document.hidden && !this.isConnected()) {
        this.reconnect();
      }
    });

    // Escuchar cambios de red
    window.addEventListener('online', () => {
      if (!this.isConnected()) {
        this.reconnect();
      }
    });
  }

  // === GESTIÓN DE CONEXIÓN ===

  public async connect(userId: number, callbacks: GlobalCallbacks = {}): Promise<boolean> {
    try {
      console.log('🔌 Iniciando conexión WebSocket para usuario:', userId);
      
      if (this.client?.connected) {
        console.log('✅ Ya conectado, actualizando callbacks');
        this.globalCallbacks = { ...this.globalCallbacks, ...callbacks };
        return true;
      }

      this.updateStatus({ isConnecting: true, error: null });
      this.currentUserId = userId;
      this.globalCallbacks = { ...this.globalCallbacks, ...callbacks };
      
      // Obtener token y email del usuario
      const token = this.getAuthToken();
      const userEmail = this.getUserEmail();
      
      if (!token || !userEmail) {
        throw new Error('Token o email de usuario no encontrado');
      }

      this.currentUserEmail = userEmail;

      // Crear cliente STOMP
      this.client = new Client({
        brokerURL: `${config.api.wsUrl}?token=${encodeURIComponent(token)}`,
        connectHeaders: {},
        debug: (str) => console.log('🔍 STOMP:', str),
        reconnectDelay: config.websocket.reconnectDelay,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
        webSocketFactory: () => {
          const ws = new WebSocket(`${config.api.wsUrl}?token=${encodeURIComponent(token)}`);
          return ws;
        }
      });

      // Configurar eventos
      this.setupClientHandlers();

      // Activar cliente
      this.client.activate();

      // Esperar conexión con timeout
      return await this.waitForConnection();

    } catch (error) {
      console.error('❌ Error conectando WebSocket:', error);
      this.updateStatus({ 
        isConnecting: false, 
        isConnected: false, 
        error: error instanceof Error ? error.message : 'Error desconocido' 
      });
      return false;
    }
  }

  private setupClientHandlers() {
    if (!this.client) return;

    this.client.onConnect = (frame: Frame) => {
      console.log('✅ WebSocket conectado:', frame);
      this.updateStatus({ 
        isConnected: true, 
        isConnecting: false, 
        error: null,
        reconnectAttempt: 0 
      });
      
      this.setupGlobalSubscriptions();
      this.startHeartbeat();
    };

    this.client.onStompError = (frame: Frame) => {
      console.error('❌ Error STOMP:', frame);
      this.updateStatus({ 
        isConnected: false, 
        isConnecting: false, 
        error: `Error STOMP: ${frame.body}` 
      });
      this.scheduleReconnect();
    };

    this.client.onWebSocketError = (error: Event) => {
      console.error('❌ Error WebSocket:', error);
      this.updateStatus({ 
        isConnected: false, 
        isConnecting: false, 
        error: 'Error de conexión WebSocket' 
      });
      this.scheduleReconnect();
    };

    this.client.onDisconnect = (frame: Frame) => {
      console.log('🔌 WebSocket desconectado:', frame);
      this.updateStatus({ 
        isConnected: false, 
        isConnecting: false, 
        error: null 
      });
      this.stopHeartbeat();
      this.scheduleReconnect();
    };

    this.client.onWebSocketClose = (event: CloseEvent) => {
      console.log('🔌 WebSocket cerrado:', event);
      this.updateStatus({ 
        isConnected: false, 
        isConnecting: false, 
        error: event.code === 1006 ? 'Conexión cerrada inesperadamente' : null 
      });
      this.scheduleReconnect();
    };
  }

  private async waitForConnection(): Promise<boolean> {
    return new Promise((resolve) => {
      const timeout = setTimeout(() => {
        resolve(false);
      }, config.websocket.connectionTimeout);

      const checkConnection = () => {
        if (this.client?.connected) {
          clearTimeout(timeout);
          resolve(true);
        } else if (connectionStatus.error) {
          clearTimeout(timeout);
          resolve(false);
        } else {
          setTimeout(checkConnection, 100);
        }
      };

      checkConnection();
    });
  }

  // === SUSCRIPCIONES GLOBALES ===

  private setupGlobalSubscriptions() {
    if (!this.client?.connected || !this.currentUserId || !this.currentUserEmail) return;

    console.log('🔧 Configurando suscripciones globales...');

    // Suscripción a notificaciones globales de chats
    this.client.subscribe(`/user/queue/chats`, (message) => {
      try {
        const chatsData = JSON.parse(message.body);
        console.log('📋 Lista de chats actualizada:', chatsData.length);
        this.globalCallbacks.onChatListUpdate?.(chatsData);
      } catch (error) {
        console.error('❌ Error parseando lista de chats:', error);
      }
    });

    // Suscripción a notificaciones de nuevos chats
    this.client.subscribe(`/user/queue/chat.nuevo`, (message) => {
      try {
        const chatData = JSON.parse(message.body);
        console.log('🆕 Nuevo chat creado:', chatData);
        // Actualizar lista de chats
        this.requestChatListUpdate();
      } catch (error) {
        console.error('❌ Error parseando nuevo chat:', error);
      }
    });

    // Suscripción a notificaciones globales de mensajes
    this.client.subscribe(`/user/queue/notificaciones`, (message) => {
      try {
        const notification: ChatNotification = JSON.parse(message.body);
        console.log('🔔 Notificación de mensaje:', notification);
        this.globalCallbacks.onChatNotification?.(notification);
        // También actualizar la lista de chats para reflejar el último mensaje
        this.requestChatListUpdate();
      } catch (error) {
        console.error('❌ Error parseando notificación:', error);
      }
    });

    // Suscripción a todos los chats del usuario para mensajes en tiempo real
    this.subscribeToAllUserChats();

    console.log('✅ Suscripciones globales configuradas');
  }

  private async subscribeToAllUserChats() {
    if (!this.currentUserId) return;

    try {
      // Obtener lista de chats del usuario
      const chats = await this.requestUserChats();
      
      // Suscribirse a cada chat
      chats.forEach(chat => {
        this.subscribeToChat(chat.chatId);
      });

    } catch (error) {
      console.error('❌ Error suscribiendo a chats del usuario:', error);
    }
  }

  private subscribeToChat(chatId: number) {
    if (!this.client?.connected) return;

    // Suscripción a mensajes del chat
    this.client.subscribe(`/topic/chat.${chatId}.mensajes`, (message) => {
      try {
        const messageData: ChatMessage = JSON.parse(message.body);
        console.log(`📨 Mensaje en chat ${chatId}:`, messageData);
        
        // Llamar callback global de mensajes
        this.globalCallbacks.onMessage?.(messageData);
        
        // Llamar callback específico del chat si existe
        const chatCallback = this.chatSpecificCallbacks.get(chatId);
        chatCallback?.onMessage?.(messageData);

        // Actualizar lista de chats para reflejar último mensaje
        this.requestChatListUpdate();
      } catch (error) {
        console.error('❌ Error parseando mensaje:', error);
      }
    });

    // Suscripción a notificaciones de lectura
    this.client.subscribe(`/topic/chat.${chatId}.lectura`, (message) => {
      try {
        const readData = JSON.parse(message.body);
        console.log(`👁️ Notificación de lectura en chat ${chatId}:`, readData);
        
        this.globalCallbacks.onReadNotification?.(readData);
        
        const chatCallback = this.chatSpecificCallbacks.get(chatId);
        chatCallback?.onRead?.(readData);
      } catch (error) {
        console.error('❌ Error parseando notificación de lectura:', error);
      }
    });
  }

  // === GESTIÓN DE RECONEXIÓN ===

  private scheduleReconnect() {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
    }

    if (connectionStatus.reconnectAttempt >= config.websocket.maxReconnectAttempts) {
      console.log('❌ Máximo número de intentos de reconexión alcanzado');
      this.updateStatus({ error: 'No se pudo establecer conexión después de varios intentos' });
      return;
    }

    const delay = config.websocket.reconnectDelay * (connectionStatus.reconnectAttempt + 1);
    console.log(`🔄 Programando reconexión en ${delay}ms (intento ${connectionStatus.reconnectAttempt + 1})`);

    this.reconnectTimer = setTimeout(() => {
      this.reconnect();
    }, delay);
  }

  public async reconnect(): Promise<boolean> {
    if (connectionStatus.isConnecting) {
      console.log('⏳ Reconexión ya en progreso...');
      return false;
    }

    console.log('🔄 Intentando reconexión...');
    connectionStatus.reconnectAttempt++;
    
    this.disconnect();
    
    if (this.currentUserId) {
      return await this.connect(this.currentUserId, this.globalCallbacks);
    }
    
    return false;
  }

  // === GESTIÓN DE HEARTBEAT ===

  private startHeartbeat() {
    this.stopHeartbeat();
    
    this.heartbeatTimer = setInterval(() => {
      if (this.client?.connected) {
        // Enviar ping simple
        try {
          this.client.publish({
            destination: '/app/ping',
            body: JSON.stringify({ timestamp: Date.now() })
          });
        } catch (error) {
          console.warn('⚠️ Error enviando heartbeat:', error);
        }
      }
    }, config.websocket.heartbeatInterval);
  }

  private stopHeartbeat() {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer);
      this.heartbeatTimer = null;
    }
  }

  // === MÉTODOS PÚBLICOS ===

  public isConnected(): boolean {
    return connectionStatus.isConnected && this.client?.connected === true;
  }

  public getConnectionStatus(): ConnectionStatus {
    return { ...connectionStatus };
  }

  public async sendMessage(chatId: number, content: string): Promise<boolean> {
    if (!this.isConnected() || !this.currentUserId) {
      console.error('❌ No hay conexión o usuario no autenticado');
      return false;
    }

    try {
      const messageDTO = {
        chatId,
        usuarioId: this.currentUserId,
        contenido: content,
        tipo: 'TEXTO'
      };

      this.client!.publish({
        destination: '/app/chat.enviarMensaje',
        body: JSON.stringify(messageDTO)
      });

      console.log('✅ Mensaje enviado:', messageDTO);
      return true;
    } catch (error) {
      console.error('❌ Error enviando mensaje:', error);
      return false;
    }
  }

  public async markAsRead(chatId: number): Promise<boolean> {
    if (!this.isConnected() || !this.currentUserId) return false;

    try {
      const readDTO = {
        chatId,
        usuarioId: this.currentUserId
      };

      this.client!.publish({
        destination: '/app/chat.marcarLeido',
        body: JSON.stringify(readDTO)
      });

      return true;
    } catch (error) {
      console.error('❌ Error marcando como leído:', error);
      return false;
    }
  }

  public async requestChatListUpdate(): Promise<void> {
    if (!this.isConnected() || !this.currentUserId) return;

    try {
      this.client!.publish({
        destination: '/app/chat.obtenerChats',
        body: JSON.stringify({ usuarioId: this.currentUserId })
      });
    } catch (error) {
      console.error('❌ Error solicitando actualización de chats:', error);
    }
  }

  private async requestUserChats(): Promise<any[]> {
    return new Promise((resolve, reject) => {
      if (!this.isConnected() || !this.currentUserId) {
        reject(new Error('No conectado'));
        return;
      }

      const timeout = setTimeout(() => {
        reject(new Error('Timeout obteniendo chats'));
      }, 5000);

      const tempSubscription = this.client!.subscribe('/user/queue/chats', (message) => {
        try {
          const chats = JSON.parse(message.body);
          clearTimeout(timeout);
          tempSubscription.unsubscribe();
          resolve(chats);
        } catch (error) {
          clearTimeout(timeout);
          tempSubscription.unsubscribe();
          reject(error);
        }
      });

      this.client!.publish({
        destination: '/app/chat.obtenerChats',
        body: JSON.stringify({ usuarioId: this.currentUserId })
      });
    });
  }

  // === GESTIÓN DE CALLBACKS ESPECÍFICOS DE CHAT ===

  public setChatCallbacks(chatId: number, callbacks: any) {
    this.chatSpecificCallbacks.set(chatId, callbacks);
    
    // Si no estamos ya suscritos a este chat, suscribirse
    if (this.isConnected()) {
      this.subscribeToChat(chatId);
    }
  }

  public removeChatCallbacks(chatId: number) {
    this.chatSpecificCallbacks.delete(chatId);
  }

  // === GESTIÓN DE LISTENERS ===

  public addStatusListener(listener: () => void) {
    this.statusListeners.add(listener);
  }

  public removeStatusListener(listener: () => void) {
    this.statusListeners.delete(listener);
  }

  private updateStatus(updates: Partial<ConnectionStatus>) {
    Object.assign(connectionStatus, updates);
    
    // Notificar callback global
    this.globalCallbacks.onConnectionChange?.(connectionStatus);
    
    // Notificar listeners
    this.statusListeners.forEach(listener => {
      try {
        listener();
      } catch (error) {
        console.warn('⚠️ Error en status listener:', error);
      }
    });
  }

  // === UTILIDADES ===

  private getAuthToken(): string | null {
    return localStorage.getItem(config.storage.token);
  }

  private getUserEmail(): string | null {
    const userStr = localStorage.getItem(config.storage.user);
    if (userStr) {
      try {
        const user = JSON.parse(userStr);
        return user.email;
      } catch {
        return null;
      }
    }
    return null;
  }

  public disconnect() {
    console.log('🔌 Desconectando WebSocket...');
    
    this.stopHeartbeat();
    
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }

    if (this.client) {
      try {
        if (this.client.connected) {
          this.client.deactivate();
        }
      } catch (error) {
        console.warn('⚠️ Error desconectando cliente:', error);
      } finally {
        this.client = null;
      }
    }

    this.chatSpecificCallbacks.clear();
    this.currentUserId = null;
    this.currentUserEmail = null;
    
    this.updateStatus({
      isConnected: false,
      isConnecting: false,
      error: null,
      reconnectAttempt: 0
    });
    
    console.log('✅ WebSocket desconectado');
  }
}

// Singleton instance
export const webSocketService = new WebSocketService();

// Composable para usar en componentes Vue
export function useWebSocket() {
  return {
    connectionStatus: readonly(connectionStatus),
    connect: webSocketService.connect.bind(webSocketService),
    disconnect: webSocketService.disconnect.bind(webSocketService),
    reconnect: webSocketService.reconnect.bind(webSocketService),
    sendMessage: webSocketService.sendMessage.bind(webSocketService),
    markAsRead: webSocketService.markAsRead.bind(webSocketService),
    isConnected: webSocketService.isConnected.bind(webSocketService),
    setChatCallbacks: webSocketService.setChatCallbacks.bind(webSocketService),
    removeChatCallbacks: webSocketService.removeChatCallbacks.bind(webSocketService),
    addStatusListener: webSocketService.addStatusListener.bind(webSocketService),
    removeStatusListener: webSocketService.removeStatusListener.bind(webSocketService)
  };
}

export default webSocketService;