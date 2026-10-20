<script setup>
import { formatPercentage } from './frequencyPresentation.js'

defineProps({
  disciplineCount: { type: Number, required: true },
  averageAttendance: { type: Number, default: null },
  averageLabel: { type: String, required: true },
  totalAbsences: { type: Number, required: true },
})
</script>

<template>
  <div class="frequency-summary-grid">
    <article class="frequency-total-card is-purple">
      <span aria-hidden="true">
        <svg viewBox="0 0 24 24"><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2Z" /><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20M8 7h8M8 10h6" /></svg>
      </span>
      <div>
        <p>Disciplinas cadastradas</p>
        <strong>{{ disciplineCount }}</strong>
        <small>Todas as disciplinas</small>
      </div>
    </article>

    <article class="frequency-total-card is-green">
      <span aria-hidden="true">
        <svg viewBox="0 0 24 24"><path d="M12 3a9 9 0 1 0 9 9h-9V3Z" /><path d="M14 3.3A9 9 0 0 1 20.7 10H14V3.3Z" /></svg>
      </span>
      <div>
        <p>Frequência média geral</p>
        <strong :class="{ 'is-text': averageAttendance === null }">{{ averageAttendance === null ? 'Sem frequência cadastrada' : formatPercentage(averageAttendance) }}</strong>
        <small>{{ averageLabel }}</small>
      </div>
    </article>

    <article class="frequency-total-card is-orange">
      <span aria-hidden="true">
        <svg viewBox="0 0 24 24"><path d="M12 4 2.7 20h18.6L12 4Z" /><path d="M12 10v4m0 3v.5" /></svg>
      </span>
      <div>
        <p>Total de faltas</p>
        <strong>{{ totalAbsences }}</strong>
        <small>Em todos os períodos</small>
      </div>
    </article>
  </div>
</template>
