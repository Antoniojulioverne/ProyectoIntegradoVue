<!-- GestionAmigos.vue - Corregido para mostrar destinatarios en peticiones enviadas -->
<template>
  <div
    class="friend-management"
    :class="{ 'dark-mode': isDarkMode, 'mounted': isMounted }"
  >
    <!-- Tabs de navegación -->
    <div class="tabs fade-in">
      <button
        @click="activeTab = 'search'"
        :class="['tab-button', { active: activeTab === 'search' }]"
      >
        <ion-icon :icon="search"></ion-icon>
        Buscar Amigos
      </button>
      <button
        @click="activeTab = 'received'"
        :class="['tab-button', { active: activeTab === 'received' }]"
      >
        <ion-icon :icon="mail"></ion-icon>
        Solicitudes
        <span v-if="peticionesRecibidas.length > 0" class="badge">
          {{ peticionesRecibidas.length }}
        </span>
      </button>
      <button
        @click="activeTab = 'sent'"
        :class="['tab-button', { active: activeTab === 'sent' }]"
      >
        <ion-icon :icon="paperPlane"></ion-icon>
        Enviadas
        <span v-if="peticionesEnviadas.length > 0" class="badge">
          {{ peticionesEnviadas.length }}
        </span>
      </button>
    </div>

    <!-- Contenido principal -->
    <div class="content">
      <!-- Tab de Búsqueda -->
      <div v-if="activeTab === 'search'" class="search-section">
        <div class="search-box fade-in">
          <input
            v-model="searchTerm"
            type="text"
            placeholder="Buscar por nombre de usuario o email"
            @keyup.enter="buscarUsuarios"
          />
          <button @click="buscarUsuarios" :disabled="loading" class="search-button">
            <ion-icon :icon="loading ? sync : search"></ion-icon>
          </button>
        </div>

        <div v-if="loading" class="loading-state fade-in">
          <ion-icon :icon="searchCircle" size="large"></ion-icon>
          <p>Buscando usuarios...</p>
        </div>

        <div v-else-if="usuariosEncontrados.length > 0" class="users-list">
          <div 
            v-for="(usuario, index) in usuariosEncontrados" 
            :key="usuario.usuarioId"
            class="user-card"
            :style="{ '--delay': `${index * 0.1}s` }"
          >
            <div class="user-info">
              <!-- Debug: Mostrar datos disponibles -->
              <!-- {{ console.log('Usuario encontrado:', usuario) }} -->
              
              <!-- Usar ProfileAvatar para mostrar foto de perfil -->
              <ProfileAvatar
                :profile-image="usuario.fotoPerfil"
                :username="usuario.username || usuario.nombreUsuario"
                :size="48"
                :is-verified="usuario.emailVerificado"
                :show-verification="false"
              />
              <div class="user-details">
                <h3>{{ usuario.username || usuario.nombreUsuario || 'Usuario' }}</h3>
                <p>{{ usuario.email || 'Email no disponible' }}</p>
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
                <span>Enviar</span>
              </button>
              
              <button 
                v-else-if="usuario.estadoRelacion === 'PETICION_ENVIADA'"
                class="btn btn-pending"
                disabled
              >
                <ion-icon :icon="time"></ion-icon>
                <span>Enviada</span>
              </button>
              
              <button 
                v-else-if="usuario.estadoRelacion === 'PETICION_RECIBIDA'"
                @click="responderPeticion(usuario.peticionId, true)"
                class="btn btn-accept"
                :disabled="sendingRequest"
              >
                <ion-icon :icon="checkmark"></ion-icon>
                <span>Aceptar</span>
              </button>
              
              <button 
                v-else-if="usuario.estadoRelacion === 'AMISTAD_EXISTENTE'"
                class="btn btn-friends"
                disabled
              >
                <ion-icon :icon="people"></ion-icon>
                <span>Amigos</span>
              </button>
            </div>
          </div>
        </div>

        <div v-else-if="searchPerformed && !loading" class="no-results fade-in">
          <ion-icon :icon="searchCircle" size="large"></ion-icon>
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
              <!-- Debug: Mostrar datos disponibles -->
              <!-- {{ console.log('Petición recibida:', peticion) }} -->
              
              <!-- Mostrar información del REMITENTE (quien envió la petición) -->
              <ProfileAvatar
                :profile-image="peticion.fotopersonaRemitente || peticion.fotoPerfilRemitente"
                :username="peticion.usernameRemitente || peticion.nombreUsuarioRemitente"
                :size="48"
                :is-verified="true"
                :show-verification="false"
              />
              <div class="request-details">
                <h3>{{ peticion.usernameRemitente || peticion.nombreUsuarioRemitente || 'Usuario' }}</h3>
                <p>{{ peticion.emailRemitente || 'Email no disponible' }}</p>
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
              <!-- Debug: Mostrar datos disponibles -->
              <!-- {{ console.log('Petición enviada:', peticion) }} -->
              
              <!-- CORREGIDO: Mostrar información del DESTINATARIO (a quien enviaste la petición) -->
              <ProfileAvatar
                :profile-image="peticion.fotopersonaDestinatario || peticion.fotoPerfilDestinatario"
                :username="peticion.usernameDestinatario || peticion.nombreUsuarioDestinatario"
                :size="48"
                :is-verified="true"
                :show-verification="false"
              />
              <div class="request-details">
                <h3>{{ peticion.usernameDestinatario || peticion.nombreUsuarioDestinatario || 'Usuario' }}</h3>
                <p>{{ peticion.emailDestinatario || 'Email no disponible' }}</p>
                <small>Enviada {{ formatearFecha(peticion.fechaCreacion) }}</small>
              </div>
            </div>
            
            <div class="request-status">
              <span :class="['status-badge', peticion.estado.toLowerCase()]">
                <ion-icon 
                  :icon="peticion.estado === 'PENDIENTE' ? hourglass : 
                         peticion.estado === 'ACEPTADA' ? checkmarkCircle : close"
                  class="status-icon"
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
import { ref, onMounted, nextTick } from 'vue'
import { 
  alertController, 
  toastController,
  IonIcon
} from '@ionic/vue'
import { 
  search, 
  personAdd, 
  people, 
  time, 
  checkmark, 
  close, 
  mail, 
  mailOpen, 
  paperPlane, 
  searchCircle, 
  sync,
  checkmarkCircle,
  hourglass
} from 'ionicons/icons'
import { useAuth } from '@/composables/useAuth'
import { useTheme } from '@/composables/useTheme'
import ProfileAvatar from '@/ui/ProfileAvatar.vue'

// Props
const props = defineProps({
  usuarioActualId: {
    type: Number,
    required: true
  },
  apiBaseUrl: {
    type: String,
    required: true
  }
})

// Composables
const { isAuthenticated, token, getHeaders } = useAuth()
const { isDarkMode } = useTheme()

// Estado reactivo
const activeTab = ref('search')
const searchTerm = ref('')
const usuariosEncontrados = ref([])
const peticionesRecibidas = ref([])
const peticionesEnviadas = ref([])
const loading = ref(false)
const sendingRequest = ref(false)
const respondingRequest = ref(false)
const searchPerformed = ref(false)
const isMounted = ref(false)

// Cargar peticiones recibidas
const cargarPeticionesRecibidas = async () => {
  if (!isAuthenticated.value) return

  try {
    const response = await fetch(
      `${props.apiBaseUrl}/usuario/${props.usuarioActualId}/peticiones-recibidas`,
      {
        headers: getHeaders()
      }
    )

    if (response.ok) {
      peticionesRecibidas.value = await response.json()
      console.log('Peticiones recibidas:', peticionesRecibidas.value)
    }
  } catch (error) {
    console.error('Error al cargar peticiones recibidas:', error)
  }
}

// Cargar peticiones enviadas
const cargarPeticionesEnviadas = async () => {
  if (!isAuthenticated.value) return

  try {
    const response = await fetch(
      `${props.apiBaseUrl}/usuario/${props.usuarioActualId}/peticiones-enviadas`,
      {
        headers: getHeaders()
      }
    )

    if (response.ok) {
      peticionesEnviadas.value = await response.json()
      console.log('Peticiones enviadas:', peticionesEnviadas.value)
    }
  } catch (error) {
    console.error('Error al cargar peticiones enviadas:', error)
  }
}

// Buscar usuarios
const buscarUsuarios = async () => {
  if (!searchTerm.value.trim() || !isAuthenticated.value) return
  
  loading.value = true
  searchPerformed.value = true
  
  try {
    const response = await fetch(
      `${props.apiBaseUrl}/usuario/buscar?termino=${encodeURIComponent(searchTerm.value)}&usuarioActualId=${props.usuarioActualId}`,
      {
        headers: getHeaders()
      }
    )
    
    if (response.ok) {
      usuariosEncontrados.value = await response.json()
      console.log('Usuarios encontrados:', usuariosEncontrados.value)
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
              
              await Promise.all([
                cargarPeticionesRecibidas(),
                cargarPeticionesEnviadas()
              ])
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

const formatearFecha = (fecha) => {
  if (!fecha) return ''
  
  const fechaObj = new Date(fecha)
  const ahora = new Date()
  const diff = ahora - fechaObj
  
  const minutos = Math.floor(diff / (1000 * 60))
  const horas = Math.floor(diff / (1000 * 60 * 60))
  const dias = Math.floor(diff / (1000 * 60 * 60 * 24))
  
  if (minutos < 1) {
    return 'Hace un momento'
  } else if (minutos < 60) {
    return `Hace ${minutos} minuto${minutos !== 1 ? 's' : ''}`
  } else if (horas < 24) {
    return `Hace ${horas} hora${horas !== 1 ? 's' : ''}`
  } else if (dias < 7) {
    return `Hace ${dias} día${dias !== 1 ? 's' : ''}`
  } else {
    return fechaObj.toLocaleDateString()
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

const recargarDatos = async () => {
  console.log('🔄 Recargando datos de GestionAmigos...')
  
  if (isAuthenticated.value) {
    await Promise.all([
      cargarPeticionesRecibidas(),
      cargarPeticionesEnviadas()
    ])
  }
  
  console.log('✅ Datos de GestionAmigos recargados')
}

defineExpose({
  recargarDatos
})

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
/* Variables CSS para temas */
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
  
  /* Contenedor principal */
  background-color: var(--bg-primary);
  color: var(--text-primary);
  padding: 20px;
  border-radius: var(--radius);
  box-shadow: var(--shadow);
  display: flex;
  flex-direction: column;
  gap: 20px;
  transition: all 0.3s ease;
  overflow: hidden;
  position: relative;
  height: 100%;
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
}

/* Animaciones */
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

.fade-in {
  opacity: 0;
  animation: fadeInUp 0.6s ease forwards;
}

.mounted .fade-in {
  opacity: 1;
}

/* Tabs */
.tabs {
  display: flex;
  background-color: var(--bg-secondary);
  border-radius: var(--radius);
  padding: 4px;
  margin-bottom: 20px;
  box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.1);
}

.tab-button {
  flex: 1;
  padding: 12px 16px;
  border: none;
  background: transparent;
  color: var(--text-secondary);
  border-radius: var(--radius);
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-weight: 600;
  position: relative;
}

.tab-button:hover {
  background-color: var(--bg-card);
  color: var(--text-primary);
}

.tab-button.active {
  background-color: var(--primary-color);
  color: white;
  box-shadow: var(--shadow);
}

.tab-button ion-icon {
  font-size: 18px;
}

.badge {
  background-color: var(--danger-color);
  color: white;
  border-radius: 50%;
  font-size: 12px;
  font-weight: 700;
  padding: 2px 6px;
  min-width: 18px;
  height: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-left: 4px;
}

/* Contenido principal */
.content {
  flex: 1;
  overflow-y: auto;
}

/* Caja de búsqueda */
.search-box {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.search-box input {
  flex: 1;
  padding: 16px 20px;
  border: 2px solid var(--border-color);
  border-radius: var(--radius);
  background-color: var(--bg-card);
  color: var(--text-primary);
  font-size: 16px;
  transition: all 0.3s ease;
}

.search-box input:focus {
  outline: none;
  border-color: var(--primary-color);
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.search-button {
  padding: 16px 20px;
  border: none;
  background-color: var(--primary-color);
  color: white;
  border-radius: var(--radius);
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.search-button:hover:not(:disabled) {
  background-color: var(--primary-hover);
  transform: translateY(-2px);
}

.search-button:disabled {
  background-color: var(--text-muted);
  cursor: not-allowed;
}

.search-button ion-icon {
  font-size: 20px;
}

/* Estados vacíos y de carga */
.empty-state,
.no-results,
.loading-state {
  text-align: center;
  padding: 40px 20px;
  color: var(--text-muted);
}

.empty-state ion-icon,
.no-results ion-icon,
.loading-state ion-icon {
  font-size: 64px;
  margin-bottom: 16px;
  opacity: 0.6;
}

.empty-state p,
.no-results p,
.loading-state p {
  font-size: 18px;
  margin: 0;
}

/* Listas */
.users-list,
.requests-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-height: 500px;
  overflow-y: auto;
}

.user-card,
.request-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  background-color: var(--bg-card);
  border-radius: var(--radius);
  box-shadow: var(--shadow);
  transition: all 0.3s ease;
  border: 1px solid var(--border-color);
  opacity: 0;
  transform: translateY(20px);
  animation: fadeInUp 0.5s forwards;
  animation-delay: var(--delay, 0s);
}

.user-card:hover,
.request-card:hover {
  box-shadow: var(--shadow-hover);
  transform: translateY(-2px);
}

.user-info,
.request-info {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-details h3,
.request-details h3 {
  margin: 0 0 4px 0;
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
}

.user-details p,
.request-details p {
  margin: 0;
  font-size: 14px;
  color: var(--text-secondary);
}

.request-details small {
  display: block;
  margin-top: 4px;
  color: var(--text-muted);
  font-size: 12px;
}

/* Botones */
.btn {
  padding: 10px 20px;
  border: none;
  border-radius: var(--radius);
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  min-width: 120px;
  justify-content: center;
}

.btn ion-icon {
  font-size: 18px;
}

.btn-primary {
  background-color: var(--primary-color);
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background-color: var(--primary-hover);
  transform: translateY(-2px);
}

.btn-accept {
  background-color: var(--success-color);
  color: white;
}

.btn-accept:hover:not(:disabled) {
  background-color: var(--success-hover);
  transform: translateY(-2px);
}

.btn-reject {
  background-color: var(--danger-color);
  color: white;
}

.btn-reject:hover:not(:disabled) {
  background-color: var(--danger-hover);
  transform: translateY(-2px);
}

.btn-pending {
  background-color: var(--warning-color);
  color: white;
  cursor: not-allowed;
}

.btn-friends {
  background-color: var(--success-color);
  color: white;
  cursor: not-allowed;
  opacity: 0.7;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* Status badges */
.status-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  border-radius: var(--radius);
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
}

.status-badge.pendiente {
  background-color: rgba(245, 158, 11, 0.1);
  color: var(--warning-color);
  border: 1px solid rgba(245, 158, 11, 0.3);
}

.status-badge.aceptada {
  background-color: rgba(16, 185, 129, 0.1);
  color: var(--success-color);
  border: 1px solid rgba(16, 185, 129, 0.3);
}

.status-badge.rechazada {
  background-color: rgba(239, 68, 68, 0.1);
  color: var(--danger-color);
  border: 1px solid rgba(239, 68, 68, 0.3);
}

.status-icon {
  font-size: 14px;
}

/* Acciones de peticiones */
.user-actions,
.request-actions {
  display: flex;
  gap: 8px;
}

.request-status {
  text-align: center;
}

/* Responsive design */
@media (max-width: 768px) {
  .friend-management {
    padding: 16px;
    gap: 16px;
  }
  
  .tabs {
    margin-bottom: 16px;
  }
  
  .tab-button {
    padding: 10px 12px;
    font-size: 14px;
    flex-direction: column;
    gap: 4px;
  }
  
  .tab-button ion-icon {
    font-size: 16px;
  }
  
  .user-card,
  .request-card {
    padding: 16px;
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }
  
  .user-actions,
  .request-actions {
    width: 100%;
    justify-content: center;
  }
  
  .btn {
    flex: 1;
    min-width: auto;
  }
  
  .search-box {
    flex-direction: column;
  }
  
  .search-button {
    align-self: center;
    min-width: 120px;
  }
}

/* Scrollbar personalizado */
.users-list::-webkit-scrollbar,
.requests-list::-webkit-scrollbar {
  width: 8px;
}

.users-list::-webkit-scrollbar-track,
.requests-list::-webkit-scrollbar-track {
  background: var(--bg-secondary);
  border-radius: 4px;
}

.users-list::-webkit-scrollbar-thumb,
.requests-list::-webkit-scrollbar-thumb {
  background: var(--border-color);
  border-radius: 4px;
}

.users-list::-webkit-scrollbar-thumb:hover,
.requests-list::-webkit-scrollbar-thumb:hover {
  background: var(--text-muted);
}

/* Estados de hover mejorados */
.user-card:hover .user-details h3,
.request-card:hover .request-details h3 {
  color: var(--primary-color);
}

/* Animación de carga */
@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.search-button ion-icon[icon="sync"] {
  animation: spin 1s linear infinite;
}

/* Efectos de focus para accesibilidad */
.tab-button:focus,
.btn:focus,
.search-button:focus {
  outline: 2px solid var(--primary-color);
  outline-offset: 2px;
}

.search-box input:focus {
  outline: none;
  border-color: var(--primary-color);
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

/* Transiciones suaves para tema oscuro */
.friend-management * {
  transition: background-color 0.3s ease, color 0.3s ease, border-color 0.3s ease;
}
</style>