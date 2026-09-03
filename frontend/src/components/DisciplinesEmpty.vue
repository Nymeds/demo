<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import AppToast from './AppToast.vue'
import DisciplineModal from './DisciplineModal.vue'
import DeleteDisciplineModal from './DeleteDisciplineModal.vue'

const props = defineProps({
  accessToken: { type: String, required: true },
})

const activeFilter = ref('all')
const viewMode = ref('list')
const searchTerm = ref('')
const sortOrder = ref('nameAsc')
const showAddModal = ref(false)
const editingDiscipline = ref(null)
const disciplineToDelete = ref(null)
const saveFeedback = ref('')
const requestError = ref('')
const loading = ref(true)
const dashboardId = ref('')
const disciplines = ref([])
const toast = ref({ message: '', type: 'success' })
let toastTimer

const filters = [
  { value: 'all', label: 'Todas' },
  { value: 'active', label: 'Em andamento' },
  { value: 'finished', label: 'Concluídas' },
  { value: 'locked', label: 'Trancadas' },
]

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

function openAddModal() {
  saveFeedback.value = ''
  editingDiscipline.value = null
  showAddModal.value = true
}

function openEditModal(discipline) {
  saveFeedback.value = ''
  editingDiscipline.value = discipline
  showAddModal.value = true
}

function closeAddModal() {
  showAddModal.value = false
  editingDiscipline.value = null
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
    const fieldErrors = data.errors && typeof data.errors === 'object'
      ? Object.values(data.errors).filter(Boolean).join(' ')
      : ''

    throw new Error(fieldErrors || data.detail || data.message || 'Não foi possível concluir a solicitação.')
  }

  return data
}

function normalizeDiscipline(discipline) {
  return {
    ...discipline,
    color: discipline.color || '#6432df',
    schedules: discipline.schedules.map(schedule => ({
      ...schedule,
      startTime: schedule.startTime.slice(0, 5),
      endTime: schedule.endTime.slice(0, 5),
    })),
  }
}

async function loadDisciplines() {
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
    disciplines.value = savedDisciplines.map(normalizeDiscipline)
  } catch (error) {
    requestError.value = error.message || 'Não foi possível carregar as disciplinas.'
    showToast(requestError.value, 'error')
  } finally {
    loading.value = false
  }
}

async function saveDiscipline(formData) {
  requestError.value = ''

  try {
    const disciplineId = editingDiscipline.value?.id
    const path = disciplineId
      ? `/api/v1/dashboards/${dashboardId.value}/disciplines/${disciplineId}`
      : `/api/v1/dashboards/${dashboardId.value}/disciplines`

    const savedDiscipline = normalizeDiscipline(await apiRequest(path, {
      method: disciplineId ? 'PUT' : 'POST',
      body: JSON.stringify(formData),
    }))

    if (disciplineId) {
      const index = disciplines.value.findIndex(item => item.id === disciplineId)
      disciplines.value[index] = savedDiscipline
      saveFeedback.value = 'Disciplina atualizada com sucesso.'
    } else {
      disciplines.value.push(savedDiscipline)
      saveFeedback.value = 'Disciplina adicionada com sucesso.'
    }

    showToast(saveFeedback.value)
    closeAddModal()
  } catch (error) {
    requestError.value = error.message || 'Não foi possível salvar a disciplina.'
    showToast(requestError.value, 'error')
  }
}

function askToDeleteDiscipline(discipline) {
  disciplineToDelete.value = discipline
}

function closeDeleteModal() {
  disciplineToDelete.value = null
}

async function confirmDeleteDiscipline() {
  if (!disciplineToDelete.value) return

  requestError.value = ''

  try {
    const disciplineId = disciplineToDelete.value.id
    await apiRequest(`/api/v1/dashboards/${dashboardId.value}/disciplines/${disciplineId}`, {
      method: 'DELETE',
    })
    disciplines.value = disciplines.value.filter(item => item.id !== disciplineId)
    saveFeedback.value = 'Disciplina excluída com sucesso.'
    showToast(saveFeedback.value)
    closeDeleteModal()
  } catch (error) {
    requestError.value = error.message || 'Não foi possível excluir a disciplina.'
    showToast(requestError.value, 'error')
  }
}

onMounted(loadDisciplines)
onBeforeUnmount(() => clearTimeout(toastTimer))

function clearFilters() {
  searchTerm.value = ''
  activeFilter.value = 'all'
  sortOrder.value = 'nameAsc'
}

const filteredDisciplines = computed(() => {
  const search = searchTerm.value.trim().toLocaleLowerCase('pt-BR')

  return disciplines.value
    .filter(discipline => {
      const matchesSearch = !search
        || discipline.name.toLocaleLowerCase('pt-BR').includes(search)
        || discipline.professorName.toLocaleLowerCase('pt-BR').includes(search)

      const matchesFilter = activeFilter.value === 'all'
        || (activeFilter.value === 'active' && discipline.status === 'IN_PROGRESS')
        || (activeFilter.value === 'finished' && discipline.status === 'APPROVED')
        || (activeFilter.value === 'locked' && discipline.status === 'LOCKED')

      return matchesSearch && matchesFilter
    })
    .sort((first, second) => {
      if (sortOrder.value === 'nameDesc') {
        return second.name.localeCompare(first.name, 'pt-BR')
      }

      if (sortOrder.value === 'newest') {
        return new Date(second.createdAt ?? 0).getTime() - new Date(first.createdAt ?? 0).getTime()
      }

      return first.name.localeCompare(second.name, 'pt-BR')
    })
})

const generalAverage = computed(() => {
  const values = disciplines.value
    .map(discipline => discipline.average)
    .filter(value => typeof value === 'number')

  return values.length
    ? values.reduce((total, value) => total + value, 0) / values.length
    : null
})

const averageAttendance = computed(() => {
  const values = disciplines.value
    .map(discipline => discipline.attendancePercentage)
    .filter(value => typeof value === 'number')

  return values.length
    ? values.reduce((total, value) => total + value, 0) / values.length
    : null
})

const dayLabels = {
  MONDAY: 'Seg',
  TUESDAY: 'Ter',
  WEDNESDAY: 'Qua',
  THURSDAY: 'Qui',
  FRIDAY: 'Sex',
  SATURDAY: 'Sáb',
  SUNDAY: 'Dom',
}

function formatAverage(value) {
  return typeof value === 'number'
    ? value.toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
    : '—'
}

function disciplineColor(discipline) {
  return discipline.color || '#6432df'
}

function statusDetails(status) {
  const statuses = {
    IN_PROGRESS: { label: 'Em andamento', className: 'is-progress' },
    APPROVED: { label: 'Concluída', className: 'is-success' },
    LOCKED: { label: 'Trancada', className: 'is-neutral' },
    FAILED_BY_GRADE: { label: 'Atenção', className: 'is-warning' },
    FAILED_BY_ATTENDANCE: { label: 'Atenção', className: 'is-warning' },
    FAILED_BY_GRADE_AND_ATTENDANCE: { label: 'Atenção', className: 'is-warning' },
    NO_DATA: { label: 'Não iniciada', className: 'is-neutral' },
  }

  return statuses[status] ?? statuses.NO_DATA
}
</script>

<template>
  <section class="disciplines-page" aria-labelledby="disciplines-title">
    <header class="disciplines-header">
      <div class="disciplines-heading">
        <span class="disciplines-heading-icon" aria-hidden="true">
          <svg viewBox="0 0 24 24">
            <path d="M4 5.5A3.5 3.5 0 0 1 7.5 2H11v17H7.5A3.5 3.5 0 0 0 4 22V5.5Z" />
            <path d="M20 5.5A3.5 3.5 0 0 0 16.5 2H13v17h3.5A3.5 3.5 0 0 1 20 22V5.5Z" />
          </svg>
        </span>
        <div>
          <h1 id="disciplines-title">Disciplinas</h1>
          <p>Gerencie suas disciplinas e acompanhe seu desempenho.</p>
        </div>
      </div>

      <div class="disciplines-actions">
        <label class="disciplines-search">
          <span class="sr-only">Buscar disciplina</span>
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <circle cx="11" cy="11" r="7" />
            <path d="m20 20-4-4" />
          </svg>
          <input v-model="searchTerm" type="search" placeholder="Buscar disciplina...">
        </label>

        <button class="disciplines-add-button" type="button" :disabled="loading || !dashboardId" @click="openAddModal">
          <span aria-hidden="true">＋</span>
          Nova disciplina
        </button>
      </div>
    </header>

    <div class="disciplines-summary-grid">
      <article class="disciplines-total-card is-purple">
        <span aria-hidden="true">
          <svg viewBox="0 0 24 24"><path d="M4 5.5A3.5 3.5 0 0 1 7.5 2H11v17H7.5A3.5 3.5 0 0 0 4 22V5.5Z" /><path d="M20 5.5A3.5 3.5 0 0 0 16.5 2H13v17h3.5A3.5 3.5 0 0 1 20 22V5.5Z" /></svg>
        </span>
        <div>
          <p>Total de disciplinas</p>
          <strong>{{ disciplines.length }}</strong>
          <small>Nesta sessão</small>
        </div>
      </article>

      <article class="disciplines-total-card is-green">
        <span aria-hidden="true">
          <svg viewBox="0 0 24 24"><path d="M4 19v-5m5 5V9m5 10v-7m5 7V5" /><path d="m4 10 5-4 5 3 6-6" /></svg>
        </span>
        <div>
          <p>Média geral</p>
          <strong>{{ formatAverage(generalAverage) }}</strong>
          <small>{{ generalAverage === null ? 'Aguardando notas' : 'Todas as disciplinas' }}</small>
        </div>
      </article>

      <article class="disciplines-total-card is-orange">
        <span aria-hidden="true">
          <svg viewBox="0 0 24 24"><circle cx="12" cy="8" r="4" /><path d="M4 21a8 8 0 0 1 16 0" /></svg>
        </span>
        <div>
          <p>Frequência média</p>
          <strong>{{ averageAttendance === null ? '—' : `${Math.round(averageAttendance)}%` }}</strong>
          <small>{{ averageAttendance === null ? 'Aguardando frequência' : 'Todas as disciplinas' }}</small>
        </div>
      </article>
    </div>

    <div class="disciplines-toolbar">
      <div class="disciplines-filters" aria-label="Filtrar disciplinas">
        <button
          v-for="filter in filters"
          :key="filter.value"
          type="button"
          :class="{ active: activeFilter === filter.value }"
          @click="activeFilter = filter.value"
        >
          {{ filter.label }}
        </button>
      </div>

      <div class="disciplines-view-options">
        <label class="disciplines-sort">
          <span>Ordenar por:</span>
          <select v-model="sortOrder" aria-label="Ordenar disciplinas">
            <option value="nameAsc">Nome (A-Z)</option>
            <option value="nameDesc">Nome (Z-A)</option>
            <option value="newest">Mais recentes</option>
          </select>
        </label>

        <button class="clear-filters" type="button" @click="clearFilters">Limpar filtros</button>

        <div class="disciplines-view-buttons" aria-label="Modo de visualização">
          <button type="button" :class="{ active: viewMode === 'list' }" aria-label="Visualização em lista" @click="viewMode = 'list'">
            <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M9 6h11M9 12h11M9 18h11" /><circle cx="4" cy="6" r="1" /><circle cx="4" cy="12" r="1" /><circle cx="4" cy="18" r="1" /></svg>
          </button>
          <button type="button" :class="{ active: viewMode === 'grid' }" aria-label="Visualização em grade" @click="viewMode = 'grid'">
            <svg viewBox="0 0 24 24" aria-hidden="true"><rect x="4" y="4" width="6" height="6" /><rect x="14" y="4" width="6" height="6" /><rect x="4" y="14" width="6" height="6" /><rect x="14" y="14" width="6" height="6" /></svg>
          </button>
        </div>
      </div>
    </div>

    <article v-if="loading" class="disciplines-loading-card" aria-live="polite">
      <span class="loading-spinner" aria-hidden="true"></span>
      <p>Carregando disciplinas...</p>
    </article>

    <article v-else-if="disciplines.length === 0" class="disciplines-empty-card" aria-labelledby="disciplines-empty-title">
      <svg class="disciplines-empty-illustration" viewBox="0 0 360 220" role="img" aria-label="Livros, caderno aberto e uma planta">
        <defs>
          <linearGradient id="book-cover" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0" stop-color="#a98df0" />
            <stop offset="1" stop-color="#7650d4" />
          </linearGradient>
        </defs>
        <circle cx="190" cy="105" r="86" fill="#f4f0ff" />
        <path d="m289 42 4 10 10 4-10 4-4 10-4-10-10-4 10-4 4-10Z" fill="#e5dcff" />
        <path d="m82 54 3 7 7 3-7 3-3 7-3-7-7-3 7-3 3-7Z" fill="#ede7ff" />

        <rect x="171" y="64" width="101" height="20" rx="5" fill="url(#book-cover)" />
        <rect x="164" y="84" width="112" height="20" rx="5" fill="#8f70df" />
        <rect x="173" y="104" width="105" height="20" rx="5" fill="#b39bea" />
        <path d="M184 69h69M177 89h80M185 109h74" stroke="#dcd1fa" stroke-width="4" stroke-linecap="round" />

        <path d="M122 178h43l-5-49h-33l-5 49Z" fill="#bdaaf0" />
        <path d="M143 133c-5-25 2-43 19-55 5 24-2 42-19 55Z" fill="#8463dc" />
        <path d="M141 137c-22-11-34-26-34-44 22 9 34 24 34 44Z" fill="#987be3" />
        <path d="M146 140c18-18 36-24 54-17-15 20-33 26-54 17Z" fill="#7655d2" />

        <path d="M158 140c22-13 43-13 63 0v52c-20-14-41-14-63 0v-52Z" fill="#fff" stroke="#8d6cdd" stroke-width="4" stroke-linejoin="round" />
        <path d="M221 140c22-13 43-13 63 0v52c-20-14-41-14-63 0v-52Z" fill="#fff" stroke="#8d6cdd" stroke-width="4" stroke-linejoin="round" />
        <path d="M221 140v52M170 153c13-5 25-5 38 0m-38 12c13-5 25-5 38 0m26-12c13-5 25-5 38 0m-38 12c13-5 25-5 38 0" fill="none" stroke="#d5c8f5" stroke-width="3" stroke-linecap="round" />
      </svg>

      <h2 id="disciplines-empty-title">Nenhuma disciplina cadastrada ainda</h2>
      <p>Adicione sua primeira disciplina para começar a organizar seus estudos e acompanhar seu desempenho.</p>

      <button class="disciplines-empty-button" type="button" @click="openAddModal">
        <span aria-hidden="true">＋</span>
        Adicionar disciplina
      </button>
    </article>

    <section v-else-if="viewMode === 'list'" class="disciplines-table-card" aria-label="Lista de disciplinas">
      <div class="disciplines-table-scroll">
        <table>
          <colgroup>
            <col class="column-discipline">
            <col class="column-professor">
            <col class="column-schedules">
            <col class="column-average">
            <col class="column-attendance">
            <col class="column-status">
            <col class="column-actions">
          </colgroup>
          <thead>
            <tr>
              <th>Disciplina</th>
              <th>Professor</th>
              <th>Horários</th>
              <th>Média</th>
              <th>Frequência</th>
              <th>Situação</th>
              <th>Ações</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="discipline in filteredDisciplines" :key="discipline.id">
              <td>
                <div class="discipline-name-cell">
                  <span class="discipline-color" :style="{ backgroundColor: `${disciplineColor(discipline)}1f`, color: disciplineColor(discipline) }" aria-hidden="true">
                    <svg viewBox="0 0 24 24"><path d="M4 5.5A3.5 3.5 0 0 1 7.5 2H11v17H7.5A3.5 3.5 0 0 0 4 22V5.5Z" /><path d="M20 5.5A3.5 3.5 0 0 0 16.5 2H13v17h3.5A3.5 3.5 0 0 1 20 22V5.5Z" /></svg>
                  </span>
                  <span class="discipline-name">{{ discipline.name }}</span>
                </div>
              </td>
              <td>{{ discipline.professorName || 'Não informado' }}</td>
              <td>
                <div class="discipline-schedules">
                  <span v-for="(schedule, index) in discipline.schedules" :key="index">
                    <svg viewBox="0 0 24 24" aria-hidden="true"><rect x="3" y="5" width="18" height="16" rx="2" /><path d="M7 3v4m10-4v4M3 10h18" /></svg>
                    {{ dayLabels[schedule.dayOfWeek] }} {{ schedule.startTime }}–{{ schedule.endTime }}
                  </span>
                </div>
              </td>
              <td :class="['discipline-average', { 'is-low': typeof discipline.average === 'number' && discipline.average < 7 }]">
                {{ formatAverage(discipline.average) }}
              </td>
              <td>
                <div class="discipline-attendance">
                  <span>{{ typeof discipline.attendancePercentage === 'number' ? `${Math.round(discipline.attendancePercentage)}%` : '—' }}</span>
                  <span class="attendance-track" aria-hidden="true">
                    <span v-if="typeof discipline.attendancePercentage === 'number'" :style="{ width: `${discipline.attendancePercentage}%` }"></span>
                  </span>
                </div>
              </td>
              <td>
                <span :class="['discipline-status', statusDetails(discipline.status).className]">
                  {{ statusDetails(discipline.status).label }}
                </span>
              </td>
              <td>
                <div class="discipline-actions-cell">
                  <button type="button" aria-label="Editar disciplina" title="Editar" @click="openEditModal(discipline)">
                    <svg viewBox="0 0 24 24" aria-hidden="true"><path d="m4 20 4-1 11-11-3-3L5 16l-1 4Z" /><path d="m14 7 3 3" /></svg>
                  </button>
                  <button class="delete-action" type="button" aria-label="Excluir disciplina" title="Excluir" @click="askToDeleteDiscipline(discipline)">
                    <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 7h16M9 7V4h6v3m3 0-1 13H7L6 7m4 4v5m4-5v5" /></svg>
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <p v-if="filteredDisciplines.length === 0" class="disciplines-no-results">Nenhuma disciplina encontrada com esses filtros.</p>
    </section>

    <section v-else class="disciplines-card-grid" aria-label="Disciplinas em grade">
      <article v-for="discipline in filteredDisciplines" :key="discipline.id" :style="{ '--card-color': disciplineColor(discipline) }">
        <header>
          <span class="discipline-color" :style="{ backgroundColor: `${disciplineColor(discipline)}1f`, color: disciplineColor(discipline) }" aria-hidden="true">
            <svg viewBox="0 0 24 24"><path d="M4 5.5A3.5 3.5 0 0 1 7.5 2H11v17H7.5A3.5 3.5 0 0 0 4 22V5.5Z" /><path d="M20 5.5A3.5 3.5 0 0 0 16.5 2H13v17h3.5A3.5 3.5 0 0 1 20 22V5.5Z" /></svg>
          </span>
          <div><h2>{{ discipline.name }}</h2><p>{{ discipline.professorName || 'Professor não informado' }}</p></div>
        </header>
        <div class="grid-card-schedules">
          <span v-for="(schedule, index) in discipline.schedules" :key="index">{{ dayLabels[schedule.dayOfWeek] }} {{ schedule.startTime }}–{{ schedule.endTime }}</span>
        </div>
        <div class="grid-card-data">
          <span><small>Média</small><strong>{{ formatAverage(discipline.average) }}</strong></span>
          <span><small>Frequência</small><strong>{{ typeof discipline.attendancePercentage === 'number' ? `${Math.round(discipline.attendancePercentage)}%` : '—' }}</strong></span>
        </div>
        <footer>
          <span :class="['discipline-status', statusDetails(discipline.status).className]">{{ statusDetails(discipline.status).label }}</span>
          <div class="discipline-actions-cell">
            <button type="button" aria-label="Editar disciplina" @click="openEditModal(discipline)"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="m4 20 4-1 11-11-3-3L5 16l-1 4Z" /><path d="m14 7 3 3" /></svg></button>
            <button class="delete-action" type="button" aria-label="Excluir disciplina" @click="askToDeleteDiscipline(discipline)"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 7h16M9 7V4h6v3m3 0-1 13H7L6 7m4 4v5m4-5v5" /></svg></button>
          </div>
        </footer>
      </article>

      <p v-if="filteredDisciplines.length === 0" class="disciplines-no-results">Nenhuma disciplina encontrada com esses filtros.</p>
    </section>

    <AppToast :message="toast.message" :type="toast.type" @close="closeToast" />

    <footer class="disciplines-footer">
      <p>Mostrando {{ filteredDisciplines.length }} de {{ disciplines.length }} disciplinas</p>
      <nav aria-label="Paginação das disciplinas">
        <button type="button" disabled>Anterior</button>
        <span aria-current="page">1</span>
        <button type="button" disabled>Próxima</button>
      </nav>
    </footer>

    <DisciplineModal
      v-if="showAddModal"
      :discipline="editingDiscipline"
      @close="closeAddModal"
      @save="saveDiscipline"
    />

    <DeleteDisciplineModal
      v-if="disciplineToDelete"
      :discipline-name="disciplineToDelete.name"
      @close="closeDeleteModal"
      @confirm="confirmDeleteDiscipline"
    />
  </section>
</template>

<style scoped>
.disciplines-page {
  display: grid;
  gap: 22px;
}

.disciplines-header {
  align-items: center;
  display: flex;
  justify-content: space-between;
}

.disciplines-heading,
.disciplines-actions,
.disciplines-toolbar,
.disciplines-view-options,
.disciplines-footer,
.disciplines-footer nav {
  align-items: center;
  display: flex;
}

.disciplines-heading {
  gap: 13px;
}

.disciplines-heading-icon {
  align-items: center;
  background: #f0eaff;
  border-radius: 9px;
  color: #6b37e8;
  display: flex;
  height: 43px;
  justify-content: center;
  width: 43px;
}

.disciplines-heading-icon svg,
.disciplines-total-card svg,
.disciplines-search svg,
.disciplines-view-buttons svg {
  fill: none;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 1.8;
}

.disciplines-heading-icon svg {
  height: 25px;
  width: 25px;
}

.disciplines-heading h1 {
  color: #151a2d;
  font-size: 1.65rem;
  font-weight: 800;
  letter-spacing: -.04em;
  line-height: 1.1;
  margin: 0 0 6px;
}

.disciplines-heading p {
  color: #697086;
  font-size: .78rem;
}

.disciplines-actions {
  gap: 16px;
}

.disciplines-search {
  color: #747b90;
  position: relative;
}

.disciplines-search svg {
  height: 18px;
  left: 14px;
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 18px;
}

.disciplines-search input {
  background: #fff;
  border: 1px solid #dedfe8;
  border-radius: 8px;
  color: #252a3e;
  min-width: 255px;
  outline: none;
  padding: 12px 14px 12px 42px;
}

.disciplines-search input:focus {
  border-color: #7544eb;
  box-shadow: 0 0 0 3px rgba(117, 68, 235, .12);
}

.disciplines-add-button,
.disciplines-empty-button {
  align-items: center;
  background: linear-gradient(100deg, #5d20df, #7419f5);
  border: 0;
  border-radius: 7px;
  box-shadow: 0 8px 18px rgba(101, 31, 225, .2);
  color: #fff;
  display: flex;
  font-size: .76rem;
  font-weight: 700;
  gap: 8px;
  justify-content: center;
  padding: 12px 18px;
}

.disciplines-add-button span,
.disciplines-empty-button span {
  font-size: 1.12rem;
  font-weight: 400;
  line-height: .8;
}

.disciplines-add-button:hover,
.disciplines-empty-button:hover {
  box-shadow: 0 11px 23px rgba(101, 31, 225, .28);
  transform: translateY(-1px);
}

.disciplines-add-button:disabled {
  cursor: wait;
  opacity: .6;
}

.disciplines-add-button:focus-visible,
.disciplines-empty-button:focus-visible {
  outline: 3px solid rgba(105, 54, 224, .28);
  outline-offset: 3px;
}

.disciplines-total-card {
  align-items: center;
  background: #fff;
  border: 1px solid #ebeaf1;
  border-radius: 10px;
  box-shadow: 0 5px 16px rgba(30, 36, 65, .035);
  display: flex;
  gap: 16px;
  padding: 18px;
  width: 100%;
}

.disciplines-summary-grid {
  display: grid;
  gap: 16px;
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.disciplines-total-card > span {
  align-items: center;
  background: #f0eaff;
  border-radius: 50%;
  color: #6d38e8;
  display: flex;
  height: 52px;
  justify-content: center;
  width: 52px;
}

.disciplines-total-card svg {
  height: 26px;
  width: 26px;
}

.disciplines-total-card p {
  color: #51586c;
  font-size: .7rem;
  font-weight: 650;
}

.disciplines-total-card strong {
  color: #151a2d;
  display: block;
  font-size: 1.35rem;
  line-height: 1;
  margin-top: 7px;
}

.disciplines-total-card small {
  color: #858b9e;
  display: block;
  font-size: .62rem;
  margin-top: 6px;
}

.disciplines-total-card.is-green > span {
  background: #e9f8ef;
  color: #20a95f;
}

.disciplines-total-card.is-green small {
  color: #259b5b;
}

.disciplines-total-card.is-orange > span {
  background: #fff1df;
  color: #ee8a18;
}

.disciplines-total-card.is-orange small {
  color: #d98117;
}

.disciplines-toolbar {
  gap: 20px;
  justify-content: space-between;
}

.disciplines-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.disciplines-filters button,
.disciplines-sort,
.disciplines-view-buttons button {
  background: #fff;
  border: 1px solid #e1e2e9;
  color: #34394c;
}

.disciplines-filters button {
  border-radius: 7px;
  font-size: .7rem;
  padding: 10px 17px;
}

.disciplines-filters button.active {
  border-color: #6f36e7;
  color: #6126d8;
  font-weight: 700;
}

.disciplines-view-options {
  gap: 12px;
}

.clear-filters {
  background: #fff;
  border: 1px solid #e1e2e9;
  border-radius: 7px;
  color: #535a70;
  font-size: .68rem;
  padding: 11px 13px;
}

.clear-filters:hover {
  border-color: #7650df;
  color: #6030cb;
}

.disciplines-sort {
  align-items: center;
  border-radius: 7px;
  display: flex;
  font-size: .68rem;
  gap: 8px;
  padding: 0 9px 0 14px;
}

.disciplines-sort span {
  font-weight: 700;
}

.disciplines-sort select {
  background: transparent;
  border: 0;
  color: #43495e;
  outline: none;
  padding: 10px 4px;
}

.disciplines-view-buttons {
  display: flex;
}

.disciplines-view-buttons button {
  align-items: center;
  display: flex;
  height: 40px;
  justify-content: center;
  width: 44px;
}

.disciplines-view-buttons button:first-child {
  border-radius: 7px 0 0 7px;
}

.disciplines-view-buttons button:last-child {
  border-left: 0;
  border-radius: 0 7px 7px 0;
}

.disciplines-view-buttons button.active {
  background: #f0eaff;
  color: #6932df;
}

.disciplines-view-buttons svg {
  height: 19px;
  width: 19px;
}

.disciplines-empty-card {
  align-items: center;
  background: #fff;
  border: 1px solid #e9e8ef;
  border-radius: 11px;
  box-shadow: 0 5px 16px rgba(30, 36, 65, .035);
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-height: 455px;
  padding: 35px;
  text-align: center;
}

.disciplines-loading-card {
  align-items: center;
  background: #fff;
  border: 1px solid #e9e8ef;
  border-radius: 11px;
  color: #747b8e;
  display: flex;
  flex-direction: column;
  font-size: .75rem;
  gap: 12px;
  justify-content: center;
  min-height: 300px;
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

.disciplines-empty-illustration {
  display: block;
  height: 200px;
  max-width: 360px;
  width: 100%;
}

.disciplines-empty-card h2 {
  color: #171c30;
  font-size: 1.16rem;
  font-weight: 800;
  letter-spacing: -.025em;
  margin: 5px 0 8px;
}

.disciplines-empty-card p {
  color: #73798e;
  font-size: .76rem;
  line-height: 1.55;
  max-width: 480px;
}

.disciplines-empty-button {
  margin-top: 20px;
}

.disciplines-table-card {
  background: #fff;
  border: 1px solid #e8e8ef;
  border-radius: 11px;
  box-shadow: 0 5px 16px rgba(30, 36, 65, .035);
  overflow: hidden;
}

.disciplines-table-scroll {
  overflow-x: auto;
}

.disciplines-table-card table {
  border-collapse: collapse;
  min-width: 970px;
  table-layout: fixed;
  width: 100%;
}

.column-discipline { width: 22%; }
.column-professor { width: 17%; }
.column-schedules { width: 20%; }
.column-average { width: 8%; }
.column-attendance { width: 13%; }
.column-status { width: 12%; }
.column-actions { width: 8%; }

.disciplines-table-card th,
.disciplines-table-card td {
  border-bottom: 1px solid #ececf1;
  padding: 15px 14px;
  text-align: left;
  vertical-align: middle;
}

.disciplines-table-card th {
  color: #495066;
  font-size: .66rem;
  font-weight: 750;
  white-space: nowrap;
}

.disciplines-table-card td {
  color: #555c70;
  font-size: .68rem;
}

.disciplines-table-card tbody tr:last-child td {
  border-bottom: 0;
}

.disciplines-table-card tbody tr:hover {
  background: #fbfaff;
}

.disciplines-table-card th:last-child,
.disciplines-table-card td:last-child {
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
  font-size: .7rem;
  font-weight: 400;
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

.discipline-schedules {
  display: grid;
  gap: 5px;
  min-width: 150px;
}

.discipline-schedules span {
  align-items: center;
  display: flex;
  font-size: .73rem;
  gap: 6px;
  white-space: nowrap;
}

.discipline-schedules svg {
  fill: none;
  height: 14px;
  stroke: #687087;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 1.7;
  width: 14px;
}

.discipline-average {
  color: #1a9a54 !important;
  font-size: .78rem !important;
  font-weight: 800;
}

.discipline-average.is-low {
  color: #e56827 !important;
}

.discipline-attendance {
  display: grid;
  gap: 6px;
  min-width: 82px;
}

.discipline-attendance > span:first-child {
  color: #303649;
  font-weight: 700;
}

.attendance-track {
  background: #e8e9ee;
  border-radius: 999px;
  height: 4px;
  overflow: hidden;
  width: 82px;
}

.attendance-track span {
  background: #20aa60;
  border-radius: inherit;
  display: block;
  height: 100%;
}

.discipline-status {
  border-radius: 999px;
  display: inline-block;
  font-size: .61rem;
  font-weight: 700;
  padding: 4px 9px;
  white-space: nowrap;
}

.discipline-status.is-progress {
  background: #eee9ff;
  color: #6532d8;
}

.discipline-status.is-success {
  background: #e8f7ed;
  color: #258b50;
}

.discipline-status.is-warning {
  background: #fff0df;
  color: #ce741a;
}

.discipline-status.is-neutral {
  background: #eff0f4;
  color: #646b7d;
}

.discipline-actions-cell {
  align-items: center;
  display: flex;
  gap: 7px;
  justify-content: center;
}

.discipline-actions-cell button {
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

.discipline-actions-cell button:hover {
  background: #f0ecff;
  color: #6532d8;
}

.discipline-actions-cell button.delete-action:hover {
  background: #fff0ed;
  color: #e24c39;
}

.discipline-actions-cell button.delete-action {
  color: #e13f32;
}

.discipline-actions-cell svg {
  fill: none;
  height: 20px;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 1.8;
  width: 20px;
}

.discipline-actions-cell .delete-action svg {
  height: 21px;
  width: 21px;
}

.disciplines-no-results {
  color: #747b8e;
  font-size: .72rem;
  padding: 35px 20px;
  text-align: center;
}

.disciplines-card-grid {
  display: grid;
  gap: 15px;
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.disciplines-card-grid > article {
  background: #fff;
  border: 1px solid #e8e8ef;
  border-radius: 11px;
  border-top: 3px solid var(--card-color);
  box-shadow: 0 5px 16px rgba(30, 36, 65, .035);
  padding: 18px;
}

.disciplines-card-grid article > header,
.disciplines-card-grid article > footer,
.grid-card-data {
  align-items: center;
  display: flex;
}

.disciplines-card-grid article > header {
  gap: 11px;
}

.disciplines-card-grid h2 {
  color: #252a3d;
  font-size: .78rem;
  font-weight: 400;
  margin: 0 0 4px;
}

.disciplines-card-grid header p,
.grid-card-schedules {
  color: #747b8e;
  font-size: .7rem;
}

.grid-card-schedules {
  display: grid;
  gap: 5px;
  margin: 18px 0;
}

.grid-card-data {
  border-bottom: 1px solid #ececf1;
  border-top: 1px solid #ececf1;
  gap: 28px;
  padding: 13px 0;
}

.grid-card-data span {
  display: grid;
  gap: 3px;
}

.grid-card-data small {
  color: #858b9c;
  font-size: .6rem;
}

.grid-card-data strong {
  color: #303548;
  font-size: .78rem;
}

.disciplines-card-grid article > footer {
  justify-content: space-between;
  padding-top: 13px;
}

.disciplines-save-feedback {
  background: #eff8ed;
  border-left: 3px solid #5f9855;
  border-radius: 6px;
  color: #3f6c38;
  font-size: .7rem;
  padding: 11px 13px;
}

.disciplines-request-error {
  align-items: center;
  background: #fff1ef;
  border-left: 3px solid #db493d;
  border-radius: 6px;
  color: #9b332a;
  display: flex;
  font-size: .7rem;
  justify-content: space-between;
  padding: 11px 13px;
}

.disciplines-request-error button {
  background: transparent;
  border: 0;
  color: #84271f;
  font-size: inherit;
  font-weight: 700;
  text-decoration: underline;
}

.disciplines-footer {
  color: #73798e;
  font-size: .68rem;
  justify-content: space-between;
}

.disciplines-footer nav {
  gap: 8px;
}

.disciplines-footer button,
.disciplines-footer span {
  border-radius: 7px;
  font-size: .68rem;
  padding: 9px 14px;
}

.disciplines-footer button {
  background: #fff;
  border: 1px solid #e6e7ed;
  color: #a5a9b7;
}

.disciplines-footer button:disabled {
  cursor: not-allowed;
  opacity: .65;
}

.disciplines-footer span {
  background: #641be2;
  color: #fff;
  min-width: 38px;
  text-align: center;
}

.sr-only {
  height: 1px;
  margin: -1px;
  overflow: hidden;
  padding: 0;
  position: absolute;
  width: 1px;
  clip: rect(0, 0, 0, 0);
}

@media (max-width: 980px) {
  .disciplines-header,
  .disciplines-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .disciplines-actions,
  .disciplines-view-options {
    justify-content: space-between;
  }

  .disciplines-search {
    flex: 1;
  }

  .disciplines-search input {
    min-width: 0;
    width: 100%;
  }

  .disciplines-summary-grid,
  .disciplines-card-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 620px) {
  .disciplines-actions,
  .disciplines-view-options,
  .disciplines-footer {
    align-items: stretch;
    flex-direction: column;
  }

  .disciplines-add-button,
  .disciplines-total-card {
    width: 100%;
  }

  .disciplines-sort {
    justify-content: space-between;
  }

  .disciplines-view-buttons button {
    flex: 1;
  }

  .disciplines-view-buttons button:first-child,
  .disciplines-view-buttons button:last-child {
    width: 50%;
  }

  .disciplines-empty-card {
    min-height: 410px;
    padding: 24px 18px;
  }

  .disciplines-empty-illustration {
    height: auto;
  }

  .disciplines-footer nav {
    justify-content: center;
  }
}
</style>
