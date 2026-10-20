<script setup>
import { formatAverage } from './dashboardPresentation.js'

defineProps({
  loading: { type: Boolean, default: false },
  disciplineCount: { type: Number, default: 0 },
  activityCount: { type: Number, default: 0 },
  pendingCount: { type: Number, default: 0 },
  generalAverage: { type: Number, default: null },
  averageAttendance: { type: Number, default: null },
})
</script>

<template>
  <div class="dashboard-summary-grid" aria-label="Resumo acadêmico">
    <article class="dashboard-summary-card is-purple">
      <span class="dashboard-summary-icon" aria-hidden="true">
        <svg viewBox="0 0 24 24"><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2Z" /><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20M8 7h8M8 10h6" /></svg>
      </span>
      <div><p>Disciplinas</p><strong>{{ loading ? '—' : disciplineCount }}</strong><small>{{ disciplineCount ? 'Cadastradas' : 'Nenhuma ainda' }}</small></div>
    </article>
    <article class="dashboard-summary-card is-green">
      <span class="dashboard-summary-icon" aria-hidden="true">
        <svg viewBox="0 0 24 24"><rect x="5" y="4" width="14" height="17" rx="2" /><path d="M9 4V2m6 2V2M8 9h8m-8 4 2 2 4-4" /></svg>
      </span>
      <div><p>Atividades</p><strong>{{ loading ? '—' : activityCount }}</strong><small>{{ activityCount ? 'Em todas as disciplinas' : 'Nenhuma ainda' }}</small></div>
    </article>
    <article class="dashboard-summary-card is-orange">
      <span class="dashboard-summary-icon" aria-hidden="true">
        <svg viewBox="0 0 24 24"><rect x="3" y="5" width="18" height="16" rx="2" /><path d="M7 3v4m10-4v4M3 10h18m5 4h4" /></svg>
      </span>
      <div><p>Pendentes</p><strong>{{ loading ? '—' : pendingCount }}</strong><small>{{ pendingCount ? 'Aguardando conclusão' : 'Tudo em dia' }}</small></div>
    </article>
    <article class="dashboard-summary-card is-violet">
      <span class="dashboard-summary-icon" aria-hidden="true">
        <svg viewBox="0 0 24 24"><path d="M4 19v-5m5 5V9m5 10v-7m5 7V5" /><path d="m4 10 5-4 5 3 6-6" /></svg>
      </span>
      <div><p>Média geral</p><strong>{{ loading ? '—' : formatAverage(generalAverage) }}</strong><small>{{ generalAverage === null ? 'Aguardando notas' : 'Das disciplinas' }}</small></div>
    </article>
    <article class="dashboard-summary-card is-blue">
      <span class="dashboard-summary-icon" aria-hidden="true">
        <svg viewBox="0 0 24 24"><path d="M12 3a9 9 0 1 1-7.8 4.5" /><path d="M12 3v9l6 4" /></svg>
      </span>
      <div><p>Frequência média</p><strong>{{ loading || averageAttendance === null ? '—' : `${Math.round(averageAttendance)}%` }}</strong><small>{{ averageAttendance === null ? 'Aguardando frequência' : 'Das disciplinas' }}</small></div>
    </article>
  </div>
</template>
