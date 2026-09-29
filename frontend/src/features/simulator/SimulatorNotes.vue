
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
    <section v-else class="filters-card">

      <!-- DISCIPLINA -->
      <div class="filter">

        <label for="simulator-discipline">Disciplina</label>

        <AppSelect
          id="simulator-discipline"
          ref="disciplineSelect"
          v-model="selectedDiscipline"
          :options="disciplineFilterOptions"
          placeholder="Selecione uma disciplina"
        >
          <template #leading>
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M4 5.5A2.5 2.5 0 0 1 6.5 3H20v16H6.5A2.5 2.5 0 0 0 4 21.5z"/>
              <path d="M4 5.5v16"/>
            </svg>
          </template>
        </AppSelect>

      </div>


      <!-- PERÍODO -->
      <div class="filter">

        <label for="simulator-period">Período/Ano</label>

        <AppSelect
          id="simulator-period"
          v-model="selectedPeriod"
          :options="periodFilterOptions"
        >
          <template #leading>
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="3" y="5" width="18" height="16" rx="2"/>
              <line x1="16" y1="3" x2="16" y2="7"/>
              <line x1="8" y1="3" x2="8" y2="7"/>
              <line x1="3" y1="10" x2="21" y2="10"/>
            </svg>
          </template>
        </AppSelect>

      </div>


    </section>


    <!-- PRIMEIRA LINHA -->
    <div v-if="!simulatorLoadError" class="main-grid">


      <!-- SITUAÇÃO ATUAL -->
      <section class="card situation-card">

        <div class="card-heading">

          <div class="heading-left">

            <h2>Situação atual</h2>

            <span class="badge">
              Média Normal
            </span>

          </div>

        </div>


        <!-- ERRO AO CARREGAR NOTAS -->
        <div
          v-if="gradesStatus === 'error'"
          class="load-error"
        >
          <p role="alert">
            Não foi possível carregar as notas desta disciplina.
          </p>
          <button
            type="button"
            class="retry-button"
            @click="loadGrades"
          >
            Tentar novamente
          </button>
        </div>


        <div v-else class="situation-body">

          <!-- CÍRCULO -->
          <div class="average-circle">

            <div class="circle-content">

              <strong>
                {{ formatAverage(currentAverage) }}
              </strong>

              <span>
                Média atual
              </span>

            </div>

          </div>


          <!-- INFORMAÇÕES -->
          <div class="situation-info">

            <div class="info-row">

              <span>
                Média atual
              </span>

              <strong>
                {{ formatAverage(currentAverage) }}
              </strong>

            </div>


            <div class="info-row">

              <span>
                Média de aprovação
              </span>

              <strong>
                {{ formatAverage(passingAverage) }}
              </strong>

            </div>


            <div class="info-row">

              <span>
                Meta desejada
              </span>

              <div class="target-value">

                <strong>
                  {{ formatAverage(desiredAverage) }}
                </strong>

                <button
                  type="button"
                  class="edit-button"
                  title="Editar meta desejada"
                  aria-label="Editar meta desejada"
                  @click="focusDesiredAverageInput"
                >

                  <svg
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="2"
                  >
                    <path d="M12 20h9"/>
                    <path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L8 18l-4 1 1-4Z"/>
                  </svg>

                </button>

              </div>

            </div>

          </div>

        </div>


        <!-- MENSAGEM -->
        <div
        v-if="gradesStatus !== 'error'"
        class="status-message"
        :class="{
          success:
            notes.length > 0 &&
            currentAverage >= passingAverage,

          warning:
            notes.length > 0 &&
            currentAverage < passingAverage,

          neutral:
            notes.length === 0
        }"
      >

          <div class="status-icon">

            <svg
              v-if="notes.length > 0 && currentAverage >= passingAverage"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <path d="m5 12 4 4L19 6"/>
            </svg>

            <span v-else>!</span>

          </div>

          <div>

            <strong>
            <template v-if="notes.length === 0">
              Nenhuma avaliação lançada.
            </template>

            <template v-else-if="currentAverage >= passingAverage">
              Você está acima da sua média de aprovação!
            </template>

            <template v-else>
              Você ainda está abaixo da média de aprovação.
            </template>
          </strong>

            <span>
              <template v-if="notes.length === 0">
                Adicione suas avaliações para começar.
              </template>

              <template v-else-if="currentAverage >= passingAverage">
                Continue mantendo um bom desempenho.
              </template>

              <template v-else>
                Use o simulador para descobrir quanto precisa tirar.
              </template>
            </span>

          </div>

        </div>

      </section>


      <!-- QUANTO PRECISO TIRAR -->
      <section class="card simulation-card">

        <h2>
          Quanto preciso tirar?
        </h2>

        <p class="description">
          Informe a média que deseja alcançar para calcular.
        </p>


        <!-- ERRO AO CARREGAR NOTAS -->
        <div
          v-if="gradesStatus === 'error'"
          class="load-error"
        >
          <p role="alert">
            Não foi possível carregar as notas desta disciplina.
          </p>
          <button
            type="button"
            class="retry-button"
            @click="loadGrades"
          >
            Tentar novamente
          </button>
        </div>


        <div v-else class="input-grid">

          <!-- MÉDIA DESEJADA -->
          <div class="field">

            <label>
              Média desejada
            </label>

            <input
              ref="desiredAverageInput"
              v-model.number="desiredAverage"
              type="number"
              min="0"
              max="10"
              step="0.1"
              @keydown="blockInvalidNumberKeys"
              :aria-invalid="Boolean(desiredAverageError)"
              aria-describedby="desired-average-error"
              placeholder="8,5"
            />

            <p
              v-if="desiredAverageError"
              id="desired-average-error"
              class="field-error"
              role="alert"
            >
              {{ desiredAverageError }}
            </p>

          </div>


          <!-- NOTA MÁXIMA -->
          <div class="field">

            <label>
              Nota máxima
            </label>

            <input
            type="number"
            :value="maxGrade"
            disabled
          />

          </div>

        </div>


        <!-- BOTÃO -->
        <button
          v-if="gradesStatus !== 'error'"
          class="simulate-button"
          :disabled="!selectedDiscipline || simulationStatus === 'loading'"
          @click="simulate"
        >
          {{ simulationStatus === 'loading' ? 'Simulando...' : 'Simular' }}
        </button>


        <!-- ERRO NA SIMULAÇÃO -->
        <div
          v-if="simulationStatus === 'error' && gradesStatus !== 'error'"
          class="load-error simulation-load-error"
        >
          <p role="alert">
            {{ simulationError || 'Não foi possível calcular a simulação.' }}
          </p>
          <button
            type="button"
            class="retry-button"
            @click="simulate"
          >
            Tentar novamente
          </button>
        </div>


        <!-- RESULTADO -->
        <div
          v-if="showResult && simulationResult && simulationStatus !== 'error' && gradesStatus !== 'error'"
          class="result-box"
        >

          <div class="result-side">

            <span>
              {{ simulationResult.status === 'ALREADY_REACHED' ? 'Situação' : 'Você precisa tirar' }}
            </span>

            <strong>
              {{
                simulationResult.status === 'ALREADY_REACHED'
                  ? 'Meta já alcançada'
                  : formatScore(simulationResult.requiredScoreRaw)
              }}
            </strong>

          </div>


          <div class="result-separator"></div>


          <div class="result-side">

            <span>
              para alcançar sua meta
            </span>

            <strong>
              {{ formatAverage(simulationResult.targetAverage) }}
            </strong>

          </div>

        </div>


        <!-- MENSAGEM RESULTADO -->
        <p
          v-if="showResult && simulationResult && simulationStatus !== 'error' && gradesStatus !== 'error'"
          class="result-message"
        >

          <template v-if="simulationResult.status === 'ALREADY_REACHED'">

            Você já alcançou sua média desejada.

          </template>

          <template v-else-if="simulationResult.status === 'IMPOSSIBLE'">

            Você precisaria de {{ formatScore(simulationResult.requiredScoreRaw) }} —
            acima da nota máxima {{ formatScore(maxGrade) }}.

          </template>

          <template v-else>

            Se tirar
            {{ formatScore(simulationResult.requiredScoreRaw) }}
            na próxima avaliação, você alcançará sua meta.

          </template>

        </p>

        <p
          v-if="gradesStatus !== 'error'"
          class="simulation-local-note"
        >
          A simulação é salva apenas neste dispositivo.
        </p>

        <button
          v-if="gradesStatus !== 'error' && hasSavedSimulation"
          type="button"
          class="clear-simulation-button"
          @click="clearSimulation"
        >
          Limpar simulação
        </button>

      </section>

    </div>


    <!-- SEGUNDA LINHA -->
    <div v-if="!simulatorLoadError" class="bottom-grid">


      <!-- NOTAS LANÇADAS -->
      <section class="card grades-card">

        <div class="card-heading">

          <h2>
            Notas lançadas
          </h2>

        </div>


        <!-- ERRO AO CARREGAR NOTAS -->
        <div
          v-if="gradesStatus === 'error'"
          class="load-error"
        >
          <p role="alert">
            Não foi possível carregar as notas desta disciplina.
          </p>
          <button
            type="button"
            class="retry-button"
            @click="loadGrades"
          >
            Tentar novamente
          </button>
        </div>


        <div v-else class="grades-table">

          <div class="grades-header">

            <span>
              Avaliação
            </span>

            <span>
              Nota obtida
            </span>

            <span>
              Nota máxima
            </span>

            <span>
              Ações
            </span>

          </div>


          <div
            v-for="note in notes"
            :key="note.id"
            class="grade-row"
          >

            <div class="evaluation">

              <div class="evaluation-icon">

                <svg
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="1.8"
                >
                  <path d="M6 3h9l3 3v15H6z"/>
                  <path d="M14 3v4h4"/>
                  <path d="M9 12h6"/>
                  <path d="M9 16h6"/>
                </svg>

              </div>

              <div class="evaluation-text">
                <span>{{ note.name }}</span>
                <small>{{ linkedActivityLabel(note) }}</small>
              </div>

            </div>


            <strong>
              {{ formatScore(note.value) }}
            </strong>


           <span>
             {{ formatScore(maxGrade) }}
          </span>


            <button
              type="button"
              class="more-button"
              title="Editar em Notas"
              aria-label="Editar esta nota na tela Notas"
              @click="emit('navigate', 'grades')"
            >
              Editar em Notas
            </button>

          </div>


          <div
            v-if="notes.length === 0"
            class="empty-row"
          >
            Nenhuma nota lançada.
          </div>


          <div
            v-if="notes.length > 0"
            class="total-row"
          >

            <strong>
              Média atual
            </strong>

            <strong>
              {{ formatAverage(currentAverage) }}
            </strong>

            <span></span>

            <span></span>

          </div>

        </div>


        <button
          ref="addGradeButton"
          class="add-grade-button"
          type="button"
          :aria-describedby="addGradeHint ? 'add-grade-hint' : undefined"
          @click="openGradeModal"
        >
          ＋ Adicionar avaliação lançada
        </button>

        <p
          v-if="addGradeHint"
          id="add-grade-hint"
          class="add-grade-hint"
          role="alert"
        >
          {{ addGradeHint }}
        </p>

      </section>


      <!-- CENÁRIOS -->
      <section class="card scenarios-card">

        <h2>
          Simular diferentes cenários
        </h2>

        <p class="description">
          Cenários hipotéticos calculados localmente — não substituem o
          resultado da simulação acima.
        </p>


        <!-- ERRO AO CARREGAR NOTAS -->
        <div
          v-if="gradesStatus === 'error'"
          class="load-error"
        >
          <p role="alert">
            Não foi possível carregar as notas desta disciplina.
          </p>
          <button
            type="button"
            class="retry-button"
            @click="loadGrades"
          >
            Tentar novamente
          </button>
        </div>


        <div v-else class="scenario-table">

          <div class="scenario-header">

            <span>
              Nota na próxima avaliação
            </span>

            <span>
              Média final projetada
            </span>

          </div>


          <div
            v-for="scenario in scenarios"
            :key="scenario"
            class="scenario-row"
          >

            <span>
              {{ formatScore(scenario) }}
            </span>

            <strong
              :class="{
                positive:
                  projectedAverage(scenario) >= desiredAverage
              }"
            >

              {{ formatAverage(projectedAverage(scenario)) }}

            </strong>

          </div>

        </div>

      </section>

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
import { formatAverage, formatScore } from '../../shared/format/grade.js'
import { apiRequest } from '../../shared/http/apiRequest.js'
import AppSelect from '../../components/ui/AppSelect.vue'
import GradeModal from './GradeModal.vue'
import SimulatorHelpModal from './SimulatorHelpModal.vue'
import { parseSavedScenario, projectedAverage as projectedAverageOf } from './simulatorRules.js'
import { periodKeyOf } from '../grades/gradesPresentation'
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
// Mesma regra de período da tela Notas: o ano pode estar em "periodo" ou em "semester"
// (disciplinas antigas gravam "2026.2" em semester e "2" em periodo).
function getDisciplinePeriod(discipline) {
  const key = periodKeyOf(discipline)

  if (!key) {
    return null
  }

  const [year, semester] = key.split('.').map(Number)

  return {
    year,
    semester,
    value: key
  }
}
const availablePeriods = computed(() => {
  const periods = disciplines.value
    .map(getDisciplinePeriod)
    .filter(item => item !== null)

  const uniquePeriods = [
    ...new Map(
      periods.map(item => [item.value, item])
    ).values()
  ]

  return uniquePeriods.sort((a, b) => {
    if (a.year !== b.year) {
      return b.year - a.year
    }

    return b.semester - a.semester
  })
})
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



const filteredDisciplines = computed(() => {
  if (!selectedPeriod.value) {
    return disciplines.value
  }

  return disciplines.value.filter(discipline => {
    const period = getDisciplinePeriod(discipline)

    return period?.value === selectedPeriod.value
  })
})


/* =========================
   Grade
========================= */

const showGradeModal = ref(false)
const addGradeHint = ref('')
const addGradeButton = ref(null)
const disciplineSelect = ref(null)

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

function linkedActivityLabel(note) {
  if (!note.activityId) {
    return 'Sem avaliação vinculada'
  }

  const activity = activities.value.find(item => item.id === note.activityId)

  return activity ? `Vinculada a ${activity.title}` : 'Avaliação vinculada'
}

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
    disciplineSelect.value?.focus()
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
  addGradeButton.value?.focus()
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
  const periods = disciplines.value
    .map(getDisciplinePeriod)
    .filter(item => item !== null)
    .sort((a, b) => {
      if (a.year !== b.year) {
        return b.year - a.year
      }

      return b.semester - a.semester
    })

  if (periods.length > 0) {
    selectedPeriod.value = periods[0].value
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

    notes.value = grades
      .map(grade => ({
        id: grade.id,
        name: grade.assessmentName,
        value: Number(grade.score),
        recordedAt: grade.recordedAt,
        activityId: grade.activityId ?? null
      }))
      .sort((a, b) => {
        return (
          new Date(b.recordedAt).getTime()
          -
          new Date(a.recordedAt).getTime()
        )
      })

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

const currentAverage = computed(() => {
  if (notes.value.length === 0) {
    return 0
  }

  const total = notes.value.reduce(
    (sum, note) => sum + Number(note.value),
    0
  )

  return total / notes.value.length
})





/* =========================
   SIMULAR
========================= */

const desiredAverageError = ref('')

// Notas e médias são positivas: "-", "+" e "e" (notação científica) não são digitáveis.
function blockInvalidNumberKeys(event) {
  if (['-', '+', 'e', 'E'].includes(event.key)) {
    event.preventDefault()
  }
}

function validateDesiredAverage(value) {
  if (value === '' || value === null || Number.isNaN(Number(value))) {
    return 'Informe a média desejada.'
  }

  if (Number(value) < 0) {
    return 'A média desejada não pode ser negativa.'
  }

  if (Number(value) > maxGrade.value) {
    return 'A média desejada deve ser de no máximo 10.'
  }

  return ''
}

const desiredAverageInput = ref(null)

function focusDesiredAverageInput() {
  desiredAverageInput.value?.focus()
}

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

  desiredAverageError.value = validateDesiredAverage(desiredAverage.value)

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
  return `studdy:simulator:${dashboardId.value}:${disciplineId}`
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


/* =========================
   PROJEÇÃO
========================= */

function projectedAverage(nextGrade) {
  return projectedAverageOf(notes.value.map(note => note.value), nextGrade)
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


<style scoped>

.field-error {
  color: #c4463e;
  font-size: 12px;
  font-weight: 600;
  margin: 6px 0 0;
}

.load-error {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 10px;
  padding: 16px;
  border: 1px solid #f3d3ce;
  border-radius: 9px;
  background: #fdf1ef;
}

.load-error p {
  margin: 0;
  color: #a3392f !important;
  font-size: 13px;
  font-weight: 600;
}

.retry-button {
  height: 36px;
  padding: 0 16px;
  border: 1px solid #c4463e;
  border-radius: 8px;
  background: #ffffff;
  color: #a3392f !important;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.retry-button:hover {
  background: #fbe6e3;
}

.page-load-error {
  margin-bottom: 20px;
}


/* =========================
   PÁGINA
========================= */

.simulator-page {

  width: 100%;
  min-height: 100vh;

  box-sizing: border-box;

  padding: 28px 32px 40px;

  background: #f7f7fb;

  color: #202033;

}


/* =========================
   CABEÇALHO
========================= */

.page-header {

  display: flex;

  align-items: center;

  justify-content: space-between;

  margin-bottom: 26px;

}


.title-area {

  display: flex;

  align-items: center;

  gap: 14px;

}


.title-icon {

  width: 42px;
  height: 42px;

  display: flex;

  align-items: center;

  justify-content: center;

  border-radius: 10px;

  background: #f0eaff;

  color: #6330e0;

}


.title-icon svg {

  width: 23px;
  height: 23px;

}


.title-area h1 {

  margin: 0;

  color: #151525 !important;

  font-size: 28px;

  font-weight: 700;

}


.title-area p {

  margin: 6px 0 0;

  color: #555267 !important;

  font-size: 14px;

}


.header-actions {

  display: flex;

  align-items: center;

  gap: 14px;

}


.help-button {

  height: 42px;

  display: flex;

  align-items: center;

  gap: 8px;

  padding: 0 18px;

  border: 1px solid #dedce8;

  border-radius: 9px;

  background: white;

  color: #3d3a4d;

  font-size: 14px;

}


.help-icon {

  width: 17px;
  height: 17px;

  display: flex;

  align-items: center;

  justify-content: center;

  border: 1px solid #858292;

  border-radius: 50%;

}


/* =========================
   FILTROS
========================= */

.filters-card {

  display: grid;

  grid-template-columns: 1.3fr 1fr;

  margin-bottom: 20px;

  background: white;

  border: 1px solid #eceaf2;

  border-radius: 12px;

  overflow: hidden;

}


.filter {

  padding: 17px 22px;

  border-right: 1px solid #eceaf2;

}


.filter:last-child {

  border-right: none;

}


.filter label {

  display: block;

  margin-bottom: 8px;

  color: #686579 !important;

  font-size: 13px;

}


/* Listas de filtro no mesmo padrão dos outros campos do site (AppSelect). */
.filter {

  --app-select-height: 42px;

  --app-select-font-size: 14px;

  --app-select-radius: 8px;

  --app-select-padding: 0 12px 0 14px;

}


/* =========================
   GRID
========================= */

.main-grid,
.bottom-grid {

  display: grid;

  grid-template-columns: 1.05fr .95fr;

  gap: 20px;

  margin-bottom: 20px;

}


.card {

  box-sizing: border-box;

  padding: 22px;

  background: #ffffff;

  border: 1px solid #eceaf2;

  border-radius: 12px;

  box-shadow: 0 2px 8px rgba(25, 20, 60, .03);

}


.card h2 {

  margin: 0;

  color: #202033 !important;

  font-size: 17px;

  font-weight: 700;

}


/* =========================
   TÍTULOS DOS CARDS
========================= */

.card-heading {

  margin-bottom: 18px;

}


.heading-left {

  display: flex;

  align-items: center;

  gap: 10px;

}


.badge {

  padding: 5px 9px;

  border-radius: 20px;

  background: #f0eaff;

  color: #6330e0 !important;

  font-size: 11px;

  font-weight: 600;

}


/* =========================
   SITUAÇÃO ATUAL
========================= */

.situation-body {

  display: flex;

  align-items: center;

  gap: 30px;

  min-height: 190px;

}


.average-circle {

  width: 170px;
  height: 170px;

  flex-shrink: 0;

  display: flex;

  align-items: center;

  justify-content: center;

  border-radius: 50%;

  background:

    radial-gradient(
      circle,
      white 0 61%,
      transparent 62%
    ),

    conic-gradient(
      #6330e0 0deg 306deg,
      #dedde6 306deg 360deg
    );

}


.circle-content {

  display: flex;

  flex-direction: column;

  align-items: center;

}


.circle-content strong {

  color: #6330e0 !important;

  font-size: 35px;

}


.circle-content span {

  color: #555267 !important;

  font-size: 12px;

}


.situation-info {

  flex: 1;

}


.info-row {

  min-height: 48px;

  display: flex;

  align-items: center;

  justify-content: space-between;

  border-bottom: 1px solid #eeeef3;

}


.info-row span {

  color: #686579 !important;

  font-size: 13px;

}


.info-row strong {

  color: #202033 !important;

}


.target-value {

  display: flex;

  align-items: center;

  gap: 10px;

}


.edit-button {

  width: 30px;
  height: 30px;

  display: flex;

  align-items: center;
  justify-content: center;

  border: 1px solid #dedce8;

  border-radius: 7px;

  background: #ffffff;

  color: #6330e0;

}


.edit-button svg {

  width: 15px;
  height: 15px;

}


/* =========================
   MENSAGEM VERDE
========================= */

.status-message {

  display: flex;

  align-items: center;

  gap: 12px;

  margin-top: 18px;

  padding: 14px 16px;

  border-radius: 9px;

}


.status-message.success {

  background: #edf9f1;

  color: #24834b !important;

}


.status-message.warning {

  background: #fff7e8;

  color: #a56c00 !important;

}

.status-message.neutral {
  background: #f2f2f6;
  color: #686579 !important;
}


.status-icon {

  width: 25px;
  height: 25px;

  flex-shrink: 0;

  display: flex;

  align-items: center;

  justify-content: center;

  border: 1.5px solid currentColor;

  border-radius: 50%;

}


.status-icon svg {

  width: 15px;
  height: 15px;

}


.status-message strong {

  display: block;

  color: inherit !important;

  font-size: 12px;

}


.status-message span {

  display: block;

  margin-top: 3px;

  color: inherit !important;

  font-size: 11px;

}


/* =========================
   SIMULAÇÃO
========================= */

.description {

  margin: 6px 0 19px;

  color: #686579 !important;

  font-size: 12px;

}


.input-grid {

  display: grid;

  grid-template-columns: 1fr 1fr;

  gap: 14px;

}


.field label {

  display: block;

  margin-bottom: 8px;

  color: #555267 !important;

  font-size: 12px;

  font-weight: 600;

}


/* CAMPO CORRIGIDO */

.field input {

  width: 100%;

  height: 42px;

  box-sizing: border-box;

  padding: 0 13px;

  border: 1px solid #dedce8;

  border-radius: 8px;

  background: #ffffff !important;

  color: #252338 !important;

  font-size: 14px;

  outline: none;

  appearance: auto;

}


.field input:focus {

  background: #ffffff !important;

  color: #252338 !important;

  border-color: #6330e0;

  outline: none;

  box-shadow: 0 0 0 2px rgba(99, 48, 224, .08);

}


.field input:disabled {

  background: #f1f1f3 !important;

  color: #555267 !important;

  opacity: 1;

}


/* =========================
   BOTÃO SIMULAR
========================= */

.simulate-button {

  width: 100%;

  height: 42px;

  margin-top: 17px;

  border: none;

  border-radius: 8px;

  background: #6330e0;

  color: white;

  font-size: 14px;

  font-weight: 600;

  cursor: pointer;

}


.simulate-button:hover {

  background: #5726ce;

}

.simulate-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}


/* =========================
   RESULTADO
========================= */

.result-box {

  min-height: 110px;

  display: grid;

  grid-template-columns: 1fr 1px 1fr;

  align-items: center;

  margin-top: 18px;

  padding: 10px 18px;

  border: 1px solid #ddd2ff;

  border-radius: 9px;

  background: #f8f5ff;

}


.result-side {

  display: flex;

  flex-direction: column;

  align-items: center;

  text-align: center;

}


.result-side span {

  color: #686579 !important;

  font-size: 11px;

}


.result-side strong {

  margin-top: 5px;

  color: #6330e0 !important;

  font-size: 29px;

}


.result-separator {

  width: 1px;

  height: 55px;

  background: #dcd1f7;

}


.result-message {

  margin: 9px 0 0;

  color: #686579 !important;

  font-size: 11px;

  text-align: center;

}


.simulation-load-error {

  margin-top: 17px;

}


.simulation-local-note {

  margin: 10px 0 0;

  color: #9997a8 !important;

  font-size: 11px;

  text-align: center;

}


.clear-simulation-button {

  width: 100%;

  height: 34px;

  margin-top: 8px;

  border: 1px solid #dedce8;

  border-radius: 8px;

  background: #ffffff;

  color: #686579 !important;

  font-size: 12px;

  font-weight: 600;

  cursor: pointer;

}


.clear-simulation-button:hover {

  border-color: #6330e0;

  color: #6330e0 !important;

}


/* =========================
   TABELA DE NOTAS
========================= */

.grades-table,
.scenario-table {

  border: 1px solid #eeeef3;

  border-radius: 8px;

  overflow: hidden;

}


.grades-header,
.grade-row,
.total-row {

  display: grid;

  grid-template-columns: 1.5fr .8fr .8fr 45px;

  align-items: center;

  padding: 11px 12px;

  font-size: 11px;

}


.grades-header {

  background: #fafafd;

  color: #686579 !important;

  font-weight: 600;

}


.grade-row {

  min-height: 48px;

  border-top: 1px solid #eeeef3;

}


.evaluation {

  display: flex;

  align-items: center;

  gap: 9px;

}


.evaluation-icon {

  width: 29px;
  height: 29px;

  display: flex;

  align-items: center;

  justify-content: center;

  border-radius: 6px;

  background: #f0eaff;

  color: #6330e0;

}


.evaluation-icon svg {

  width: 16px;
  height: 16px;

}


.more-button {

  border: none;

  background: transparent;

  color: #555267;

  font-size: 19px;

  cursor: pointer;

}


.total-row {

  min-height: 42px;

  border-top: 1px solid #eeeef3;

}


.total-row strong {

  color: #6330e0 !important;

}


.empty-row {

  padding: 25px;

  color: #9997a8 !important;

  text-align: center;

}


.add-grade-button {

  width: 100%;

  height: 40px;

  margin-top: 12px;

  border: 1px dashed #cfcbdc;

  border-radius: 8px;

  background: #ffffff;

  color: #6330e0 !important;

  cursor: pointer;

}


.add-grade-button:hover {

  border-color: #6330e0;

  background: #f7f3ff;

}


.add-grade-button:focus-visible {

  outline: 2px solid rgba(99, 48, 224, 0.45);

  outline-offset: 2px;

}


.add-grade-hint {

  margin: 8px 0 0;

  color: #b4520c;

  font-size: 12px;

  font-weight: 600;

}


.evaluation-text {

  display: grid;

  gap: 2px;

  min-width: 0;

}


.evaluation-text small {

  color: #8a879b !important;

  font-size: 10px;

}


/* =========================
   CENÁRIOS
========================= */

.scenario-header,
.scenario-row {

  display: grid;

  grid-template-columns: 1fr 1fr;

  align-items: center;

  min-height: 35px;

  text-align: center;

  font-size: 11px;

}


.scenario-header {

  background: #fafafd;

  color: #686579 !important;

  font-weight: 600;

}


.scenario-row {

  border-top: 1px solid #eeeef3;

}


.scenario-row strong {

  color: #292638 !important;

}


.scenario-row strong.positive {

  color: #238b4e !important;

}


/* =========================
   RESPONSIVO
========================= */

@media (max-width: 1100px) {

  .main-grid,
  .bottom-grid {

    grid-template-columns: 1fr;

  }

}


@media (max-width: 800px) {

  .filters-card {

    grid-template-columns: 1fr;

  }

  .filter {

    border-right: none;

    border-bottom: 1px solid #eceaf2;

  }

  .header-actions {

    display: none;

  }

}


@media (max-width: 600px) {

  .simulator-page {

    padding: 18px;

  }

  .situation-body {

    flex-direction: column;

  }

  .input-grid {

    grid-template-columns: 1fr;

  }

}

</style>
