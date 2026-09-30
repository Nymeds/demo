<script setup>
import { WEEK_DAY_LABELS, categoryClass, dayCreateLabel, daySelectLabel, formatTime } from './calendarPresentation.js'

defineProps({
  days: { type: Array, required: true },
  viewMode: { type: String, required: true },
})

const emit = defineEmits(['create-event', 'edit-event', 'open-day', 'select-day'])
</script>

<template>
  <div :class="['calendar-weekdays', `is-${viewMode}`]" aria-hidden="true">
    <span v-for="label in WEEK_DAY_LABELS" :key="label">{{ label }}</span>
  </div>

  <div :class="['calendar-grid', `is-${viewMode}`]">
    <div
      v-for="day in days"
      :key="day.key"
      :class="['calendar-day', { 'is-outside': !day.isCurrentMonth, 'is-today': day.isToday, 'is-selected': day.isSelected }]"
    >
      <!-- Só no celular (calendar.css, até 760px): a célula do Mês vira o número com marcadores e
           tocar nela seleciona o dia, cuja agenda aparece abaixo da grade. -->
      <button
        v-if="viewMode === 'month'"
        type="button"
        class="calendar-day-select"
        :aria-label="daySelectLabel(day.date, day.totalCount)"
        :aria-pressed="day.isSelected"
        :aria-current="day.isToday ? 'date' : undefined"
        @click="emit('select-day', day.date)"
      >
        <span class="calendar-day-select-number">{{ day.number }}</span>
        <span class="calendar-day-dots" aria-hidden="true">
          <span v-for="dot in day.dots" :key="dot" :class="['calendar-day-dot', categoryClass(dot)]"></span>
        </span>
      </button>

      <button
        type="button"
        class="calendar-day-create"
        :aria-label="dayCreateLabel(day.date)"
        @click="emit('create-event', day.date)"
      ></button>

      <!-- Só no celular: na Semana em lista, cada bloco precisa dizer o dia da semana. -->
      <span v-if="viewMode === 'week'" class="calendar-day-weekday" aria-hidden="true">{{ WEEK_DAY_LABELS[day.date.getDay()] }}</span>
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
