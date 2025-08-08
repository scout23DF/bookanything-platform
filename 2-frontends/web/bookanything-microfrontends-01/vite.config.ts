import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import federation from '@originjs/vite-plugin-federation'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    react(),
    federation({
      name: 'bookanything_microfrontends_01',
      filename: 'remoteEntry.js',
      exposes: {
        './BookAnythingApp': './src/App.tsx',
      },
      shared: ['react', 'react-dom', 'leaflet', 'react-leaflet'],
    }),
  ],
  build: {
    modulePreload: false,
    target: 'esnext',
    minify: false,
    cssCodeSplit: false,
  },
})
