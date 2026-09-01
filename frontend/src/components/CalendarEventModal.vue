<script setup>
import { computed, ref } from 'vue'

const props = defineProps({
  event: { type: Object, default: null },
  disciplines: { type: Array, default: () => [] },
  defaultDate: { type: String, default: '' },
})

const emit = defineEmits(['close', 'save', 'delete'])

const categories = [
  { value: 'CLASS', label: 'Aula' },
  { value: 'ACTIVITY', label: 'Atividade' },
  { value: 'EXAM', label: 'Prova' },
  { value: 'ASSIGNMENT', label: 'Trabalho' },
  { value: 'OTHER', label: 'Outro' },
]

const isEditing = computed(() => Boolean(props.event))

// O input datetime-local trabalha com "AAAA-MM-DDTHH:mm", que é exatamente o
// formato que o LocalDateTime da API entende — nada de fuso horário pelo meio.
function toInputValue(value) {
  return value ? value.slice(0, 16) : ''
}

const title = ref(props.event?.title ?? '')
const description = ref(props.event?.description ?? '')
const category = ref(props.event?.category ?? 'CLASS')
const disciplineId = ref(props.event?.disciplineId ?? '')
const startsAt = ref(toInputValue(props.event?.startsAt) || props.defaultDate)
const endsAt = ref(toInputValue(props.event?.endsAt))
const formError = ref('')

// Evento cuja disciplina foi apagada: o aviso explica por que o campo está vazio.
const disciplineWasDeleted = computed(() => Boolean(props.event?.disciplineDeleted))

function submitForm() {
  if (!startsAt.value) {
    formError.value = 'A data e hora de início são obrigatórias.'
    return
  }

  if (endsAt.value && endsAt.value <= startsAt.value) {
    formError.value = 'A data e hora de término devem ser posteriores às de início.'
    return
  }

  formError.value = ''

  emit('save', {
    title: title.value.trim(),
    description: description.value.trim() || null,
    category: category.value,
    startsAt: `${startsAt.value}:00`,
    endsAt: endsAt.value ? `${endsAt.value}:00` : null,
    disciplineId: disciplineId.value || null,
  })
}
</script>

<template>
  <div class="modal-backdrop" @mousedown.self="emit('close')">
    <section class="event-modal" role="dialog" aria-modal="true" aria-labelledby="event-modal-title">
      <header class="modal-header">
        <div class="modal-title">
          <span aria-hidden="true">＋</span>
          <div>
            <h2 id="event-modal-title">{{ isEditing ? 'Editar evento' : 'Novo evento' }}</h2>
            <p>{{ isEditing ? 'Atualize as informações deste compromisso.' : 'Adicione um compromisso ou prazo ao seu calendário.' }}</p>
          </div>
        </div>

        <button class="modal-close" type="button" aria-label="Fechar modal" @click="emit('close')">×</button>
      </header>

      <form @submit.prevent="submitForm">
        <label class="form-field">
          <span>Título <strong>*</strong></span>
          <input v-model.trim="title" type="text" maxlength="120" placeholder="Ex.: Prova 1 - Estruturas" required autofocus>
        </label>

        <fieldset class="form-field">
          <legend>Categoria <strong>*</strong></legend>
          <div class="category-options">
            <label
              v-for="option in categories"
              :key="option.value"
              class="category-option"
              :class="[`is-${option.value.toLowerCase()}`, { selected: category === option.value }]"
            >
              <input v-model="category" type="radio" name="category" :value="option.value">
              <span aria-hidden="true" class="category-dot"></span>
              {{ option.label }}
            </label>
          </div>
        </fieldset>

        <label class="form-field">
          <span>Disciplina <small>(opcional)</small></span>
          <select v-model="disciplineId">
            <option value="">Sem disciplina</option>
            <option v-for="discipline in disciplines" :key="discipline.id" :value="discipline.id">
              {{ discipline.name }}
            </option>
          </select>
          <small v-if="disciplineWasDeleted" class="field-warning">
            Essa disciplina não existe mais. Se você salvar assim, o evento fica sem disciplina.
          </small>
        </label>

        <div class="form-row">
          <label class="form-field">
            <span>Início <strong>*</strong></span>
            <input v-model="startsAt" type="datetime-local" required>
          </label>

          <label class="form-field">
            <span>Término <small>(opcional)</small></span>
            <input v-model="endsAt" type="datetime-local">
          </label>
        </div>

        <p class="form-hint">Prazos de entrega não precisam de término — basta informar o horário limite no início.</p>

        <label class="form-field">
          <span>Descrição <small>(opcional)</small></span>
          <textarea v-model.trim="description" maxlength="500" rows="3" placeholder="Ex.: Conteúdo das aulas 1 a 6."></textarea>
        </label>

        <p v-if="formError" class="form-error" role="alert">{{ formError }}</p>

        <footer class="modal-actions">
          <button v-if="isEditing" class="delete-event" type="button" @click="emit('delete')">
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d="M4 7h16M9 7V4h6v3m3 0-1 13H7L6 7m4 4v5m4-5v5" />
            </svg>
            Excluir evento
          </button>

          <span class="modal-actions-spacer"></span>

          <button class="cancel-action" type="button" @click="emit('close')">Cancelar</button>
          <button class="save-action" type="submit">{{ isEditing ? 'Salvar alterações' : 'Criar evento' }}</button>
        </footer>
      </form>
    </section>
  </div>
</template>

<style scoped>
.modal-backdrop {
  align-items: center;
  background: rgba(16, 20, 34, .58);
  display: flex;
  inset: 0;
  justify-content: center;
  padding: 20px;
  position: fixed;
  z-index: 110;
}

.event-modal {
  background: #fff;
  border-radius: 15px;
  box-shadow: 0 24px 70px rgba(15, 18, 35, .28);
  max-height: 92vh;
  max-width: 545px;
  overflow-y: auto;
  padding: 26px 28px 24px;
  width: 100%;
}

.modal-header { align-items: flex-start; display: flex; gap: 12px; justify-content: space-between; margin-bottom: 20px; }
.modal-title { align-items: center; display: flex; gap: 12px; }
.modal-title > span { align-items: center; background: #f1edff; border-radius: 10px; color: #6330e0; display: flex; flex: 0 0 40px; font-size: 1.2rem; height: 40px; justify-content: center; }
.modal-title h2 { color: #171c30; font-size: 1.05rem; font-weight: 800; letter-spacing: -.025em; }
.modal-title p { color: #757c91; font-size: .72rem; margin-top: 3px; }
.modal-close { background: none; border: 0; color: #8b90a3; font-size: 1.5rem; line-height: 1; padding: 2px 6px; }
.modal-close:hover { color: #3b4055; }

.form-field { display: block; margin-bottom: 15px; }
.form-field > span,
.form-field legend { color: #3b4055; display: block; font-size: .72rem; font-weight: 700; margin-bottom: 7px; }
.form-field strong { color: #d1436a; }
.form-field small { color: #9096a8; font-weight: 500; }
fieldset.form-field { border: 0; padding: 0; }

.form-field input[type="text"],
.form-field input[type="datetime-local"],
.form-field select,
.form-field textarea {
  background: #fff;
  border: 1px solid #dfe1ea;
  border-radius: 8px;
  color: #1d2236;
  font-family: inherit;
  font-size: .78rem;
  padding: 11px 12px;
  resize: vertical;
  width: 100%;
}

.form-field input:focus,
.form-field select:focus,
.form-field textarea:focus { border-color: #7d55f2; outline: 2px solid rgba(105, 54, 224, .18); outline-offset: 0; }

.field-warning { color: #c2415f; display: block; font-size: .67rem; font-weight: 600; margin-top: 6px; }

.category-options { display: flex; flex-wrap: wrap; gap: 8px; }
.category-option { align-items: center; border: 1px solid #e2e0ec; border-radius: 999px; color: #4a5066; cursor: pointer; display: flex; font-size: .71rem; font-weight: 650; gap: 7px; padding: 8px 13px; position: relative; }
.category-option input { opacity: 0; pointer-events: none; position: absolute; }
.category-dot { border-radius: 50%; flex: 0 0 9px; height: 9px; width: 9px; }
.category-option.is-class .category-dot { background: #2daf68; }
.category-option.is-activity .category-dot { background: #7a4ced; }
.category-option.is-exam .category-dot { background: #3a7fd9; }
.category-option.is-assignment .category-dot { background: #ef8b1f; }
.category-option.is-other .category-dot { background: #e2537c; }
.category-option.selected { background: #f4f0ff; border-color: #a186f0; color: #4a24b6; }
.category-option:focus-within { outline: 2px solid rgba(105, 54, 224, .28); outline-offset: 2px; }

.form-row { display: grid; gap: 13px; grid-template-columns: repeat(2, minmax(0, 1fr)); }
.form-hint { color: #8b90a3; font-size: .67rem; margin: -6px 0 15px; }
.form-error { background: #fff0f3; border-radius: 7px; color: #c2415f; font-size: .71rem; font-weight: 600; margin-bottom: 13px; padding: 10px 12px; }

.modal-actions { align-items: center; border-top: 1px solid #eeecf5; display: flex; gap: 9px; margin-top: 4px; padding-top: 16px; }
.modal-actions-spacer { flex: 1; }
.modal-actions button { border-radius: 8px; font-size: .75rem; font-weight: 700; padding: 11px 17px; }
.cancel-action { background: #fff; border: 1px solid #dfe1ea; color: #4a5066; }
.cancel-action:hover { background: #f5f5fa; }
.save-action { background: linear-gradient(100deg, #5c20de, #741dff); border: 0; box-shadow: 0 8px 19px rgba(102, 36, 225, .2); color: #fff; }
.save-action:hover { box-shadow: 0 11px 24px rgba(102, 36, 225, .28); transform: translateY(-1px); }
.delete-event { align-items: center; background: #fff; border: 1px solid #f3d3d9; color: #c2415f; display: flex; gap: 7px; }
.delete-event:hover { background: #fff5f7; }
.delete-event svg { fill: none; height: 15px; stroke: currentColor; stroke-linecap: round; stroke-linejoin: round; stroke-width: 1.8; width: 15px; }

@media (max-width: 520px) {
  .form-row { grid-template-columns: 1fr; }
  .modal-actions { flex-wrap: wrap; }
  .modal-actions-spacer { display: none; }
}
</style>
