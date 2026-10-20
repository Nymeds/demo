<script setup>
import { categoryClass, formatTime } from './calendarPresentation.js'

defineProps({
  periodLabel: { type: String, required: true },
  events: { type: Array, required: true },
})

const emit = defineEmits(['edit-event'])
</script>

<template>
  <div class="day-view">
    <h2>{{ periodLabel }}</h2>

    <ul v-if="events.length" class="day-view-list">
      <li v-for="event in events" :key="event.id">
        <component
          :is="event.isExamActivity ? 'div' : 'button'"
          :type="event.isExamActivity ? undefined : 'button'"
          :class="['day-view-event', categoryClass(event.category), { 'is-readonly': event.isExamActivity }]"
          @click="event.isExamActivity ? null : emit('edit-event', event)"
        >
          <span class="day-view-time">
            {{ formatTime(event.startsAt) }}<template v-if="event.endsAt"> – {{ formatTime(event.endsAt) }}</template>
          </span>
          <span class="day-view-body">
            <strong>{{ event.title }}<small v-if="event.isExamActivity" class="readonly-tag">Prova</small></strong>
            <small v-if="event.disciplineName" :class="{ 'is-deleted': event.disciplineDeleted }">{{ event.disciplineName }}</small>
            <small v-if="event.description" class="day-view-description">{{ event.description }}</small>
          </span>
        </component>
      </li>
    </ul>

    <p v-else class="calendar-empty">Nenhum evento neste dia.</p>
  </div>
</template>
