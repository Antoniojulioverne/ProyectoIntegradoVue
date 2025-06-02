import { createRouter, createWebHistory } from '@ionic/vue-router';
import { RouteRecordRaw } from 'vue-router';
import HomePage from '../views/auth/HomePage.vue'
import Dashboard from '../views/Dashboard.vue'
import RegistroPage from '../views/auth/RegistroPage.vue'

const routes: Array<RouteRecordRaw> = [
  {
    path: '/',
    redirect: '/home'
  },
  {
    path: '/home',
    name: 'Home',
    component: HomePage
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: Dashboard
  },
  {
    path: '/registro',
    name: 'RegistroPage',
    component: RegistroPage
  },
  {
    path: '/chat/:chatId',
    name: 'Chat',
    component: () => import('@/views/chat/ChatView.vue')
  },
  {
    path: '/chatList',
    name: 'ChatListView',
    component: () => import('@/views/chat/ChatListView.vue')
  },
  {
    path: '/verificar-email',
    name: 'VerificacionEmail',
    component: () => import('@/views/auth/VerificacionEmail.vue')
  },
  {
    path: '/solicitar-recuperacion',
    name: 'SolicitarRecuperacion',
    component: () => import('@/views/auth/SolicitarRecuperacion.vue')
  },
  {
    path: '/recuperar-contrasena',
    name: 'RecuperarContraseña',
    component: () => import('@/views/auth/RecuperarContraseña.vue')
  },
    {
    path: '/amigos',
    name: 'AmigosPage',
    component: () => import('@/views/AmigosPage.vue')
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

export default router