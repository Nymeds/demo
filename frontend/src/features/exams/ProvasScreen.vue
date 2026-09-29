<script setup>
import { apiRequest } from '../../shared/http/apiRequest.js'
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { loadActiveDashboard } from '../../shared/dashboards/useActiveDashboard.js'
import { useFocusTrap } from '../../shared/a11y/useFocusTrap.js'
import { isOverdue as isDateOverdue, parseLocalDate, startOfDay } from '../../shared/date/localDate.js'
import { NEXT_DAYS_WINDOW, filterExams, isInMonthOf, isWithinNextDays } from './examFilters.js'

const emit = defineEmits(['navigate'])

const CLOCK_INTERVAL_MS = 60000
const COLOR_PALETTE = ['purple', 'green', 'orange', 'blue']

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

const examModalRef = ref(null)
const detailsModalRef = ref(null)
const deleteModalRef = ref(null)
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

const STATUS_OPTIONS = [
  { value: 'PENDING', label: 'Agendada' },
  { value: 'IN_PROGRESS', label: 'Em andamento' },
  { value: 'COMPLETED', label: 'Concluída' },
]

const form = ref({
  title: '',
  disciplineId: '',
  date: '',
  content: '',
  status: 'PENDING',
})

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

function colorForDiscipline(disciplineId) {
  const index = disciplines.value.findIndex(discipline => discipline.id === disciplineId)
  if (index < 0) return 'purple'
  return COLOR_PALETTE[index % COLOR_PALETTE.length]
}

function disciplineById(id) {
  return disciplines.value.find(discipline => discipline.id === id)
}

function disciplineLabel(discipline) {
  if (!discipline) return 'Disciplina'

  const hasHomonym = disciplines.value.some(other => (
    other.id !== discipline.id && other.name === discipline.name
  ))

  if (!hasHomonym) return discipline.name

  const distinguisher = discipline.professorName || discipline.periodo || discipline.semester
  return distinguisher ? `${discipline.name} (${distinguisher})` : discipline.name
}

function normalizeExam(exam) {
  const discipline = disciplineById(exam.disciplineId)

  return {
    ...exam,
    disciplineName: disciplineLabel(discipline),
    color: colorForDiscipline(exam.disciplineId),
  }
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
    exams.value = loadedExams.map(normalizeExam)
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

const monthLabel = computed(() => new Intl.DateTimeFormat('pt-BR', {
  month: 'long',
  year: 'numeric',
}).format(viewedMonthDate.value).replace(/^./, value => value.toUpperCase()))

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

function formatDate(dateString) {
  const date = parseLocalDate(dateString)
  if (!date) return '—'
  return new Intl.DateTimeFormat('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  }).format(date)
}

function formatShortDate(dateString) {
  const date = parseLocalDate(dateString)
  if (!date) return '—'
  return new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: '2-digit' }).format(date)
}

function formatWeekday(dateString) {
  const date = parseLocalDate(dateString)
  if (!date) return '—'
  return new Intl.DateTimeFormat('pt-BR', { weekday: 'long' }).format(date)
}

function isOverdue(exam) {
  if (exam.status === 'COMPLETED') return false
  return isDateOverdue(exam.dueDate, now.value)
}

function examState(exam) {
  if (exam.status === 'COMPLETED') return 'completed'
  if (isOverdue(exam)) return 'overdue'
  return 'scheduled'
}

function statusLabel(exam) {
  const state = examState(exam)
  if (state === 'completed') return 'Concluída'
  if (state === 'overdue') return 'Atrasada'
  return 'Agendada'
}

function statusClass(exam) {
  const state = examState(exam)
  return state === 'completed' ? 'is-completed' : state === 'overdue' ? 'is-overdue' : 'is-scheduled'
}

function resetPage() {
  page.value = 1
}

function previousPage() {
  if (page.value > 1) page.value -= 1
}

function nextPage() {
  if (page.value < pageCount.value) page.value += 1
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
  form.value = {
    title: '',
    disciplineId: '',
    date: '',
    content: '',
    status: 'PENDING',
  }
  showModal.value = true
}

function openEditExam(exam) {
  openActionMenu.value = null
  editingExam.value = exam
  formError.value = ''
  form.value = {
    title: exam.title,
    disciplineId: exam.disciplineId,
    date: exam.dueDate,
    content: exam.description || '',
    status: exam.status,
  }
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
      exams.value[index] = normalizeExam(updated)
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

async function saveExam() {
  if (saving.value) return
  if (!form.value.title || !form.value.disciplineId || !form.value.date) return

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
            title: form.value.title,
            description: form.value.content,
            dueDate: form.value.date,
            status: form.value.status,
            type: 'EXAM',
          }),
        },
      )

      const index = exams.value.findIndex(item => item.id === editingExam.value.id)
      if (index >= 0) exams.value[index] = normalizeExam(saved)
      showToast('Prova atualizada com sucesso.')
    } else {
      saved = await apiRequest(
        `/api/v1/dashboards/${dashboardId.value}/disciplines/${form.value.disciplineId}/activities`,
        {
          method: 'POST',
          body: JSON.stringify({
            title: form.value.title,
            description: form.value.content,
            dueDate: form.value.date,
            status: 'PENDING',
            type: 'EXAM',
          }),
        },
      )

      const created = normalizeExam(saved)
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

const calendarDays = computed(() => {
  const year = viewedMonthDate.value.getFullYear()
  const month = viewedMonthDate.value.getMonth()
  const firstDay = new Date(year, month, 1).getDay()
  const daysInMonth = new Date(year, month + 1, 0).getDate()

  return Array.from({ length: firstDay + daysInMonth }, (_, index) => {
    if (index < firstDay) {
      return { number: '', key: `empty-${index}`, state: '' }
    }

    const number = index - firstDay + 1
    const iso = `${year}-${String(month + 1).padStart(2, '0')}-${String(number).padStart(2, '0')}`
    const dayExams = exams.value.filter(item => item.dueDate === iso)
    const isToday = isViewingCurrentMonth.value && number === today.value.getDate()
    const hasOverdue = dayExams.some(item => isOverdue(item))
    const hasScheduled = dayExams.some(item => item.status !== 'COMPLETED' && !isOverdue(item))
    const hasCompleted = dayExams.some(item => item.status === 'COMPLETED')

    const state = isToday
      ? 'today'
      : hasOverdue
        ? 'overdue'
        : hasScheduled
          ? 'upcoming'
          : hasCompleted
            ? 'completed'
            : ''

    return { number, key: iso, state }
  })
})

useFocusTrap(showModal, examModalRef, { onClose: closeModal, closeOnEscape: () => !saving.value })
useFocusTrap(showDetailsModal, detailsModalRef, { onClose: closeDetailsModal })
useFocusTrap(() => !!examToDelete.value, deleteModalRef, { onClose: closeDeleteModal, closeOnEscape: () => !deleting.value })

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

    <div class="exams-summary-grid">
      <article class="exams-summary-card is-purple">
        <span class="summary-icon" aria-hidden="true">
          <svg viewBox="0 0 24 24"><rect x="4" y="5" width="16" height="16" rx="2" /><path d="M8 3v4m8-4v4M4 10h16" /></svg>
        </span>
        <div>
          <p>Total de provas</p>
          <strong>{{ totalExams }}</strong>
          <small>Todas as disciplinas</small>
        </div>
      </article>

      <article class="exams-summary-card is-green">
        <span class="summary-icon" aria-hidden="true">
          <svg viewBox="0 0 24 24"><rect x="4" y="5" width="16" height="16" rx="2" /><path d="M8 3v4m8-4v4M4 10h16M9 15l2 2 4-4" /></svg>
        </span>
        <div>
          <p>Próximas provas</p>
          <strong>{{ upcomingExams.length }}</strong>
          <small>Nos próximos 7 dias</small>
        </div>
      </article>

      <article class="exams-summary-card is-orange">
        <span class="summary-icon" aria-hidden="true">
          <svg viewBox="0 0 24 24"><path class="hourglass-shape" d="M6.5 3h11M6.5 21h11M8 3h8c0 4-1.6 5.7-4 7.5-2.4 1.8-4 3.5-4 7.5h8c0-4-1.6-5.7-4-7.5C9.6 8.7 8 7 8 3z" /><path class="hourglass-sand" d="M9.1 5.1h5.8c-.5 1.9-1.4 2.8-2.9 4-.4-.3-.8-.6-1.2-.9-1-.8-1.5-1.6-1.7-3.1zM9.1 18.9h5.8c-.5-1.9-1.4-2.8-2.9-4-.4.3-.8.6-1.2.9-1 .8-1.5 1.6-1.7 3.1z" /></svg>
        </span>
        <div>
          <p>Este mês</p>
          <strong>{{ thisMonthExams }}</strong>
          <small>{{ monthLabel }}</small>
        </div>
      </article>

      <article class="exams-summary-card is-blue">
        <span class="summary-icon" aria-hidden="true">
          <svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="9" /><path d="m8 12 3 3 5-6" /></svg>
        </span>
        <div>
          <p>Provas concluídas</p>
          <strong>{{ completedExams }}</strong>
          <small>{{ totalExams ? Math.round((completedExams / totalExams) * 100) : 0 }}% do total</small>
        </div>
      </article>
    </div>

    <div class="exams-layout">
      <div class="exams-main-column">
        <section class="exams-panel">
          <div class="exams-filters">
            <label class="exams-search">
              <span class="sr-only">Buscar prova</span>
              <svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="11" cy="11" r="7" /><path d="m20 20-3.5-3.5" /></svg>
              <input v-model="search" type="search" placeholder="Buscar prova..." @input="resetPage">
            </label>

            <label>
              <span>Disciplinas</span>
              <select v-model="selectedDiscipline" @change="resetPage">
                <option value="all">Todas</option>
                <option v-for="discipline in disciplines" :key="discipline.id" :value="discipline.id">{{ discipline.name }}</option>
              </select>
            </label>

            <label>
              <span>Status</span>
              <select v-model="selectedStatus" @change="resetPage">
                <option value="all">Todos</option>
                <option value="scheduled">Agendadas</option>
                <option value="completed">Concluídas</option>
              </select>
            </label>

            <label>
              <span>Período</span>
              <select v-model="selectedPeriod" @change="resetPage">
                <option value="all">Todos</option>
                <option value="next7">Próximos 7 dias</option>
                <option value="month">Este mês</option>
              </select>
            </label>
          </div>

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

          <template v-else>
            <div class="exams-table-wrap">
              <table class="exams-table">
                <thead>
                  <tr>
                    <th>Prova</th>
                    <th>Disciplina</th>
                    <th>Data</th>
                    <th>Conteúdo</th>
                    <th>Status</th>
                    <th>Ações</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="exam in visibleExams" :key="exam.id">
                    <td>
                      <div class="exam-name">
                        <span class="exam-row-icon" :class="`is-${exam.color}`" aria-hidden="true">
                          <svg viewBox="0 0 24 24"><rect x="5" y="3" width="14" height="18" rx="2" /><path d="M9 3v3h6V3M8 11h8M8 15h5" /></svg>
                        </span>
                        <div>
                          <strong>{{ exam.title }}</strong>
                          <small>Avaliação</small>
                        </div>
                      </div>
                    </td>
                    <td>
                      <strong class="discipline-name">{{ exam.disciplineName }}</strong>
                    </td>
                    <td>
                      <strong>{{ formatDate(exam.dueDate) }}</strong>
                      <small>{{ formatWeekday(exam.dueDate) }}</small>
                    </td>
                    <td class="content-cell">{{ exam.description || 'Conteúdo não informado' }}</td>
                    <td>
                      <span class="status-pill" :class="statusClass(exam)">
                        <svg v-if="exam.status !== 'COMPLETED'" viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="8.5" /><path d="M12 10v5" /><circle cx="12" cy="7.2" r=".7" fill="currentColor" stroke="none" /></svg>
                        <svg v-else viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="8.5" /><path d="m8.5 12 2.3 2.4 4.8-5" /></svg>
                        {{ statusLabel(exam) }}
                      </span>
                    </td>
                    <td>
                      <div class="row-actions">
                        <button type="button" aria-label="Visualizar prova" title="Visualizar prova" @click="viewExam(exam)">
                          <svg viewBox="0 0 24 24"><path d="M2.5 12s3.5-6 9.5-6 9.5 6 9.5 6-3.5 6-9.5 6-9.5-6-9.5-6Z" /><circle cx="12" cy="12" r="2.5" /></svg>
                        </button>
                        <div class="row-action-menu-wrap">
                          <button
                            type="button"
                            aria-label="Mais opções"
                            title="Mais opções"
                            :aria-expanded="openActionMenu === exam.id"
                            @click="toggleActionMenu(exam.id)"
                          >
                            <svg viewBox="0 0 24 24"><circle cx="5" cy="12" r="1" /><circle cx="12" cy="12" r="1" /><circle cx="19" cy="12" r="1" /></svg>
                          </button>
                          <div v-if="openActionMenu === exam.id" class="row-action-menu">
                            <button type="button" @click="viewExam(exam)">Visualizar</button>
                            <button type="button" @click="openEditExam(exam)">Editar</button>
                            <button type="button" :disabled="isExamBusy(exam.id)" @click="toggleExamStatus(exam)">
                              {{ exam.status === 'COMPLETED' ? 'Marcar como agendada' : 'Marcar como concluída' }}
                            </button>
                            <button type="button" class="is-danger" @click="askToDeleteExam(exam)">Excluir</button>
                          </div>
                        </div>
                      </div>
                    </td>
                  </tr>
                  <tr v-if="visibleExams.length === 0">
                    <td colspan="6" class="empty-row">
                      <span aria-hidden="true">🔎</span>
                      <strong>Nenhuma prova encontrada</strong>
                      <small>Tente ajustar os filtros ou cadastrar uma nova prova.</small>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>

            <footer class="exams-table-footer">
              <p>Mostrando {{ visibleExams.length }} de {{ filteredExams.length }} provas</p>
              <nav aria-label="Paginação">
                <button type="button" :disabled="page === 1" @click="previousPage">‹</button>
                <button v-for="number in pageCount" :key="number" type="button" :class="{ active: page === number }" @click="page = number">{{ number }}</button>
                <button type="button" :disabled="page === pageCount" @click="nextPage">›</button>
              </nav>
            </footer>
          </template>
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
        <section class="upcoming-card">
          <header>
            <strong>Próximas provas</strong>
            <button type="button" @click="selectedStatus = 'scheduled'; resetPage()">Ver todas</button>
          </header>

          <p v-if="upcomingExams.length === 0" class="upcoming-empty">Nenhuma prova nos próximos {{ NEXT_DAYS_WINDOW }} dias.</p>

          <button v-for="(exam, index) in upcomingExams" :key="exam.id" class="upcoming-item" type="button" @click="viewExam(exam)">
            <span class="upcoming-number" :class="`is-${exam.color}`">{{ index + 1 }}</span>
            <span class="upcoming-details">
              <strong>{{ exam.title }}</strong>
              <small>{{ exam.disciplineName }}</small>
            </span>
            <span class="upcoming-date">
              <strong>{{ formatShortDate(exam.dueDate) }}</strong>
            </span>
          </button>
        </section>

        <section class="mini-calendar">
          <header>
            <button type="button" aria-label="Mês anterior" @click="previousMonth">‹</button>
            <strong>{{ monthLabel }}</strong>
            <button type="button" aria-label="Próximo mês" @click="nextMonth">›</button>
          </header>

          <div class="calendar-weekdays">
            <span v-for="day in ['Dom', 'Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb']" :key="day">{{ day }}</span>
          </div>

          <div class="calendar-grid">
            <span
              v-for="day in calendarDays"
              :key="day.key"
              :class="['calendar-day', day.state]"
            >{{ day.number }}</span>
          </div>

          <div class="calendar-legend">
            <span><i class="legend-dot is-today"></i>Hoje</span>
            <span><i class="legend-dot is-upcoming"></i>Agendada</span>
            <span><i class="legend-dot is-overdue"></i>Atrasada</span>
            <span><i class="legend-dot is-completed"></i>Concluída</span>
          </div>
        </section>
      </aside>
    </div>

    <div v-if="showDetailsModal && selectedExam" class="exam-modal-overlay" @click.self="closeDetailsModal">
      <section ref="detailsModalRef" class="exam-modal exam-details-modal" role="dialog" aria-modal="true" aria-labelledby="exam-details-title" tabindex="-1">
        <header>
          <div>
            <span class="modal-kicker">Detalhes da prova</span>
            <h2 id="exam-details-title">{{ selectedExam.title }}</h2>
          </div>
          <button type="button" aria-label="Fechar" @click="closeDetailsModal">×</button>
        </header>

        <div class="exam-details-grid">
          <div>
            <span>Disciplina</span>
            <strong>{{ selectedExam.disciplineName }}</strong>
          </div>
          <div>
            <span>Data</span>
            <strong>{{ formatDate(selectedExam.dueDate) }}</strong>
          </div>
          <div>
            <span>Status</span>
            <strong>{{ statusLabel(selectedExam) }}</strong>
          </div>
          <div class="exam-details-full">
            <span>Conteúdo</span>
            <strong>{{ selectedExam.description || 'Conteúdo não informado' }}</strong>
          </div>
        </div>

        <footer>
          <button class="exams-button is-secondary" type="button" @click="openEditExam(selectedExam); closeDetailsModal()">Editar</button>
          <button class="exams-button is-primary" type="button" @click="closeDetailsModal">Fechar</button>
        </footer>
      </section>
    </div>

    <div v-if="showModal" class="exam-modal-overlay" @click.self="closeModal">
      <section ref="examModalRef" class="exam-modal" role="dialog" aria-modal="true" aria-labelledby="new-exam-title" tabindex="-1">
        <header>
          <div>
            <span class="modal-kicker">{{ editingExam ? 'Editar avaliação' : 'Nova avaliação' }}</span>
            <h2 id="new-exam-title">{{ editingExam ? 'Editar prova' : 'Cadastrar prova' }}</h2>
          </div>
          <button type="button" aria-label="Fechar" @click="closeModal">×</button>
        </header>

        <form @submit.prevent="saveExam">
          <label>
            Nome da prova
            <input v-model.trim="form.title" required placeholder="Ex.: Prova 2 - Estruturas" :disabled="saving">
          </label>

          <div class="modal-grid">
            <label>
              Disciplina
              <select v-model="form.disciplineId" required :disabled="saving || !!editingExam">
                <option value="" disabled>Selecione</option>
                <option v-for="discipline in disciplines" :key="discipline.id" :value="discipline.id">{{ disciplineLabel(discipline) }}</option>
              </select>
              <small v-if="editingExam" class="field-hint">A disciplina não pode ser alterada após a criação.</small>
            </label>
            <label>
              Data
              <input v-model="form.date" type="date" required :disabled="saving">
            </label>
          </div>

          <label v-if="editingExam">
            Status
            <select v-model="form.status" :disabled="saving">
              <option v-for="option in STATUS_OPTIONS" :key="option.value" :value="option.value">{{ option.label }}</option>
            </select>
          </label>

          <label>
            Conteúdo
            <textarea v-model.trim="form.content" rows="3" placeholder="Conteúdos que serão cobrados" :disabled="saving"></textarea>
          </label>

          <p v-if="formError" class="exams-request-error" role="alert">{{ formError }}</p>

          <footer>
            <button class="exams-button is-secondary" type="button" :disabled="saving" @click="closeModal">Cancelar</button>
            <button class="exams-button is-primary" type="submit" :disabled="saving">
              {{ saving ? 'Salvando...' : editingExam ? 'Salvar alterações' : 'Salvar prova' }}
            </button>
          </footer>
        </form>
      </section>
    </div>

    <div v-if="examToDelete" class="exam-modal-overlay" @click.self="closeDeleteModal">
      <section ref="deleteModalRef" class="exam-modal exam-delete-modal" role="dialog" aria-modal="true" aria-labelledby="delete-exam-title" tabindex="-1">
        <header>
          <div>
            <span class="modal-kicker">Excluir prova</span>
            <h2 id="delete-exam-title">Confirmar exclusão</h2>
          </div>
          <button type="button" aria-label="Fechar" :disabled="deleting" @click="closeDeleteModal">×</button>
        </header>

        <p class="exam-delete-text">
          Tem certeza que deseja excluir a prova <strong>{{ examToDelete.title }}</strong>? Esta ação não pode ser desfeita.
        </p>

        <p v-if="requestError" class="exams-request-error" role="alert">{{ requestError }}</p>

        <footer>
          <button class="exams-button is-secondary" type="button" :disabled="deleting" @click="closeDeleteModal">Cancelar</button>
          <button class="exams-button is-danger" type="button" :disabled="deleting" @click="confirmDeleteExam">
            {{ deleting ? 'Excluindo...' : 'Excluir' }}
          </button>
        </footer>
      </section>
    </div>

    <div v-if="toast.message" class="exams-toast" :class="`is-${toast.type}`" role="status">
      <span>{{ toast.message }}</span>
      <button type="button" aria-label="Fechar aviso" @click="closeToast">×</button>
    </div>
  </section>
</template>

<style scoped>
.exams-page {
  --purple: #6330e0;
  --purple-dark: #5c20de;
  --text: #151a2d;
  --muted: #73798e;
  --border: #ebeaf1;
  --surface: #fff;
  display: grid;
  gap: 16px;
  min-width: 0;
}

.exams-header,
.exams-heading,
.exams-header-actions {
  align-items: center;
  display: flex;
}

.exams-header {
  justify-content: space-between;
  margin-bottom: 2px;
}

.exams-heading { gap: 14px; }
.exams-title-icon {
  align-items: center;
  background: #f0ebff;
  border-radius: 11px;
  color: var(--purple);
  display: flex;
  flex: 0 0 46px;
  height: 46px;
  justify-content: center;
}
.exams-title-icon svg { fill: none; height: 25px; stroke: currentColor; stroke-linecap: round; stroke-linejoin: round; stroke-width: 1.7; width: 25px; }
.exams-heading h1 { color: #13182a; font-size: 1.75rem; font-weight: 800; letter-spacing: -.04em; margin: 0 0 4px; }
.exams-heading p { color: #687086; font-size: .78rem; margin: 0; }

.exams-header-actions { gap: 12px; }

.exams-button {
  align-items: center;
  border: 0;
  border-radius: 7px;
  display: inline-flex;
  font-size: .74rem;
  font-weight: 750;
  gap: 7px;
  justify-content: center;
  padding: 11px 16px;
  transition: transform .15s, box-shadow .15s, background .15s;
}
.exams-button.is-primary { background: linear-gradient(100deg, #5c20de, #741dff); box-shadow: 0 8px 19px rgba(102, 36, 225, .2); color: #fff; }
.exams-button.is-primary:hover { box-shadow: 0 11px 24px rgba(102, 36, 225, .28); transform: translateY(-1px); }
.exams-button.is-primary:disabled { cursor: not-allowed; opacity: .55; transform: none; box-shadow: none; }
.exams-button.is-secondary { background: #f3f1f8; color: #4c4560; }
.exams-button.is-danger { background: #e0433c; color: #fff; }
.exams-button.is-danger:hover { background: #c7362f; }

.exams-request-error {
  background: #fff1f0;
  border: 1px solid #f1d3d0;
  border-radius: 8px;
  color: #ad3834;
  font-size: .68rem;
  margin: 0;
  padding: 11px 14px;
}

.exams-summary-grid { display: grid; gap: 14px; grid-template-columns: repeat(4, minmax(0, 1fr)); }
.exams-summary-card {
  align-items: center;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 12px;
  box-shadow: 0 5px 16px rgba(30, 36, 65, .035);
  display: flex;
  gap: 13px;
  min-height: 112px;
  padding: 17px;
}
.summary-icon {
  align-items: center;
  background: #f1edff;
  border-radius: 50%;
  color: #6739e7;
  display: flex;
  flex: 0 0 48px;
  height: 48px;
  justify-content: center;
}
.summary-icon svg { fill: none; height: 24px; stroke: currentColor; stroke-linecap: round; stroke-linejoin: round; stroke-width: 1.8; width: 24px; }
.summary-icon svg .hourglass-sand { fill: currentColor; stroke: none; }
.exams-summary-card p { color: #596078; font-size: .67rem; margin: 0; }
.exams-summary-card strong { color: #171c30; display: block; font-size: 1.28rem; line-height: 1; margin: 7px 0 5px; }
.exams-summary-card small { color: #858b9e; display: block; font-size: .61rem; }
.exams-summary-card.is-green .summary-icon { background: #e8f8ef; color: #2daf68; }
.exams-summary-card.is-orange .summary-icon { background: #fff0e2; color: #ee831e; }
.exams-summary-card.is-blue .summary-icon { background: #eaf2ff; color: #347bd8; }

.exams-layout { display: grid; gap: 16px; grid-template-columns: minmax(0, 1fr) 292px; align-items: start; }
.exams-main-column { min-width: 0; overflow: hidden; }
.exams-panel,
.upcoming-card,
.mini-calendar {
  background: #fff;
  border: 1px solid var(--border);
  border-radius: 12px;
  box-shadow: 0 5px 16px rgba(30, 36, 65, .035);
}

.exams-filters {
  align-items: start;
  border-bottom: 1px solid #f0eff4;
  display: grid;
  gap: 14px;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  min-width: 0;
  padding: 16px;
}
.exams-filters label {
  color: #596078;
  display: grid;
  font-size: .61rem;
  font-weight: 700;
  gap: 6px;
  min-width: 0;
  width: 100%;
}
.exams-filters label > span {
  height: 15px;
  line-height: 15px;
}
.exams-search { align-self: end; height: 32px; min-width: 0; position: relative; }
.exams-search svg { color: #8b91a1; fill: none; height: 17px; left: 11px; position: absolute; stroke: currentColor; stroke-linecap: round; stroke-width: 1.7; top: 50%; transform: translateY(-50%); width: 17px; }
.exams-search input {
  background: #fff;
  border: 1px solid #dedfe5;
  border-radius: 6px;
  box-shadow: 0 1px 3px rgba(32, 37, 56, .06);
  box-sizing: border-box;
  height: 32px;
  padding-left: 34px;
  width: 100%;
}
.exams-filters select {
  appearance: auto;
  background: #fff;
  border: 1px solid #dedfe5;
  border-radius: 6px;
  box-shadow: 0 1px 3px rgba(32, 37, 56, .06);
  box-sizing: border-box;
  height: 32px;
  min-height: 32px;
  padding: 0 10px;
}
.exams-filters select:focus,
.exams-search input:focus { border-color: #8b6cf1; box-shadow: 0 0 0 3px rgba(99, 48, 224, .08); }

.exams-state-card {
  align-items: center;
  display: flex;
  flex-direction: column;
  gap: 6px;
  justify-content: center;
  min-height: 280px;
  padding: 35px;
  text-align: center;
}
.exams-state-card span { font-size: 2rem; }
.exams-state-card h2 { color: #202538; font-size: 1rem; font-weight: 800; margin: 10px 0 4px; }
.exams-state-card p { color: #777e91; font-size: .7rem; line-height: 1.5; margin: 0 0 14px; max-width: 380px; }
.exams-loader {
  animation: exams-spin .8s linear infinite;
  border: 3px solid #e7e0fb;
  border-radius: 50%;
  border-top-color: #6b37df;
  height: 34px;
  width: 34px;
}
@keyframes exams-spin { to { transform: rotate(360deg); } }

.exams-table-wrap { overflow-x: auto; overflow-y: visible; }
.exams-table { border-collapse: collapse; min-width: 870px; width: 100%; }
.exams-table th {
  background: #fbfbfd;
  color: #6d7386;
  font-size: .59rem;
  font-weight: 750;
  padding: 13px 11px;
  text-align: left;
  white-space: nowrap;
}
.exams-table td { border-top: 1px solid #f0eff4; color: #454b60; font-size: .65rem; padding: 13px 11px; vertical-align: middle; }
.exams-table tbody tr:hover { background: #fcfbff; }
.exam-name { align-items: center; display: flex; gap: 10px; min-width: 190px; }
.exam-row-icon {
  align-items: center;
  background: #f1edff;
  border-radius: 9px;
  color: #6330e0;
  display: flex;
  flex: 0 0 34px;
  height: 34px;
  justify-content: center;
}
.exam-row-icon svg { fill: none; height: 18px; stroke: currentColor; stroke-linecap: round; stroke-linejoin: round; stroke-width: 1.7; width: 18px; }
.exam-row-icon.is-green { background: #e8f8ef; color: #2daf68; }
.exam-row-icon.is-orange { background: #fff0e2; color: #ee831e; }
.exam-row-icon.is-blue { background: #eaf2ff; color: #347bd8; }
.exam-name strong,
.discipline-name { color: #202538; display: block; font-size: .68rem; font-weight: 750; }
.exam-name small,
.exams-table td > small,
.exams-table td small { color: #858b9e; display: block; font-size: .57rem; margin-top: 3px; }
.content-cell { max-width: 180px; line-height: 1.35; }
.status-pill {
  align-items: center;
  border-radius: 999px;
  display: inline-flex;
  font-size: .57rem;
  font-weight: 750;
  gap: 5px;
  padding: 5px 9px;
  white-space: nowrap;
}
.status-pill { background: #f1edff; color: #6330e0; }
.status-pill.is-completed { background: #e8f8ef; color: #20945a; }
.status-pill.is-overdue { background: #fdeceb; color: #c0392b; }
.status-pill svg { fill: none; height: 13px; stroke: currentColor; stroke-linecap: round; stroke-linejoin: round; stroke-width: 1.8; width: 13px; }
.row-actions { align-items: center; display: flex; gap: 3px; }
.row-actions button { background: transparent; border: 0; color: #7d8394; height: 30px; padding: 5px; width: 30px; }
.row-actions button:hover { background: #f2efff; border-radius: 6px; color: #6330e0; }
.row-actions svg { fill: none; height: 17px; stroke: currentColor; stroke-linecap: round; stroke-linejoin: round; stroke-width: 1.6; width: 17px; }
.row-action-menu-wrap { position: relative; }
.row-action-menu {
  background: #fff;
  border: 1px solid #e4e2ec;
  border-radius: 8px;
  box-shadow: 0 10px 28px rgba(20, 24, 48, .12);
  min-width: 176px;
  padding: 5px;
  position: absolute;
  right: 0;
  top: 34px;
  z-index: 20;
}
.row-action-menu button {
  align-items: center;
  border-radius: 6px;
  color: #42485b;
  display: flex;
  font-size: .62rem;
  height: auto;
  justify-content: flex-start;
  padding: 8px 9px;
  width: 100%;
}
.row-action-menu button:hover { background: #f6f4fb; color: #6330e0; }
.row-action-menu button.is-danger { color: #c54848; }
.row-action-menu button.is-danger:hover { background: #fff1f1; color: #b83e3e; }

.exams-table-footer { align-items: center; display: flex; justify-content: space-between; padding: 13px 16px; }
.exams-table-footer p { color: #747a8d; font-size: .61rem; margin: 0; }
.exams-table-footer nav { display: flex; gap: 4px; }
.exams-table-footer button {
  background: #fff;
  border: 1px solid #dedee7;
  border-radius: 6px;
  color: #535a6e;
  font-size: .65rem;
  height: 30px;
  min-width: 30px;
}
.exams-table-footer button.active { background: #6330e0; border-color: #6330e0; color: #fff; }
.exams-table-footer button:disabled { cursor: not-allowed; opacity: .4; }

.exams-side-column { display: grid; gap: 16px; }
.upcoming-card { padding: 16px; }
.upcoming-card header,
.mini-calendar header { align-items: center; display: flex; justify-content: space-between; }
.upcoming-card header { margin-bottom: 12px; }
.upcoming-card header strong { color: #202538; font-size: .75rem; }
.upcoming-card header button { background: none; border: 0; color: #6330e0; font-size: .61rem; font-weight: 750; }
.upcoming-empty { color: #858b9e; font-size: .63rem; padding: 10px 0; }
.upcoming-item { align-items: center; background: transparent; border: 0; border-top: 1px solid #f0eff4; display: grid; gap: 9px; grid-template-columns: 31px 1fr auto; padding: 11px 0; text-align: left; width: 100%; }
.upcoming-number { align-items: center; border-radius: 7px; display: flex; font-size: .66rem; font-weight: 800; height: 30px; justify-content: center; width: 30px; }
.upcoming-number.is-purple { background: #f0ebff; color: #6330e0; }
.upcoming-number.is-green { background: #e8f8ef; color: #2daf68; }
.upcoming-number.is-orange { background: #fff0e2; color: #ee831e; }
.upcoming-number.is-blue { background: #eaf2ff; color: #347bd8; }
.upcoming-details strong { color: #252a3c; display: block; font-size: .64rem; }
.upcoming-details small,
.upcoming-date small { color: #858b9e; display: block; font-size: .56rem; margin-top: 3px; }
.upcoming-date { text-align: right; }
.upcoming-date strong { color: #252a3c; display: block; font-size: .61rem; }

.mini-calendar { padding: 16px; }
.mini-calendar header strong { color: #252a3c; font-size: .76rem; }
.mini-calendar header button { background: none; border: 0; color: #747a8d; font-size: 1.35rem; line-height: 1; padding: 3px 6px; }
.calendar-weekdays,
.calendar-grid { display: grid; grid-template-columns: repeat(7, 1fr); text-align: center; }
.calendar-weekdays { color: #7f8596; font-size: .55rem; margin: 17px 0 8px; }
.calendar-grid { gap: 5px 2px; }
.calendar-day { align-items: center; border-radius: 50%; color: #52586c; display: flex; font-size: .58rem; height: 27px; justify-content: center; margin: auto; width: 27px; }
.calendar-day.today { background: #6330e0; color: #fff; font-weight: 800; }
.calendar-day.upcoming { background: #e8f8ef; color: #20945a; font-weight: 750; }
.calendar-day.overdue { background: #fdeceb; color: #c0392b; font-weight: 750; }
.calendar-day.completed { background: #eaf2ff; color: #347bd8; font-weight: 750; }
.calendar-legend { border-top: 1px solid #f0eff4; display: grid; gap: 7px; margin-top: 14px; padding-top: 13px; }
.calendar-legend span { align-items: center; color: #777d90; display: flex; font-size: .54rem; gap: 6px; }
.legend-dot { border-radius: 50%; display: inline-block; height: 6px; width: 6px; }
.legend-dot.is-today { background: #6330e0; }
.legend-dot.is-upcoming { background: #2daf68; }
.legend-dot.is-overdue { background: #c0392b; }
.legend-dot.is-completed { background: #347bd8; }

.exams-tip { align-items: center; background: #f2efff; border-radius: 9px; display: flex; gap: 12px; margin-top: 15px; padding: 12px 16px; }
.tip-icon { align-items: center; background: #e5dcff; border-radius: 50%; display: flex; flex: 0 0 34px; height: 34px; justify-content: center; }
.exams-tip div { min-width: 0; }
.exams-tip strong { color: #30364a; font-size: .66rem; }
.exams-tip p { color: #6c7287; font-size: .59rem; margin: 3px 0 0; }
.exams-tip button { background: #fff; border: 1px solid #ddd7ee; border-radius: 6px; color: #6330e0; font-size: .59rem; font-weight: 750; margin-left: auto; padding: 8px 11px; white-space: nowrap; }

.exam-modal-overlay { align-items: center; background: rgba(8, 13, 27, .58); display: flex; inset: 0; justify-content: center; padding: 20px; position: fixed; z-index: 100; }
.exam-modal { background: #fff; border-radius: 14px; box-shadow: 0 24px 70px rgba(10, 15, 35, .25); max-width: 620px; padding: 22px; width: 100%; }
.exam-modal > header { align-items: flex-start; display: flex; justify-content: space-between; margin-bottom: 18px; }
.modal-kicker { color: #6330e0; font-size: .6rem; font-weight: 800; text-transform: uppercase; }
.exam-modal h2 { color: #181d30; font-size: 1.2rem; margin: 4px 0 0; }
.exam-modal > header button { background: #f4f2f8; border: 0; border-radius: 7px; color: #646a7b; font-size: 1.3rem; height: 32px; width: 32px; }
.exam-modal form { display: grid; gap: 13px; }
.exam-modal label { color: #555b6e; display: grid; font-size: .65rem; font-weight: 700; gap: 6px; }
.exam-modal input,
.exam-modal select,
.exam-modal textarea { border: 1px solid #dedfe7; border-radius: 7px; color: #272c40; font: inherit; font-size: .72rem; outline: none; padding: 10px 11px; }
.exam-modal textarea { resize: vertical; }
.exam-modal input:disabled,
.exam-modal select:disabled,
.exam-modal textarea:disabled { background: #f6f5f9; color: #8e93a4; cursor: not-allowed; }
.field-hint { color: #858b9e; font-size: .58rem; font-weight: 500; margin-top: -1px; }
.exam-modal input:focus,
.exam-modal select:focus,
.exam-modal textarea:focus { border-color: #8b6cf1; box-shadow: 0 0 0 3px rgba(99, 48, 224, .08); }
.modal-grid { display: grid; gap: 12px; grid-template-columns: repeat(2, 1fr); }
.exam-modal footer { display: flex; justify-content: flex-end; gap: 8px; margin-top: 4px; }
.exam-details-grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}
.exam-details-grid > div {
  background: #faf9fd;
  border: 1px solid #eeeaf5;
  border-radius: 8px;
  padding: 11px 12px;
}
.exam-details-grid span {
  color: #858b9e;
  display: block;
  font-size: .58rem;
  margin-bottom: 5px;
}
.exam-details-grid strong {
  color: #292e41;
  display: block;
  font-size: .68rem;
  line-height: 1.35;
}
.exam-details-full { grid-column: 1 / -1; }
.exam-delete-text { color: #454b60; font-size: .72rem; line-height: 1.55; margin: 0; }

.exams-toast {
  align-items: center;
  background: #202538;
  border-radius: 9px;
  bottom: 22px;
  box-shadow: 0 16px 34px rgba(10, 15, 35, .25);
  color: #fff;
  display: flex;
  font-size: .68rem;
  gap: 14px;
  justify-content: space-between;
  padding: 12px 16px;
  position: fixed;
  right: 22px;
  z-index: 200;
}
.exams-toast.is-error { background: #c0392b; }
.exams-toast button { background: transparent; border: 0; color: inherit; font-size: 1rem; }

.sr-only { height: 1px; margin: -1px; overflow: hidden; position: absolute; width: 1px; clip: rect(0,0,0,0); }
.exams-page button:focus-visible,
.exams-page a:focus-visible,
.exams-page input:focus-visible,
.exams-page select:focus-visible,
.exams-page textarea:focus-visible { outline: 2px solid #6330e0; outline-offset: 2px; }
@media (max-width: 1050px) {
  .exams-layout { grid-template-columns: 1fr; }
  .exams-side-column { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media (max-width: 980px) {
  .exams-summary-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .exams-filters { grid-template-columns: 1fr 1fr; }
}
@media (max-width: 650px) {
  .exams-header { align-items: flex-start; gap: 12px; }
  .exams-header-actions { flex-shrink: 0; }
  .exams-summary-grid { grid-template-columns: 1fr; }
  .exams-filters { grid-template-columns: 1fr; }
  .exams-side-column { grid-template-columns: 1fr; }
  .exam-details-grid { grid-template-columns: 1fr; }
  .exam-details-full { grid-column: auto; }
  .exams-tip { align-items: flex-start; flex-wrap: wrap; }
  .exams-tip button { margin-left: 46px; }
  .modal-grid { grid-template-columns: 1fr; }
}
</style>
