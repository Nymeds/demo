<script setup>
import { computed, ref } from 'vue'

const props = defineProps({
  activity: { type: Object, default: null },
  disciplines: { type: Array, required: true },
  saving: { type: Boolean, default: false },
})

const emit = defineEmits(['close', 'save'])

const isEditing = computed(() => Boolean(props.activity))

const disciplineId = ref(
  props.activity?.disciplineId
    || props.disciplines[0]?.id
    || '',
)

const title = ref(props.activity?.title ?? '')
const description = ref(props.activity?.description ?? '')
const dueDate = ref(props.activity?.dueDate ?? '')
const status = ref(props.activity?.status ?? 'PENDING')

const statuses = [
  { value: 'PENDING', label: 'Pendente' },
  { value: 'IN_PROGRESS', label: 'Em andamento' },
  { value: 'COMPLETED', label: 'Concluída' },
]

function submitForm() {
  emit('save', {
    disciplineId: disciplineId.value,
    title: title.value.trim(),
    description: description.value.trim(),
    dueDate: dueDate.value,
    status: status.value,
  })
}
</script>

<template>
  <div class="activity-modal-backdrop" @mousedown.self="emit('close')">
    <section
      class="activity-modal"
      role="dialog"
      aria-modal="true"
      aria-labelledby="activity-modal-title"
    >
      <header class="activity-modal-header">
        <div class="activity-modal-title">
          <span aria-hidden="true">
            <svg viewBox="0 0 24 24">
              <rect x="5" y="4" width="14" height="17" rx="2" />
              <path d="M9 4V2m6 2V2M8 9h8m-8 4 2 2 4-4" />
            </svg>
          </span>

          <div>
            <h2 id="activity-modal-title">
              {{ isEditing ? 'Editar atividade' : 'Nova atividade' }}
            </h2>
            <p>
              {{
                isEditing
                  ? 'Atualize as informações da atividade.'
                  : 'Cadastre uma tarefa, trabalho ou compromisso acadêmico.'
              }}
            </p>
          </div>
        </div>

        <button
          class="activity-modal-close"
          type="button"
          aria-label="Fechar modal"
          @click="emit('close')"
        >
          ×
        </button>
      </header>

      <form @submit.prevent="submitForm">
        <label class="activity-form-field">
          <span>Disciplina <strong>*</strong></span>
          <select v-model="disciplineId" required :disabled="isEditing">
            <option disabled value="">Selecione uma disciplina</option>
            <option
              v-for="discipline in disciplines"
              :key="discipline.id"
              :value="discipline.id"
            >
              {{ discipline.name }}
            </option>
          </select>
          <small v-if="isEditing">
            Para mover a atividade para outra disciplina, crie uma nova atividade.
          </small>
        </label>

        <label class="activity-form-field">
          <span>Título <strong>*</strong></span>
          <input
            v-model="title"
            type="text"
            maxlength="160"
            placeholder="Ex.: Entregar trabalho de Engenharia de Software"
            required
            autofocus
          >
        </label>

        <label class="activity-form-field">
          <span>Descrição <small>(opcional)</small></span>
          <textarea
            v-model="description"
            maxlength="2000"
            rows="5"
            placeholder="Adicione detalhes importantes sobre a atividade..."
          ></textarea>
          <small>{{ description.length }}/2000 caracteres</small>
        </label>

        <div class="activity-form-grid">
          <label class="activity-form-field">
            <span>Data de entrega <strong>*</strong></span>
            <input v-model="dueDate" type="date" required>
          </label>

          <label class="activity-form-field">
            <span>Status <strong>*</strong></span>
            <select v-model="status" required>
              <option
                v-for="option in statuses"
                :key="option.value"
                :value="option.value"
              >
                {{ option.label }}
              </option>
            </select>
          </label>
        </div>

        <footer class="activity-modal-footer">
          <button
            class="activity-cancel-button"
            type="button"
            @click="emit('close')"
          >
            Cancelar
          </button>

          <button class="activity-save-button" type="submit" :disabled="saving">
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d="M5 4h12l2 2v14H5V4Z" />
              <path d="M8 4v6h8V4M9 20v-6h6v6" />
            </svg>
            {{ saving ? 'Salvando…' : isEditing ? 'Salvar alterações' : 'Salvar atividade' }}
          </button>
        </footer>
      </form>
    </section>
  </div>
</template>

<style scoped>
.activity-modal-backdrop {
  align-items: center;
  background: rgba(16, 20, 34, .58);
  display: flex;
  inset: 0;
  justify-content: center;
  overflow-y: auto;
  padding: 30px 18px;
  position: fixed;
  z-index: 100;
}

.activity-modal {
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 24px 70px rgba(15, 18, 35, .28);
  color: #202538;
  max-width: 650px;
  max-height: calc(100dvh - 32px);
  overflow-y: auto;
  overscroll-behavior: contain;
  padding: 28px 32px 25px;
  width: 100%;
}

.activity-modal-header,
.activity-modal-title,
.activity-modal-footer,
.activity-save-button {
  align-items: center;
  display: flex;
}

.activity-save-button:disabled { cursor: wait; opacity: .7; }

.activity-modal-header {
  justify-content: space-between;
  margin-bottom: 25px;
}

.activity-modal-title {
  gap: 14px;
}

.activity-modal-title > span {
  align-items: center;
  background: #e8f8ef;
  border-radius: 9px;
  color: #2daf68;
  display: flex;
  height: 50px;
  justify-content: center;
  width: 50px;
}

.activity-modal-title svg,
.activity-save-button svg {
  fill: none;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 1.8;
}

.activity-modal-title svg {
  height: 25px;
  width: 25px;
}

.activity-modal-title h2 {
  color: #303548;
  font-size: 1.22rem;
  font-weight: 800;
  letter-spacing: -.03em;
  margin: 0 0 5px;
}

.activity-modal-title p {
  color: #70778b;
  font-size: .73rem;
}

.activity-modal-close {
  background: transparent;
  border: 0;
  color: #4e5569;
  cursor: pointer;
  font-size: 1.7rem;
  line-height: 1;
  padding: 6px;
}

.activity-modal form {
  display: grid;
  gap: 20px;
}

.activity-form-field {
  color: #282d40;
  display: grid;
  font-size: .74rem;
  font-weight: 700;
  gap: 8px;
}

.activity-form-field strong {
  color: #e84260;
}

.activity-form-field small {
  color: #777e91;
  font-size: .63rem;
  font-weight: 450;
}

.activity-form-field input,
.activity-form-field select,
.activity-form-field textarea {
  background: #fff;
  border: 1px solid #dfe1e8;
  border-radius: 7px;
  color: #242a3d;
  font: inherit;
  font-weight: 400;
  outline: none;
  padding: 12px 14px;
  resize: vertical;
}

.activity-form-field select {
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='14' height='14' viewBox='0 0 24 24' fill='none' stroke='%23575e73' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='m6 9 6 6 6-6'/%3E%3C/svg%3E");
  background-position: right 15px center;
  background-repeat: no-repeat;
  padding-right: 40px;
}

.activity-form-field select:disabled {
  background-color: #f4f4f8;
  color: #747a8d;
}

.activity-form-field input:focus,
.activity-form-field select:focus,
.activity-form-field textarea:focus {
  border-color: #7544eb;
  box-shadow: 0 0 0 3px rgba(117, 68, 235, .11);
}

.activity-form-field input::placeholder,
.activity-form-field textarea::placeholder {
  color: #aaa7b5;
  opacity: 1;
}

.activity-form-grid {
  display: grid;
  gap: 14px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.activity-modal-footer {
  border-top: 1px solid #ececf1;
  gap: 12px;
  justify-content: flex-end;
  margin-top: 3px;
  padding-top: 20px;
}

.activity-cancel-button,
.activity-save-button {
  border-radius: 7px;
  cursor: pointer;
  font-size: .72rem;
  font-weight: 700;
  padding: 11px 18px;
}

.activity-cancel-button {
  background: #fff;
  border: 1px solid #dfe1e8;
  color: #343a4e;
}

.activity-save-button {
  background: linear-gradient(100deg, #5d20df, #7419f5);
  border: 0;
  box-shadow: 0 8px 18px rgba(101, 31, 225, .2);
  color: #fff;
  gap: 8px;
}

.activity-save-button svg {
  height: 17px;
  width: 17px;
}

button:focus-visible,
input:focus-visible,
select:focus-visible,
textarea:focus-visible {
  outline: 3px solid rgba(105, 54, 224, .28);
  outline-offset: 2px;
}

@media (max-width: 620px) {
  .activity-modal-backdrop {
    align-items: flex-start;
    padding: 12px;
  }

  .activity-modal {
    border-radius: 12px;
    padding: 22px 18px;
  }

  .activity-form-grid {
    grid-template-columns: 1fr;
  }

  .activity-modal-footer {
    align-items: stretch;
    flex-direction: column-reverse;
  }

  .activity-save-button {
    justify-content: center;
  }
}
</style>
