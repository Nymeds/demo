<script setup>
defineProps({
  total: { type: Number, required: true },
  pending: { type: Number, required: true },
  inProgress: { type: Number, required: true },
  completed: { type: Number, required: true },
})
</script>

<template>
  <div class="activities-summary-grid">
    <article class="activities-summary-card is-purple">
      <span class="activities-summary-icon" aria-hidden="true">
        <svg viewBox="0 0 24 24">
          <rect x="5" y="4" width="14" height="17" rx="2" />
          <path d="M9 4V2m6 2V2M8 9h8" />
        </svg>
      </span>
      <div>
        <p>Total de atividades</p>
        <strong>{{ total }}</strong>
        <small>Todas as disciplinas</small>
      </div>
    </article>

    <article class="activities-summary-card is-orange">
      <span class="activities-summary-icon" aria-hidden="true">
        <svg viewBox="0 0 24 24">
          <circle cx="12" cy="12" r="9" />
          <path d="M12 7v5l3 2" />
        </svg>
      </span>
      <div>
        <p>Pendentes</p>
        <strong>{{ pending }}</strong>
        <small>Aguardando início</small>
      </div>
    </article>

    <article class="activities-summary-card is-blue">
      <span class="activities-summary-icon" aria-hidden="true">
        <svg viewBox="0 0 24 24">
          <path d="M4 12h16M12 4v16" />
        </svg>
      </span>
      <div>
        <p>Em andamento</p>
        <strong>{{ inProgress }}</strong>
        <small>Em execução</small>
      </div>
    </article>

    <article class="activities-summary-card is-green">
      <span class="activities-summary-icon" aria-hidden="true">
        <svg viewBox="0 0 24 24">
          <circle cx="12" cy="12" r="9" />
          <path d="m8 12 3 3 5-6" />
        </svg>
      </span>
      <div>
        <p>Concluídas</p>
        <strong>{{ completed }}</strong>
        <small>Finalizadas</small>
      </div>
    </article>
  </div>
</template>
