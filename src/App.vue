<template>
  <ion-app>
    <!-- Renderizar el menú lateral solo si no estamos en páginas de autenticación -->
    <SideMenu v-if="!isAuthPage" />
    
    <!-- Contenido principal -->
    <ion-router-outlet id="main-content"></ion-router-outlet>
  </ion-app>
</template>

<script setup lang="ts">
import { IonApp, IonRouterOutlet } from '@ionic/vue';
import { computed, onMounted, onUnmounted } from 'vue';
import { useRoute } from 'vue-router';
import SideMenu from '@/views/SideMenu.vue';
import { StatusBar, Style } from '@capacitor/status-bar';
const route = useRoute();

// Lista de rutas donde NO queremos mostrar el menú lateral
const authRoutes = [
  '/home',
  '/registro', 
  '/verificar-email',
  '/solicitar-recuperacion',
  '/recuperar-contrasena'
];

// Computed property para verificar si estamos en una página de auth
const isAuthPage = computed(() => {
  return authRoutes.includes(route.path);
});


const updateStatusBar = () => {
  const isDarkMode = window.matchMedia('(prefers-color-scheme: dark)').matches;
  
  if (isDarkMode) {
    // Modo oscuro
    StatusBar.setStyle({ style: Style.Dark });
    StatusBar.setBackgroundColor({ color: '#000000' }); // o el color que uses para modo oscuro
  } else {
    // Modo claro
    StatusBar.setStyle({ style: Style.Light });
    StatusBar.setBackgroundColor({ color: '#ffffff' });
  }
};

onMounted(() => {
  // Configurar StatusBar inicial
  StatusBar.setOverlaysWebView({ overlay: false });
  updateStatusBar();
  
  // Escuchar cambios en el tema del sistema
  const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
  mediaQuery.addEventListener('change', updateStatusBar);
  
  // Guardar referencia para cleanup
  (window as any).statusBarMediaQuery = mediaQuery;
});

onUnmounted(() => {
  // Limpiar el listener
  const mediaQuery = (window as any).statusBarMediaQuery;
  if (mediaQuery) {
    mediaQuery.removeEventListener('change', updateStatusBar);
  }
});
</script>

<style>
/* Estos estilos globales se aplicarán a toda la aplicación */
:root {
  --ion-safe-area-top: env(safe-area-inset-top);
  --ion-safe-area-bottom: env(safe-area-inset-bottom);
}

/* Asegúrate de que los headers de todas las páginas respeten el área segura */
ion-header {
  padding-top: var(--ion-safe-area-top);
}

/* Para dispositivos con cámaras en el centro o notch */
@media (max-width: 768px) {
  ion-header ion-toolbar {
    padding-top: var(--ion-safe-area-top);
  }
}
</style>