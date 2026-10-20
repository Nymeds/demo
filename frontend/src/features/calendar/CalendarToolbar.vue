<script setup>
defineProps({
  periodLabel: { type: String, required: true },
  viewMode: { type: String, required: true },
})

const emit = defineEmits(['today', 'move', 'change-view'])
</script>

<template>
  <div class="calendar-toolbar">
    <button class="today-button" type="button" @click="emit('today')">Hoje</button>

    <div class="period-navigation">
      <button type="button" aria-label="Período anterior" @click="emit('move', -1)">‹</button>
      <strong>{{ periodLabel }}</strong>
      <button type="button" aria-label="Próximo período" @click="emit('move', 1)">›</button>
    </div>

    <div class="view-switch" role="group" aria-label="Modo de visualização">
      <button type="button" :class="{ active: viewMode === 'month' }" @click="emit('change-view', 'month')">Mês</button>
      <button type="button" :class="{ active: viewMode === 'week' }" @click="emit('change-view', 'week')">Semana</button>
      <button type="button" :class="{ active: viewMode === 'day' }" @click="emit('change-view', 'day')">Dia</button>
    </div>
  </div>
</template>
