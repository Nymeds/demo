<script setup>
import { computed, ref } from 'vue'
import { formatLongDate } from './settingsApi'

const MIN_PASSWORD_LENGTH = 8
const MAX_PASSWORD_LENGTH = 72
// O BCrypt do backend limita a senha em bytes; letras acentuadas ocupam 2 bytes em UTF-8.
const BCRYPT_MAX_BYTES = 72
const utf8 = new TextEncoder()

const props = defineProps({
  api: { type: Object, required: true },
  passwordChangedAt: { type: String, default: null },
})

const emit = defineEmits(['saved', 'failed'])

const currentPassword = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const showPasswords = ref(false)
const saving = ref(false)

const newPasswordTooShort = computed(() => newPassword.value.length > 0 && newPassword.value.length < MIN_PASSWORD_LENGTH)
const newPasswordTooLong = computed(() => utf8.encode(newPassword.value).length > BCRYPT_MAX_BYTES)
const newPasswordInvalid = computed(() => newPasswordTooShort.value || newPasswordTooLong.value)
const confirmationMismatch = computed(() => confirmPassword.value.length > 0 && confirmPassword.value !== newPassword.value)
const canSubmit = computed(() => (
  currentPassword.value.length > 0
  && newPassword.value.length >= MIN_PASSWORD_LENGTH
  && !newPasswordTooLong.value
  && newPassword.value === confirmPassword.value
  && !saving.value
))
const lastChange = computed(() => formatLongDate(props.passwordChangedAt))

function clearForm() {
  currentPassword.value = ''
  newPassword.value = ''
  confirmPassword.value = ''
  showPasswords.value = false
}

async function submit() {
  if (!canSubmit.value) return

  saving.value = true

  try {
    const authResponse = await props.api.changePassword({
      currentPassword: currentPassword.value,
      newPassword: newPassword.value,
    })
    clearForm()
    emit('saved', authResponse)
  } catch (error) {
    emit('failed', error)
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <section class="settings-card" aria-labelledby="settings-password-title">
    <header class="settings-card-header">
      <span class="settings-card-icon" aria-hidden="true">
        <svg viewBox="0 0 24 24"><rect x="5" y="10" width="14" height="11" rx="2" /><path d="M8 10V7a4 4 0 0 1 8 0v3" /></svg>
      </span>
      <div>
        <h2 id="settings-password-title">Segurança</h2>
        <p>Troque sua senha. Os outros acessos abertos com a senha antiga serão encerrados.</p>
      </div>
    </header>

    <form class="settings-form" @submit.prevent="submit">
      <label class="settings-field">
        Senha atual
        <span class="settings-password-wrap">
          <input
            v-model="currentPassword"
            :type="showPasswords ? 'text' : 'password'"
            autocomplete="current-password"
            required
            :maxlength="MAX_PASSWORD_LENGTH"
          >
          <button
            class="settings-password-toggle"
            type="button"
            :aria-pressed="showPasswords"
            @click="showPasswords = !showPasswords"
          >
            {{ showPasswords ? 'Ocultar' : 'Mostrar' }}
          </button>
        </span>
      </label>

      <label class="settings-field">
        Nova senha
        <input
          v-model="newPassword"
          :type="showPasswords ? 'text' : 'password'"
          autocomplete="new-password"
          required
          :minlength="MIN_PASSWORD_LENGTH"
          :maxlength="MAX_PASSWORD_LENGTH"
          :aria-invalid="newPasswordInvalid"
          aria-describedby="settings-new-password-hint"
        >
        <span id="settings-new-password-hint" :class="['settings-hint', { 'is-error': newPasswordInvalid }]">
          {{ newPasswordTooLong
            ? 'Senha longa demais. Letras acentuadas ocupam mais espaço; use menos caracteres.'
            : `Use de ${MIN_PASSWORD_LENGTH} a ${MAX_PASSWORD_LENGTH} caracteres.` }}
        </span>
      </label>

      <label class="settings-field">
        Confirmar nova senha
        <input
          v-model="confirmPassword"
          :type="showPasswords ? 'text' : 'password'"
          autocomplete="new-password"
          required
          :maxlength="MAX_PASSWORD_LENGTH"
          :aria-invalid="confirmationMismatch"
          aria-describedby="settings-confirm-password-hint"
        >
        <span
          id="settings-confirm-password-hint"
          :class="['settings-hint', { 'is-error': confirmationMismatch }]"
          aria-live="polite"
        >
          {{ confirmationMismatch ? 'As senhas informadas não são iguais.' : 'Digite a nova senha mais uma vez.' }}
        </span>
      </label>

      <p class="settings-meta">
        {{ lastChange ? `Última troca de senha em ${lastChange}.` : 'Você ainda não trocou a senha desde o cadastro.' }}
      </p>

      <div class="settings-actions">
        <button class="settings-button is-primary" type="submit" :disabled="!canSubmit">
          {{ saving ? 'Alterando…' : 'Alterar senha' }}
        </button>
      </div>
    </form>
  </section>
</template>
