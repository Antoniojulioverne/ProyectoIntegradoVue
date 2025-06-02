<template>
  <ion-card class="summary-card">
    <ion-card-content>
      <div class="summary-item">
        <div class="summary-value">{{ value }}</div>
        <div class="summary-label">{{ title }}</div>
      </div>
    </ion-card-content>
  </ion-card>
</template>

<script setup>
import { IonCard, IonCardContent } from '@ionic/vue';

defineProps({
  title: String,
  value: [String, Number]
});
</script>