<script setup>
defineProps({
  monthLabel: { type: String, required: true },
  days: { type: Array, required: true },
})

const emit = defineEmits(['previous-month', 'next-month'])
</script>

<template>
  <section class="mini-calendar">
    <header>
      <button type="button" aria-label="Mês anterior" @click="emit('previous-month')">‹</button>
      <strong>{{ monthLabel }}</strong>
      <button type="button" aria-label="Próximo mês" @click="emit('next-month')">›</button>
    </header>

    <div class="calendar-weekdays">
      <span v-for="day in ['Dom', 'Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb']" :key="day">{{ day }}</span>
    </div>

    <div class="calendar-grid">
      <span
        v-for="day in days"
        :key="day.key"
        :class="['calendar-day', day.state]"
      >{{ day.number }}</span>
    </div>

    <div class="calendar-legend">
      <span><i class="legend-dot is-today"></i>Hoje</span>
      <span><i class="legend-dot is-upcoming"></i>Agendada</span>
      <span><i class="legend-dot is-overdue"></i>Atrasada</span>
      <span><i class="legend-dot is-completed"></i>Concluída</span>
    </div>
  </section>
</template>
