<script setup>
import { computed, ref, watch } from 'vue'
import { attendanceAfterAbsences } from './frequencyRules.js'

const props = defineProps({
  // Linhas montadas na FrequencyPage: id, name, configured,
  // absences, attendancePercentage e minimumPercentage.
  rows: { type: Array, required: true },
  initialDisciplineId: { type: String, default: '' },
})

const emit = defineEmits(['close', 'save'])

const NOTE_LIMIT = 300

const reasons = ['Saúde', 'Pessoal', 'Trabalho', 'Transporte', 'Outro']

function todayIso() {
  const now = new Date()
  return new Date(now.getTime() - now.getTimezoneOffset() * 60000)
    .toISOString()
    .slice(0, 10)
}

const date = ref(todayIso())
const selectedId = ref(props.initialDisciplineId || props.rows[0]?.id || '')
const quantity = ref(1)
const reason = ref(reasons[0])
const note = ref('')
const formError = ref('')

const selected = computed(() => props.rows.find(row => row.id === selectedId.value) ?? null)

watch(selected, () => {
  formError.value = ''
})

const preview = computed(() => {
  const row = selected.value
  if (!row) return null

  const amount = Number(quantity.value) || 0
  const next = attendanceAfterAbsences(row.absences + amount)

  return {
    current: row.attendancePercentage,
    next,
    impact: row.attendancePercentage - next,
    below: next < row.minimumPercentage,
  }
})

function formatPercentage(value, digits = 0) {
  return typeof value === 'number'
    ? `${value.toLocaleString('pt-BR', { maximumFractionDigits: digits })}%`
    : '—'
}

function submitForm() {
  const row = selected.value

  if (!row) {
    formError.value = 'Selecione uma disciplina.'
    return
  }

  const amount = Number(quantity.value)

  if (!Number.isInteger(amount) || amount < 1) {
    formError.value = 'A quantidade de faltas precisa ser um número inteiro maior que zero.'
    return
  }

  if (!date.value) {
    formError.value = 'Informe a data da falta.'
    return
  }

  formError.value = ''

  emit('save', {
    disciplineId: row.id,
    date: date.value,
    quantity: amount,
    reason: reason.value,
    note: note.value.trim(),
  })
}
</script>

<template>
  <div class="modal-backdrop" @mousedown.self="emit('close')">
    <section class="absence-modal" role="dialog" aria-modal="true" aria-labelledby="register-absence-title">
      <header class="modal-header">
        <div class="modal-title">
          <span aria-hidden="true">
            <svg viewBox="0 0 24 24"><rect x="3" y="5" width="18" height="16" rx="2" /><path d="M7 3v4m10-4v4M3 10h18" /></svg>
          </span>
          <div>
            <h2 id="register-absence-title">Registrar falta</h2>
            <p>Adicione uma falta ao histórico da disciplina.</p>
          </div>
        </div>

        <button class="modal-close" type="button" aria-label="Fechar modal" @click="emit('close')">×</button>
      </header>

      <form @submit.prevent="submitForm">
        <label class="form-field">
          <span>Data da falta <strong>*</strong></span>
          <input v-model="date" type="date" required>
        </label>

        <label class="form-field">
          <span>Disciplina <strong>*</strong></span>
          <select v-model="selectedId" required>
            <option value="" disabled>Selecione uma disciplina</option>
            <option v-for="row in rows" :key="row.id" :value="row.id">
              {{ row.name }}
            </option>
          </select>
        </label>

        <div class="paired-fields">
          <label class="form-field">
            <span>Quantidade de faltas <strong>*</strong></span>
            <input v-model.number="quantity" type="number" min="1" step="1" required>
          </label>

          <label class="form-field">
            <span>Motivo <strong>*</strong></span>
            <select v-model="reason" required>
              <option v-for="option in reasons" :key="option" :value="option">{{ option }}</option>
            </select>
          </label>
        </div>

        <label class="form-field">
          <span>Observação <small>(opcional)</small></span>
          <div class="textarea-wrapper">
            <textarea v-model="note" :maxlength="NOTE_LIMIT" rows="3" placeholder="Ex.: Consulta médica."></textarea>
            <small class="char-count">{{ note.length }}/{{ NOTE_LIMIT }}</small>
          </div>
        </label>

        <p class="info-box">
          <svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="9" /><path d="M12 11v5m0-8.5v.5" /></svg>
          <span>
            Duas aulas seguidas da mesma disciplina podem contar como 1 falta.
            Para disciplinas diferentes, registre uma falta em cada disciplina.
          </span>
        </p>

        <section v-if="preview" class="impact-box" aria-labelledby="impact-title">
          <h3 id="impact-title">Impacto na frequência</h3>

          <div class="impact-grid">
            <div>
              <small>Frequência atual</small>
              <strong>{{ formatPercentage(preview.current) }}</strong>
            </div>
            <div>
              <small>Impacto</small>
              <span class="impact-badge">-{{ preview.impact.toLocaleString('pt-BR') }}%</span>
            </div>
            <div>
              <small>Nova frequência</small>
              <strong :class="{ 'is-below': preview.below }">{{ formatPercentage(preview.next) }}</strong>
            </div>
          </div>

          <span class="impact-track" aria-hidden="true">
            <span :class="{ 'is-below': preview.below }" :style="{ width: `${Math.max(preview.next, 0)}%` }"></span>
          </span>
        </section>

        <p v-else class="warning-box">
          <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 4 2.7 20h18.6L12 4Z" /><path d="M12 10v4m0 3v.5" /></svg>
          <span>Selecione uma disciplina para visualizar o impacto das faltas.</span>
        </p>

        <p v-if="formError" class="form-error" role="alert">{{ formError }}</p>

        <footer class="modal-footer">
          <button class="cancel-button" type="button" @click="emit('close')">Cancelar</button>
          <button class="save-button" type="submit" :disabled="!selected">
            <svg viewBox="0 0 24 24" aria-hidden="true"><rect x="3" y="5" width="18" height="16" rx="2" /><path d="M7 3v4m10-4v4M3 10h18m-6 5 2 2 4-4" /></svg>
            Registrar falta
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

.absence-modal {
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

.modal-title > span svg,
.save-button svg,
.info-box svg,
.warning-box svg {
  fill: none;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 1.7;
}

.modal-title > span svg {
  height: 25px;
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

.absence-modal form {
  display: grid;
  gap: 19px;
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

.form-field small {
  color: #656c80;
  font-size: inherit;
  font-weight: 450;
}

.paired-fields {
  display: grid;
  gap: 16px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.form-field input,
.form-field select,
.textarea-wrapper {
  background-color: #fff;
  border: 1px solid #dfe1e8;
  border-radius: 7px;
  color: #242a3d;
  color-scheme: light;
  font-weight: 400;
  outline: none;
}

.form-field input {
  font-size: .74rem;
  padding: 12px 14px;
}

.form-field select {
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='14' height='14' viewBox='0 0 24 24' fill='none' stroke='%23575e73' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='m6 9 6 6 6-6'/%3E%3C/svg%3E");
  background-position: right 17px center;
  background-repeat: no-repeat;
  font-size: .74rem;
  padding: 12px 42px 12px 14px;
}

.textarea-wrapper {
  display: grid;
  padding: 4px 4px 6px;
}

.textarea-wrapper textarea {
  background: transparent;
  border: 0;
  color: #242a3d;
  font-family: inherit;
  font-size: .74rem;
  font-weight: 400;
  outline: none;
  padding: 9px 10px;
  resize: vertical;
}

.textarea-wrapper textarea::placeholder {
  color: #aaa7b5;
  opacity: 1;
}

.char-count {
  color: #8d93a5;
  font-size: .62rem;
  font-weight: 400;
  justify-self: end;
  padding-right: 8px;
}

.form-field input:focus,
.form-field select:focus,
.textarea-wrapper:focus-within {
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
}

.info-box svg,
.warning-box svg {
  flex: 0 0 18px;
  height: 18px;
  stroke-width: 1.8;
  width: 18px;
}

.impact-box {
  background: #fbfaff;
  border: 1px solid #ebe7f7;
  border-radius: 10px;
  padding: 18px;
}

.impact-box h3 {
  color: #262b3e;
  font-size: .92rem;
  font-weight: 800;
  letter-spacing: -.02em;
  margin: 0 0 14px;
}

.impact-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  margin-bottom: 14px;
  text-align: center;
}

.impact-grid > div {
  display: grid;
  gap: 7px;
  justify-items: center;
}

.impact-grid > div + div {
  border-left: 1px solid #eae7f3;
}

.impact-grid small {
  color: #6d7489;
  font-size: .65rem;
}

.impact-grid strong {
  color: #1f2436;
  font-size: 1.3rem;
  line-height: 1;
}

.impact-grid strong.is-below {
  color: #d9811a;
}

.impact-badge {
  background: #ffeceb;
  border-radius: 999px;
  color: #c93a2c;
  font-size: .68rem;
  font-weight: 700;
  padding: 5px 11px;
}

.impact-track {
  background: #e8e9ee;
  border-radius: 999px;
  display: block;
  height: 7px;
  overflow: hidden;
}

.impact-track span {
  background: #20aa60;
  border-radius: inherit;
  display: block;
  height: 100%;
}

.impact-track span.is-below {
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
  height: 17px;
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

  .absence-modal {
    border-radius: 12px;
    padding: 22px 18px;
  }

  .paired-fields {
    grid-template-columns: 1fr;
  }

  .impact-grid {
    gap: 14px;
    grid-template-columns: 1fr;
  }

  .impact-grid > div + div {
    border-left: 0;
    border-top: 1px solid #eae7f3;
    padding-top: 12px;
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
