<script setup>
import { apiRequest } from '../../shared/http/apiRequest.js'
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import AppSelect from '../../components/ui/AppSelect.vue'
import AppToast from '../../components/ui/AppToast.vue'
import FrequencyConfigModal from './FrequencyConfigModal.vue'
import RegisterAbsenceModal from './RegisterAbsenceModal.vue'
import DeleteAbsenceModal from './DeleteAbsenceModal.vue'
import FrequencySummaryCards from './FrequencySummaryCards.vue'
import FrequencyTable from './FrequencyTable.vue'
import AbsenceHistoryCard from './AbsenceHistoryCard.vue'
import { ATTENTION_MARGIN } from './frequencyRules.js'
import {
  averageAttendanceOf,
  averageLabelOf,
  buildRow,
  filterRows,
  normalizeDiscipline,
  normalizeFrequency,
  periodOptionsOf,
  SITUATION_FILTERS,
  sortAbsenceHistory,
  totalAbsencesOf,
  withFrequency,
} from './frequencyPresentation.js'
import './frequency.css'
import { loadActiveDashboard } from '../../shared/dashboards/useActiveDashboard.js'

const loading = ref(true)
const hasLoadedOnce = ref(false)
// Erro persistente do carregamento completo (lista de disciplinas). Quando já
// existem dados carregados, os dados antigos continuam visíveis e este erro
// vira apenas um aviso de "dados possivelmente desatualizados".
const loadError = ref('')
const requestError = ref('')
const dashboardId = ref('')
const disciplines = ref([])
const searchTerm = ref('')
const periodSelectOptions = computed(() => [
  { value: 'all', label: 'Todos' },
  ...periodOptions.value.map(period => ({ value: period, label: period }))
])
const periodFilter = ref('all')
const situationFilter = ref('all')
const showConfigModal = ref(false)
const showAbsenceModal = ref(false)
const modalTargetId = ref('')
const showAllHistory = ref(false)
const toast = ref({ message: '', type: 'success' })

// Margem do aviso de frequência configurada em Preferências. Usa o valor fixo
// (ATTENTION_MARGIN) enquanto não carregada, quando não definida pelo usuário
// ou quando o carregamento das preferências falha.
const attendanceAlertMargin = ref(ATTENTION_MARGIN)

const registering = ref(false)
const registerServerError = ref('')

const deletingEntry = ref(null)
const deletingInFlight = ref(false)
const deleteServerError = ref('')

// Aviso não bloqueante exibido quando a operação principal (criar/excluir falta)
// deu certo, mas a atualização dos números da disciplina falhou. `retry` refaz
// apenas a atualização, nunca a operação inteira.
const refreshWarning = ref(null)

const absenceHistory = ref([])
let toastTimer
let loadRequestId = 0

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

// `ownerDashboardId` permite montar o caminho durante o carregamento, antes de
// dashboardId.value ser preenchido.
function frequencyPath(disciplineId, ownerDashboardId = dashboardId.value) {
  return `/api/v1/dashboards/${ownerDashboardId}/disciplines/${disciplineId}/frequency`
}

function absencePath(disciplineId, recordId = '', ownerDashboardId = dashboardId.value) {
  const base = `${frequencyPath(disciplineId, ownerDashboardId)}/absences`
  return recordId ? `${base}/${recordId}` : base
}

function disciplinePath(disciplineId) {
  return `/api/v1/dashboards/${dashboardId.value}/disciplines/${disciplineId}`
}

async function loadFrequency(disciplineId) {
  try {
    return normalizeFrequency(await apiRequest(frequencyPath(disciplineId)))
  } catch (error) {
    if (error.status === 404) return null
    throw error
  }
}

// Falha ao carregar a margem do aviso de frequência não deve derrubar a tela:
// mantém-se o valor fixo (ATTENTION_MARGIN) como alternativa.
async function loadAttendanceAlertMargin() {
  try {
    const preferences = await apiRequest('/api/v1/settings/preferences')
    const margin = Number(preferences?.attendanceAlertMargin)
    attendanceAlertMargin.value = Number.isFinite(margin) ? margin : ATTENTION_MARGIN
  } catch {
    attendanceAlertMargin.value = ATTENTION_MARGIN
  }
}

async function loadAbsenceHistory(disciplineId, ownerDashboardId) {
  return apiRequest(absencePath(disciplineId, '', ownerDashboardId))
}

async function loadData() {
  // Contador de requisições: se uma chamada mais recente já respondeu quando
  // esta terminar, o resultado desta é descartado (evita sobrescrever dados
  // novos com uma resposta antiga fora de ordem).
  const requestId = ++loadRequestId
  loading.value = !hasLoadedOnce.value
  requestError.value = ''

  try {
    let dashboard = await loadActiveDashboard(apiRequest)

    if (!dashboard) {
      dashboard = await apiRequest('/api/v1/dashboards', {
        method: 'POST',
        body: JSON.stringify({ name: 'Organização acadêmica', status: 'ACTIVE' }),
      })
    }

    // A lista de disciplinas já traz faltas, frequência atual e máximo de
    // faltas; só o histórico de lançamentos exige uma busca por disciplina.
    const savedDisciplines = await apiRequest(`/api/v1/dashboards/${dashboard.id}/disciplines`)

    const histories = await Promise.all(
      savedDisciplines.map(discipline => loadAbsenceHistory(discipline.id, dashboard.id)),
    )

    if (requestId !== loadRequestId) return

    dashboardId.value = dashboard.id
    disciplines.value = savedDisciplines.map(withFrequency)
    absenceHistory.value = sortAbsenceHistory(histories.flat())

    hasLoadedOnce.value = true
    loadError.value = ''
  } catch (error) {
    if (requestId !== loadRequestId) return

    const message = error.message || 'Não foi possível carregar a frequência.'
    loadError.value = message
    requestError.value = message

    // Só interrompe a tela com um toast quando não havia nenhum dado prévio
    // para mostrar; se já há disciplinas carregadas, elas permanecem visíveis
    // e o aviso de dados desatualizados aparece no lugar do toast.
    if (!hasLoadedOnce.value) showToast(message, 'error')
  } finally {
    if (requestId === loadRequestId) loading.value = false
  }
}

onMounted(() => {
  loadData()
  loadAttendanceAlertMargin()
})
onBeforeUnmount(() => clearTimeout(toastTimer))

const rows = computed(() => disciplines.value.map(discipline => buildRow(discipline, attendanceAlertMargin.value)))

const periodOptions = computed(() => periodOptionsOf(rows.value))

const filteredRows = computed(() => filterRows(rows.value, {
  searchTerm: searchTerm.value,
  periodFilter: periodFilter.value,
  situationFilter: situationFilter.value,
}))

const averageAttendance = computed(() => averageAttendanceOf(rows.value))

const totalAbsences = computed(() => totalAbsencesOf(rows.value))

const averageLabel = computed(() => averageLabelOf(averageAttendance.value))

function clearFilters() {
  searchTerm.value = ''
  periodFilter.value = 'all'
  situationFilter.value = 'all'
}

function openConfigModal(disciplineId = '') {
  modalTargetId.value = disciplineId
  showConfigModal.value = true
}

function openAbsenceModal(disciplineId = '') {
  modalTargetId.value = disciplineId
  registerServerError.value = ''
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

function clearRefreshWarning() {
  refreshWarning.value = null
}

function setRefreshWarning(message, retry) {
  refreshWarning.value = { message, retry }
}

async function retryRefreshWarning() {
  const pending = refreshWarning.value
  if (!pending) return
  clearRefreshWarning()
  await pending.retry()
}

// Atualiza os números de uma disciplina após criar/excluir uma falta. Se essa
// atualização falhar, a operação principal (que já concluiu) continua sendo
// reportada como sucesso; só aparece o aviso não bloqueante, com retentativa
// que refaz apenas esta atualização.
async function refreshFrequencyOrWarn(disciplineId, warningMessage) {
  try {
    const frequency = await loadFrequency(disciplineId)
    replaceFrequency(disciplineId, frequency)
  } catch {
    setRefreshWarning(warningMessage, () => refreshFrequencyOrWarn(disciplineId, warningMessage))
  }
}

async function registerAbsence({ disciplineId, date, quantity, reason, note }) {
  if (registering.value) return

  registering.value = true
  registerServerError.value = ''

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

    absenceHistory.value.unshift(entry)
    closeModals()
    showToast(quantity > 1 ? 'Faltas registradas.' : 'Falta registrada.')

    await refreshFrequencyOrWarn(
      disciplineId,
      'Falta registrada, mas não foi possível atualizar os números. Tentar novamente',
    )
  } catch (error) {
    // A falta não foi criada: o erro (incluindo a mensagem HTTP 400 do
    // servidor para data futura) fica visível dentro do próprio formulário.
    registerServerError.value = error.message || 'Não foi possível registrar a falta.'
  } finally {
    registering.value = false
  }
}

function requestDeleteAbsence(entry) {
  deletingEntry.value = entry
  deleteServerError.value = ''
}

function cancelDeleteAbsence() {
  if (deletingInFlight.value) return
  deletingEntry.value = null
  deleteServerError.value = ''
}

async function confirmDeleteAbsence() {
  const entry = deletingEntry.value
  if (!entry || deletingInFlight.value) return

  deletingInFlight.value = true
  deleteServerError.value = ''

  try {
    const discipline = disciplines.value.find(item => item.id === entry.disciplineId)

    if (!discipline?.frequency) {
      deleteServerError.value = 'A frequência desta disciplina não está mais disponível.'
      return
    }

    await apiRequest(absencePath(entry.disciplineId, entry.id), { method: 'DELETE' })

    absenceHistory.value = absenceHistory.value.filter(item => item.id !== entry.id)
    deletingEntry.value = null
    showToast('Lançamento desfeito.')

    await refreshFrequencyOrWarn(
      entry.disciplineId,
      'Falta removida, mas não foi possível atualizar os números. Tentar novamente',
    )
  } catch (error) {
    deleteServerError.value = error.message || 'Não foi possível desfazer o lançamento.'
  } finally {
    deletingInFlight.value = false
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

    <FrequencySummaryCards
      :discipline-count="rows.length"
      :average-attendance="averageAttendance"
      :average-label="averageLabel"
      :total-absences="totalAbsences"
    />

    <div class="frequency-toolbar">
      <div class="frequency-situation-filters" aria-label="Filtrar por situação">
        <button
          v-for="option in SITUATION_FILTERS"
          :key="option.value"
          type="button"
          :class="{ active: situationFilter === option.value }"
          @click="situationFilter = option.value"
        >
          {{ option.label }}
        </button>
      </div>

      <div class="frequency-filter-options">
        <div class="frequency-period-filter">
          <span id="frequency-period-label">Período:</span>
          <AppSelect
            id="frequency-period-filter"
            v-model="periodFilter"
            :options="periodSelectOptions"
            aria-label="Filtrar frequência por período"
          />
        </div>

        <button class="clear-filters" type="button" @click="clearFilters">Limpar filtros</button>
      </div>
    </div>

    <article v-if="loading" class="frequency-loading-card" aria-live="polite">
      <span class="loading-spinner" aria-hidden="true"></span>
      <p>Carregando frequência...</p>
    </article>

    <article v-else-if="loadError && disciplines.length === 0" class="frequency-empty-card" role="alert">
      <h2>Não foi possível carregar a frequência</h2>
      <p>{{ loadError }}</p>
      <button class="retry-button" type="button" @click="loadData">Tentar novamente</button>
    </article>

    <article v-else-if="disciplines.length === 0" class="frequency-empty-card">
      <h2>Nenhuma disciplina cadastrada</h2>
      <p>Cadastre uma disciplina para começar a acompanhar as faltas e a frequência do período.</p>
    </article>

    <template v-else>
      <p v-if="loadError" class="frequency-stale-banner" role="alert">
        Não foi possível atualizar a frequência. Os dados exibidos podem estar desatualizados.
        <button type="button" @click="loadData">Tentar novamente</button>
      </p>

      <p v-if="refreshWarning" class="frequency-refresh-warning" role="alert">
        {{ refreshWarning.message }}
        <button type="button" @click="retryRefreshWarning">Tentar novamente</button>
      </p>

      <FrequencyTable :rows="filteredRows" @configure="openConfigModal" />

      <AbsenceHistoryCard
        v-if="!loading && disciplines.length > 0"
        :entries="absenceHistory"
        :show-all="showAllHistory"
        @toggle-show-all="showAllHistory = !showAllHistory"
        @delete="requestDeleteAbsence"
      />
    </template>

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
      :submitting="registering"
      :server-error="registerServerError"
      @close="closeModals"
      @save="registerAbsence"
    />

    <DeleteAbsenceModal
      v-if="deletingEntry"
      :entry="deletingEntry"
      :deleting="deletingInFlight"
      :server-error="deleteServerError"
      @close="cancelDeleteAbsence"
      @confirm="confirmDeleteAbsence"
    />

  </section>
</template>
