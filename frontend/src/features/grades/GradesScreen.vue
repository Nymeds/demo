<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import AppToast from '../../components/ui/AppToast.vue'
import { createApiClient, SessionExpiredError } from '../../api/apiClient'
import AppSelect from '../../components/ui/AppSelect.vue'
import GradeModal from './GradeEntryModal.vue'
import GradesSummaryCards from './GradesSummaryCards.vue'
import GradesTable from './GradesTable.vue'
import AverageEvolutionChart from './AverageEvolutionChart.vue'
import PerformanceDonut from './PerformanceDonut.vue'
import {
  SORT_OPTIONS,
  distributionOf,
  evolutionOf,
  latestPeriodKey,
  matchesSearch,
  periodKeyOf,
  periodKeysOf,
  sortEntries,
  summarize,
} from './gradesPresentation'
import './grades.css'
import { useFocusTrap } from '../../shared/a11y/useFocusTrap.js'
import { loadActiveDashboard } from '../../shared/dashboards/useActiveDashboard.js'

const ALL = 'all'
const TOAST_DURATION_MS = 4500

const emit = defineEmits(['navigate'])

const { request } = createApiClient()
const loading = ref(true)
const loadError = ref('')
const dashboardId = ref('')
const entries = ref([])
const activeStatus = ref('all')
const viewMode = ref('list')
const page = ref(1)
const pageSize = 8
const statuses = [
  { value: 'all', label: 'Todas' },
  { value: 'IN_PROGRESS', label: 'Em andamento' },
  { value: 'COMPLETED', label: 'Concluídas' },
  { value: 'LOCKED', label: 'Trancadas' },
]
// Meta pessoal de média definida em Configurações. null = sem meta ou falha ao carregar (não bloqueia a tela).
const gradeGoal = ref(null)
const editingGrade = ref(null)
const deletion = ref(null)
const deleting = ref(false)
const deleteError = ref('')
const deleteDialog = ref(null)
let deleteTrigger = null
useFocusTrap(() => !!deletion.value, deleteDialog, {
  onClose: () => { deletion.value = null },
  closeOnEscape: () => !deleting.value,
  returnFocus: false,
})
watch(deletion, async value => {
  if (value) return
  await nextTick()
  deleteTrigger?.focus()
})

const search = ref('')
const selectedPeriod = ref(ALL)
const sortOrder = ref('name')
const toast = ref({ message: '', type: 'success' })
let toastTimer = null

// Formulário de notas vinculado às avaliações cadastradas em Atividades.
const addGradeButton = ref(null)
const showAddGrade = ref(false)
const addDisciplineId = ref('')
const addActivities = ref([])
const addActivitiesStatus = ref('idle')
const addGradedActivityIds = ref([])
const savingGrade = ref(false)
const addGradeError = ref('')
// A tabela guarda as notas já abertas; mudar a chave recria a tabela para buscá-las de novo.
const tableVersion = ref(0)

const periodOptions = computed(() => periodKeysOf(entries.value))
const periodEntries = computed(() => (
  selectedPeriod.value === ALL
    ? entries.value
    : entries.value.filter(entry => periodKeyOf(entry) === selectedPeriod.value)
))
const visibleEntries = computed(() => sortEntries(
  periodEntries.value.filter(entry => (
    (activeStatus.value === ALL || entry.status === activeStatus.value)
    && matchesSearch(entry, search.value)
  )),
  sortOrder.value,
))
const summary = computed(() => summarize(periodEntries.value))
const distribution = computed(() => distributionOf(periodEntries.value))
const evolution = computed(() => evolutionOf(entries.value))
const pageCount = computed(() => Math.max(1, Math.ceil(visibleEntries.value.length / pageSize)))
const pagedEntries = computed(() => visibleEntries.value.slice((page.value - 1) * pageSize, page.value * pageSize))
watch([search, activeStatus, sortOrder, selectedPeriod], () => { page.value = 1 })
watch(pageCount, count => { page.value = Math.min(page.value, count) })
function clearFilters() {
  search.value = ''
  activeStatus.value = ALL
  selectedPeriod.value = ALL
  sortOrder.value = 'name'
  page.value = 1
}
const periodLabel = computed(() => (selectedPeriod.value === ALL ? 'Em todos os períodos' : `No período ${selectedPeriod.value}`))

// Opções das listas de filtro no formato do AppSelect (lista no visual do site).
const periodSelectOptions = computed(() => [
  { value: ALL, label: 'Todos' },
  ...periodOptions.value.map(period => ({ value: period, label: period })),
])

const addDisciplineOptions = computed(() => [...entries.value]
  .sort((first, second) => first.name.localeCompare(second.name, 'pt-BR'))
  .map(entry => ({
    id: entry.disciplineId,
    name: periodKeyOf(entry) ? `${entry.name} (${periodKeyOf(entry)})` : entry.name,
  })))
const addDisciplineName = computed(() => (
  entries.value.find(entry => entry.disciplineId === addDisciplineId.value)?.name ?? ''
))


function showToast(message, type = 'success') {
  clearTimeout(toastTimer)
  toast.value = { message, type }
  toastTimer = setTimeout(closeToast, TOAST_DURATION_MS)
}

function closeToast() {
  toast.value = { message: '', type: toast.value.type }
}

function handleFailure(error) {
  if (error instanceof SessionExpiredError) {
    return
  }

  showToast(error.message || 'Não foi possível carregar as informações.', 'error')
}

async function load() {
  loading.value = true
  loadError.value = ''

  try {
    const dashboard = await loadActiveDashboard(request)

    if (!dashboard) {
      entries.value = []
      return
    }

    dashboardId.value = dashboard.id

    const gradebook = await fetchGradebook()
    entries.value = gradebook
    selectedPeriod.value = latestPeriodKey(gradebook) ?? ALL
  } catch (error) {
    if (error instanceof SessionExpiredError) {
      return
    }

    loadError.value = error.message || 'Não foi possível carregar suas notas.'
  } finally {
    loading.value = false
  }
}

// A meta é só um complemento visual: se a busca falhar, a tela de Notas segue funcionando normalmente.
async function loadGoal() {
  try {
    const preferences = await request('/api/v1/settings/preferences')
    gradeGoal.value = preferences.gradeGoal ?? null
  } catch (error) {
    gradeGoal.value = null
  }
}

async function fetchGradebook() {
  const [gradebook, disciplines] = await Promise.all([
    request(`/api/v1/dashboards/${dashboardId.value}/gradebook`),
    request(`/api/v1/dashboards/${dashboardId.value}/disciplines`),
  ])
  return gradebook.map(entry => ({ ...entry, status: disciplines.find(item => item.id === entry.disciplineId)?.status ?? 'IN_PROGRESS' }))
}
function loadGrades(disciplineId) {
  return request(`/api/v1/dashboards/${dashboardId.value}/disciplines/${disciplineId}/grades`)
}

// Atualiza as médias sem mexer nos filtros que a pessoa escolheu.
async function refreshGradebook() {
  try {
    entries.value = await fetchGradebook()
    tableVersion.value += 1
  } catch (error) {
    handleFailure(error)
  }
}

async function selectAddDiscipline(disciplineId) {
  addDisciplineId.value = disciplineId
  addActivities.value = []
  addGradedActivityIds.value = []
  addGradeError.value = ''

  if (!disciplineId) {
    addActivitiesStatus.value = 'idle'
    return
  }

  addActivitiesStatus.value = 'loading'

  try {
    // As notas já lançadas dizem quais avaliações ficam bloqueadas no modal.
    const [activities, grades] = await Promise.all([
      request(`/api/v1/dashboards/${dashboardId.value}/disciplines/${disciplineId}/activities`),
      loadGrades(disciplineId),
    ])

    if (disciplineId !== addDisciplineId.value) return

    addActivities.value = activities
    addGradedActivityIds.value = grades.map(grade => grade.activityId).filter(Boolean)
    addActivitiesStatus.value = 'ready'
  } catch (error) {
    if (error instanceof SessionExpiredError) {
      return
    }

    if (disciplineId !== addDisciplineId.value) return

    addActivitiesStatus.value = 'error'
  }
}

// Abre na disciplina quando há apenas uma no período.
function openAddGrade() {
  editingGrade.value = null
  const preselected = periodEntries.value.length === 1 ? periodEntries.value[0].disciplineId : ''

  showAddGrade.value = true
  selectAddDiscipline(preselected)
}

async function closeAddGrade() {
  if (savingGrade.value) return
  showAddGrade.value = false
  await nextTick()
  addGradeButton.value?.focus()
}

function goToActivities() {
  showAddGrade.value = false
  emit('navigate', 'activities')
}

async function saveGrade(formData) {
  if (savingGrade.value || !addDisciplineId.value) return

  savingGrade.value = true
  addGradeError.value = ''

  try {
    await request(
      `/api/v1/dashboards/${dashboardId.value}/disciplines/${addDisciplineId.value}/grades${editingGrade.value ? `/${editingGrade.value.id}` : ''}`,
      { method: editingGrade.value ? 'PUT' : 'POST', body: formData },
    )

    showAddGrade.value = false
    await nextTick()
    addGradeButton.value?.focus()
    showToast('Nota lançada.')
    await refreshGradebook()
  } catch (error) {
    if (error instanceof SessionExpiredError) {
      return
    }

    // A mensagem do backend (por exemplo, avaliação já com nota) aparece dentro do modal.
    addGradeError.value = error.message || 'Não foi possível lançar a nota.'
  } finally {
    savingGrade.value = false
  }
}

async function editGrade(entry, grade) {
  editingGrade.value = grade
  showAddGrade.value = true
  await selectAddDiscipline(entry.disciplineId)
}
function askDelete(entry, grade) {
  deleteTrigger = document.activeElement
  deletion.value = { entry, grade }
  deleteError.value = ''
}
async function deleteGrade() {
  if (deleting.value || !deletion.value) return
  deleting.value = true
  try {
    const { entry, grade } = deletion.value
    await request(`/api/v1/dashboards/${dashboardId.value}/disciplines/${entry.disciplineId}/grades/${grade.id}`, { method: 'DELETE' })
    deletion.value = null
    showToast('Nota excluída.')
    await refreshGradebook()
  } catch (error) {
    deleteError.value = error.message
    handleFailure(error)
  } finally { deleting.value = false }
}
onMounted(() => {
  load()
  loadGoal()
})
onBeforeUnmount(() => clearTimeout(toastTimer))
</script>

<template>
  <section class="grades-page" aria-labelledby="grades-title">
    <header class="grades-header">
      <div class="grades-title">
        <span class="grades-title-icon" aria-hidden="true">
          <svg viewBox="0 0 24 24"><path d="M3 17l6-6 4 4 8-8" /><path d="M14 7h7v7" /></svg>
        </span>
        <div>
          <h1 id="grades-title">Notas</h1>
          <p>Acompanhe seu desempenho por disciplina.</p>
        </div>
      </div>

      <div class="grades-header-actions">
        <label class="grades-field is-search">
          <span class="grades-visually-hidden">Buscar disciplina ou professor</span>
          <svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="11" cy="11" r="7" /><path d="m20 20-3.5-3.5" /></svg>
          <input v-model="search" type="search" maxlength="120" placeholder="Buscar disciplina...">
        </label>
      <button
        ref="addGradeButton"
        class="grades-button is-primary"
        type="button"
        :disabled="loading || Boolean(loadError) || entries.length === 0"
        @click="openAddGrade"
      >
        <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 5v14M5 12h14" /></svg>
        Adicionar nota
      </button>
      </div>
    </header>

    <div v-if="loading" class="grades-status" role="status">
      <h2>Carregando suas notas…</h2>
      <p>Estamos reunindo as médias das suas disciplinas.</p>
    </div>

    <div v-else-if="loadError" class="grades-status" role="alert">
      <h2>Não foi possível carregar suas notas</h2>
      <p>{{ loadError }}</p>
      <button class="grades-button is-primary" type="button" @click="load">Tentar novamente</button>
    </div>

    <div v-else-if="entries.length === 0" class="grades-status">
      <h2>Nenhuma disciplina cadastrada ainda</h2>
      <p>Cadastre suas disciplinas e lance as notas para acompanhar seu desempenho aqui.</p>
      <button class="grades-button is-primary" type="button" @click="emit('navigate', 'disciplines')">Cadastrar disciplina</button>
    </div>

    <template v-else>
      <GradesSummaryCards :summary="summary" :period-label="periodLabel" :goal="gradeGoal" @navigate="section => emit('navigate', section)" />
      <div class="grades-charts">
        <section class="grades-panel grades-chart-card" aria-labelledby="grades-evolution-title">
          <h2 id="grades-evolution-title">Evolução da média geral</h2>
          <AverageEvolutionChart :points="evolution" :goal="gradeGoal" />
        </section>
        <section class="grades-panel grades-chart-card" aria-labelledby="grades-distribution-title">
          <h2 id="grades-distribution-title">Distribuição por desempenho</h2>
          <PerformanceDonut :distribution="distribution" :general-average="summary.generalAverage" />
        </section>
      </div>
      <div class="grades-toolbar">
        <div class="grades-tabs" aria-label="Situação da disciplina">
          <button v-for="status in statuses" :key="status.value" type="button" :class="{ active: activeStatus === status.value }" :aria-pressed="activeStatus === status.value" @click="activeStatus = status.value">{{ status.label }}</button>
        </div>
        <div class="grades-toolbar-actions">
          <label v-if="periodOptions.length > 1" class="grades-field grades-period"><span class="grades-visually-hidden">Período</span><AppSelect v-model="selectedPeriod" :options="periodSelectOptions" /></label>
          <label class="grades-sort"><span>Ordenar por:</span><AppSelect v-model="sortOrder" :options="SORT_OPTIONS" /></label>
          <button class="grades-button is-secondary" type="button" @click="clearFilters"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M3 4h18l-7 8v7l-4 2v-9Z" /></svg>Limpar filtros</button>
          <div class="grades-view-toggle" aria-label="Visualização">
            <button type="button" :class="{ active: viewMode === 'list' }" :aria-pressed="viewMode === 'list'" aria-label="Visualizar em lista" @click="viewMode = 'list'"><svg viewBox="0 0 24 24"><path d="M8 5h13M8 12h13M8 19h13M3 5h1M3 12h1M3 19h1" /></svg></button>
            <button type="button" :class="{ active: viewMode === 'grid' }" :aria-pressed="viewMode === 'grid'" aria-label="Visualizar em grade" @click="viewMode = 'grid'"><svg viewBox="0 0 24 24"><path d="M3 3h7v7H3zM14 3h7v7h-7zM3 14h7v7H3zM14 14h7v7h-7z" /></svg></button>
          </div>
        </div>
      </div>
      <section class="grades-panel grades-list" aria-label="Notas por disciplina">
        <GradesTable :key="tableVersion" :entries="pagedEntries" :view-mode="viewMode" :load-grades="loadGrades" :goal="gradeGoal" @edit="editGrade" @delete="askDelete" @add="entry => { openAddGrade(); selectAddDiscipline(entry.disciplineId) }" @failed="handleFailure" />
      </section>
      <footer class="grades-footer">
        <p class="grades-count" aria-live="polite">Mostrando {{ pagedEntries.length }} de {{ visibleEntries.length }} disciplinas</p>
        <nav class="grades-pagination" aria-label="Páginas de disciplinas">
          <button type="button" :disabled="page === 1" @click="page--">Anterior</button>
          <span aria-current="page">{{ page }}</span>
          <button type="button" :disabled="page === pageCount" @click="page++">Próxima</button>
        </nav>
      </footer>
      <aside class="grades-tip" aria-label="Dica">
        <span class="grades-tip-icon" aria-hidden="true"><svg viewBox="0 0 24 24"><path d="M9 18h6M10 21h4M12 3a6 6 0 0 0-3.5 10.9c.6.5 1 1.2 1 2.1h5c0-.9.4-1.6 1-2.1A6 6 0 0 0 12 3Z" /></svg></span>
        <div><strong>Dica</strong><p>Quer saber quanto precisa tirar na próxima avaliação? O simulador usa as notas que você já lançou.</p></div>
        <button class="grades-button is-secondary" type="button" @click="emit('navigate', 'simulator')"><svg viewBox="0 0 24 24"><path d="M3 17l6-6 4 4 8-8" /></svg>Simulador de Notas</button>
      </aside>
    </template>

    <GradeModal
      v-if="showAddGrade"
      :grade="editingGrade"
      :disciplines="addDisciplineOptions"
      :discipline-id="addDisciplineId"
      :discipline-name="addDisciplineName"
      :activities="addActivities"
      :activities-status="addActivitiesStatus"
      :graded-activity-ids="addGradedActivityIds"
      :saving="savingGrade"
      :error-message="addGradeError"
      @select-discipline="selectAddDiscipline"
      @retry="selectAddDiscipline(addDisciplineId)"
      @go-to-activities="goToActivities"
      @close="closeAddGrade"
      @save="saveGrade"
    />

    <div v-if="deletion" class="grades-confirm-overlay">
      <section ref="deleteDialog" class="grades-confirm" role="alertdialog" aria-modal="true" aria-labelledby="delete-grade-title">
        <h2 id="delete-grade-title">Excluir nota?</h2>
        <p>A nota de {{ deletion.grade.assessmentName }} em {{ deletion.entry.name }} será excluída e a média será recalculada.</p>
        <p v-if="deleteError" role="alert">{{ deleteError }}</p>
        <div><button class="grades-button is-secondary" type="button" :disabled="deleting" @click="deletion = null">Cancelar</button><button class="grades-button is-danger" type="button" :disabled="deleting" @click="deleteGrade">{{ deleting ? 'Excluindo...' : 'Excluir nota' }}</button></div>
      </section>
    </div>
    <AppToast :message="toast.message" :type="toast.type" @close="closeToast" />
  </section>
</template>
