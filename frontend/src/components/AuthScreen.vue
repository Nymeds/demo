<script setup>
import { computed, onMounted, ref } from 'vue'
import loginPanelImage from '../assets/login-panel.png'
import registerPanelImage from '../assets/register-panel.png'
import DashboardScreen from './DashboardScreen.vue'

const mode = ref('login')
const name = ref('')
const email = ref('')
const password = ref('')
const confirmPassword = ref('')
const acceptedTerms = ref(false)
const rememberMe = ref(false)
const showPassword = ref(false)
const loading = ref(false)
const feedback = ref('')
const feedbackType = ref('')
const authenticatedUser = ref(null)
const accessToken = ref('')

const persistentTokenKey = 'acad-organize.access-token'
const sessionTokenKey = 'acad-organize.session-token'

const isLogin = computed(() => mode.value === 'login')
const title = computed(() => (isLogin.value ? 'Bem-vindo de volta!' : 'Criar conta'))
const subtitle = computed(() => (
  isLogin.value
    ? 'Faça login para acessar sua conta.'
    : 'Preencha os dados para começar a organizar seus estudos.'
))

function clearStoredTokens() {
  localStorage.removeItem(persistentTokenKey)
  sessionStorage.removeItem(sessionTokenKey)
}

function storeAccessToken(token) {
  clearStoredTokens()

  if (rememberMe.value) {
    localStorage.setItem(persistentTokenKey, token)
  } else {
    sessionStorage.setItem(sessionTokenKey, token)
  }
}

async function restoreSession() {
  const persistentToken = localStorage.getItem(persistentTokenKey)
  const storedToken = persistentToken || sessionStorage.getItem(sessionTokenKey)

  if (!storedToken) return

  rememberMe.value = Boolean(persistentToken)

  try {
    const response = await fetch('/api/v1/users/me', {
      headers: { Authorization: `Bearer ${storedToken}` },
    })

    if (!response.ok) {
      if (response.status === 401 || response.status === 403) {
        clearStoredTokens()
        feedback.value = 'Sua sessão expirou. Entre novamente.'
        feedbackType.value = 'error'
      }
      return
    }

    authenticatedUser.value = await response.json()
    accessToken.value = storedToken
  } catch {
    feedback.value = 'Não foi possível restaurar sua sessão. Verifique se a API está ativa.'
    feedbackType.value = 'error'
  }
}

onMounted(restoreSession)

function switchMode(nextMode) {
  mode.value = nextMode
  feedback.value = ''
  feedbackType.value = ''
  password.value = ''
  confirmPassword.value = ''
  showPassword.value = false
}

async function submit() {
  if (!isLogin.value && password.value !== confirmPassword.value) {
    feedback.value = 'As senhas informadas não são iguais.'
    feedbackType.value = 'error'
    return
  }

  loading.value = true
  feedback.value = ''

  const path = isLogin.value ? '/api/v1/auth/login' : '/api/v1/auth/register'
  const payload = isLogin.value
    ? { email: email.value, password: password.value }
    : { name: name.value, email: email.value, password: password.value }

  try {
    const response = await fetch(path, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    })

    const data = await response.json().catch(() => ({}))

    if (!response.ok) {
      throw new Error(data.detail || data.message || 'Não foi possível concluir a solicitação.')
    }

    if (isLogin.value) {
      const userResponse = await fetch('/api/v1/users/me', {
        headers: { Authorization: `Bearer ${data.accessToken}` },
      })

      if (!userResponse.ok) {
        throw new Error('Login realizado, mas não foi possível carregar o perfil.')
      }

      authenticatedUser.value = await userResponse.json()
      accessToken.value = data.accessToken
      storeAccessToken(data.accessToken)
    } else {
      feedback.value = 'Conta criada com sucesso. Agora entre com seus dados.'
      feedbackType.value = 'success'
      mode.value = 'login'
      name.value = ''
      password.value = ''
      confirmPassword.value = ''
      acceptedTerms.value = false
    }
  } catch (error) {
    feedback.value = error.message || 'Não foi possível conectar à API.'
    feedbackType.value = 'error'
  } finally {
    loading.value = false
  }
}

function logout() {
  clearStoredTokens()
  authenticatedUser.value = null
  accessToken.value = ''
  password.value = ''
  feedback.value = ''
}
</script>

<template>
  <DashboardScreen
    v-if="authenticatedUser"
    :user="authenticatedUser"
    :access-token="accessToken"
    @logout="logout"
  />

  <main v-else class="auth-page">
    <div class="auth-decoration auth-decoration-top" aria-hidden="true"></div>
    <div class="auth-decoration auth-decoration-bottom" aria-hidden="true"></div>

    <section class="auth-card" aria-labelledby="auth-title">
      <aside class="auth-presentation" :class="{ 'is-register': !isLogin }">
        <img
          class="auth-panel-image"
          :src="isLogin ? loginPanelImage : registerPanelImage"
          :alt="isLogin
            ? 'Apresentação do AcadOrganize e seus recursos acadêmicos'
            : 'Ambiente de estudos com notebook, livros e proteção de dados'"
          width="794"
          height="1979"
        >
      </aside>

      <section class="auth-form-panel">
        <div class="auth-form-wrap">
          <header>
            <p class="auth-eyebrow">{{ isLogin ? 'Acesse sua conta' : 'Comece agora' }}</p>
            <h2 id="auth-title">{{ title }} <span v-if="isLogin" aria-hidden="true">👋</span></h2>
            <p>{{ subtitle }}</p>
          </header>

          <form @submit.prevent="submit">
            <label v-if="!isLogin">
              Nome completo
              <span class="auth-input-wrap">
                <svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="8" r="4" /><path d="M4 21a8 8 0 0 1 16 0" /></svg>
                <input v-model.trim="name" type="text" autocomplete="name" required maxlength="100" placeholder="Seu nome completo">
              </span>
            </label>

            <label>
              E-mail
              <span class="auth-input-wrap">
                <svg viewBox="0 0 24 24" aria-hidden="true"><rect x="3" y="5" width="18" height="14" rx="2" /><path d="m4 7 8 6 8-6" /></svg>
                <input v-model.trim="email" type="email" autocomplete="email" required placeholder="seu@email.com">
              </span>
            </label>

            <label>
              Senha
              <span class="auth-input-wrap">
                <svg viewBox="0 0 24 24" aria-hidden="true"><rect x="5" y="10" width="14" height="11" rx="2" /><path d="M8 10V7a4 4 0 0 1 8 0v3" /></svg>
                <input
                  v-model="password"
                  :type="showPassword ? 'text' : 'password'"
                  :autocomplete="isLogin ? 'current-password' : 'new-password'"
                  required
                  minlength="8"
                  maxlength="72"
                  placeholder="Mínimo de 8 caracteres"
                >
                <button class="password-toggle" type="button" :aria-label="showPassword ? 'Ocultar senha' : 'Mostrar senha'" @click="showPassword = !showPassword">
                  <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M2 12s3.5-6 10-6 10 6 10 6-3.5 6-10 6S2 12 2 12Z" /><circle cx="12" cy="12" r="2.5" /></svg>
                </button>
              </span>
            </label>

            <label v-if="!isLogin">
              Confirmar senha
              <span class="auth-input-wrap">
                <svg viewBox="0 0 24 24" aria-hidden="true"><rect x="5" y="10" width="14" height="11" rx="2" /><path d="M8 10V7a4 4 0 0 1 8 0v3" /></svg>
                <input v-model="confirmPassword" :type="showPassword ? 'text' : 'password'" autocomplete="new-password" required minlength="8" maxlength="72" placeholder="Digite a senha novamente">
              </span>
            </label>

            <div v-if="isLogin" class="auth-form-options">
              <label class="auth-checkbox">
                <input v-model="rememberMe" type="checkbox">
                <span>Lembrar de mim</span>
              </label>
              <span class="disabled-link" title="Funcionalidade ainda não disponível">Esqueci minha senha</span>
            </div>

            <label v-else class="auth-checkbox auth-terms">
              <input v-model="acceptedTerms" type="checkbox" required>
              <span>Li e concordo com os <span class="terms-highlight">Termos de Uso e a Política de Privacidade</span>.</span>
            </label>

            <button class="auth-primary-button" type="submit" :disabled="loading">
              <span>{{ loading ? 'Aguarde...' : isLogin ? 'Entrar' : 'Criar minha conta' }}</span>
              <svg v-if="!loading" viewBox="0 0 24 24" aria-hidden="true"><path d="m9 18 6-6-6-6" /></svg>
            </button>
          </form>

          <p v-if="feedback" class="auth-feedback" :class="feedbackType" role="status">{{ feedback }}</p>

          <p class="auth-switch">
            {{ isLogin ? 'Ainda não tem uma conta?' : 'Já tem uma conta?' }}
            <button type="button" @click="switchMode(isLogin ? 'register' : 'login')">
              {{ isLogin ? 'Cadastre-se' : 'Fazer login' }}
            </button>
          </p>
        </div>
      </section>
    </section>

    <p class="auth-copyright">2026. Organização acadêmica feita para estudantes.</p>
  </main>
</template>
