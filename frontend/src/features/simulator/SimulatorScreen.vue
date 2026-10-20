
<template>
  <div class="simulator-page">

    <!-- CABEÇALHO -->
    <header class="page-header">

      <div class="title-area">

        <div class="title-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="5" y="3" width="14" height="18" rx="2"/>
            <line x1="8" y1="7" x2="16" y2="7"/>
            <line x1="8" y1="11" x2="10" y2="11"/>
            <line x1="14" y1="11" x2="16" y2="11"/>
            <line x1="8" y1="15" x2="10" y2="15"/>
            <line x1="14" y1="15" x2="16" y2="15"/>
          </svg>
        </div>

        <div>
          <h1>Simulador de Notas</h1>
          <p>
            Simule suas notas e descubra quanto precisa para alcançar sua meta.
          </p>
        </div>

      </div>

      <!-- AÇÕES DO CABEÇALHO -->
      <div class="header-actions">

        <button ref="helpButton" class="help-button" type="button" @click="showHelp = true">

          <span class="help-icon">?</span>

          Como funciona?

        </button>

      </div>

    </header>


    <!-- ERRO AO CARREGAR DISCIPLINAS -->
    <section
      v-if="simulatorLoadError"
      class="load-error page-load-error"
    >
      <p role="alert">
        Não foi possível carregar suas disciplinas.
      </p>
      <button
        type="button"
        class="retry-button"
        @click="loadSimulator"
      >
        Tentar novamente
      </button>
    </section>


    <!-- FILTROS -->
    <SimulatorFilters
      v-else
      ref="filters"
      v-model:discipline="selectedDiscipline"
      v-model:period="selectedPeriod"
      :discipline-options="disciplineFilterOptions"
      :period-options="periodFilterOptions"
    />


    <!-- PRIMEIRA LINHA -->
    <div v-if="!simulatorLoadError" class="main-grid">

      <SimulatorSituationCard
        :grades-status="gradesStatus"
        :notes-count="notes.length"
        :current-average="currentAverage"
        :passing-average="passingAverage"
        :desired-average="desiredAverage"
        @retry-grades="loadGrades"
        @edit-target="simulationCard?.focusDesiredAverageInput()"
      />

      <SimulatorSimulationCard
        ref="simulationCard"
        v-model:desired-average="desiredAverage"
        :grades-status="gradesStatus"
        :selected-discipline="selectedDiscipline"
        :desired-average-error="desiredAverageError"
        :max-grade="maxGrade"
        :simulation-status="simulationStatus"
        :simulation-error="simulationError"
        :simulation-result="simulationResult"
        :show-result="showResult"
        :has-saved-simulation="hasSavedSimulation"
        @retry-grades="loadGrades"
        @simulate="simulate"
        @clear-simulation="clearSimulation"
      />

    </div>


    <!-- SEGUNDA LINHA -->
    <div v-if="!simulatorLoadError" class="bottom-grid">

      <SimulatorGradesCard
        ref="gradesCard"
        :grades-status="gradesStatus"
        :notes="notes"
        :activities="activities"
        :current-average="currentAverage"
        :max-grade="maxGrade"
        :add-grade-hint="addGradeHint"
        @retry-grades="loadGrades"
        @navigate="emit('navigate', $event)"
        @add-grade="openGradeModal"
      />

      <SimulatorScenariosCard
        :grades-status="gradesStatus"
        :scenarios="scenarios"
        :note-values="noteValues"
        :desired-average="desiredAverage"
        @retry-grades="loadGrades"
      />

    </div>

  </div>
  <GradeModal
    v-if="showGradeModal"
    :activities="activities"
    :activities-status="activitiesStatus"
    :graded-activity-ids="gradedActivityIds"
    :discipline-name="selectedDisciplineName"
    :saving="savingGrade"
    :error-message="gradeError"
    @close="closeGradeModal"
    @save="saveGrade"
    @retry="loadActivities"
    @go-to-activities="goToActivities"
  />
  <SimulatorHelpModal
    v-if="showHelp"
    @close="closeHelp"
  />
</template>


<script setup>
import { apiRequest } from '../../shared/http/apiRequest.js'
import GradeModal from './GradeModal.vue'
import SimulatorHelpModal from './SimulatorHelpModal.vue'
import SimulatorFilters from './SimulatorFilters.vue'
import SimulatorSituationCard from './SimulatorSituationCard.vue'
import SimulatorSimulationCard from './SimulatorSimulationCard.vue'
import SimulatorGradesCard from './SimulatorGradesCard.vue'
import SimulatorScenariosCard from './SimulatorScenariosCard.vue'
import {
  currentAverageOf,
  filterDisciplinesByPeriod,
  notesFromGrades,
  parseSavedScenario,
  periodsOf,
  simulationStorageKey as storageKeyOf,
  validateDesiredAverage
} from './simulatorRules.js'
import './simulator.css'
import { loadActiveDashboard } from '../../shared/dashboards/useActiveDashboard.js'
import {
  computed,
  nextTick,
  onMounted,
  ref,
  watch
} from 'vue'

// Explicação passo a passo aberta pelo botão "Como funciona?"
const showHelp = ref(false)
const helpButton = ref(null)

async function closeHelp() {
  showHelp.value = false
  await nextTick()
  helpButton.value?.focus()
}

const selectedDiscipline = ref('')
const dashboardId = ref('')
const disciplines = ref([])
const notes = ref([])
const selectedPeriod = ref('')


const emit = defineEmits(['navigate'])
// periodos
const availablePeriods = computed(() => periodsOf(disciplines.value))
// Opções das listas de filtro no formato do AppSelect (lista no visual do site).
const disciplineFilterOptions = computed(() =>
  filteredDisciplines.value.map(discipline => ({
    value: discipline.id,
    label: discipline.name
  }))
)

const periodFilterOptions = computed(() => [
  { value: '', label: 'Todos os períodos' },
  ...availablePeriods.value.map(item => ({ value: item.value, label: item.value }))
])



const filteredDisciplines = computed(() =>
  filterDisciplinesByPeriod(disciplines.value, selectedPeriod.value)
)


/* =========================
   Grade
========================= */

const showGradeModal = ref(false)
const addGradeHint = ref('')
const gradesCard = ref(null)
const filters = ref(null)

// Provas e trabalhos da disciplina: a nota lançada fica vinculada a um deles (relatório, RF06).
const activities = ref([])
const activitiesStatus = ref('idle')

const gradedActivityIds = computed(() =>
  notes.value
    .map(note => note.activityId)
    .filter(Boolean)
)

const selectedDisciplineName = computed(() =>
  disciplines.value.find(item => item.id === selectedDiscipline.value)?.name ?? ''
)

async function loadActivities() {
  const disciplineId = selectedDiscipline.value

  if (!dashboardId.value || !disciplineId) {
    activities.value = []
    activitiesStatus.value = 'idle'
    return
  }

  activitiesStatus.value = 'loading'

  try {
    const result = await apiRequest(
      `/api/v1/dashboards/${dashboardId.value}/disciplines/${disciplineId}/activities`
    )

    // Ignora a resposta se a disciplina mudou enquanto a requisição estava em andamento.
    if (disciplineId !== selectedDiscipline.value) {
      return
    }

    activities.value = result
    activitiesStatus.value = 'ready'
  } catch {
    if (disciplineId !== selectedDiscipline.value) {
      return
    }

    activities.value = []
    activitiesStatus.value = 'error'
  }
}

// Sem disciplina escolhida o botão explica o que falta, em vez de ficar sem resposta.
function openGradeModal() {
  if (!selectedDiscipline.value) {
    addGradeHint.value = 'Selecione uma disciplina acima para lançar a nota.'
    filters.value?.focusDiscipline()
    return
  }

  addGradeHint.value = ''
  gradeError.value = ''

  if (activitiesStatus.value === 'idle' || activitiesStatus.value === 'error') {
    loadActivities()
  }

  showGradeModal.value = true
}

async function closeGradeModal() {
  showGradeModal.value = false
  await nextTick()
  gradesCard.value?.focusAddGradeButton()
}

function goToActivities() {
  showGradeModal.value = false
  emit('navigate', 'activities')
}

// salvar a grade
const savingGrade = ref(false)

async function saveGrade(formData) {
  if (savingGrade.value) {
    return
  }

  if (
    !dashboardId.value ||
    !selectedDiscipline.value
  ) {
    return
  }

  savingGrade.value = true

  try {
    await apiRequest(
      `/api/v1/dashboards/${dashboardId.value}/disciplines/${selectedDiscipline.value}/grades`,
      {
        method: 'POST',

        body: JSON.stringify({
          assessmentName:
            formData.assessmentName,

          score:
            Number(formData.score),

          recordedAt:
            formData.recordedAt,

          activityId:
            formData.activityId
        })
      }
    )

    await loadGrades()

    showResult.value = false

    closeGradeModal()

  } catch (error) {
    // A mensagem aparece dentro do modal, em vez de a falha passar despercebida.
    gradeError.value =
      error.message || 'Não foi possível adicionar a avaliação.'

  } finally {
    savingGrade.value = false
  }
}

const gradeError = ref('')

const passingAverage = ref(6)
const desiredAverage = ref(6)

const showResult = ref(false)

const maxGrade = ref(10)

const scenarios = ref([
  5,
  6,
  7,
  8,
  9,
  10
])


function selectLatestPeriod() {
  const [latest] = periodsOf(disciplines.value)

  if (latest) {
    selectedPeriod.value = latest.value
  }
}


/* =========================
   NOTAS
========================= */

const simulatorLoadError = ref('')

async function loadSimulator() {
  simulatorLoadError.value = ''

  try {

    let dashboard = await loadActiveDashboard(apiRequest)

    if (!dashboard) {
      return
    }

    dashboardId.value = dashboard.id

    disciplines.value =
      await apiRequest(
        `/api/v1/dashboards/${dashboard.id}/disciplines`
      )
    selectLatestPeriod()
  } catch (error) {

    simulatorLoadError.value =
      error.message || 'Não foi possível carregar as disciplinas.'

  }
}


/* =========================
   CONFIGURAÇÕES
========================= */

// adendo , ta uma bosta é melhor colcoar em uma factoryzinha legal

// Status por carregamento de notas: evita que uma resposta atrasada de uma
// disciplina antiga sobrescreva as notas da disciplina selecionada agora.
const gradesStatus = ref('idle')
const gradesError = ref('')
let gradesRequestSeq = 0

async function loadGrades() {
  const disciplineId = selectedDiscipline.value
  const requestSeq = ++gradesRequestSeq

  if (
    !dashboardId.value ||
    !disciplineId
  ) {
    notes.value = []
    gradesStatus.value = 'idle'
    gradesError.value = ''
    return
  }

  gradesStatus.value = 'loading'
  gradesError.value = ''

  try {
    const grades = await apiRequest(
      `/api/v1/dashboards/${dashboardId.value}/disciplines/${disciplineId}/grades`
    )

    // Descarta a resposta se não for mais a requisição mais recente ou se a
    // disciplina selecionada mudou enquanto a requisição estava em andamento.
    if (
      requestSeq !== gradesRequestSeq ||
      disciplineId !== selectedDiscipline.value
    ) {
      return
    }

    notes.value = notesFromGrades(grades)

    gradesStatus.value = 'ready'

  } catch (error) {
    if (
      requestSeq !== gradesRequestSeq ||
      disciplineId !== selectedDiscipline.value
    ) {
      return
    }

    notes.value = []
    gradesStatus.value = 'error'
    gradesError.value =
      error.message || 'Não foi possível carregar as notas desta disciplina.'
  }
}


/* =========================
   MÉDIA ATUAL
========================= */

const currentAverage = computed(() => currentAverageOf(notes.value))

const noteValues = computed(() => notes.value.map(note => note.value))





/* =========================
   SIMULAR
========================= */

const desiredAverageError = ref('')

const simulationCard = ref(null)

/* =========================
   SIMULAÇÃO VIA API
========================= */

// O resultado exibido vem sempre da API (POST .../simulator), nunca de conta
// feita no navegador. status/requiredScoreRaw chegam prontos do backend.
const simulationStatus = ref('idle')
const simulationError = ref('')
const simulationResult = ref(null)
let simulationRequestSeq = 0

async function simulate() {
  if (!selectedDiscipline.value || gradesStatus.value === 'error') {
    return
  }

  if (simulationStatus.value === 'loading') {
    return
  }

  desiredAverageError.value = validateDesiredAverage(desiredAverage.value, maxGrade.value)

  if (desiredAverageError.value) {
    showResult.value = false
    return
  }

  const disciplineId = selectedDiscipline.value
  const target = Number(desiredAverage.value)
  const requestSeq = ++simulationRequestSeq

  simulationStatus.value = 'loading'
  simulationError.value = ''

  try {
    const result = await apiRequest(
      `/api/v1/dashboards/${dashboardId.value}/disciplines/${disciplineId}/simulator`,
      {
        method: 'POST',
        body: JSON.stringify({ targetAverage: target })
      }
    )

    // Ignora respostas atrasadas de uma simulação anterior ou de uma
    // disciplina que já não é mais a selecionada.
    if (
      requestSeq !== simulationRequestSeq ||
      disciplineId !== selectedDiscipline.value
    ) {
      return
    }

    simulationResult.value = result
    simulationStatus.value = 'ready'
    showResult.value = true

    saveSimulationScenario(disciplineId, target)
  } catch (error) {
    if (
      requestSeq !== simulationRequestSeq ||
      disciplineId !== selectedDiscipline.value
    ) {
      return
    }

    simulationResult.value = null
    simulationStatus.value = 'error'
    simulationError.value =
      error.message || 'Não foi possível calcular a simulação.'
    showResult.value = false
  }
}

/* =========================
   PERSISTÊNCIA LOCAL (por usuário/disciplina)
========================= */

function simulationStorageKey(disciplineId) {
  return storageKeyOf(dashboardId.value, disciplineId)
}

function saveSimulationScenario(disciplineId, target) {
  if (!dashboardId.value || !disciplineId) {
    return
  }

  try {
    window.localStorage.setItem(
      simulationStorageKey(disciplineId),
      JSON.stringify({ desiredAverage: target })
    )
    hasSavedSimulation.value = true
  } catch {
    // Armazenamento indisponível (modo privado, quota etc.): a simulação
    // continua funcionando, só não é lembrada da próxima vez.
  }
}

function loadSimulationScenario(disciplineId) {
  if (!dashboardId.value || !disciplineId) {
    return null
  }

  try {
    const key = simulationStorageKey(disciplineId)
    const raw = window.localStorage.getItem(key)
    const scenario = parseSavedScenario(raw)
    if (raw && !scenario) window.localStorage.removeItem(key)
    return scenario
  } catch {
    return null
  }
}

function removeSimulationScenario(disciplineId) {
  if (!dashboardId.value || !disciplineId) {
    return
  }

  try {
    window.localStorage.removeItem(simulationStorageKey(disciplineId))
  } catch {
    // Nada a fazer se o armazenamento não estiver disponível.
  }
}

const hasSavedSimulation = ref(false)

function clearSimulation() {
  const disciplineId = selectedDiscipline.value

  removeSimulationScenario(disciplineId)
  hasSavedSimulation.value = false

  const discipline = disciplines.value.find(item => item.id === disciplineId)

  desiredAverage.value = Number(discipline?.passingAverage ?? 6)
  desiredAverageError.value = ''
  simulationResult.value = null
  simulationStatus.value = 'idle'
  simulationError.value = ''
  showResult.value = false
}


// Pra montar essa bomba
onMounted(() => {
  loadSimulator()
})

// ao trocar o período, limpa a disciplina e os resultados anteriores
watch(selectedPeriod, () => {
  selectedDiscipline.value = ''
  notes.value = []
  showResult.value = false
  simulationResult.value = null
  simulationStatus.value = 'idle'
})

// toda vez que mudar a disciplina, recarrega as notas
watch(selectedDiscipline, async () => {
  showResult.value = false
  addGradeHint.value = ''
  activities.value = []
  activitiesStatus.value = 'idle'

  // Limpa imediatamente as notas antigas para que a disciplina nova nunca
  // mostre, mesmo que por um instante, as notas da disciplina anterior.
  notes.value = []
  gradesStatus.value = 'idle'
  gradesError.value = ''

  simulationResult.value = null
  simulationStatus.value = 'idle'
  simulationError.value = ''
  desiredAverageError.value = ''
  hasSavedSimulation.value = false

  if (!selectedDiscipline.value) {
    return
  }

  const disciplineId = selectedDiscipline.value

  const discipline = disciplines.value.find(
    item => item.id === disciplineId
  )

  if (discipline) {
    passingAverage.value =
      Number(discipline.passingAverage ?? 6)

    desiredAverage.value =
      Number(discipline.passingAverage ?? 6)
  }

  // Restaura a última simulação salva neste dispositivo para esta
  // disciplina, se existir, e recalcula via API para mostrar um valor atual.
  const saved = loadSimulationScenario(disciplineId)

  if (saved && typeof saved.desiredAverage === 'number') {
    desiredAverage.value = saved.desiredAverage
    hasSavedSimulation.value = true
  }

  await Promise.all([loadGrades(), loadActivities()])

  if (saved && disciplineId === selectedDiscipline.value) {
    await simulate()
  }
})

</script>
