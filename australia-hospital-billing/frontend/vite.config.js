import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// In development every /api call is proxied to Spring Boot, so the browser never makes a
// cross-origin request. (The backend also has CORS configured for other setups.)
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        // Drop the browser's Origin header: through the proxy the call is same-origin,
        // so Spring's CORS check must not reject it (e.g. when Vite falls back to port 5174).
        configure: (proxy) => proxy.on('proxyReq', (proxyReq) => proxyReq.removeHeader('origin')),
      },
    },
  },
});
