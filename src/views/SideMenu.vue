<template>
  <ion-menu content-id="main-content" menu-id="menu" side="start" class="dark-menu">
    <ion-content class="menu-ion-content" :fullscreen="true">
      <!-- Sección de perfil -->
      <div class="profile-section">

        <h2 class="profile-name">¡¡Hola, {{ userStats.username }}!!</h2>
      </div>
      
      <!-- Lista de navegación usando ion-list pero con estilos forzados -->
      <ion-list class="menu-list">
        <ion-item button @click="navigateTo('/Dashboard')" class="menu-item">
          <ion-icon slot="start" name="grid-outline" class="menu-icon"></ion-icon>
          <ion-label class="menu-label">Estadísticas</ion-label>
        </ion-item>
        
        <ion-item button @click="navigateTo('/chatlist')" class="menu-item">
          <ion-icon slot="start" name="chatbubbles-outline" class="menu-icon"></ion-icon>
          <ion-label class="menu-label">Mis Chats</ion-label>
        </ion-item>
        
        <ion-item button @click="navigateTo('/amigos')" class="menu-item">
          <ion-icon slot="start" name="people-outline" class="menu-icon"></ion-icon>
          <ion-label class="menu-label">Amigos</ion-label>
        </ion-item>
        
        <ion-item button @click="navigateTo('/MiPerfil')" class="menu-item">
          <ion-icon slot="start" name="settings-outline" class="menu-icon"></ion-icon>
          <ion-label class="menu-label">Cuenta</ion-label>
        </ion-item>
        
        <!-- Toggle de tema -->
       <!-- <ion-item @click="toggleDarkMode" class="menu-item theme-toggle" lines="none">
          <ion-icon 
            slot="start"
            :name="isDarkMode ? 'sunny-outline' : 'moon-outline'"
            class="menu-icon theme-icon"
          ></ion-icon>
          <ion-label class="menu-label">
            {{ isDarkMode ? 'Modo Claro' : 'Modo Oscuro' }}
          </ion-label>
        </ion-item>-->
      </ion-list>
    </ion-content>
    
    <!-- Footer con botón de logout -->
    <ion-footer class="menu-footer">
      <ion-toolbar class="footer-toolbar">
        <ion-button 
          expand="block" 
          color="danger" 
          @click="logout" 
          class="logout-button"
        >
          <ion-icon slot="start" :icon="logOutOutline"></ion-icon>
          Salir
        </ion-button>
      </ion-toolbar>
    </ion-footer>
  </ion-menu>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { 
  IonMenu, 
  IonContent, 
  IonList, 
  IonItem, 
  IonLabel,
  IonFooter, 
  IonToolbar, 
  IonButton, 
  IonIcon, 
  toastController, 
  menuController 
} from '@ionic/vue';
import { logOutOutline } from 'ionicons/icons';
import { useRouter } from 'vue-router';
import axios from 'axios';
import { useApi } from '../composables/useApi';
import { useAuth } from '../composables/useAuth';
import { useTheme } from '../composables/useTheme';

const { logout: authLogout, userStats, isAuthenticated, initializeUser, updateUserStats, updateUserGames, updateUserRank } = useAuth();
const { isDarkMode, toggleDarkMode } = useTheme();
const router = useRouter();

const navigateTo = (path: string) => {
  menuController.close('menu');
  router.push(path);
};

const logout = async () => {
  try {
    await authLogout();
    router.push('/');
  } catch (error) {
    console.error('Error al cerrar sesión:', error);
    router.push('/');
  }
};
</script>

<style scoped>
/* Forzar tema oscuro en el menú completo */
.dark-menu {
  --background: #1a1a2e !important;
  --color: #e4e6ea !important;
}

/* Ion-content del menú */
.menu-ion-content {
  --background: #1a1a2e !important;
  --color: #e4e6ea !important;
  background: #1a1a2e !important;
}

/* Sección de perfil */
.profile-section {
  padding: 2rem 1.5rem;
  text-align: center;
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%) !important;
  margin-bottom: 1rem;
  position: relative;
  overflow: hidden;
}

.profile-section::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: url('data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><circle cx="20" cy="20" r="2" fill="rgba(255,255,255,0.15)"/><circle cx="80" cy="40" r="1" fill="rgba(255,255,255,0.1)"/><circle cx="40" cy="80" r="1.5" fill="rgba(255,255,255,0.12)"/></svg>');
  pointer-events: none;
}

.profile-image {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  border: 3px solid rgba(255, 255, 255, 0.4);
  object-fit: cover;
  margin-bottom: 1rem;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.3);
  transition: transform 0.3s ease;
  position: relative;
  z-index: 1;
}

.profile-image:hover {
  transform: scale(1.05);
}

.profile-name {
  color: white !important;
  font-size: 1.2rem;
  font-weight: 600;
  margin: 0;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.4);
  position: relative;
  z-index: 1;
}

/* Lista del menú */
.menu-list {
  --background: #1a1a2e !important;
  background: #1a1a2e !important;
  padding: 0 0.5rem;
}

/* Items del menú */
.menu-item {
  --background: #16213e !important;
  --color: #e4e6ea !important;
  --border-radius: 12px;
  --padding-start: 1rem;
  --padding-end: 1rem;
  --min-height: 50px;
  --border-color: rgba(79, 172, 254, 0.2) !important;
  --border-width: 1px !important;
  --border-style: solid !important;
  
  background: #16213e !important;
  margin-bottom: 0.5rem;
  border-radius: 12px !important;
  border: 1px solid rgba(79, 172, 254, 0.2) !important;
  transition: all 0.3s ease;
  animation: slideInLeft 0.3s ease forwards;
}

.menu-item:hover {
  --background: #1e2a4a !important;
  --border-color: #4facfe !important;
  background: #1e2a4a !important;
  border-color: #4facfe !important;
  transform: translateX(5px);
  box-shadow: 0 4px 12px rgba(79, 172, 254, 0.2);
}

.menu-icon {
  color: #4facfe !important;
  font-size: 1.2rem;
  transition: color 0.3s ease;
}

.menu-item:hover .menu-icon {
  color: #00f2fe !important;
}

.menu-label {
  --color: #e4e6ea !important;
  color: #e4e6ea !important;
  font-weight: 500;
  font-size: 0.95rem;
}

/* Toggle de tema */
.theme-toggle {
  --border-color: rgba(79, 172, 254, 0.3) !important;
  border-top: 1px solid rgba(79, 172, 254, 0.3) !important;
  margin-top: 1rem !important;
  padding-top: 1rem !important;
  cursor: pointer;
}

.theme-toggle::part(native) {
  --detail-icon: none !important;
}

.theme-toggle ion-icon[slot="end"] {
  display: none !important;
}

.theme-icon {
  color: #fbbf24 !important;
}

.theme-toggle:hover .theme-icon {
  color: #f59e0b !important;
}

/* Footer del menú */
.menu-footer {
  --background: #1a1a2e !important;
  background: #1a1a2e !important;
  border-top: 1px solid rgba(79, 172, 254, 0.3);
}

.footer-toolbar {
  --background: #1a1a2e !important;
  --border-color: transparent !important;
  background: #1a1a2e !important;
}

.logout-button {
  --background: linear-gradient(135deg, #ef4444, #dc2626) !important;
  --color: white !important;
  margin: 1rem;
  --border-radius: 12px;
  font-weight: 600;
  transition: all 0.3s ease;
  background: linear-gradient(135deg, #ef4444, #dc2626) !important;
  color: white !important;
}

.logout-button:hover {
  --background: linear-gradient(135deg, #dc2626, #b91c1c) !important;
  background: linear-gradient(135deg, #dc2626, #b91c1c) !important;
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(239, 68, 68, 0.4);
}

/* Animaciones */
@keyframes slideInLeft {
  from {
    opacity: 0;
    transform: translateX(-20px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

.menu-item:nth-child(1) { animation-delay: 0.1s; }
.menu-item:nth-child(2) { animation-delay: 0.2s; }
.menu-item:nth-child(3) { animation-delay: 0.3s; }
.menu-item:nth-child(4) { animation-delay: 0.4s; }
.menu-item:nth-child(5) { animation-delay: 0.5s; }

/* Responsive */
@media (max-width: 768px) {
  .profile-section {
    padding: 1.5rem 1rem;
  }
  
  .profile-image {
    width: 70px;
    height: 70px;
  }
  
  .profile-name {
    font-size: 1.1rem;
  }
  
  .menu-item {
    --min-height: 45px;
    --padding-start: 0.85rem;
    --padding-end: 0.85rem;
  }
  
  .menu-icon {
    font-size: 1.1rem;
  }
  
  .menu-label {
    font-size: 0.9rem;
  }
}

/* Forzar que no haya fondos blancos */
ion-menu,
ion-content,
ion-list,
ion-item,
ion-toolbar,
ion-footer {
  --background: #1a1a2e !important;
  --color: #e4e6ea !important;
}

/* Asegurar que el overlay funcione correctamente */
ion-menu::part(container) {
  background: #1a1a2e !important;
}

ion-menu::part(backdrop) {
  background: rgba(0, 0, 0, 0.5) !important;
}
</style>