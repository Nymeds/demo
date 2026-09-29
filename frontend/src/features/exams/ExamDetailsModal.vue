<script setup>
import { ref } from 'vue'
import { useFocusTrap } from '../../shared/a11y/useFocusTrap.js'
import { formatDate, statusLabel } from './examPresentation.js'

defineProps({
  exam: { type: Object, required: true },
  now: { type: Date, required: true },
})

const emit = defineEmits(['close', 'edit'])

const modalRef = ref(null)

useFocusTrap(() => true, modalRef, { onClose: () => emit('close') })
</script>

<template>
  <div class="exam-modal-overlay" @click.self="emit('close')">
    <section ref="modalRef" class="exam-modal exam-details-modal" role="dialog" aria-modal="true" aria-labelledby="exam-details-title" tabindex="-1">
      <header>
        <div>
          <span class="modal-kicker">Detalhes da prova</span>
          <h2 id="exam-details-title">{{ exam.title }}</h2>
        </div>
        <button type="button" aria-label="Fechar" @click="emit('close')">×</button>
      </header>

      <div class="exam-details-grid">
        <div>
          <span>Disciplina</span>
          <strong>{{ exam.disciplineName }}</strong>
        </div>
        <div>
          <span>Data</span>
          <strong>{{ formatDate(exam.dueDate) }}</strong>
        </div>
        <div>
          <span>Status</span>
          <strong>{{ statusLabel(exam, now) }}</strong>
        </div>
        <div class="exam-details-full">
          <span>Conteúdo</span>
          <strong>{{ exam.description || 'Conteúdo não informado' }}</strong>
        </div>
      </div>

      <footer>
        <button class="exams-button is-secondary" type="button" @click="emit('edit', exam)">Editar</button>
        <button class="exams-button is-primary" type="button" @click="emit('close')">Fechar</button>
      </footer>
    </section>
  </div>
</template>
