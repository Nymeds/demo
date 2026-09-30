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
  <section class="disciplines-table-card" aria-label="Lista de disciplinas">
    <div class="disciplines-table-scroll">
      <table>
        <colgroup>
          <col class="column-discipline">
          <col class="column-professor">
          <col class="column-schedules">
          <col class="column-average">
          <col class="column-attendance">
          <col class="column-status">
          <col class="column-actions">
        </colgroup>
        <thead>
          <tr>
            <th>Disciplina</th>
            <th>Professor</th>
            <th><span class="schedule-column-heading">Horários</span></th>
            <th>Média</th>
            <th>Frequência</th>
            <th>Situação</th>
            <th>Ações</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="discipline in disciplines" :key="discipline.id">
            <td>
              <div class="discipline-name-cell">
                <span class="discipline-color" :style="{ backgroundColor: `${disciplineColor(discipline)}1f`, color: disciplineColor(discipline) }" aria-hidden="true">
                  <svg viewBox="0 0 24 24"><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2Z" /><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20M8 7h8M8 10h6" /></svg>
                </span>
                <span class="discipline-name" :data-tooltip="discipline.name">{{ discipline.name }}</span>
              </div>
            </td>
            <td class="discipline-professor" :data-tooltip="discipline.professorName || 'Sem professor'">
              {{ discipline.professorName || 'Sem professor' }}
            </td>
            <td>
              <div class="discipline-schedules">
                <span
                  v-for="(schedule, index) in discipline.schedules"
                  :key="index"
                  :data-tooltip="`${dayLabels[schedule.dayOfWeek]} ${schedule.startTime} – ${schedule.endTime}`"
                >
                  <svg viewBox="0 0 24 24" aria-hidden="true"><rect x="3" y="5" width="18" height="16" rx="2" /><path d="M7 3v4m10-4v4M3 10h18" /></svg>
                  <span>{{ dayLabels[schedule.dayOfWeek] }} {{ schedule.startTime }} – {{ schedule.endTime }}</span>
                </span>
              </div>
            </td>
            <td :class="['discipline-average', { 'is-low': typeof discipline.average === 'number' && discipline.average < 7 }]">
              {{ formatAverage(discipline.average) }}
            </td>
            <td>
              <div :class="['discipline-attendance', 'is-' + disciplineAttendanceSituation(discipline, attendanceAlertMargin)]">
                <span>{{ attendanceLabel(discipline.attendancePercentage) }}</span>
                <span class="attendance-track" aria-hidden="true">
                  <span v-if="typeof discipline.attendancePercentage === 'number'" :style="{ width: `${discipline.attendancePercentage}%` }"></span>
                </span>
              </div>
            </td>
            <td>
              <span :class="['discipline-status', statusDetails(discipline.status).className]">
                {{ statusDetails(discipline.status).label }}
              </span>
            </td>
            <td>
              <div class="discipline-actions-cell">
                <button
                  class="status-menu-trigger"
                  type="button"
                  :aria-label="`Alterar situação de ${discipline.name}`"
                  aria-haspopup="menu"
                  :aria-expanded="openMenuId === discipline.id"
                  data-tooltip="Alterar situação"
                  data-tooltip-position="left"
                  @click.stop="emit('toggle-status', discipline, $event)"
                >
                  <svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="5" cy="12" r="1.7" /><circle cx="12" cy="12" r="1.7" /><circle cx="19" cy="12" r="1.7" /></svg>
                </button>
                <button type="button" aria-label="Editar disciplina" data-tooltip="Editar" data-tooltip-position="left" @click="emit('edit', discipline)">
                  <svg viewBox="0 0 24 24" aria-hidden="true"><path d="m4 20 4-1 11-11-3-3L5 16l-1 4Z" /><path d="m14 7 3 3" /></svg>
                </button>
                <button class="delete-action" type="button" aria-label="Excluir disciplina" data-tooltip="Excluir" data-tooltip-position="left" @click="emit('delete', discipline)">
                  <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 7h16M9 7V4h6v3m3 0-1 13H7L6 7m4 4v5m4-5v5" /></svg>
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <p v-if="disciplines.length === 0" class="disciplines-no-results">Nenhuma disciplina encontrada com esses filtros.</p>
  </section>
</template>
