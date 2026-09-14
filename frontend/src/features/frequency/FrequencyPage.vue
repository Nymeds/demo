<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import AppToast from '../../components/ui/AppToast.vue'
import FrequencyConfigModal from './FrequencyConfigModal.vue'
import RegisterAbsenceModal from './RegisterAbsenceModal.vue'
import ResetFrequencyModal from './ResetFrequencyModal.vue'

const props = defineProps({
  accessToken: { type: String, required: true },
})

// Margem, em pontos percentuais acima do mínimo exigido, para sinalizar "Atenção".
const ATTENTION_MARGIN = 10
const HISTORY_PREVIEW_SIZE = 3

const loading = ref(true)
const requestError = ref('')
const dashboardId = ref('')
const disciplines = ref([])
const periodFilter = ref('all')
const situationFilter = ref('all')
const showConfigModal = ref(false)
const showAbsenceModal = ref(false)
const modalTargetId = ref('')
const rowToReset = ref(null)
const showAllHistory = ref(false)
const toast = ref({ message: '', type: 'success' })

// O backend guarda apenas o contador de faltas. Data, motivo e observação
// vivem aqui enquanto a sessão estiver aberta.
const absenceHistory = ref([])
let historyId = 1
let toastTimer

const situationFilters = [
  { value: 'all', label: 'Todas' },
  { value: 'good', label: 'Ótimo' },
  { value: 'warning', label: 'Atenção' },
  { value: 'bad', label: 'Ruim' },
  { value: 'empty', label: 'Não iniciado' },
]

const situationDetails = {
  good: { label: 'Ótimo', className: 'is-success' },
  warning: { label: 'Atenção', className: 'is-warning' },
  bad: { label: 'Ruim', className: 'is-danger' },
  empty: { label: 'Não iniciado', className: 'is-neutral' },
}

function showToast(message, type = 'success') {
  toast.value = { message, type }
  clearTimeout(toastTimer)
  toastTimer = setTimeout(() => {
    toast.value.message = ''
  }, 4500)
}

function closeToast() {
  clearTimeout(toastTimer)
  toast.value.message = ''
}

async function apiRequest(path, options = {}) {
  const response = await fetch(path, {
    ...options,
    headers: {
      Authorization: `Bearer ${props.accessToken}`,
      ...(options.body ? { 'Content-Type': 'application/json' } : {}),
      ...options.headers,
    },
  })

  const data = response.status === 204
    ? null
    : await response.json().catch(() => ({}))

  if (!response.ok) {
    const fieldErrors = data?.errors && typeof data.errors === 'object'
      ? Object.values(data.errors).filter(Boolean).join(' ')
      : ''

    const error = new Error(
      fieldErrors || data?.detail || data?.message || 'Não foi possível concluir a solicitação.',
    )
    error.status = response.status
    throw error
  }

  return data
}

function frequencyPath(disciplineId) {
  return `/api/v1/dashboards/${dashboardId.value}/disciplines/${disciplineId}/frequency`
}

function disciplinePath(disciplineId) {
  return `/api/v1/dashboards/${dashboardId.value}/disciplines/${disciplineId}`
}

// attendancePercentage chega como BigDecimal e pode ser serializado como string.
function normalizeFrequency(frequency) {
  return {
    ...frequency,
    attendancePercentage: Number(frequency.attendancePercentage),
  }
}

function normalizeDiscipline(discipline) {
  return {
    ...discipline,
    color: discipline.color || '#6432df',
    schedules: (discipline.schedules ?? []).map(schedule => ({
      ...schedule,
      startTime: schedule.startTime.slice(0, 5),
      endTime: schedule.endTime.slice(0, 5),
    })),
  }
}

async function loadFrequency(disciplineId) {
  try {
    return normalizeFrequency(await apiRequest(frequencyPath(disciplineId)))
  } catch (error) {
    if (error.status === 404) return null
    throw error
  }
}

async function loadData() {
  loading.value = true
  requestError.value = ''

  try {
    const dashboards = await apiRequest('/api/v1/dashboards')
    let dashboard = dashboards.find(item => item.status === 'ACTIVE') || dashboards[0]

    if (!dashboard) {
      dashboard = await apiRequest('/api/v1/dashboards', {
        method: 'POST',
        body: JSON.stringify({ name: 'Organização acadêmica', status: 'ACTIVE' }),
      })
    }

    dashboardId.value = dashboard.id
    const savedDisciplines = await apiRequest(`/api/v1/dashboards/${dashboard.id}/disciplines`)

    disciplines.value = await Promise.all(
      savedDisciplines.map(async discipline => ({
        ...normalizeDiscipline(discipline),
        frequency: await loadFrequency(discipline.id),
      })),
    )
  } catch (error) {
    requestError.value = error.message || 'Não foi possível carregar a frequência.'
    showToast(requestError.value, 'error')
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
onBeforeUnmount(() => clearTimeout(toastTimer))

function periodOf(discipline) {
  if (!discipline.periodo) return '—'
  return `${discipline.periodo}.${discipline.semester ?? 1}`
}

function situationOf(attendance, minimum, remainingAbsences) {
  if (attendance < minimum) return 'bad'
  if (remainingAbsences <= 1 || attendance < minimum + ATTENTION_MARGIN) return 'warning'
  return 'good'
}

function buildRow(discipline) {
  const frequency = discipline.frequency
  const minimumPercentage = Number(discipline.minimumAttendancePercentage ?? 0)

  const base = {
    id: discipline.id,
    name: discipline.name,
    // Troque por discipline.code caso você adicione o campo na entidade Discipline.
    subtitle: discipline.professorName || periodOf(discipline),
    color: discipline.color,
    period: periodOf(discipline),
    minimumPercentage,
    discipline,
  }

  if (!frequency) {
    return {
      ...base,
      configured: false,
      totalClasses: null,
      absences: 0,
      attendancePercentage: null,
      lossPerAbsence: null,
      remainingAbsences: null,
      situation: 'empty',
    }
  }

  const remainingAbsences = frequency.maximumAbsences - frequency.absences

  return {
    ...base,
    configured: true,
    totalClasses: frequency.totalClasses,
    absences: frequency.absences,
    attendancePercentage: frequency.attendancePercentage,
    lossPerAbsence: frequency.totalClasses > 0 ? 100 / frequency.totalClasses : null,
    remainingAbsences,
    situation: situationOf(frequency.attendancePercentage, minimumPercentage, remainingAbsences),
  }
}

const rows = computed(() => disciplines.value.map(buildRow))

const periodOptions = computed(() => {
  const periods = new Set(rows.value.map(row => row.period).filter(period => period !== '—'))
  return [...periods].sort().reverse()
})

const rowsInPeriod = computed(() => (
  periodFilter.value === 'all'
    ? rows.value
    : rows.value.filter(row => row.period === periodFilter.value)
))

const filteredRows = computed(() => (
  situationFilter.value === 'all'
    ? rowsInPeriod.value
    : rowsInPeriod.value.filter(row => row.situation === situationFilter.value)
))

const averageAttendance = computed(() => {
  const values = rowsInPeriod.value
    .filter(row => row.configured)
    .map(row => row.attendancePercentage)

  return values.length
    ? values.reduce((total, value) => total + value, 0) / values.length
    : null
})

const totalAbsences = computed(() => (
  rowsInPeriod.value.reduce((total, row) => total + row.absences, 0)
))

const averageLabel = computed(() => {
  const average = averageAttendance.value
  if (average === null) return 'Aguardando configuração'
  if (average >= 90) return 'Boa frequência'
  if (average >= 75) return 'Atenção às faltas'
  return 'Frequência crítica'
})

const visibleHistory = computed(() => (
  showAllHistory.value
    ? absenceHistory.value
    : absenceHistory.value.slice(0, HISTORY_PREVIEW_SIZE)
))

function formatPercentage(value, digits = 0) {
  return typeof value === 'number'
    ? `${value.toLocaleString('pt-BR', { maximumFractionDigits: digits })}%`
    : '—'
}

function formatDate(isoDate) {
  const date = new Date(`${isoDate}T00:00:00`)
  const weekday = date.toLocaleDateString('pt-BR', { weekday: 'short' }).replace('.', '')

  return `${date.toLocaleDateString('pt-BR')} (${weekday.charAt(0).toUpperCase()}${weekday.slice(1)})`
}

function barClass(row) {
  return `is-${row.situation}`
}

function openConfigModal(disciplineId = '') {
  modalTargetId.value = disciplineId
  showConfigModal.value = true
}

function openAbsenceModal(disciplineId = '') {
  modalTargetId.value = disciplineId
  showAbsenceModal.value = true
}

function closeModals() {
  showConfigModal.value = false
  showAbsenceModal.value = false
  modalTargetId.value = ''
}

function replaceFrequency(disciplineId, frequency) {
  const index = disciplines.value.findIndex(item => item.id === disciplineId)
  if (index === -1) return
  disciplines.value[index] = { ...disciplines.value[index], frequency }
}

function disciplinePayload(discipline, minimumAttendancePercentage) {
  return {
    name: discipline.name,
    professorName: discipline.professorName ?? '',
    color: discipline.color,
    passingAverage: discipline.passingAverage,
    minimumAttendancePercentage,
    periodo: String(discipline.periodo),
    semester: String(discipline.semester),
    schedules: (discipline.schedules ?? []).map(({ dayOfWeek, startTime, endTime }) => ({
      dayOfWeek,
      startTime,
      endTime,
    })),
  }
}

async function saveConfig({ disciplineId, totalClasses, minimumAttendancePercentage }) {
  requestError.value = ''

  try {
    const index = disciplines.value.findIndex(item => item.id === disciplineId)
    const discipline = disciplines.value[index]

    // A frequência mínima pertence a Discipline, não a Frequency.
    if (Number(discipline.minimumAttendancePercentage) !== Number(minimumAttendancePercentage)) {
      const updated = await apiRequest(disciplinePath(disciplineId), {
        method: 'PUT',
        body: JSON.stringify(disciplinePayload(discipline, minimumAttendancePercentage)),
      })

      disciplines.value[index] = {
        ...normalizeDiscipline(updated),
        frequency: discipline.frequency,
      }
    }

    // POST na primeira vez (o GET devolveu 404), PUT nas demais.
    const hasFrequency = Boolean(discipline.frequency)
    const frequency = normalizeFrequency(await apiRequest(frequencyPath(disciplineId), {
      method: hasFrequency ? 'PUT' : 'POST',
      body: JSON.stringify({
        totalClasses,
        absences: discipline.frequency?.absences ?? 0,
      }),
    }))

    replaceFrequency(disciplineId, frequency)
    closeModals()
    showToast('Configuração de frequência salva.')
  } catch (error) {
    requestError.value = error.message || 'Não foi possível salvar a configuração.'
    showToast(requestError.value, 'error')
  }
}

async function registerAbsence({ disciplineId, date, quantity, reason, note }) {
  requestError.value = ''

  try {
    const discipline = disciplines.value.find(item => item.id === disciplineId)
    const current = discipline.frequency

    const frequency = normalizeFrequency(await apiRequest(frequencyPath(disciplineId), {
      method: 'PUT',
      body: JSON.stringify({
        totalClasses: current.totalClasses,
        absences: current.absences + quantity,
      }),
    }))

    replaceFrequency(disciplineId, frequency)

    absenceHistory.value.unshift({
      id: historyId++,
      disciplineId,
      disciplineName: discipline.name,
      date,
      quantity,
      reason,
      note,
      impact: current.attendancePercentage - frequency.attendancePercentage,
    })

    closeModals()
    showToast(quantity > 1 ? 'Faltas registradas.' : 'Falta registrada.')
  } catch (error) {
    requestError.value = error.message || 'Não foi possível registrar a falta.'
    showToast(requestError.value, 'error')
  }
}

async function undoAbsence(entry) {
  requestError.value = ''

  try {
    const discipline = disciplines.value.find(item => item.id === entry.disciplineId)

    if (!discipline?.frequency) {
      showToast('A frequência desta disciplina não está mais disponível.', 'error')
      return
    }

    const frequency = normalizeFrequency(await apiRequest(frequencyPath(entry.disciplineId), {
      method: 'PUT',
      body: JSON.stringify({
        totalClasses: discipline.frequency.totalClasses,
        absences: Math.max(discipline.frequency.absences - entry.quantity, 0),
      }),
    }))

    replaceFrequency(entry.disciplineId, frequency)
    absenceHistory.value = absenceHistory.value.filter(item => item.id !== entry.id)
    showToast('Lançamento desfeito.')
  } catch (error) {
    requestError.value = error.message || 'Não foi possível desfazer o lançamento.'
    showToast(requestError.value, 'error')
  }
}

function askToReset(row) {
  rowToReset.value = row
}

function closeResetModal() {
  rowToReset.value = null
}

async function confirmReset() {
  if (!rowToReset.value) return

  const { id, totalClasses } = rowToReset.value
  requestError.value = ''

  try {
    const frequency = normalizeFrequency(await apiRequest(frequencyPath(id), {
      method: 'PUT',
      body: JSON.stringify({ totalClasses, absences: 0 }),
    }))

    replaceFrequency(id, frequency)
    absenceHistory.value = absenceHistory.value.filter(entry => entry.disciplineId !== id)
    closeResetModal()
    showToast('Faltas zeradas.')
  } catch (error) {
    requestError.value = error.message || 'Não foi possível zerar as faltas.'
    showToast(requestError.value, 'error')
  }
}
</script>

<template>
  <section class="frequency-page" aria-labelledby="frequency-title">
    <header class="frequency-header">
      <div class="frequency-heading">
        <h1 id="frequency-title">Frequência</h1>
        <p>Acompanhe suas faltas e a frequência em cada disciplina.</p>
      </div>

      <div class="frequency-actions">
        <label class="frequency-select">
          <span>Período</span>
          <select v-model="periodFilter">
            <option value="all">Todos</option>
            <option v-for="period in periodOptions" :key="period" :value="period">{{ period }}</option>
          </select>
        </label>

        <label class="frequency-select">
          <span>Situação</span>
          <select v-model="situationFilter">
            <option v-for="option in situationFilters" :key="option.value" :value="option.value">
              {{ option.label }}
            </option>
          </select>
        </label>

        <button
          class="primary-button"
          type="button"
          :disabled="loading || !dashboardId"
          @click="openAbsenceModal()"
        >
          <span aria-hidden="true">＋</span>
          Registrar falta
        </button>

        <button
          class="secondary-button"
          type="button"
          :disabled="loading || !dashboardId"
          @click="openConfigModal()"
        >
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <circle cx="12" cy="12" r="3.2" />
            <path d="M19.4 15a1.6 1.6 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.6 1.6 0 0 0-1.8-.3 1.6 1.6 0 0 0-1 1.5V21a2 2 0 0 1-4 0v-.1a1.6 1.6 0 0 0-1-1.5 1.6 1.6 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.6 1.6 0 0 0 .3-1.8 1.6 1.6 0 0 0-1.5-1H3a2 2 0 0 1 0-4h.1a1.6 1.6 0 0 0 1.5-1 1.6 1.6 0 0 0-.3-1.8l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.6 1.6 0 0 0 1.8.3H9a1.6 1.6 0 0 0 1-1.5V3a2 2 0 0 1 4 0v.1a1.6 1.6 0 0 0 1 1.5 1.6 1.6 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.6 1.6 0 0 0-.3 1.8V9a1.6 1.6 0 0 0 1.5 1H21a2 2 0 0 1 0 4h-.1a1.6 1.6 0 0 0-1.5 1Z" />
          </svg>
          Configurar frequência
        </button>
      </div>
    </header>

    <div class="frequency-summary-grid">
      <article class="frequency-total-card is-purple">
        <span aria-hidden="true">
          <svg viewBox="0 0 24 24"><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2Z" /><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20M8 7h8M8 10h6" /></svg>
        </span>
        <div>
          <p>Disciplinas cadastradas</p>
          <strong>{{ rowsInPeriod.length }}</strong>
          <small>{{ periodFilter === 'all' ? 'Todas as disciplinas' : `Período ${periodFilter}` }}</small>
        </div>
      </article>

      <article class="frequency-total-card is-green">
        <span aria-hidden="true">
          <svg viewBox="0 0 24 24"><path d="M12 3a9 9 0 1 0 9 9h-9V3Z" /><path d="M14 3.3A9 9 0 0 1 20.7 10H14V3.3Z" /></svg>
        </span>
        <div>
          <p>Frequência média geral</p>
          <strong>{{ formatPercentage(averageAttendance) }}</strong>
          <small>{{ averageLabel }}</small>
        </div>
      </article>

      <article class="frequency-total-card is-orange">
        <span aria-hidden="true">
          <svg viewBox="0 0 24 24"><path d="M12 4 2.7 20h18.6L12 4Z" /><path d="M12 10v4m0 3v.5" /></svg>
        </span>
        <div>
          <p>Total de faltas</p>
          <strong>{{ totalAbsences }}</strong>
          <small>{{ periodFilter === 'all' ? 'Em todos os períodos' : 'Neste período' }}</small>
        </div>
      </article>
    </div>

    <article v-if="loading" class="frequency-loading-card" aria-live="polite">
      <span class="loading-spinner" aria-hidden="true"></span>
      <p>Carregando frequência...</p>
    </article>

    <article v-else-if="disciplines.length === 0" class="frequency-empty-card">
      <h2>Nenhuma disciplina cadastrada</h2>
      <p>Cadastre uma disciplina para começar a acompanhar as faltas e a frequência do período.</p>
    </article>

    <section v-else class="frequency-table-card" aria-labelledby="frequency-table-title">
      <header class="table-card-header">
        <h2 id="frequency-table-title">Frequência por disciplina</h2>
      </header>

      <div class="frequency-table-scroll">
        <table>
          <colgroup>
            <col class="column-discipline">
            <col class="column-minimum">
            <col class="column-loss">
            <col class="column-absences">
            <col class="column-attendance">
            <col class="column-situation">
            <col class="column-actions">
          </colgroup>
          <thead>
            <tr>
              <th>Disciplina</th>
              <th>Frequência mínima</th>
              <th>Perda por falta</th>
              <th>Faltas</th>
              <th>Frequência atual</th>
              <th>Situação</th>
              <th>Ações</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in filteredRows" :key="row.id">
              <td>
                <div class="discipline-name-cell">
                  <span
                    class="discipline-color"
                    :style="{ backgroundColor: `${row.color}1f`, color: row.color }"
                    aria-hidden="true"
                  >
                    <svg viewBox="0 0 24 24"><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2Z" /><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20M8 7h8M8 10h6" /></svg>
                  </span>
                  <span>
                    <span class="discipline-name">{{ row.name }}</span>
                    <small>{{ row.subtitle }}</small>
                  </span>
                </div>
              </td>
              <td>{{ row.configured ? formatPercentage(row.minimumPercentage) : '—' }}</td>
              <td>{{ row.configured ? formatPercentage(row.lossPerAbsence, 1) : '—' }}</td>
              <td class="absences-cell">{{ row.absences }}</td>
              <td>
                <div class="attendance-cell">
                  <span :class="['attendance-value', barClass(row)]">
                    {{ formatPercentage(row.attendancePercentage) }}
                  </span>
                  <span class="attendance-track" aria-hidden="true">
                    <span
                      v-if="row.configured"
                      :class="barClass(row)"
                      :style="{ width: `${Math.min(row.attendancePercentage, 100)}%` }"
                    ></span>
                  </span>
                </div>
              </td>
              <td>
                <span :class="['frequency-status', situationDetails[row.situation].className]">
                  {{ situationDetails[row.situation].label }}
                </span>
              </td>
              <td>
                <div class="frequency-actions-cell">
                  <button
                    type="button"
                    aria-label="Configurar frequência da disciplina"
                    title="Configurar frequência"
                    @click="openConfigModal(row.id)"
                  >
                    <svg viewBox="0 0 24 24" aria-hidden="true"><path d="m4 20 4-1 11-11-3-3L5 16l-1 4Z" /><path d="m14 7 3 3" /></svg>
                  </button>
                  <button
                    class="reset-action"
                    type="button"
                    :disabled="!row.configured || row.absences === 0"
                    aria-label="Zerar faltas da disciplina"
                    title="Zerar faltas"
                    @click="askToReset(row)"
                  >
                    <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 7h16M9 7V4h6v3m3 0-1 13H7L6 7m4 4v5m4-5v5" /></svg>
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <p v-if="filteredRows.length === 0" class="frequency-no-results">
        Nenhuma disciplina encontrada com esses filtros.
      </p>
    </section>

    <section
      v-if="!loading && disciplines.length > 0"
      class="frequency-history-card"
      aria-labelledby="frequency-history-title"
    >
      <header class="table-card-header">
        <h2 id="frequency-history-title">Histórico recente de faltas</h2>

        <button
          v-if="absenceHistory.length > HISTORY_PREVIEW_SIZE"
          class="see-all"
          type="button"
          @click="showAllHistory = !showAllHistory"
        >
          {{ showAllHistory ? 'Ver menos' : 'Ver todas' }}
          <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M2 12s4-7 10-7 10 7 10 7-4 7-10 7S2 12 2 12Z" /><circle cx="12" cy="12" r="3" /></svg>
        </button>
      </header>

      <p v-if="absenceHistory.length === 0" class="frequency-no-results">
        Nenhuma falta registrada nesta sessão. Os lançamentos aparecem aqui assim que você registrar uma falta.
      </p>

      <div v-else class="frequency-table-scroll">
        <table>
          <thead>
            <tr>
              <th>Data</th>
              <th>Disciplina</th>
              <th>Motivo</th>
              <th>Observação</th>
              <th>Impacto</th>
              <th>Ações</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="entry in visibleHistory" :key="entry.id">
              <td>{{ formatDate(entry.date) }}</td>
              <td>{{ entry.disciplineName }}</td>
              <td>{{ entry.reason }}</td>
              <td>{{ entry.note || '—' }}</td>
              <td><span class="impact-badge">-{{ formatPercentage(entry.impact, 1) }}</span></td>
              <td>
                <div class="frequency-actions-cell">
                  <button
                    class="reset-action"
                    type="button"
                    aria-label="Desfazer lançamento de falta"
                    title="Desfazer lançamento"
                    @click="undoAbsence(entry)"
                  >
                    <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 10h10a5 5 0 0 1 0 10H9" /><path d="m8 6-4 4 4 4" /></svg>
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <AppToast :message="toast.message" :type="toast.type" @close="closeToast" />

    <FrequencyConfigModal
      v-if="showConfigModal"
      :rows="rows"
      :initial-discipline-id="modalTargetId"
      @close="closeModals"
      @save="saveConfig"
    />

    <RegisterAbsenceModal
      v-if="showAbsenceModal"
      :rows="rows"
      :initial-discipline-id="modalTargetId"
      @close="closeModals"
      @save="registerAbsence"
    />

    <ResetFrequencyModal
      v-if="rowToReset"
      :discipline-name="rowToReset.name"
      :absences="rowToReset.absences"
      @close="closeResetModal"
      @confirm="confirmReset"
    />
  </section>
</template>

<style scoped>
.frequency-page {
  display: grid;
  gap: 22px;
}

.frequency-header,
.frequency-actions,
.frequency-total-card,
.table-card-header {
  align-items: center;
  display: flex;
}

.frequency-header {
  gap: 20px;
  justify-content: space-between;
}

.frequency-heading h1 {
  color: #151a2d;
  font-size: 1.65rem;
  font-weight: 800;
  letter-spacing: -.04em;
  line-height: 1.1;
  margin: 0 0 6px;
}

.frequency-heading p {
  color: #697086;
  font-size: .78rem;
}

.frequency-actions {
  flex-wrap: wrap;
  gap: 13px;
}

.frequency-select {
  color: #282d40;
  display: grid;
  font-size: .68rem;
  font-weight: 700;
  gap: 6px;
}

.frequency-select select {
  appearance: none;
  background-color: #fff;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='14' height='14' viewBox='0 0 24 24' fill='none' stroke='%23575e73' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='m6 9 6 6 6-6'/%3E%3C/svg%3E");
  background-position: right 14px center;
  background-repeat: no-repeat;
  border: 1px solid #dedfe8;
  border-radius: 8px;
  color: #242a3d;
  font-weight: 400;
  min-width: 140px;
  outline: none;
  padding: 11px 38px 11px 14px;
}

.frequency-select select:focus {
  border-color: #7544eb;
  box-shadow: 0 0 0 3px rgba(117, 68, 235, .12);
}

.primary-button,
.secondary-button {
  align-items: center;
  align-self: flex-end;
  border-radius: 7px;
  display: flex;
  font-size: .76rem;
  font-weight: 700;
  gap: 8px;
  justify-content: center;
  min-height: 42px;
  padding: 12px 18px;
}

.primary-button {
  background: linear-gradient(100deg, #5d20df, #7419f5);
  border: 0;
  box-shadow: 0 8px 18px rgba(101, 31, 225, .2);
  color: #fff;
}

.primary-button span {
  font-size: 1.12rem;
  font-weight: 400;
  line-height: .8;
}

.primary-button:hover {
  box-shadow: 0 11px 23px rgba(101, 31, 225, .28);
  transform: translateY(-1px);
}

.secondary-button {
  background: #fff;
  border: 1px solid #cfc4ef;
  color: #6030cb;
}

.secondary-button:hover {
  border-color: #7650df;
  box-shadow: 0 6px 14px rgba(101, 31, 225, .12);
}

.secondary-button svg {
  fill: none;
  height: 17px;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 1.7;
  width: 17px;
}

.primary-button:disabled,
.secondary-button:disabled {
  cursor: wait;
  opacity: .6;
}

.primary-button:focus-visible,
.secondary-button:focus-visible,
.see-all:focus-visible,
.frequency-actions-cell button:focus-visible {
  outline: 3px solid rgba(105, 54, 224, .28);
  outline-offset: 3px;
}

.frequency-summary-grid {
  display: grid;
  gap: 16px;
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.frequency-total-card {
  background: #fff;
  border: 1px solid #ebeaf1;
  border-radius: 10px;
  box-shadow: 0 5px 16px rgba(30, 36, 65, .035);
  gap: 16px;
  padding: 18px;
  width: 100%;
}

.frequency-total-card > span {
  align-items: center;
  background: #f0eaff;
  border-radius: 50%;
  color: #6d38e8;
  display: flex;
  height: 52px;
  justify-content: center;
  width: 52px;
}

.frequency-total-card svg {
  fill: none;
  height: 26px;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 1.8;
  width: 26px;
}

.frequency-total-card p {
  color: #51586c;
  font-size: .7rem;
  font-weight: 650;
}

.frequency-total-card strong {
  color: #151a2d;
  display: block;
  font-size: 1.35rem;
  line-height: 1;
  margin-top: 7px;
}

.frequency-total-card small {
  color: #858b9e;
  display: block;
  font-size: .62rem;
  margin-top: 6px;
}

.frequency-total-card.is-green > span {
  background: #e9f8ef;
  color: #20a95f;
}

.frequency-total-card.is-green small {
  color: #259b5b;
}

.frequency-total-card.is-orange > span {
  background: #fff1df;
  color: #ee8a18;
}

.frequency-total-card.is-orange small {
  color: #d98117;
}

.frequency-loading-card,
.frequency-empty-card {
  align-items: center;
  background: #fff;
  border: 1px solid #e9e8ef;
  border-radius: 11px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  justify-content: center;
  min-height: 280px;
  padding: 30px;
  text-align: center;
}

.frequency-loading-card p,
.frequency-empty-card p {
  color: #747b8e;
  font-size: .76rem;
  line-height: 1.55;
  max-width: 460px;
}

.frequency-empty-card h2 {
  color: #171c30;
  font-size: 1.1rem;
  font-weight: 800;
  letter-spacing: -.025em;
  margin: 0;
}

.loading-spinner {
  animation: spin .75s linear infinite;
  border: 3px solid #e8e1fa;
  border-radius: 50%;
  border-top-color: #6932df;
  height: 28px;
  width: 28px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

@media (prefers-reduced-motion: reduce) {
  .loading-spinner {
    animation-duration: 2.4s;
  }

  .primary-button:hover {
    transform: none;
  }
}

.frequency-table-card,
.frequency-history-card {
  background: #fff;
  border: 1px solid #e8e8ef;
  border-radius: 11px;
  box-shadow: 0 5px 16px rgba(30, 36, 65, .035);
  overflow: hidden;
}

.table-card-header {
  border-bottom: 1px solid #ececf1;
  justify-content: space-between;
  padding: 18px 20px;
}

.table-card-header h2 {
  color: #1d2337;
  font-size: .95rem;
  font-weight: 800;
  letter-spacing: -.02em;
  margin: 0;
}

.see-all {
  align-items: center;
  background: #fff;
  border: 1px solid #e1e2e9;
  border-radius: 7px;
  color: #535a70;
  display: flex;
  font-size: .68rem;
  gap: 7px;
  padding: 9px 13px;
}

.see-all:hover {
  border-color: #7650df;
  color: #6030cb;
}

.see-all svg {
  fill: none;
  height: 16px;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 1.7;
  width: 16px;
}

.frequency-table-scroll {
  overflow-x: auto;
}

.frequency-table-card table,
.frequency-history-card table {
  border-collapse: collapse;
  min-width: 940px;
  table-layout: fixed;
  width: 100%;
}

.column-discipline { width: 24%; }
.column-minimum { width: 13%; }
.column-loss { width: 12%; }
.column-absences { width: 8%; }
.column-attendance { width: 17%; }
.column-situation { width: 13%; }
.column-actions { width: 10%; }

.frequency-table-card th,
.frequency-table-card td,
.frequency-history-card th,
.frequency-history-card td {
  border-bottom: 1px solid #ececf1;
  padding: 15px 14px;
  text-align: left;
  vertical-align: middle;
}

.frequency-table-card th,
.frequency-history-card th {
  color: #495066;
  font-size: .66rem;
  font-weight: 750;
  white-space: nowrap;
}

.frequency-table-card td,
.frequency-history-card td {
  color: #555c70;
  font-size: .68rem;
}

.frequency-table-card tbody tr:last-child td,
.frequency-history-card tbody tr:last-child td {
  border-bottom: 0;
}

.frequency-table-card tbody tr:hover,
.frequency-history-card tbody tr:hover {
  background: #fbfaff;
}

.frequency-table-card th:last-child,
.frequency-table-card td:last-child,
.frequency-history-card th:last-child,
.frequency-history-card td:last-child {
  text-align: center;
}

.discipline-name-cell {
  align-items: center;
  display: flex;
  gap: 11px;
  min-width: 185px;
}

.discipline-name {
  color: #24293c;
  display: block;
  font-size: .72rem;
}

.discipline-name-cell small {
  color: #858b9e;
  font-size: .62rem;
}

.discipline-color {
  align-items: center;
  border-radius: 50%;
  display: flex;
  flex: 0 0 38px;
  height: 38px;
  justify-content: center;
  width: 38px;
}

.discipline-color svg {
  fill: none;
  height: 20px;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 1.8;
  width: 20px;
}

.absences-cell {
  color: #303649 !important;
  font-weight: 700;
}

.attendance-cell {
  display: grid;
  gap: 7px;
  min-width: 150px;
}

.attendance-value {
  font-size: .84rem;
  font-weight: 800;
}

.attendance-value.is-good { color: #1a9a54; }
.attendance-value.is-warning { color: #d9811a; }
.attendance-value.is-bad { color: #d93b2b; }
.attendance-value.is-empty { color: #767d90; }

.attendance-track {
  background: #e8e9ee;
  border-radius: 999px;
  height: 5px;
  max-width: 185px;
  overflow: hidden;
}

.attendance-track span {
  border-radius: inherit;
  display: block;
  height: 100%;
}

.attendance-track .is-good { background: #20aa60; }
.attendance-track .is-warning { background: #f0951f; }
.attendance-track .is-bad { background: #e04433; }

.frequency-status {
  border-radius: 999px;
  display: inline-block;
  font-size: .61rem;
  font-weight: 700;
  padding: 4px 11px;
  white-space: nowrap;
}

.frequency-status.is-success {
  background: #e8f7ed;
  color: #258b50;
}

.frequency-status.is-warning {
  background: #fff0df;
  color: #ce741a;
}

.frequency-status.is-danger {
  background: #ffeceb;
  color: #c93a2c;
}

.frequency-status.is-neutral {
  background: #eff0f4;
  color: #646b7d;
}

.impact-badge {
  background: #ffeceb;
  border-radius: 999px;
  color: #c93a2c;
  display: inline-block;
  font-size: .62rem;
  font-weight: 700;
  padding: 4px 10px;
}

.frequency-actions-cell {
  align-items: center;
  display: flex;
  gap: 7px;
  justify-content: center;
}

.frequency-actions-cell button {
  align-items: center;
  background: transparent;
  border: 0;
  border-radius: 6px;
  color: #576079;
  display: flex;
  height: 32px;
  justify-content: center;
  width: 32px;
}

.frequency-actions-cell button:hover {
  background: #f0ecff;
  color: #6532d8;
}

.frequency-actions-cell button.reset-action {
  color: #e13f32;
}

.frequency-actions-cell button.reset-action:hover {
  background: #fff0ed;
  color: #e24c39;
}

.frequency-actions-cell button:disabled {
  cursor: not-allowed;
  opacity: .4;
}

.frequency-actions-cell button:disabled:hover {
  background: transparent;
}

.frequency-actions-cell svg {
  fill: none;
  height: 20px;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 1.8;
  width: 20px;
}

.frequency-no-results {
  color: #747b8e;
  font-size: .72rem;
  line-height: 1.55;
  padding: 32px 20px;
  text-align: center;
}

@media (max-width: 980px) {
  .frequency-header {
    align-items: stretch;
    flex-direction: column;
  }

  .frequency-summary-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 620px) {
  .frequency-actions {
    display: grid;
    grid-template-columns: 1fr 1fr;
  }

  .primary-button,
  .secondary-button {
    grid-column: 1 / -1;
  }
}
</style>
