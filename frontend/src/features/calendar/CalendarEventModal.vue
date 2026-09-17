<script setup>
import { computed, ref } from 'vue'
import AppDatePicker from '../../components/ui/AppDatePicker.vue'
import AppSelect from '../../components/ui/AppSelect.vue'
import AppTimePicker from '../../components/ui/AppTimePicker.vue'

const props = defineProps({
  event: { type: Object, default: null },
  disciplines: { type: Array, default: () => [] },
  defaultDate: { type: String, default: '' },
})

const emit = defineEmits(['close', 'save', 'delete'])

const DEFAULT_TIME = '08:00'

const categories = [
  { value: 'CLASS', label: 'Aula' },
  { value: 'ACTIVITY', label: 'Atividade' },
  { value: 'EXAM', label: 'Prova' },
  { value: 'ASSIGNMENT', label: 'Trabalho' },
  { value: 'OTHER', label: 'Outro' },
]

const isEditing = computed(() => Boolean(props.event))

// A API troca datas como "AAAA-MM-DDTHH:mm:ss" (LocalDateTime, sem fuso horário pelo meio).
// Os seletores do site trabalham com as duas metades separadas: "AAAA-MM-DD" e "HH:mm".
function toDatePart(value) {
  return value ? value.slice(0, 10) : ''
}

function toTimePart(value) {
  return value ? value.slice(11, 16) : ''
}

function joinDateTime(date, time) {
  return `${date}T${time}:00`
}

const title = ref(props.event?.title ?? '')
const description = ref(props.event?.description ?? '')
const category = ref(props.event?.category ?? 'CLASS')
const disciplineId = ref(props.event?.disciplineId ?? '')
const startDate = ref(toDatePart(props.event?.startsAt) || toDatePart(props.defaultDate))
const startTime = ref(toTimePart(props.event?.startsAt) || toTimePart(props.defaultDate) || DEFAULT_TIME)
const endDate = ref(toDatePart(props.event?.endsAt))
const endTime = ref(toTimePart(props.event?.endsAt))
const submitted = ref(false)
const formError = ref('')

// Evento cuja disciplina foi apagada: o aviso explica por que o campo está vazio.
const disciplineWasDeleted = computed(() => Boolean(props.event?.disciplineDeleted))

const disciplineOptions = computed(() => [
  { value: '', label: 'Sem disciplina' },
  ...props.disciplines.map(discipline => ({ value: discipline.id, label: discipline.name })),
])

// O término é opcional, mas é uma data e um horário só: preencher metade não vale.
const hasEnd = computed(() => Boolean(endDate.value || endTime.value))

function clearEnd() {
  endDate.value = ''
  endTime.value = ''
}

function submitForm() {
  submitted.value = true

  if (!startDate.value || !startTime.value) {
    formError.value = 'A data e a hora de início são obrigatórias.'
    return
  }

  if (hasEnd.value && (!endDate.value || !endTime.value)) {
    formError.value = 'Informe a data e a hora do término, ou deixe os dois em branco.'
    return
  }

  const startsAt = joinDateTime(startDate.value, startTime.value)
  const endsAt = hasEnd.value ? joinDateTime(endDate.value, endTime.value) : null

  if (endsAt && endsAt <= startsAt) {
    formError.value = 'A data e hora de término devem ser posteriores às de início.'
    return
  }

  formError.value = ''

  emit('save', {
    title: title.value.trim(),
    description: description.value.trim() || null,
    category: category.value,
    startsAt,
    endsAt,
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

        <div class="form-field">
          <label for="event-discipline">Disciplina <small>(opcional)</small></label>
          <AppSelect id="event-discipline" v-model="disciplineId" :options="disciplineOptions" placeholder="Sem disciplina" />
          <small v-if="disciplineWasDeleted" class="field-warning">
            Essa disciplina não existe mais. Se você salvar assim, o evento fica sem disciplina.
          </small>
        </div>

        <div class="form-row">
          <div class="form-field">
            <label for="event-start-date">Início <strong>*</strong></label>
            <div class="datetime-pair">
              <AppDatePicker id="event-start-date" v-model="startDate" placeholder="dd/mm/aaaa" :invalid="submitted && !startDate" />
              <AppTimePicker v-model="startTime" aria-label="Horário de início" :invalid="submitted && !startTime" />
            </div>
          </div>

          <div class="form-field">
            <label for="event-end-date">
              Término <small>(opcional)</small>
              <button v-if="hasEnd" class="field-clear" type="button" @click="clearEnd">Limpar</button>
            </label>
            <div class="datetime-pair">
              <AppDatePicker id="event-end-date" v-model="endDate" placeholder="dd/mm/aaaa" :min="startDate" :invalid="submitted && hasEnd && !endDate" />
              <AppTimePicker v-model="endTime" aria-label="Horário de término" :invalid="submitted && hasEnd && !endTime" />
            </div>
          </div>
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
  max-width: 580px;
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
.form-field > label,
.form-field legend { align-items: center; color: #3b4055; display: flex; font-size: .72rem; font-weight: 700; gap: 5px; margin-bottom: 7px; }
.form-field strong { color: #d1436a; }
.form-field small { color: #9096a8; font-weight: 500; }
fieldset.form-field { border: 0; padding: 0; }

/* Data, horário e disciplina usam os seletores do site (components/ui) em vez dos nativos do
   navegador: os nativos mudam de desenho a cada navegador e não acompanham o modo noite.
   Altura, fonte e arredondamento vêm daqui, para ficarem do tamanho dos outros campos do modal. */
.form-field {
  --app-date-height: 40px;
  --app-date-font-size: .78rem;
  --app-date-radius: 8px;
  --app-date-padding: 0 12px;
  --app-time-height: 40px;
  --app-time-font-size: .78rem;
  --app-time-radius: 8px;
  --app-time-padding: 0 10px;
  --app-select-height: 40px;
  --app-select-font-size: .78rem;
  --app-select-radius: 8px;
  --app-select-padding: 0 12px;
}

.form-field input[type="text"],
.form-field textarea {
  background: #fff;
  border: 1px solid #dfe1ea;
  border-radius: 8px;
  color: #1d2236;
  font-family: inherit;
  font-size: .78rem;
  width: 100%;
}

.form-field input[type="text"] { height: 40px; padding: 0 12px; }
.form-field textarea { padding: 11px 12px; resize: vertical; }

.form-field input:focus,
.form-field textarea:focus { border-color: #7d55f2; outline: 2px solid rgba(105, 54, 224, .18); outline-offset: 0; }

.datetime-pair { display: grid; gap: 8px; grid-template-columns: minmax(0, 1fr) 100px; }

.field-clear { background: none; border: 0; color: #6330e0; font-size: .68rem; font-weight: 700; margin-left: auto; padding: 0; }
.field-clear:hover { text-decoration: underline; }

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

@media (max-width: 560px) {
  .form-row { grid-template-columns: 1fr; }
  .modal-actions { flex-wrap: wrap; }
  .modal-actions-spacer { display: none; }
}
</style>
