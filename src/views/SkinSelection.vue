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
  </div> 
</template>  

<script setup> 
import { computed, onMounted, ref } from 'vue'; 
import { IonRadioGroup } from '@ionic/vue'; 
import SkinOption from '../stats/SkinOption.vue';
import { useAuth } from '@/composables/useAuth'; // Ajusta la ruta según tu estructura
import axios from 'axios';

const props = defineProps({   
  modelValue: String 
});  

const emit = defineEmits(['update:modelValue', 'apply']);  

// Usar el composable de autenticación
const { usuario, updateUserSkin, getHeaders } = useAuth();

// Estado interno para manejar la skin seleccionada
const internalSelectedSkin = ref('');

const selectedSkin = computed({   
  get: () => internalSelectedSkin.value || props.modelValue,   
  set: (value) => {
    internalSelectedSkin.value = value;
    emit('update:modelValue', value);
  }
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

// Función para obtener la skin del usuario desde la API
const fetchUserSkin = async () => {
  try {
    // Verificar que hay un usuario autenticado
    if (!usuario.value || !usuario.value.usuarioId) {
      console.warn('No hay usuario autenticado');
      return;
    }

    // Hacer la petición HTTP GET usando axios con headers de autenticación
    const response = await axios.get(
      `https://gameconnect-latest.onrender.com/GameConnect/usuario/${usuario.value.usuarioId}`, 
      {
        headers: getHeaders(),
        timeout: 10000
      }
    );
    
    const userDataResponse = response.data;
    
    // Verificar si tiene skin y es válida
    if (userDataResponse.skin && (userDataResponse.skin === 'red' || userDataResponse.skin === 'blue')) {
      // Actualizar el estado del componente
      internalSelectedSkin.value = userDataResponse.skin;
      emit('update:modelValue', userDataResponse.skin);
      
      // Solo actualizar si la skin es diferente
      if (usuario.value.skin !== userDataResponse.skin) {
        // Usar la función específica para actualizar la skin
        updateUserSkin(userDataResponse.skin);
        
        console.log(`Skin actualizada: ${usuario.value.skin} -> ${userDataResponse.skin}`);
      }
    }

  } catch (error) {
    console.error('Error al obtener la skin del usuario:', error);
    
    // Manejo de errores específicos
    if (error.response) {
      console.error(`Error HTTP ${error.response.status}:`, error.response.data);
    } else if (error.request) {
      console.error('No se recibió respuesta del servidor');
    } else {
      console.error('Error de configuración:', error.message);
    }
  }
};

// Cargar la skin desde la API al montar el componente
onMounted(() => {
  fetchUserSkin();
});
</script>