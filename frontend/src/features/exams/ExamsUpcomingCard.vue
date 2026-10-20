<script setup>
import { NEXT_DAYS_WINDOW } from './examFilters.js'
import { formatShortDate } from './examPresentation.js'

defineProps({
  exams: { type: Array, required: true },
})

const emit = defineEmits(['view', 'show-all'])
</script>

<template>
  <section class="upcoming-card">
    <header>
      <strong>Próximas provas</strong>
      <button type="button" @click="emit('show-all')">Ver todas</button>
    </header>

    <p v-if="exams.length === 0" class="upcoming-empty">Nenhuma prova nos próximos {{ NEXT_DAYS_WINDOW }} dias.</p>

    <button v-for="(exam, index) in exams" :key="exam.id" class="upcoming-item" type="button" @click="emit('view', exam)">
      <span class="upcoming-number" :class="`is-${exam.color}`">{{ index + 1 }}</span>
      <span class="upcoming-details">
        <strong>{{ exam.title }}</strong>
        <small>{{ exam.disciplineName }}</small>
      </span>
      <span class="upcoming-date">
        <strong>{{ formatShortDate(exam.dueDate) }}</strong>
      </span>
    </button>
  </section>
</template>
