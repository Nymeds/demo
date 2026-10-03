<script setup>
import { CATEGORIES, categoryClass } from './calendarPresentation.js'

defineProps({
  selected: { type: Array, required: true },
})

const emit = defineEmits(['toggle'])
</script>

<template>
  <section class="side-card">
    <header class="side-card-header">
      <h2>Filtros</h2>
    </header>

    <div class="filter-options" role="group" aria-label="Filtrar por categoria">
      <button
        v-for="category in CATEGORIES"
        :key="category.value"
        type="button"
        :class="['filter-chip', categoryClass(category.value), { selected: selected.includes(category.value) }]"
        :aria-pressed="selected.includes(category.value)"
        @click="emit('toggle', category.value)"
      >
        <span aria-hidden="true" class="filter-chip-dot"></span>
        {{ category.label }}
      </button>
    </div>
  </section>
</template>
