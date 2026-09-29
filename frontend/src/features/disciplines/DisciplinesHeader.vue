<script setup>
import { computed } from 'vue'

const props = defineProps({
  searchTerm: { type: String, default: '' },
  addDisabled: { type: Boolean, default: false },
})

const emit = defineEmits(['update:searchTerm', 'add'])

const search = computed({
  get: () => props.searchTerm,
  set: value => emit('update:searchTerm', value),
})
</script>

<template>
  <header class="disciplines-header">
    <div class="disciplines-heading">
      <span class="disciplines-heading-icon" aria-hidden="true">
        <svg viewBox="0 0 24 24">
          <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2Z" />
          <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20M8 7h8M8 10h6" />
        </svg>
      </span>
      <div>
        <h1 id="disciplines-title">Disciplinas</h1>
        <p>Gerencie suas disciplinas e acompanhe seu desempenho.</p>
      </div>
    </div>

    <div class="disciplines-actions">
      <label class="disciplines-search">
        <span class="sr-only">Buscar disciplina</span>
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <circle cx="11" cy="11" r="7" />
          <path d="m20 20-4-4" />
        </svg>
        <input v-model="search" type="search" placeholder="Buscar disciplina...">
      </label>

      <button class="disciplines-add-button" type="button" :disabled="addDisabled" @click="emit('add')">
        <span aria-hidden="true">＋</span>
        Nova disciplina
      </button>
    </div>
  </header>
</template>
