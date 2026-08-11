<script setup>
import { computed, ref } from 'vue'
import SurpriseButton from './SurpriseButton.vue'
import DashboardScreen from './DashboardScreen.vue'

const mode = ref('login')
const name = ref('')
const email = ref('')
const password = ref('')
const loading = ref(false)
const feedback = ref('')
const feedbackType = ref('')
const authenticatedUser = ref(null)
const accessToken = ref('')

const isLogin = computed(() => mode.value === 'login')
const title = computed(() => (isLogin.value ? 'Boas-vindas de volta' : 'Crie sua conta'))
const subtitle = computed(() => (
  isLogin.value
    ? 'Entre para acompanhar sua organização acadêmica.'
    : 'Comece a organizar sua rotina acadêmica em um só lugar.'
))

function switchMode(nextMode) {
  mode.value = nextMode
  feedback.value = ''
  feedbackType.value = ''
}

async function submit() {
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
      feedback.value = 'Login realizado com sucesso.'
    } else {
      feedback.value = 'Conta criada com sucesso. Agora faça login para testar o acesso.'
      mode.value = 'login'
      name.value = ''
      password.value = ''
    }

    feedbackType.value = 'success'
  } catch (error) {
    feedback.value = error.message || 'Não foi possível conectar à API.'
    feedbackType.value = 'error'
  } finally {
    loading.value = false
  }
}

function logout() {
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
    <SurpriseButton />
    <section class="auth-intro" aria-labelledby="product-title">
      <div class="auth-brand"><span>•</span> Studdy</div>
      <div>
        <p class="auth-eyebrow">Organização acadêmica</p>
        <h1 id="product-title">Seu semestre,<br><em>sob controle.</em></h1>
        <p class="auth-intro-copy">Uma base simples para reunir tarefas, prazos e compromissos da sua vida acadêmica.</p>
      </div>
      <div class="auth-feature-list">
        <p><span>✓</span> Acompanhe prazos importantes</p>
        <p><span>✓</span> Centralize sua rotina de estudos</p>
      </div>
    </section>

    <section class="auth-panel" aria-labelledby="auth-title">
      <div v-if="authenticatedUser" class="auth-success-card">
        <div class="auth-success-icon">✓</div>
        <p class="auth-eyebrow">Sessão ativa</p>
        <h2 id="auth-title">Olá, {{ authenticatedUser.name }}!</h2>
        <p>Seu login foi validado pela API e o usuário foi encontrado no banco H2.</p>
        <dl>
          <div><dt>E-mail</dt><dd>{{ authenticatedUser.email }}</dd></div>
          <div><dt>ID</dt><dd>{{ authenticatedUser.id }}</dd></div>
        </dl>
        <button class="auth-secondary-button" type="button" @click="logout">Sair da demonstração</button>
      </div>

      <div v-else class="auth-form-wrap">
        <div class="auth-tab-list" role="tablist" aria-label="Autenticação">
          <button :class="{ active: isLogin }" type="button" role="tab" :aria-selected="isLogin" @click="switchMode('login')">Entrar</button>
          <button :class="{ active: !isLogin }" type="button" role="tab" :aria-selected="!isLogin" @click="switchMode('register')">Criar conta</button>
        </div>

        <header>
          <p class="auth-eyebrow">Acesso ao sistema</p>
          <h2 id="auth-title">{{ title }}</h2>
          <p>{{ subtitle }}</p>
        </header>

        <form @submit.prevent="submit">
          <label v-if="!isLogin">Nome completo
            <input v-model.trim="name" type="text" autocomplete="name" required maxlength="100" placeholder="Ex.: Rafael Silva">
          </label>
          <label>E-mail acadêmico
            <input v-model.trim="email" type="email" autocomplete="email" required placeholder="voce@exemplo.com">
          </label>
          <label>Senha
            <input v-model="password" type="password" autocomplete="current-password" required minlength="8" placeholder="Mínimo de 8 caracteres">
          </label>
          <button class="auth-primary-button" type="submit" :disabled="loading">
            {{ loading ? 'Enviando...' : isLogin ? 'Entrar na conta' : 'Criar minha conta' }}
          </button>
        </form>

        <p v-if="feedback" class="auth-feedback" :class="feedbackType" role="status">{{ feedback }}</p>
        <p class="auth-api-note">Esta tela envia requisições para <code>/api/v1/auth</code>.</p>
      </div>
    </section>
  </main>
</template>
