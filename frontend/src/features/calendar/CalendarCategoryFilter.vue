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

    <div class="filter-options">
      <label
        v-for="category in CATEGORIES"
        :key="category.value"
        :class="['filter-option', categoryClass(category.value)]"
      >
        <input
          type="checkbox"
          :checked="selected.includes(category.value)"
          @change="emit('toggle', category.value)"
        >
        {{ category.label }}
      </label>
    </div>
  </section>
</template>
