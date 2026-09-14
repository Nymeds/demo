<script setup>
import { computed, ref, watch } from 'vue'

const props = defineProps({
  // Linhas montadas na FrequencyPage: id, name, color, minimumPercentage,
  // configured, totalClasses e absences.
  rows: { type: Array, required: true },
  initialDisciplineId: { type: String, default: '' },
})

const emit = defineEmits(['close', 'save'])

const DEFAULT_MINIMUM = 75
const DEFAULT_TOTAL_CLASSES = 20

const selectedId = ref(props.initialDisciplineId || props.rows[0]?.id || '')
const minimumAttendance = ref(DEFAULT_MINIMUM)
const totalClasses = ref(DEFAULT_TOTAL_CLASSES)
const formError = ref('')

const selected = computed(() => props.rows.find(row => row.id === selectedId.value) ?? null)

function applySelected(row) {
  if (!row) return
  minimumAttendance.value = Number(row.minimumPercentage) || DEFAULT_MINIMUM
  totalClasses.value = row.totalClasses ?? DEFAULT_TOTAL_CLASSES
  formError.value = ''
}

applySelected(selected.value)
watch(selected, applySelected)

const total = computed(() => Number(totalClasses.value) || 0)
const minimum = computed(() => Number(minimumAttendance.value) || 0)

const lossPerAbsence = computed(() => (total.value > 0 ? 100 / total.value : 0))

// Mesmo arredondamento do FrequencyService: meia aula a menos não cumpre a exigência.
const minimumClasses = computed(() => (
  total.value > 0 ? Math.ceil((total.value * minimum.value) / 100) : 0
))

const maximumAbsences = computed(() => Math.max(total.value - minimumClasses.value, 0))

const simulation = computed(() => {
  if (total.value <= 0) return []

  const steps = [...new Set([0, 1, maximumAbsences.value])]
    .filter(absences => absences <= total.value)
    .sort((first, second) => first - second)

  return steps.map(absences => {
    const percentage = ((total.value - absences) / total.value) * 100
    return { absences, percentage, below: percentage < minimum.value }
  })
})

function formatPercentage(value, digits = 0) {
  return `${value.toLocaleString('pt-BR', { maximumFractionDigits: digits })}%`
}

function submitForm() {
  if (!selected.value) {
    formError.value = 'Selecione uma disciplina.'
    return
  }

  if (!Number.isInteger(total.value) || total.value <= 0) {
    formError.value = 'O total de aulas precisa ser um número inteiro maior que zero.'
    return
  }

  if (minimum.value <= 0 || minimum.value > 100) {
    formError.value = 'A frequência mínima precisa estar entre 1% e 100%.'
    return
  }

  if (selected.value.absences > total.value) {
    formError.value = `Esta disciplina já tem ${selected.value.absences} faltas registradas. `
      + 'O total de aulas precisa ser maior ou igual a esse número.'
    return
  }

  formError.value = ''

  emit('save', {
    disciplineId: selected.value.id,
    totalClasses: total.value,
    minimumAttendancePercentage: minimum.value,
  })
}
</script>

<template>
  <div class="modal-backdrop" @mousedown.self="emit('close')">
    <section class="frequency-modal" role="dialog" aria-modal="true" aria-labelledby="config-frequency-title">
      <header class="modal-header">
        <div class="modal-title">
          <span aria-hidden="true">
            <svg viewBox="0 0 24 24">
              <circle cx="12" cy="12" r="3.2" />
              <path d="M19.4 15a1.6 1.6 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.6 1.6 0 0 0-1.8-.3 1.6 1.6 0 0 0-1 1.5V21a2 2 0 0 1-4 0v-.1a1.6 1.6 0 0 0-1-1.5 1.6 1.6 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.6 1.6 0 0 0 .3-1.8 1.6 1.6 0 0 0-1.5-1H3a2 2 0 0 1 0-4h.1a1.6 1.6 0 0 0 1.5-1 1.6 1.6 0 0 0-.3-1.8l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.6 1.6 0 0 0 1.8.3H9a1.6 1.6 0 0 0 1-1.5V3a2 2 0 0 1 4 0v.1a1.6 1.6 0 0 0 1 1.5 1.6 1.6 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.6 1.6 0 0 0-.3 1.8V9a1.6 1.6 0 0 0 1.5 1H21a2 2 0 0 1 0 4h-.1a1.6 1.6 0 0 0-1.5 1Z" />
            </svg>
          </span>
          <div>
            <h2 id="config-frequency-title">Configurar frequência</h2>
            <p>Defina as regras de frequência de cada disciplina.</p>
          </div>
        </div>

        <button class="modal-close" type="button" aria-label="Fechar modal" @click="emit('close')">×</button>
      </header>

      <form @submit.prevent="submitForm">
        <label class="form-field">
          <span>Disciplina <strong>*</strong></span>
          <select v-model="selectedId" required autofocus>
            <option value="" disabled>Selecione uma disciplina</option>
            <option v-for="row in rows" :key="row.id" :value="row.id">{{ row.name }}</option>
          </select>
        </label>

        <div class="paired-fields">
          <label class="form-field">
            <span>Frequência mínima permitida <strong>*</strong></span>
            <div class="input-with-suffix">
              <input v-model.number="minimumAttendance" type="number" min="1" max="100" step="1" required>
              <span aria-hidden="true">%</span>
            </div>
          </label>

          <label class="form-field">
            <span>Total de aulas no período <strong>*</strong></span>
            <div class="input-with-suffix">
              <input v-model.number="totalClasses" type="number" min="1" step="1" required>
              <span aria-hidden="true">aulas</span>
            </div>
          </label>
        </div>

        <p class="info-box">
          <svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="9" /><path d="M12 11v5m0-8.5v.5" /></svg>
          <span>
            Cada falta reduzirá {{ formatPercentage(lossPerAbsence, 1) }} da frequência desta disciplina.
          </span>
        </p>

        <section class="simulation" aria-labelledby="simulation-title">
          <h3 id="simulation-title">Simulação</h3>

          <div v-for="step in simulation" :key="step.absences" class="simulation-row">
            <span class="simulation-label">
              {{ step.absences }} {{ step.absences === 1 ? 'falta' : 'faltas' }}
            </span>
            <span class="simulation-value">= {{ formatPercentage(step.percentage) }}</span>
            <span class="simulation-track" aria-hidden="true">
              <span :class="{ 'is-below': step.below }" :style="{ width: `${step.percentage}%` }"></span>
            </span>
          </div>

          <p class="warning-box">
            <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 4 2.7 20h18.6L12 4Z" /><path d="M12 10v4m0 3v.5" /></svg>
            <span>
              Abaixo de {{ formatPercentage(minimum) }}, a situação será considerada ruim. Você pode faltar
              até {{ maximumAbsences }} {{ maximumAbsences === 1 ? 'aula' : 'aulas' }} no período.
            </span>
          </p>
        </section>

        <p v-if="formError" class="form-error" role="alert">{{ formError }}</p>

        <footer class="modal-footer">
          <button class="cancel-button" type="button" @click="emit('close')">Cancelar</button>
          <button class="save-button" type="submit" :disabled="!selected">
            <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M5 4h12l2 2v14H5V4Z" /><path d="M8 4v6h8V4M9 20v-6h6v6" /></svg>
            Salvar configuração
          </button>
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
  overflow-y: auto;
  padding: 30px 18px;
  position: fixed;
  z-index: 100;
}

.frequency-modal {
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 24px 70px rgba(15, 18, 35, .28);
  color: #202538;
  max-height: calc(100dvh - 32px);
  max-width: 620px;
  overflow-y: auto;
  overscroll-behavior: contain;
  padding: 28px 32px 25px;
  width: 100%;
}

.modal-header,
.modal-title,
.modal-footer,
.save-button {
  align-items: center;
  display: flex;
}

.modal-header {
  justify-content: space-between;
  margin-bottom: 25px;
}

.modal-title {
  gap: 14px;
}

.modal-title > span {
  align-items: center;
  background: #f0eaff;
  border-radius: 9px;
  color: #6731df;
  display: flex;
  height: 50px;
  justify-content: center;
  width: 50px;
}

.modal-title > span svg {
  fill: none;
  height: 25px;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 1.7;
  width: 25px;
}

.modal-title h2 {
  color: #303548;
  font-size: 1.22rem;
  font-weight: 800;
  letter-spacing: -.03em;
  margin: 0 0 5px;
}

.modal-title p {
  color: #70778b;
  font-size: .73rem;
}

.modal-close {
  background: transparent;
  border: 0;
  color: #4e5569;
  font-size: 1.7rem;
  line-height: 1;
  padding: 6px;
}

.frequency-modal form {
  display: grid;
  gap: 21px;
}

.form-field {
  color: #282d40;
  display: grid;
  font-size: .74rem;
  font-weight: 700;
  gap: 8px;
}

.form-field strong {
  color: #e84260;
}

.paired-fields {
  display: grid;
  gap: 16px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.form-field select {
  appearance: none;
  background-color: #fff;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='14' height='14' viewBox='0 0 24 24' fill='none' stroke='%23575e73' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='m6 9 6 6 6-6'/%3E%3C/svg%3E");
  background-position: right 17px center;
  background-repeat: no-repeat;
  border: 1px solid #dfe1e8;
  border-radius: 7px;
  color: #242a3d;
  font-size: .74rem;
  font-weight: 400;
  outline: none;
  padding: 12px 42px 12px 14px;
}

.input-with-suffix {
  align-items: center;
  background: #fff;
  border: 1px solid #dfe1e8;
  border-radius: 7px;
  display: flex;
  overflow: hidden;
}

.input-with-suffix input {
  border: 0;
  color: #242a3d;
  flex: 1;
  font-weight: 400;
  min-width: 0;
  outline: none;
  padding: 12px 14px;
}

.input-with-suffix > span {
  align-self: stretch;
  border-left: 1px solid #eceef3;
  color: #6d7489;
  display: grid;
  font-size: .7rem;
  font-weight: 400;
  padding: 0 13px;
  place-items: center;
}

.form-field select:focus,
.input-with-suffix:focus-within {
  border-color: #7544eb;
  box-shadow: 0 0 0 3px rgba(117, 68, 235, .11);
}

.info-box,
.warning-box {
  align-items: flex-start;
  border-radius: 8px;
  display: flex;
  font-size: .7rem;
  gap: 10px;
  line-height: 1.5;
  padding: 12px 14px;
}

.info-box {
  background: #eef4ff;
  color: #2f5493;
}

.warning-box {
  background: #fff4e6;
  color: #9a6414;
  margin-top: 16px;
}

.info-box svg,
.warning-box svg {
  fill: none;
  flex: 0 0 18px;
  height: 18px;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 1.8;
  width: 18px;
}

.simulation {
  background: #fbfaff;
  border: 1px solid #ebe7f7;
  border-radius: 10px;
  padding: 18px;
}

.simulation h3 {
  color: #262b3e;
  font-size: .92rem;
  font-weight: 800;
  letter-spacing: -.02em;
  margin: 0 0 14px;
}

.simulation-row {
  align-items: center;
  display: grid;
  gap: 12px;
  grid-template-columns: 78px 62px 1fr;
  margin-bottom: 10px;
}

.simulation-label {
  color: #4b5268;
  font-size: .7rem;
}

.simulation-value {
  color: #262b3e;
  font-size: .7rem;
  font-weight: 700;
}

.simulation-track {
  background: #e8e9ee;
  border-radius: 999px;
  height: 6px;
  overflow: hidden;
}

.simulation-track span {
  background: #20aa60;
  border-radius: inherit;
  display: block;
  height: 100%;
}

.simulation-track span.is-below {
  background: #f0951f;
}

.form-error {
  color: #b63b4f;
  font-size: .7rem;
  line-height: 1.5;
}

.modal-footer {
  border-top: 1px solid #ececf1;
  gap: 12px;
  justify-content: flex-end;
  margin-top: 4px;
  padding-top: 20px;
}

.cancel-button,
.save-button {
  border-radius: 7px;
  font-size: .72rem;
  font-weight: 700;
  padding: 11px 18px;
}

.cancel-button {
  background: #fff;
  border: 1px solid #dfe1e8;
  color: #343a4e;
}

.save-button {
  background: linear-gradient(100deg, #5d20df, #7419f5);
  border: 0;
  box-shadow: 0 8px 18px rgba(101, 31, 225, .2);
  color: #fff;
  gap: 8px;
}

.save-button:disabled {
  cursor: not-allowed;
  opacity: .55;
}

.save-button svg {
  fill: none;
  height: 17px;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 1.8;
  width: 17px;
}

button:focus-visible {
  outline: 3px solid rgba(105, 54, 224, .28);
  outline-offset: 2px;
}

@media (max-width: 680px) {
  .modal-backdrop {
    align-items: flex-start;
    padding: 12px;
  }

  .frequency-modal {
    border-radius: 12px;
    padding: 22px 18px;
  }

  .paired-fields {
    grid-template-columns: 1fr;
  }

  .simulation-row {
    grid-template-columns: 70px 56px 1fr;
  }

  .modal-footer {
    align-items: stretch;
    flex-direction: column-reverse;
  }

  .save-button {
    justify-content: center;
  }
}
</style>
