<template>
  <div class="friend-management" :class="{ 'dark-mode': isDarkMode, 'mounted': isMounted }">
    <!-- Tabs para navegar entre secciones -->
    <div class="tabs">
      <button 
        v-for="(tab, index) in tabs" 
        :key="tab.id"
        @click="activeTab = tab.id"
        :class="['tab-button', { active: activeTab === tab.id }]"
        :style="{ '--delay': `${index * 0.1}s` }"
      >
        <ion-icon :icon="tab.icon"></ion-icon>
        {{ tab.label }}
        <span v-if="tab.badge" class="badge">{{ tab.badge }}</span>
      </button>
    </div>

    <!-- Contenido de las tabs -->
    <div class="tab-content">
      
      <!-- Tab de Búsqueda -->
      <div v-if="activeTab === 'search'" class="search-section">
        <div class="search-container">
          <ion-searchbar
            v-model="searchTerm"
            placeholder="Buscar usuarios por nombre o email..."
            @ionInput="buscarUsuarios"
            debounce="500"
          ></ion-searchbar>
        </div>

        <div v-if="loading" class="loading fade-in">
          <ion-spinner></ion-spinner>
          <p>Buscando usuarios...</p>
        </div>

        <div v-if="usuariosEncontrados.length > 0" class="users-list">
          <div 
            v-for="(usuario, index) in usuariosEncontrados" 
            :key="usuario.usuarioId"
            class="user-card"
            :style="{ '--delay': `${index * 0.1}s` }"
          >
            <div class="user-info">
              <div class="user-avatar">
                <img v-if="usuario.skin" :src="usuario.skin" :alt="usuario.username" />
                <ion-icon v-else :icon="personCircle" size="large"></ion-icon>
              </div>
              <div class="user-details">
                <h3>{{ usuario.username }}</h3>
                <p>{{ usuario.email }}</p>
              </div>
            </div>
            
            <div class="user-actions">
              <button 
                v-if="usuario.estadoRelacion === 'SIN_RELACION'"
                @click="enviarPeticion(usuario.usuarioId)"
                class="btn btn-primary"
                :disabled="sendingRequest"
              >
                <ion-icon :icon="personAdd"></ion-icon>
                <span>Enviar solicitud</span>
              </button>
              
              <span v-else-if="usuario.estadoRelacion === 'PETICION_ENVIADA'" class="status-badge sent">
                <ion-icon :icon="checkmarkCircle"></ion-icon>
                <span>Solicitud enviada</span>
              </span>
              
              <span v-else-if="usuario.estadoRelacion === 'PETICION_RECIBIDA'" class="status-badge received">
                <ion-icon :icon="mailOpen"></ion-icon>
                <span>Te envió solicitud</span>
              </span>
              
              <span v-else-if="usuario.estadoRelacion === 'AMISTAD_EXISTENTE'" class="status-badge friends">
                <ion-icon :icon="people"></ion-icon>
                <span>Ya son amigos</span>
              </span>
            </div>
          </div>
        </div>

        <div v-if="searchTerm && !loading && usuariosEncontrados.length === 0" class="no-results fade-in">
          <ion-icon :icon="searchOutline" size="large"></ion-icon>
          <p>No se encontraron usuarios con ese término</p>
        </div>
      </div>

      <!-- Tab de Peticiones Recibidas -->
      <div v-if="activeTab === 'received'" class="received-section">
        <div v-if="peticionesRecibidas.length === 0" class="empty-state fade-in">
          <ion-icon :icon="mailOpen" size="large"></ion-icon>
          <p>No tienes peticiones de amistad pendientes</p>
        </div>

        <div v-else class="requests-list">
          <div 
            v-for="(peticion, index) in peticionesRecibidas" 
            :key="peticion.peticionId"
            class="request-card"
            :style="{ '--delay': `${index * 0.1}s` }"
          >
            <div class="request-info">
              <div class="user-avatar">
                <img v-if="peticion.skinRemitente" :src="peticion.skinRemitente" :alt="peticion.usernameRemitente" />
                <ion-icon v-else :icon="personCircle" size="large"></ion-icon>
              </div>
              <div class="request-details">
                <h3>{{ peticion.usernameRemitente }}</h3>
                <p>{{ peticion.emailRemitente }}</p>
                <small>{{ formatearFecha(peticion.fechaCreacion) }}</small>
              </div>
            </div>
            
            <div class="request-actions">
              <button 
                @click="responderPeticion(peticion.peticionId, true)"
                class="btn btn-accept"
                :disabled="respondingRequest"
              >
                <ion-icon :icon="checkmark"></ion-icon>
                <span>Aceptar</span>
              </button>
              
              <button 
                @click="responderPeticion(peticion.peticionId, false)"
                class="btn btn-reject"
                :disabled="respondingRequest"
              >
                <ion-icon :icon="close"></ion-icon>
                <span>Rechazar</span>
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Tab de Peticiones Enviadas -->
      <div v-if="activeTab === 'sent'" class="sent-section">
        <div v-if="peticionesEnviadas.length === 0" class="empty-state fade-in">
          <ion-icon :icon="paperPlane" size="large"></ion-icon>
          <p>No has enviado peticiones de amistad</p>
        </div>

        <div v-else class="requests-list">
          <div 
            v-for="(peticion, index) in peticionesEnviadas" 
            :key="peticion.peticionId"
            class="request-card"
            :style="{ '--delay': `${index * 0.1}s` }"
          >
            <div class="request-info">
              <div class="user-avatar">
                <img v-if="peticion.skinRemitente" :src="peticion.skinRemitente" :alt="peticion.usernameRemitente" />
                <ion-icon v-else :icon="personCircle" size="large"></ion-icon>
              </div>
              <div class="request-details">
                <h3>{{ peticion.usernameRemitente }}</h3>
                <p>{{ peticion.emailRemitente }}</p>
                <small>{{ formatearFecha(peticion.fechaCreacion) }}</small>
              </div>
            </div>
            
            <div class="request-status">
              <span :class="['status-badge', peticion.estado.toLowerCase()]">
                <ion-icon 
                  :icon="peticion.estado === 'PENDIENTE' ? hourglass : 
                         peticion.estado === 'ACEPTADA' ? checkmarkCircle : 
                         closeCircle"
                ></ion-icon>
                <span>{{ getEstadoText(peticion.estado) }}</span>
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, nextTick } from 'vue'
import { 
  IonSearchbar, 
  IonSpinner, 
  IonIcon,
  alertController,
  toastController
} from '@ionic/vue'
import { 
  personAdd, 
  personCircle, 
  people, 
  searchOutline, 
  mailOpen,
  paperPlane,
  checkmark,
  close,
  checkmarkCircle,
  closeCircle,
  hourglass
} from 'ionicons/icons'
import { useAuth } from '@/composables/useAuth'
import { useTheme } from '@/composables/useTheme'

// Composables
const { usuario, token, isAuthenticated, getHeaders } = useAuth()
const { isDarkMode } = useTheme()

// Props
const props = defineProps({
  usuarioActualId: {
    type: Number,
    required: true
  },
  apiBaseUrl: {
    type: String,
    default: 'http://192.168.1.234:8090/GameConnect'
  }
})

// Estado reactivo
const activeTab = ref('search')
const searchTerm = ref('')
const usuariosEncontrados = ref([])
const peticionesRecibidas = ref([])
const peticionesEnviadas = ref([])
const loading = ref(false)
const sendingRequest = ref(false)
const respondingRequest = ref(false)
const isMounted = ref(false)

// Configuración de tabs
const tabs = computed(() => [
  {
    id: 'search',
    label: 'Buscar',
    icon: searchOutline,
    badge: null
  },
  {
    id: 'received',
    label: 'Recibidas',
    icon: mailOpen,
    badge: peticionesRecibidas.value.length > 0 ? peticionesRecibidas.value.length : null
  },
  {
    id: 'sent',
    label: 'Enviadas',
    icon: paperPlane,
    badge: null
  }
])

// Métodos (mantienen la misma lógica)
const buscarUsuarios = async () => {
  if (!searchTerm.value.trim()) {
    usuariosEncontrados.value = []
    return
  }

  if (!isAuthenticated.value) {
    mostrarToast('Debes iniciar sesión para buscar usuarios', 'danger')
    return
  }

  loading.value = true
  try {
    const response = await fetch(
      `${props.apiBaseUrl}/usuario/buscar?termino=${encodeURIComponent(searchTerm.value)}&usuarioActualId=${props.usuarioActualId}`,
      {
        headers: getHeaders()
      }
    )
    
    if (response.ok) {
      usuariosEncontrados.value = await response.json()
    } else if (response.status === 401) {
      mostrarToast('Sesión expirada. Por favor, inicia sesión nuevamente', 'danger')
    } else {
      console.error('Error al buscar usuarios')
      usuariosEncontrados.value = []
    }
  } catch (error) {
    console.error('Error:', error)
    usuariosEncontrados.value = []
    mostrarToast('Error al buscar usuarios', 'danger')
  } finally {
    loading.value = false
  }
}

const enviarPeticion = async (usuarioDestinatarioId) => {
  if (!isAuthenticated.value) {
    mostrarToast('Debes iniciar sesión para enviar peticiones', 'danger')
    return
  }

  sendingRequest.value = true
  try {
    const response = await fetch(
      `${props.apiBaseUrl}/usuario/${props.usuarioActualId}/peticion-amistad`,
      {
        method: 'POST',
        headers: {
          ...getHeaders(),
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          usuarioDestinatarioId: usuarioDestinatarioId
        })
      }
    )

    if (response.ok) {
      mostrarToast('Petición de amistad enviada correctamente', 'success')
      const usuario = usuariosEncontrados.value.find(u => u.usuarioId === usuarioDestinatarioId)
      if (usuario) {
        usuario.estadoRelacion = 'PETICION_ENVIADA'
      }
      await cargarPeticionesEnviadas()
    } else if (response.status === 401) {
      mostrarToast('Sesión expirada. Por favor, inicia sesión nuevamente', 'danger')
    } else {
      const errorMsg = await response.text()
      mostrarToast(errorMsg || 'Error al enviar petición', 'danger')
    }
  } catch (error) {
    console.error('Error:', error)
    mostrarToast('Error al enviar petición', 'danger')
  } finally {
    sendingRequest.value = false
  }
}

const responderPeticion = async (peticionId, aceptar) => {
  if (!isAuthenticated.value) {
    mostrarToast('Debes iniciar sesión para responder peticiones', 'danger')
    return
  }

  const alert = await alertController.create({
    header: aceptar ? 'Aceptar solicitud' : 'Rechazar solicitud',
    message: aceptar 
      ? '¿Estás seguro de que quieres aceptar esta solicitud de amistad?' 
      : '¿Estás seguro de que quieres rechazar esta solicitud?',
    buttons: [
      {
        text: 'Cancelar',
        role: 'cancel'
      },
      {
        text: aceptar ? 'Aceptar' : 'Rechazar',
        handler: async () => {
          respondingRequest.value = true
          try {
            const response = await fetch(
              `${props.apiBaseUrl}/peticion-amistad/${peticionId}/responder`,
              {
                method: 'PUT',
                headers: {
                  ...getHeaders(),
                  'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                  aceptar: aceptar
                })
              }
            )

            if (response.ok) {
              const resultado = await response.json()
              if (aceptar) {
                mostrarToast('¡Solicitud aceptada! Se ha creado un chat automáticamente.', 'success')
              } else {
                mostrarToast('Solicitud rechazada', 'medium')
              }
              await cargarPeticionesRecibidas()
              if (usuariosEncontrados.value.length > 0) {
                await buscarUsuarios()
              }
            } else if (response.status === 401) {
              mostrarToast('Sesión expirada. Por favor, inicia sesión nuevamente', 'danger')
            } else {
              const errorMsg = await response.text()
              mostrarToast(errorMsg || 'Error al responder petición', 'danger')
            }
          } catch (error) {
            console.error('Error:', error)
            mostrarToast('Error al responder petición', 'danger')
          } finally {
            respondingRequest.value = false
          }
        }
      }
    ]
  })
  
  await alert.present()
}

const cargarPeticionesRecibidas = async () => {
  if (!isAuthenticated.value) return

  try {
    const response = await fetch(
      `${props.apiBaseUrl}/usuario/${props.usuarioActualId}/peticiones-recibidas`,
      { headers: getHeaders() }
    )
    
    if (response.ok) {
      peticionesRecibidas.value = await response.json()
    } else if (response.status === 401) {
      mostrarToast('Sesión expirada. Por favor, inicia sesión nuevamente', 'danger')
    }
  } catch (error) {
    console.error('Error:', error)
  }
}

const cargarPeticionesEnviadas = async () => {
  if (!isAuthenticated.value) return

  try {
    const response = await fetch(
      `${props.apiBaseUrl}/usuario/${props.usuarioActualId}/peticiones-enviadas`,
      { headers: getHeaders() }
    )
    
    if (response.ok) {
      peticionesEnviadas.value = await response.json()
    } else if (response.status === 401) {
      mostrarToast('Sesión expirada. Por favor, inicia sesión nuevamente', 'danger')
    }
  } catch (error) {
    console.error('Error:', error)
  }
}

const formatearFecha = (fechaStr) => {
  const fecha = new Date(fechaStr)
  const ahora = new Date()
  const diferencia = ahora - fecha
  
  const minutos = Math.floor(diferencia / (1000 * 60))
  const horas = Math.floor(diferencia / (1000 * 60 * 60))
  const dias = Math.floor(diferencia / (1000 * 60 * 60 * 24))
  
  if (minutos < 60) {
    return `Hace ${minutos} minuto${minutos !== 1 ? 's' : ''}`
  } else if (horas < 24) {
    return `Hace ${horas} hora${horas !== 1 ? 's' : ''}`
  } else if (dias < 7) {
    return `Hace ${dias} día${dias !== 1 ? 's' : ''}`
  } else {
    return fecha.toLocaleDateString()
  }
}

const getEstadoText = (estado) => {
  switch (estado) {
    case 'PENDIENTE': return 'Pendiente'
    case 'ACEPTADA': return 'Aceptada'
    case 'RECHAZADA': return 'Rechazada'
    default: return estado
  }
}

const mostrarToast = async (mensaje, color = 'medium') => {
  const toast = await toastController.create({
    message: mensaje,
    duration: 3000,
    position: 'top',
    color: color
  })
  await toast.present()
}

// Lifecycle
onMounted(async () => {
  if (isAuthenticated.value) {
    await Promise.all([
      cargarPeticionesRecibidas(),
      cargarPeticionesEnviadas()
    ])
  } else {
    mostrarToast('Debes iniciar sesión para gestionar amigos', 'warning')
  }
  
  // Activar animaciones después del montaje
  await nextTick()
  setTimeout(() => {
    isMounted.value = true
  }, 100)
})
</script>

<style scoped>
/* Variables CSS para temas *//* Variables CSS para temas */
.friend-management {
  --bg-primary: #ffffff;
  --bg-secondary: #f8fafc;
  --bg-card: #ffffff;
  --text-primary: #1a202c;
  --text-secondary: #4a5568;
  --text-muted: #718096;
  --border-color: #e2e8f0;
  --shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
  --shadow-hover: 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05);
  --radius: 12px;
  --radius-lg: 16px;
  
  /* Colores de botones */
  --primary-color: #3b82f6;
  --primary-hover: #2563eb;
  --success-color: #10b981;
  --success-hover: #059669;
  --danger-color: #ef4444;
  --danger-hover: #dc2626;
  --warning-color: #f59e0b;
  --warning-hover: #d97706;
}

.friend-management.dark-mode {
  --bg-primary: #0f172a;
  --bg-secondary: #1e293b;
  --bg-card: #334155;
  --text-primary: #f1f5f9;
  --text-secondary: #cbd5e1;
  --text-muted: #94a3b8;
  --border-color: #475569;
  --shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.3), 0 2px 4px -1px rgba(0, 0, 0, 0.2);
  --shadow-hover: 0 10px 15px -3px rgba(0, 0, 0, 0.4), 0 4px 6px -2px rgba(0, 0, 0, 0.3);
  
  /* Colores ajustados para modo oscuro */
  --primary-color: #60a5fa;
  --primary-hover: #3b82f6;
  --success-color: #34d399;
  --success-hover: #10b981;
  --danger-color: #f87171;
  --danger-hover: #ef4444;
  --warning-color: #fbbf24;
  --warning-hover: #f59e0b;
}

.friend-management {
  padding: 20px;
  max-width: 900px;
  margin: 0 auto;
  background: var(--bg-primary);
  color: var(--text-primary);
  transition: all 0.3s ease;
  min-height: 100vh;
  opacity: 0;
  transform: translateY(20px);
  animation: mountFadeIn 0.6s ease-out forwards;
}

/* Animación principal de montaje */
@keyframes mountFadeIn {
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* Tabs mejoradas con animación escalonada */
.tabs {
  display: flex;
  background: var(--bg-secondary);
  border-radius: var(--radius-lg);
  padding: 6px;
  margin-bottom: 24px;
  gap: 4px;
  box-shadow: var(--shadow);
  border: 1px solid var(--border-color);
}

.tab-button {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 14px 20px;
  border: none;
  background: transparent;
  border-radius: var(--radius);
  font-weight: 600;
  font-size: 14px;
  color: var(--text-muted);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: pointer;
  position: relative;
  overflow: hidden;
  opacity: 0;
  transform: translateY(10px);
}

/* Animación escalonada para tabs */
.friend-management.mounted .tab-button {
  animation: slideInFade 0.4s ease-out forwards;
  animation-delay: var(--delay);
}

@keyframes slideInFade {
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.tab-button:hover:not(.active) {
  background: var(--bg-card);
  color: var(--text-secondary);
  transform: translateY(-1px);
}

.tab-button.active {
  background: var(--primary-color);
  color: white;
  box-shadow: var(--shadow);
  transform: translateY(-2px);
}

.tab-button.active::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, rgba(255,255,255,0.2) 0%, rgba(255,255,255,0) 100%);
  pointer-events: none;
}

.badge {
  background: var(--danger-color);
  color: white;
  border-radius: 20px;
  padding: 4px 10px;
  font-size: 11px;
  font-weight: 700;
  min-width: 20px;
  text-align: center;
  box-shadow: 0 2px 4px rgba(239, 68, 68, 0.3);
  animation: badgePulse 2s infinite;
}

@keyframes badgePulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.05); }
}

/* Contenido principal */
.tab-content {
  min-height: 500px;
}

.search-container {
  margin-bottom: 24px;
  opacity: 0;
  animation: fadeInUp 0.5s ease-out 0.3s forwards;
}

/* Estados de carga y vacío mejorados */
.loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 60px 20px;
  color: var(--text-muted);
}

.loading ion-spinner {
  --color: var(--primary-color);
}

.empty-state,
.no-results {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
  padding: 80px 20px;
  color: var(--text-muted);
  text-align: center;
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  border: 2px dashed var(--border-color);
}

.empty-state ion-icon,
.no-results ion-icon {
  color: var(--text-muted);
  opacity: 0.6;
}

.empty-state p,
.no-results p {
  margin: 0;
  font-size: 16px;
  font-weight: 500;
}

/* Animaciones suaves para estados */
.fade-in {
  animation: fadeIn 0.4s ease-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* Cards mejoradas con animación suave */
.users-list,
.requests-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.user-card,
.request-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px;
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow);
  border: 1px solid var(--border-color);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
  opacity: 0;
  transform: translateY(15px);
  animation: cardSlideIn 0.4s ease-out forwards;
  animation-delay: var(--delay);
}

@keyframes cardSlideIn {
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.user-card:hover,
.request-card:hover {
  box-shadow: var(--shadow-hover);
  transform: translateY(-2px);
}

.user-card::before,
.request-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, var(--primary-color), var(--success-color));
  opacity: 0;
  transition: opacity 0.3s ease;
}

.user-card:hover::before,
.request-card:hover::before {
  opacity: 1;
}

.user-info,
.request-info {
  display: flex;
  align-items: center;
  gap: 16px;
  flex: 1;
}

/* Avatar mejorado */
.user-avatar {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--primary-color), var(--success-color));
  position: relative;
  box-shadow: 0 4px 8px rgba(0,0,0,0.15);
}

.user-avatar::after {
  content: '';
  position: absolute;
  top: 2px;
  left: 2px;
  right: 2px;
  bottom: 2px;
  border-radius: 50%;
  background: var(--bg-card);
  z-index: 1;
}

.user-avatar img,
.user-avatar ion-icon {
  position: relative;
  z-index: 2;
}

.user-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%;
}

.user-details,
.request-details {
  flex: 1;
}

.user-details h3,
.request-details h3 {
  margin: 0 0 6px 0;
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1.2;
}

.user-details p,
.request-details p {
  margin: 0 0 4px 0;
  font-size: 14px;
  color: var(--text-secondary);
  font-weight: 500;
}

.request-details small {
  font-size: 12px;
  color: var(--text-muted);
  font-weight: 400;
}

/* Botones mejorados */
.user-actions,
.request-actions,
.request-status {
  display: flex;
  gap: 12px;
  align-items: center;
}

.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px 20px;
  border: none;
  border-radius: var(--radius);
  font-weight: 600;
  font-size: 14px;
  text-decoration: none;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
  white-space: nowrap;
  min-width: 120px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.3), transparent);
  transition: left 0.6s ease;
}

.btn:hover::before {
  left: 100%;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none !important;
}

.btn:disabled::before {
  display: none;
}

.btn:not(:disabled):hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 12px rgba(0,0,0,0.15);
}

.btn:not(:disabled):active {
  transform: translateY(0);
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

/* Variantes de botones */
.btn-primary {
  background: var(--primary-color);
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background: var(--primary-hover);
}

.btn-accept {
  background: var(--success-color);
  color: white;
}

.btn-accept:hover:not(:disabled) {
  background: var(--success-hover);
}

.btn-reject {
  background: var(--danger-color);
  color: white;
}

.btn-reject:hover:not(:disabled) {
  background: var(--danger-hover);
}

/* Status badges mejorados */
.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  border-radius: var(--radius);
  font-weight: 600;
  font-size: 13px;
  white-space: nowrap;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
  position: relative;
  overflow: hidden;
  animation: statusSlideIn 0.4s ease-out;
}

@keyframes statusSlideIn {
  from {
    opacity: 0;
    transform: scale(0.9);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

.status-badge.sent {
  background: linear-gradient(135deg, var(--primary-color), var(--primary-hover));
  color: white;
}

.status-badge.received {
  background: linear-gradient(135deg, var(--warning-color), var(--warning-hover));
  color: white;
}

.status-badge.friends {
  background: linear-gradient(135deg, var(--success-color), var(--success-hover));
  color: white;
}

.status-badge.pendiente {
  background: linear-gradient(135deg, var(--warning-color), var(--warning-hover));
  color: white;
}

.status-badge.aceptada {
  background: linear-gradient(135deg, var(--success-color), var(--success-hover));
  color: white;
}

.status-badge.rechazada {
  background: linear-gradient(135deg, var(--danger-color), var(--danger-hover));
  color: white;
}

/* Efectos de shimmer para estados de carga */
.status-badge.pendiente::after {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.3), transparent);
  animation: shimmer 2s infinite;
}

@keyframes shimmer {
  0% { left: -100%; }
  100% { left: 100%; }
}

/* Responsividad mejorada */
@media (max-width: 768px) {
  .friend-management {
    padding: 16px;
  }
  
  .tabs {
    flex-direction: column;
    gap: 8px;
    padding: 8px;
  }
  
  .tab-button {
    padding: 16px;
    font-size: 16px;
  }
  
  .user-card,
  .request-card {
    flex-direction: column;
    align-items: stretch;
    gap: 16px;
    padding: 16px;
  }
  
  .user-info,
  .request-info {
    justify-content: center;
    text-align: center;
  }
  
  .user-actions,
  .request-actions {
    justify-content: center;
    flex-wrap: wrap;
  }
  
  .btn {
    flex: 1;
    min-width: auto;
  }
  
  .user-avatar {
    width: 48px;
    height: 48px;
  }
  
  .empty-state,
  .no-results {
    padding: 40px 16px;
  }
}

@media (max-width: 480px) {
  .friend-management {
    padding: 12px;
  }
  
  .user-details h3,
  .request-details h3 {
    font-size: 16px;
  }
  
  .user-details p,
  .request-details p {
    font-size: 13px;
  }
  
  .btn {
    padding: 10px 16px;
    font-size: 13px;
  }
  
  .status-badge {
    padding: 8px 12px;
    font-size: 12px;
  }
}

/* Mejoras de accesibilidad */
@media (prefers-reduced-motion: reduce) {
  * {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
  }
}

/* Focus states mejorados */
.tab-button:focus-visible,
.btn:focus-visible {
  outline: 2px solid var(--primary-color);
  outline-offset: 2px;
}

/* Estados de hover más suaves en dispositivos táctiles */
@media (hover: hover) {
  .user-card:hover,
  .request-card:hover {
    transform: translateY(-4px);
  }
  
  .btn:hover:not(:disabled) {
    transform: translateY(-3px);
  }
}

/* Scrollbar personalizado */
.users-list::-webkit-scrollbar,
.requests-list::-webkit-scrollbar {
  width: 6px;
}

.users-list::-webkit-scrollbar-track,
.requests-list::-webkit-scrollbar-track {
  background: var(--bg-secondary);
  border-radius: 3px;
}

.users-list::-webkit-scrollbar-thumb,
.requests-list::-webkit-scrollbar-thumb {
  background: var(--border-color);
  border-radius: 3px;
}

.users-list::-webkit-scrollbar-thumb:hover,
.requests-list::-webkit-scrollbar-thumb:hover {
  background: var(--text-muted);
}
</style>