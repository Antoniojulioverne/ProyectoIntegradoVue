// Configuración centralizada de la aplicación
export const config = {
  // API Configuration
  api: {
    baseUrl: import.meta.env.VITE_API_BASE_URL || 'https://gameconnect-latest.onrender.com',
    endpoints: {
      gameConnect: import.meta.env.VITE_API_ENDPOINT || '/GameConnect',
      auth: import.meta.env.VITE_AUTH_ENDPOINT || '/auth',
      ws: import.meta.env.VITE_WS_ENDPOINT || '/ws'
    },
    get fullApiUrl() {
      return `${this.baseUrl}${this.endpoints.gameConnect}`;
    },
    get authUrl() {
      return `${this.baseUrl}${this.endpoints.auth}`;
    },
    get wsUrl() {
      return `ws://${this.baseUrl.replace(/^https?:\/\//, '')}${this.endpoints.ws}`;
    },
    timeout: 15000
  },

  // WebSocket Configuration
  websocket: {
    maxReconnectAttempts: parseInt(import.meta.env.VITE_WS_RECONNECT_ATTEMPTS) || 5,
    reconnectDelay: parseInt(import.meta.env.VITE_WS_RECONNECT_DELAY) || 2000,
    heartbeatInterval: 30000,
    connectionTimeout: 10000
  },

  // App Configuration
  app: {
    name: import.meta.env.VITE_APP_NAME || 'Game Stats',
    version: import.meta.env.VITE_APP_VERSION || '1.0.0',
    defaultSkin: 'blue'
  },

  // Storage Keys
  storage: {
    token: 'token',
    user: 'usuario',
    tempUserData: 'tempUserData'
  },

  // Design System Tokens
  design: {
    colors: {
      primary: {
        50: '#eff6ff',
        100: '#dbeafe',
        500: '#3b82f6',
        600: '#2563eb',
        700: '#1d4ed8',
        900: '#1e3a8a'
      },
      secondary: {
        500: '#10b981',
        600: '#059669'
      },
      error: {
        500: '#ef4444',
        600: '#dc2626'
      },
      warning: {
        500: '#f59e0b',
        600: '#d97706'
      }
    },
    spacing: {
      xs: '0.25rem',
      sm: '0.5rem',
      md: '1rem',
      lg: '1.5rem',
      xl: '2rem',
      '2xl': '3rem'
    },
    borderRadius: {
      sm: '0.375rem',
      md: '0.5rem',
      lg: '0.75rem',
      xl: '1rem',
      '2xl': '1.5rem'
    },
    shadows: {
      sm: '0 1px 3px 0 rgba(0, 0, 0, 0.1), 0 1px 2px 0 rgba(0, 0, 0, 0.06)',
      md: '0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06)',
      lg: '0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05)',
      xl: '0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04)'
    }
  }
};

export default config;