<script setup>
import { formatAverage } from '../../shared/format/grade.js'

defineProps({
  total: { type: Number, required: true },
  generalAverage: { type: Number, default: null },
  averageAttendance: { type: Number, default: null },
  attendanceSituation: { type: String, required: true },
})
</script>

<template>
  <div class="disciplines-summary-grid">
    <article class="disciplines-total-card is-purple">
      <span aria-hidden="true">
        <svg viewBox="0 0 24 24"><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2Z" /><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20M8 7h8M8 10h6" /></svg>
      </span>
      <div>
        <p>Total de disciplinas</p>
        <strong>{{ total }}</strong>
        <small>Nesta sessão</small>
      </div>
    </article>

    <article class="disciplines-total-card is-green">
      <span aria-hidden="true">
        <svg viewBox="0 0 24 24"><path d="M4 19v-5m5 5V9m5 10v-7m5 7V5" /><path d="m4 10 5-4 5 3 6-6" /></svg>
      </span>
      <div>
        <p>Média geral</p>
        <strong>{{ formatAverage(generalAverage) }}</strong>
        <small>{{ generalAverage === null ? 'Aguardando notas' : 'Todas as disciplinas' }}</small>
      </div>
    </article>

    <article :class="['disciplines-total-card', 'is-attendance-' + attendanceSituation]">
      <span aria-hidden="true">
        <svg viewBox="0 0 24 24"><circle cx="12" cy="8" r="4" /><path d="M4 21a8 8 0 0 1 16 0" /></svg>
      </span>
      <div>
        <p>Frequência média</p>
        <strong :class="{ 'is-text': averageAttendance === null }">{{ averageAttendance === null ? 'Sem frequência cadastrada' : `${Math.round(averageAttendance)}%` }}</strong>
        <small>{{ averageAttendance === null ? 'Aguardando frequência' : 'Todas as disciplinas' }}</small>
      </div>
    </article>
  </div>
</template>
