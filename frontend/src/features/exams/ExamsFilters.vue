<script setup>
import { computed } from 'vue'

const props = defineProps({
  search: { type: String, required: true },
  discipline: { type: [String, Number], required: true },
  status: { type: String, required: true },
  period: { type: String, required: true },
  disciplines: { type: Array, required: true },
})

const emit = defineEmits(['update:search', 'update:discipline', 'update:status', 'update:period', 'filter-change'])

const searchModel = computed({
  get: () => props.search,
  set: value => emit('update:search', value),
})
const disciplineModel = computed({
  get: () => props.discipline,
  set: value => emit('update:discipline', value),
})
const statusModel = computed({
  get: () => props.status,
  set: value => emit('update:status', value),
})
const periodModel = computed({
  get: () => props.period,
  set: value => emit('update:period', value),
})
</script>

<template>
  <div class="exams-filters">
    <label class="exams-search">
      <span class="sr-only">Buscar prova</span>
      <svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="11" cy="11" r="7" /><path d="m20 20-3.5-3.5" /></svg>
      <input v-model="searchModel" type="search" placeholder="Buscar prova..." @input="emit('filter-change')">
    </label>

    <label>
      <span>Disciplinas</span>
      <select v-model="disciplineModel" @change="emit('filter-change')">
        <option value="all">Todas</option>
        <option v-for="discipline in disciplines" :key="discipline.id" :value="discipline.id">{{ discipline.name }}</option>
      </select>
    </label>

    <label>
      <span>Status</span>
      <select v-model="statusModel" @change="emit('filter-change')">
        <option value="all">Todos</option>
        <option value="scheduled">Agendadas</option>
        <option value="completed">Concluídas</option>
      </select>
    </label>

    <label>
      <span>Período</span>
      <select v-model="periodModel" @change="emit('filter-change')">
        <option value="all">Todos</option>
        <option value="next7">Próximos 7 dias</option>
        <option value="month">Este mês</option>
      </select>
    </label>
  </div>
</template>
