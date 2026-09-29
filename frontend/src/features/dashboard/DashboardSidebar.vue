<script setup>
import SidebarUserMenu from './SidebarUserMenu.vue'

defineProps({
  name: { type: String, default: '' },
  initial: { type: String, default: '' },
  avatarUrl: { type: String, default: '' },
  activeSection: { type: String, required: true },
})

const emit = defineEmits(['navigate', 'logout'])
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
        @click="emit('navigate', 'dashboard')"
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
        @click="emit('navigate', 'disciplines')"
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
        @click="emit('navigate', 'activities')"
      >
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <rect x="5" y="4" width="14" height="17" rx="2" />
          <path d="M9 4V2m6 2V2M8 9h8m-8 4 2 2 4-4" />
        </svg>
        Atividades
      </button>
      <button
        type="button"
        :class="{ active: activeSection === 'exams' }"
        :aria-current="activeSection === 'exams' ? 'page' : undefined"
        @click="emit('navigate', 'exams')"
      >
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <rect x="5" y="3" width="14" height="18" rx="2" />
          <path d="M9 3V2m6 1V2M8 9h8m-8 4h8m-8 4h5" />
        </svg>
        Provas
      </button>
      <button
        type="button"
        :class="{ active: activeSection === 'frequency' }"
        :aria-current="activeSection === 'frequency' ? 'page' : undefined"
        @click="emit('navigate', 'frequency')"
      >
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <rect x="3" y="5" width="18" height="16" rx="2" />
          <path d="M16 3v4M8 3v4M3 11h18m-13 5 2 2 4-4" />
        </svg>
        Frequência
      </button>
      <button
        type="button"
        :class="{ active: activeSection === 'grades' }"
        :aria-current="activeSection === 'grades' ? 'page' : undefined"
        @click="emit('navigate', 'grades')"
      >
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M3 17l6-6 4 4 8-8" />
          <path d="M14 7h7v7" />
        </svg>
        Notas
      </button>
      <button
        type="button"
        :class="{ active: activeSection === 'simulator' }"
        :aria-current="activeSection === 'simulator' ? 'page' : undefined"
        @click="emit('navigate', 'simulator')"
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
        @click="emit('navigate', 'calendar')"
      >
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <rect x="3" y="5" width="18" height="16" rx="2" />
          <path d="M7 3v4m10-4v4M3 10h18" />
        </svg>
        Calendário
      </button>
    </nav>
    <nav class="dashboard-profile-navigation" aria-label="Conta">
      <button
        type="button"
        :class="{ active: activeSection === 'profile' }"
        :aria-current="activeSection === 'profile' ? 'page' : undefined"
        @click="emit('navigate', 'profile')"
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
        @open-settings="emit('navigate', 'settings')"
      />
      <button class="dashboard-logout" type="button" @click="emit('logout')">
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M10 5H5v14h5M14 8l4 4-4 4M8 12h10" />
        </svg>
        Sair
      </button>
    </div>
  </aside>
</template>
