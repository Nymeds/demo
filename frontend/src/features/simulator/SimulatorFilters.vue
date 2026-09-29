<script setup>
import { ref } from 'vue'
import AppSelect from '../../components/ui/AppSelect.vue'

defineProps({
  discipline: { type: [String, Number], default: '' },
  period: { type: String, default: '' },
  disciplineOptions: { type: Array, required: true },
  periodOptions: { type: Array, required: true },
})

const emit = defineEmits(['update:discipline', 'update:period'])

const disciplineSelect = ref(null)

defineExpose({ focusDiscipline: () => disciplineSelect.value?.focus() })
</script>

<template>
  <!-- FILTROS -->
  <section class="filters-card">

    <!-- DISCIPLINA -->
    <div class="filter">

      <label for="simulator-discipline">Disciplina</label>

      <AppSelect
        id="simulator-discipline"
        ref="disciplineSelect"
        :model-value="discipline"
        :options="disciplineOptions"
        placeholder="Selecione uma disciplina"
        @update:model-value="emit('update:discipline', $event)"
      >
        <template #leading>
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M4 5.5A2.5 2.5 0 0 1 6.5 3H20v16H6.5A2.5 2.5 0 0 0 4 21.5z"/>
            <path d="M4 5.5v16"/>
          </svg>
        </template>
      </AppSelect>

    </div>


    <!-- PERÍODO -->
    <div class="filter">

      <label for="simulator-period">Período/Ano</label>

      <AppSelect
        id="simulator-period"
        :model-value="period"
        :options="periodOptions"
        @update:model-value="emit('update:period', $event)"
      >
        <template #leading>
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="3" y="5" width="18" height="16" rx="2"/>
            <line x1="16" y1="3" x2="16" y2="7"/>
            <line x1="8" y1="3" x2="8" y2="7"/>
            <line x1="3" y1="10" x2="21" y2="10"/>
          </svg>
        </template>
      </AppSelect>

    </div>

  </section>
</template>
