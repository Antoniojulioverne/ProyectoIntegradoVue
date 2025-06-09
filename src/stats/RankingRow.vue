<template>
  <tr :class="{ 'current-user': isCurrentUser, 'top-three': position <= 3 }">
    <td class="position-cell" data-label="Posición">
      <div class="position-wrapper">
        <ion-icon 
          v-if="position === 1" 
          :icon="trophy" 
          class="trophy-icon gold"
        />
        <ion-icon 
          v-else-if="position === 2" 
          :icon="trophy" 
          class="trophy-icon silver"
        />
        <ion-icon 
          v-else-if="position === 3" 
          :icon="trophy" 
          class="trophy-icon bronze"
        />
        <span class="position-number">{{ position }}</span>
        <ion-badge 
          v-if="isCurrentUser" 
          color="primary" 
          class="current-badge"
        >
          Tú
        </ion-badge>
      </div>
    </td>
    
    <td class="player-cell" data-label="Jugador">
      <div class="player-info">
        <!-- Usar ProfileAvatar para mostrar foto de perfil o iniciales -->
        <div class="player-avatar">
          <ProfileAvatar
            :profile-image="player.fotoPerfil"
            :username="player.username"
            :size="40"
            :is-verified="player.emailVerificado"
            :show-verification="false"
            :border-color="isCurrentUser ? 'var(--ion-color-primary)' : null"
          />
        </div>
        <div class="player-details">
          <div class="username-wrapper">
            <span class="username">{{ player.username || 'Usuario Desconocido' }}</span>
            <ion-icon 
              v-if="player.emailVerificado && !isCurrentUser" 
              :icon="checkmarkCircle"
              class="verified-icon"
            />
          </div>
          <span class="player-meta" v-if="player.gamesPlayed">
            {{ player.gamesPlayed }} partidas jugadas
          </span>
          <span class="player-meta" v-else-if="player.totalPoints">
            {{ formatScore(player.totalPoints) }} puntos totales
          </span>
        </div>
      </div>
    </td>
    
    <td class="score-cell" data-label="Puntos">
      <div class="score-wrapper">
        <span class="score">{{ formatScore(player.puntosMaximos) }}</span>
        <ion-icon :icon="flame" class="score-icon" />
      </div>
    </td>
    
    <td class="date-cell" data-label="Fecha">
      <span class="date">{{ formatDate(player.fechaPartida) }}</span>
    </td>
  </tr>
</template>

<script setup>
import { IonIcon, IonBadge } from '@ionic/vue';
import { trophy, flame, checkmarkCircle } from 'ionicons/icons';
import ProfileAvatar from '../ui/ProfileAvatar.vue';
import { onMounted } from 'vue';

const props = defineProps({
  player: {
    type: Object,
    required: true,
    default: () => ({
      username: 'Usuario Desconocido',
      puntosMaximos: 0,
      fechaPartida: null,
      fotoPerfil: null,
      emailVerificado: false,
      gamesPlayed: null,
      totalPoints: null
    })
  },
  position: {
    type: Number,
    required: true
  },
  isCurrentUser: {
    type: Boolean,
    default: false
  }
});

// Debug info
onMounted(() => {
  if (props.isCurrentUser) {
    console.log('RankingRow - Usuario actual:', {
      username: props.player.username,
      puntosMaximos: props.player.puntosMaximos,
      tieneImagen: !!props.player.fotoPerfil,
      imgLength: props.player.fotoPerfil?.length
    });
  }
});

const formatScore = (score) => {
  if (score === null || score === undefined) return '0';
  return score.toLocaleString('es-ES');
};

const formatDate = (date) => {
  if (!date || date === 'No disponible') return 'No disponible';
  
  try {
    // Handle both string and Date objects
    const dateObj = typeof date === 'string' ? new Date(date) : date;
    
    if (isNaN(dateObj.getTime())) {
      return 'Fecha inválida';
    }
    
    return dateObj.toLocaleDateString('es-ES', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric'
    });
  } catch (error) {
    console.error('Error formatting date:', error);
    return 'Fecha inválida';
  }
};
</script>

<style scoped>
tr {
  transition: all 0.3s ease;
}

tr:hover {
  background-color: var(--ion-color-light-tint);
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.current-user {
  background: linear-gradient(135deg, 
    var(--ion-color-primary-tint) 0%, 
    var(--ion-color-light) 100%);
  font-weight: 600;
  border-left: 4px solid var(--ion-color-primary);
}

.current-user:hover {
  background: linear-gradient(135deg, 
    var(--ion-color-primary-shade) 0%, 
    var(--ion-color-primary-tint) 100%);
}

.top-three {
  background: linear-gradient(135deg, 
    var(--ion-color-warning-tint) 0%, 
    var(--ion-color-light) 100%);
  border-left: 4px solid var(--ion-color-warning);
}

.position-cell {
  width: 100px;
  text-align: center;
  padding: 16px 12px;
}

.position-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.trophy-icon {
  font-size: 22px;
}

.gold {
  color: #FFD700;
}

.silver {
  color: #C0C0C0;
}

.bronze {
  color: #CD7F32;
}

.position-number {
  font-size: 18px;
  font-weight: 600;
  color: var(--ion-color-dark);
}

.current-badge {
  font-size: 10px;
  padding: 3px 8px;
  font-weight: 500;
  border-radius: 10px;
}

.player-cell {
  padding: 12px 16px;
  width: 50%;
}

.player-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.player-avatar {
  flex-shrink: 0;
}

.player-details {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.username-wrapper {
  display: flex;
  align-items: center;
  gap: 6px;
}

.username {
  font-size: 16px;
  font-weight: 600;
  color: var(--ion-color-dark);
}

.verified-icon {
  color: var(--ion-color-success);
  font-size: 16px;
}

.player-meta {
  font-size: 12px;
  color: var(--ion-color-medium);
}

.score-cell {
  width: 140px;
  padding: 12px 16px;
  text-align: center;
}

.score-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.score {
  font-size: 18px;
  font-weight: 600;
  color: var(--ion-color-dark);
}

.score-icon {
  color: var(--ion-color-danger);
  font-size: 18px;
}

.date-cell {
  width: 130px;
  padding: 12px 16px;
  text-align: center;
}

.date {
  font-size: 14px;
  color: var(--ion-color-medium);
}

/* Adaptación para modo oscuro */
:global(.dark-theme) .position-number,
:global(.dark-theme) .username,
:global(.dark-theme) .score {
  color: var(--ion-color-light);
}

:global(.dark-theme) tr:hover {
  background-color: var(--ion-color-dark-tint);
}

:global(.dark-theme) .current-user {
  background: linear-gradient(135deg, 
    var(--ion-color-primary-shade) 0%, 
    var(--ion-color-dark) 100%);
}

:global(.dark-theme) .top-three {
  background: linear-gradient(135deg, 
    var(--ion-color-warning-shade) 0%, 
    var(--ion-color-dark) 100%);
}

/* Media queries para responsividad */
@media (max-width: 768px) {
  .position-cell {
    width: auto;
    padding: 12px 8px;
  }
  
  .trophy-icon {
    font-size: 18px;
  }
  
  .position-number {
    font-size: 16px;
  }
  
  .current-badge {
    font-size: 9px;
    padding: 2px 6px;
  }
  
  .player-info {
    gap: 8px;
  }
  
  .username {
    font-size: 14px;
  }
  
  .player-meta {
    font-size: 11px;
  }
  
  .score {
    font-size: 16px;
  }
  
  .date {
    font-size: 13px;
  }
}
</style>