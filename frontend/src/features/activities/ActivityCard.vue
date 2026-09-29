<script setup>
import { formatDate, isOverdue, statusDetails } from './activitiesPresentation.js'

defineProps({
  activity: { type: Object, required: true },
  busy: { type: Boolean, default: false },
})

const emit = defineEmits(['complete', 'edit', 'delete'])
</script>

<template>
  <article
    class="activity-card"
    :class="{ 'is-overdue': isOverdue(activity) }"
  >
    <span
      class="activity-discipline-bar"
      :style="{ background: activity.disciplineColor }"
      aria-hidden="true"
    ></span>

    <div class="activity-main">
      <div class="activity-title-row">
        <div>
          <span
            class="activity-discipline"
            :style="{
              '--discipline-color': activity.disciplineColor,
            }"
          >
            <span aria-hidden="true"></span>
            {{ activity.disciplineName }}
          </span>

          <h2>
            {{ activity.title }}
            <span v-if="activity.type === 'EXAM'" class="activity-type-badge">Prova</span>
          </h2>
        </div>

      </div>

      <p v-if="activity.description" class="activity-description">
        {{ activity.description }}
      </p>

      <div class="activity-meta">
        <span :class="{ overdue: isOverdue(activity) }">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <rect x="3" y="5" width="18" height="16" rx="2" />
            <path d="M7 3v4m10-4v4M3 10h18" />
          </svg>
          {{ isOverdue(activity) ? 'Atrasada: ' : 'Entrega: ' }}
          {{ formatDate(activity.dueDate) }}
        </span>
      </div>
    </div>

    <div class="activity-card-actions">
      <span
        class="activity-status"
        :class="statusDetails(activity.status).className"
      >
        {{ statusDetails(activity.status).label }}
      </span>
      <button
        v-if="activity.status !== 'COMPLETED'"
        class="activity-complete"
        type="button"
        title="Marcar como concluída"
        :disabled="busy"
        @click="emit('complete', activity)"
      >
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="m5 12 4 4 10-10" />
        </svg>
        Concluir
      </button>

      <button
        class="activity-icon-button"
        type="button"
        title="Editar atividade"
        aria-label="Editar atividade"
        @click="emit('edit', activity)"
      >
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="m4 20 4-1 11-11-3-3L5 16l-1 4Z" />
          <path d="m14 7 3 3" />
        </svg>
      </button>

      <button
        class="activity-icon-button is-delete"
        type="button"
        title="Excluir atividade"
        aria-label="Excluir atividade"
        @click="emit('delete', activity)"
      >
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M4 7h16M9 7V4h6v3m3 0-1 13H7L6 7m4 4v5m4-5v5" />
        </svg>
      </button>
    </div>
  </article>
</template>
