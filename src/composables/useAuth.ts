import { ref, computed } from 'vue';
import axios, { AxiosResponse } from 'axios';
import { toastController } from '@ionic/vue';

interface Usuario {
  usuarioId: number;
  username: string;
  email: string;
  password: string;
  skin: string;
  codigoVerificacion: string | null;
  fechaExpiracionCodigo: string | null;
  emailVerificado: boolean;
  tokenRecuperacion: string | null;
  fechaExpiracionToken: string | null;
  fotoPerfil: string | null;
  fechaCreacion: string | null;
}

interface UserStats {
  id: number | null;
  username: string;
  maxScore: number;
  maxScoreDate: string | null;
  totalGames: number;
  averageScore: number;
  rank: number | string;
  totalPoints: number;
  totalCoins: number;
  recentGames: any[];
  fotoPerfil: string | null; // ← Corregido: puede ser null
  emailVerificado: boolean; // ← Agregado: faltaba este campo
}

// Estado reactivo global
const usuario = ref<Usuario | null>(null);
const token = ref<string | null>(null);

const userStats = ref<UserStats>({
  id: null,
  username: '',
  maxScore: 0,
  maxScoreDate: null,
  totalGames: 0,
  averageScore: 0,
  rank: 0,
  totalPoints: 0,
  totalCoins: 0,
  recentGames: [],
  fotoPerfil: null, // ← Corregido: inicializar como null
  emailVerificado: false // ← Agregado: faltaba este campo
});

const API_BASE_URL = 'http://192.168.1.234:8090/auth';

export function useAuth() {
  const isAuthenticated = computed(() => {
    return !!(token.value && usuario.value);
  });

  // Función para cargar datos desde localStorage
  const cargarDatosAuth = (): void => {
    try {
      // Cargar usuario
      const usuarioStr = localStorage.getItem('usuario');
      if (usuarioStr) {
        usuario.value = JSON.parse(usuarioStr) as Usuario;
        console.log('📸 Usuario cargado desde localStorage:', {
          username: usuario.value?.username,
          fotoPerfil: usuario.value?.fotoPerfil ? 'SÍ' : 'NO',
          emailVerificado: usuario.value?.emailVerificado
        });
      }
      
      // Cargar token
      const tokenStorage = localStorage.getItem('token');
      if (tokenStorage) {
        token.value = tokenStorage;
        // Configurar axios con el token
        axios.defaults.headers.common['Authorization'] = `Bearer ${tokenStorage}`;
      }
    } catch (error) {
      console.error('Error al cargar datos de autenticación:', error);
      // Limpiar datos corruptos
      localStorage.removeItem('usuario');
      localStorage.removeItem('token');
      usuario.value = null;
      token.value = null;
    }
  };

  const initializeUser = (): Usuario | null => {
    cargarDatosAuth();
    
    if (usuario.value) {
      // ← Corregido: mapear correctamente todos los campos
      userStats.value = {
        id: usuario.value.usuarioId,
        username: usuario.value.username,
        maxScore: 0,
        maxScoreDate: null,
        totalGames: 0,
        averageScore: 0,
        rank: 0,
        totalPoints: 0,
        totalCoins: 0,
        recentGames: [],
        fotoPerfil: usuario.value.fotoPerfil || null, // ← Corregido: manejar null correctamente
        emailVerificado: usuario.value.emailVerificado || false // ← Agregado: mapear emailVerificado
      };

      console.log('📸 UserStats inicializado:', {
        username: userStats.value.username,
        fotoPerfil: userStats.value.fotoPerfil ? 'SÍ' : 'NO',
        emailVerificado: userStats.value.emailVerificado
      });

      return usuario.value;
    }
    return null;
  };

  // Función para guardar usuario
  const guardarUsuario = (nuevoUsuario: Usuario): void => {
    usuario.value = nuevoUsuario;
    localStorage.setItem('usuario', JSON.stringify(nuevoUsuario));
    
    // ← Agregado: actualizar userStats cuando se guarda un nuevo usuario
    if (nuevoUsuario) {
      userStats.value = {
        ...userStats.value,
        id: nuevoUsuario.usuarioId,
        username: nuevoUsuario.username,
        fotoPerfil: nuevoUsuario.fotoPerfil || null,
        emailVerificado: nuevoUsuario.emailVerificado || false
      };
    }
  };

  // Función para guardar token
  const guardarToken = (nuevoToken: string): void => {
    token.value = nuevoToken;
    localStorage.setItem('token', nuevoToken);
    axios.defaults.headers.common['Authorization'] = `Bearer ${nuevoToken}`;
  };

  // Headers para peticiones HTTP
  const getHeaders = () => {
    const headers: Record<string, string> = {
      'Content-Type': 'application/json'
    };
    
    if (token.value) {
      headers['Authorization'] = `Bearer ${token.value}`;
    }
    
    return headers;
  };

  const updateUserStats = (newStats: Partial<UserStats>) => {
    userStats.value = { ...userStats.value, ...newStats };
    
    console.log('📊 UserStats actualizado:', {
      ...newStats,
      fotoPerfil: newStats.fotoPerfil ? 'SÍ' : userStats.value.fotoPerfil ? 'SÍ' : 'NO'
    });
  };

  const updateUserGames = (games: any[]): void => {
    userStats.value.recentGames = games;
    userStats.value.totalGames = games.length;

    if (games.length > 0) {
      const maxScoreGame = games.reduce((max, game) => (game.puntos > max.puntos ? game : max));
      userStats.value.maxScore = maxScoreGame.puntos;
      userStats.value.maxScoreDate = maxScoreGame.fecha;

      const totalScore = games.reduce((sum, game) => sum + game.puntos, 0);
      userStats.value.averageScore = Math.round(totalScore / games.length);
    }
  };

  const updateUserRank = (ranking: any[], username: string): void => {
    const userPosition = ranking.findIndex(player => player.username === username);
    userStats.value.rank = userPosition !== -1 ? userPosition + 1 : 'N/A';
  };

  // ← Agregado: función para actualizar foto de perfil
  const updateUserPhoto = (newPhoto: string | null): void => {
    if (usuario.value) {
      usuario.value.fotoPerfil = newPhoto;
      localStorage.setItem('usuario', JSON.stringify(usuario.value));
    }
    
    userStats.value.fotoPerfil = newPhoto;
    
    console.log('📸 Foto de perfil actualizada:', newPhoto ? 'SÍ' : 'NO');
  };

  const logout = async () => {
    try {
      // Limpiar estado reactivo
      usuario.value = null;
      token.value = null;
      
      // ← Corregido: resetear userStats completamente
      userStats.value = {
        id: null,
        username: '',
        maxScore: 0,
        maxScoreDate: null,
        totalGames: 0,
        averageScore: 0,
        rank: 0,
        totalPoints: 0,
        totalCoins: 0,
        recentGames: [],
        fotoPerfil: null,
        emailVerificado: false
      };
      
      // Limpiar localStorage
      localStorage.removeItem('token');
      localStorage.removeItem('usuario');
      
      // Limpiar headers de axios
      delete axios.defaults.headers.common['Authorization'];

      const toast = await toastController.create({
        message: 'Sesión cerrada correctamente',
        duration: 2000,
        position: 'bottom',
        color: 'success'
      });
      await toast.present();
    } catch (error) {
      console.error('Error al cerrar sesión:', error);
      // Forzar limpieza en caso de error
      localStorage.clear();
      delete axios.defaults.headers.common['Authorization'];
      usuario.value = null;
      token.value = null;
      // ← Resetear userStats en caso de error también
      userStats.value = {
        id: null,
        username: '',
        maxScore: 0,
        maxScoreDate: null,
        totalGames: 0,
        averageScore: 0,
        rank: 0,
        totalPoints: 0,
        totalCoins: 0,
        recentGames: [],
        fotoPerfil: null,
        emailVerificado: false
      };
      throw error;
    }
  };

  return {
    // Estado reactivo
    usuario: computed(() => usuario.value),
    token: computed(() => token.value),
    userStats,
    isAuthenticated,
    
    // Funciones
    cargarDatosAuth,
    initializeUser,
    guardarUsuario,
    guardarToken,
    getHeaders,
    updateUserStats,
    updateUserGames,
    updateUserRank,
    updateUserPhoto, // ← Agregado: nueva función
    logout
  };
}

export function useAuthExtended() {
  const isLoading = ref(false);
  const error = ref<string | null>(null);

  const showToast = async (
    message: string,
    color: string = 'success',
    duration: number = 2000
  ) => {
    const toast = await toastController.create({
      message,
      duration,
      position: 'bottom',
      color
    });
    await toast.present();
  };

  const handleApiError = (err: any, defaultMessage = 'Error desconocido'): string => {
    console.error('API Error:', err);

    if (err.code === 'ECONNREFUSED' || err.code === 'ERR_NETWORK') {
      return 'No se pudo conectar con el servidor. Verifica que esté funcionando.';
    }
    if (err.code === 'ECONNABORTED') {
      return 'La conexión tardó demasiado. Intenta de nuevo.';
    }
    if (err.response) {
      const status = err.response.status;
      const data = err.response.data;
      
      if (status === 400) {
        return typeof data === 'string' ? data : 'Datos inválidos';
      }
      if (status === 401) {
        return 'Credenciales incorrectas';
      }
      if (status === 403) {
        return 'Email no verificado';
      }
      if (status === 404) {
        return 'Usuario no encontrado';
      }
      
      return typeof data === 'string' ? data : `Error del servidor: ${status}`;
    }
    if (err.request) {
      return 'No se recibió respuesta del servidor.';
    }

    return err.message || defaultMessage;
  };

  // Verificar código de email
  const verificarEmail = async (codigo: string): Promise<{ success: boolean; message: string }> => {
    try {
      isLoading.value = true;
      error.value = null;
      codigo = codigo.trim();
      
      const response: AxiosResponse<string> = await axios.post(`${API_BASE_URL}/verificar-email`, {
        codigo
      }, {
        headers: {
          'Content-Type': 'application/json'
        },
        timeout: 30000
      });

      await showToast('Email verificado correctamente', 'success');
      return { success: true, message: response.data };
    } catch (err: any) {
      const errorMessage = handleApiError(err, 'Error al verificar email');
      error.value = errorMessage;
      await showToast(errorMessage, 'danger', 3000);
      return { success: false, message: errorMessage };
    } finally {
      isLoading.value = false;
    }
  };

  // Reenviar código de verificación
  const reenviarCodigo = async (email: string): Promise<{ success: boolean; message: string }> => {
    try {
      isLoading.value = true;
      error.value = null;

      const response: AxiosResponse<string> = await axios.post(`${API_BASE_URL}/reenviar-codigo`, {
        email
      }, {
        headers: {
          'Content-Type': 'application/json'
        },
        timeout: 30000
      });

      await showToast('Código de verificación reenviado', 'success');
      return { success: true, message: response.data };
    } catch (err: any) {
      const errorMessage = handleApiError(err, 'Error al reenviar código');
      error.value = errorMessage;
      await showToast(errorMessage, 'danger', 3000);
      return { success: false, message: errorMessage };
    } finally {
      isLoading.value = false;
    }
  };

  // Solicitar recuperación de contraseña
  const solicitarRecuperacion = async (email: string): Promise<{ success: boolean; message: string }> => {
    try {
      isLoading.value = true;
      error.value = null;

      const response: AxiosResponse<string> = await axios.post(`${API_BASE_URL}/solicitar-recuperacion`, {
        email
      }, {
        headers: {
          'Content-Type': 'application/json'
        },
        timeout: 10000
      });

      await showToast('Se ha enviado un enlace de recuperación a tu email', 'success', 3000);
      return { success: true, message: response.data };
    } catch (err: any) {
      const errorMessage = handleApiError(err, 'Error al solicitar recuperación');
      error.value = errorMessage;
      await showToast(errorMessage, 'danger', 3000);
      return { success: false, message: errorMessage };
    } finally {
      isLoading.value = false;
    }
  };

  // Validar token de recuperación
  const validarTokenRecuperacion = async (token: string): Promise<{ success: boolean; message: string }> => {
    try {
      isLoading.value = true;
      const response: AxiosResponse<string> = await axios.get(`${API_BASE_URL}/validar-token-recuperacion`, {
        params: { token },
        timeout: 10000
      });

      return { success: true, message: response.data };
    } catch (err: any) {
      const errorMessage = handleApiError(err, 'Token inválido o expirado');
      return { success: false, message: errorMessage };
    } finally {
      isLoading.value = false;
    }
  };

  // Restablecer contraseña
  const restablecerContrasena = async (token: string, nuevaContrasena: string): Promise<{ success: boolean; message: string }> => {
    try {
      isLoading.value = true;
      error.value = null;

      const response: AxiosResponse<string> = await axios.post(`${API_BASE_URL}/restablecer-contrasena`, {
        token,
        nuevaContrasena
      }, {
        headers: {
          'Content-Type': 'application/json'
        },
        timeout: 10000
      });

      await showToast('Contraseña restablecida correctamente', 'success');
      return { success: true, message: response.data };
    } catch (err: any) {
      const errorMessage = handleApiError(err, 'Error al restablecer contraseña');
      error.value = errorMessage;
      await showToast(errorMessage, 'danger', 3000);
      return { success: false, message: errorMessage };
    } finally {
      isLoading.value = false;
    }
  };
  
  return {
    isLoading,
    error,
    verificarEmail,
    reenviarCodigo,
    solicitarRecuperacion,
    validarTokenRecuperacion,
    restablecerContrasena,
    showToast
  };
}