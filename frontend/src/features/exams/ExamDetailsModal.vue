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
  <div class="app-modal-backdrop" @mousedown.self="emit('close')">
    <section ref="modalRef" class="app-modal exam-modal exam-details-modal" role="dialog" aria-modal="true" aria-labelledby="exam-details-title" tabindex="-1">
      <header class="app-modal-header">
        <div class="exam-modal-title">
          <span class="exam-modal-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24"><rect x="5" y="3" width="14" height="18" rx="2" /><path d="M9 3v3h6V3M8 11h8M8 15h5" /></svg>
          </span>
          <div>
            <h2 id="exam-details-title">{{ exam.title }}</h2>
            <p>Detalhes da prova</p>
          </div>
        </div>
        <button class="app-modal-close" type="button" aria-label="Fechar modal" data-tooltip="Fechar" data-tooltip-position="bottom" @click="emit('close')">×</button>
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

      <footer class="exam-modal-footer">
        <button class="exam-cancel-button" type="button" @click="emit('edit', exam)">Editar</button>
        <button class="exam-save-button" type="button" @click="emit('close')">Fechar</button>
      </footer>
    </section>
  </div>
</template>
