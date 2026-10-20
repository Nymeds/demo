<script setup>
import { MINI_WEEK_DAY_LABELS, categoryClass } from './calendarPresentation.js'

defineProps({
  label: { type: String, required: true },
  days: { type: Array, required: true },
})

const emit = defineEmits(['move', 'select-day'])
</script>

<template>
  <section class="side-card">
    <header class="mini-calendar-header">
      <button type="button" aria-label="Mês anterior" @click="emit('move', -1)">‹</button>
      <strong>{{ label }}</strong>
      <button type="button" aria-label="Próximo mês" @click="emit('move', 1)">›</button>
    </header>

    <div class="mini-calendar-weekdays" aria-hidden="true">
      <span v-for="(dayLabel, index) in MINI_WEEK_DAY_LABELS" :key="index">{{ dayLabel }}</span>
    </div>

    <div class="mini-calendar-grid">
      <button
        v-for="day in days"
        :key="day.key"
        type="button"
        :class="['mini-calendar-day', {
          'is-outside': !day.isCurrentMonth,
          'is-today': day.isToday,
          'is-selected': day.isSelected,
        }]"
        @click="emit('select-day', day.date)"
      >
        {{ day.number }}
        <span class="mini-calendar-dots" aria-hidden="true">
          <span v-for="dot in day.dots" :key="dot" :class="['mini-dot', categoryClass(dot)]"></span>
        </span>
      </button>
    </div>
  </section>
</template>
