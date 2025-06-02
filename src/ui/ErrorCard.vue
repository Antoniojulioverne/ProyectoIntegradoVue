<template>
  <ion-card class="error-card">
    <ion-card-content>
      <p class="error-message">{{ message }}</p>
      <ion-button @click="$emit('retry')" color="primary" size="small">
        Reintentar
      </ion-button>
    </ion-card-content>
  </ion-card>
</template>

<script setup>
import { IonCard, IonCardContent, IonButton } from '@ionic/vue';

defineProps({
  message: String
});

defineEmits(['retry']);
</script>