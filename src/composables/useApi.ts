import { ref } from 'vue';
import axios, { AxiosRequestConfig, AxiosResponse } from 'axios';
import { toastController } from '@ionic/vue';

const API_BASE_URL = 'http://192.168.1.234:8090/GameConnect';

interface Partida {
  partidaId: number;
  usuario: any;
  puntos: number;
  monedas: number;
  fecha: string;
}

export function useApi() {
  const isLoading = ref(false);
  const error = ref<string | null>(null);

  const setupAxiosInterceptors = () => {
    // Limpiar interceptores anteriores para evitar duplicados
    axios.interceptors.response.clear();
    
    const token = localStorage.getItem('token');
    if (token) {
      axios.defaults.headers.common['Authorization'] = `Bearer ${token}`;
    }

    axios.interceptors.response.use(
      response => response,
      error => {
        console.error('Interceptor error:', error.response?.status, error.response?.data);
        
        // Solo redirigir si es realmente un problema de token expirado
        if (error.response?.status === 401) {
          const errorData = error.response?.data;
          const isTokenExpired = errorData?.message?.includes('token') || 
                                errorData?.message?.includes('expired') ||
                                errorData?.error?.includes('token') ||
                                error.response?.headers?.['www-authenticate']?.includes('expired');
          
          if (isTokenExpired) {
            console.warn('Token realmente expirado, limpiando sesión');
            localStorage.removeItem('token');
            localStorage.removeItem('usuario');
            window.location.href = '/';
          }
        }
        
        // Para 403, no limpiar automáticamente - puede ser un problema de permisos específico
        return Promise.reject(error);
      }
    );
  };

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
      const statusText = err.response.statusText;
      const errorData = err.response.data;
      
      // Información más detallada del error
      console.error('Error details:', {
        status,
        statusText,
        data: errorData,
        url: err.config?.url,
        headers: err.config?.headers
      });
      
      if (status === 403) {
        return 'No tienes permisos para acceder a este recurso';
      }
      if (status === 401) {
        return 'Tu sesión ha expirado. Por favor, inicia sesión nuevamente';
      }
      
      return `Error del servidor: ${status} - ${statusText}`;
    }
    if (err.request) {
      return 'No se recibió respuesta del servidor.';
    }

    return err.message || defaultMessage;
  };
const changeUserSkin = async (userId: number, skin: string) => {
  try {
    const token = localStorage.getItem('token');
    if (!token) {
      throw new Error('Token de sesión no encontrado');
    }

    const config: AxiosRequestConfig = {
      method: 'PUT',
      url: `${API_BASE_URL}/usuario/${userId}/skin`,
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      data: { skin } // 👈 Esto genera {"skin": "red"} automáticamente
    };

    console.log(`Changing skin for user ${userId} to ${skin}`);
    const response = await axios(config);
    await showToast(`Skin actualizado a ${skin}`, 'success');
    return response.data;
  } catch (err: any) {
    const errorMessage = handleApiError(err, 'No se pudo cambiar la skin');
    console.error('changeUserSkin error:', errorMessage);
    await showToast(errorMessage, 'danger');
    throw new Error(errorMessage);
  }
};

  const makeRequest = async (
    url: string,
    options: AxiosRequestConfig = {}
  ): Promise<AxiosResponse> => {
    try {
      const token = localStorage.getItem('token');
      if (!token) {
        throw new Error('Token de sesión no encontrado');
      }

      // Verificar que el token no esté vacío o corrupto
      if (token.length < 10) {
        throw new Error('Token de sesión inválido');
      }

      const config: AxiosRequestConfig = {
        timeout: 15000, // Aumentar timeout
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`,
          'Accept': 'application/json'
        },
        ...options
      };

      console.log('Making request to:', `${API_BASE_URL}${url}`);
      console.log('Token (first 20 chars):', token.substring(0, 20) + '...');

      const response = await axios.get(`${API_BASE_URL}${url}`, config);
      return response;
    } catch (err: any) {
      console.error('Request failed:', {
        url: `${API_BASE_URL}${url}`,
        error: err.response?.data || err.message,
        status: err.response?.status
      });
      throw err; // Lanzar el error original para mejor debugging
    }
  };

  const fetchUserGames = async (userId: number): Promise<Partida[]> => {
    try {
      console.log('Fetching games for user:', userId);
      const response = await makeRequest(`/usuario/${userId}/partidas`);

      if (!Array.isArray(response.data)) {
        console.error('Invalid response format:', response.data);
        throw new Error('La respuesta no es un array válido');
      }

      const sortedGames = response.data.sort(
        (a: Partida, b: Partida) => new Date(b.fecha).getTime() - new Date(a.fecha).getTime()
      );

      await showToast(`Se cargaron ${sortedGames.length} partidas correctamente`);
      return sortedGames;
    } catch (err: any) {
      const errorMessage = handleApiError(err);
      console.error('fetchUserGames error:', errorMessage);
      await showToast(errorMessage, 'danger', 4000);
      throw new Error(errorMessage);
    }
  };

  const fetchUserPointsAndMoney = async (userId: number) => {
    try {
      console.log('Fetching points and money for user:', userId);
      
      let totalPoints = 0;
      let totalCoins = 0;

      // Hacer llamadas secuenciales para mejor debugging
      try {
        const pointsResponse = await makeRequest(`/usuario/${userId}/puntos`);
        totalPoints = pointsResponse.data || 0;
        console.log('Points fetched successfully:', totalPoints);
      } catch (error) {
        console.error('Error fetching points:', error);
        // Continuar con monedas aunque falle puntos
      }

      try {
        const moneyResponse = await makeRequest(`/usuario/${userId}/monedas`);
        totalCoins = moneyResponse.data || 0;
        console.log('Coins fetched successfully:', totalCoins);
      } catch (error) {
        console.error('Error fetching coins:', error);
      }

      return { totalPoints, totalCoins };
    } catch (error) {
      console.error('Error al obtener puntos y dinero:', error);
      return { totalPoints: 0, totalCoins: 0 };
    }
  };

  const fetchRankingData = async () => {
    try {
      console.log('Fetching ranking data');
      const response = await makeRequest('/usuario/ranking');

      if (!Array.isArray(response.data)) {
        throw new Error('La respuesta no es un array válido');
      }

      await showToast(`Se cargó el ranking con ${response.data.length} jugadores`);
      return response.data;
    } catch (err: any) {
      const errorMessage = handleApiError(err);
      console.error('fetchRankingData error:', errorMessage);
      await showToast(errorMessage, 'danger', 4000);
      throw new Error(errorMessage);
    }
  };

  
  setupAxiosInterceptors();

  return {
    isLoading,
    error,
    fetchUserGames,
    fetchUserPointsAndMoney,
    fetchRankingData,
    showToast,
    changeUserSkin,
  };
}