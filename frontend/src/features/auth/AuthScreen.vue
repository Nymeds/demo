<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import loginPanelImage from '../../assets/images/login-panel.png'
import registerPanelImage from '../../assets/images/register-panel.png'
import DashboardScreen from '../dashboard/DashboardScreen.vue'
import PasswordRecoveryScreen from './PasswordRecoveryScreen.vue'
import { sessionRequest, SessionExpiredError } from './sessionApi'
import AccessDeniedScreen from './AccessDeniedScreen.vue'
import { ACCESS_DENIED_EVENT, protectedFetch } from '../../api/protectedFetch'
import { currentRoute } from './routeAccess'
import {
  beginDeliberateLogout,
  clearSession,
  forgetLegacyTokens,
  getSession,
  LOGOUT_PENDING_KEY,
  onSessionRemovedElsewhere,
  REMEMBER_ME_DAYS,
  saveSession,
} from '../../shared/auth/session.js'
import { exceedsPasswordBytes, PASSWORD_TOO_LONG_MESSAGE } from '../../shared/auth/passwordRules.js'
import { apiRequest, renewSession } from '../../shared/http/apiRequest.js'
import LegalModal from '../legal/LegalModal.vue'
import { useLegalVersions } from '../legal/useLegalVersions.js'

const route = ref(currentRoute())
const mode = ref(route.value.publicMode || 'login')
const deniedStatus = ref(null)
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
const restoringSession = ref(true)
const endingSession = ref(false)
let renewTimer
let sessionVersion = 0

const legal = useLegalVersions()
const legalDocument = ref('')
const fieldErrors = ref({})

function openLegal(doc) {
  legalDocument.value = doc
  if (!legal.versions.value && !legal.loading.value) legal.load()
}

function closeLegal() {
  legalDocument.value = ''
}

const isLogin = computed(() => mode.value === 'login')
const isRegister = computed(() => mode.value === 'register')
const title = computed(() => (isLogin.value ? 'Bem-vindo de volta!' : 'Criar conta'))
const subtitle = computed(() => (
  isLogin.value
    ? 'Faça login para acessar sua conta.'
    : 'Preencha os dados para começar a organizar seus estudos.'
))

// O access token vale poucos minutos: é renovado pelo cookie da sessão um minuto antes de vencer.
function scheduleRenewal(delayMs) {
  clearTimeout(renewTimer)
  const expiresAt = getSession()?.expiresAt
  const delay = delayMs ?? (expiresAt ? Math.max(1000, expiresAt - Date.now() - 60000) : 60000)
  renewTimer = setTimeout(renewAccessToken, delay)
}

function logoutPending(value) {
  try {
    if (value === true) localStorage.setItem(LOGOUT_PENDING_KEY, '1')
    if (value === false) localStorage.removeItem(LOGOUT_PENDING_KEY)
    return localStorage.getItem(LOGOUT_PENDING_KEY) === '1'
  } catch { return false }
}

async function renewAccessToken() {
  if (!authenticatedUser.value || endingSession.value) return
  const version = sessionVersion
  try {
    const auth = await renewSession()
    if (version !== sessionVersion) return
    rememberMe.value = auth.rememberMe
    scheduleRenewal()
  } catch (error) {
    if (version !== sessionVersion) return
    if (error instanceof SessionExpiredError) denyAccess(error.status)
    else scheduleRenewal(10000)
  }
}

function onWindowFocus() {
  const expiresAt = getSession()?.expiresAt
  if (expiresAt && expiresAt - Date.now() < 60000) renewAccessToken()
}

function denyAccess(status = 401) {
  if (deniedStatus.value) return
  deniedStatus.value = status
  sessionVersion++
  clearTimeout(renewTimer)
  authenticatedUser.value = null
  restoringSession.value = false
  clearSession()
}

function onAccessDenied(event) {
  denyAccess(event.detail.status)
}

function onRouteChanged() {
  route.value = currentRoute()
  if (restoringSession.value || deniedStatus.value) return
  if (!route.value.publicMode && !authenticatedUser.value) denyAccess()
  else if (route.value.publicMode && !authenticatedUser.value) switchMode(route.value.publicMode)
}

async function returnToLogin() {
  // Revoga o cookie antes de permitir uma nova tentativa de entrada.
  await logout()
  window.history.replaceState(null, '', '/login')
  route.value = currentRoute()
  switchMode('login')
  deniedStatus.value = null
}

async function restoreSession() {
  const version = sessionVersion
  forgetLegacyTokens()
  try {
    if (logoutPending()) {
      await sessionRequest('logout')
      logoutPending(false)
      if (!route.value.publicMode) denyAccess()
      return
    }
    const auth = await renewSession()
    const response = await protectedFetch('/api/v1/users/me', {
      headers: { Authorization: `Bearer ${auth.accessToken}` },
      signal: AbortSignal.timeout(15000),
    })
    if (!response.ok) {
      throw new Error('Não foi possível carregar sua sessão. Entre novamente ou tente recarregar a página.')
    }
    const user = await response.json()
    if (version !== sessionVersion) return
    authenticatedUser.value = user
    rememberMe.value = auth.rememberMe
    scheduleRenewal()
  } catch (error) {
    if (version !== sessionVersion) return
    if (error instanceof SessionExpiredError && !route.value.publicMode) {
      denyAccess(error.status)
      return
    }
    // Não sobrescreve uma mensagem mais recente (ex.: senha recuperada enquanto a sessão era verificada).
    if (!(error instanceof SessionExpiredError) && !feedback.value) {
      feedback.value = 'Não foi possível restaurar sua sessão. Verifique a conexão ou entre novamente.'
      feedbackType.value = 'error'
    }
  } finally {
    if (version === sessionVersion) restoringSession.value = false
  }
}

let stopSessionRemovedListener = null

onMounted(() => {
  window.addEventListener(ACCESS_DENIED_EVENT, onAccessDenied)
  window.addEventListener('popstate', onRouteChanged)
  window.addEventListener('hashchange', onRouteChanged)
  stopSessionRemovedListener = onSessionRemovedElsewhere(() => {
    if (authenticatedUser.value && !endingSession.value) logout('other-tab')
  })
  restoreSession()
  window.addEventListener('focus', onWindowFocus)
  if (isRegister.value) legal.load()
})

onBeforeUnmount(() => {
  sessionVersion++
  clearTimeout(renewTimer)
  window.removeEventListener('focus', onWindowFocus)
  window.removeEventListener(ACCESS_DENIED_EVENT, onAccessDenied)
  window.removeEventListener('popstate', onRouteChanged)
  window.removeEventListener('hashchange', onRouteChanged)
  stopSessionRemovedListener?.()
})

function switchMode(nextMode) {
  mode.value = nextMode
  feedback.value = ''
  feedbackType.value = ''
  password.value = ''
  confirmPassword.value = ''
  showPassword.value = false
  fieldErrors.value = {}
  if (nextMode === 'register' && !legal.versions.value) legal.load()
}

function recoveryCompleted(recoveredEmail) {
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

  if (!isLogin.value && exceedsPasswordBytes(password.value)) {
    feedback.value = PASSWORD_TOO_LONG_MESSAGE
    feedbackType.value = 'error'
    return
  }

  if (!isLogin.value && !legal.versions.value?.termsVersion) {
    feedback.value = 'Não foi possível carregar a versão dos Termos de Uso. Tente novamente.'
    feedbackType.value = 'error'
    return
  }

  loading.value = true
  feedback.value = ''
  fieldErrors.value = {}

  const path = isLogin.value ? '/api/v1/auth/login' : '/api/v1/auth/register'
  const payload = isLogin.value
    ? { email: email.value, password: password.value, rememberMe: rememberMe.value }
    : {
      name: name.value,
      email: email.value,
      password: password.value,
      acceptedTerms: acceptedTerms.value,
      termsVersion: legal.versions.value.termsVersion,
    }

  try {
    const data = await apiRequest(path, {
      method: 'POST',
      body: payload,
      skipAuth: true,
      // O login devolve o cookie HttpOnly da sessão; "Lembrar de mim" define a validade dele.
      credentials: 'same-origin',
      signal: AbortSignal.timeout(15000),
      fallbackMessage: 'Não foi possível concluir a solicitação.',
    })

    if (isLogin.value) {
      sessionVersion++
      saveSession(data, { rememberMe: rememberMe.value })

      try {
        authenticatedUser.value = await apiRequest('/api/v1/users/me')
      } catch {
        clearSession()
        throw new Error('Login realizado, mas não foi possível carregar o perfil.')
      }

      logoutPending(false)
      password.value = ''
      scheduleRenewal()
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
    fieldErrors.value = error?.fieldErrors || {}
    if (!isLogin.value && await termsVersionChanged(error, payload.termsVersion)) {
      acceptedTerms.value = false
      feedback.value = 'Os Termos de Uso foram atualizados. Leia a versão atual e marque a caixa para aceitar novamente.'
      feedbackType.value = 'error'
      return
    }
    feedback.value = error instanceof TypeError || error?.name === 'TimeoutError'
      ? 'Não foi possível conectar ao servidor. Confirme que a API está iniciada.'
      : error.message || 'Não foi possível conectar à API.'
    feedbackType.value = 'error'
  } finally {
    loading.value = false
  }
}

// O backend não envia um código estável para "Termos atualizados" (só 400 com texto livre).
// Em vez de casar o texto, um 400 sem erros de campo dispara nova leitura das versões vigentes:
// se a versão enviada já não é a atual, é divergência de Termos.
async function termsVersionChanged(error, sentVersion) {
  const hasFieldErrors = Object.keys(error?.fieldErrors || {}).length > 0
  if (error?.status !== 400 || hasFieldErrors) return false

  await legal.load()
  const current = legal.versions.value?.termsVersion
  return Boolean(current) && current !== sentVersion
}

const LOGOUT_MESSAGES = Object.freeze({
  'account-deleted': { text: 'Sua conta e todos os seus dados foram excluídos.', type: 'success' },
  'other-tab': { text: 'Você saiu em outra aba.', type: 'success' },
})

async function logout(reason) {
  if (endingSession.value) return
  endingSession.value = true
  sessionVersion++
  clearTimeout(renewTimer)
  // Saída intencional: nenhuma resposta 401 em voo pode virar "acesso negado".
  beginDeliberateLogout()

  let failed = false
  // O cookie é o mesmo para todas as abas: só a aba que iniciou a saída o revoga no servidor.
  if (reason !== 'other-tab') {
    logoutPending(true)
    try {
      await sessionRequest('logout')
      logoutPending(false)
    } catch { failed = true }
  }

  clearSession()
  authenticatedUser.value = null
  rememberMe.value = false
  endingSession.value = false

  if (!deniedStatus.value) {
    window.history.replaceState(null, '', '/login')
    route.value = currentRoute()
    switchMode('login')
  }

  const message = LOGOUT_MESSAGES[reason]
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

</script>

<template>
  <AccessDeniedScreen v-if="deniedStatus" :status="deniedStatus" @login="returnToLogin" />
  <DashboardScreen
    v-else-if="authenticatedUser"
    :user="authenticatedUser"
    :route-section="route.publicMode ? '' : route.section"
    @logout="logout"
    @user-updated="updateAuthenticatedUser"
  />

  <main v-else class="auth-page">
    <div class="auth-decoration auth-decoration-top" aria-hidden="true"></div>
    <div class="auth-decoration auth-decoration-bottom" aria-hidden="true"></div>

    <section class="auth-card" :class="{ 'is-recovery': mode === 'recovery' }" aria-labelledby="auth-title">
      <aside class="auth-presentation" :class="{ 'is-register': isRegister }">
        <img
          class="auth-panel-image"
          :src="isRegister ? registerPanelImage : loginPanelImage"
          :alt="isRegister
            ? 'Ambiente de estudos com notebook, livros e proteção de dados'
            : 'Apresentação do AcadOrganize e seus recursos acadêmicos'"
          width="794"
          height="1979"
        >
        <!-- Tablet e celular: a arte vertical cortava o título; o mesmo conteúdo em texto se ajusta à largura. -->
        <div class="auth-compact-brand" aria-hidden="true">
          <template v-if="!isRegister">
            <svg class="auth-compact-cap" viewBox="0 0 64 48">
              <path d="M32 4 2 17l30 13 30-13Z" />
              <path d="M14 23v12c0 4 8 8 18 8s18-4 18-8V23L32 31Z" />
              <path class="auth-compact-tassel" d="M57 19v14" />
            </svg>
            <strong>AcadOrganize</strong>
            <span>Seu aliado na organização da vida acadêmica.</span>
          </template>
          <template v-else>
            <strong>Organize seus estudos e alcance seus objetivos</strong>
            <span>Com o AcadOrganize, você tem tudo o que precisa para se manter no controle da sua vida acadêmica.</span>
          </template>
        </div>
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
                <input v-model="rememberMe" type="checkbox" :disabled="loading || restoringSession" aria-describedby="remember-help">
                <span>
                  Manter conectado neste dispositivo
                  <small id="remember-help" class="auth-helper">Você continuará conectado por até {{ REMEMBER_ME_DAYS }} dias. Não use em computadores compartilhados.</small>
                </span>
              </label>
              <button class="auth-text-button" type="button" :disabled="loading" @click="switchMode('recovery')">Esqueci minha senha</button>
            </div>

            <div v-else class="auth-terms-block">
              <div class="auth-checkbox auth-terms">
                <input id="accept-terms" v-model="acceptedTerms" type="checkbox" required :aria-invalid="Boolean(fieldErrors.acceptedTerms)" :aria-describedby="fieldErrors.acceptedTerms ? 'accept-terms-error' : undefined">
                <label for="accept-terms">
                  Li e concordo com os
                  <button type="button" class="auth-link-button" @click="openLegal('terms')">Termos de Uso</button>
                  e a
                  <button type="button" class="auth-link-button" @click="openLegal('privacy')">Política de Privacidade</button>.
                </label>
              </div>
              <p v-if="fieldErrors.acceptedTerms || fieldErrors.termsVersion" id="accept-terms-error" class="auth-field-error" role="alert">{{ fieldErrors.acceptedTerms || fieldErrors.termsVersion }}</p>
              <p v-if="legal.error.value" class="auth-field-error" role="alert">
                {{ legal.error.value }}
                <button type="button" class="auth-link-button" :disabled="legal.loading.value" @click="legal.load()">Tentar novamente</button>
              </p>
            </div>

            <button class="auth-primary-button" type="submit" :disabled="loading || restoringSession || (!isLogin && !legal.versions.value)">
              <span>{{ loading || restoringSession ? 'Aguarde...' : isLogin ? 'Entrar' : 'Criar minha conta' }}</span>
              <svg v-if="!loading" viewBox="0 0 24 24" aria-hidden="true"><path d="m9 18 6-6-6-6" /></svg>
            </button>
          </form>

          <p v-if="feedback" class="auth-feedback" :class="feedbackType" :role="feedbackType === 'error' ? 'alert' : 'status'">{{ feedback }}</p>

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

    <p class="auth-copyright">
      2026. Organização acadêmica feita para estudantes.
    </p>

    <footer class="auth-legal-footer">
      <button type="button" class="auth-link-button" @click="openLegal('terms')">Termos de Uso</button>
      ·
      <button type="button" class="auth-link-button" @click="openLegal('privacy')">Política de Privacidade</button>
    </footer>

    <LegalModal
      v-if="legalDocument"
      :document="legalDocument"
      :version="(legalDocument === 'terms' ? legal.versions.value?.termsVersion : legal.versions.value?.privacyVersion) || ''"
      @close="closeLegal"
    />
  </main>
</template>

<style scoped>
.auth-helper { color: #77728a; display: block; font-size: .7rem; margin-top: 2px; }
.auth-form-options { align-items: flex-start; gap: 12px; }
.auth-form-options .auth-text-button { flex-shrink: 0; }
.auth-link-button { background: none; border: 0; color: #6849d7; cursor: pointer; font: inherit; padding: 0; text-decoration: underline; }
.auth-link-button:focus-visible { outline: 3px solid #b9a7f5; outline-offset: 2px; }
.auth-field-error { color: #df3f32; font-size: .74rem; margin: 6px 0 0; }
.auth-form-panel .auth-terms label { color: #646171; cursor: default; display: block; font-size: .75rem; font-weight: 450; }

@media (max-width: 760px) {
  .auth-helper { font-size: .75rem; }
}
</style>
