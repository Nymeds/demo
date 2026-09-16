<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import AppToast from '../../components/ui/AppToast.vue'
import DeleteAccountCard from './DeleteAccountCard.vue'
import PasswordSettingsCard from './PasswordSettingsCard.vue'
import PreferencesSettingsCard from './PreferencesSettingsCard.vue'
import { createSettingsApi, SessionExpiredError } from './settingsApi'
import './settings.css'

const TOAST_DURATION_MS = 4500

const { accessToken } = defineProps({
  accessToken: { type: String, required: true },
})

const emit = defineEmits([
  'token-refreshed',
  'preferences-updated',
  'account-deleted',
  'session-expired',
])

const api = createSettingsApi(() => accessToken)
const loading = ref(true)
const loadError = ref('')
const profile = ref(null)
const preferences = ref(null)
const toast = ref({ message: '', type: 'success' })
let toastTimer = null

function showToast(message, type = 'success') {
  clearTimeout(toastTimer)
  toast.value = { message, type }
  toastTimer = setTimeout(closeToast, TOAST_DURATION_MS)
}

function closeToast() {
  toast.value = { message: '', type: toast.value.type }
}

function handleFailure(error) {
  if (error instanceof SessionExpiredError) {
    emit('session-expired')
    return
  }

  showToast(error.message || 'Não foi possível salvar as configurações.', 'error')
}

async function load() {
  loading.value = true
  loadError.value = ''

  try {
    const [loadedProfile, loadedPreferences] = await Promise.all([api.getProfile(), api.getPreferences()])
    profile.value = loadedProfile
    preferences.value = loadedPreferences
  } catch (error) {
    if (error instanceof SessionExpiredError) {
      emit('session-expired')
      return
    }

    loadError.value = error.message || 'Não foi possível carregar as configurações.'
  } finally {
    loading.value = false
  }
}

function onPasswordChanged(authResponse) {
  emit('token-refreshed', authResponse.accessToken)
  profile.value = { ...profile.value, passwordChangedAt: new Date().toISOString() }
  showToast('Senha alterada. Outros dispositivos conectados precisarão entrar novamente.')
}

function onPreferencesSaved(savedPreferences) {
  preferences.value = savedPreferences
  emit('preferences-updated', savedPreferences)
  showToast('Preferências salvas.')
}

onMounted(load)
onBeforeUnmount(() => clearTimeout(toastTimer))
</script>

<template>
  <section class="settings-page" aria-labelledby="settings-title">
    <header class="settings-header">
      <span class="settings-eyebrow">Sua conta</span>
      <h1 id="settings-title">Configurações</h1>
      <p>Gerencie sua segurança, suas preferências e sua conta.</p>
    </header>

    <div v-if="loading" class="settings-status" role="status">
      <h2>Carregando suas configurações…</h2>
      <p>Isso leva só um instante.</p>
    </div>

    <div v-else-if="loadError" class="settings-status" role="alert">
      <h2>Não foi possível carregar as configurações</h2>
      <p>{{ loadError }}</p>
      <button class="settings-button is-primary" type="button" @click="load">Tentar novamente</button>
    </div>

    <div v-else-if="profile && preferences" class="settings-grid">
      <PasswordSettingsCard
        class="is-wide"
        :api="api"
        :password-changed-at="profile.passwordChangedAt"
        @saved="onPasswordChanged"
        @failed="handleFailure"
      />
      <PreferencesSettingsCard
        :api="api"
        :preferences="preferences"
        @saved="onPreferencesSaved"
        @failed="handleFailure"
      />
      <DeleteAccountCard :api="api" @deleted="emit('account-deleted')" @failed="handleFailure" />
    </div>

    <AppToast :message="toast.message" :type="toast.type" @close="closeToast" />
  </section>
</template>
