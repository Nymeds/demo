<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'

const VIEWPORT = 240 // lado da área de recorte, em pixels de tela (cabe numa tela de 320 px)
const OUTPUT_SIZE = 256 // foto compacta e padronizada para o perfil e a barra lateral
const JPEG_QUALITY = 0.9
const MAX_ZOOM = 4
const ZOOM_STEP = 0.1
const KEYBOARD_NUDGE = 10
const SIDEBAR_AVATAR = 36 // mesmo tamanho da caixa do usuário na barra lateral
const PROFILE_AVATAR = 72 // prévia compacta da foto no perfil

const props = defineProps({
  file: { type: File, required: true },
  name: { type: String, required: true },
  saving: { type: Boolean, default: false },
  confirmLabel: { type: String, default: 'Salvar foto' },
})

const emit = defineEmits(['close', 'confirm', 'failed'])

const sourceUrl = URL.createObjectURL(props.file)
const natural = ref(null)
const zoom = ref(1)
const offset = ref({ x: 0, y: 0 })
const dialog = ref(null)
let sourceImage = null
let drag = null

// Com zoom 1 a imagem cobre exatamente a área de recorte pelo lado menor.
const scale = computed(() => (
  natural.value ? (VIEWPORT / Math.min(natural.value.width, natural.value.height)) * zoom.value : 1
))
const displayed = computed(() => (
  natural.value
    ? { width: natural.value.width * scale.value, height: natural.value.height * scale.value }
    : { width: 0, height: 0 }
))

// A imagem nunca pode sair da área de recorte e deixar um canto vazio.
function clampOffset({ x, y }) {
  return {
    x: Math.min(0, Math.max(VIEWPORT - displayed.value.width, x)),
    y: Math.min(0, Math.max(VIEWPORT - displayed.value.height, y)),
  }
}

// O zoom acontece em volta do centro do círculo, não do canto da imagem.
function setZoom(nextZoom) {
  const center = VIEWPORT / 2
  const previousScale = scale.value
  const pointX = (center - offset.value.x) / previousScale
  const pointY = (center - offset.value.y) / previousScale

  zoom.value = Math.min(MAX_ZOOM, Math.max(1, Math.round(nextZoom * 100) / 100))
  offset.value = clampOffset({ x: center - pointX * scale.value, y: center - pointY * scale.value })
}

function startDrag(event) {
  if (!natural.value) return

  drag = { pointerId: event.pointerId, startX: event.clientX, startY: event.clientY, origin: offset.value }
  event.currentTarget.setPointerCapture(event.pointerId)
}

function moveDrag(event) {
  if (!drag || event.pointerId !== drag.pointerId) return

  offset.value = clampOffset({
    x: drag.origin.x + event.clientX - drag.startX,
    y: drag.origin.y + event.clientY - drag.startY,
  })
}

function endDrag(event) {
  if (drag?.pointerId === event.pointerId) drag = null
}

function onWheel(event) {
  if (natural.value) setZoom(zoom.value - Math.sign(event.deltaY) * ZOOM_STEP)
}

const KEYBOARD_MOVES = Object.freeze({
  ArrowLeft: [-KEYBOARD_NUDGE, 0],
  ArrowRight: [KEYBOARD_NUDGE, 0],
  ArrowUp: [0, -KEYBOARD_NUDGE],
  ArrowDown: [0, KEYBOARD_NUDGE],
})

function onViewportKeydown(event) {
  if (!natural.value) return

  const move = KEYBOARD_MOVES[event.key]

  if (move) {
    event.preventDefault()
    offset.value = clampOffset({ x: offset.value.x + move[0], y: offset.value.y + move[1] })
  } else if (event.key === '+' || event.key === '=') {
    event.preventDefault()
    setZoom(zoom.value + ZOOM_STEP)
  } else if (event.key === '-') {
    event.preventDefault()
    setZoom(zoom.value - ZOOM_STEP)
  }
}

// As prévias repetem o mesmo enquadramento, só que na escala de cada lugar onde a foto aparece.
function previewStyle(size) {
  if (!natural.value) return {}

  const ratio = size / VIEWPORT

  return {
    backgroundImage: `url("${sourceUrl}")`,
    backgroundPosition: `${offset.value.x * ratio}px ${offset.value.y * ratio}px`,
    backgroundSize: `${displayed.value.width * ratio}px ${displayed.value.height * ratio}px`,
  }
}

const sidebarPreview = computed(() => previewStyle(SIDEBAR_AVATAR))
const profilePreview = computed(() => previewStyle(PROFILE_AVATAR))

function confirmCrop() {
  if (!sourceImage || props.saving) return

  const canvas = document.createElement('canvas')
  canvas.width = OUTPUT_SIZE
  canvas.height = OUTPUT_SIZE

  const context = canvas.getContext('2d')
  const sourceSide = VIEWPORT / scale.value

  context.fillStyle = '#ffffff'
  context.fillRect(0, 0, OUTPUT_SIZE, OUTPUT_SIZE)
  context.imageSmoothingQuality = 'high'
  context.drawImage(
    sourceImage,
    -offset.value.x / scale.value,
    -offset.value.y / scale.value,
    sourceSide,
    sourceSide,
    0,
    0,
    OUTPUT_SIZE,
    OUTPUT_SIZE,
  )

  canvas.toBlob(image => {
    if (image) emit('confirm', image)
    else emit('failed', new Error('Não foi possível preparar a foto. Tente outra imagem.'))
  }, 'image/jpeg', JPEG_QUALITY)
}

function handleKeydown(event) {
  if (event.key === 'Escape' && !props.saving) {
    emit('close')
    return
  }

  if (event.key !== 'Tab') return

  const focusable = [...dialog.value.querySelectorAll('button:not([disabled]), input:not([disabled]), [tabindex="0"]')]
  if (focusable.length === 0) return

  const first = focusable[0]
  const last = focusable[focusable.length - 1]
  const active = document.activeElement

  if (event.shiftKey && (active === first || active === dialog.value)) {
    event.preventDefault()
    last.focus()
  } else if (!event.shiftKey && active === last) {
    event.preventDefault()
    first.focus()
  }
}

function closeFromBackdrop() {
  if (!props.saving) emit('close')
}

onMounted(() => {
  dialog.value?.focus()

  const image = new Image()
  image.onload = () => {
    sourceImage = image
    natural.value = { width: image.naturalWidth, height: image.naturalHeight }
    offset.value = clampOffset({
      x: (VIEWPORT - displayed.value.width) / 2,
      y: (VIEWPORT - displayed.value.height) / 2,
    })
  }
  image.onerror = () => emit('failed', new Error('Não foi possível abrir esta imagem. Tente outro arquivo.'))
  image.src = sourceUrl
})

onBeforeUnmount(() => URL.revokeObjectURL(sourceUrl))
</script>

<template>
  <div class="crop-backdrop" @mousedown.self="closeFromBackdrop">
    <section
      ref="dialog"
      class="crop-modal"
      role="dialog"
      aria-modal="true"
      aria-labelledby="avatar-crop-title"
      aria-describedby="avatar-crop-help"
      tabindex="-1"
      @keydown="handleKeydown"
    >
      <header class="crop-header">
        <div>
          <h2 id="avatar-crop-title">Ajustar foto de perfil</h2>
          <p id="avatar-crop-help">Arraste a imagem para enquadrar e use o zoom. O que ficar dentro do círculo será a sua foto.</p>
        </div>
        <button class="crop-close" type="button" aria-label="Cancelar e fechar" :disabled="saving" @click="emit('close')">×</button>
      </header>

      <div class="crop-body">
        <div
          class="crop-viewport"
          :style="{ width: `${VIEWPORT}px`, height: `${VIEWPORT}px` }"
          tabindex="0"
          role="group"
          aria-label="Área de recorte. Use as setas para mover a imagem e as teclas + e − para o zoom."
          @pointerdown="startDrag"
          @pointermove="moveDrag"
          @pointerup="endDrag"
          @pointercancel="endDrag"
          @wheel.prevent="onWheel"
          @keydown="onViewportKeydown"
        >
          <img
            v-if="natural"
            class="crop-image"
            :src="sourceUrl"
            alt=""
            draggable="false"
            :style="{
              width: `${displayed.width}px`,
              height: `${displayed.height}px`,
              transform: `translate(${offset.x}px, ${offset.y}px)`,
            }"
          >
          <span v-else class="crop-loading">Carregando imagem…</span>
          <span class="crop-mask" aria-hidden="true"></span>
        </div>

        <aside class="crop-previews" aria-label="Pré-visualização da foto">
          <p class="crop-previews-title">Como vai ficar</p>

          <div class="crop-sidebar-preview">
            <span class="crop-avatar is-small" :style="sidebarPreview"></span>
            <span class="crop-sidebar-text">
              <strong>{{ name }}</strong>
              <small>Usuário conectado</small>
            </span>
          </div>
          <small class="crop-preview-caption">Na barra lateral</small>

          <span class="crop-avatar is-large" :style="profilePreview"></span>
          <small class="crop-preview-caption">Em Configurações</small>
        </aside>
      </div>

      <label class="crop-zoom">
        <span>Zoom</span>
        <input
          type="range"
          min="1"
          :max="MAX_ZOOM"
          :step="ZOOM_STEP"
          :value="zoom"
          :disabled="!natural"
          @input="setZoom(Number($event.target.value))"
        >
        <output>{{ Math.round(zoom * 100) }}%</output>
      </label>

      <footer class="crop-actions">
        <button class="settings-button is-secondary" type="button" :disabled="saving" @click="emit('close')">Cancelar</button>
        <button class="settings-button is-primary" type="button" :disabled="!natural || saving" @click="confirmCrop">
          {{ saving ? 'Salvando…' : confirmLabel }}
        </button>
      </footer>
    </section>
  </div>
</template>

<style scoped>
.crop-backdrop { align-items: center; background: rgba(16, 20, 34, .58); display: flex; inset: 0; justify-content: center; padding: 20px; position: fixed; z-index: 130; }
.crop-modal { background: #fff; border-radius: 16px; box-shadow: 0 24px 70px rgba(15, 18, 35, .28); display: grid; gap: 18px; max-height: calc(100svh - 40px); max-width: 540px; overflow-y: auto; padding: 22px 24px; width: 100%; }
.crop-modal:focus { outline: none; }

.crop-header { align-items: flex-start; display: flex; gap: 12px; justify-content: space-between; }
.crop-header h2 { color: #171c30; font-size: 1.1rem; font-weight: 800; letter-spacing: -.02em; margin: 0 0 4px; }
.crop-header p { color: #6c7287; font-size: .74rem; line-height: 1.5; margin: 0; }
.crop-close { background: transparent; border: 0; border-radius: 8px; color: #6c7287; cursor: pointer; font-size: 1.4rem; line-height: 1; padding: 4px 9px; }
.crop-close:hover:not(:disabled) { background: #f4f1ff; color: #30364a; }
.crop-close:focus-visible { outline: 3px solid rgba(105, 54, 224, .28); outline-offset: 2px; }

.crop-body { align-items: center; display: grid; gap: 22px; grid-template-columns: auto minmax(0, 1fr); }
.crop-viewport { background: #1b2238; border-radius: 12px; cursor: grab; overflow: hidden; position: relative; touch-action: none; user-select: none; }
.crop-viewport:active { cursor: grabbing; }
.crop-viewport:focus-visible { outline: 3px solid rgba(105, 54, 224, .35); outline-offset: 3px; }
.crop-image { left: 0; max-width: none; pointer-events: none; position: absolute; top: 0; }
.crop-mask { border-radius: 50%; box-shadow: 0 0 0 999px rgba(9, 19, 38, .6); inset: 0; outline: 2px solid rgba(255, 255, 255, .85); outline-offset: -2px; pointer-events: none; position: absolute; }
.crop-loading { color: #c5cbd8; display: grid; font-size: .74rem; inset: 0; place-items: center; position: absolute; }

.crop-previews { display: grid; gap: 6px; justify-items: start; min-width: 0; }
.crop-previews-title { color: #7240df; font-size: .62rem; font-weight: 800; letter-spacing: .09em; margin: 0 0 4px; text-transform: uppercase; }
.crop-sidebar-preview { align-items: center; background: #111a2f; border-radius: 9px; display: flex; gap: 10px; max-width: 220px; padding: 10px; width: 100%; }
.crop-sidebar-text { display: grid; min-width: 0; }
.crop-sidebar-text strong { color: #fff; font-size: .71rem; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.crop-sidebar-text small { color: #a9b1c1; font-size: .61rem; margin-top: 2px; }
.crop-avatar { background-color: #e9e4fb; background-repeat: no-repeat; border-radius: 50%; display: block; flex: 0 0 auto; }
.crop-avatar.is-small { height: 36px; width: 36px; }
.crop-avatar.is-large { height: 72px; margin-top: 8px; width: 72px; }
.crop-preview-caption { color: #7b8192; font-size: .62rem; }

.crop-zoom { align-items: center; color: #30364a; display: grid; font-size: .72rem; font-weight: 700; gap: 12px; grid-template-columns: auto minmax(0, 1fr) 44px; }
.crop-zoom input { accent-color: #6832df; width: 100%; }
.crop-zoom output { color: #6c7287; font-variant-numeric: tabular-nums; text-align: right; }

.crop-actions { display: flex; gap: 10px; justify-content: flex-end; }

@media (max-width: 560px) {
  .crop-modal { padding: 18px 16px; }
  .crop-body { grid-template-columns: 1fr; justify-items: center; }
  .crop-previews { justify-items: center; }
  .crop-actions .settings-button { flex: 1; }
}
</style>
