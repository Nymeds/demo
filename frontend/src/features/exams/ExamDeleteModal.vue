<script setup>
import { ref } from 'vue'
import { useFocusTrap } from '../../shared/a11y/useFocusTrap.js'

const props = defineProps({
  exam: { type: Object, required: true },
  deleting: { type: Boolean, default: false },
  errorMessage: { type: String, default: '' },
})

const emit = defineEmits(['close', 'confirm'])

const modalRef = ref(null)

useFocusTrap(() => true, modalRef, { onClose: () => emit('close'), closeOnEscape: () => !props.deleting })
</script>

<template>
  <div class="app-modal-backdrop" @mousedown.self="!deleting && emit('close')">
    <section ref="modalRef" class="app-modal exam-modal exam-delete-modal" role="dialog" aria-modal="true" aria-labelledby="delete-exam-title" tabindex="-1">
      <header class="app-modal-header">
        <div class="exam-modal-title">
          <span class="exam-modal-icon is-danger" aria-hidden="true">
            <svg viewBox="0 0 24 24"><path d="M4 7h16M9 7V4h6v3M6 7l1 13h10l1-13M10 11v6M14 11v6" /></svg>
          </span>
          <div>
            <h2 id="delete-exam-title">Excluir prova</h2>
            <p>Confirme a exclusão da avaliação.</p>
          </div>
        </div>
        <button class="app-modal-close" type="button" aria-label="Fechar modal" data-tooltip="Fechar" data-tooltip-position="bottom" :disabled="deleting" @click="emit('close')">×</button>
      </header>

      <p class="exam-delete-text">
        Tem certeza que deseja excluir a prova <strong>{{ exam.title }}</strong>? Esta ação não pode ser desfeita.
      </p>

      <p v-if="errorMessage" class="app-field-error" role="alert">{{ errorMessage }}</p>

      <footer class="exam-modal-footer">
        <button class="exam-cancel-button" type="button" :disabled="deleting" @click="emit('close')">Cancelar</button>
        <button class="exam-save-button is-danger" type="button" :disabled="deleting" @click="emit('confirm')">
          {{ deleting ? 'Excluindo…' : 'Excluir' }}
        </button>
      </footer>
    </section>
  </div>
</template>
