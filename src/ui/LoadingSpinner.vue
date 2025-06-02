<template>
  <div class="loading-container">
    <ion-spinner name="crescent"></ion-spinner>
    <p>{{ message }}</p>
  </div>
</template>

<script setup lang="ts">
import { IonSpinner } from '@ionic/vue';

interface Props {
  message?: string;
}

const props = withDefaults(defineProps<Props>(), {
  message: 'Cargando...'
});
</script>
