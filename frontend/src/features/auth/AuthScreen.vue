<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import loginPanelImage from '../../assets/images/login-panel.png'
import registerPanelImage from '../../assets/images/register-panel.png'
import DashboardScreen from '../dashboard/DashboardScreen.vue'
import {
  beginDeliberateLogout,
  clearSession,
  getRefreshToken,
  getSession,
  onSessionExpired,
  onSessionRemovedElsewhere,
  REMEMBER_ME_DAYS,
  saveSession,
} from '../../shared/auth/session.js'
import { exceedsPasswordBytes, PASSWORD_TOO_LONG_MESSAGE } from '../../shared/auth/passwordRules.js'
import { apiRequest } from '../../shared/http/apiRequest.js'
import LegalModal from '../legal/LegalModal.vue'
import { useLegalVersions } from '../legal/useLegalVersions.js'

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

const legal = useLegalVersions()
const legalDocument = ref('')
const fieldErrors = ref({})

function openLegal(doc) {
  legalDocument.value = doc
}

function closeLegal() {
  legalDocument.value = ''
}

const isLogin = computed(() => mode.value === 'login')
const title = computed(() => (isLogin.value ? 'Bem-vindo de volta!' : 'Criar conta'))
const subtitle = computed(() => (
  isLogin.value
    ? 'Faça login para acessar sua conta.'
    : 'Preencha os dados para começar a organizar seus estudos.'
))

async function restoreSession() {
  const stored = getSession()

  if (!stored?.accessToken) return

  rememberMe.value = Boolean(stored.rememberMe)

  try {
    authenticatedUser.value = await apiRequest('/api/v1/users/me')
  } catch (error) {
    // Sessão expirada já é tratada pelo ouvinte de onSessionExpired.
    if (error.name === 'SessionExpiredError') return
    feedback.value = 'Não foi possível restaurar sua sessão. Verifique se a API está ativa.'
    feedbackType.value = 'error'
  }
}

let stopSessionExpiredListener = null
let stopSessionRemovedListener = null

onMounted(() => {
  stopSessionExpiredListener = onSessionExpired(() => logout('session-expired'))
  stopSessionRemovedListener = onSessionRemovedElsewhere(() => {
    if (authenticatedUser.value) logout('other-tab')
  })
  restoreSession()
  legal.load()
})

onBeforeUnmount(() => {
  stopSessionExpiredListener?.()
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

async function submit() {
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
      fallbackMessage: 'Não foi possível concluir a solicitação.',
    })

    if (isLogin.value) {
      saveSession(data, { rememberMe: rememberMe.value })

      try {
        authenticatedUser.value = await apiRequest('/api/v1/users/me')
      } catch (error) {
        clearSession()
        throw new Error('Login realizado, mas não foi possível carregar o perfil.')
      }

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
    feedback.value = error instanceof TypeError
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
  'session-expired': { text: 'Sua sessão expirou. Entre novamente.', type: 'error' },
  'other-tab': { text: 'Você saiu em outra aba.', type: 'success' },
})

function logout(reason) {
  const message = LOGOUT_MESSAGES[reason]
  const refreshToken = getRefreshToken()
  const isDeliberate = reason !== 'session-expired'

  // Logout intencional: nenhuma resposta 401 em voo pode virar "sua sessão expirou".
  if (isDeliberate) beginDeliberateLogout()

  // Revoga a sessão no servidor; falhas são ignoradas para nunca prender o usuário na tela.
  if (refreshToken && isDeliberate && reason !== 'other-tab') {
    apiRequest('/api/v1/auth/logout', {
      method: 'POST',
      body: { refreshToken },
      skipAuth: true,
    }).catch(() => {})
  }

  clearSession()
  authenticatedUser.value = null
  password.value = ''
  feedback.value = message?.text || ''
  feedbackType.value = message?.type || ''

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
  <DashboardScreen
    v-if="authenticatedUser"
    :user="authenticatedUser"
    @logout="logout"
    @user-updated="updateAuthenticatedUser"
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
                <input v-model="rememberMe" type="checkbox" aria-describedby="remember-help">
                <span>
                  Manter conectado neste dispositivo
                  <small id="remember-help" class="auth-helper">Você continuará conectado por até {{ REMEMBER_ME_DAYS }} dias. Não use em computadores compartilhados.</small>
                </span>
              </label>
              <span class="auth-forgot">
                <button class="disabled-link auth-forgot-button" type="button" disabled aria-describedby="forgot-password-help">Esqueci minha senha</button>
                <small id="forgot-password-help" class="auth-helper">Em breve — fale com o suporte.</small>
              </span>
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

            <button class="auth-primary-button" type="submit" :disabled="loading || (!isLogin && !legal.versions.value)">
              <span>{{ loading ? 'Aguarde...' : isLogin ? 'Entrar' : 'Criar minha conta' }}</span>
              <svg v-if="!loading" viewBox="0 0 24 24" aria-hidden="true"><path d="m9 18 6-6-6-6" /></svg>
            </button>
          </form>

          <p v-if="feedback" class="auth-feedback" :class="feedbackType" :role="feedbackType === 'error' ? 'alert' : 'status'">{{ feedback }}</p>

          <p class="auth-switch">
            {{ isLogin ? 'Ainda não tem uma conta?' : 'Já tem uma conta?' }}
            <button type="button" @click="switchMode(isLogin ? 'register' : 'login')">
              {{ isLogin ? 'Cadastre-se' : 'Fazer login' }}
            </button>
          </p>
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
.auth-forgot { display: flex; flex-direction: column; }
.auth-forgot-button { background: none; border: 0; font: inherit; font-size: .74rem; padding: 0; text-align: left; }
.auth-link-button { background: none; border: 0; color: #6849d7; cursor: pointer; font: inherit; padding: 0; text-decoration: underline; }
.auth-link-button:focus-visible { outline: 3px solid #b9a7f5; outline-offset: 2px; }
.auth-field-error { color: #9a402d; font-size: .74rem; margin: 6px 0 0; }
.auth-form-panel .auth-terms label { color: #646171; cursor: default; display: block; font-size: .75rem; font-weight: 450; }
</style>
