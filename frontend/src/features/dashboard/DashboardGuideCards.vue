<script setup>
const emit = defineEmits(['navigate'])
</script>

<template>
  <div class="dashboard-guide-grid" aria-label="Próximos passos">
    <article class="dashboard-guide-card is-purple">
      <span class="dashboard-guide-icon" aria-hidden="true">
        <svg viewBox="0 0 24 24"><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2Z" /><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20M8 7h8M8 10h6" /></svg>
      </span>
      <div><h3>Adicione suas disciplinas</h3><p>Comece pelas matérias que você está cursando.</p><button type="button" @click="emit('navigate', 'disciplines')">Cadastrar disciplina <span aria-hidden="true">→</span></button></div>
    </article>
    <article class="dashboard-guide-card is-green">
      <span class="dashboard-guide-icon" aria-hidden="true">
        <svg viewBox="0 0 24 24"><rect x="5" y="4" width="14" height="17" rx="2" /><path d="M9 4V2m6 2V2M8 9h8m-8 4 2 2 4-4" /></svg>
      </span>
      <div><h3>Crie atividades</h3><p>Organize tarefas, trabalhos e compromissos.</p><button type="button" @click="emit('navigate', 'activities')">Gerenciar atividades <span aria-hidden="true">→</span></button></div>
    </article>
    <article class="dashboard-guide-card is-orange">
      <span class="dashboard-guide-icon" aria-hidden="true">
        <svg viewBox="0 0 24 24"><rect x="3" y="5" width="18" height="16" rx="2" /><path d="M7 3v4m10-4v4M3 10h18m5 4h4" /></svg>
      </span>
      <div><h3>Agende suas provas</h3><p>Organize as datas das suas avaliações.</p><button type="button" @click="emit('navigate', 'exams')">Ver provas <span aria-hidden="true">→</span></button></div>
    </article>
    <article class="dashboard-guide-card is-violet">
      <span class="dashboard-guide-icon" aria-hidden="true">
        <svg viewBox="0 0 24 24"><path d="M4 19v-5m5 5V9m5 10v-7m5 7V5" /><path d="m4 10 5-4 5 3 6-6" /></svg>
      </span>
      <div><h3>Acompanhe seu progresso</h3><p>Veja gráficos e estatísticas das suas notas.</p><button type="button" @click="emit('navigate', 'grades')">Ver notas <span aria-hidden="true">→</span></button></div>
    </article>
  </div>
</template>
