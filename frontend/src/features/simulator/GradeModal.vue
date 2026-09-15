<template>
  <div
    class="modal-overlay"
    @keydown.esc="closeIfIdle"
    @keydown.tab="trapFocus"
    @click.self="closeIfIdle"
  >
    <div
      ref="card"
      class="modal-card"
      role="dialog"
      aria-modal="true"
      aria-labelledby="grade-modal-title"
      aria-describedby="grade-modal-description"
    >

      <h2 id="grade-modal-title">Lançar nota de avaliação</h2>

      <p id="grade-modal-description" class="modal-description">
        A nota fica vinculada a uma prova ou trabalho cadastrado em Atividades,
        comprovando de qual avaliação ela veio.
      </p>

      <!-- Na tela Notas a disciplina é escolhida aqui; no simulador ela já vem selecionada. -->
      <div v-if="choosesDiscipline" class="field">
        <label for="grade-modal-discipline">Disciplina</label>

        <AppSelect
          id="grade-modal-discipline"
          :model-value="disciplineId"
          :options="disciplineOptions"
          :disabled="saving"
          placeholder="Selecione a disciplina"
          @update:model-value="value => emit('select-discipline', value)"
        />
      </div>

      <div v-if="choosesDiscipline && !disciplineId" class="modal-empty">
        <p>Escolha a disciplina para ver as provas e trabalhos cadastrados nela.</p>

        <div class="actions">
          <button type="button" class="cancel-button" @click="emit('close')">Cancelar</button>
        </div>
      </div>

      <p v-else-if="activitiesStatus === 'loading' || activitiesStatus === 'idle'" class="modal-status" role="status">
        Carregando as avaliações de {{ disciplineName }}…
      </p>

      <div v-else-if="activitiesStatus === 'error'" class="modal-empty" role="alert">
        <p>Não foi possível carregar as avaliações desta disciplina.</p>

        <div class="actions">
          <button type="button" class="cancel-button" @click="emit('close')">Fechar</button>
          <button type="button" class="save-button" @click="emit('retry')">Tentar de novo</button>
        </div>
      </div>

      <div v-else-if="activities.length === 0" class="modal-empty">
        <p><strong>{{ disciplineName }}</strong> ainda não tem provas ou trabalhos cadastrados.</p>
        <p>Cadastre a avaliação em Atividades e volte aqui para lançar a nota dela.</p>

        <div class="actions">
          <button type="button" class="cancel-button" @click="emit('close')">Cancelar</button>
          <button type="button" class="save-button" @click="emit('go-to-activities')">Ir para Atividades</button>
        </div>
      </div>

      <form v-else novalidate @submit.prevent="submit">

        <div class="field">
          <label for="grade-modal-activity">Avaliação cadastrada</label>

          <AppSelect
            id="grade-modal-activity"
            v-model="activityId"
            :options="activityOptions"
            :invalid="submitted && Boolean(activityError)"
            describedby="grade-modal-activity-help"
            placeholder="Selecione a prova ou trabalho"
          />

          <p v-if="submitted && activityError" id="grade-modal-activity-help" class="field-error" role="alert">
            {{ activityError }}
          </p>
          <p v-else-if="!hasSelectableActivity" id="grade-modal-activity-help" class="field-hint">
            Todas as avaliações desta disciplina já têm nota ou ainda não aconteceram.
          </p>
        </div>


        <div v-if="selectedActivity" class="proof-card" aria-live="polite">
          <span class="proof-label">Comprovação</span>
          <strong>{{ selectedActivity.title }}</strong>
          <span>Realizada em {{ formatDate(selectedActivity.dueDate) }} · {{ statusLabel(selectedActivity.status) }}</span>
          <span v-if="selectedActivity.description" class="proof-description">{{ selectedActivity.description }}</span>
        </div>


        <div class="field">
          <label for="grade-modal-score">Nota obtida</label>

          <input
            id="grade-modal-score"
            v-model="score"
            type="number"
            inputmode="decimal"
            min="0"
            :max="MAX_SCORE"
            step="0.01"
            placeholder="Ex: 8,5"
            required
            :aria-invalid="showScoreError"
            aria-describedby="grade-modal-score-error"
            @keydown="blockInvalidNumberKeys"
          >

          <p v-if="showScoreError" id="grade-modal-score-error" class="field-error" role="alert">{{ scoreError }}</p>
        </div>


        <div class="field">
          <label for="grade-modal-date">Data em que a nota saiu</label>

          <AppDatePicker
            id="grade-modal-date"
            v-model="recordedAt"
            :min="selectedActivity ? selectedActivity.dueDate : ''"
            :max="today"
            :invalid="submitted && Boolean(dateError)"
            describedby="grade-modal-date-error"
          />

          <p v-if="submitted && dateError" id="grade-modal-date-error" class="field-error">{{ dateError }}</p>
        </div>

        <p v-if="errorMessage" class="form-error" role="alert">{{ errorMessage }}</p>

        <div class="actions">

          <button
            type="button"
            class="cancel-button"
            :disabled="saving"
            @click="emit('close')"
          >
            Cancelar
          </button>

          <button
            type="submit"
            class="save-button"
            :disabled="saving || !hasSelectableActivity"
          >
            {{ saving ? 'Salvando...' : 'Lançar nota' }}
          </button>

        </div>

      </form>

    </div>
  </div>
</template>


<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import AppDatePicker from '../../components/ui/AppDatePicker.vue'
import AppSelect from '../../components/ui/AppSelect.vue'

const MAX_SCORE = 10
const ASSESSMENT_NAME_LIMIT = 120

const STATUS_LABELS = {
  PENDING: 'Pendente',
  IN_PROGRESS: 'Em andamento',
  COMPLETED: 'Concluída'
}

const props = defineProps({
  activities: {
    type: Array,
    default: () => []
  },
  activitiesStatus: {
    type: String,
    default: 'idle'
  },
  gradedActivityIds: {
    type: Array,
    default: () => []
  },
  disciplineName: {
    type: String,
    default: ''
  },
  // Opcional: com a lista de disciplinas, o modal mostra a escolha da disciplina (tela Notas).
  disciplines: {
    type: Array,
    default: () => []
  },
  disciplineId: {
    type: String,
    default: ''
  },
  saving: {
    type: Boolean,
    default: false
  },
  errorMessage: {
    type: String,
    default: ''
  }
})


const emit = defineEmits([
  'close',
  'save',
  'retry',
  'go-to-activities',
  'select-discipline'
])

// Data de hoje no fuso do navegador. Com toISOString (UTC), à noite no Brasil a data já seria a
// de amanhã, e o backend recusaria a nota por estar "no futuro".
function localIsoDate() {
  const now = new Date()
  now.setMinutes(now.getMinutes() - now.getTimezoneOffset())
  return now.toISOString().slice(0, 10)
}

const today = localIsoDate()
const activityId = ref('')
const score = ref('')
const recordedAt = ref(today)
const submitted = ref(false)
const card = ref(null)

const choosesDiscipline = computed(() => props.disciplines.length > 0)

const disciplineOptions = computed(() => props.disciplines.map(discipline => ({
  value: discipline.id,
  label: discipline.name
})))

const selectedActivity = computed(() => (
  props.activities.find(activity => activity.id === activityId.value) ?? null
))

function isGraded(activity) {
  return props.gradedActivityIds.includes(activity.id)
}

function isFuture(activity) {
  return activity.dueDate > today
}

// Só dá para lançar nota de avaliação que já aconteceu e que ainda não tem nota.
function isSelectable(activity) {
  return !isGraded(activity) && !isFuture(activity)
}

const hasSelectableActivity = computed(() => props.activities.some(isSelectable))

function formatDate(isoDate) {
  const [year, month, day] = String(isoDate).split('-')
  return `${day}/${month}/${year}`
}

function statusLabel(status) {
  return STATUS_LABELS[status] ?? status
}

// As bloqueadas continuam na lista, com o motivo, para a pessoa entender por que não pode escolher.
const activityOptions = computed(() => props.activities.map(activity => {
  const graded = isGraded(activity)
  const future = isFuture(activity)

  return {
    value: activity.id,
    label: activity.title,
    meta: `${future ? 'Acontece em' : 'Realizada em'} ${formatDate(activity.dueDate)}`,
    disabled: graded || future,
    badge: graded ? 'Nota já lançada' : (future ? 'Ainda não aconteceu' : undefined),
    badgeTone: graded ? 'neutral' : 'warning'
  }
}))

const activityError = computed(() => (
  selectedActivity.value ? '' : 'Selecione a prova ou trabalho desta nota.'
))

const scoreError = computed(() => {
  if (score.value === '' || score.value === null) return 'Informe a nota obtida.'

  const value = Number(score.value)

  if (Number.isNaN(value)) return 'Informe um número válido.'
  if (value < 0) return 'A nota não pode ser negativa.'
  if (value > MAX_SCORE) return 'A nota não pode ser maior que 10.'
  if (Math.abs(value * 100 - Math.round(value * 100)) > 1e-9) return 'Use no máximo duas casas decimais.'

  return ''
})

// Mostra o erro da nota assim que algo é digitado, sem esperar o envio.
const showScoreError = computed(() => (
  Boolean(scoreError.value) && (submitted.value || score.value !== '')
))

const dateError = computed(() => {
  if (!recordedAt.value) return 'Informe a data.'
  if (recordedAt.value > today) return 'A data não pode estar no futuro.'
  if (selectedActivity.value && recordedAt.value < selectedActivity.value.dueDate) {
    return `A nota não pode ter data anterior à avaliação (${formatDate(selectedActivity.value.dueDate)}).`
  }
  return ''
})

// Notas são positivas: "-", "+" e "e" (notação científica) não são digitáveis.
function blockInvalidNumberKeys(event) {
  if (['-', '+', 'e', 'E'].includes(event.key)) {
    event.preventDefault()
  }
}

function closeIfIdle() {
  if (!props.saving) emit('close')
}

function focusableElements() {
  return [...(card.value?.querySelectorAll('input:not([disabled]), button:not([disabled]):not([tabindex="-1"])') ?? [])]
}

// Mantém o Tab dentro do diálogo enquanto ele estiver aberto.
function trapFocus(event) {
  const elements = focusableElements()
  if (elements.length === 0) return

  const first = elements[0]
  const last = elements[elements.length - 1]

  if (event.shiftKey && document.activeElement === first) {
    event.preventDefault()
    last.focus()
  } else if (!event.shiftKey && document.activeElement === last) {
    event.preventDefault()
    first.focus()
  }
}

// Quando as avaliações aparecem, o foco vai direto para a escolha da prova.
async function focusFirstField() {
  await nextTick()
  const activityPicker = card.value?.querySelector('#grade-modal-activity')
  const target = activityPicker ?? focusableElements()[0]
  target?.focus()
}

function submit() {
  submitted.value = true

  if (activityError.value || scoreError.value || dateError.value || props.saving) {
    return
  }

  emit('save', {
    activityId: selectedActivity.value.id,
    assessmentName: selectedActivity.value.title.trim().slice(0, ASSESSMENT_NAME_LIMIT),
    score: Number(score.value),
    recordedAt: recordedAt.value
  })
}

// Trocar de disciplina invalida a avaliação escolhida antes.
watch(() => props.disciplineId, () => {
  activityId.value = ''
  submitted.value = false
})

// Só leva o foco para a escolha da prova quando ele ficou sem lugar (o "Carregando…" sumiu da tela).
// Se a pessoa está em outro campo do modal, como a escolha da disciplina, o foco fica onde está.
watch(() => props.activitiesStatus, () => {
  if (!card.value?.contains(document.activeElement)) focusFirstField()
})

onMounted(focusFirstField)
</script>


<style scoped>

.modal-overlay {
  position: fixed;
  inset: 0;

  z-index: 9999;

  display: flex;
  align-items: center;
  justify-content: center;

  padding: 20px;

  background: rgba(20, 18, 35, 0.45);
}


.modal-card {
  width: 100%;
  max-width: 460px;
  max-height: calc(100vh - 40px);

  overflow-y: auto;

  box-sizing: border-box;

  padding: 26px;

  border-radius: 14px;

  background: #ffffff;

  box-shadow:
    0 20px 60px rgba(20, 18, 35, 0.2);
}


.modal-card h2 {
  margin: 0 0 8px;

  color: #202033;

  font-size: 21px;
}


.modal-description,
.modal-status,
.modal-empty p {
  margin: 0 0 18px;

  color: #6b6880;

  font-size: 13px;
  line-height: 1.5;
}


.modal-empty p + p {
  margin-top: -10px;
}


.field {
  margin-bottom: 17px;
}


.field label {
  display: block;

  margin-bottom: 7px;

  color: #555267;

  font-size: 13px;
  font-weight: 600;
}


.field input {
  width: 100%;
  height: 42px;

  box-sizing: border-box;

  padding: 0 12px;

  border: 1px solid #dedce8;
  border-radius: 8px;

  background: #ffffff;

  color: #252338;

  font: inherit;
  font-size: 14px;

  outline: none;
}


.field input::placeholder {
  color: #8a879b;
}


.field input:hover {
  border-color: #c9c1ea;
}


.field input:focus {
  border-color: #6330e0;

  box-shadow:
    0 0 0 3px rgba(99, 48, 224, 0.14);
}


.field input[aria-invalid="true"] {
  border-color: #c4463e;
}


.proof-card {
  display: grid;
  gap: 3px;

  margin: -4px 0 17px;
  padding: 12px 14px;

  border: 1px solid #e3dafc;
  border-left: 3px solid #6330e0;
  border-radius: 8px;

  background: #f7f3ff;

  color: #555267;

  font-size: 12px;
}


.proof-card strong {
  color: #252338;

  font-size: 14px;
}


.proof-label {
  color: #6330e0;

  font-size: 11px;
  font-weight: 700;
  letter-spacing: .06em;
  text-transform: uppercase;
}


.proof-description {
  color: #7a7790;
}


.field-hint {
  margin: 6px 0 0;

  color: #7a7790;

  font-size: 12px;
}


.field-error,
.form-error {
  margin: 6px 0 0;

  color: #c4463e;

  font-size: 12px;
  font-weight: 600;
}


.form-error {
  margin-top: 4px;
}


.actions {
  display: flex;
  justify-content: flex-end;
  flex-wrap: wrap;

  gap: 10px;

  margin-top: 24px;
}


.actions button {
  height: 40px;

  padding: 0 18px;

  border-radius: 8px;

  font-size: 14px;
  font-weight: 600;

  cursor: pointer;
}


.actions button:focus-visible {
  outline: 2px solid rgba(99, 48, 224, 0.45);
  outline-offset: 2px;
}


.cancel-button {
  border: 1px solid #dedce8;

  background: #ffffff;

  color: #555267;
}


.save-button {
  border: none;

  background: #6330e0;

  color: #ffffff;
}


.save-button:hover:not(:disabled) {
  background: #5726ce;
}


.actions button:disabled {
  opacity: 0.6;

  cursor: not-allowed;
}

</style>
