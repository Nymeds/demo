<script setup>
import { computed, nextTick, ref } from 'vue'
import { useAvatar } from '../../composables/useAvatar'
import AvatarCropModal from './AvatarCropModal.vue'

// Limite só para abrir no recorte; o que vai para a API é a versão recortada, bem menor.
const MAX_SOURCE_BYTES = 15 * 1024 * 1024
const ACCEPTED_TYPES = Object.freeze(['image/png', 'image/jpeg', 'image/webp'])

const props = defineProps({
  api: { type: Object, required: true },
  name: { type: String, required: true },
})

const emit = defineEmits(['saved', 'failed'])

const { avatarUrl, showAvatar, clearAvatar } = useAvatar()
const fileInput = ref(null)
const chooseButton = ref(null)
const selectedFile = ref(null)
const saving = ref(false)
const removing = ref(false)

const initial = computed(() => props.name.trim().charAt(0).toUpperCase() || '?')
const busy = computed(() => saving.value || removing.value)

function chooseFile() {
  fileInput.value?.click()
}

function onFileChosen(event) {
  const [file] = event.target.files
  // Limpa a seleção para o mesmo arquivo poder ser escolhido de novo depois.
  event.target.value = ''

  if (!file) return

  if (!ACCEPTED_TYPES.includes(file.type)) {
    emit('failed', new Error('Formato não suportado. Escolha uma imagem PNG, JPG ou WebP.'))
    return
  }

  if (file.size > MAX_SOURCE_BYTES) {
    emit('failed', new Error('A imagem escolhida é muito grande. Use uma de até 15 MB.'))
    return
  }

  selectedFile.value = file
}

async function closeCrop() {
  selectedFile.value = null
  await nextTick()
  chooseButton.value?.focus()
}

async function onCropFailed(error) {
  await closeCrop()
  emit('failed', error)
}

async function saveCroppedImage(image) {
  saving.value = true

  try {
    await props.api.uploadAvatar(image)
    showAvatar(image)
    await closeCrop()
    emit('saved', 'Foto de perfil atualizada.')
  } catch (error) {
    emit('failed', error)
  } finally {
    saving.value = false
  }
}

async function removeAvatar() {
  removing.value = true

  try {
    await props.api.deleteAvatar()
    clearAvatar()
    emit('saved', 'Foto de perfil removida.')
  } catch (error) {
    emit('failed', error)
  } finally {
    removing.value = false
  }
}
</script>

<template>
  <div class="avatar-field">
    <span class="avatar-field-preview" aria-hidden="true">
      <img v-if="avatarUrl" :src="avatarUrl" alt="">
      <template v-else>{{ initial }}</template>
    </span>

    <div class="avatar-field-text">
      <strong>Foto de perfil</strong>
      <span>PNG, JPG ou WebP. Você ajusta o enquadramento antes de salvar.</span>
      <div class="avatar-field-actions">
        <button ref="chooseButton" class="settings-button is-secondary" type="button" :disabled="busy" @click="chooseFile">
          {{ avatarUrl ? 'Trocar foto' : 'Escolher foto' }}
        </button>
        <button v-if="avatarUrl" class="avatar-field-remove" type="button" :disabled="busy" @click="removeAvatar">
          {{ removing ? 'Removendo…' : 'Remover foto' }}
        </button>
      </div>
    </div>

    <input
      ref="fileInput"
      class="avatar-field-input"
      type="file"
      :accept="ACCEPTED_TYPES.join(',')"
      tabindex="-1"
      aria-hidden="true"
      @change="onFileChosen"
    >

    <AvatarCropModal
      v-if="selectedFile"
      :file="selectedFile"
      :name="name"
      :saving="saving"
      @close="closeCrop"
      @confirm="saveCroppedImage"
      @failed="onCropFailed"
    />
  </div>
</template>

<style scoped>
.avatar-field { align-items: center; border-bottom: 1px solid #eff0f5; display: flex; gap: 16px; margin-bottom: 16px; padding-bottom: 18px; }
.avatar-field-preview { align-items: center; background: linear-gradient(135deg, #7749f7, #5320da); border-radius: 50%; color: #fff; display: flex; flex: 0 0 72px; font-size: 1.5rem; font-weight: 800; height: 72px; justify-content: center; overflow: hidden; }
.avatar-field-preview img { display: block; height: 100%; max-width: none; object-fit: cover; width: 100%; }
.avatar-field-text { display: grid; gap: 3px; min-width: 0; }
.avatar-field-text strong { color: #30364a; font-size: .76rem; }
.avatar-field-text > span { color: #7b8192; font-size: .66rem; line-height: 1.45; }
.avatar-field-actions { align-items: center; display: flex; flex-wrap: wrap; gap: 6px 12px; margin-top: 8px; }
.avatar-field-remove { background: transparent; border: 0; border-radius: 6px; color: #c4463e; cursor: pointer; font-size: .7rem; font-weight: 750; padding: 6px 4px; }
.avatar-field-remove:disabled { cursor: not-allowed; opacity: .55; }
.avatar-field-remove:focus-visible { outline: 3px solid rgba(105, 54, 224, .28); outline-offset: 2px; }
.avatar-field-input { display: none; }
</style>
