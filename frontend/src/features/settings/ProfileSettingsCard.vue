<script setup>
import { computed, ref, watch } from 'vue'
import AvatarSettingsField from './AvatarSettingsField.vue'
import { formatLongDate } from './settingsApi'

const props = defineProps({
  api: { type: Object, required: true },
  profile: { type: Object, required: true },
})

const emit = defineEmits(['saved', 'avatar-saved', 'failed'])

const name = ref('')
const email = ref('')
const currentPassword = ref('')
const saving = ref(false)

function reset() {
  name.value = props.profile.name
  email.value = props.profile.email
  currentPassword.value = ''
}

watch(() => props.profile, reset, { immediate: true })

// O backend compara o e-mail já normalizado; a tela faz o mesmo para só pedir a senha quando precisa.
const emailChanged = computed(() => email.value.trim().toLowerCase() !== props.profile.email)
const hasChanges = computed(() => name.value.trim() !== props.profile.name || emailChanged.value)
const memberSince = computed(() => formatLongDate(props.profile.createdAt))

async function submit() {
  if (!hasChanges.value || saving.value) return

  saving.value = true

  try {
    const updatedProfile = await props.api.updateProfile({
      name: name.value,
      email: email.value,
      currentPassword: emailChanged.value ? currentPassword.value : null,
    })
    emit('saved', updatedProfile)
  } catch (error) {
    emit('failed', error)
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <section class="settings-card" aria-labelledby="settings-profile-title">
    <header class="settings-card-header">
      <span class="settings-card-icon" aria-hidden="true">
        <svg viewBox="0 0 24 24"><circle cx="12" cy="8" r="4" /><path d="M4 21a8 8 0 0 1 16 0" /></svg>
      </span>
      <div>
        <h2 id="settings-profile-title">Perfil</h2>
        <p>Nome e e-mail usados para identificar e acessar sua conta.</p>
      </div>
    </header>

    <AvatarSettingsField
      :api="api"
      :name="profile.name"
      @saved="emit('avatar-saved', $event)"
      @failed="emit('failed', $event)"
    />

    <form class="settings-form" @submit.prevent="submit">
      <label class="settings-field">
        Nome completo
        <input v-model="name" type="text" autocomplete="name" required maxlength="100">
      </label>

      <label class="settings-field">
        E-mail
        <input v-model="email" type="email" autocomplete="email" required maxlength="150">
      </label>

      <label v-if="emailChanged" class="settings-field">
        Senha atual
        <input
          v-model="currentPassword"
          type="password"
          autocomplete="current-password"
          required
          maxlength="72"
          aria-describedby="settings-profile-password-hint"
        >
        <span id="settings-profile-password-hint" class="settings-hint">
          Por segurança, confirme sua senha para trocar o e-mail de acesso.
        </span>
      </label>

      <p v-if="memberSince" class="settings-meta">Conta criada em {{ memberSince }}.</p>

      <div class="settings-actions">
        <button class="settings-button is-secondary" type="button" :disabled="!hasChanges || saving" @click="reset">
          Descartar
        </button>
        <button class="settings-button is-primary" type="submit" :disabled="!hasChanges || saving">
          {{ saving ? 'Salvando…' : 'Salvar perfil' }}
        </button>
      </div>
    </form>
  </section>
</template>
