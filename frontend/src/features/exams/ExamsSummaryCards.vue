<script setup>
defineProps({
  totalExams: { type: Number, required: true },
  upcomingCount: { type: Number, required: true },
  thisMonthExams: { type: Number, required: true },
  completedExams: { type: Number, required: true },
  monthLabel: { type: String, required: true },
})
</script>

<template>
  <div class="exams-summary-grid">
    <article class="exams-summary-card is-purple">
      <span class="summary-icon" aria-hidden="true">
        <svg viewBox="0 0 24 24"><rect x="4" y="5" width="16" height="16" rx="2" /><path d="M8 3v4m8-4v4M4 10h16" /></svg>
      </span>
      <div>
        <p>Total de provas</p>
        <strong>{{ totalExams }}</strong>
        <small>Todas as disciplinas</small>
      </div>
    </article>

    <article class="exams-summary-card is-green">
      <span class="summary-icon" aria-hidden="true">
        <svg viewBox="0 0 24 24"><rect x="4" y="5" width="16" height="16" rx="2" /><path d="M8 3v4m8-4v4M4 10h16M9 15l2 2 4-4" /></svg>
      </span>
      <div>
        <p>Próximas provas</p>
        <strong>{{ upcomingCount }}</strong>
        <small>Nos próximos 7 dias</small>
      </div>
    </article>

    <article class="exams-summary-card is-orange">
      <span class="summary-icon" aria-hidden="true">
        <svg viewBox="0 0 24 24"><path class="hourglass-shape" d="M6.5 3h11M6.5 21h11M8 3h8c0 4-1.6 5.7-4 7.5-2.4 1.8-4 3.5-4 7.5h8c0-4-1.6-5.7-4-7.5C9.6 8.7 8 7 8 3z" /><path class="hourglass-sand" d="M9.1 5.1h5.8c-.5 1.9-1.4 2.8-2.9 4-.4-.3-.8-.6-1.2-.9-1-.8-1.5-1.6-1.7-3.1zM9.1 18.9h5.8c-.5-1.9-1.4-2.8-2.9-4-.4.3-.8.6-1.2.9-1 .8-1.5 1.6-1.7 3.1z" /></svg>
      </span>
      <div>
        <p>Este mês</p>
        <strong>{{ thisMonthExams }}</strong>
        <small>{{ monthLabel }}</small>
      </div>
    </article>

    <article class="exams-summary-card is-blue">
      <span class="summary-icon" aria-hidden="true">
        <svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="9" /><path d="m8 12 3 3 5-6" /></svg>
      </span>
      <div>
        <p>Provas concluídas</p>
        <strong>{{ completedExams }}</strong>
        <small>{{ totalExams ? Math.round((completedExams / totalExams) * 100) : 0 }}% do total</small>
      </div>
    </article>
  </div>
</template>
