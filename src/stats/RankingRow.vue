<template>
  <tr :class="{ 'current-user': isCurrentUser, 'top-three': position <= 3 }">
    <td class="position-cell">
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
    
    <td class="player-cell">
      <div class="player-info">
        <!-- Reemplazar avatar-circle con ProfileAvatar -->
        <div class="player-avatar">
         <ProfileAvatar
  :profile-image="player.fotoPerfil"
  :username="player.username"
  :size="40"
  :is-verified="player.emailVerificado"
  :show-verification="false"
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

defineProps({
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
  gap: 6px;
  flex-wrap: wrap;
}

.trophy-icon {
  font-size: 20px;
}

.trophy-icon.gold {
  color: #FFD700;
  filter: drop-shadow(0 2px 4px rgba(255, 215, 0, 0.3));
}

.trophy-icon.silver {
  color: #C0C0C0;
  filter: drop-shadow(0 2px 4px rgba(192, 192, 192, 0.3));
}

.trophy-icon.bronze {
  color: #CD7F32;
  filter: drop-shadow(0 2px 4px rgba(205, 127, 50, 0.3));
}

.position-number {
  font-weight: 700;
  font-size: 18px;
  color: var(--ion-color-dark);
  min-width: 20px;
}

.current-badge {
  font-size: 10px;
  font-weight: 600;
  margin-left: 4px;
}

.player-cell {
  padding: 16px;
  min-width: 200px;
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
  gap: 2px;
  flex: 1;
  min-width: 0;
}

.username-wrapper {
  display: flex;
  align-items: center;
  gap: 6px;
}

.username {
  font-weight: 600;
  font-size: 16px;
  color: var(--ion-color-dark);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.verified-icon {
  color: var(--ion-color-success);
  font-size: 16px;
  flex-shrink: 0;
}

.player-meta {
  font-size: 12px;
  color: var(--ion-color-medium);
}

.score-cell {
  padding: 16px;
  text-align: right;
  min-width: 120px;
}

.score-wrapper {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
}

.score {
  font-weight: 700;
  font-size: 18px;
  color: var(--ion-color-primary);
}

.score-icon {
  font-size: 18px;
  color: var(--ion-color-danger);
}

.date-cell {
  padding: 16px 12px;
  text-align: center;
  min-width: 120px;
}

.date {
  font-size: 14px;
  color: var(--ion-color-medium);
  white-space: nowrap;
}

/* Responsive adjustments */
@media (max-width: 768px) {
  .position-number {
    font-size: 16px;
  }
  
  .username {
    font-size: 14px;
  }
  
  .score {
    font-size: 16px;
  }
  
  td {
    padding: 12px 8px;
  }
  
  .verified-icon {
    font-size: 14px;
  }
}

/* Dark theme support */
.dark-theme .current-user {
  background: linear-gradient(135deg, 
    var(--ion-color-primary-shade) 0%, 
    var(--ion-color-dark-tint) 100%);
}

.dark-theme .top-three {
  background: linear-gradient(135deg, 
    var(--ion-color-warning-shade) 0%, 
    var(--ion-color-dark-tint) 100%);
}

.dark-theme .username {
  color: var(--ion-color-light);
}

.dark-theme .position-number {
  color: var(--ion-color-light);
}
</style>