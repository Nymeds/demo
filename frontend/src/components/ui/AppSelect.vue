<script setup>
// Lista de escolha no visual do site. O <select> nativo abre uma lista desenhada pelo sistema
// operacional, com cores que não seguem a página nem o modo noite.
// Altura, fonte, arredondamento e espaçamento vêm das variáveis --app-select-*, definidas por
// cada tela, para o campo ter o mesmo tamanho dos outros campos dela.
import { computed, nextTick, onBeforeUnmount, ref, useId, watch } from 'vue'

const LIST_MAX_HEIGHT = 280
const LIST_MIN_WIDTH = 180
const POPUP_GAP = 6
const VIEWPORT_MARGIN = 12

const props = defineProps({
  modelValue: { type: [String, Number, Boolean], default: '' },
  // Cada opção: { value, label, meta?, disabled?, badge?, badgeTone?: 'neutral' | 'warning' }
  options: { type: Array, default: () => [] },
  placeholder: { type: String, default: 'Selecione' },
  id: { type: String, default: undefined },
  invalid: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false },
  describedby: { type: String, default: undefined },
  // Para campos sem rótulo visível, como a ordenação dentro de uma barra.
  ariaLabel: { type: String, default: undefined },
  // 'default' tem borda e fundo; 'ghost' fica sem moldura, para ordenações dentro de barras.
  variant: { type: String, default: 'default' },
})

const emit = defineEmits(['update:modelValue'])

const uid = useId()
const listId = `${uid}-list`
const root = ref(null)
const trigger = ref(null)
const list = ref(null)
const open = ref(false)
const activeIndex = ref(-1)
const popupStyle = ref({})

const selected = computed(() => props.options.find(option => option.value === props.modelValue) ?? null)

function optionId(index) {
  return `${uid}-option-${index}`
}

function isEnabled(index) {
  const option = props.options[index]
  return Boolean(option) && !option.disabled
}

function firstEnabled(from, step) {
  for (let index = from; index >= 0 && index < props.options.length; index += step) {
    if (isEnabled(index)) return index
  }

  return -1
}

async function setActive(index) {
  activeIndex.value = index
  await nextTick()
  if (index >= 0) document.getElementById(optionId(index))?.scrollIntoView({ block: 'nearest' })
}

function moveActive(step) {
  const start = activeIndex.value < 0
    ? (step > 0 ? 0 : props.options.length - 1)
    : activeIndex.value + step
  const next = firstEnabled(start, step)

  if (next >= 0) setActive(next)
}

// A lista fica fixa na tela para não ser cortada pela rolagem do modal; abre para cima se faltar
// espaço e encolhe (com rolagem) quando falta espaço dos dois lados.
function position() {
  if (!trigger.value || !list.value) return

  const rect = trigger.value.getBoundingClientRect()
  const width = Math.min(Math.max(rect.width, LIST_MIN_WIDTH), window.innerWidth - VIEWPORT_MARGIN * 2)
  const left = Math.min(Math.max(rect.left, VIEWPORT_MARGIN), window.innerWidth - width - VIEWPORT_MARGIN)
  // Altura natural com a borda; o limite da lista é só o espaço disponível (nunca a altura exata do
  // conteúdo), para arredondamentos de subpixel não criarem uma rolagem de 1px à toa.
  const borders = list.value.offsetHeight - list.value.clientHeight
  const height = Math.min(list.value.scrollHeight + borders, LIST_MAX_HEIGHT)
  const spaceBelow = window.innerHeight - rect.bottom - VIEWPORT_MARGIN - POPUP_GAP
  const spaceAbove = rect.top - VIEWPORT_MARGIN - POPUP_GAP
  const opensAbove = spaceBelow < height && spaceAbove > spaceBelow
  const maxHeight = Math.max(0, Math.min(LIST_MAX_HEIGHT, opensAbove ? spaceAbove : spaceBelow))
  const shownHeight = Math.min(height, maxHeight)

  popupStyle.value = {
    left: `${left}px`,
    width: `${width}px`,
    top: `${opensAbove ? rect.top - shownHeight - POPUP_GAP : rect.bottom + POPUP_GAP}px`,
    maxHeight: `${maxHeight}px`,
  }
}

async function openList() {
  if (props.disabled || open.value) return

  popupStyle.value = { visibility: 'hidden' }
  open.value = true
  await nextTick()
  position()

  const selectedIndex = props.options.findIndex(option => option.value === props.modelValue)
  setActive(isEnabled(selectedIndex) ? selectedIndex : firstEnabled(0, 1))
}

function closeList({ restoreFocus = false } = {}) {
  open.value = false
  activeIndex.value = -1
  if (restoreFocus) trigger.value?.focus()
}

function toggle() {
  if (open.value) closeList()
  else openList()
}

function choose(index) {
  if (!isEnabled(index)) return

  emit('update:modelValue', props.options[index].value)
  closeList({ restoreFocus: true })
}

function jumpToLetter(key) {
  const letter = key.toLocaleLowerCase('pt-BR')
  const index = props.options.findIndex((option, optionIndex) => (
    isEnabled(optionIndex) && String(option.label).toLocaleLowerCase('pt-BR').startsWith(letter)
  ))

  if (index >= 0) setActive(index)
}

function onKeydown(event) {
  switch (event.key) {
    case 'ArrowDown':
    case 'ArrowUp':
      event.preventDefault()
      if (open.value) moveActive(event.key === 'ArrowDown' ? 1 : -1)
      else openList()
      break
    case 'Home':
    case 'End':
      if (!open.value) return
      event.preventDefault()
      setActive(event.key === 'Home' ? firstEnabled(0, 1) : firstEnabled(props.options.length - 1, -1))
      break
    case 'Enter':
    case ' ':
      // Fechada, o próprio clique do botão abre a lista.
      if (!open.value) return
      event.preventDefault()
      if (activeIndex.value >= 0) choose(activeIndex.value)
      break
    case 'Escape':
      if (!open.value) return
      // Fecha só a lista, sem fechar também o modal onde ela está.
      event.preventDefault()
      event.stopPropagation()
      closeList()
      break
    case 'Tab':
      if (open.value) closeList()
      break
    default:
      if (open.value && event.key.length === 1 && /\S/.test(event.key)) jumpToLetter(event.key)
  }
}

function onPointerDown(event) {
  if (!root.value?.contains(event.target)) closeList()
}

function onScroll(event) {
  if (!list.value?.contains(event.target)) position()
}

function onResize() {
  closeList()
}

function toggleListeners(isOpen) {
  const method = isOpen ? 'addEventListener' : 'removeEventListener'
  document[method]('pointerdown', onPointerDown)
  window[method]('scroll', onScroll, true)
  window[method]('resize', onResize)
}

watch(open, toggleListeners)
onBeforeUnmount(() => toggleListeners(false))

defineExpose({ focus: () => trigger.value?.focus() })
</script>

<template>
  <div ref="root" class="app-select" :class="[`is-${variant}`, { 'is-open': open, 'is-invalid': invalid }]">
    <button
      :id="id"
      ref="trigger"
      type="button"
      class="app-select-trigger"
      role="combobox"
      aria-haspopup="listbox"
      :aria-expanded="open"
      :aria-controls="listId"
      :aria-activedescendant="open && activeIndex >= 0 ? optionId(activeIndex) : undefined"
      :aria-invalid="invalid"
      :aria-describedby="describedby"
      :aria-label="ariaLabel"
      :disabled="disabled"
      @click="toggle"
      @keydown="onKeydown"
    >
      <span v-if="$slots.leading" class="app-select-leading" aria-hidden="true"><slot name="leading" /></span>

      <span v-if="selected" class="app-select-value">
        <span class="app-select-label">{{ selected.label }}</span>
        <small v-if="selected.meta">{{ selected.meta }}</small>
      </span>
      <span v-else class="app-select-placeholder">{{ placeholder }}</span>

      <svg class="app-select-chevron" viewBox="0 0 24 24" aria-hidden="true"><path d="m6 9 6 6 6-6" /></svg>
    </button>

    <!-- @click.prevent: dentro de um <label>, um clique na lista não reabre o campo pelo rótulo. -->
    <ul
      v-if="open"
      :id="listId"
      ref="list"
      class="app-select-list"
      role="listbox"
      :style="popupStyle"
      @mousedown.prevent
      @click.prevent
    >
      <li v-if="options.length === 0" class="app-select-empty">Nenhuma opção disponível.</li>

      <li
        v-for="(option, index) in options"
        :id="optionId(index)"
        :key="String(option.value)"
        class="app-select-option"
        :class="{
          'is-active': index === activeIndex,
          'is-selected': option.value === modelValue,
          'is-disabled': option.disabled,
        }"
        role="option"
        :aria-selected="option.value === modelValue"
        :aria-disabled="option.disabled || undefined"
        @pointerenter="isEnabled(index) && (activeIndex = index)"
        @click="choose(index)"
      >
        <span class="app-select-option-text">
          <span>{{ option.label }}</span>
          <small v-if="option.meta">{{ option.meta }}</small>
        </span>

        <span v-if="option.badge" class="app-select-badge" :class="`is-${option.badgeTone ?? 'neutral'}`">
          {{ option.badge }}
        </span>
        <svg
          v-else-if="option.value === modelValue"
          class="app-select-check"
          viewBox="0 0 24 24"
          aria-hidden="true"
        ><path d="m5 12.5 4.5 4.5L19 7.5" /></svg>
      </li>
    </ul>
  </div>
</template>

<style scoped>
.app-select { min-width: 0; position: relative; width: 100%; }

.app-select-trigger { align-items: center; background: #ffffff; border: 1px solid #dedce8; border-radius: var(--app-select-radius, 8px); box-sizing: border-box; color: #252338; cursor: pointer; display: flex; font: inherit; font-size: var(--app-select-font-size, 14px); font-weight: 400; gap: 10px; min-height: var(--app-select-height, 42px); padding: var(--app-select-padding, 6px 12px); text-align: left; width: 100%; }
.app-select-trigger:hover:not(:disabled) { border-color: #c9c1ea; }
.app-select-trigger:focus-visible,
.app-select.is-open .app-select-trigger { border-color: #6330e0; box-shadow: 0 0 0 3px rgba(99, 48, 224, .14); outline: none; }
.app-select.is-invalid .app-select-trigger { border-color: #c4463e; }
.app-select-trigger:disabled { background: #f4f4f8; color: #747a8d; cursor: not-allowed; }

.app-select.is-ghost .app-select-trigger { background: transparent; border-color: transparent; }
.app-select.is-ghost .app-select-trigger:hover:not(:disabled) { background: #f4f0ff; border-color: transparent; }

.app-select-leading { align-items: center; color: #6330e0; display: flex; flex: 0 0 auto; }
.app-select-leading :slotted(svg) { display: block; height: 18px; width: 18px; }

.app-select-value { display: grid; flex: 1; min-width: 0; }
.app-select-label { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.app-select-value small,
.app-select-option-text small { color: #7a7790; font-size: 12px; font-weight: 400; }
.app-select-placeholder { color: #8a879b; flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.app-select-chevron { fill: none; flex: 0 0 16px; height: 16px; margin-left: auto; stroke: #6b6880; stroke-linecap: round; stroke-linejoin: round; stroke-width: 2; transition: transform .15s ease; width: 16px; }
.app-select.is-open .app-select-chevron { transform: rotate(180deg); }

.app-select-list { background: #ffffff; border: 1px solid #e3dff0; border-radius: 10px; box-shadow: 0 16px 40px rgba(20, 18, 35, .18); box-sizing: border-box; list-style: none; margin: 0; overflow-y: auto; padding: 6px; position: fixed; z-index: 1000; }
.app-select-empty { color: #7a7790; font-size: 13px; padding: 10px; }

.app-select-option { align-items: center; border-radius: 7px; color: #252338; cursor: pointer; display: flex; font-size: max(13px, var(--app-select-font-size, 14px)); font-weight: 400; gap: 10px; justify-content: space-between; padding: 9px 10px; }
.app-select-option.is-active { background: #f4f0ff; }
.app-select-option.is-selected { color: #5726ce; font-weight: 600; }
.app-select-option.is-disabled { cursor: not-allowed; opacity: .55; }
.app-select-option-text { display: grid; gap: 1px; min-width: 0; }

.app-select-badge { border-radius: 999px; flex: 0 0 auto; font-size: 11px; font-weight: 700; padding: 3px 8px; white-space: nowrap; }
.app-select-badge.is-neutral { background: #efeef5; color: #5f5c72; }
.app-select-badge.is-warning { background: #fff2e2; color: #a75a10; }

.app-select-check { fill: none; flex: 0 0 16px; height: 16px; stroke: #6330e0; stroke-linecap: round; stroke-linejoin: round; stroke-width: 2.4; width: 16px; }

@media (prefers-reduced-motion: reduce) {
  .app-select-chevron { transition: none; }
}
</style>
