<script setup>
import {
  activityStatus,
  disciplineColorOf,
  disciplineNameOf,
  formatCompactDate,
} from './dashboardPresentation.js'

defineProps({
  activities: { type: Array, required: true },
  activitiesError: { type: String, default: '' },
  disciplines: { type: Array, required: true },
  todayIso: { type: String, required: true },
})

const emit = defineEmits(['navigate', 'retry'])
</script>

<template>
  <section class="dashboard-panel dashboard-side-activities" aria-labelledby="dashboard-activities-title">
    <header class="dashboard-panel-header">
      <h2 id="dashboard-activities-title">Atividades pendentes</h2>
      <button type="button" @click="emit('navigate', 'activities')">Ver todas <span aria-hidden="true">→</span></button>
    </header>

    <div v-if="activitiesError" class="dashboard-panel-empty dashboard-activities-error">
      <span aria-hidden="true">
        <svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="9" /><path d="M12 8v5m0 3v.01" /></svg>
      </span>
      <div>
        <strong>Não foi possível carregar as atividades</strong>
        <p>{{ activitiesError }}</p>
      </div>
      <button type="button" @click="emit('retry')">Tentar novamente</button>
    </div>

    <div v-else-if="activities.length === 0" class="dashboard-compact-empty">
      <span aria-hidden="true">✓</span>
      <p>Você não possui atividades pendentes.</p>
    </div>

    <ul v-else class="dashboard-compact-activity-list">
      <li v-for="activity in activities" :key="activity.id">
        <span
          class="dashboard-compact-activity-icon"
          :style="{ '--discipline-color': disciplineColorOf(disciplines, activity.disciplineId) }"
          aria-hidden="true"
        >
          <svg viewBox="0 0 24 24"><rect x="5" y="4" width="14" height="17" rx="2" /><path d="M9 4V2m6 2V2M8 9h8m-8 4h6" /></svg>
        </span>
        <div class="dashboard-activity-info">
          <strong>{{ activity.title }}</strong>
          <small>{{ disciplineNameOf(disciplines, activity.disciplineId) }}</small>
        </div>
        <div class="dashboard-compact-activity-meta">
          <time :datetime="activity.dueDate || undefined">{{ formatCompactDate(activity.dueDate) }}</time>
          <span :class="['dashboard-activity-status', activityStatus(activity, todayIso).className]">{{ activityStatus(activity, todayIso).label }}</span>
        </div>
      </li>
    </ul>
  </section>
</template>
