<script setup>
import { computed, ref } from 'vue'
import { useFocusTrap } from '../../shared/a11y/useFocusTrap.js'
import { disciplineLabel } from './examPresentation.js'

const STATUS_OPTIONS = [
  { value: 'PENDING', label: 'Agendada' },
  { value: 'IN_PROGRESS', label: 'Em andamento' },
  { value: 'COMPLETED', label: 'Concluída' },
]

const props = defineProps({
  exam: { type: Object, default: null },
  disciplines: { type: Array, required: true },
  saving: { type: Boolean, default: false },
  errorMessage: { type: String, default: '' },
})

const emit = defineEmits(['close', 'save'])

const modalRef = ref(null)
const isEditing = computed(() => !!props.exam)

const form = ref(props.exam
  ? {
      title: props.exam.title,
      disciplineId: props.exam.disciplineId,
      date: props.exam.dueDate,
      content: props.exam.description || '',
      status: props.exam.status,
    }
  : {
      title: '',
      disciplineId: '',
      date: '',
      content: '',
      status: 'PENDING',
    })

useFocusTrap(() => true, modalRef, { onClose: () => emit('close'), closeOnEscape: () => !props.saving })
</script>

<template>
  <div class="exam-modal-overlay" @click.self="emit('close')">
    <section ref="modalRef" class="exam-modal" role="dialog" aria-modal="true" aria-labelledby="new-exam-title" tabindex="-1">
      <header>
        <div>
          <span class="modal-kicker">{{ isEditing ? 'Editar avaliação' : 'Nova avaliação' }}</span>
          <h2 id="new-exam-title">{{ isEditing ? 'Editar prova' : 'Cadastrar prova' }}</h2>
        </div>
        <button type="button" aria-label="Fechar" @click="emit('close')">×</button>
      </header>

      <form @submit.prevent="emit('save', form)">
        <label>
          Nome da prova
          <input v-model.trim="form.title" required placeholder="Ex.: Prova 2 - Estruturas" :disabled="saving">
        </label>

        <div class="modal-grid">
          <label>
            Disciplina
            <select v-model="form.disciplineId" required :disabled="saving || isEditing">
              <option value="" disabled>Selecione</option>
              <option v-for="discipline in disciplines" :key="discipline.id" :value="discipline.id">{{ disciplineLabel(disciplines, discipline) }}</option>
            </select>
            <small v-if="isEditing" class="field-hint">A disciplina não pode ser alterada após a criação.</small>
          </label>
          <label>
            Data
            <input v-model="form.date" type="date" required :disabled="saving">
          </label>
        </div>

        <label v-if="isEditing">
          Status
          <select v-model="form.status" :disabled="saving">
            <option v-for="option in STATUS_OPTIONS" :key="option.value" :value="option.value">{{ option.label }}</option>
          </select>
        </label>

        <label>
          Conteúdo
          <textarea v-model.trim="form.content" rows="3" placeholder="Conteúdos que serão cobrados" :disabled="saving"></textarea>
        </label>

        <p v-if="errorMessage" class="exams-request-error" role="alert">{{ errorMessage }}</p>

        <footer>
          <button class="exams-button is-secondary" type="button" :disabled="saving" @click="emit('close')">Cancelar</button>
          <button class="exams-button is-primary" type="submit" :disabled="saving">
            {{ saving ? 'Salvando...' : isEditing ? 'Salvar alterações' : 'Salvar prova' }}
          </button>
        </footer>
      </form>
    </section>
  </div>
</template>
