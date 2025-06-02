<template>
  <ion-card class="no-games-card">
    <ion-card-content>
      <p class="no-games-message">{{ title }}</p>
      <p v-if="subtitle" class="no-games-subtitle">{{ subtitle }}</p>
    </ion-card-content>
  </ion-card>
</template>

<script setup>
import { IonCard, IonCardContent } from '@ionic/vue';

defineProps({
  title: String,
  subtitle: String
});
</script>