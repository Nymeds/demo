<script setup>
// Seletor de horário no visual do site. O relógio nativo do <input type="time"> muda de navegador
// para navegador e não segue o modo noite.
// Altura, fonte e arredondamento vêm das variáveis --app-time-*, definidas por cada tela.
import { computed, nextTick, onBeforeUnmount, ref, useId, watch } from 'vue'

const HOURS = Object.freeze(Array.from({ length: 24 }, (_, hour) => String(hour).padStart(2, '0')))
const DEFAULT_HOUR = '08'
const DEFAULT_MINUTE = '00'
const MINUTES_IN_HOUR = 60
const POPUP_WIDTH = 188
const POPUP_GAP = 6
const VIEWPORT_MARGIN = 12
const TIME_PATTERN = /^(\d{2}):(\d{2})/

const props = defineProps({
  // Horário "HH:MM"; valores vindos da API como "HH:MM:SS" também são aceitos.
  modelValue: { type: String, default: '' },
  minuteStep: { type: Number, default: 5 },
  id: { type: String, default: undefined },
  invalid: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false },
  describedby: { type: String, default: undefined },
  ariaLabel: { type: String, default: undefined },
  placeholder: { type: String, default: '--:--' },
})

const emit = defineEmits(['update:modelValue'])

const uid = useId()
const dialogId = `${uid}-time`
const root = ref(null)
const trigger = ref(null)
const popup = ref(null)
const open = ref(false)
const popupStyle = ref({})

const parts = computed(() => {
  const match = TIME_PATTERN.exec(props.modelValue ?? '')
  return match ? { hour: match[1], minute: match[2] } : null
})

const displayValue = computed(() => (parts.value ? `${parts.value.hour}:${parts.value.minute}` : ''))
const activeHour = computed(() => parts.value?.hour ?? DEFAULT_HOUR)
const activeMinute = computed(() => parts.value?.minute ?? DEFAULT_MINUTE)

// Minutos de 5 em 5; um horário já salvo fora desse passo (ex.: 07:52) continua aparecendo na lista.
const minutes = computed(() => {
  const values = new Set(Array.from(
    { length: Math.ceil(MINUTES_IN_HOUR / props.minuteStep) },
    (_, index) => String(index * props.minuteStep).padStart(2, '0'),
  ))

  if (parts.value) values.add(parts.value.minute)
  return [...values].sort()
})

const dialogLabel = computed(() => (props.ariaLabel ? `Escolher ${props.ariaLabel.toLocaleLowerCase('pt-BR')}` : 'Escolher horário'))

function buttonOf(column, value) {
  return popup.value?.querySelector(`[data-column="${column}"][data-value="${value}"]`)
    ?? popup.value?.querySelector(`[data-column="${column}"]`)
}

function centerInColumn(button) {
  const column = button?.parentElement
  if (!column) return
  column.scrollTop = button.offsetTop - column.offsetTop - (column.clientHeight - button.offsetHeight) / 2
}

function focusColumn(column) {
  const button = buttonOf(column, column === 'hour' ? activeHour.value : activeMinute.value)
  centerInColumn(button)
  button?.focus()
}

// O seletor fica fixo na tela para não ser cortado pela rolagem do modal; abre para cima se faltar espaço.
function position() {
  if (!trigger.value || !popup.value) return

  const rect = trigger.value.getBoundingClientRect()
  const width = Math.min(POPUP_WIDTH, window.innerWidth - VIEWPORT_MARGIN * 2)
  const height = popup.value.offsetHeight
  const spaceBelow = window.innerHeight - rect.bottom - VIEWPORT_MARGIN
  const opensAbove = spaceBelow < height && rect.top > spaceBelow
  const left = Math.min(Math.max(rect.left, VIEWPORT_MARGIN), window.innerWidth - width - VIEWPORT_MARGIN)

  popupStyle.value = {
    left: `${left}px`,
    width: `${width}px`,
    top: `${opensAbove ? Math.max(VIEWPORT_MARGIN, rect.top - height - POPUP_GAP) : rect.bottom + POPUP_GAP}px`,
  }
}

async function openPicker() {
  if (props.disabled || open.value) return

  popupStyle.value = { width: `${POPUP_WIDTH}px`, visibility: 'hidden' }
  open.value = true

  await nextTick()
  position()
  // Um elemento invisível não recebe foco, então a hora só é focada depois do posicionamento.
  await nextTick()
  centerInColumn(buttonOf('minute', activeMinute.value))
  focusColumn('hour')
}

function closePicker({ restoreFocus = false } = {}) {
  open.value = false
  if (restoreFocus) trigger.value?.focus()
}

function toggle() {
  if (open.value) closePicker()
  else openPicker()
}

// Escolher a hora mantém o seletor aberto para escolher os minutos; escolher os minutos fecha.
async function choose(column, value) {
  const hour = column === 'hour' ? value : activeHour.value
  const minute = column === 'minute' ? value : activeMinute.value

  emit('update:modelValue', `${hour}:${minute}`)

  if (column === 'minute') {
    closePicker({ restoreFocus: true })
    return
  }

  await nextTick()
  focusColumn('minute')
}

function onTriggerKeydown(event) {
  if (event.key === 'ArrowDown' && !open.value) {
    event.preventDefault()
    openPicker()
  }
}

function onPopupKeydown(event) {
  if (event.key === 'Escape') {
    // Fecha só o seletor, sem fechar também o modal onde ele está.
    event.preventDefault()
    event.stopPropagation()
    closePicker({ restoreFocus: true })
    return
  }

  const column = event.target?.dataset?.column
  if (!column) return

  const buttons = [...popup.value.querySelectorAll(`[data-column="${column}"]`)]
  const index = buttons.indexOf(event.target)
  let next = null

  if (event.key === 'ArrowDown') next = buttons[Math.min(index + 1, buttons.length - 1)]
  else if (event.key === 'ArrowUp') next = buttons[Math.max(index - 1, 0)]
  else if (event.key === 'Home') next = buttons[0]
  else if (event.key === 'End') next = buttons[buttons.length - 1]
  else if (event.key === 'ArrowRight' || event.key === 'ArrowLeft') {
    event.preventDefault()
    focusColumn(event.key === 'ArrowRight' ? 'minute' : 'hour')
    return
  }

  if (next) {
    event.preventDefault()
    centerInColumn(next)
    next.focus()
  }
}

function onFocusOut(event) {
  if (!open.value || root.value?.contains(event.relatedTarget)) return

  setTimeout(() => {
    if (!open.value) return

    const active = document.activeElement
    if (root.value?.contains(active)) return

    if (!active || active === document.body) focusColumn('hour')
    else closePicker()
  })
}

function onPointerDown(event) {
  if (!root.value?.contains(event.target)) closePicker()
}

function onResize() {
  closePicker()
}

function onScroll(event) {
  if (!popup.value?.contains(event.target)) position()
}

function toggleListeners(isOpen) {
  const method = isOpen ? 'addEventListener' : 'removeEventListener'
  document[method]('pointerdown', onPointerDown)
  window[method]('resize', onResize)
  window[method]('scroll', onScroll, true)
}

watch(open, toggleListeners)
onBeforeUnmount(() => toggleListeners(false))
</script>

<template>
  <div ref="root" class="app-time" :class="{ 'is-open': open, 'is-invalid': invalid }" @focusout="onFocusOut">
    <button
      :id="id"
      ref="trigger"
      type="button"
      class="app-time-trigger"
      aria-haspopup="dialog"
      :aria-expanded="open"
      :aria-controls="open ? dialogId : undefined"
      :aria-invalid="invalid"
      :aria-describedby="describedby"
      :aria-label="ariaLabel ? `${ariaLabel}${displayValue ? `: ${displayValue}` : ''}` : undefined"
      :disabled="disabled"
      @click="toggle"
      @keydown="onTriggerKeydown"
    >
      <span :class="displayValue ? 'app-time-value' : 'app-time-placeholder'">{{ displayValue || placeholder }}</span>
      <svg class="app-time-icon" viewBox="0 0 24 24" aria-hidden="true">
        <circle cx="12" cy="12" r="8.5" />
        <path d="M12 7.5V12l3 2" />
      </svg>
    </button>

    <!-- @click.prevent: dentro de um <label>, um clique no seletor não reabre o campo pelo rótulo. -->
    <div
      v-if="open"
      :id="dialogId"
      ref="popup"
      class="app-time-popup"
      role="dialog"
      :aria-label="dialogLabel"
      :style="popupStyle"
      @keydown="onPopupKeydown"
      @mousedown.prevent
      @click.prevent
    >
      <div class="app-time-columns">
        <div class="app-time-group">
          <span class="app-time-heading" aria-hidden="true">Hora</span>
          <div class="app-time-column" role="group" aria-label="Hora">
            <button
              v-for="hour in HOURS"
              :key="hour"
              type="button"
              class="app-time-option"
              :class="{ 'is-selected': parts?.hour === hour }"
              data-column="hour"
              :data-value="hour"
              :tabindex="hour === activeHour ? 0 : -1"
              :aria-pressed="parts?.hour === hour"
              :aria-label="`${hour} horas`"
              @click="choose('hour', hour)"
            >
              {{ hour }}
            </button>
          </div>
        </div>

        <div class="app-time-group">
          <span class="app-time-heading" aria-hidden="true">Min</span>
          <div class="app-time-column" role="group" aria-label="Minutos">
            <button
              v-for="minute in minutes"
              :key="minute"
              type="button"
              class="app-time-option"
              :class="{ 'is-selected': parts?.minute === minute }"
              data-column="minute"
              :data-value="minute"
              :tabindex="minute === activeMinute ? 0 : -1"
              :aria-pressed="parts?.minute === minute"
              :aria-label="`${minute} minutos`"
              @click="choose('minute', minute)"
            >
              {{ minute }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.app-time { min-width: 0; position: relative; width: 100%; }

.app-time-trigger { align-items: center; background: #ffffff; border: 1px solid #dedce8; border-radius: var(--app-time-radius, 8px); box-sizing: border-box; color: #252338; cursor: pointer; display: flex; font: inherit; font-size: var(--app-time-font-size, 14px); font-variant-numeric: tabular-nums; font-weight: 400; gap: 8px; height: var(--app-time-height, 42px); justify-content: space-between; padding: var(--app-time-padding, 0 12px); text-align: left; width: 100%; }
.app-time-trigger:hover:not(:disabled) { border-color: #c9c1ea; }
.app-time-trigger:focus-visible,
.app-time.is-open .app-time-trigger { border-color: #6330e0; box-shadow: 0 0 0 3px rgba(99, 48, 224, .14); outline: none; }
.app-time.is-invalid .app-time-trigger { border-color: #c4463e; }
.app-time-trigger:disabled { background: #f4f4f8; color: #747a8d; cursor: not-allowed; }
.app-time-placeholder { color: #8a879b; }
.app-time-icon { fill: none; flex: 0 0 16px; height: 16px; stroke: #6330e0; stroke-linecap: round; stroke-linejoin: round; stroke-width: 1.8; width: 16px; }

.app-time-popup { background: #ffffff; border: 1px solid #e3dff0; border-radius: 12px; box-shadow: 0 18px 44px rgba(20, 18, 35, .2); box-sizing: border-box; padding: 10px; position: fixed; z-index: 1000; }
.app-time-columns { display: grid; gap: 8px; grid-template-columns: repeat(2, minmax(0, 1fr)); }
.app-time-group { display: grid; gap: 6px; min-width: 0; }
.app-time-heading { color: #8a879b; font-size: 11px; font-weight: 700; letter-spacing: .06em; text-align: center; text-transform: uppercase; }
.app-time-column { display: grid; gap: 2px; max-height: 216px; overflow-y: auto; scrollbar-width: thin; }

.app-time-option { background: transparent; border: 0; border-radius: 7px; color: #252338; cursor: pointer; font: inherit; font-size: 13px; font-variant-numeric: tabular-nums; min-height: 32px; padding: 0; }
.app-time-option:hover { background: #f4f0ff; color: #5726ce; }
.app-time-option.is-selected { background: #6330e0; color: #ffffff; font-weight: 700; }
.app-time-option:focus-visible { outline: 2px solid rgba(99, 48, 224, .5); outline-offset: -2px; }
</style>
