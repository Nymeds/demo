<script setup>
import { classDateTime, formatClassSchedule } from './dashboardPresentation.js'

defineProps({
  nextClass: { type: Object, default: null },
  now: { type: Date, required: true },
})

const emit = defineEmits(['navigate'])
</script>

<template>
  <section class="dashboard-panel dashboard-classes-panel" aria-labelledby="dashboard-classes-title">
    <header class="dashboard-panel-header">
      <div>
        <span class="dashboard-eyebrow">Próxima aula</span>
        <h2 id="dashboard-classes-title">Sua próxima aula</h2>
      </div>
      <button type="button" @click="emit('navigate', 'disciplines')">Ver disciplinas <span aria-hidden="true">→</span></button>
    </header>

    <div v-if="!nextClass" class="dashboard-panel-empty dashboard-classes-empty">
      <span aria-hidden="true">
        <svg viewBox="0 0 24 24"><rect x="3" y="5" width="18" height="16" rx="2" /><path d="M7 3v4m10-4v4M3 10h18" /></svg>
      </span>
      <div><strong>Nenhum horário de aula cadastrado</strong><p>Adicione os dias e horários nas suas disciplinas para visualizar as próximas aulas.</p></div>
      <button type="button" @click="emit('navigate', 'disciplines')">Cadastrar horários</button>
    </div>

    <ul v-else class="dashboard-class-list">
      <li>
        <span
          class="dashboard-class-icon"
          :style="{ '--discipline-color': nextClass.discipline.color || '#6d3ce8' }"
          aria-hidden="true"
        >
          <svg viewBox="0 0 24 24"><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2Z" /><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20M8 7h8M8 10h6" /></svg>
        </span>
        <div class="dashboard-class-info">
          <strong>{{ nextClass.discipline.name }}</strong>
          <small>Prof: {{ nextClass.discipline.professorName || 'Não informado' }}</small>
        </div>
        <time class="dashboard-class-time" :datetime="classDateTime(nextClass)">
          <svg viewBox="0 0 24 24" aria-hidden="true"><rect x="3" y="5" width="18" height="16" rx="2" /><path d="M7 3v4m10-4v4M3 10h18" /></svg>
          <span>{{ formatClassSchedule(nextClass, now) }}</span>
        </time>
        <button
          class="dashboard-class-open"
          type="button"
          :aria-label="`Ver disciplina ${nextClass.discipline.name}`"
          @click="emit('navigate', 'disciplines')"
        >
          <svg viewBox="0 0 24 24" aria-hidden="true"><path d="m9 5 7 7-7 7" /></svg>
        </button>
      </li>
    </ul>
  </section>
</template>
