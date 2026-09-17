import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import nightTheme from './postcss/nightTheme.js'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), 'VITE_')

  return {
    plugins: [vue()],
    css: {
      postcss: {
        plugins: [nightTheme()],
      },
    },
    server: {
      proxy: {
        '/api': {
          target: env.VITE_API_PROXY_TARGET || 'http://localhost:8080',
          changeOrigin: true,
          secure: true,
        },
      },
    },
  }
})
