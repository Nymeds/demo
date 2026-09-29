<script setup>
import { apiRequest } from '../../shared/http/apiRequest.js'
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { loadActiveDashboard } from '../../shared/dashboards/useActiveDashboard.js'
import { startOfDay } from '../../shared/date/localDate.js'
import ExamDeleteModal from './ExamDeleteModal.vue'
import ExamDetailsModal from './ExamDetailsModal.vue'
import ExamFormModal from './ExamFormModal.vue'
import ExamsCalendar from './ExamsCalendar.vue'
import ExamsFilters from './ExamsFilters.vue'
import ExamsSummaryCards from './ExamsSummaryCards.vue'
import ExamsTable from './ExamsTable.vue'
import ExamsUpcomingCard from './ExamsUpcomingCard.vue'
import { NEXT_DAYS_WINDOW, filterExams, isInMonthOf, isWithinNextDays } from './examFilters.js'
import { buildCalendarDays, formatMonthLabel, normalizeExam } from './examPresentation.js'
import './exams.css'

const emit = defineEmits(['navigate'])

const CLOCK_INTERVAL_MS = 60000

const dashboardId = ref('')
const disciplines = ref([])
const exams = ref([])
const loading = ref(true)
const requestError = ref('')
const loadFailed = ref(false)
const saving = ref(false)
const toast = ref({ message: '', type: 'success' })
let toastTimer

const search = ref('')
const selectedDiscipline = ref('all')
const selectedStatus = ref('all')
const selectedPeriod = ref('all')
const page = ref(1)
const pageSize = 6
const showModal = ref(false)
const selectedExam = ref(null)
const openActionMenu = ref(null)
const showDetailsModal = ref(false)
const examToDelete = ref(null)
const deleting = ref(false)
const monthOffset = ref(0)
const editingExam = ref(null)
const formError = ref('')

const busyExamIds = ref(new Set())
let loadRequestId = 0

function isExamBusy(id) {
  return busyExamIds.value.has(id)
}

function setExamBusy(id, busy) {
  const next = new Set(busyExamIds.value)
  if (busy) next.add(id)
  else next.delete(id)
  busyExamIds.value = next
}

const now = ref(new Date())
let clockTimer

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

async function loadExams() {
  const requestId = ++loadRequestId
  const isStale = () => requestId !== loadRequestId
  loading.value = true
  requestError.value = ''
  loadFailed.value = false

  try {
    let dashboard = await loadActiveDashboard(apiRequest)

    if (!dashboard) {
      dashboard = await apiRequest('/api/v1/dashboards', {
        method: 'POST',
        body: JSON.stringify({
          name: 'Organização acadêmica',
          status: 'ACTIVE',
        }),
      })
    }

    if (isStale()) return
    dashboardId.value = dashboard.id

    const loadedDisciplines = await apiRequest(`/api/v1/dashboards/${dashboard.id}/disciplines`)
    if (isStale()) return
    disciplines.value = loadedDisciplines

    if (disciplines.value.length === 0) {
      exams.value = []
      return
    }

    const loadedExams = await apiRequest(`/api/v1/dashboards/${dashboard.id}/activities?type=EXAM`)
    if (isStale()) return
    exams.value = loadedExams.map(exam => normalizeExam(exam, disciplines.value))
  } catch (error) {
    if (isStale()) return
    requestError.value = error.message || 'Não foi possível carregar as provas.'
    loadFailed.value = true
    showToast(requestError.value, 'error')
  } finally {
    if (!isStale()) loading.value = false
  }
}

const today = computed(() => startOfDay(now.value))

const monthLabel = computed(() => formatMonthLabel(viewedMonthDate.value))

const viewedMonthDate = computed(() => new Date(
  today.value.getFullYear(),
  today.value.getMonth() + monthOffset.value,
  1,
))

const isViewingCurrentMonth = computed(() => monthOffset.value === 0)

const totalExams = computed(() => exams.value.length)
const completedExams = computed(() => exams.value.filter(exam => exam.status === 'COMPLETED').length)
const scheduledExams = computed(() => exams.value.filter(exam => exam.status !== 'COMPLETED'))

const upcomingExams = computed(() => [...scheduledExams.value]
  .filter(exam => isWithinNextDays(exam.dueDate, NEXT_DAYS_WINDOW, today.value))
  .sort((a, b) => a.dueDate.localeCompare(b.dueDate))
  .slice(0, 3))

const thisMonthExams = computed(() => exams.value.filter(exam => isInMonthOf(exam.dueDate, today.value)).length)

const filteredExams = computed(() => filterExams(exams.value, {
  search: search.value,
  discipline: selectedDiscipline.value,
  status: selectedStatus.value,
  period: selectedPeriod.value,
  today: today.value,
}))

const pageCount = computed(() => Math.max(1, Math.ceil(filteredExams.value.length / pageSize)))

const visibleExams = computed(() => filteredExams.value.slice(
  (page.value - 1) * pageSize,
  page.value * pageSize,
))

function resetPage() {
  page.value = 1
}

function showAllScheduled() {
  selectedStatus.value = 'scheduled'
  resetPage()
}

function previousPage() {
  if (page.value > 1) page.value -= 1
}

function nextPage() {
  if (page.value < pageCount.value) page.value += 1
}

function goToPage(number) {
  page.value = number
}

function previousMonth() {
  monthOffset.value -= 1
}

function nextMonth() {
  monthOffset.value += 1
}

function openNewExam() {
  if (disciplines.value.length === 0) {
    requestError.value = 'Cadastre uma disciplina antes de criar uma prova.'
    showToast(requestError.value, 'error')
    return
  }

  editingExam.value = null
  formError.value = ''
  showModal.value = true
}

function openEditExam(exam) {
  openActionMenu.value = null
  editingExam.value = exam
  formError.value = ''
  showModal.value = true
}

function closeModal() {
  showModal.value = false
  editingExam.value = null
  formError.value = ''
}

function toggleActionMenu(examId) {
  openActionMenu.value = openActionMenu.value === examId ? null : examId
}

function viewExam(exam) {
  selectedExam.value = exam
  openActionMenu.value = null
  showDetailsModal.value = true
}

function closeDetailsModal() {
  showDetailsModal.value = false
  selectedExam.value = null
}

function editFromDetails(exam) {
  openEditExam(exam)
  closeDetailsModal()
}

async function toggleExamStatus(exam) {
  openActionMenu.value = null
  if (isExamBusy(exam.id)) return
  requestError.value = ''
  setExamBusy(exam.id, true)

  const nextStatus = exam.status === 'COMPLETED' ? 'PENDING' : 'COMPLETED'

  try {
    const updated = await apiRequest(
      `/api/v1/dashboards/${dashboardId.value}/disciplines/${exam.disciplineId}/activities/${exam.id}`,
      {
        method: 'PUT',
        body: JSON.stringify({
          title: exam.title,
          description: exam.description,
          dueDate: exam.dueDate,
          status: nextStatus,
        }),
      },
    )

    const index = exams.value.findIndex(item => item.id === exam.id)
    if (index >= 0) {
      exams.value[index] = normalizeExam(updated, disciplines.value)
    }

    showToast(nextStatus === 'COMPLETED' ? 'Prova marcada como concluída.' : 'Prova marcada como agendada novamente.')
  } catch (error) {
    requestError.value = error.message || 'Não foi possível atualizar a prova.'
    showToast(requestError.value, 'error')
  } finally {
    setExamBusy(exam.id, false)
  }
}

function askToDeleteExam(exam) {
  openActionMenu.value = null
  requestError.value = ''
  examToDelete.value = exam
}

function closeDeleteModal() {
  examToDelete.value = null
}

async function confirmDeleteExam() {
  if (!examToDelete.value || deleting.value) return

  requestError.value = ''
  deleting.value = true

  try {
    const exam = examToDelete.value

    await apiRequest(
      `/api/v1/dashboards/${dashboardId.value}/disciplines/${exam.disciplineId}/activities/${exam.id}`,
      { method: 'DELETE' },
    )

    exams.value = exams.value.filter(item => item.id !== exam.id)
    if (page.value > pageCount.value) page.value = pageCount.value
    examToDelete.value = null
    showToast('Prova excluída com sucesso.')
  } catch (error) {
    requestError.value = error.message || 'Não foi possível excluir a prova.'
    showToast(requestError.value, 'error')
  } finally {
    deleting.value = false
  }
}

async function saveExam(form) {
  if (saving.value) return
  if (!form.title || !form.disciplineId || !form.date) return

  formError.value = ''
  saving.value = true

  try {
    let saved

    if (editingExam.value) {
      saved = await apiRequest(
        `/api/v1/dashboards/${dashboardId.value}/disciplines/${editingExam.value.disciplineId}/activities/${editingExam.value.id}`,
        {
          method: 'PUT',
          body: JSON.stringify({
            title: form.title,
            description: form.content,
            dueDate: form.date,
            status: form.status,
            type: 'EXAM',
          }),
        },
      )

      const index = exams.value.findIndex(item => item.id === editingExam.value.id)
      if (index >= 0) exams.value[index] = normalizeExam(saved, disciplines.value)
      showToast('Prova atualizada com sucesso.')
    } else {
      saved = await apiRequest(
        `/api/v1/dashboards/${dashboardId.value}/disciplines/${form.disciplineId}/activities`,
        {
          method: 'POST',
          body: JSON.stringify({
            title: form.title,
            description: form.content,
            dueDate: form.date,
            status: 'PENDING',
            type: 'EXAM',
          }),
        },
      )

      const created = normalizeExam(saved, disciplines.value)
      exams.value.push(created)
      resetPage()
      if (filteredExams.value.some(item => item.id === created.id)) {
        showToast('Prova cadastrada com sucesso.')
      } else {
        showToast('Prova criada — ajuste os filtros para vê-la.')
      }
    }

    showModal.value = false
    editingExam.value = null
  } catch (error) {
    formError.value = error.message || 'Não foi possível salvar a prova.'
  } finally {
    saving.value = false
  }
}

const calendarDays = computed(() => buildCalendarDays(
  exams.value,
  viewedMonthDate.value,
  today.value,
  isViewingCurrentMonth.value,
  now.value,
))

onMounted(() => {
  loadExams()
  clockTimer = setInterval(() => {
    now.value = new Date()
  }, CLOCK_INTERVAL_MS)
})

onBeforeUnmount(() => {
  clearTimeout(toastTimer)
  clearInterval(clockTimer)
})
</script>

<template>
  <section class="exams-page" aria-labelledby="exams-title">
    <header class="exams-header">
      <div class="exams-heading">
        <span class="exams-title-icon" aria-hidden="true">
          <svg viewBox="0 0 24 24">
            <rect x="5" y="3" width="14" height="18" rx="2" />
            <path d="M9 3v3h6V3M8 11h8M8 15h5" />
          </svg>
        </span>
        <div>
          <h1 id="exams-title">Provas</h1>
          <p>Acompanhe suas provas e revise os conteúdos.</p>
        </div>
      </div>

      <div class="exams-header-actions">
        <button
          class="exams-button is-primary"
          type="button"
          :disabled="loading || !dashboardId || disciplines.length === 0"
          @click="openNewExam"
        >
          <span aria-hidden="true">＋</span>
          Nova prova
        </button>
      </div>
    </header>

    <p v-if="requestError" class="exams-request-error" role="alert">
      {{ requestError }}
    </p>

    <ExamsSummaryCards
      :total-exams="totalExams"
      :upcoming-count="upcomingExams.length"
      :this-month-exams="thisMonthExams"
      :completed-exams="completedExams"
      :month-label="monthLabel"
    />

    <div class="exams-layout">
      <div class="exams-main-column">
        <section class="exams-panel">
          <ExamsFilters
            v-model:search="search"
            v-model:discipline="selectedDiscipline"
            v-model:status="selectedStatus"
            v-model:period="selectedPeriod"
            :disciplines="disciplines"
            @filter-change="resetPage"
          />

          <div v-if="loading" class="exams-state-card">
            <span class="exams-loader" aria-hidden="true"></span>
            <h2>Carregando provas...</h2>
            <p>Aguarde enquanto buscamos os dados.</p>
          </div>

          <div v-else-if="loadFailed" class="exams-state-card">
            <span aria-hidden="true">⚠️</span>
            <h2>Não foi possível carregar as provas</h2>
            <p>{{ requestError || 'Ocorreu um erro ao buscar os dados. Tente novamente.' }}</p>
            <button class="exams-button is-primary" type="button" @click="loadExams">Tentar novamente</button>
          </div>

          <div v-else-if="disciplines.length === 0" class="exams-state-card">
            <span aria-hidden="true">📚</span>
            <h2>Cadastre uma disciplina primeiro</h2>
            <p>As provas precisam estar vinculadas a uma disciplina.</p>
            <button class="exams-button is-primary" type="button" @click="emit('navigate', 'disciplines')">
              <span aria-hidden="true">＋</span>
              Cadastrar disciplina
            </button>
          </div>

          <div v-else-if="exams.length === 0" class="exams-state-card">
            <span aria-hidden="true">📝</span>
            <h2>Nenhuma prova cadastrada</h2>
            <p>Cadastre sua primeira prova para começar a organizar seus estudos.</p>
            <button class="exams-button is-primary" type="button" @click="openNewExam">
              <span aria-hidden="true">＋</span>
              Nova prova
            </button>
          </div>

          <ExamsTable
            v-else
            :exams="visibleExams"
            :filtered-count="filteredExams.length"
            :page="page"
            :page-count="pageCount"
            :open-action-menu="openActionMenu"
            :busy-exam-ids="busyExamIds"
            :now="now"
            @view="viewExam"
            @edit="openEditExam"
            @toggle-status="toggleExamStatus"
            @delete="askToDeleteExam"
            @toggle-menu="toggleActionMenu"
            @previous-page="previousPage"
            @next-page="nextPage"
            @go-to-page="goToPage"
          />
        </section>

        <aside class="exams-tip">
          <span class="tip-icon" aria-hidden="true">💡</span>
          <div>
            <strong>Dica</strong>
            <p>Revise o conteúdo com antecedência e consulte o simulador de notas para acompanhar seu desempenho.</p>
          </div>
          <button type="button" @click="emit('navigate', 'simulator')">Simulador de Notas</button>
        </aside>
      </div>

      <aside class="exams-side-column">
        <ExamsUpcomingCard :exams="upcomingExams" @view="viewExam" @show-all="showAllScheduled" />
        <ExamsCalendar
          :month-label="monthLabel"
          :days="calendarDays"
          @previous-month="previousMonth"
          @next-month="nextMonth"
        />
      </aside>
    </div>

    <ExamDetailsModal
      v-if="showDetailsModal && selectedExam"
      :exam="selectedExam"
      :now="now"
      @close="closeDetailsModal"
      @edit="editFromDetails"
    />

    <ExamFormModal
      v-if="showModal"
      :exam="editingExam"
      :disciplines="disciplines"
      :saving="saving"
      :error-message="formError"
      @close="closeModal"
      @save="saveExam"
    />

    <ExamDeleteModal
      v-if="examToDelete"
      :exam="examToDelete"
      :deleting="deleting"
      :error-message="requestError"
      @close="closeDeleteModal"
      @confirm="confirmDeleteExam"
    />

    <div v-if="toast.message" class="exams-toast" :class="`is-${toast.type}`" role="status">
      <span>{{ toast.message }}</span>
      <button type="button" aria-label="Fechar aviso" @click="closeToast">×</button>
    </div>
  </section>
</template>
