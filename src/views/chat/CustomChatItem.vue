<!-- CustomChatItem.vue -->
<template>
  <div 
    class="custom-chat-item" 
    :class="{ 'has-unread': hasUnreadMessages }"
    @click="$emit('click', chat)"
  >
    <div class="chat-avatar">
      <div class="avatar-circle">
        <ion-icon name="chatbubble-ellipses"></ion-icon>
      </div>
    </div>
    
    <div class="chat-content">
      <div class="chat-header">
        <h3 class="chat-name">{{ chatName }}</h3>
        <div class="chat-indicators">
          <div 
            v-if="hasUnreadMessages" 
            class="unread-badge"
          >
            {{ unreadCount }}
          </div>
          <ion-icon name="chevron-forward" class="arrow-icon"></ion-icon>
        </div>
      </div>
      
      <div class="chat-preview">
        <p v-if="lastMessage" class="last-message">
          {{ lastMessage }}
        </p>
        <p v-if="hasUnreadMessages" class="unread-info">
          {{ unreadCount }} mensajes no leídos
        </p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { IonIcon } from '@ionic/vue';

interface Props {
  chat: {
    chatId: number;
    nombreChat?: string;
    ultimoMensaje?: {
      contenido: string;
      fechaEnvio: string;
    };
    mensajesNoLeidos: number;
    participantes: Array<{
      usuarioId: number;
      username: string;
    }>;
  };
  currentUserId: number;
}

const props = defineProps<Props>();

const emit = defineEmits<{
  click: [chat: Props['chat']];
}>();

const chatName = computed(() => {
  if (props.chat.nombreChat?.trim()) {
    return props.chat.nombreChat;
  }
  
  const otherUser = props.chat.participantes?.find(
    p => p.usuarioId !== props.currentUserId
  );
  
  return otherUser?.username || `Chat #${props.chat.chatId}`;
});

const lastMessage = computed(() => {
  return props.chat.ultimoMensaje?.contenido || '';
});

const hasUnreadMessages = computed(() => {
  return props.chat.mensajesNoLeidos > 0;
});

const unreadCount = computed(() => {
  return props.chat.mensajesNoLeidos;
});
</script>

<style scoped>
.custom-chat-item {
  display: flex;
  align-items: center;
  padding: 16px;
  background: var(--ion-color-light, #ffffff);
  border-radius: 16px;
  margin-bottom: 12px;
  border: 1px solid var(--ion-color-step-100, rgba(0, 0, 0, 0.1));
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}

.custom-chat-item::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  background: linear-gradient(to bottom, 
    var(--ion-color-primary, #3b82f6), 
    var(--ion-color-primary-shade, #2563eb)
  );
  transform: scaleY(0);
  transition: transform 0.3s ease;
  transform-origin: bottom;
}

.custom-chat-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.15);
  border-color: var(--ion-color-primary-tint, rgba(59, 130, 246, 0.3));
}

.custom-chat-item:hover::before {
  transform: scaleY(1);
}

.custom-chat-item:active {
  transform: translateY(-1px);
}

.chat-avatar {
  margin-right: 16px;
  flex-shrink: 0;
}

.avatar-circle {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: linear-gradient(135deg, 
    var(--ion-color-primary, #3b82f6), 
    var(--ion-color-primary-shade, #2563eb)
  );
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.2);
}

.avatar-circle ion-icon {
  color: white;
  font-size: 24px;
}

.chat-content {
  flex: 1;
  min-width: 0; /* Para que funcione text-overflow */
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.chat-name {
  font-size: 1.1rem;
  font-weight: 600;
  color: var(--ion-color-dark, #1e293b);
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
  margin-right: 12px;
}

.chat-indicators {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.unread-badge {
  background: var(--ion-color-primary, #3b82f6);
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
  color: var(--ion-color-medium, #64748b);
  font-size: 16px;
  transition: all 0.3s ease;
}

.custom-chat-item:hover .arrow-icon {
  color: var(--ion-color-primary, #3b82f6);
  transform: translateX(4px);
}

.chat-preview {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.last-message {
  font-size: 0.9rem;
  color: var(--ion-color-medium, #64748b);
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  line-height: 1.4;
}

.unread-info {
  font-size: 0.8rem;
  color: var(--ion-color-primary, #3b82f6);
  font-weight: 500;
  margin: 0;
}

.has-unread {
  border-color: var(--ion-color-primary-tint, rgba(59, 130, 246, 0.2));
}

.has-unread .chat-name {
  font-weight: 700;
}

/* Tema oscuro */
@media (prefers-color-scheme: dark) {
  .custom-chat-item {
    background: var(--ion-color-step-50, #1e293b);
    border-color: var(--ion-color-step-200, rgba(51, 65, 85, 0.8));
  }
  
  .chat-name {
    color: var(--ion-color-light, #f1f5f9);
  }
  
  .last-message {
    color: var(--ion-color-medium, #94a3b8);
  }
}

/* Responsive */
@media (max-width: 768px) {
  .custom-chat-item {
    padding: 12px;
  }
  
  .avatar-circle {
    width: 40px;
    height: 40px;
  }
  
  .avatar-circle ion-icon {
    font-size: 20px;
  }
  
  .chat-name {
    font-size: 1rem;
  }
  
  .last-message {
    font-size: 0.85rem;
  }
}
</style>