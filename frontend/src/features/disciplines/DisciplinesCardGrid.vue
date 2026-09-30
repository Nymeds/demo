<script setup>
import { formatAverage } from '../../shared/format/grade.js'
import { DAY_LABELS as dayLabels, attendanceLabel, disciplineAttendanceSituation, disciplineColor, statusDetails } from './disciplinesPresentation.js'

defineProps({
  disciplines: { type: Array, required: true },
  openMenuId: { type: [String, Number], default: null },
  attendanceAlertMargin: { type: Number, required: true },
})

const emit = defineEmits(['toggle-status', 'edit', 'delete'])
</script>

<template>
  <section class="disciplines-card-grid" aria-label="Disciplinas em grade">
    <article v-for="discipline in disciplines" :key="discipline.id" :style="{ '--card-color': disciplineColor(discipline) }">
      <header>
        <span class="discipline-color" :style="{ backgroundColor: `${disciplineColor(discipline)}1f`, color: disciplineColor(discipline) }" aria-hidden="true">
          <svg viewBox="0 0 24 24"><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2Z" /><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20M8 7h8M8 10h6" /></svg>
        </span>
        <div><h2>{{ discipline.name }}</h2><p>{{ discipline.professorName || 'Sem professor' }}</p></div>
      </header>
      <div class="grid-card-schedules">
        <span v-for="(schedule, index) in discipline.schedules" :key="index">{{ dayLabels[schedule.dayOfWeek] }} {{ schedule.startTime }} – {{ schedule.endTime }}</span>
      </div>
      <div class="grid-card-data">
        <span><small>Média</small><strong>{{ formatAverage(discipline.average) }}</strong></span>
        <span :class="['grid-attendance', 'is-' + disciplineAttendanceSituation(discipline, attendanceAlertMargin)]"><small>Frequência</small><strong>{{ attendanceLabel(discipline.attendancePercentage) }}</strong></span>
      </div>
      <footer>
        <span :class="['discipline-status', statusDetails(discipline.status).className]">{{ statusDetails(discipline.status).label }}</span>
        <div class="discipline-actions-cell">
          <button
            class="status-menu-trigger"
            type="button"
            :aria-label="`Alterar situação de ${discipline.name}`"
            aria-haspopup="menu"
            :aria-expanded="openMenuId === discipline.id"
            data-tooltip="Alterar situação"
            @click.stop="emit('toggle-status', discipline, $event)"
          >
            <svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="5" cy="12" r="1.7" /><circle cx="12" cy="12" r="1.7" /><circle cx="19" cy="12" r="1.7" /></svg>
          </button>
          <button type="button" aria-label="Editar disciplina" @click="emit('edit', discipline)"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="m4 20 4-1 11-11-3-3L5 16l-1 4Z" /><path d="m14 7 3 3" /></svg></button>
          <button class="delete-action" type="button" aria-label="Excluir disciplina" @click="emit('delete', discipline)"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 7h16M9 7V4h6v3m3 0-1 13H7L6 7m4 4v5m4-5v5" /></svg></button>
        </div>
      </footer>
    </article>

    <p v-if="disciplines.length === 0" class="disciplines-no-results">Nenhuma disciplina encontrada com esses filtros.</p>
  </section>
</template>
