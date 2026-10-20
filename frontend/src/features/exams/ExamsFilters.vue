<script setup>
import { computed } from 'vue'
import AppSelect from '../../components/ui/AppSelect.vue'

const props = defineProps({
  search: { type: String, required: true },
  discipline: { type: [String, Number], required: true },
  status: { type: String, required: true },
  period: { type: String, required: true },
  disciplines: { type: Array, required: true },
})

const emit = defineEmits(['update:search', 'update:discipline', 'update:status', 'update:period', 'filter-change'])

const disciplineOptions = computed(() => [
  { value: 'all', label: 'Todas' },
  ...props.disciplines.map(discipline => ({ value: discipline.id, label: discipline.name })),
])
const statusOptions = [
  { value: 'all', label: 'Todos' },
  { value: 'scheduled', label: 'Agendadas' },
  { value: 'completed', label: 'Concluídas' },
]
const periodOptions = [
  { value: 'all', label: 'Todos' },
  { value: 'next7', label: 'Próximos 7 dias' },
  { value: 'month', label: 'Este mês' },
]

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

    <div class="exams-filter-field">
      <span>Disciplinas</span>
      <AppSelect v-model="disciplineModel" :options="disciplineOptions" aria-label="Filtrar por disciplina" @update:model-value="emit('filter-change')" />
    </div>

    <div class="exams-filter-field">
      <span>Status</span>
      <AppSelect v-model="statusModel" :options="statusOptions" aria-label="Filtrar por status" @update:model-value="emit('filter-change')" />
    </div>

    <div class="exams-filter-field">
      <span>Período</span>
      <AppSelect v-model="periodModel" :options="periodOptions" aria-label="Filtrar por período" @update:model-value="emit('filter-change')" />
    </div>
  </div>
</template>
