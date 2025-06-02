<template>
  <div class="skin-selection-container">
    <h2 class="section-title">Selecciona tu Skin</h2>

    <ion-radio-group v-model="selectedSkin" class="skin-radio-group">
      <div class="skin-options">
        <SkinOption
          v-for="skin in skins" 
          :key="skin.value"
          :skin="skin"
          :selected="selectedSkin === skin.value"
          @select="selectedSkin = skin.value"
        />
      </div>
    </ion-radio-group>

    <ion-button 
      expand="block" 
      class="apply-skin-button" 
      @click="$emit('apply')"
    >
      Aplicar Cambios
    </ion-button>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { IonRadioGroup, IonButton } from '@ionic/vue';
import SkinOption from '../stats/SkinOption.vue';

const props = defineProps({
  modelValue: String
});

const emit = defineEmits(['update:modelValue', 'apply']);

const selectedSkin = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
});

const skins = [
  {
    value: 'blue',
    name: 'Azul',
    image: '/src/assets/Sonic.gif',
    alt: 'Skin Azul'
  },
  {
    value: 'red',
    name: 'Rojo',
    image: '/src/assets/Mario.gif',
    alt: 'Skin Rojo'
  }
];
</script>