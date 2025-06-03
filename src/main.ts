import { createApp } from 'vue'
import App from './App.vue'
import router from './router';

import { IonicVue } from '@ionic/vue';

/* Core CSS required for Ionic components to work properly */
import '@ionic/vue/css/core.css';

/* Basic CSS for apps built with Ionic */
import '@ionic/vue/css/normalize.css';
import '@ionic/vue/css/structure.css';
import '@ionic/vue/css/typography.css';

/* Optional CSS utils that can be commented out */
import '@ionic/vue/css/padding.css';
import '@ionic/vue/css/float-elements.css';
import '@ionic/vue/css/text-alignment.css';
import '@ionic/vue/css/text-transformation.css';
import '@ionic/vue/css/flex-utils.css';
import '@ionic/vue/css/display.css';

/* Ionic Dark Mode */
import '@ionic/vue/css/palettes/dark.system.css';

/* Theme variables */
import './theme/variables.css';

/* Design System - DEBE IR DESPUÉS DE LOS ESTILOS DE IONIC */
import './styles/design-system.css';

// Global window polyfill
(window as any).global = window;

// Iconos de Ionic
import {
  send,
  checkmark,
  close,
  arrowBack,
  person,
  chatbox,
  chatbubblesOutline,
  chatbubbles,
  logOut,
  moon,
  sunny,
  menu,
  home,
  heart,
  star,
  thumbsUp,
  eye,
  pencil,
  trash,
  settings,
  videocamOutline,
  callOutline,
  wifiOutline,
  checkmarkDone,
  checkmarkDoneOutline,
  chatbubbleEllipses,
  chevronForward,
  moonOutline,
  sunnyOutline,
  gridOutline,
  peopleOutline,
  settingsOutline,
  wifi,
  add,
  people,
  personAdd,
  personCircle,
  searchOutline,
  mailOpen,
  paperPlane,
  hourglass,
  closeCircle,
  alertCircle,
  refresh,
  sync,
  cloudOfflineOutline,
  addCircleOutline,
  ellipsisVertical,
  exit,

} from 'ionicons/icons';

import { addIcons } from 'ionicons';

// Registrar todos los iconos
addIcons({
  exit,
  'ellipsis-vertical':ellipsisVertical,
  send,
  add,
  checkmark,
  close,
  people,
  'add-circle-outline':addCircleOutline,
  'arrow-back': arrowBack,
  person,
  'person-add': personAdd,
  'person-circle': personCircle,
  chatbox,
  'chatbubbles-outline': chatbubblesOutline,
  'log-out': logOut,
  'chatbubbles': chatbubbles,
  moon,
  'moon-outline': moonOutline,
  'sunny-outline': sunnyOutline,
  sunny,
  'grid-outline': gridOutline,
  'people-outline': peopleOutline,
  'settings-outline': settingsOutline,
  menu,
  home,
  heart,
  star,
  'thumbs-up': thumbsUp,
  eye,
  pencil,
  trash,
  wifi,
  settings,
  'videocam-outline': videocamOutline,
  'call-outline': callOutline,
  'wifi-outline': wifiOutline,
  'checkmark-done': checkmarkDone,
  'checkmark-done-outline': checkmarkDoneOutline,
  'chatbubble-ellipses': chatbubbleEllipses,
  'chevron-forward': chevronForward,
  'search-outline': searchOutline,
  'mail-open': mailOpen,
  'paper-plane': paperPlane,
  hourglass,
  'close-circle': closeCircle,
  'alert-circle': alertCircle,
  refresh,
  sync,
  'cloud-offline-outline': cloudOfflineOutline
});

// Importar el composable de autenticación
import { useAuth } from '@/composables/useAuth'

// Configurar interceptores de API globalmente
import { useApi } from '@/composables/useApi'
const { setupAxiosInterceptors } = useApi()
setupAxiosInterceptors()

const app = createApp(App)
  .use(IonicVue)
  .use(router)

// Inicializar autenticación al arrancar la app
const { cargarDatosAuth } = useAuth()
cargarDatosAuth()

// Configurar manejo global de errores
app.config.errorHandler = (err, vm, info) => {
  console.error('❌ Error global de Vue:', err, info)
  
  // En desarrollo, mostrar errores más detallados
  if (import.meta.env.DEV) {
    console.error('Componente:', vm)
    console.error('Info:', info)
  }
}

// Configurar warnings en desarrollo
if (import.meta.env.DEV) {
  app.config.warnHandler = (msg, vm, trace) => {
    console.warn('⚠️ Warning de Vue:', msg)
    if (trace) {
      console.warn('Trace:', trace)
    }
  }
}

router.isReady().then(() => {
  app.mount('#app');
});