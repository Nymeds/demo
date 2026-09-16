<script setup>
import { nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { useTheme } from '../../composables/useTheme'

defineProps({
  name: { type: String, required: true },
  initial: { type: String, required: true },
  fallbackAvatarUrl: { type: String, default: '' },
  settingsActive: { type: Boolean, default: false },
})

const emit = defineEmits(['open-settings'])

const { isNight, toggleTheme } = useTheme()
const open = ref(false)
const root = ref(null)
const trigger = ref(null)
const panel = ref(null)

function menuItems() {
  return panel.value ? [...panel.value.querySelectorAll('[role^="menuitem"]')] : []
}

async function openMenu() {
  open.value = true
  await nextTick()
  menuItems()[0]?.focus()
}

function closeMenu({ restoreFocus = false } = {}) {
  open.value = false
  if (restoreFocus) trigger.value?.focus()
}

function toggleMenu() {
  if (open.value) closeMenu()
  else openMenu()
}

function openSettings() {
  closeMenu()
  emit('open-settings')
}

function moveFocus(step) {
  const items = menuItems()
  const index = items.indexOf(document.activeElement)
  items[(index + step + items.length) % items.length]?.focus()
}

function closeOnOutsidePointer(event) {
  if (!root.value?.contains(event.target)) closeMenu()
}

watch(open, isOpen => {
  if (isOpen) document.addEventListener('pointerdown', closeOnOutsidePointer)
  else document.removeEventListener('pointerdown', closeOnOutsidePointer)
})

onBeforeUnmount(() => document.removeEventListener('pointerdown', closeOnOutsidePointer))
</script>

<template>
  <div ref="root" class="user-menu" @keydown.esc="closeMenu({ restoreFocus: true })">
    <Transition name="user-menu">
      <div
        v-if="open"
        id="sidebar-user-menu"
        ref="panel"
        class="user-menu-panel"
        role="menu"
        aria-label="Opções da conta"
        @keydown.down.prevent="moveFocus(1)"
        @keydown.up.prevent="moveFocus(-1)"
      >
        <button
          class="user-menu-item"
          type="button"
          role="menuitemcheckbox"
          :aria-checked="isNight"
          @click="toggleTheme"
        >
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M20.5 14.5A8.5 8.5 0 0 1 9.5 3.5a8.5 8.5 0 1 0 11 11Z" />
          </svg>
          <span>Modo noite</span>
          <span class="user-menu-switch" aria-hidden="true"><span></span></span>
        </button>

        <div class="user-menu-separator" role="separator"></div>

        <button
          class="user-menu-item"
          :class="{ 'is-current': settingsActive }"
          type="button"
          role="menuitem"
          @click="openSettings"
        >
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <circle cx="12" cy="12" r="3" />
            <path d="M19.4 15a1.7 1.7 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-1.8-.3 1.7 1.7 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-.1a1.7 1.7 0 0 0-1.1-1.5 1.7 1.7 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.7 1.7 0 0 0 .3-1.8 1.7 1.7 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.1a1.7 1.7 0 0 0 1.5-1.1 1.7 1.7 0 0 0-.3-1.8l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.7 1.7 0 0 0 1.8.3H9a1.7 1.7 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.1a1.7 1.7 0 0 0 1 1.5 1.7 1.7 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0-.3 1.8V9a1.7 1.7 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.1a1.7 1.7 0 0 0-1.5 1Z" />
          </svg>
          <span>Configurações</span>
        </button>
      </div>
    </Transition>

    <button
      ref="trigger"
      class="user-menu-trigger"
      type="button"
      aria-haspopup="menu"
      :aria-expanded="open"
      :aria-controls="open ? 'sidebar-user-menu' : undefined"
      :title="name"
      @click="toggleMenu"
    >
      <span class="user-menu-avatar" aria-hidden="true">
        <img v-if="fallbackAvatarUrl" :src="fallbackAvatarUrl" alt="">
        <template v-else>{{ initial }}</template>
      </span>
      <span class="user-menu-details">
        <strong>{{ name }}</strong>
        <small>Usuário conectado</small>
      </span>
      <svg class="user-menu-chevron" viewBox="0 0 24 24" aria-hidden="true"><path d="m7 14 5-5 5 5" /></svg>
    </button>
  </div>
</template>

<style scoped>
.user-menu { margin-bottom: 9px; position: relative; }

.user-menu-trigger { align-items: center; background: rgba(255, 255, 255, .045); border: 1px solid transparent; border-radius: 9px; color: #fff; cursor: pointer; display: flex; gap: 10px; min-width: 0; padding: 10px; text-align: left; transition: background-color .18s, border-color .18s; width: 100%; }
.user-menu-trigger:hover { background: rgba(255, 255, 255, .08); }
.user-menu-trigger[aria-expanded="true"] { border-color: rgba(148, 126, 255, .45); }
.user-menu-trigger:focus-visible { outline: 2px solid #947eff; outline-offset: 2px; }
.user-menu-avatar { align-items: center; background: linear-gradient(135deg, #7749f7, #5320da); border-radius: 50%; color: #fff; display: flex; flex: 0 0 36px; font-size: .78rem; font-weight: 800; height: 36px; justify-content: center; overflow: hidden; }
.user-menu-avatar img { display: block; height: 100%; max-width: none; object-fit: cover; width: 100%; }
.user-menu-details { flex: 1; min-width: 0; }
.user-menu-details strong { display: block; font-size: .71rem; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.user-menu-details small { color: rgba(215, 221, 233, .72); display: block; font-size: .61rem; margin-top: 2px; }
.user-menu-chevron { fill: none; flex: 0 0 16px; height: 16px; stroke: rgba(255, 255, 255, .55); stroke-linecap: round; stroke-linejoin: round; stroke-width: 2; transition: transform .18s; width: 16px; }
.user-menu-trigger[aria-expanded="true"] .user-menu-chevron { transform: rotate(180deg); }

.user-menu-panel { background: #18223a; border: 1px solid rgba(255, 255, 255, .09); border-radius: 12px; bottom: calc(100% + 8px); box-shadow: 0 18px 40px rgba(3, 8, 20, .45); left: 0; padding: 6px; position: absolute; right: 0; z-index: 90; }
.user-menu-item { align-items: center; background: transparent; border: 0; border-radius: 8px; color: rgba(236, 239, 246, .92); cursor: pointer; display: flex; font-size: .74rem; gap: 11px; padding: 10px 11px; text-align: left; width: 100%; }
.user-menu-item:hover,
.user-menu-item:focus-visible { background: rgba(255, 255, 255, .07); outline: none; }
.user-menu-item.is-current { color: #c4b5fd; }
.user-menu-item svg { fill: none; flex: 0 0 18px; height: 18px; stroke: currentColor; stroke-linecap: round; stroke-linejoin: round; stroke-width: 1.8; width: 18px; }
.user-menu-item > span:first-of-type { flex: 1; }
.user-menu-switch { background: rgba(255, 255, 255, .18); border-radius: 999px; flex: 0 0 30px; height: 17px; position: relative; transition: background-color .18s; }
.user-menu-switch span { background: #fff; border-radius: 50%; height: 13px; left: 2px; position: absolute; top: 2px; transition: transform .18s; width: 13px; }
.user-menu-item[aria-checked="true"] .user-menu-switch { background: #6b3ad6; }
.user-menu-item[aria-checked="true"] .user-menu-switch span { transform: translateX(13px); }
.user-menu-separator { background: rgba(255, 255, 255, .07); height: 1px; margin: 4px 6px; }

.user-menu-enter-active,
.user-menu-leave-active { transition: opacity .16s ease, transform .16s ease; }
.user-menu-enter-from,
.user-menu-leave-to { opacity: 0; transform: translateY(6px); }

@media (max-width: 760px) {
  .user-menu { margin: 0; }
  .user-menu-trigger { background: transparent; height: 100%; justify-content: center; padding: 6px; }
  .user-menu-details,
  .user-menu-chevron { display: none; }
  .user-menu-avatar { flex-basis: 32px; height: 32px; }
  .user-menu-panel { bottom: calc(100% + 14px); left: auto; right: -4px; width: 230px; }
}
</style>
