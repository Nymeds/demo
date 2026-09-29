<script setup>
import { WEEK_DAY_LABELS, categoryClass, dayCreateLabel, formatTime } from './calendarPresentation.js'

defineProps({
  days: { type: Array, required: true },
  viewMode: { type: String, required: true },
})

const emit = defineEmits(['create-event', 'edit-event', 'open-day'])
</script>

<template>
  <div class="calendar-weekdays" aria-hidden="true">
    <span v-for="label in WEEK_DAY_LABELS" :key="label">{{ label }}</span>
  </div>

  <div :class="['calendar-grid', `is-${viewMode}`]">
    <div
      v-for="day in days"
      :key="day.key"
      :class="['calendar-day', { 'is-outside': !day.isCurrentMonth, 'is-today': day.isToday }]"
    >
      <button
        type="button"
        class="calendar-day-create"
        :aria-label="dayCreateLabel(day.date)"
        @click="emit('create-event', day.date)"
      ></button>

      <span class="calendar-day-number">{{ day.number }}</span>

      <ul class="calendar-day-events">
        <li v-for="event in day.events" :key="event.id">
          <component
            :is="event.isExamActivity ? 'div' : 'button'"
            :type="event.isExamActivity ? undefined : 'button'"
            :class="['calendar-event', categoryClass(event.category), { 'is-readonly': event.isExamActivity }]"
            @click="event.isExamActivity ? null : emit('edit-event', event)"
          >
            <strong>{{ event.title }}<small v-if="event.isExamActivity" class="readonly-tag">Prova</small></strong>
            <small v-if="event.disciplineName" :class="{ 'is-deleted': event.disciplineDeleted }">{{ event.disciplineName }}</small>
            <small class="calendar-event-time">{{ formatTime(event.startsAt) }}</small>
          </component>
        </li>
      </ul>

      <button
        v-if="day.hiddenCount"
        type="button"
        class="calendar-day-more"
        :aria-label="`Ver os ${day.hiddenCount + day.events.length} eventos do dia ${day.number}`"
        @click="emit('open-day', day.date)"
      >
        mais {{ day.hiddenCount }}
      </button>
    </div>
  </div>
</template>
