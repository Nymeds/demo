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
  <div class="exam-modal-overlay" @click.self="emit('close')">
    <section ref="modalRef" class="exam-modal exam-delete-modal" role="dialog" aria-modal="true" aria-labelledby="delete-exam-title" tabindex="-1">
      <header>
        <div>
          <span class="modal-kicker">Excluir prova</span>
          <h2 id="delete-exam-title">Confirmar exclusão</h2>
        </div>
        <button type="button" aria-label="Fechar" :disabled="deleting" @click="emit('close')">×</button>
      </header>

      <p class="exam-delete-text">
        Tem certeza que deseja excluir a prova <strong>{{ exam.title }}</strong>? Esta ação não pode ser desfeita.
      </p>

      <p v-if="errorMessage" class="exams-request-error" role="alert">{{ errorMessage }}</p>

      <footer>
        <button class="exams-button is-secondary" type="button" :disabled="deleting" @click="emit('close')">Cancelar</button>
        <button class="exams-button is-danger" type="button" :disabled="deleting" @click="emit('confirm')">
          {{ deleting ? 'Excluindo...' : 'Excluir' }}
        </button>
      </footer>
    </section>
  </div>
</template>
