<script setup>
import {
  disciplineColorOf,
  disciplineNameOf,
  formatCompactDate,
  reminderKindLabel,
  reminderSectionFor,
} from './dashboardPresentation.js'

defineProps({
  groups: { type: Array, required: true },
  hiddenCount: { type: Number, default: 0 },
  disciplines: { type: Array, required: true },
})

const emit = defineEmits(['navigate'])
</script>

<template>
  <section class="dashboard-panel dashboard-reminders-panel" aria-labelledby="dashboard-reminders-title">
    <header class="dashboard-panel-header">
      <div>
        <span class="dashboard-eyebrow">Avisos</span>
        <h2 id="dashboard-reminders-title">Prazos se aproximando</h2>
      </div>
    </header>

    <div v-if="groups.length === 0" class="dashboard-compact-empty">
      <span aria-hidden="true">✓</span>
      <p>Nenhuma prova ou atividade dentro da antecedência configurada.</p>
    </div>

    <template v-else>
    <template v-for="group in groups" :key="group.key">
    <h3 class="dashboard-reminder-group">{{ group.label }}</h3>
    <ul class="dashboard-compact-activity-list">
      <li v-for="reminder in group.items" :key="reminder.id">
        <span
          class="dashboard-compact-activity-icon"
          :style="{ '--discipline-color': disciplineColorOf(disciplines, reminder.disciplineId) }"
          aria-hidden="true"
        >
          <svg viewBox="0 0 24 24"><path d="M12 3a9 9 0 1 0 9 9" /><path d="M12 7v5l3 2" /></svg>
        </span>
        <div class="dashboard-activity-info">
          <strong>{{ reminder.title }}</strong>
          <small>{{ reminderKindLabel(reminder) }} · {{ disciplineNameOf(disciplines, reminder.disciplineId) }}</small>
        </div>
        <div class="dashboard-compact-activity-meta">
          <time :datetime="reminder.dueDate || undefined">{{ formatCompactDate(reminder.dueDate) }}</time>
          <button
            type="button"
            class="dashboard-reminder-link"
            :aria-label="`Ver ${reminderKindLabel(reminder).toLowerCase()} ${reminder.title}`"
            @click="emit('navigate', reminderSectionFor(reminder))"
          >
            Ver <span aria-hidden="true">→</span>
          </button>
        </div>
      </li>
    </ul>
    </template>
    </template>
    <button
      v-if="hiddenCount > 0"
      type="button"
      class="dashboard-reminder-more"
      @click="emit('navigate', 'activities')"
    >
      Ver todas em Atividades (+{{ hiddenCount }}) <span aria-hidden="true">→</span>
    </button>
  </section>
</template>
