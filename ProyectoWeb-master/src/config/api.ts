// src/config/api.ts
import { Capacitor } from '@capacitor/core';

export const getApiUrl = (): string => {
  // Detecta si está corriendo en dispositivo nativo o web
  if (Capacitor.isNativePlatform()) {
    // Para dispositivos móviles, usa la IP de tu PC en la red local
    return 'http://192.168.1.234:8090';
  } else {
    // Para navegador web, puedes usar localhost o la IP
    return 'http://192.168.1.234:8090'; // O 'http://localhost:8090'
  }
};

// Servicio de ejemplo
export class GameService {
  private apiUrl = getApiUrl();

  async obtenerAmigos(userId: number) {
    try {
      const response = await fetch(`${this.apiUrl}/GameConnect/usuario/${userId}/amigos`, {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
        },
        credentials: 'include' // Para incluir cookies si las necesitas
      });

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Error al obtener amigos:', error);
      throw error;
    }
  }
}