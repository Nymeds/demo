<script setup>
// Calendário no visual do site. O calendário nativo do <input type="date"> é desenhado pelo
// navegador, com cores e layout diferentes de cada navegador e do modo noite.
import { computed, nextTick, onBeforeUnmount, ref, useId, watch } from 'vue'

const WEEKDAYS = Object.freeze([
  { short: 'D', long: 'domingo' },
  { short: 'S', long: 'segunda-feira' },
  { short: 'T', long: 'terça-feira' },
  { short: 'Q', long: 'quarta-feira' },
  { short: 'Q', long: 'quinta-feira' },
  { short: 'S', long: 'sexta-feira' },
  { short: 'S', long: 'sábado' },
])
const DAYS_IN_WEEK = 7
const POPUP_WIDTH = 300
const POPUP_GAP = 6
const VIEWPORT_MARGIN = 12
const ISO_DATE = /^\d{4}-\d{2}-\d{2}$/

const props = defineProps({
  // Datas no formato "AAAA-MM-DD", o mesmo que o backend recebe.
  modelValue: { type: String, default: '' },
  min: { type: String, default: '' },
  max: { type: String, default: '' },
  id: { type: String, default: undefined },
  invalid: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false },
  describedby: { type: String, default: undefined },
  placeholder: { type: String, default: 'Selecione a data' },
})

const emit = defineEmits(['update:modelValue'])

const monthFormatter = new Intl.DateTimeFormat('pt-BR', { month: 'long', year: 'numeric' })
const dayLabelFormatter = new Intl.DateTimeFormat('pt-BR', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })

function isIso(value) {
  return ISO_DATE.test(value ?? '')
}

function toIso(date) {
  return [
    date.getFullYear(),
    String(date.getMonth() + 1).padStart(2, '0'),
    String(date.getDate()).padStart(2, '0'),
  ].join('-')
}

// As datas são montadas no fuso local: new Date("2026-09-15") seria meia-noite UTC, o dia anterior no Brasil.
function fromIso(iso) {
  const [year, month, day] = iso.split('-').map(Number)
  return new Date(year, month - 1, day)
}

function addDays(date, days) {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate() + days)
}

function addMonths(date, months) {
  const target = new Date(date.getFullYear(), date.getMonth() + months, 1)
  const lastDay = new Date(target.getFullYear(), target.getMonth() + 1, 0).getDate()
  return new Date(target.getFullYear(), target.getMonth(), Math.min(date.getDate(), lastDay))
}

function monthStartOf(iso) {
  const date = fromIso(iso)
  return new Date(date.getFullYear(), date.getMonth(), 1)
}

const uid = useId()
const dialogId = `${uid}-calendar`
const titleId = `${uid}-title`
const todayIso = toIso(new Date())

const root = ref(null)
const trigger = ref(null)
const popup = ref(null)
const open = ref(false)
const popupStyle = ref({})
const viewMonth = ref(monthStartOf(todayIso))
const focusedIso = ref(todayIso)

function isOutOfRange(iso) {
  return (isIso(props.min) && iso < props.min) || (isIso(props.max) && iso > props.max)
}

function clampIso(iso) {
  if (isIso(props.min) && iso < props.min) return props.min
  if (isIso(props.max) && iso > props.max) return props.max
  return iso
}

const displayValue = computed(() => {
  if (!isIso(props.modelValue)) return ''

  const [year, month, day] = props.modelValue.split('-')
  return `${day}/${month}/${year}`
})

const monthTitle = computed(() => {
  const text = monthFormatter.format(viewMonth.value)
  return text.charAt(0).toUpperCase() + text.slice(1)
})

const weeks = computed(() => {
  const year = viewMonth.value.getFullYear()
  const month = viewMonth.value.getMonth()
  const daysInMonth = new Date(year, month + 1, 0).getDate()
  const cells = Array.from({ length: new Date(year, month, 1).getDay() }, () => null)

  for (let day = 1; day <= daysInMonth; day += 1) {
    const date = new Date(year, month, day)
    const iso = toIso(date)

    cells.push({
      iso,
      day,
      label: dayLabelFormatter.format(date),
      disabled: isOutOfRange(iso),
      isToday: iso === todayIso,
      isSelected: iso === props.modelValue,
    })
  }

  while (cells.length % DAYS_IN_WEEK !== 0) cells.push(null)

  return Array.from({ length: cells.length / DAYS_IN_WEEK }, (_, week) => (
    cells.slice(week * DAYS_IN_WEEK, (week + 1) * DAYS_IN_WEEK)
  ))
})

const canGoPrevious = computed(() => (
  !isIso(props.min) || toIso(new Date(viewMonth.value.getFullYear(), viewMonth.value.getMonth(), 0)) >= props.min
))
const canGoNext = computed(() => (
  !isIso(props.max) || toIso(new Date(viewMonth.value.getFullYear(), viewMonth.value.getMonth() + 1, 1)) <= props.max
))
const canPickToday = computed(() => !isOutOfRange(todayIso))

function focusFocusedDay() {
  popup.value?.querySelector(`[data-iso="${focusedIso.value}"]`)?.focus()
}

async function moveFocus(iso) {
  focusedIso.value = clampIso(iso)
  viewMonth.value = monthStartOf(focusedIso.value)
  await nextTick()
  focusFocusedDay()
}

// O calendário fica fixo na tela para não ser cortado pela rolagem do modal; abre para cima se faltar espaço.
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

async function openCalendar() {
  if (props.disabled || open.value) return

  focusedIso.value = clampIso(isIso(props.modelValue) ? props.modelValue : todayIso)
  viewMonth.value = monthStartOf(focusedIso.value)
  popupStyle.value = { width: `${POPUP_WIDTH}px`, visibility: 'hidden' }
  open.value = true

  await nextTick()
  position()
  // Um elemento invisível não recebe foco, então o dia só é focado depois do posicionamento.
  await nextTick()
  focusFocusedDay()
}

function closeCalendar({ restoreFocus = false } = {}) {
  open.value = false
  if (restoreFocus) trigger.value?.focus()
}

function toggle() {
  if (open.value) closeCalendar()
  else openCalendar()
}

function choose(iso) {
  if (isOutOfRange(iso)) return

  emit('update:modelValue', iso)
  closeCalendar({ restoreFocus: true })
}

async function showMonth(step) {
  focusedIso.value = clampIso(toIso(addMonths(fromIso(focusedIso.value), step)))
  viewMonth.value = monthStartOf(focusedIso.value)

  // O dia que estava focado some junto com o mês antigo; sem isto o foco cairia na página
  // e o teclado (setas, Esc) deixaria de funcionar no calendário.
  await nextTick()
  if (open.value && !root.value?.contains(document.activeElement)) focusFocusedDay()
}

function onTriggerKeydown(event) {
  if (event.key === 'ArrowDown' && !open.value) {
    event.preventDefault()
    openCalendar()
  }
}

function onPopupKeydown(event) {
  if (event.key !== 'Escape') return

  // Fecha só o calendário, sem fechar também o modal onde ele está.
  event.preventDefault()
  event.stopPropagation()
  closeCalendar({ restoreFocus: true })
}

const DAY_MOVES = Object.freeze({ ArrowLeft: -1, ArrowRight: 1, ArrowUp: -DAYS_IN_WEEK, ArrowDown: DAYS_IN_WEEK })

function onGridKeydown(event) {
  const current = fromIso(focusedIso.value)

  if (event.key in DAY_MOVES) {
    event.preventDefault()
    moveFocus(toIso(addDays(current, DAY_MOVES[event.key])))
  } else if (event.key === 'Home' || event.key === 'End') {
    event.preventDefault()
    const weekday = current.getDay()
    moveFocus(toIso(addDays(current, event.key === 'Home' ? -weekday : DAYS_IN_WEEK - 1 - weekday)))
  } else if (event.key === 'PageUp' || event.key === 'PageDown') {
    event.preventDefault()
    moveFocus(toIso(addMonths(current, event.key === 'PageUp' ? -1 : 1)))
  }
}

function onFocusOut(event) {
  if (!open.value || root.value?.contains(event.relatedTarget)) return

  // Trocar de mês recria os botões dos dias: o dia focado some da página e o foco fica sem destino
  // por um instante, sem ter saído do calendário. Só fecha se o foco foi mesmo para outro elemento.
  setTimeout(() => {
    if (!open.value) return

    const active = document.activeElement
    if (root.value?.contains(active)) return

    if (!active || active === document.body) focusFocusedDay()
    else closeCalendar()
  })
}

function onPointerDown(event) {
  if (!root.value?.contains(event.target)) closeCalendar()
}

function onResize() {
  closeCalendar()
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
  <div ref="root" class="app-date" :class="{ 'is-open': open, 'is-invalid': invalid }" @focusout="onFocusOut">
    <button
      :id="id"
      ref="trigger"
      type="button"
      class="app-date-trigger"
      aria-haspopup="dialog"
      :aria-expanded="open"
      :aria-controls="open ? dialogId : undefined"
      :aria-invalid="invalid"
      :aria-describedby="describedby"
      :disabled="disabled"
      @click="toggle"
      @keydown="onTriggerKeydown"
    >
      <span :class="displayValue ? 'app-date-value' : 'app-date-placeholder'">{{ displayValue || placeholder }}</span>
      <svg class="app-date-icon" viewBox="0 0 24 24" aria-hidden="true">
        <rect x="3.5" y="5" width="17" height="15.5" rx="2.5" />
        <path d="M3.5 10h17M8 3v4M16 3v4" />
      </svg>
    </button>

    <div
      v-if="open"
      :id="dialogId"
      ref="popup"
      class="app-date-popup"
      role="dialog"
      :aria-labelledby="titleId"
      :style="popupStyle"
      @keydown="onPopupKeydown"
      @mousedown.prevent
      @click.prevent
    >
      <div class="app-date-header">
        <button type="button" class="app-date-nav" :disabled="!canGoPrevious" aria-label="Mês anterior" @click="showMonth(-1)">
          <svg viewBox="0 0 24 24" aria-hidden="true"><path d="m15 6-6 6 6 6" /></svg>
        </button>
        <strong :id="titleId" aria-live="polite">{{ monthTitle }}</strong>
        <button type="button" class="app-date-nav" :disabled="!canGoNext" aria-label="Próximo mês" @click="showMonth(1)">
          <svg viewBox="0 0 24 24" aria-hidden="true"><path d="m9 6 6 6-6 6" /></svg>
        </button>
      </div>

      <table class="app-date-grid" @keydown="onGridKeydown">
        <thead>
          <tr>
            <th v-for="weekday in WEEKDAYS" :key="weekday.long" scope="col">
              <abbr :title="weekday.long">{{ weekday.short }}</abbr>
            </th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(week, weekIndex) in weeks" :key="weekIndex">
            <td v-for="(cell, dayIndex) in week" :key="cell ? cell.iso : `vazio-${weekIndex}-${dayIndex}`">
              <button
                v-if="cell"
                type="button"
                class="app-date-day"
                :class="{ 'is-today': cell.isToday, 'is-selected': cell.isSelected }"
                :data-iso="cell.iso"
                :tabindex="cell.iso === focusedIso ? 0 : -1"
                :disabled="cell.disabled"
                :aria-label="cell.label"
                :aria-pressed="cell.isSelected"
                :aria-current="cell.isToday ? 'date' : undefined"
                @click="choose(cell.iso)"
              >
                {{ cell.day }}
              </button>
            </td>
          </tr>
        </tbody>
      </table>

      <div class="app-date-footer">
        <button type="button" class="app-date-link" :disabled="!canPickToday" @click="choose(todayIso)">Hoje</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.app-date { min-width: 0; position: relative; width: 100%; }

.app-date-trigger { align-items: center; background: #ffffff; border: 1px solid #dedce8; border-radius: var(--app-date-radius, 8px); box-sizing: border-box; color: #252338; cursor: pointer; display: flex; font: inherit; font-size: var(--app-date-font-size, 14px); font-weight: 400; gap: 10px; height: var(--app-date-height, 42px); justify-content: space-between; padding: var(--app-date-padding, 0 12px); text-align: left; width: 100%; }
.app-date-trigger:hover:not(:disabled) { border-color: #c9c1ea; }
.app-date-trigger:focus-visible,
.app-date.is-open .app-date-trigger { border-color: #6330e0; box-shadow: 0 0 0 3px rgba(99, 48, 224, .14); outline: none; }
.app-date.is-invalid .app-date-trigger { border-color: #c4463e; }
.app-date-trigger:disabled { background: #f4f4f8; color: #747a8d; cursor: not-allowed; }
.app-date-placeholder { color: #8a879b; }
.app-date-icon { fill: none; flex: 0 0 18px; height: 18px; stroke: #6330e0; stroke-linecap: round; stroke-linejoin: round; stroke-width: 1.8; width: 18px; }

.app-date-popup { background: #ffffff; border: 1px solid #e3dff0; border-radius: 12px; box-shadow: 0 18px 44px rgba(20, 18, 35, .2); box-sizing: border-box; padding: 12px; position: fixed; z-index: 1000; }

.app-date-header { align-items: center; display: flex; justify-content: space-between; margin-bottom: 8px; }
.app-date-header strong { color: #202033; font-size: 14px; }
.app-date-nav { align-items: center; background: transparent; border: 0; border-radius: 8px; color: #555267; cursor: pointer; display: flex; height: 32px; justify-content: center; width: 32px; }
.app-date-nav svg { fill: none; height: 16px; stroke: currentColor; stroke-linecap: round; stroke-linejoin: round; stroke-width: 2; width: 16px; }
.app-date-nav:hover:not(:disabled) { background: #f4f0ff; color: #5726ce; }
.app-date-nav:disabled { cursor: not-allowed; opacity: .35; }

.app-date-grid { border-collapse: collapse; table-layout: fixed; width: 100%; }
.app-date-grid th { color: #8a879b; font-size: 11px; font-weight: 700; padding: 4px 0 6px; text-align: center; }
.app-date-grid abbr { text-decoration: none; }
.app-date-grid td { padding: 2px; text-align: center; }

.app-date-day { aspect-ratio: 1; background: transparent; border: 0; border-radius: 8px; color: #252338; cursor: pointer; font: inherit; font-size: 13px; font-variant-numeric: tabular-nums; padding: 0; width: 100%; }
.app-date-day:hover:not(:disabled) { background: #f4f0ff; color: #5726ce; }
.app-date-day.is-today { box-shadow: inset 0 0 0 1px #b9a5f2; color: #5726ce; font-weight: 700; }
.app-date-day.is-selected { background: #6330e0; box-shadow: none; color: #ffffff; font-weight: 700; }
.app-date-day:disabled { color: #c3c1cf; cursor: not-allowed; }

.app-date-footer { border-top: 1px solid #efedf6; display: flex; justify-content: flex-end; margin-top: 8px; padding-top: 8px; }
.app-date-link { background: transparent; border: 0; border-radius: 6px; color: #6330e0; cursor: pointer; font: inherit; font-size: 13px; font-weight: 700; padding: 4px 8px; }
.app-date-link:hover:not(:disabled) { background: #f4f0ff; }
.app-date-link:disabled { cursor: not-allowed; opacity: .45; }

.app-date-day:focus-visible,
.app-date-nav:focus-visible,
.app-date-link:focus-visible { outline: 2px solid rgba(99, 48, 224, .5); outline-offset: 1px; }
</style>
