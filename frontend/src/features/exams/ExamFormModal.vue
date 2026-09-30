<script setup>
import { computed, ref } from 'vue'
import AppDatePicker from '../../components/ui/AppDatePicker.vue'
import AppSelect from '../../components/ui/AppSelect.vue'
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

const disciplineOptions = computed(() => props.disciplines.map(discipline => ({
  value: discipline.id,
  label: disciplineLabel(props.disciplines, discipline),
})))

const submitted = ref(false)
const validationError = ref('')

function submitForm() {
  if (props.saving) return
  submitted.value = true
  if (!form.value.disciplineId || !form.value.date) {
    validationError.value = 'Preencha a disciplina e a data da prova.'
    return
  }
  validationError.value = ''
  emit('save', form.value)
}

useFocusTrap(() => true, modalRef, { onClose: () => emit('close'), closeOnEscape: () => !props.saving })
</script>

<template>
  <div class="app-modal-backdrop" @mousedown.self="emit('close')">
    <section ref="modalRef" class="app-modal exam-modal" role="dialog" aria-modal="true" aria-labelledby="new-exam-title" tabindex="-1">
      <header class="app-modal-header">
        <div class="exam-modal-title">
          <span class="exam-modal-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24"><rect x="5" y="3" width="14" height="18" rx="2" /><path d="M9 3v3h6V3M8 11h8M8 15h5" /></svg>
          </span>
          <div>
            <h2 id="new-exam-title">{{ isEditing ? 'Editar prova' : 'Nova prova' }}</h2>
            <p>{{ isEditing ? 'Atualize as informações da prova.' : 'Cadastre uma avaliação para acompanhar no calendário.' }}</p>
          </div>
        </div>
        <button class="app-modal-close" type="button" aria-label="Fechar modal" data-tooltip="Fechar" data-tooltip-position="bottom" @click="emit('close')">×</button>
      </header>

      <form novalidate @submit.prevent="submitForm">
        <label class="exam-form-field">
          <span>Nome da prova <strong>*</strong></span>
          <input v-model.trim="form.title" required maxlength="160" placeholder="Ex.: Prova 2 - Estruturas" :disabled="saving">
        </label>

        <div class="exam-form-grid">
          <label class="exam-form-field">
            <span>Disciplina <strong>*</strong></span>
            <AppSelect
              v-model="form.disciplineId"
              :options="disciplineOptions"
              :disabled="saving || isEditing"
              :invalid="submitted && !form.disciplineId"
              placeholder="Selecione uma disciplina"
            />
            <small v-if="isEditing">A disciplina não pode ser alterada após a criação.</small>
          </label>
          <label class="exam-form-field">
            <span>Data <strong>*</strong></span>
            <AppDatePicker v-model="form.date" :disabled="saving" :invalid="submitted && !form.date" />
          </label>
        </div>

        <label v-if="isEditing" class="exam-form-field">
          <span>Status <strong>*</strong></span>
          <AppSelect v-model="form.status" :options="STATUS_OPTIONS" :disabled="saving" />
        </label>

        <label class="exam-form-field">
          <span>Conteúdo <small>(opcional)</small></span>
          <textarea v-model.trim="form.content" rows="4" maxlength="2000" placeholder="Conteúdos que serão cobrados" :disabled="saving"></textarea>
        </label>

        <p v-if="validationError" class="app-field-error" role="alert">{{ validationError }}</p>
        <p v-if="errorMessage" class="app-field-error" role="alert">{{ errorMessage }}</p>

        <footer class="exam-modal-footer">
          <button class="exam-cancel-button" type="button" :disabled="saving" @click="emit('close')">Cancelar</button>
          <button class="exam-save-button" type="submit" :disabled="saving">
            {{ saving ? 'Salvando…' : isEditing ? 'Salvar alterações' : 'Salvar prova' }}
          </button>
        </footer>
      </form>
    </section>
  </div>
</template>
