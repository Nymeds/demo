<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useTheme } from '../../composables/useTheme'
import { useFocusTrap } from '../../shared/a11y/useFocusTrap.js'
import SidebarUserMenu from './SidebarUserMenu.vue'

const props = defineProps({
  name: { type: String, default: '' },
  initial: { type: String, default: '' },
  avatarUrl: { type: String, default: '' },
  activeSection: { type: String, required: true },
})

const emit = defineEmits(['navigate', 'logout'])

// Seções que, no celular, ficam na folha "Mais" (o botão fica ativo quando uma delas está aberta).
const moreSections = ['exams', 'frequency', 'grades', 'simulator', 'profile', 'settings']

const { isNight, toggleTheme } = useTheme()
const moreOpen = ref(false)
const moreSheet = ref(null)
const moreActive = computed(() => moreSections.includes(props.activeSection))

function closeMore() {
  moreOpen.value = false
}

function navigate(section) {
  closeMore()
  emit('navigate', section)
}

function logout() {
  closeMore()
  emit('logout')
}

useFocusTrap(moreOpen, moreSheet, { onClose: closeMore })

// A folha só existe no layout de celular: se a janela crescer com ela aberta, fecha.
let mobileQuery = null
function closeWhenDesktop(event) {
  if (!event.matches) closeMore()
}
function stopWatchingViewport() {
  mobileQuery?.removeEventListener?.('change', closeWhenDesktop)
  mobileQuery = null
}
watch(moreOpen, open => {
  stopWatchingViewport()
  if (!open || typeof window === 'undefined' || typeof window.matchMedia !== 'function') return
  mobileQuery = window.matchMedia('(max-width: 760px)')
  mobileQuery.addEventListener?.('change', closeWhenDesktop)
})
onBeforeUnmount(stopWatchingViewport)
</script>

<template>
  <aside class="dashboard-sidebar">
    <div class="dashboard-brand">
      <span class="dashboard-brand-icon" aria-hidden="true">
        <svg viewBox="0 0 24 24">
          <path d="m3 9 9-4 9 4-9 4-9-4Z" />
          <path d="M7 11v5c3 2 7 2 10 0v-5M21 9v6" />
        </svg>
      </span>
      <span>
        <strong>AcadOrganize</strong>
        <small>Organize seus estudos</small>
      </span>
    </div>
    <nav class="dashboard-navigation" aria-label="Navegação principal">
      <button
        type="button"
        :class="{ active: activeSection === 'dashboard' }"
        :aria-current="activeSection === 'dashboard' ? 'page' : undefined"
        title="Dashboard"
        aria-label="Dashboard"
        @click="navigate('dashboard')"
      >
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="m3 11 9-8 9 8" />
          <path d="M5 10v10h14V10M9 20v-6h6v6" />
        </svg>
        Dashboard
      </button>
      <button
        type="button"
        :class="{ active: activeSection === 'disciplines' }"
        :aria-current="activeSection === 'disciplines' ? 'page' : undefined"
        title="Disciplinas"
        aria-label="Disciplinas"
        @click="navigate('disciplines')"
      >
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2Z" />
          <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20M8 7h8M8 10h6" />
        </svg>
        Disciplinas
      </button>
      <button
        type="button"
        :class="{ active: activeSection === 'activities' }"
        :aria-current="activeSection === 'activities' ? 'page' : undefined"
        title="Atividades"
        aria-label="Atividades"
        @click="navigate('activities')"
      >
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <rect x="5" y="4" width="14" height="17" rx="2" />
          <path d="M9 4V2m6 2V2M8 9h8m-8 4 2 2 4-4" />
        </svg>
        Atividades
      </button>
      <button
        class="is-secondary"
        type="button"
        :class="{ active: activeSection === 'exams' }"
        :aria-current="activeSection === 'exams' ? 'page' : undefined"
        title="Provas"
        aria-label="Provas"
        @click="navigate('exams')"
      >
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <rect x="5" y="3" width="14" height="18" rx="2" />
          <path d="M9 3V2m6 1V2M8 9h8m-8 4h8m-8 4h5" />
        </svg>
        Provas
      </button>
      <button
        class="is-secondary"
        type="button"
        :class="{ active: activeSection === 'frequency' }"
        :aria-current="activeSection === 'frequency' ? 'page' : undefined"
        title="Frequência"
        aria-label="Frequência"
        @click="navigate('frequency')"
      >
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <rect x="3" y="5" width="18" height="16" rx="2" />
          <path d="M16 3v4M8 3v4M3 11h18m-13 5 2 2 4-4" />
        </svg>
        Frequência
      </button>
      <button
        class="is-secondary"
        type="button"
        :class="{ active: activeSection === 'grades' }"
        :aria-current="activeSection === 'grades' ? 'page' : undefined"
        title="Notas"
        aria-label="Notas"
        @click="navigate('grades')"
      >
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M3 17l6-6 4 4 8-8" />
          <path d="M14 7h7v7" />
        </svg>
        Notas
      </button>
      <button
        class="is-secondary"
        type="button"
        :class="{ active: activeSection === 'simulator' }"
        :aria-current="activeSection === 'simulator' ? 'page' : undefined"
        title="Simulador de Notas"
        aria-label="Simulador de Notas"
        @click="navigate('simulator')"
      >
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M4 19V5h16v14H4Z" />
          <path d="M8 15v-3m4 3V9m4 6v-5" />
        </svg>
        Simulador de Notas
      </button>
      <button
        type="button"
        :class="{ active: activeSection === 'calendar' }"
        :aria-current="activeSection === 'calendar' ? 'page' : undefined"
        title="Calendário"
        aria-label="Calendário"
        @click="navigate('calendar')"
      >
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <rect x="3" y="5" width="18" height="16" rx="2" />
          <path d="M7 3v4m10-4v4M3 10h18" />
        </svg>
        Calendário
      </button>
      <button
        class="dashboard-more-button"
        type="button"
        :class="{ active: moreActive }"
        aria-haspopup="dialog"
        :aria-expanded="moreOpen"
        :aria-controls="moreOpen ? 'dashboard-more-sheet' : undefined"
        @click="moreOpen = !moreOpen"
      >
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <circle cx="5" cy="12" r="1.5" />
          <circle cx="12" cy="12" r="1.5" />
          <circle cx="19" cy="12" r="1.5" />
        </svg>
        Mais
      </button>
    </nav>
    <nav class="dashboard-profile-navigation" aria-label="Conta">
      <button
        type="button"
        :class="{ active: activeSection === 'profile' }"
        :aria-current="activeSection === 'profile' ? 'page' : undefined"
        title="Perfil"
        aria-label="Perfil"
        @click="navigate('profile')"
      >
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <circle cx="12" cy="8" r="4" />
          <path d="M4 21a8 8 0 0 1 16 0" />
        </svg>
        Perfil
      </button>
    </nav>
    <div class="dashboard-sidebar-footer">
      <SidebarUserMenu
        :name="name"
        :initial="initial"
        :fallback-avatar-url="avatarUrl"
        :settings-active="activeSection === 'settings'"
        @open-settings="navigate('settings')"
      />
      <button class="dashboard-logout" type="button" data-tooltip="Sair" aria-label="Sair" @click="logout">
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M10 5H5v14h5M14 8l4 4-4 4M8 12h10" />
        </svg>
        Sair
      </button>
    </div>

    <div v-if="moreOpen" class="dashboard-more-backdrop" aria-hidden="true" @click="closeMore"></div>
    <div
      v-if="moreOpen"
      id="dashboard-more-sheet"
      ref="moreSheet"
      class="dashboard-more-sheet"
      role="dialog"
      aria-modal="true"
      aria-label="Mais opções"
      tabindex="-1"
    >
      <button
        class="dashboard-more-item"
        type="button"
        :class="{ active: activeSection === 'exams' }"
        :aria-current="activeSection === 'exams' ? 'page' : undefined"
        @click="navigate('exams')"
      >
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <rect x="5" y="3" width="14" height="18" rx="2" />
          <path d="M9 3V2m6 1V2M8 9h8m-8 4h8m-8 4h5" />
        </svg>
        Provas
      </button>
      <button
        class="dashboard-more-item"
        type="button"
        :class="{ active: activeSection === 'frequency' }"
        :aria-current="activeSection === 'frequency' ? 'page' : undefined"
        @click="navigate('frequency')"
      >
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <rect x="3" y="5" width="18" height="16" rx="2" />
          <path d="M16 3v4M8 3v4M3 11h18m-13 5 2 2 4-4" />
        </svg>
        Frequência
      </button>
      <button
        class="dashboard-more-item"
        type="button"
        :class="{ active: activeSection === 'grades' }"
        :aria-current="activeSection === 'grades' ? 'page' : undefined"
        @click="navigate('grades')"
      >
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M3 17l6-6 4 4 8-8" />
          <path d="M14 7h7v7" />
        </svg>
        Notas
      </button>
      <button
        class="dashboard-more-item"
        type="button"
        :class="{ active: activeSection === 'simulator' }"
        :aria-current="activeSection === 'simulator' ? 'page' : undefined"
        @click="navigate('simulator')"
      >
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M4 19V5h16v14H4Z" />
          <path d="M8 15v-3m4 3V9m4 6v-5" />
        </svg>
        Simulador de Notas
      </button>
      <button
        class="dashboard-more-item"
        type="button"
        :class="{ active: activeSection === 'profile' }"
        :aria-current="activeSection === 'profile' ? 'page' : undefined"
        @click="navigate('profile')"
      >
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <circle cx="12" cy="8" r="4" />
          <path d="M4 21a8 8 0 0 1 16 0" />
        </svg>
        Perfil
      </button>
      <button
        class="dashboard-more-item"
        type="button"
        :class="{ active: activeSection === 'settings' }"
        :aria-current="activeSection === 'settings' ? 'page' : undefined"
        @click="navigate('settings')"
      >
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <circle cx="12" cy="12" r="3" />
          <path d="M19.4 15a1.7 1.7 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-1.8-.3 1.7 1.7 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-.1a1.7 1.7 0 0 0-1.1-1.5 1.7 1.7 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.7 1.7 0 0 0 .3-1.8 1.7 1.7 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.1a1.7 1.7 0 0 0 1.5-1.1 1.7 1.7 0 0 0-.3-1.8l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.7 1.7 0 0 0 1.8.3H9a1.7 1.7 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.1a1.7 1.7 0 0 0 1 1.5 1.7 1.7 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0-.3 1.8V9a1.7 1.7 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.1a1.7 1.7 0 0 0-1.5 1Z" />
        </svg>
        Configurações
      </button>
      <button class="dashboard-more-item" type="button" :aria-pressed="isNight" @click="toggleTheme">
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M20.5 14.5A8.5 8.5 0 0 1 9.5 3.5a8.5 8.5 0 1 0 11 11Z" />
        </svg>
        Modo noite
      </button>
      <button class="dashboard-more-item" type="button" @click="logout">
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M10 5H5v14h5M14 8l4 4-4 4M8 12h10" />
        </svg>
        Sair
      </button>
    </div>
  </aside>
</template>
