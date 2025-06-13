<!-- GestionAmigos.vue - Actualizado con fotos de perfil usando ProfileAvatar -->
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

        <div v-if="usuariosEncontrados.length > 0" class="users-list">
          <div 
            v-for="(usuario, index) in usuariosEncontrados" 
            :key="usuario.usuarioId"
            class="user-card"
            :style="{ '--delay': `${index * 0.1}s` }"
          >
            <div class="user-info">
              <!-- Usar ProfileAvatar para mostrar foto de perfil -->
              <ProfileAvatar
                :profile-image="usuario.fotoPerfil"
                :username="usuario.username"
                :size="48"
                :is-verified="usuario.emailVerificado"
                :show-verification="false"
              />
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
                <span>Añadir</span>
              </button>
              
              <button 
                v-else-if="usuario.estadoRelacion === 'PETICION_ENVIADA'"
                class="btn btn-disabled"
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
              <!-- Usar ProfileAvatar para mostrar foto de perfil -->
              <ProfileAvatar
                :profile-image="peticion.fotoPerfil"
                :username="peticion.usernameRemitente"
                :size="48"
                :is-verified="peticion.emailVerificado"
                :show-verification="false"
              />
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
              <!-- Usar ProfileAvatar para mostrar foto de perfil -->
              <ProfileAvatar
                :profile-image="peticion.fotoPerfilDestinatario"
                :username="peticion.usernameDestinatario"
                :size="48"
                :is-verified="peticion.emailVerificado"
                :show-verification="false"
              />
              <div class="request-details">
                <h3>{{ peticion.usernameDestinatario }}</h3>
                <p>{{ peticion.emailDestinatario }}</p>
                <small>{{ formatearFecha(peticion.fechaCreacion) }}</small>
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
import { ref, onMounted, nextTick, computed } from 'vue'
import { 
  alertController, 
  toastController 
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
  personCircle, 
  searchCircle, 
  sync,
  checkmarkCircle,
  hourglass
} from 'ionicons/icons'
import { useAuth } from '@/composables/useAuth'
import { useTheme } from '@/composables/useTheme'
import ProfileAvatar from '@/ui/ProfileAvatar.vue' // Importar ProfileAvatar

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
                mostrarToast('Solicitud rechazada', 'success')
              }
              
              // Recargar las listas
              await cargarPeticionesRecibidas()
              await cargarPeticionesEnviadas()
              await buscarUsuarios() // Actualizar si el usuario estaba en la búsqueda
              
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

// Funciones auxiliares
const formatearFecha = (fechaString) => {
  if (!fechaString) return 'Fecha desconocida'
  
  const fecha = new Date(fechaString)
  const ahora = new Date()
  const diferencia = ahora.getTime() - fecha.getTime()
  
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

/* Tabs de navegación */
.tabs {
  display: flex;
  gap: 12px;
  background-color: var(--bg-secondary);
  padding: 12px;
  border-radius: var(--radius);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  position: relative;
  z-index: 1;
}

.tab-button {
  flex: 1;
  padding: 16px 20px;
  border: none;
  border-radius: var(--radius);
  background-color: transparent;
  color: var(--text-secondary);
  font-weight: 600;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  position: relative;
}

.tab-button.active {
  background-color: var(--bg-card);
  color: var(--primary-color);
  box-shadow: var(--shadow);
  transform: translateY(-2px);
}

.tab-button:hover:not(.active) {
  background-color: rgba(0, 0, 0, 0.05);
  transform: translateY(-1px);
}

.tab-button ion-icon {
  font-size: 18px;
}

.badge {
  position: absolute;
  top: 8px;
  right: 8px;
  background-color: var(--danger-color);
  color: white;
  font-size: 10px;
  min-width: 18px;
  height: 18px;
  border-radius: 9px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 6px;
  font-weight: 700;
}

/* Contenido principal */
.content {
  flex: 1;
  overflow: hidden;
  position: relative;
}

/* Sección de búsqueda */
.search-box {
  display: flex;
  margin-bottom: 20px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  border-radius: var(--radius);
  overflow: hidden;
}

.search-box input {
  flex: 1;
  padding: 16px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius) 0 0 var(--radius);
  font-size: 16px;
  background-color: var(--bg-card);
  color: var(--text-primary);
}

.search-box input:focus {
  outline: none;
  border-color: var(--primary-color);
}

.search-box input::placeholder {
  color: var(--text-muted);
}

.search-button {
  padding: 0 20px;
  border: none;
  background-color: var(--primary-color);
  color: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background-color 0.3s ease;
}

.search-button:hover:not(:disabled) {
  background-color: var(--primary-hover);
}

.search-button:disabled {
  background-color: var(--text-muted);
  cursor: not-allowed;
}

.search-button ion-icon {
  font-size: 20px;
}

/* Listas de usuarios y peticiones */
.users-list,
.requests-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-height: 500px;
  overflow-y: auto;
  padding-right: 4px;
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

.user-actions,
.request-actions {
  display: flex;
  gap: 8px;
}

.request-status {
  text-align: center;
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

.btn-friends {
  background-color: var(--text-muted);
  color: white;
}

.btn-disabled {
  background-color: var(--text-muted);
  color: white;
  cursor: not-allowed;
}

.btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
  transform: none;
}

/* Status badges */
.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 16px;
  border-radius: var(--radius);
  font-weight: 600;
  font-size: 14px;
}

.status-badge.pendiente {
  background-color: var(--warning-color);
  color: white;
}

.status-badge.aceptada {
  background-color: var(--success-color);
  color: white;
}

.status-badge.rechazada {
  background-color: var(--text-muted);
  color: white;
}

.status-icon {
  font-size: 16px;
}

/* Empty states */
.empty-state,
.no-results {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: var(--text-muted);
  text-align: center;
  opacity: 0;
  animation: fadeIn 0.5s forwards;
  animation-delay: 0.2s;
}

.empty-state ion-icon,
.no-results ion-icon {
  font-size: 48px;
  margin-bottom: 16px;
  color: var(--text-secondary);
}

.empty-state p,
.no-results p {
  font-size: 16px;
  margin: 0;
}

/* Animaciones */
@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
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

.fade-in {
  opacity: 0;
  animation: fadeIn 0.5s forwards;
  animation-delay: var(--delay, 0s);
}

.friend-management.mounted .fade-in {
  animation-play-state: running;
}

/* Animación de carga */
.loading-bar {
  position: absolute;
  top: 0;
  left: 0;
  height: 3px;
  width: 100%;
  background: linear-gradient(to right, var(--primary-color), var(--success-color));
  transform: translateX(-100%);
  animation: loading 1.5s infinite;
}

@keyframes loading {
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