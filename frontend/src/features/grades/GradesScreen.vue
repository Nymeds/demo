<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import AppToast from '../../components/ui/AppToast.vue'
import { createApiClient, SessionExpiredError } from '../../api/apiClient'
import AppSelect from '../../components/ui/AppSelect.vue'
import GradeModal from '../simulator/GradeModal.vue'
import AverageEvolutionChart from './AverageEvolutionChart.vue'
import GradesSummaryCards from './GradesSummaryCards.vue'
import GradesTable from './GradesTable.vue'
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

const ALL = 'all'
const TOAST_DURATION_MS = 4500

const { accessToken } = defineProps({
  accessToken: { type: String, required: true },
})

const emit = defineEmits(['navigate', 'session-expired'])

const { request } = createApiClient(() => accessToken)
const loading = ref(true)
const loadError = ref('')
const dashboardId = ref('')
const entries = ref([])
const gradeGoal = ref(null)

const search = ref('')
const selectedPeriod = ref(ALL)
const selectedDiscipline = ref(ALL)
const sortOrder = ref(SORT_OPTIONS[0].value)
const toast = ref({ message: '', type: 'success' })
let toastTimer = null

// Lançamento de nota pela tela Notas: mesmo modal do simulador, com a nota ligada à prova (RF06).
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
const disciplineOptions = computed(() => [...periodEntries.value].sort((first, second) => first.name.localeCompare(second.name, 'pt-BR')))
const visibleEntries = computed(() => sortEntries(
  periodEntries.value.filter(entry => (
    (selectedDiscipline.value === ALL || entry.disciplineId === selectedDiscipline.value)
    && matchesSearch(entry, search.value)
  )),
  sortOrder.value,
))
const summary = computed(() => summarize(periodEntries.value))
const distribution = computed(() => distributionOf(periodEntries.value))
const evolution = computed(() => evolutionOf(entries.value))
const periodLabel = computed(() => (selectedPeriod.value === ALL ? 'Em todos os períodos' : `No período ${selectedPeriod.value}`))

// Opções das listas de filtro no formato do AppSelect (lista no visual do site).
const periodSelectOptions = computed(() => [
  { value: ALL, label: 'Todos' },
  ...periodOptions.value.map(period => ({ value: period, label: period })),
])
const disciplineSelectOptions = computed(() => [
  { value: ALL, label: 'Todas' },
  ...disciplineOptions.value.map(entry => ({ value: entry.disciplineId, label: entry.name })),
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

// A disciplina escolhida pode não existir no novo período.
watch(selectedPeriod, () => {
  selectedDiscipline.value = ALL
})

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
    emit('session-expired')
    return
  }

  showToast(error.message || 'Não foi possível carregar as informações.', 'error')
}

async function loadPreferences() {
  try {
    return await request('/api/v1/settings/preferences')
  } catch (error) {
    if (error instanceof SessionExpiredError) throw error
    // A meta é opcional: sem as preferências, o cartão mostra o atalho para defini-la.
    return null
  }
}

async function load() {
  loading.value = true
  loadError.value = ''

  try {
    const dashboards = await request('/api/v1/dashboards')
    const dashboard = dashboards.find(item => item.status === 'ACTIVE') || dashboards[0]

    if (!dashboard) {
      entries.value = []
      return
    }

    dashboardId.value = dashboard.id

    const [gradebook, preferences] = await Promise.all([
      request(`/api/v1/dashboards/${dashboard.id}/gradebook`),
      loadPreferences(),
    ])

    entries.value = gradebook
    gradeGoal.value = preferences?.gradeGoal ?? null
    selectedPeriod.value = latestPeriodKey(gradebook) ?? ALL
  } catch (error) {
    if (error instanceof SessionExpiredError) {
      emit('session-expired')
      return
    }

    loadError.value = error.message || 'Não foi possível carregar suas notas.'
  } finally {
    loading.value = false
  }
}

function loadGrades(disciplineId) {
  return request(`/api/v1/dashboards/${dashboardId.value}/disciplines/${disciplineId}/grades`)
}

// Atualiza as médias sem mexer nos filtros que a pessoa escolheu.
async function refreshGradebook() {
  try {
    entries.value = await request(`/api/v1/dashboards/${dashboardId.value}/gradebook`)
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
      emit('session-expired')
      return
    }

    if (disciplineId !== addDisciplineId.value) return

    addActivitiesStatus.value = 'error'
  }
}

// Já abre na disciplina filtrada, ou na única do período, para poupar um clique.
function openAddGrade() {
  const preselected = selectedDiscipline.value !== ALL
    ? selectedDiscipline.value
    : (periodEntries.value.length === 1 ? periodEntries.value[0].disciplineId : '')

  showAddGrade.value = true
  selectAddDiscipline(preselected)
}

async function closeAddGrade() {
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
      `/api/v1/dashboards/${dashboardId.value}/disciplines/${addDisciplineId.value}/grades`,
      { method: 'POST', body: formData },
    )

    await closeAddGrade()
    showToast('Nota lançada.')
    await refreshGradebook()
  } catch (error) {
    if (error instanceof SessionExpiredError) {
      emit('session-expired')
      return
    }

    // A mensagem do backend (por exemplo, avaliação já com nota) aparece dentro do modal.
    addGradeError.value = error.message || 'Não foi possível lançar a nota.'
  } finally {
    savingGrade.value = false
  }
}

onMounted(load)
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

      <button
        ref="addGradeButton"
        class="grades-button is-primary"
        type="button"
        :disabled="loading || Boolean(loadError) || entries.length === 0"
        @click="openAddGrade"
      >
        <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 5v14M5 12h14" /></svg>
        Adicionar notas
      </button>
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
      <GradesSummaryCards
        :summary="summary"
        :goal="gradeGoal"
        :period-label="periodLabel"
        @open-settings="emit('navigate', 'settings')"
      />

      <div class="grades-layout">
        <section class="grades-panel grades-list" aria-labelledby="grades-list-title">
          <h2 id="grades-list-title" class="grades-visually-hidden">Médias por disciplina</h2>

          <div class="grades-filters">
            <label class="grades-field is-search">
              <span class="grades-visually-hidden">Buscar disciplina ou professor</span>
              <svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="11" cy="11" r="7" /><path d="m20 20-3.5-3.5" /></svg>
              <input v-model="search" type="search" maxlength="120" placeholder="Buscar disciplina ou professor…">
            </label>

            <label class="grades-field">
              <span>Período</span>
              <AppSelect v-model="selectedPeriod" :options="periodSelectOptions" />
            </label>

            <label class="grades-field">
              <span>Disciplinas</span>
              <AppSelect v-model="selectedDiscipline" :options="disciplineSelectOptions" />
            </label>

            <label class="grades-field">
              <span>Ordenar por</span>
              <AppSelect v-model="sortOrder" :options="SORT_OPTIONS" />
            </label>
          </div>

          <GradesTable
            :key="tableVersion"
            :entries="visibleEntries"
            :load-grades="loadGrades"
            @open-simulator="emit('navigate', 'simulator')"
            @failed="handleFailure"
          />

          <p class="grades-count" aria-live="polite">
            Mostrando {{ visibleEntries.length }} de {{ periodEntries.length }} disciplinas
          </p>
        </section>

        <aside class="grades-side">
          <section class="grades-panel" aria-labelledby="grades-distribution-title">
            <h2 id="grades-distribution-title">Resumo de desempenho</h2>
            <PerformanceDonut :distribution="distribution" :general-average="summary.generalAverage" />
          </section>

          <section class="grades-panel" aria-labelledby="grades-evolution-title">
            <h2 id="grades-evolution-title">Evolução da média geral</h2>
            <AverageEvolutionChart :points="evolution" :goal="gradeGoal" />
          </section>

          <section class="grades-panel grades-info" aria-labelledby="grades-info-title">
            <h2 id="grades-info-title">Como as médias são calculadas</h2>
            <p>
              A média de cada disciplina é a média simples das notas lançadas nela. A média geral é a média
              das disciplinas que já têm notas no período escolhido.
            </p>
          </section>
        </aside>
      </div>

      <aside class="grades-tip" aria-label="Dica">
        <span class="grades-tip-icon" aria-hidden="true">
          <svg viewBox="0 0 24 24"><path d="M9 18h6M10 21h4M12 3a6 6 0 0 0-3.5 10.9c.6.5 1 1.2 1 2.1h5c0-.9.4-1.6 1-2.1A6 6 0 0 0 12 3Z" /></svg>
        </span>
        <div>
          <strong>Dica</strong>
          <p>Quer saber quanto precisa tirar na próxima avaliação? O simulador usa as notas que você já lançou.</p>
        </div>
        <button class="grades-button is-secondary" type="button" @click="emit('navigate', 'simulator')">
          <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M3 17l6-6 4 4 8-8" /></svg>
          Simulador de Notas
        </button>
      </aside>
    </template>

    <GradeModal
      v-if="showAddGrade"
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

    <AppToast :message="toast.message" :type="toast.type" @close="closeToast" />
  </section>
</template>
