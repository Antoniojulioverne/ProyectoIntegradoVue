// chatApi.ts - Versión corregida
import axios, { AxiosInstance } from "axios";

const baseUrl = "https://gameconnect-latest.onrender.com/GameConnect/chat";

// Función para obtener el token
function getToken(): string {
  return localStorage.getItem("token") || "";
}

// Crear instancia de axios con interceptor para token dinámico
const axiosInstance: AxiosInstance = axios.create({
  baseURL: baseUrl,
});

// Interceptor para añadir token dinámicamente
axiosInstance.interceptors.request.use(
  (config) => {
    const token = getToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Interceptor para manejar respuestas y errores
axiosInstance.interceptors.response.use(
  (response) => {
    return response;
  },
  (error) => {
    console.error("Error en API:", error.response?.data || error.message);
    
    // Si es 401, el token puede haber expirado
    if (error.response?.status === 401) {
      console.error("Token expirado o inválido");
      // Aquí podrías redirigir al login o renovar el token
    }
    
    return Promise.reject(error);
  }
);

export async function obtenerChatsDeUsuario(usuarioId: number) {
  try {
    console.log(`Obteniendo chats para usuario ${usuarioId}`);
    const res = await axiosInstance.get(`${baseUrl}/usuario/${usuarioId}`);
    
    // Verificar que la respuesta sea un array
    if (!Array.isArray(res.data)) {
      console.warn("La respuesta no es un array:", res.data);
      return [];
    }
    
    return res.data;
  } catch (error) {
    console.error("Error obteniendo chats:", error);
    throw error;
  }
}

export async function crearChatPrivado(usuario1Id: number, usuario2Id: number) {
  try {
    const res = await axiosInstance.post(`/privado`, { usuario1Id, usuario2Id });
    return res.data;
  } catch (error) {
    console.error("Error creando chat privado:", error);
    throw error;
  }
}

export async function obtenerMensajesDeChat(chatId: number) {
  try {
    console.log(`Obteniendo mensajes para chat ${chatId}`);
    const res = await axiosInstance.get(`/${chatId}/mensajes`);
    
    // Verificar que la respuesta sea un array
    if (!Array.isArray(res.data)) {
      console.warn("La respuesta de mensajes no es un array:", res.data);
      return [];
    }
    
    return res.data;
  } catch (error) {
    console.error("Error obteniendo mensajes:", error);
    throw error;
  }
}

export async function enviarMensajeREST(mensajeDTO: any) {
  try {
    const res = await axiosInstance.post(`/mensaje`, mensajeDTO);
    return res.data;
  } catch (error) {
    console.error("Error enviando mensaje REST:", error);
    throw error;
  }
}

export async function marcarMensajesLeidosREST(chatId: number, usuarioId: number) {
  try {
    await axiosInstance.put(`/${chatId}/marcar-leido/${usuarioId}`);
  } catch (error) {
    console.error("Error marcando mensajes como leídos:", error);
    throw error;
  }
}