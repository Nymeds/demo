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
          // Repassa o IP real de cada aparelho (X-Forwarded-For). Sem isto todos chegam ao backend como
          // 127.0.0.1 e um colega errando a senha bloqueia o login dos outros. O backend só confia nesse
          // cabeçalho no perfil dev e no backend do "npm run init" (estratégia "native", confiando só em 127.0.0.1/::1 e no salto mais à direita).
          xfwd: true,
        },
      },
    },
  }
})
