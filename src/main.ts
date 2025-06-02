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

/**
 * Ionic Dark Mode
 * -----------------------------------------------------
 * For more info, please see:
 * https://ionicframework.com/docs/theming/dark-mode
 */

/* @import '@ionic/vue/css/palettes/dark.always.css'; */
/* @import '@ionic/vue/css/palettes/dark.class.css'; */
import '@ionic/vue/css/palettes/dark.system.css';

/* Theme variables */
import './theme/variables.css';
// En main.ts o main.js
(window as any).global = window;
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
  people
} from 'ionicons/icons';

import { addIcons } from 'ionicons';

addIcons({
  send,
  add,
  checkmark,
  close,
  people,
  'arrow-back': arrowBack,
  person,
  chatbox,
  'chatbubbles-outline': chatbubblesOutline,
  'log-out': logOut,
  'chatbubbles': chatbubbles,
  moon,
  'moon-outline':moonOutline,
  'sunny-outline':sunnyOutline,
  sunny,
  'grid-outline':gridOutline,
  'people-outline':peopleOutline,
  'settings-outline':settingsOutline,
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
});



// Importar el composable de autenticación
import { useAuth } from '@/composables/useAuth'

const app = createApp(App)
  .use(IonicVue)
  .use(router)

// Inicializar autenticación al arrancar la app
const { cargarDatosAuth } = useAuth()
cargarDatosAuth()

router.isReady().then(() => {
  app.mount('#app');
});
