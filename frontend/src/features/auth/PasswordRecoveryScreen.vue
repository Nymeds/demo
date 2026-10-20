<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import { recoveryRequest } from './recoveryApi'

const props = defineProps({ initialEmail: { type: String, default: '' } })
const emit = defineEmits(['cancel', 'completed'])
const step = ref('request')
const email = ref(props.initialEmail)
const code = ref('')
const password = ref('')
const confirmation = ref('')
const resetToken = ref('')
const showPassword = ref(false)
const loading = ref(false)
const feedback = ref('')
const feedbackType = ref('')
const resendAt = ref(0)
const resetExpiresAt = ref(0)
const now = ref(Date.now())
const heading = ref(null)
const feedbackElement = ref(null)
const controller = new AbortController()
let timer
const waitSeconds = computed(() => Math.max(0, Math.ceil((resendAt.value - now.value) / 1000)))
const titles = { request: 'Recupere sua senha', verify: 'Confira seu e-mail', reset: 'Crie uma nova senha', success: 'Senha recuperada!' }
const title = computed(() => titles[step.value])
const progress = computed(() => ({ request: 1, verify: 2, reset: 3, success: 3 })[step.value])

async function focusHeading() {
  await nextTick()
  heading.value?.focus()
}

async function focusFeedback() {
  await nextTick()
  feedbackElement.value?.focus()
}

onMounted(() => {
  focusHeading()
  timer = setInterval(() => {
    now.value = Date.now()
    if (step.value === 'reset' && !loading.value && now.value >= resetExpiresAt.value) {
      clearSecrets()
      step.value = 'request'
      feedback.value = 'O prazo para criar a senha terminou. Solicite um novo código.'
      feedbackType.value = 'error'
      focusFeedback()
    }
  }, 1000)
})

function clearSecrets() {
  code.value = ''
  resetToken.value = ''
  password.value = ''
  confirmation.value = ''
  showPassword.value = false
}

onUnmounted(() => {
  clearInterval(timer)
  controller.abort()
  clearSecrets()
})

function changeEmail() {
  clearSecrets()
  feedback.value = ''
  step.value = 'request'
  focusHeading()
}

async function perform(action) {
  if (loading.value) return
  if (action === 'request' && waitSeconds.value > 0) return
  if (action === 'reset') {
    if (password.value !== confirmation.value) {
      feedback.value = 'As senhas informadas não são iguais.'
      feedbackType.value = 'error'
      await focusFeedback()
      return
    }
    if (new TextEncoder().encode(password.value).length > 72) {
      feedback.value = 'A senha deve ter no máximo 72 bytes. Caracteres como acentos podem ocupar mais de um byte.'
      feedbackType.value = 'error'
      await focusFeedback()
      return
    }
  }
  loading.value = true
  feedback.value = ''
  try {
    const payload = { email: email.value.trim() }
    if (action === 'verify') payload.code = code.value
    if (action === 'reset') Object.assign(payload, { resetToken: resetToken.value, newPassword: password.value })
    const data = await recoveryRequest(action, payload, controller.signal)
    if (controller.signal.aborted) return
    if (action === 'request') {
      clearSecrets()
      now.value = Date.now()
      resendAt.value = now.value + 60000
      step.value = 'verify'
      feedback.value = data.message
      feedbackType.value = 'success'
    } else if (action === 'verify') {
      code.value = ''
      resetToken.value = data.resetToken
      resetExpiresAt.value = Date.now() + data.expiresIn * 1000
      step.value = 'reset'
    } else {
      clearSecrets()
      step.value = 'success'
    }
    await focusHeading()
  } catch (error) {
    if (!controller.signal.aborted) {
      feedback.value = error.message
      feedbackType.value = 'error'
      await focusFeedback()
    }
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <header>
    <p class="auth-eyebrow">Recuperação de acesso</p>
    <h2 id="auth-title" ref="heading" tabindex="-1">{{ title }}</h2>
    <p v-if="step === 'request'">Informe o e-mail da sua conta para receber um código de recuperação.</p>
    <p v-else-if="step === 'verify'">Digite o código de 6 dígitos enviado para <strong class="recovery-email">{{ email }}</strong>.</p>
    <p v-else-if="step === 'reset'">Escolha uma senha de 8 a 72 caracteres. Você tem 5 minutos para concluir.</p>
    <p v-else>Sua senha foi alterada e as sessões anteriores foram encerradas. Entre com sua nova senha.</p>
  </header>

  <ol v-if="step !== 'success'" class="recovery-progress" aria-label="Etapas de recuperação">
    <li v-for="(label, index) in ['E-mail', 'Código', 'Nova senha']" :key="label"
        :class="{ active: progress >= index + 1 }" :aria-current="progress === index + 1 ? 'step' : undefined">
      <span aria-hidden="true">{{ index + 1 }}</span>{{ label }}
    </li>
  </ol>

  <form v-if="step === 'request'" :aria-busy="loading" @submit.prevent="perform('request')">
    <label>E-mail da sua conta
      <span class="auth-input-wrap">
        <svg viewBox="0 0 24 24" aria-hidden="true"><rect x="3" y="5" width="18" height="14" rx="2" /><path d="m4 7 8 6 8-6" /></svg>
        <input v-model.trim="email" type="email" autocomplete="email" required maxlength="150" placeholder="seu@email.com" :disabled="loading">
      </span>
    </label>
    <button class="auth-primary-button" type="submit" :disabled="loading || waitSeconds > 0">
      {{ loading ? 'Solicitando código...' : waitSeconds > 0 ? `Aguarde ${waitSeconds}s para enviar` : 'Enviar código de recuperação' }}
    </button>
  </form>

  <form v-else-if="step === 'verify'" :aria-busy="loading" @submit.prevent="perform('verify')">
    <label>Código de recuperação
      <span class="auth-input-wrap">
        <input v-model.trim="code" class="recovery-code" type="text" inputmode="numeric" autocomplete="one-time-code"
               pattern="[0-9]{6}" minlength="6" maxlength="6" required placeholder="000000" aria-describedby="recovery-code-hint" :disabled="loading">
      </span>
    </label>
    <p id="recovery-code-hint" class="recovery-hint">O código vale por 10 minutos. Após 5 tentativas incorretas, solicite um novo código.</p>
    <button class="auth-primary-button" type="submit" :disabled="loading">{{ loading ? 'Verificando...' : 'Verificar código' }}</button>
    <button class="auth-text-button" type="button" :disabled="loading || waitSeconds > 0" @click="perform('request')">
      {{ waitSeconds > 0 ? `Reenviar código em ${waitSeconds}s` : 'Reenviar código' }}
    </button>
    <button class="auth-text-button" type="button" :disabled="loading" @click="changeEmail">Corrigir e-mail</button>
    <p class="recovery-hint">Confira o spam. Ao reenviar, use o código mais recente. Limite de 5 envios por hora.</p>
  </form>

  <form v-else-if="step === 'reset'" :aria-busy="loading" @submit.prevent="perform('reset')">
    <label>Nova senha
      <span class="auth-input-wrap">
        <svg viewBox="0 0 24 24" aria-hidden="true"><rect x="5" y="10" width="14" height="11" rx="2" /><path d="M8 10V7a4 4 0 0 1 8 0v3" /></svg>
        <input v-model="password" :type="showPassword ? 'text' : 'password'" autocomplete="new-password" required minlength="8" maxlength="72" placeholder="Mínimo de 8 caracteres" :disabled="loading">
        <button class="password-toggle" type="button" :aria-label="showPassword ? 'Ocultar senha' : 'Mostrar senha'" :aria-pressed="showPassword" @click="showPassword = !showPassword">
          <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M2 12s3.5-6 10-6 10 6 10 6-3.5 6-10 6S2 12 2 12Z" /><circle cx="12" cy="12" r="2.5" /></svg>
        </button>
      </span>
    </label>
    <label>Confirmar nova senha
      <span class="auth-input-wrap">
        <svg viewBox="0 0 24 24" aria-hidden="true"><rect x="5" y="10" width="14" height="11" rx="2" /><path d="M8 10V7a4 4 0 0 1 8 0v3" /></svg>
        <input v-model="confirmation" :type="showPassword ? 'text' : 'password'" autocomplete="new-password" required minlength="8" maxlength="72" placeholder="Digite a nova senha novamente" :disabled="loading">
      </span>
    </label>
    <button class="auth-primary-button" type="submit" :disabled="loading">{{ loading ? 'Salvando senha...' : 'Salvar nova senha' }}</button>
    <button class="auth-text-button" type="button" :disabled="loading" @click="changeEmail">Solicitar novo código</button>
  </form>

  <p v-if="feedback" ref="feedbackElement" tabindex="-1" class="auth-feedback" :class="feedbackType" :role="feedbackType === 'error' ? 'alert' : 'status'">{{ feedback }}</p>
  <button v-if="step === 'success'" class="auth-primary-button recovery-success-button" type="button" @click="emit('completed', email)">Voltar para o login</button>
  <p v-else class="auth-switch"><button type="button" :disabled="loading" @click="emit('cancel')">Voltar para o login</button></p>
</template>
