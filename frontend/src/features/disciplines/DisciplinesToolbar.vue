<script setup>
import { computed } from 'vue'
import AppSelect from '../../components/ui/AppSelect.vue'
import { FILTERS, SORT_OPTIONS } from './disciplinesPresentation.js'

const props = defineProps({
  activeFilter: { type: String, required: true },
  sortOrder: { type: String, required: true },
  viewMode: { type: String, required: true },
})

const emit = defineEmits(['update:activeFilter', 'update:sortOrder', 'update:viewMode', 'clear'])

const sort = computed({
  get: () => props.sortOrder,
  set: value => emit('update:sortOrder', value),
})
</script>

<template>
  <div class="disciplines-toolbar">
    <div class="disciplines-filters" aria-label="Filtrar disciplinas">
      <button
        v-for="filter in FILTERS"
        :key="filter.value"
        type="button"
        :class="{ active: activeFilter === filter.value }"
        @click="emit('update:activeFilter', filter.value)"
      >
        {{ filter.label }}
      </button>
    </div>

    <div class="disciplines-view-options">
      <label class="disciplines-sort">
        <span>Ordenar por:</span>
        <AppSelect v-model="sort" :options="SORT_OPTIONS" variant="ghost" aria-label="Ordenar disciplinas" />
      </label>

      <button class="clear-filters" type="button" @click="emit('clear')">Limpar filtros</button>

      <div class="disciplines-view-buttons" aria-label="Modo de visualização">
        <button type="button" :class="{ active: viewMode === 'list' }" aria-label="Visualização em lista" @click="emit('update:viewMode', 'list')">
          <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M9 6h11M9 12h11M9 18h11" /><circle cx="4" cy="6" r="1" /><circle cx="4" cy="12" r="1" /><circle cx="4" cy="18" r="1" /></svg>
        </button>
        <button type="button" :class="{ active: viewMode === 'grid' }" aria-label="Visualização em grade" @click="emit('update:viewMode', 'grid')">
          <svg viewBox="0 0 24 24" aria-hidden="true"><rect x="4" y="4" width="6" height="6" /><rect x="14" y="4" width="6" height="6" /><rect x="4" y="14" width="6" height="6" /><rect x="14" y="14" width="6" height="6" /></svg>
        </button>
      </div>
    </div>
  </div>
</template>
