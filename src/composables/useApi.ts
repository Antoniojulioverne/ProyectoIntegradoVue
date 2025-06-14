import { ref, computed,readonly } from 'vue';
import axios, { AxiosRequestConfig, AxiosResponse } from 'axios';
import { toastController } from '@ionic/vue';
import config from '@/config/config';

// Tipos
interface ApiError {
  message: string;
  status?: number;
  code?: string;
}


interface Partida {
  partidaId: number;
  usuario: any;
  puntos: number;
  monedas: number;
  fecha: string;
}

// Estado reactivo global del API
const globalApiState = ref({
  isLoading: false,
  error: null as ApiError | null,
  lastRequest: null as string | null
});

export function useApi() {
  // Estado local
  const isLoading = ref(false);
  const error = ref<ApiError | null>(null);

  // Configuración de axios mejorada
  const setupAxiosInterceptors = () => {
    // Limpiar interceptores anteriores
    axios.interceptors.request.clear();
    axios.interceptors.response.clear();
    
    // Request interceptor
    axios.interceptors.request.use(
      (config) => {
        const token = getAuthToken();
        if (token) {
          config.headers.Authorization = `Bearer ${token}`;
        }
        
        // Configuración por defecto
        config.timeout = config.timeout || 15000;
        config.headers['Content-Type'] = config.headers['Content-Type'] || 'application/json';
        config.headers['Accept'] = 'application/json';
        
        globalApiState.value.lastRequest = config.url || null;
        
        return config;
      },
      (error) => {
        console.error('❌ Error en request interceptor:', error);
        return Promise.reject(error);
      }
    );

    // Response interceptor
    axios.interceptors.response.use(
      (response) => {
        // Log exitoso para debugging
        console.log(`✅ API Success: ${response.config.method?.toUpperCase()} ${response.config.url}`);
        return response;
      },
      (error) => {
        console.error('❌ API Error:', {
          url: error.config?.url,
          method: error.config?.method,
          status: error.response?.status,
          data: error.response?.data
        });
        
        // Manejo automático de errores comunes
        if (error.response?.status === 401) {
          handleUnauthorized();
        }
        
        return Promise.reject(error);
      }
    );
  };

  // === UTILIDADES ===

  const getAuthToken = (): string | null => {
    return localStorage.getItem(config.storage.token);
  };

  const handleUnauthorized = () => {
    console.warn('🔐 Token expirado o inválido');
    localStorage.removeItem(config.storage.token);
    localStorage.removeItem(config.storage.user);
    // Redirigir al login se maneja en el componente
    showToast('Tu sesión ha expirado. Por favor, inicia sesión nuevamente', 'warning');
  };

  const showToast = async (
    message: string,
    color: string = 'success',
    duration: number = 3000
  ) => {
    const toast = await toastController.create({
      message,
      duration,
      position: 'bottom',
      color
    });
    await toast.present();
  };

  const handleApiError = (err: any, operation: string = 'operación'): ApiError => {
    console.error(`❌ Error en ${operation}:`, err);

    let apiError: ApiError = {
      message: `Error en ${operation}`,
      status: err.response?.status,
      code: err.code
    };

    // Errores de red
    if (err.code === 'ECONNREFUSED' || err.code === 'ERR_NETWORK') {
      apiError.message = 'No se pudo conectar con el servidor';
    } else if (err.code === 'ECONNABORTED') {
      apiError.message = 'La conexión tardó demasiado';
    } 
    // Errores HTTP
    else if (err.response) {
      const status = err.response.status;
      const data = err.response.data;
      
      switch (status) {
        case 400:
          apiError.message = typeof data === 'string' ? data : 'Datos inválidos';
          break;
        case 401:
          apiError.message = 'No autorizado - Inicia sesión nuevamente';
          break;
        case 403:
          apiError.message = 'No tienes permisos para esta acción';
          break;
        case 404:
          apiError.message = 'Recurso no encontrado';
          break;
        case 409:
          apiError.message = 'Conflicto - El recurso ya existe';
          break;
        case 422:
          apiError.message = 'Datos de entrada inválidos';
          break;
        case 429:
          apiError.message = 'Demasiadas solicitudes - Intenta más tarde';
          break;
        case 500:
          apiError.message = 'Error interno del servidor';
          break;
        default:
          apiError.message = typeof data === 'string' ? data : `Error del servidor (${status})`;
      }
    }
    // Error de request
    else if (err.request) {
      apiError.message = 'No se recibió respuesta del servidor';
    }
    // Error desconocido
    else {
      apiError.message = err.message || 'Error desconocido';
    }

    return apiError;
  };

  // === MÉTODOS GENÉRICOS ===

  const makeRequest = async <T>(
  method: 'GET' | 'POST' | 'PUT' | 'DELETE',
  endpoint: string,
  data?: any,
  axiosConfig?: AxiosRequestConfig  // ✅ Cambiar nombre del parámetro
): Promise<T> => {
  setLoading(true);
  setError(null);

  try {
    // ✅ Ahora config se refiere al objeto de configuración global
    const url = endpoint.startsWith('http') ? endpoint : `${config.api.fullApiUrl}${endpoint}`;
    
    let response: AxiosResponse<T>;
    
    switch (method) {
      case 'GET':
        response = await axios.get(url, axiosConfig);  // ✅ Usar axiosConfig
        break;
      case 'POST':
        response = await axios.post(url, data, axiosConfig);  // ✅ Usar axiosConfig
        break;
      case 'PUT':
        response = await axios.put(url, data, axiosConfig);  // ✅ Usar axiosConfig
        break;
      case 'DELETE':
        response = await axios.delete(url, axiosConfig);  // ✅ Usar axiosConfig
        break;
      default:
        throw new Error(`Método HTTP no soportado: ${method}`);
    }

    return response.data;
  } catch (err: any) {
    const apiError = handleApiError(err, `${method} ${endpoint}`);
    setError(apiError);
    throw apiError;
  } finally {
    setLoading(false);
  }
};

  // === OPERACIONES ESPECÍFICAS ===

  // Usuarios
  const fetchUserGames = async (userId: number): Promise<Partida[]> => {
  console.log('📊 Obteniendo partidas del usuario:', userId);
  
  try {
    const games = await makeRequest<Partida[]>('GET', `/usuario/${userId}/partidas`);
    
    // ✅ Verificar que games sea un array antes de hacer sort
    if (!Array.isArray(games)) {
      console.warn('⚠️ El backend no devolvió un array de partidas:', games);
      return []; // Devolver array vacío
    }
    
    // Verificar si el array está vacío
    if (games.length === 0) {
      console.log('ℹ️ El usuario no tiene partidas');
      await showToast('No has jugado partidas aún');
      return [];
    }
    
    // Ordenar por fecha descendente
    const sortedGames = games.sort(
      (a: Partida, b: Partida) => new Date(b.fecha).getTime() - new Date(a.fecha).getTime()
    );

    await showToast(`Se cargaron ${sortedGames.length} partidas correctamente`);
    return sortedGames;
    
  } catch (error: any) {
    console.error('❌ Error obteniendo partidas:', error);
    
    // ✅ Manejar específicamente el caso de "no content" (204)
    if (error.response?.status === 204 || 
        error.message?.includes('no tiene partidas') ||
        error.message?.includes('NO_CONTENT')) {
      console.log('ℹ️ Usuario sin partidas (HTTP 204)');
      await showToast('No has jugado partidas aún');
      return [];
    }
    
    // Para otros errores, relanzar
    throw error;
  }
};

  const fetchUserStats = async (userId: number) => {
    console.log('📈 Obteniendo estadísticas del usuario:', userId);
    
    try {
      const [pointsData, coinsData] = await Promise.allSettled([
        makeRequest<number>('GET', `/usuario/${userId}/puntos`),
        makeRequest<number>('GET', `/usuario/${userId}/monedas`)
      ]);

      const totalPoints = pointsData.status === 'fulfilled' ? pointsData.value : 0;
      const totalCoins = coinsData.status === 'fulfilled' ? coinsData.value : 0;

      console.log('✅ Estadísticas obtenidas:', { totalPoints, totalCoins });
      return { totalPoints, totalCoins };
      
    } catch (error) {
      console.error('❌ Error obteniendo estadísticas:', error);
      return { totalPoints: 0, totalCoins: 0 };
    }
  };

  const fetchRankingData = async () => {
    console.log('🏆 Obteniendo ranking global');
    
    const ranking = await makeRequest<any[]>('GET', '/usuario/ranking');
    
    await showToast(`Ranking cargado con ${ranking.length} jugadores`);
    return ranking;
  };

  const changeUserSkin = async (userId: number, skin: string) => {
    console.log('🎨 Cambiando skin del usuario:', userId, 'a:', skin);
    
    await makeRequest('PUT', `/usuario/${userId}/skin`, { skin });
    
    await showToast(`Skin actualizado a ${skin}`, 'success');
  };

  const createGame = async (gameData: any) => {
    console.log('🎮 Creando nueva partida:', gameData);
    
    const newGame = await makeRequest('POST', '/partida', gameData);
    
    await showToast('Partida registrada exitosamente', 'success');
    return newGame;
  };

  // Búsqueda
  const searchUsers = async (term: string, currentUserId: number) => {
    if (!term.trim()) {
      throw new Error('El término de búsqueda no puede estar vacío');
    }

    console.log('🔍 Buscando usuarios con término:', term);
    
    return await makeRequest('GET', 
      `/usuario/buscar?termino=${encodeURIComponent(term)}&usuarioActualId=${currentUserId}`);
  };

  // === GESTIÓN DE ESTADO ===

  const setLoading = (loading: boolean) => {
    isLoading.value = loading;
    globalApiState.value.isLoading = loading;
  };

  const setError = (err: ApiError | null) => {
    error.value = err;
    globalApiState.value.error = err;
  };

  const clearError = () => {
    setError(null);
  };

  // === COMPUTED ===

  const hasError = computed(() => error.value !== null);
  const isConnected = computed(() => !error.value || error.value.code !== 'ERR_NETWORK');

  // === INICIALIZACIÓN ===

  setupAxiosInterceptors();

  // === RETORNO DEL COMPOSABLE ===

  return {
    // Estado
    isLoading: readonly(isLoading),
    error: readonly(error),
    hasError,
    isConnected,
    globalApiState: readonly(globalApiState),

    // Métodos genéricos
    makeRequest,
    showToast,
    clearError,

    // Métodos específicos - Usuarios
    fetchUserGames,
    fetchUserStats,
    fetchRankingData,
    changeUserSkin,
    searchUsers,
    createGame,

    // Utilidades
    handleApiError,
    setupAxiosInterceptors
  };
}

// Hook global para acceso rápido al estado de la API
export const useGlobalApiState = () => {
  return {
    globalApiState: readonly(globalApiState),
    isGloballyLoading: computed(() => globalApiState.value.isLoading),
    hasGlobalError: computed(() => globalApiState.value.error !== null)
  };
};