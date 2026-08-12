import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

const apiTarget = process.env.VITE_API_PROXY_TARGET ?? 'http://api:8080'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    host: '0.0.0.0',
    hmr: {
      clientPort: 5173,
    },
    proxy: {
      '/api': apiTarget,
    },
    watch: {
      usePolling: process.env.VITE_USE_POLLING === 'true',
    },
  },
})
