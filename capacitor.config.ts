import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'io.ionic.starter',
  appName: 'ProyectoWeb',
   server: {
    // CRÍTICO: Permitir contenido HTTP desde HTTPS
    allowNavigation: [
      'https://gameconnect-latest.onrender.com',
      'http://192.168.1.234:*',
      'http://localhost:8090',
      'http://10.0.2.2:8090' // Para emulador Android
    ],
    cleartext: true,
    // Permitir mixed content (HTTP desde HTTPS)
    allowMixedContent: true
  },
  android: {
    // Permitir mixed content en Android
    allowMixedContent: true,
    captureInput: true,
    webContentsDebuggingEnabled: true // Para debug
  },
  ios: {
    // Configuración similar para iOS
    allowsLinkPreview: false,
    scrollEnabled: true
  },
  plugins: {
    CapacitorHttp: {
      enabled: true
    }
  }
};
export default config;
