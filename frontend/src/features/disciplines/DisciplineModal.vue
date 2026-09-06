<script setup>
import { computed, ref } from 'vue'

const props = defineProps({
  discipline: { type: Object, default: null },
})

const emit = defineEmits(['close', 'save'])

const isEditing = computed(() => Boolean(props.discipline))
const name = ref(props.discipline?.name ?? '')
const professorName = ref(props.discipline?.professorName ?? '')
const selectedColor = ref(props.discipline?.color ?? '#6432df')
const passingAverage = ref(props.discipline?.passingAverage ?? 6)
const minimumAttendancePercentage = ref(props.discipline?.minimumAttendancePercentage ?? 75)
const timeError = ref('')

const initialSchedules = props.discipline?.schedules?.length
  ? props.discipline.schedules.map((schedule, index) => ({ id: index + 1, ...schedule }))
  : [{ id: 1, dayOfWeek: 'MONDAY', startTime: '', endTime: '' }]

let nextScheduleId = initialSchedules.length + 1
const schedules = ref(initialSchedules)

const weekDays = [
  { value: 'MONDAY', label: 'Segunda-feira' },
  { value: 'TUESDAY', label: 'Terça-feira' },
  { value: 'WEDNESDAY', label: 'Quarta-feira' },
  { value: 'THURSDAY', label: 'Quinta-feira' },
  { value: 'FRIDAY', label: 'Sexta-feira' },
  { value: 'SATURDAY', label: 'Sábado' },
]

const colors = [
  '#6432df',
  '#3182f6',
  '#16b978',
  '#2bc1b5',
  '#ff8a24',
  '#ec4f8f',
  '#9b6ce3',
  '#aeb5c4',
]

function addSchedule() {
  schedules.value.push({
    id: nextScheduleId++,
    dayOfWeek: 'MONDAY',
    startTime: '',
    endTime: '',
  })
}

function removeSchedule(index) {
  schedules.value.splice(index, 1)
}

function submitForm() {
  if (schedules.value.length === 0) {
    timeError.value = 'Adicione pelo menos um horário para a disciplina.'
    return
  }

  const hasInvalidTime = schedules.value.some(schedule => (
    schedule.startTime && schedule.endTime && schedule.endTime <= schedule.startTime
  ))

  if (hasInvalidTime) {
    timeError.value = 'O horário final precisa ser posterior ao horário inicial.'
    return
  }

  timeError.value = ''

  emit('save', {
    name: name.value.trim(),
    professorName: professorName.value.trim(),
    color: selectedColor.value,
    passingAverage: passingAverage.value,
    minimumAttendancePercentage: minimumAttendancePercentage.value,
    schedules: schedules.value.map(({ dayOfWeek, startTime, endTime }) => ({
      dayOfWeek,
      startTime,
      endTime,
    })),
  })
}
</script>

<template>
  <div class="modal-backdrop" @mousedown.self="emit('close')">
    <section class="discipline-modal" role="dialog" aria-modal="true" aria-labelledby="new-discipline-title">
      <header class="modal-header">
        <div class="modal-title">
          <span aria-hidden="true">＋</span>
          <div>
            <h2 id="new-discipline-title">{{ isEditing ? 'Editar disciplina' : 'Nova disciplina' }}</h2>
            <p>{{ isEditing ? 'Atualize as informações da disciplina.' : 'Preencha as informações da sua nova disciplina.' }}</p>
          </div>
        </div>

        <button class="modal-close" type="button" aria-label="Fechar modal" @click="emit('close')">×</button>
      </header>

      <form @submit.prevent="submitForm">
        <label class="form-field">
          <span>Nome da disciplina <strong>*</strong></span>
          <input v-model.trim="name" type="text" maxlength="120" placeholder="Ex.: Estruturas de Dados" required autofocus>
        </label>

        <label class="form-field">
          <span>Professor <small>(opcional)</small></span>
          <input v-model.trim="professorName" type="text" maxlength="120" placeholder="Ex.: Prof. João da Silva">
        </label>

        <div class="performance-fields">
          <label class="form-field">
            <span>Média de aprovação <strong>*</strong></span>
            <input v-model.number="passingAverage" type="number" min="0" max="10" step="0.01" required>
          </label>

          <label class="form-field">
            <span>Frequência mínima (%) <strong>*</strong></span>
            <input v-model.number="minimumAttendancePercentage" type="number" min="0" max="100" step="0.01" required>
          </label>
        </div>

        <fieldset class="schedule-fieldset">
          <legend>Horário das aulas <strong>*</strong></legend>
          <p>Adicione os dias da semana e os respectivos horários.</p>

          <div v-for="(schedule, index) in schedules" :key="schedule.id" class="schedule-row">
            <select v-model="schedule.dayOfWeek" aria-label="Dia da semana" required>
              <option v-for="day in weekDays" :key="day.value" :value="day.value">{{ day.label }}</option>
            </select>

            <input v-model="schedule.startTime" type="time" :aria-label="`Horário inicial ${index + 1}`" required>
            <span class="schedule-separator">até</span>
            <input v-model="schedule.endTime" type="time" :aria-label="`Horário final ${index + 1}`" required>

            <button
              class="remove-schedule"
              type="button"
              :aria-label="`Remover horário ${index + 1}`"
              @click="removeSchedule(index)"
            >
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="M4 7h16M9 7V4h6v3m3 0-1 13H7L6 7m4 4v5m4-5v5" />
              </svg>
            </button>
          </div>

          <p v-if="timeError" class="time-error" role="alert">{{ timeError }}</p>

          <button class="add-schedule" type="button" @click="addSchedule">
            <span aria-hidden="true">＋</span>
            Adicionar horário
          </button>
        </fieldset>

        <fieldset class="color-fieldset">
          <legend>Cor da disciplina <strong>*</strong></legend>
          <p>Escolha uma cor para identificar sua disciplina no calendário e nas listas.</p>

          <div class="color-options">
            <label v-for="color in colors" :key="color" :style="{ '--discipline-color': color }">
              <input v-model="selectedColor" type="radio" name="discipline-color" :value="color">
              <span :aria-label="`Selecionar a cor ${color}`">
                <svg v-if="selectedColor === color" viewBox="0 0 24 24" aria-hidden="true"><path d="m6 12 4 4 8-9" /></svg>
              </span>
            </label>
          </div>
        </fieldset>

        <footer class="modal-footer">
          <button class="cancel-button" type="button" @click="emit('close')">Cancelar</button>
          <button class="save-button" type="submit">
            <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M5 4h12l2 2v14H5V4Z" /><path d="M8 4v6h8V4M9 20v-6h6v6" /></svg>
            {{ isEditing ? 'Salvar alterações' : 'Salvar disciplina' }}
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

.discipline-modal {
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 24px 70px rgba(15, 18, 35, .28);
  color: #202538;
  max-width: 720px;
  max-height: calc(100dvh - 32px);
  overflow-y: auto;
  overscroll-behavior: contain;
  padding: 28px 32px 25px;
  width: 100%;
}

.modal-header,
.modal-title,
.modal-footer,
.save-button,
.add-schedule {
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
  font-size: 1.9rem;
  height: 50px;
  justify-content: center;
  line-height: 1;
  width: 50px;
}

.modal-title h2 {
  color: #303548;
  font-size: 1.22rem;
  font-weight: 800;
  letter-spacing: -.03em;
  margin: 0 0 5px;
}

.modal-title p,
.schedule-fieldset > p,
.color-fieldset > p {
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

.discipline-modal form {
  display: grid;
  gap: 21px;
}

.performance-fields {
  display: grid;
  gap: 16px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.form-field {
  color: #282d40;
  display: grid;
  font-size: .74rem;
  font-weight: 700;
  gap: 8px;
}

.form-field strong,
.schedule-fieldset strong,
.color-fieldset strong {
  color: #e84260;
}

.form-field small {
  color: #656c80;
  font-size: inherit;
  font-weight: 450;
}

.form-field > input,
.schedule-row select,
.schedule-row input {
  background: #fff;
  border: 1px solid #dfe1e8;
  border-radius: 7px;
  color: #242a3d;
  outline: none;
}

.form-field > input {
  font-weight: 400;
  padding: 12px 14px;
}

.form-field > input::placeholder {
  color: #aaa7b5;
  font-weight: 400;
  opacity: 1;
}

.form-field input:focus,
.schedule-row select:focus,
.schedule-row input:focus {
  border-color: #7544eb;
  box-shadow: 0 0 0 3px rgba(117, 68, 235, .11);
}

.schedule-fieldset,
.color-fieldset {
  border: 0;
  margin: 0;
  min-width: 0;
  padding: 0;
}

.schedule-fieldset legend,
.color-fieldset legend {
  color: #282d40;
  font-size: .74rem;
  font-weight: 700;
  margin-bottom: 5px;
  padding: 0;
}

.schedule-fieldset > p,
.color-fieldset > p {
  margin-bottom: 13px;
}

.schedule-row {
  align-items: center;
  display: grid;
  gap: 12px;
  grid-template-columns: minmax(170px, 1fr) 115px auto 115px 43px;
  margin-top: 10px;
}

.schedule-row select,
.schedule-row input {
  background-color: #fff;
  color-scheme: light;
  height: 42px;
  min-width: 0;
  padding: 0 12px;
  width: 100%;
}

.schedule-row input[type="time"] {
  font-size: .68rem;
  font-weight: 400;
}

.schedule-row select {
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='14' height='14' viewBox='0 0 24 24' fill='none' stroke='%23575e73' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='m6 9 6 6 6-6'/%3E%3C/svg%3E");
  background-position: right 17px center;
  background-repeat: no-repeat;
  font-size: .72rem;
  padding-right: 42px;
}

.schedule-separator {
  color: #73798b;
  font-size: .68rem;
}

.remove-schedule {
  align-items: center;
  background: #fff;
  border: 1px solid #dfe1e8;
  border-radius: 7px;
  color: #596075;
  display: flex;
  height: 42px;
  justify-content: center;
  width: 43px;
}

.remove-schedule svg,
.save-button svg {
  fill: none;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 1.8;
}

.remove-schedule svg {
  height: 18px;
  width: 18px;
}

.time-error {
  color: #b63b4f;
  font-size: .68rem;
  margin-top: 8px;
}

.add-schedule {
  background: #fff;
  border: 1px solid #cfc4ef;
  border-radius: 7px;
  color: #6030cb;
  font-size: .7rem;
  font-weight: 700;
  gap: 7px;
  margin-top: 12px;
  padding: 9px 13px;
}

.add-schedule span {
  font-size: 1.05rem;
  line-height: .8;
}

.color-options {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
}

.color-options label {
  cursor: pointer;
}

.color-options input {
  height: 1px;
  opacity: 0;
  position: absolute;
  width: 1px;
}

.color-options label > span {
  align-items: center;
  background: var(--discipline-color);
  border: 3px solid #fff;
  border-radius: 50%;
  box-shadow: 0 0 0 1px transparent;
  color: #fff;
  display: flex;
  height: 33px;
  justify-content: center;
  width: 33px;
}

.color-options input:checked + span {
  box-shadow: 0 0 0 2px var(--discipline-color);
}

.color-options input:focus-visible + span {
  outline: 3px solid rgba(105, 54, 224, .3);
  outline-offset: 3px;
}

.color-options svg {
  fill: none;
  height: 19px;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 2.2;
  width: 19px;
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

  .discipline-modal {
    border-radius: 12px;
    padding: 22px 18px;
  }

  .modal-title > span {
    height: 43px;
    width: 43px;
  }

  .schedule-row {
    grid-template-columns: 1fr 1fr 43px;
  }

  .performance-fields {
    grid-template-columns: 1fr;
  }

  .schedule-row select {
    grid-column: 1 / -1;
  }

  .schedule-separator {
    display: none;
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
