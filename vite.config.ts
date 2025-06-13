/// <reference types="vitest" />

import legacy from '@vitejs/plugin-legacy'
import vue from '@vitejs/plugin-vue'
import path from 'path'
import { defineConfig } from 'vite'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    legacy()
  ],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  test: {
    globals: true,
    environment: 'jsdom'
  } ,build: {
    rollupOptions: {
      output: {
        manualChunks: {
          // Separar Ionic en su propio chunk
          'ionic': ['@ionic/vue', '@ionic/vue-router'],
          // Vue ecosystem
          'vue-vendor': ['vue', 'vue-router'],
          // Tu servicio WebSocket (si es grande)
          'websocket': ['./src/services/websocket/WebSocketService'],
        }
      }
    },
    chunkSizeWarningLimit: 600
  }
})

