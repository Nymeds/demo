<script setup>
import { computed, ref, watch } from 'vue'
import { START_SECTIONS } from './settingsApi'

const DEADLINE_LIMITS = Object.freeze({ min: 1, max: 30 })
const MARGIN_LIMITS = Object.freeze({ min: 0, max: 30 })
const GOAL_LIMITS = Object.freeze({ min: 0, max: 10 })
const EXAMPLE_MINIMUM_ATTENDANCE = 75

const props = defineProps({
  api: { type: Object, required: true },
  preferences: { type: Object, required: true },
})

const emit = defineEmits(['saved', 'failed'])

const deadlineAlertDays = ref(3)
const attendanceAlertMargin = ref(10)
const startSection = ref('DASHBOARD')
// Vazio significa "sem meta"; o campo number devolve '' quando é apagado.
const gradeGoal = ref('')
const saving = ref(false)

function reset() {
  deadlineAlertDays.value = props.preferences.deadlineAlertDays
  attendanceAlertMargin.value = props.preferences.attendanceAlertMargin
  startSection.value = props.preferences.startSection
  gradeGoal.value = props.preferences.gradeGoal ?? ''
}

watch(() => props.preferences, reset, { immediate: true })

function isWithin(value, limits) {
  return Number.isInteger(value) && value >= limits.min && value <= limits.max
}

const normalizedGoal = computed(() => (gradeGoal.value === '' || gradeGoal.value === null ? null : Number(gradeGoal.value)))
const goalInvalid = computed(() => {
  const goal = normalizedGoal.value
  if (goal === null) return false
  return Number.isNaN(goal) || goal < GOAL_LIMITS.min || goal > GOAL_LIMITS.max || Math.abs(goal * 10 - Math.round(goal * 10)) > 1e-9
})
const deadlineInvalid = computed(() => !isWithin(deadlineAlertDays.value, DEADLINE_LIMITS))
const marginInvalid = computed(() => !isWithin(attendanceAlertMargin.value, MARGIN_LIMITS))
const hasChanges = computed(() => (
  deadlineAlertDays.value !== props.preferences.deadlineAlertDays
  || attendanceAlertMargin.value !== props.preferences.attendanceAlertMargin
  || startSection.value !== props.preferences.startSection
  || normalizedGoal.value !== (props.preferences.gradeGoal ?? null)
))
const attendanceExample = computed(() => (
  marginInvalid.value ? null : Math.min(100, EXAMPLE_MINIMUM_ATTENDANCE + attendanceAlertMargin.value)
))

// Campos numéricos positivos: "-", "+" e "e" (notação científica) não são digitáveis.
function blockInvalidNumberKeys(event) {
  if (['-', '+', 'e', 'E'].includes(event.key)) event.preventDefault()
}

async function submit() {
  if (!hasChanges.value || deadlineInvalid.value || marginInvalid.value || goalInvalid.value || saving.value) return

  saving.value = true

  try {
    const savedPreferences = await props.api.updatePreferences({
      deadlineAlertDays: deadlineAlertDays.value,
      attendanceAlertMargin: attendanceAlertMargin.value,
      startSection: startSection.value,
      gradeGoal: normalizedGoal.value,
    })
    emit('saved', savedPreferences)
  } catch (error) {
    emit('failed', error)
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <section class="settings-card is-wide" aria-labelledby="settings-preferences-title">
    <header class="settings-card-header">
      <span class="settings-card-icon" aria-hidden="true">
        <svg viewBox="0 0 24 24"><path d="M6 8a6 6 0 1 1 12 0c0 7 3 8 3 8H3s3-1 3-8" /><path d="M10.3 20a2 2 0 0 0 3.4 0" /></svg>
      </span>
      <div>
        <h2 id="settings-preferences-title">Preferências</h2>
        <p>Defina quando você quer ser avisado, sua meta de média e qual tela abrir ao entrar.</p>
      </div>
    </header>

    <form class="settings-form" @submit.prevent="submit">
      <div class="settings-form-row">
        <label class="settings-field">
          Avisar prazos com antecedência de (dias)
          <input
            v-model.number="deadlineAlertDays"
            type="number"
            inputmode="numeric"
            required
            step="1"
            :min="DEADLINE_LIMITS.min"
            :max="DEADLINE_LIMITS.max"
            :aria-invalid="deadlineInvalid"
            aria-describedby="settings-deadline-hint"
            @keydown="blockInvalidNumberKeys"
          >
          <span id="settings-deadline-hint" :class="['settings-hint', { 'is-error': deadlineInvalid }]">
            Provas e trabalhos entram nos avisos quando faltarem até {{ deadlineInvalid ? '…' : deadlineAlertDays }} dia(s).
            Valores de {{ DEADLINE_LIMITS.min }} a {{ DEADLINE_LIMITS.max }}.
          </span>
        </label>

        <label class="settings-field">
          Margem do aviso de frequência (pontos percentuais)
          <input
            v-model.number="attendanceAlertMargin"
            type="number"
            inputmode="numeric"
            required
            step="1"
            :min="MARGIN_LIMITS.min"
            :max="MARGIN_LIMITS.max"
            :aria-invalid="marginInvalid"
            aria-describedby="settings-margin-hint"
            @keydown="blockInvalidNumberKeys"
          >
          <span id="settings-margin-hint" :class="['settings-hint', { 'is-error': marginInvalid }]">
            <template v-if="attendanceExample !== null">
              Exemplo: numa disciplina com mínimo de {{ EXAMPLE_MINIMUM_ATTENDANCE }}%, você será avisado abaixo de {{ attendanceExample }}%.
            </template>
            <template v-else>Use um valor de {{ MARGIN_LIMITS.min }} a {{ MARGIN_LIMITS.max }}.</template>
          </span>
        </label>
      </div>

      <label class="settings-field">
        Meta de média (opcional)
        <input
          v-model="gradeGoal"
          type="number"
          inputmode="decimal"
          step="0.1"
          :min="GOAL_LIMITS.min"
          :max="GOAL_LIMITS.max"
          placeholder="Ex.: 8,5"
          :aria-invalid="goalInvalid"
          aria-describedby="settings-goal-hint"
          @keydown="blockInvalidNumberKeys"
        >
        <span id="settings-goal-hint" :class="['settings-hint', { 'is-error': goalInvalid }]">
          {{ goalInvalid
            ? 'Use um valor de 0 a 10, com no máximo uma casa decimal.'
            : 'Aparece na tela Notas como “Meta definida”. Deixe em branco para não usar meta.' }}
        </span>
      </label>

      <fieldset class="settings-fieldset">
        <legend>Tela inicial ao entrar</legend>
        <div class="settings-options">
          <label v-for="option in START_SECTIONS" :key="option.value" class="settings-option">
            <input v-model="startSection" type="radio" name="settings-start-section" :value="option.value">
            <span>{{ option.label }}</span>
          </label>
        </div>
      </fieldset>

      <div class="settings-actions">
        <button class="settings-button is-secondary" type="button" :disabled="!hasChanges || saving" @click="reset">
          Descartar
        </button>
        <button
          class="settings-button is-primary"
          type="submit"
          :disabled="!hasChanges || deadlineInvalid || marginInvalid || goalInvalid || saving"
        >
          {{ saving ? 'Salvando…' : 'Salvar preferências' }}
        </button>
      </div>
    </form>
  </section>
</template>
