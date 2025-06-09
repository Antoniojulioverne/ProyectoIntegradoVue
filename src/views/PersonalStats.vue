<template>
  <!-- Solo mostrar cuando userStats tenga datos válidos -->
  <div v-if="hasValidData" class="stats-container">
    <!-- Summary Cards -->
    <div class="stats-summary">
      <StatsCard title="Monedas totales" :value="userStats.totalCoins" />
      <StatsCard title="Puntos totales" :value="userStats.totalPoints" />
    </div>

    <!-- Best Score Card -->
    <ion-card class="highlight-card">
      <ion-card-header>
        <ion-card-subtitle>Tu mejor registro</ion-card-subtitle>
        <ion-card-title class="max-score">{{ userStats.maxScore }}</ion-card-title>
      </ion-card-header>
      <ion-card-content>
        <div class="score-details">
          <div class="score-item">
            <ion-icon :icon="trophyOutline" class="score-icon"></ion-icon>
            <div class="score-text">
              <span class="score-label">Puntuación máxima</span>
              <span class="score-date">{{ formatDate(userStats.maxScoreDate) }}</span>
            </div>
          </div>
        </div>
      </ion-card-content>
    </ion-card>

    <h2 class="section-title">Últimas partidas</h2>

    <!-- Games Section -->
    <ErrorCard 
      v-if="gamesError" 
      :message="gamesError" 
      @retry="$emit('retry-games')" 
    />

    <LoadingSpinner 
      v-else-if="isLoadingGames" 
      message="Cargando partidas..." 
    />

    <EmptyState 
      v-else-if="userStats.recentGames.length === 0"
      title="No tienes partidas registradas aún."
      subtitle="¡Juega tu primera partida para ver tus estadísticas!"
    />

    <GamesList 
      v-else 
      :games="userStats.recentGames" 
    />
  </div>

  <!-- Loading state mientras no hay datos válidos -->
  <div v-else class="loading-container">
    <ion-spinner name="crescent"></ion-spinner>
    <p>Cargando estadísticas del usuario...</p>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { IonCard, IonCardHeader, IonCardTitle, IonCardSubtitle, IonCardContent, IonIcon, IonSpinner } from '@ionic/vue';
import { trophyOutline } from 'ionicons/icons';
import { formatDate } from '@/utils/dateUtils';
import StatsCard from '../ui/StatsCard.vue';
import ErrorCard from '../ui/ErrorCard.vue';
import LoadingSpinner from '../ui/LoadingSpinner.vue';
import EmptyState from '../ui/EmptyState.vue';
import GamesList from '../stats/GamesList.vue';

const props = defineProps({
  userStats: Object,
  isLoadingGames: Boolean,
  gamesError: String
});

defineEmits(['retry-games']);

// Computed para verificar si los datos son válidos
const hasValidData = computed(() => {
  const stats = props.userStats;
  
  // Verificar que existe el objeto y tiene username
  if (!stats || !stats.username) {
    console.log('❌ PersonalStats: No hay username');
    return false;
  }
  
  // Verificar que al menos tenga datos básicos cargados
  // (totalPoints y totalCoins pueden ser 0 legítimamente, pero undefined significa que no se han cargado)
  const hasBasicData = stats.totalPoints !== undefined && stats.totalCoins !== undefined;
  
  console.log('🔍 PersonalStats hasValidData:', {
    username: stats.username,
    totalPoints: stats.totalPoints,
    totalCoins: stats.totalCoins,
    hasBasicData
  });
  
  return hasBasicData;
});
</script>