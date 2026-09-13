<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import AppToast from '../../components/ui/AppToast.vue'

const props = defineProps({
  accessToken: { type: String, required: true },
  user: { type: Object, required: true },
})

const emit = defineEmits(['updated'])

const maximumPhotoSize = 2 * 1024 * 1024
const photoInput = ref(null)
const profile = ref({ ...props.user })
const avatarUrl = ref('')
const loading = ref(true)
const saving = ref(false)
const photoBusy = ref(false)
const pageError = ref('')
const fieldErrors = ref({})
const toast = ref({ message: '', type: 'success' })
const form = reactive({
  name: '',
  username: '',
  email: '',
  phone: '',
  birthDate: '',
  gender: '',
  location: '',
})

let savedForm = ''
let toastTimer

const genderOptions = [
  { value: '', label: 'Não informado' },
  { value: 'FEMALE', label: 'Feminino' },
  { value: 'MALE', label: 'Masculino' },
  { value: 'NON_BINARY', label: 'Não binário' },
  { value: 'OTHER', label: 'Outro' },
  { value: 'PREFER_NOT_TO_SAY', label: 'Prefiro não informar' },
]

const maximumBirthDate = computed(() => {
  const yesterday = new Date()
  yesterday.setDate(yesterday.getDate() - 1)
  return [
    yesterday.getFullYear(),
    String(yesterday.getMonth() + 1).padStart(2, '0'),
    String(yesterday.getDate()).padStart(2, '0'),
  ].join('-')
})

const userInitials = computed(() => {
  const parts = (profile.value.name || props.user.name || 'Usuário')
    .trim()
    .split(/\s+/)
    .filter(Boolean)

  return parts
    .slice(0, 2)
    .map(part => part.charAt(0).toUpperCase())
    .join('') || 'U'
})

const isDirty = computed(() => formSnapshot() !== savedForm)
const hasProfilePhoto = computed(() => Boolean(profile.value.hasProfilePhoto))
const updatedAtLabel = computed(() => {
  if (!profile.value.updatedAt) return 'Ainda não atualizado'

  return `Atualizado em ${new Intl.DateTimeFormat('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(profile.value.updatedAt))}`
})

function formSnapshot() {
  return JSON.stringify({
    name: form.name,
    username: form.username,
    email: form.email,
    phone: form.phone,
    birthDate: form.birthDate,
    gender: form.gender,
    location: form.location,
  })
}

function fillForm(userProfile) {
  form.name = userProfile.name ?? ''
  form.username = userProfile.username ?? ''
  form.email = userProfile.email ?? ''
  form.phone = userProfile.phone ?? ''
  form.birthDate = userProfile.birthDate ?? ''
  form.gender = userProfile.gender ?? ''
  form.location = userProfile.location ?? ''
  savedForm = formSnapshot()
  fieldErrors.value = {}
}

function showToast(message, type = 'success') {
  toast.value = { message, type }
  clearTimeout(toastTimer)
  toastTimer = setTimeout(() => {
    toast.value.message = ''
  }, 4500)
}

function closeToast() {
  clearTimeout(toastTimer)
  toast.value.message = ''
}

async function apiRequest(path, options = {}) {
  const isFormData = options.body instanceof FormData
  const response = await fetch(path, {
    ...options,
    headers: {
      Authorization: `Bearer ${props.accessToken}`,
      ...(!isFormData && options.body ? { 'Content-Type': 'application/json' } : {}),
      ...options.headers,
    },
  })

  const data = response.status === 204
    ? null
    : await response.json().catch(() => ({}))

  if (!response.ok) {
    const error = new Error(data.detail || data.message || 'Não foi possível concluir a solicitação.')
    error.fieldErrors = data.errors && typeof data.errors === 'object' ? data.errors : {}
    throw error
  }

  return data
}

function clearAvatarUrl() {
  if (avatarUrl.value) URL.revokeObjectURL(avatarUrl.value)
  avatarUrl.value = ''
}

async function loadAvatar(userProfile = profile.value) {
  clearAvatarUrl()
  if (!userProfile.hasProfilePhoto || !userProfile.profilePhotoUrl) return

  try {
    const response = await fetch(userProfile.profilePhotoUrl, {
      headers: { Authorization: `Bearer ${props.accessToken}` },
      cache: 'no-store',
    })

    if (!response.ok) throw new Error('Não foi possível carregar a foto.')
    avatarUrl.value = URL.createObjectURL(await response.blob())
  } catch {
    showToast('A foto de perfil não pôde ser carregada.', 'error')
  }
}

async function loadProfile() {
  loading.value = true
  pageError.value = ''

  try {
    const loadedProfile = await apiRequest('/api/v1/users/me')
    profile.value = loadedProfile
    fillForm(loadedProfile)
    await loadAvatar(loadedProfile)
    emit('updated', loadedProfile)
  } catch (error) {
    pageError.value = error.message || 'Não foi possível carregar o perfil.'
  } finally {
    loading.value = false
  }
}

async function saveProfile() {
  saving.value = true
  fieldErrors.value = {}

  try {
    const updatedProfile = await apiRequest('/api/v1/users/me', {
      method: 'PUT',
      body: JSON.stringify({
        name: form.name,
        username: form.username || null,
        email: form.email,
        phone: form.phone || null,
        birthDate: form.birthDate || null,
        gender: form.gender || null,
        location: form.location || null,
      }),
    })

    profile.value = updatedProfile
    fillForm(updatedProfile)
    emit('updated', updatedProfile)
    showToast('Perfil atualizado com sucesso.')
  } catch (error) {
    fieldErrors.value = error.fieldErrors || {}
    showToast(error.message || 'Não foi possível atualizar o perfil.', 'error')
  } finally {
    saving.value = false
  }
}

function cancelChanges() {
  fillForm(profile.value)
  showToast('Alterações descartadas.')
}

function choosePhoto() {
  if (!photoBusy.value) photoInput.value?.click()
}

async function uploadPhoto(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return

  if (file.size > maximumPhotoSize) {
    showToast('A foto deve ter no máximo 2 MB.', 'error')
    return
  }

  if (file.type && !['image/png', 'image/jpeg'].includes(file.type)) {
    showToast('Escolha uma imagem PNG ou JPG.', 'error')
    return
  }

  photoBusy.value = true
  try {
    const body = new FormData()
    body.append('file', file)
    const updatedProfile = await apiRequest('/api/v1/users/me/profile-photo', {
      method: 'PUT',
      body,
    })

    profile.value = updatedProfile
    await loadAvatar(updatedProfile)
    emit('updated', updatedProfile)
    showToast('Foto de perfil atualizada.')
  } catch (error) {
    showToast(error.message || 'Não foi possível atualizar a foto.', 'error')
  } finally {
    photoBusy.value = false
  }
}

async function removePhoto() {
  if (!hasProfilePhoto.value || photoBusy.value) return

  photoBusy.value = true
  try {
    await apiRequest('/api/v1/users/me/profile-photo', { method: 'DELETE' })
    const updatedProfile = {
      ...profile.value,
      hasProfilePhoto: false,
      profilePhotoUrl: null,
      updatedAt: new Date().toISOString(),
    }
    profile.value = updatedProfile
    clearAvatarUrl()
    emit('updated', updatedProfile)
    showToast('Foto de perfil removida.')
  } catch (error) {
    showToast(error.message || 'Não foi possível remover a foto.', 'error')
  } finally {
    photoBusy.value = false
  }
}

onMounted(loadProfile)
onBeforeUnmount(() => {
  clearTimeout(toastTimer)
  clearAvatarUrl()
})
</script>

<template>
  <section class="profile-page" aria-labelledby="profile-title">
    <AppToast :message="toast.message" :type="toast.type" @close="closeToast" />

    <header class="profile-header">
      <span class="profile-title-icon" aria-hidden="true">
        <svg viewBox="0 0 24 24">
          <circle cx="12" cy="8" r="4" />
          <path d="M4 21a8 8 0 0 1 16 0" />
        </svg>
      </span>
      <div>
        <h1 id="profile-title">Perfil</h1>
        <p>Edite seus dados pessoais e mantenha sua conta atualizada.</p>
      </div>
    </header>

    <article v-if="loading" class="profile-state-card" aria-live="polite">
      <span class="profile-spinner" aria-hidden="true"></span>
      <p>Carregando seu perfil...</p>
    </article>

    <article v-else-if="pageError" class="profile-state-card is-error" role="alert">
      <span aria-hidden="true">!</span>
      <div>
        <h2>Não foi possível carregar o perfil</h2>
        <p>{{ pageError }}</p>
        <button type="button" @click="loadProfile">Tentar novamente</button>
      </div>
    </article>

    <div v-else class="profile-layout">
      <form class="profile-card" @submit.prevent="saveProfile">
        <div class="profile-card-heading">
          <div>
            <h2>Dados pessoais</h2>
            <p>Atualize as informações usadas na sua conta.</p>
          </div>
          <small>{{ updatedAtLabel }}</small>
        </div>

        <section class="profile-photo-section" aria-labelledby="profile-photo-title">
          <span class="profile-avatar">
            <img v-if="avatarUrl" :src="avatarUrl" :alt="`Foto de perfil de ${profile.name}`">
            <strong v-else aria-hidden="true">{{ userInitials }}</strong>
            <span class="profile-camera" aria-hidden="true">
              <svg viewBox="0 0 24 24"><path d="M4 7h4l2-3h4l2 3h4v12H4V7Z" /><circle cx="12" cy="13" r="4" /></svg>
            </span>
          </span>

          <div class="profile-photo-copy">
            <h3 id="profile-photo-title">Foto de perfil</h3>
            <p>Use uma imagem PNG ou JPG de até 2 MB.</p>
            <div class="profile-photo-actions">
              <input
                ref="photoInput"
                class="sr-only"
                type="file"
                accept=".png,.jpg,.jpeg,image/png,image/jpeg"
                @change="uploadPhoto"
              >
              <button class="profile-outline-button" type="button" :disabled="photoBusy" @click="choosePhoto">
                <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 16V4m0 0L7 9m5-5 5 5" /><path d="M5 14v5h14v-5" /></svg>
                {{ photoBusy ? 'Processando...' : hasProfilePhoto ? 'Alterar foto' : 'Adicionar foto' }}
              </button>
              <button
                v-if="hasProfilePhoto"
                class="profile-remove-photo"
                type="button"
                :disabled="photoBusy"
                @click="removePhoto"
              >
                Remover
              </button>
            </div>
          </div>
        </section>

        <div class="profile-divider"></div>

        <div class="profile-fields">
          <label>
            <span>Nome completo</span>
            <input
              v-model="form.name"
              type="text"
              autocomplete="name"
              maxlength="100"
              required
              :aria-invalid="Boolean(fieldErrors.name)"
            >
            <small v-if="fieldErrors.name" class="profile-field-error">{{ fieldErrors.name }}</small>
          </label>

          <label>
            <span>Nome de usuário <small>(opcional)</small></span>
            <input
              v-model="form.username"
              type="text"
              autocomplete="username"
              minlength="3"
              maxlength="30"
              pattern="[A-Za-z0-9][A-Za-z0-9._]{1,28}[A-Za-z0-9]"
              placeholder="ex.: gabrielsilva"
              :aria-invalid="Boolean(fieldErrors.username)"
            >
            <small v-if="fieldErrors.username" class="profile-field-error">{{ fieldErrors.username }}</small>
          </label>

          <label>
            <span>E-mail</span>
            <input
              v-model="form.email"
              type="email"
              autocomplete="email"
              maxlength="150"
              required
              :aria-invalid="Boolean(fieldErrors.email)"
            >
            <small v-if="fieldErrors.email" class="profile-field-error">{{ fieldErrors.email }}</small>
          </label>

          <label>
            <span>Telefone <small>(opcional)</small></span>
            <input
              v-model="form.phone"
              type="tel"
              inputmode="tel"
              autocomplete="tel"
              maxlength="20"
              placeholder="(00) 00000-0000"
              :aria-invalid="Boolean(fieldErrors.phone)"
            >
            <small v-if="fieldErrors.phone" class="profile-field-error">{{ fieldErrors.phone }}</small>
          </label>

          <label>
            <span>Data de nascimento <small>(opcional)</small></span>
            <input
              v-model="form.birthDate"
              type="date"
              autocomplete="bday"
              :max="maximumBirthDate"
              :aria-invalid="Boolean(fieldErrors.birthDate)"
            >
            <small v-if="fieldErrors.birthDate" class="profile-field-error">{{ fieldErrors.birthDate }}</small>
          </label>

          <label>
            <span>Gênero <small>(opcional)</small></span>
            <select v-model="form.gender" :aria-invalid="Boolean(fieldErrors.gender)">
              <option v-for="option in genderOptions" :key="option.value" :value="option.value">
                {{ option.label }}
              </option>
            </select>
            <small v-if="fieldErrors.gender" class="profile-field-error">{{ fieldErrors.gender }}</small>
          </label>

          <label class="profile-location-field">
            <span>Localização <small>(opcional)</small></span>
            <input
              v-model="form.location"
              type="text"
              autocomplete="address-level2"
              maxlength="120"
              placeholder="Cidade - Estado"
              :aria-invalid="Boolean(fieldErrors.location)"
            >
            <small v-if="fieldErrors.location" class="profile-field-error">{{ fieldErrors.location }}</small>
          </label>
        </div>

        <footer class="profile-form-actions">
          <button class="profile-cancel-button" type="button" :disabled="saving || !isDirty" @click="cancelChanges">
            Cancelar
          </button>
          <button class="profile-save-button" type="submit" :disabled="saving || !isDirty">
            <span v-if="saving" class="profile-button-spinner" aria-hidden="true"></span>
            {{ saving ? 'Salvando...' : 'Salvar alterações' }}
          </button>
        </footer>
      </form>

      <aside class="profile-side-column">
        <article class="profile-info-card">
          <span class="profile-info-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24"><path d="M12 3 5 6v5c0 4.6 2.9 8 7 10 4.1-2 7-5.4 7-10V6l-7-3Z" /><path d="m9 12 2 2 4-5" /></svg>
          </span>
          <div>
            <h2>Seus dados estão seguros</h2>
            <p>Suas informações são protegidas e utilizadas apenas para personalizar sua experiência na plataforma.</p>
          </div>
        </article>

      </aside>
    </div>
  </section>
</template>

<style scoped>
.profile-page { margin: 0 auto; max-width: 1380px; width: 100%; }
.profile-header { align-items: center; display: flex; gap: 14px; margin-bottom: 24px; }
.profile-title-icon { align-items: center; background: #ece7ff; border-radius: 11px; color: #6231e6; display: flex; flex: 0 0 46px; height: 46px; justify-content: center; }
.profile-title-icon svg { fill: none; height: 25px; stroke: currentColor; stroke-linecap: round; stroke-linejoin: round; stroke-width: 1.9; width: 25px; }
.profile-header h1 { color: #14192c; font-size: clamp(1.65rem, 2.4vw, 2rem); letter-spacing: -.04em; margin: 0; }
.profile-header p { color: #687087; font-size: .82rem; margin: 4px 0 0; }
.profile-layout { align-items: start; display: grid; gap: 22px; grid-template-columns: minmax(0, 1fr) minmax(280px, 330px); }
.profile-card,
.profile-info-card,
.profile-state-card { background: #fff; border: 1px solid #e6e7ef; border-radius: 14px; box-shadow: 0 8px 28px rgba(29, 35, 63, .045); }
.profile-card { padding: clamp(24px, 3vw, 34px); }
.profile-card-heading { align-items: flex-start; display: flex; gap: 20px; justify-content: space-between; }
.profile-card-heading h2 { color: #171c30; font-size: 1.05rem; margin: 0 0 6px; }
.profile-card-heading p { color: #6f7689; font-size: .76rem; margin: 0; }
.profile-card-heading > small { color: #969bad; flex: 0 0 auto; font-size: .62rem; padding-top: 4px; }
.profile-photo-section { align-items: center; display: flex; gap: 25px; padding: 25px 0 4px; }
.profile-avatar { align-items: center; background: linear-gradient(145deg, #6b35ec, #4c16d7); border-radius: 50%; color: #fff; display: flex; flex: 0 0 112px; height: 112px; justify-content: center; position: relative; }
.profile-avatar > strong { font-size: 2.35rem; font-weight: 650; letter-spacing: -.05em; }
.profile-avatar > img { border-radius: inherit; height: 100%; object-fit: cover; width: 100%; }
.profile-camera { align-items: center; background: #fff; border: 1px solid #e3e5ec; border-radius: 50%; bottom: -1px; box-shadow: 0 4px 12px rgba(24, 29, 49, .12); color: #46506a; display: flex; height: 35px; justify-content: center; position: absolute; right: -1px; width: 35px; }
.profile-camera svg { fill: none; height: 17px; stroke: currentColor; stroke-linecap: round; stroke-linejoin: round; stroke-width: 1.8; width: 17px; }
.profile-photo-copy h3 { color: #1b2033; font-size: .86rem; margin: 0 0 5px; }
.profile-photo-copy p { color: #7b8193; font-size: .7rem; margin: 0 0 13px; }
.profile-photo-actions { align-items: center; display: flex; flex-wrap: wrap; gap: 10px; }
.profile-outline-button,
.profile-remove-photo { align-items: center; background: #fff; border: 1px solid #d9d2ee; border-radius: 8px; color: #5f2dd9; cursor: pointer; display: inline-flex; font-size: .72rem; font-weight: 750; gap: 7px; padding: 9px 12px; }
.profile-outline-button svg { fill: none; height: 16px; stroke: currentColor; stroke-linecap: round; stroke-linejoin: round; stroke-width: 1.8; width: 16px; }
.profile-outline-button:hover { background: #f7f4ff; border-color: #bba9ea; }
.profile-remove-photo { border-color: #f0d6d2; color: #c3483e; }
.profile-remove-photo:hover { background: #fff5f4; }
.profile-divider { background: #eceef3; height: 1px; margin: 28px 0; }
.profile-fields { display: grid; gap: 21px 24px; grid-template-columns: repeat(2, minmax(0, 1fr)); }
.profile-fields label { display: grid; gap: 7px; min-width: 0; }
.profile-fields label > span { color: #35405b; font-size: .72rem; font-weight: 700; }
.profile-fields label > span small { color: #8b91a2; font-size: .62rem; font-weight: 500; }
.profile-fields input,
.profile-fields select { background: #fff; border: 1px solid #dfe2ea; border-radius: 8px; color: #20263a; font-size: .78rem; height: 43px; outline: none; padding: 0 12px; transition: border-color .18s, box-shadow .18s; width: 100%; }
.profile-fields input::placeholder { color: #a5a9b5; }
.profile-fields input:hover,
.profile-fields select:hover { border-color: #c9c3dc; }
.profile-fields input:focus,
.profile-fields select:focus { border-color: #7544e4; box-shadow: 0 0 0 3px rgba(105, 57, 222, .11); }
.profile-fields input[aria-invalid='true'],
.profile-fields select[aria-invalid='true'] { border-color: #d95a50; }
.profile-location-field { grid-column: 1 / -1; }
.profile-field-error { color: #bd4038; font-size: .61rem; line-height: 1.3; }
.profile-form-actions { align-items: center; border-top: 1px solid #eceef3; display: flex; gap: 11px; justify-content: flex-end; margin-top: 30px; padding-top: 23px; }
.profile-cancel-button,
.profile-save-button { border-radius: 8px; cursor: pointer; font-size: .75rem; font-weight: 750; min-height: 41px; padding: 10px 17px; }
.profile-cancel-button { background: #fff; border: 1px solid #dfe1e8; color: #31384c; }
.profile-save-button { align-items: center; background: linear-gradient(100deg, #5d22df, #741dff); border: 0; box-shadow: 0 8px 18px rgba(99, 39, 222, .2); color: #fff; display: inline-flex; gap: 8px; justify-content: center; min-width: 138px; }
.profile-cancel-button:hover:not(:disabled) { background: #f8f8fb; }
.profile-save-button:hover:not(:disabled) { box-shadow: 0 11px 23px rgba(99, 39, 222, .28); transform: translateY(-1px); }
button:disabled { cursor: not-allowed; opacity: .55; }
.profile-button-spinner,
.profile-spinner { animation: profile-spin .7s linear infinite; border: 2px solid currentColor; border-right-color: transparent; border-radius: 50%; display: inline-block; height: 15px; width: 15px; }
.profile-side-column { display: grid; gap: 16px; }
.profile-info-card { align-items: flex-start; display: flex; gap: 15px; padding: 25px 22px; }
.profile-info-icon { align-items: center; background: #efe9ff; border-radius: 50%; color: #6735e1; display: flex; flex: 0 0 45px; height: 45px; justify-content: center; }
.profile-info-icon svg { fill: none; height: 22px; stroke: currentColor; stroke-linecap: round; stroke-linejoin: round; stroke-width: 1.8; width: 22px; }
.profile-info-card h2 { color: #1d2235; font-size: .82rem; line-height: 1.35; margin: 2px 0 8px; }
.profile-info-card p { color: #697187; font-size: .7rem; line-height: 1.65; margin: 0; }
.profile-state-card { align-items: center; display: flex; gap: 13px; justify-content: center; min-height: 230px; padding: 30px; }
.profile-state-card p { font-size: .78rem; margin: 0; }
.profile-state-card.is-error { justify-content: flex-start; }
.profile-state-card.is-error > span { align-items: center; background: #fff0ef; border-radius: 50%; color: #c4433b; display: flex; flex: 0 0 42px; font-weight: 900; height: 42px; justify-content: center; }
.profile-state-card h2 { color: #292f43; font-size: .9rem; margin: 0 0 5px; }
.profile-state-card.is-error button { background: #6832df; border: 0; border-radius: 7px; color: #fff; cursor: pointer; font-size: .68rem; margin-top: 12px; padding: 8px 11px; }
.sr-only { height: 1px; margin: -1px; overflow: hidden; padding: 0; position: absolute; width: 1px; clip: rect(0, 0, 0, 0); white-space: nowrap; }
@keyframes profile-spin { to { transform: rotate(360deg); } }
@media (max-width: 1040px) {
  .profile-layout { grid-template-columns: 1fr; }
  .profile-side-column { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media (max-width: 680px) {
  .profile-header { align-items: flex-start; }
  .profile-header p { line-height: 1.5; }
  .profile-card { padding: 22px 18px; }
  .profile-card-heading { display: block; }
  .profile-card-heading > small { display: block; margin-top: 8px; }
  .profile-photo-section { align-items: flex-start; }
  .profile-avatar { flex-basis: 88px; height: 88px; }
  .profile-avatar > strong { font-size: 1.8rem; }
  .profile-fields,
  .profile-side-column { grid-template-columns: 1fr; }
  .profile-location-field { grid-column: auto; }
}
@media (max-width: 460px) {
  .profile-photo-section { flex-direction: column; }
  .profile-form-actions { align-items: stretch; flex-direction: column-reverse; }
  .profile-cancel-button,
  .profile-save-button { width: 100%; }
}
</style>
