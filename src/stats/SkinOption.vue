<template>
  <div class="skin-option">
    <div 
      class="skin-rectangle"
      :class="[`skin-${skin.value}`, { 'selected': selected }]"
      @click="$emit('select')"
    >
      <img :src="skin.image" :alt="skin.alt" class="skin-image">
    </div>
    <ion-item lines="none" class="radio-item">
      <ion-radio :value="skin.value" class="skin-radio">
        {{ skin.name }}
      </ion-radio>
    </ion-item>
  </div>
</template>

<script setup>
import { IonItem, IonRadio } from '@ionic/vue';

defineProps({
  skin: Object,
  selected: Boolean
});

defineEmits(['select']);
</script>