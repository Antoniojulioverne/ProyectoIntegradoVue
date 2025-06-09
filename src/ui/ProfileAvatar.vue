<template>
  <div class="profile-avatar-container" :class="{ 'verified': showVerification && isVerified }">
    <!-- Imagen de perfil principal -->
    <img 
      v-if="profileImage"
      :src="getProfileImageSrc(profileImage)" 
      :alt="username || 'Usuario'"
      class="avatar-image"
      @error="handleImageError"
      @load="handleImageLoad"
      :style="{ 
        width: size + 'px', 
        height: size + 'px',
        borderColor: borderColor || 'var(--primary-color)',
        display: imageError ? 'none' : 'block'
      }"
    />
    
    <!-- Fallback a iniciales cuando no hay imagen o falla -->
    <div 
      class="avatar-initials"
      :style="{ 
        width: size + 'px', 
        height: size + 'px', 
        fontSize: (size * 0.35) + 'px',
        borderColor: borderColor || 'var(--primary-color)',
        display: (!profileImage || imageError) ? 'flex' : 'none'
      }"
    >
      {{ getInitials(username) }}
    </div>
    
    <!-- Badge de verificación -->
    <div 
      v-if="showVerification && isVerified" 
      class="verified-badge"
      :style="{ 
        width: (size * 0.3) + 'px', 
        height: (size * 0.3) + 'px',
        bottom: (size * 0.05) + 'px',
        right: (size * 0.05) + 'px'
      }"
    >
      <ion-icon name="checkmark-circle"></ion-icon>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue';
import { IonIcon } from '@ionic/vue';

// Props con tipos
const props = defineProps({
  profileImage: {
    type: String,
    default: null
  },
  username: {
    type: String,
    default: 'Usuario'
  },
  size: {
    type: Number,
    default: 80
  },
  isVerified: {
    type: Boolean,
    default: false
  },
  showVerification: {
    type: Boolean,
    default: true
  },
  borderColor: {
    type: String,
    default: null
  }
});

// Estado local
const imageError = ref(false);
const imageLoaded = ref(false);
const imageAttempted = ref(false);

// Watch para resetear el estado cuando cambia la imagen
watch(() => props.profileImage, (newVal) => {
  if (newVal) {
    imageError.value = false;
    imageAttempted.value = false;
  }
});

// Lógica para manejar la URL de la imagen
const getProfileImageSrc = (fotoPerfil) => {
  if (!fotoPerfil) return null;

  console.log('ProfileAvatar - Procesando imagen:', fotoPerfil.substring(0, 50) + '...');
  
  // Si ya es una URL completa
  if (fotoPerfil.startsWith('http://') || fotoPerfil.startsWith('https://')) {
    return fotoPerfil;
  }
  
  // Si es base64 sin prefijo, añadir el prefijo
  if (!fotoPerfil.startsWith('data:image/')) {
    console.log('ProfileAvatar - Añadiendo prefijo base64');
    return `data:image/jpeg;base64,${fotoPerfil}`;
  }
  
  // Si ya tiene el prefijo, devolverlo tal como está
  return fotoPerfil;
};

// Manejo de errores al cargar la imagen
const handleImageError = (event) => {
  console.error('ProfileAvatar - Error cargando imagen de perfil:', props.username);
  imageError.value = true;
  imageLoaded.value = false;
  imageAttempted.value = true;
};

// Manejo de carga exitosa
const handleImageLoad = () => {
  console.log('ProfileAvatar - Imagen cargada correctamente:', props.username);
  imageError.value = false;
  imageLoaded.value = true;
  imageAttempted.value = true;
};

// Obtener iniciales del nombre
const getInitials = (userName) => {
  if (!userName) return '?';
  
  const words = userName.trim().split(' ');
  if (words.length >= 2) {
    return (words[0][0] + words[1][0]).toUpperCase();
  }
  return userName.charAt(0).toUpperCase();
};

// Debug info
onMounted(() => {
  console.log('ProfileAvatar montado:', {
    username: props.username,
    hasImage: !!props.profileImage,
    imgLength: props.profileImage?.length
  });
});
</script>

<style scoped>
.profile-avatar-container {
  position: relative;
  display: inline-block;
}

.avatar-image {
  border-radius: 50%;
  object-fit: cover;
  border: 3px solid;
  transition: all 0.3s ease;
  background-color: var(--ion-color-light, #f4f5f8);
}

.avatar-initials {
  border-radius: 50%;
  background: linear-gradient(135deg, var(--primary-dark, #1a2a6c), var(--primary-medium, #b21f1f));
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  border: 3px solid;
  transition: all 0.3s ease;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
}

.verified-badge {
  position: absolute;
  background: var(--secondary-color, #b21f1f);
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid var(--card-bg, white);
  z-index: 2;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.verified-badge ion-icon {
  font-size: 70%;
}

/* Hover effects */
.profile-avatar-container:hover .avatar-image,
.profile-avatar-container:hover .avatar-initials {
  transform: scale(1.05);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

/* Responsive adjustments */
@media (max-width: 768px) {
  .avatar-image,
  .avatar-initials {
    border-width: 2px;
  }
  
  .verified-badge {
    border-width: 1px;
  }
}

/* Dark theme support */
@media (prefers-color-scheme: dark) {
  .avatar-image {
    background-color: var(--ion-color-dark, #222428);
  }
  
  .verified-badge {
    border-color: var(--ion-color-dark, #222428);
  }
}
</style>