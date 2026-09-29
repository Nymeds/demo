<script setup>
defineProps({
  completionPercentage: { type: Number, default: 0 },
  completedCount: { type: Number, default: 0 },
  totalCount: { type: Number, default: 0 },
  pendingCount: { type: Number, default: 0 },
  overdueCount: { type: Number, default: 0 },
})
</script>

<template>
  <section class="dashboard-panel dashboard-progress-panel" aria-labelledby="dashboard-progress-title">
    <span class="dashboard-eyebrow">Seu ritmo</span>
    <h2 id="dashboard-progress-title">Progresso das atividades</h2>
    <div class="dashboard-progress-value"><strong>{{ completionPercentage }}%</strong><span>{{ completedCount }} de {{ totalCount }} concluídas</span></div>
    <div class="dashboard-progress-track" aria-hidden="true"><span :style="{ width: `${completionPercentage}%` }"></span></div>
    <div class="dashboard-progress-meta"><span><strong>{{ pendingCount }}</strong> pendentes</span><span :class="{ 'has-overdue': overdueCount > 0 }"><strong>{{ overdueCount }}</strong> atrasadas</span></div>
  </section>
</template>
