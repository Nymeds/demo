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
  grade: { type: Object, default: null },
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
const activityId = ref(props.grade?.activityId ?? '')
const observation = ref(props.grade?.observation ?? '')
const score = ref(props.grade?.score ?? '')
const recordedAt = ref(props.grade?.recordedAt ?? today)
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
  return activity.id !== props.grade?.activityId && props.gradedActivityIds.includes(activity.id)
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
  (selectedActivity.value || (props.grade && !props.grade.activityId)) ? '' : 'Selecione a prova ou trabalho desta nota.'
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
  return [...(card.value?.querySelectorAll('input:not([disabled]), textarea:not([disabled]), button:not([disabled]):not([tabindex="-1"])') ?? [])]
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

  if (!props.disciplineId || activityError.value || scoreError.value || dateError.value || props.saving || (selectedActivity.value && !isSelectable(selectedActivity.value))) {
    return
  }

  emit('save', {
    activityId: selectedActivity.value?.id ?? null,
    assessmentName: (selectedActivity.value?.title ?? props.grade?.assessmentName ?? '').trim().slice(0, ASSESSMENT_NAME_LIMIT),
    observation: observation.value.trim() || null,
    score: Number(score.value),
    recordedAt: recordedAt.value
  })
}

// Trocar de disciplina invalida a avaliação escolhida antes.
watch(() => props.disciplineId, () => {
  activityId.value = props.grade?.activityId ?? ''
  submitted.value = false
})

// Só leva o foco para a escolha da prova quando ele ficou sem lugar (o "Carregando…" sumiu da tela).
// Se a pessoa está em outro campo do modal, como a escolha da disciplina, o foco fica onde está.
watch(() => props.activitiesStatus, () => {
  if (!card.value?.contains(document.activeElement)) focusFirstField()
})

onMounted(focusFirstField)
</script>
<template>
  <div class="grade-entry-overlay" @click.self="closeIfIdle" @keydown.esc="closeIfIdle" @keydown.tab="trapFocus">
    <section ref="card" class="grade-entry-modal" role="dialog" aria-modal="true" aria-labelledby="grade-modal-title">
      <header class="grade-entry-header">
        <span class="grade-entry-icon" aria-hidden="true">{{ grade ? '✎' : '+' }}</span>
        <div><h2 id="grade-modal-title">{{ grade ? 'Editar nota' : 'Adicionar nota' }}</h2><p>Preencha as informações para {{ grade ? 'atualizar a' : 'lançar uma nova' }} nota.</p></div>
        <button class="grade-entry-close" type="button" aria-label="Fechar" :disabled="saving" @click="closeIfIdle">×</button>
      </header>
      <form novalidate @submit.prevent="submit">
        <fieldset :disabled="saving" class="grade-entry-fields">
          <label class="grade-entry-field">Disciplina <strong>*</strong>
            <AppSelect id="grade-modal-discipline" :model-value="disciplineId" :options="disciplineOptions" :disabled="saving || Boolean(grade)" placeholder="Selecione a disciplina" @update:model-value="value => emit('select-discipline', value)" />
          </label>
          <div class="grade-entry-field"><label for="grade-modal-activity">Avaliação <strong>*</strong></label>
            <input v-if="grade && !grade.activityId" :value="grade.assessmentName" readonly aria-label="Avaliação">
            <AppSelect v-else id="grade-modal-activity" v-model="activityId" :options="activityOptions" :disabled="!disciplineId || activitiesStatus !== 'ready' || saving" :invalid="submitted && Boolean(activityError)" placeholder="Selecione a prova ou trabalho" />
            <small v-if="activitiesStatus === 'loading'" role="status">Carregando avaliações...</small>
            <small v-else-if="activitiesStatus === 'error'" role="alert">Não foi possível carregar. <button type="button" class="grade-entry-link" @click="emit('retry')">Tentar novamente</button></small>
            <small v-else>Se não encontrar, cadastre primeiro em <button type="button" class="grade-entry-link" @click="emit('go-to-activities')">Atividades</button>.</small>
            <small v-if="submitted && activityError" class="grade-entry-error">{{ activityError }}</small>
          </div>
          <label class="grade-entry-field" for="grade-modal-score">Nota obtida <strong>*</strong>
            <input id="grade-modal-score" v-model="score" type="number" inputmode="decimal" min="0" max="10" step="0.01" placeholder="Ex: 8,5" :aria-invalid="showScoreError" @keydown="blockInvalidNumberKeys">
            <small :class="{ 'grade-entry-error': showScoreError }">{{ showScoreError ? scoreError : 'Digite um valor entre 0 e 10.' }}</small>
          </label>
          <div class="grade-entry-field"><label for="grade-modal-date">Data da avaliação <strong>*</strong></label>
            <AppDatePicker id="grade-modal-date" v-model="recordedAt" :max="today" :min="selectedActivity?.dueDate ?? ''" :invalid="submitted && Boolean(dateError)" />
            <small v-if="submitted && dateError" class="grade-entry-error">{{ dateError }}</small>
          </div>
          <label class="grade-entry-field is-wide">Observação (opcional)
            <textarea v-model="observation" maxlength="200" rows="3" placeholder="Ex: prova teórica, trabalho em grupo, etc."></textarea>
            <small class="grade-entry-counter">{{ observation.length }}/200</small>
          </label>
        </fieldset>
        <p v-if="errorMessage" class="grade-entry-error" role="alert">{{ errorMessage }}</p>
        <footer class="grade-entry-actions">
          <button class="grades-button is-secondary" type="button" :disabled="saving" @click="closeIfIdle">Cancelar</button>
          <button class="grades-button is-primary" type="submit" :disabled="saving || !disciplineId || (activitiesStatus !== 'ready')">✓ {{ saving ? 'Salvando...' : 'Salvar nota' }}</button>
        </footer>
      </form>
    </section>
  </div>
</template>
<style scoped>
.grade-entry-overlay { position: fixed; inset: 0; z-index: 200; display: flex; align-items: center; justify-content: center; padding: 20px; background: #151c3b70; backdrop-filter: blur(2px); }
.grade-entry-modal { background: #fff; border: 1px solid #e8e6ef; border-radius: 14px; width: min(100%, 570px); max-height: 90dvh; overflow-y: auto; box-shadow: 0 24px 80px #10153133; color: #171c30; }
.grade-entry-header { display: flex; align-items: center; gap: 16px; padding: 18px 20px; border-bottom: 1px solid #ececf4; }
.grade-entry-header h2 { margin: 0 0 4px; font-size: 1.15rem; }
.grade-entry-header p { margin: 0; font-size: .72rem; color: #777e96; }
.grade-entry-icon { background: #ece4ff; color: #7526ff; display: grid; place-items: center; border-radius: 10px; width: 44px; height: 44px; font-size: 28px; flex-shrink: 0; }
.grade-entry-close { margin-left: auto; border: 0; background: none; color: #68718c; font-size: 25px; cursor: pointer; }
.grade-entry-modal form { padding: 18px 20px 20px; }
.grade-entry-fields { border: 0; margin: 0; padding: 0; display: grid; grid-template-columns: repeat(2,minmax(0,1fr)); gap: 20px 18px; }
.grade-entry-field { display: block; min-width: 0; font-size: .74rem; color: #515874; font-weight: 650; }
.grade-entry-field strong { color: #e24650; }
.grade-entry-field > input, .grade-entry-field > textarea, .grade-entry-field > :deep(.app-select), .grade-entry-field > :deep(.app-date-picker) { margin-top: 6px; }
.grade-entry-field input, .grade-entry-field textarea { display: block; width: 100%; box-sizing: border-box; border: 1px solid #dcddeb; border-radius: 8px; min-height: 42px; padding: 10px 12px; font: inherit; font-weight: 400; color: #1d2439; background: #fff; }
.grade-entry-field textarea { resize: vertical; }
.grade-entry-field small { display: block; color: #7b8197; font-size: .66rem; font-weight: 400; margin-top: 5px; }
.grade-entry-field .grade-entry-error, .grade-entry-error { color: #c53f43; font-size: .72rem; }
.grade-entry-field.is-wide { grid-column: 1/-1; }
.grade-entry-counter { text-align: right; }
.grade-entry-link { padding: 0; background: none; border: 0; color: inherit; text-decoration: underline; cursor: pointer; font: inherit; }
.grade-entry-actions { display: flex; justify-content: flex-end; gap: 12px; margin-top: 22px; }
@media(max-width: 520px) { .grade-entry-fields { grid-template-columns: 1fr; } .grade-entry-header { gap: 10px; } }
</style>
