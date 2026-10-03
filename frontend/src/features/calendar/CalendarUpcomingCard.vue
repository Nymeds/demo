<script setup>
import { UPCOMING_DISPLAY_LIMIT, categoryClass, formatTime, upcomingDayLabel } from './calendarPresentation.js'

defineProps({
  events: { type: Array, required: true },
  totalCount: { type: Number, required: true },
  showAll: { type: Boolean, required: true },
  error: { type: String, default: '' },
  stale: { type: Boolean, default: false },
  today: { type: Date, required: true },
})

const emit = defineEmits(['toggle-limit', 'retry', 'edit-event'])
</script>

<template>
  <section class="side-card">
    <header class="side-card-header">
      <h2>Próximos eventos</h2>
      <button
        v-if="totalCount > UPCOMING_DISPLAY_LIMIT"
        type="button"
        class="side-card-action"
        @click="emit('toggle-limit')"
      >
        {{ showAll ? 'Ver menos' : 'Ver todos' }}
      </button>
    </header>

    <div v-if="error" class="calendar-section-error is-inline" role="alert">
      <p>{{ error }}<template v-if="stale"> Esta lista pode estar desatualizada.</template></p>
      <button type="button" class="retry-button" @click="emit('retry')">Tentar novamente</button>
    </div>

    <ul v-if="events.length" class="upcoming-list">
      <li v-for="event in events" :key="event.id">
        <component
          :is="event.isExamActivity ? 'div' : 'button'"
          :type="event.isExamActivity ? undefined : 'button'"
          :class="['upcoming-item', { 'is-readonly': event.isExamActivity }]"
          @click="event.isExamActivity ? null : emit('edit-event', event)"
        >
          <span :class="['upcoming-dot', categoryClass(event.category)]" aria-hidden="true"></span>
          <span class="upcoming-body">
            <strong>{{ event.title }}<small v-if="event.isExamActivity" class="readonly-tag">Prova</small></strong>
            <small v-if="event.disciplineName" :class="{ 'is-deleted': event.disciplineDeleted }">{{ event.disciplineName }}</small>
          </span>
          <span class="upcoming-when">
            <strong>{{ upcomingDayLabel(event.startsAt, today) }}</strong>
            <small>{{ formatTime(event.startsAt) }}</small>
          </span>
        </component>
      </li>
    </ul>

    <p v-else-if="!error" class="calendar-empty">Nenhum evento programado.</p>

    <p v-if="totalCount > events.length" class="upcoming-count-hint">
      Mostrando {{ events.length }} de {{ totalCount }}
    </p>
  </section>
</template>
