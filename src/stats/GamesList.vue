<template>
  <div class="games-list">
    <ion-card 
      v-for="(game, index) in games" 
      :key="index" 
      class="game-card"
    >
      <ion-card-content>
        <div class="game-info">
          <div class="game-date">{{ formatDate(game.fecha) }}</div>
          <div class="game-score">{{ game.puntos }} pts</div>
          <div class="game-coins">{{ game.monedas }} monedas</div>
        </div>
      </ion-card-content>
    </ion-card>
  </div>
</template>

<script setup>
import { IonCard, IonCardContent } from '@ionic/vue';
import { formatDate } from '@/utils/dateUtils';

defineProps({
  games: Array
});
</script>