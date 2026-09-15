<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import AppToast from '../../components/ui/AppToast.vue'
import FrequencyConfigModal from './FrequencyConfigModal.vue'
import RegisterAbsenceModal from './RegisterAbsenceModal.vue'
import { frequencySituation, LOSS_PER_ABSENCE, maximumAbsencesFor } from './frequencyRules.js'

const props = defineProps({
  accessToken: { type: String, required: true },
})

const HISTORY_PREVIEW_SIZE = 3

const loading = ref(true)
const requestError = ref('')
const dashboardId = ref('')
const disciplines = ref([])
const searchTerm = ref('')
const periodFilter = ref('all')
const situationFilter = ref('all')
const showConfigModal = ref(false)
const showAbsenceModal = ref(false)
const modalTargetId = ref('')
const showAllHistory = ref(false)
const toast = ref({ message: '', type: 'success' })

const absenceHistory = ref([])
let toastTimer

const situationFilters = [
  { value: 'all', label: 'Todas' },
  { value: 'good', label: 'Ótimo' },
  { value: 'warning', label: 'Atenção' },
  { value: 'bad', label: 'Ruim' },
]

const situationDetails = {
  good: { label: 'Ótimo', className: 'is-success' },
  warning: { label: 'Atenção', className: 'is-warning' },
  bad: { label: 'Ruim', className: 'is-danger' },
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

function absencePath(disciplineId, recordId = '') {
  const base = `${frequencyPath(disciplineId)}/absences`
  return recordId ? `${base}/${recordId}` : base
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

    const loadedDisciplines = await Promise.all(savedDisciplines.map(async discipline => {
      const [frequency, history] = await Promise.all([
        loadFrequency(discipline.id),
        loadAbsenceHistory(discipline.id),
      ])

      return {
        discipline: { ...normalizeDiscipline(discipline), frequency },
        history,
      }
    }))

    disciplines.value = loadedDisciplines.map(item => item.discipline)
    absenceHistory.value = loadedDisciplines
      .flatMap(item => item.history)
      .sort((first, second) => {
        const dateOrder = second.date.localeCompare(first.date)
        return dateOrder || second.createdAt.localeCompare(first.createdAt)
      })
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
      absences: 0,
      attendancePercentage: 100,
      lossPerAbsence: LOSS_PER_ABSENCE,
      remainingAbsences: maximumAbsencesFor(minimumPercentage),
      situation: frequencySituation(100, minimumPercentage, maximumAbsencesFor(minimumPercentage)),
    }
  }

  const remainingAbsences = frequency.maximumAbsences - frequency.absences

  return {
    ...base,
    configured: true,
    absences: frequency.absences,
    attendancePercentage: frequency.attendancePercentage,
    lossPerAbsence: LOSS_PER_ABSENCE,
    remainingAbsences,
    situation: frequencySituation(frequency.attendancePercentage, minimumPercentage, remainingAbsences),
  }
}

const rows = computed(() => disciplines.value.map(buildRow))

const periodOptions = computed(() => {
  const periods = new Set(rows.value.map(row => row.period).filter(period => period !== '—'))
  return [...periods].sort().reverse()
})

const filteredRows = computed(() => {
  const search = searchTerm.value.trim().toLocaleLowerCase('pt-BR')

  return rows.value.filter(row => {
    const matchesSearch = !search
      || row.name.toLocaleLowerCase('pt-BR').includes(search)
      || row.subtitle.toLocaleLowerCase('pt-BR').includes(search)
    const matchesPeriod = periodFilter.value === 'all' || row.period === periodFilter.value
    const matchesSituation = situationFilter.value === 'all' || row.situation === situationFilter.value

    return matchesSearch && matchesPeriod && matchesSituation
  })
})

const averageAttendance = computed(() => {
  const values = rows.value
    .map(row => row.attendancePercentage)

  return values.length
    ? values.reduce((total, value) => total + value, 0) / values.length
    : null
})

const totalAbsences = computed(() => (
  rows.value.reduce((total, row) => total + row.absences, 0)
))

function clearFilters() {
  searchTerm.value = ''
  periodFilter.value = 'all'
  situationFilter.value = 'all'
}

async function loadAbsenceHistory(disciplineId) {
  return apiRequest(absencePath(disciplineId))
}

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

async function saveConfig({ disciplineId, minimumAttendancePercentage }) {
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
    const entry = await apiRequest(absencePath(disciplineId), {
      method: 'POST',
      body: JSON.stringify({
        date,
        quantity,
        reason,
        note,
      }),
    })
    const frequency = await loadFrequency(disciplineId)

    replaceFrequency(disciplineId, frequency)
    absenceHistory.value.unshift(entry)

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

    await apiRequest(absencePath(entry.disciplineId, entry.id), { method: 'DELETE' })
    const frequency = await loadFrequency(entry.disciplineId)

    replaceFrequency(entry.disciplineId, frequency)
    absenceHistory.value = absenceHistory.value.filter(item => item.id !== entry.id)
    showToast('Lançamento desfeito.')
  } catch (error) {
    requestError.value = error.message || 'Não foi possível desfazer o lançamento.'
    showToast(requestError.value, 'error')
  }
}

</script>

<template>
  <section class="frequency-page" aria-labelledby="frequency-title">
    <header class="frequency-header">
      <div class="frequency-heading">
        <span class="frequency-heading-icon" aria-hidden="true">
          <svg viewBox="0 0 24 24">
            <rect x="3" y="5" width="18" height="16" rx="2" />
            <path d="M16 3v4M8 3v4M3 11h18m-13 5 2 2 4-4" />
          </svg>
        </span>
        <div>
          <h1 id="frequency-title">Frequência</h1>
          <p>Acompanhe suas faltas e a frequência em cada disciplina.</p>
        </div>
      </div>

      <div class="frequency-actions">
        <label class="frequency-search">
          <span class="sr-only">Buscar disciplina</span>
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <circle cx="11" cy="11" r="7" />
            <path d="m20 20-4-4" />
          </svg>
          <input v-model="searchTerm" type="search" placeholder="Buscar disciplina...">
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

      </div>
    </header>

    <div class="frequency-summary-grid">
      <article class="frequency-total-card is-purple">
        <span aria-hidden="true">
          <svg viewBox="0 0 24 24"><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2Z" /><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20M8 7h8M8 10h6" /></svg>
        </span>
        <div>
          <p>Disciplinas cadastradas</p>
          <strong>{{ rows.length }}</strong>
          <small>Todas as disciplinas</small>
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
          <small>Em todos os períodos</small>
        </div>
      </article>
    </div>

    <div class="frequency-toolbar">
      <div class="frequency-situation-filters" aria-label="Filtrar por situação">
        <button
          v-for="option in situationFilters"
          :key="option.value"
          type="button"
          :class="{ active: situationFilter === option.value }"
          @click="situationFilter = option.value"
        >
          {{ option.label }}
        </button>
      </div>

      <div class="frequency-filter-options">
        <label class="frequency-period-filter">
          <span>Período:</span>
          <select v-model="periodFilter" aria-label="Filtrar frequência por período">
            <option value="all">Todos</option>
            <option v-for="period in periodOptions" :key="period" :value="period">{{ period }}</option>
          </select>
        </label>

        <button class="clear-filters" type="button" @click="clearFilters">Limpar filtros</button>
      </div>
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
              <td>{{ formatPercentage(row.minimumPercentage) }}</td>
              <td>{{ row.lossPerAbsence }}%</td>
              <td class="absences-cell">{{ row.absences }}</td>
              <td>
                <div class="attendance-cell">
                  <span :class="['attendance-value', barClass(row)]">
                    {{ formatPercentage(row.attendancePercentage) }}
                  </span>
                  <span class="attendance-track" aria-hidden="true">
                    <span
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
        Nenhuma falta registrada. Os lançamentos aparecem aqui assim que você registrar uma falta.
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
              <td><span class="impact-badge">-{{ entry.impact.toLocaleString('pt-BR') }}%</span></td>
              <td>
                <div class="frequency-actions-cell">
                  <button
                    class="undo-action"
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

  </section>
</template>

<style scoped>
.frequency-page {
  display: grid;
  gap: 22px;
}

.frequency-header,
.frequency-actions,
.frequency-toolbar,
.frequency-filter-options,
.frequency-total-card,
.table-card-header {
  align-items: center;
  display: flex;
}

.frequency-header {
  gap: 20px;
  justify-content: space-between;
}

.frequency-heading {
  align-items: center;
  display: flex;
  flex: 1 1 auto;
  gap: 13px;
  min-width: 0;
}

.frequency-heading-icon {
  align-items: center;
  background: #f0eaff;
  border-radius: 9px;
  color: #6b37e8;
  display: flex;
  flex: 0 0 43px;
  height: 43px;
  justify-content: center;
  width: 43px;
}

.frequency-heading-icon svg {
  fill: none;
  height: 25px;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 1.8;
  width: 25px;
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
  gap: 11px;
  justify-content: flex-end;
  min-width: 0;
}

.frequency-search {
  color: #747b90;
  flex: 0 1 245px;
  max-width: 245px;
  min-width: 0;
  position: relative;
  width: 245px;
}

.frequency-search svg {
  fill: none;
  height: 18px;
  left: 12px;
  position: absolute;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 1.8;
  top: 50%;
  transform: translateY(-50%);
  width: 18px;
}

.frequency-search input {
  background: #fff;
  border: 1px solid #dedfe8;
  border-radius: 8px;
  color: #252a3e;
  font-size: .72rem;
  min-width: 0;
  outline: none;
  padding: 11px 12px 11px 38px;
  width: 100%;
}

.frequency-search input:focus,
.frequency-period-filter:focus-within {
  border-color: #7544eb;
  box-shadow: 0 0 0 3px rgba(117, 68, 235, .12);
}

.primary-button {
  align-items: center;
  border-radius: 7px;
  display: flex;
  font-size: .72rem;
  font-weight: 700;
  gap: 7px;
  justify-content: center;
  padding: 11px 15px;
  white-space: nowrap;
}

.primary-button {
  background: linear-gradient(100deg, #5d20df, #7419f5);
  border: 0;
  box-shadow: 0 8px 18px rgba(101, 31, 225, .18);
  color: #fff;
}

.primary-button span {
  font-size: 1.05rem;
  font-weight: 400;
  line-height: .8;
}

.primary-button:hover {
  box-shadow: 0 11px 23px rgba(101, 31, 225, .28);
  transform: translateY(-1px);
}

.primary-button:disabled {
  cursor: wait;
  opacity: .6;
}

.primary-button:focus-visible,
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

.frequency-toolbar {
  flex-wrap: wrap;
  gap: 20px;
  justify-content: space-between;
  min-width: 0;
}

.frequency-situation-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  min-width: 0;
}

.frequency-situation-filters button,
.frequency-period-filter,
.clear-filters {
  background: #fff;
  border: 1px solid #e1e2e9;
  color: #34394c;
}

.frequency-situation-filters button {
  border-radius: 7px;
  font-size: .7rem;
  padding: 10px 17px;
}

.frequency-situation-filters button.active {
  border-color: #6f36e7;
  color: #6126d8;
  font-weight: 700;
}

.frequency-filter-options {
  flex-wrap: wrap;
  gap: 12px;
  justify-content: flex-end;
  min-width: 0;
}

.frequency-period-filter {
  align-items: center;
  border-radius: 7px;
  display: flex;
  font-size: .68rem;
  gap: 8px;
  min-width: 0;
  padding: 0 9px 0 14px;
}

.frequency-period-filter span {
  font-weight: 700;
}

.frequency-period-filter select {
  background: transparent;
  border: 0;
  color: #43495e;
  outline: none;
  padding: 10px 4px;
}

.clear-filters {
  border-radius: 7px;
  color: #535a70;
  font-size: .68rem;
  padding: 11px 13px;
}

.clear-filters:hover {
  border-color: #7650df;
  color: #6030cb;
}

.sr-only {
  clip: rect(0, 0, 0, 0);
  height: 1px;
  margin: -1px;
  overflow: hidden;
  padding: 0;
  position: absolute;
  width: 1px;
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
  font-size: .72rem;
  font-weight: 750;
  white-space: nowrap;
}

.frequency-table-card td,
.frequency-history-card td {
  color: #555c70;
  font-size: .78rem;
  line-height: 1.4;
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

.frequency-table-card th:not(:first-child),
.frequency-table-card td:not(:first-child),
.frequency-history-card th:nth-child(5),
.frequency-history-card td:nth-child(5) {
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
  font-size: .8rem;
}

.discipline-name-cell small {
  color: #858b9e;
  font-size: .68rem;
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
  font-weight: 400;
}

.attendance-cell {
  display: grid;
  gap: 7px;
  margin-inline: auto;
  max-width: 185px;
  min-width: 0;
  width: 100%;
}

.attendance-value {
  font-size: .9rem;
  font-weight: 800;
}

.attendance-value.is-good { color: #1a9a54; }
.attendance-value.is-warning { color: #d9811a; }
.attendance-value.is-bad { color: #d93b2b; }
.attendance-value.is-empty { color: #767d90; }

.attendance-track {
  background: #e8e9ee;
  border-radius: 999px;
  display: block;
  height: 5px;
  max-width: 185px;
  overflow: hidden;
  width: 100%;
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
  font-size: .68rem;
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
  font-size: .68rem;
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

.frequency-actions-cell button.undo-action {
  color: #e13f32;
}

.frequency-actions-cell button.undo-action:hover {
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
  .frequency-header,
  .frequency-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .frequency-actions,
  .frequency-filter-options {
    justify-content: space-between;
    width: 100%;
  }

  .frequency-summary-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 620px) {
  .frequency-actions,
  .frequency-filter-options {
    align-items: stretch;
    flex-direction: column;
  }

  .frequency-search,
  .frequency-period-filter,
  .primary-button,
  .clear-filters {
    max-width: none;
    width: 100%;
  }

  .frequency-period-filter {
    justify-content: space-between;
  }
}
</style>
