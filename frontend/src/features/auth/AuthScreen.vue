<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import loginPanelImage from '../../assets/images/login-panel.png'
import registerPanelImage from '../../assets/images/register-panel.png'
import { useAvatar } from '../../composables/useAvatar'
import DashboardScreen from '../dashboard/DashboardScreen.vue'
import PasswordRecoveryScreen from './PasswordRecoveryScreen.vue'
import { sessionRequest, SessionExpiredError } from './sessionApi'

const { clearAvatar } = useAvatar()

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
const restoringSession = ref(true)
const endingSession = ref(false)
let renewTimer
let renewPromise
let tokenExpiresAt = 0
let sessionVersion = 0

const persistentTokenKey = 'acad-organize.access-token'
const sessionTokenKey = 'acad-organize.session-token'
const logoutPendingKey = 'acad-organize.logout-pending'

const isLogin = computed(() => mode.value === 'login')
const title = computed(() => (isLogin.value ? 'Bem-vindo de volta!' : 'Criar conta'))
const subtitle = computed(() => (
  isLogin.value
    ? 'Faça login para acessar sua conta.'
    : 'Preencha os dados para começar a organizar seus estudos.'
))

function clearStoredTokens() {
  try {
    localStorage.removeItem(persistentTokenKey)
    sessionStorage.removeItem(sessionTokenKey)
  } catch { /* A sessão por cookie também funciona quando Web Storage está bloqueado. */ }
}

function storeAccessToken(token, expiresIn = 900) {
  clearStoredTokens()
  accessToken.value = token
  tokenExpiresAt = Date.now() + expiresIn * 1000
  clearTimeout(renewTimer)
  renewTimer = setTimeout(renewAccessToken, Math.max(1, expiresIn - 60) * 1000)
}

function logoutPending(value) {
  try {
    if (value === true) localStorage.setItem(logoutPendingKey, '1')
    if (value === false) localStorage.removeItem(logoutPendingKey)
    return localStorage.getItem(logoutPendingKey) === '1'
  } catch { return false }
}

async function renewAccessToken() {
  if (!authenticatedUser.value || endingSession.value) return
  if (renewPromise) return renewPromise
  const version = sessionVersion
  renewPromise = (async () => {
    try {
      const auth = await sessionRequest('refresh')
      if (version !== sessionVersion) return
      rememberMe.value = auth.rememberMe
      storeAccessToken(auth.accessToken, auth.expiresIn)
    } catch (error) {
      if (version !== sessionVersion) return
      if (error instanceof SessionExpiredError) await logout('session-expired')
      else renewTimer = setTimeout(renewAccessToken, 10000)
    }
  })().finally(() => { renewPromise = null })
  return renewPromise
}

function onWindowFocus() {
  if (tokenExpiresAt - Date.now() < 60000) renewAccessToken()
}

async function restoreSession() {
  clearStoredTokens()
  try {
    if (logoutPending()) {
      await sessionRequest('logout')
      logoutPending(false)
      return
    }
    const auth = await sessionRequest('refresh')
    const response = await fetch('/api/v1/users/me', {
      headers: { Authorization: `Bearer ${auth.accessToken}` },
      signal: AbortSignal.timeout(15000),
    })
    if (!response.ok) {
      throw new Error('Não foi possível carregar sua sessão. Entre novamente ou tente recarregar a página.')
    }
    authenticatedUser.value = await response.json()
    rememberMe.value = auth.rememberMe
    storeAccessToken(auth.accessToken, auth.expiresIn)
  } catch (error) {
    if (!(error instanceof SessionExpiredError)) {
      feedback.value = 'Não foi possível restaurar sua sessão. Verifique a conexão ou entre novamente.'
      feedbackType.value = 'error'
    }
  } finally {
    restoringSession.value = false
  }
}

onMounted(() => {
  restoreSession()
  window.addEventListener('focus', onWindowFocus)
})
onBeforeUnmount(() => {
  sessionVersion++
  clearTimeout(renewTimer)
  window.removeEventListener('focus', onWindowFocus)
})

function switchMode(nextMode) {
  mode.value = nextMode
  feedback.value = ''
  feedbackType.value = ''
  password.value = ''
  confirmPassword.value = ''
  showPassword.value = false
}

function recoveryCompleted(recoveredEmail) {
  clearStoredTokens()
  switchMode('login')
  email.value = recoveredEmail
  feedback.value = 'Senha recuperada com sucesso. Entre com sua nova senha.'
  feedbackType.value = 'success'
}

async function submit() {
  if (loading.value || restoringSession.value) return
  if (!isLogin.value && password.value !== confirmPassword.value) {
    feedback.value = 'As senhas informadas não são iguais.'
    feedbackType.value = 'error'
    return
  }

  loading.value = true
  feedback.value = ''

  const path = isLogin.value ? '/api/v1/auth/login' : '/api/v1/auth/register'
  const payload = isLogin.value
    ? { email: email.value, password: password.value, rememberMe: rememberMe.value }
    : { name: name.value, email: email.value, password: password.value }

  try {
    const response = await fetch(path, {
      method: 'POST',
      credentials: 'same-origin',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
      signal: AbortSignal.timeout(15000),
    })

    const data = await response.json().catch(() => ({}))

    if (!response.ok) {
      if ([502, 503, 504].includes(response.status)) {
        throw new Error('O servidor está indisponível. Verifique se a API está iniciada e tente novamente.')
      }

      const fieldErrors = data.errors && typeof data.errors === 'object'
        ? Object.values(data.errors).filter(Boolean).join(' ')
        : ''

      throw new Error(fieldErrors || data.detail || data.message || `Não foi possível concluir a solicitação (erro ${response.status}).`)
    }

    if (isLogin.value) {
      const userResponse = await fetch('/api/v1/users/me', {
        headers: { Authorization: `Bearer ${data.accessToken}` },
      })

      if (!userResponse.ok) {
        throw new Error('Login realizado, mas não foi possível carregar o perfil.')
      }

      authenticatedUser.value = await userResponse.json()
      sessionVersion++
      logoutPending(false)
      password.value = ''
      storeAccessToken(data.accessToken, data.expiresIn)
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
    feedback.value = error instanceof TypeError
      ? 'Não foi possível conectar ao servidor. Confirme que a API está iniciada.'
      : error.message || 'Não foi possível conectar à API.'
    feedbackType.value = 'error'
  } finally {
    loading.value = false
  }
}

const LOGOUT_MESSAGES = Object.freeze({
  'account-deleted': { text: 'Sua conta e todos os seus dados foram excluídos.', type: 'success' },
  'session-expired': { text: 'Sua sessão expirou. Entre novamente para continuar.', type: 'error' },
})

async function logout(reason) {
  if (endingSession.value) return
  endingSession.value = true
  sessionVersion++
  clearTimeout(renewTimer)
  logoutPending(true)
  let failed = false
  try {
    await sessionRequest('logout')
    logoutPending(false)
  } catch { failed = true }
  const message = LOGOUT_MESSAGES[reason]

  clearStoredTokens()
  authenticatedUser.value = null
  accessToken.value = ''
  password.value = ''
  rememberMe.value = false
  tokenExpiresAt = 0
  endingSession.value = false
  // A foto do usuário anterior não pode aparecer para quem entrar em seguida.
  clearAvatar()
  feedback.value = message?.text || (failed ? 'Você saiu neste navegador. Reconecte para encerrar também a sessão salva no servidor.' : '')
  feedbackType.value = message?.type || (failed ? 'error' : '')

  if (reason === 'account-deleted') {
    email.value = ''
  }
}

function updateAuthenticatedUser(profile) {
  authenticatedUser.value = { ...authenticatedUser.value, ...profile }
  email.value = profile.email
}

function refreshAccessToken(token) {
  sessionVersion++
  storeAccessToken(token)
}

</script>

<template>
  <DashboardScreen
    v-if="authenticatedUser"
    :user="authenticatedUser"
    :access-token="accessToken"
    @logout="logout"
    @user-updated="updateAuthenticatedUser"
    @token-refreshed="refreshAccessToken"
  />

  <main v-else class="auth-page">
    <div class="auth-decoration auth-decoration-top" aria-hidden="true"></div>
    <div class="auth-decoration auth-decoration-bottom" aria-hidden="true"></div>

    <section class="auth-card" :class="{ 'is-recovery': mode === 'recovery' }" aria-labelledby="auth-title">
      <aside class="auth-presentation" :class="{ 'is-register': mode === 'register' }">
        <img
          class="auth-panel-image"
          :src="mode !== 'register' ? loginPanelImage : registerPanelImage"
          :alt="mode !== 'register'
            ? 'Apresentação do AcadOrganize e seus recursos acadêmicos'
            : 'Ambiente de estudos com notebook, livros e proteção de dados'"
          width="794"
          height="1979"
        >
      </aside>

      <section class="auth-form-panel">
        <div class="auth-form-wrap">
          <PasswordRecoveryScreen v-if="mode === 'recovery'" :initial-email="email"
            @cancel="switchMode('login')" @completed="recoveryCompleted" />
          <template v-else>
          <header>
            <p class="auth-eyebrow">{{ isLogin ? 'Acesse sua conta' : 'Comece agora' }}</p>
            <h2 id="auth-title">{{ title }} <span v-if="isLogin" aria-hidden="true">👋</span></h2>
            <p>{{ subtitle }}</p>
          </header>

          <p v-if="restoringSession" class="auth-feedback" role="status">Verificando sua sessão...</p>
          <form :aria-busy="loading || restoringSession" @submit.prevent="submit">
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
                <input v-model="rememberMe" type="checkbox" :disabled="loading || restoringSession" aria-describedby="remember-me-help">
                <span>Lembrar de mim</span>
              </label>
              <button class="auth-text-button" type="button" :disabled="loading" @click="switchMode('recovery')">Esqueci minha senha</button>
            </div>
            <p v-if="isLogin" id="remember-me-help" class="auth-session-help">Marque para continuar conectado por até 30 dias neste navegador.</p>

            <label v-else class="auth-checkbox auth-terms">
              <input v-model="acceptedTerms" type="checkbox" required>
              <span>Li e concordo com os <span class="terms-highlight">Termos de Uso e a Política de Privacidade</span>.</span>
            </label>

            <button class="auth-primary-button" type="submit" :disabled="loading || restoringSession">
              <span>{{ loading || restoringSession ? 'Aguarde...' : isLogin ? 'Entrar' : 'Criar minha conta' }}</span>
              <svg v-if="!loading" viewBox="0 0 24 24" aria-hidden="true"><path d="m9 18 6-6-6-6" /></svg>
            </button>
          </form>

          <p v-if="feedback" class="auth-feedback" :class="feedbackType" role="status">{{ feedback }}</p>

          <p class="auth-switch">
            {{ isLogin ? 'Ainda não tem uma conta?' : 'Já tem uma conta?' }}
            <button type="button" :disabled="loading" @click="switchMode(isLogin ? 'register' : 'login')">
              {{ isLogin ? 'Cadastre-se' : 'Fazer login' }}
            </button>
          </p>
          </template>
        </div>
      </section>
    </section>

    <p class="auth-copyright">2026. Organização acadêmica feita para estudantes.</p>
  </main>
</template>
