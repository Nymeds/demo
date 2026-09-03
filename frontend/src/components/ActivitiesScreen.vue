<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import ActivityModal from './ActivityModal.vue'
import AppToast from './AppToast.vue'
import DeleteActivityModal from './DeleteActivityModal.vue'

const props = defineProps({
  accessToken: { type: String, required: true },
})
const emit = defineEmits(['navigate'])

const dashboardId = ref('')
const disciplines = ref([])
const activities = ref([])
const loading = ref(true)
const requestError = ref('')
const saveFeedback = ref('')

const searchTerm = ref('')
const activeFilter = ref('all')
const sortOrder = ref('dueAsc')

const showActivityModal = ref(false)
const editingActivity = ref(null)
const activityToDelete = ref(null)
const saving = ref(false)
const toast = ref({ message: '', type: 'success' })
let toastTimer

const filters = [
  { value: 'all', label: 'Todas' },
  { value: 'pending', label: 'Pendentes' },
  { value: 'progress', label: 'Em andamento' },
  { value: 'completed', label: 'Concluídas' },
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

function disciplineById(id) {
  return disciplines.value.find(discipline => discipline.id === id)
}

function normalizeActivity(activity) {
  const discipline = disciplineById(activity.disciplineId)

  return {
    ...activity,
    disciplineName: discipline?.name || 'Disciplina',
    disciplineColor: discipline?.color || '#6432df',
  }
}

async function loadActivities() {
  loading.value = true
  requestError.value = ''

  try {
    const dashboards = await apiRequest('/api/v1/dashboards')
    let dashboard = dashboards.find(item => item.status === 'ACTIVE') || dashboards[0]

    if (!dashboard) {
      dashboard = await apiRequest('/api/v1/dashboards', {
        method: 'POST',
        body: JSON.stringify({
          name: 'Organização acadêmica',
          status: 'ACTIVE',
        }),
      })
    }

    dashboardId.value = dashboard.id

    disciplines.value = await apiRequest(
      `/api/v1/dashboards/${dashboard.id}/disciplines`,
    )

    if (disciplines.value.length === 0) {
      activities.value = []
      return
    }

    const activityLists = await Promise.all(
      disciplines.value.map(async discipline => {
        const savedActivities = await apiRequest(
          `/api/v1/dashboards/${dashboard.id}/disciplines/${discipline.id}/activities`,
        )

        return savedActivities.map(activity => ({
          ...activity,
          disciplineName: discipline.name,
          disciplineColor: discipline.color || '#6432df',
        }))
      }),
    )

    const loadedActivities = activityLists.flat()
    activities.value = [...new Map(loadedActivities.map(activity => [activity.id, activity])).values()]
  } catch (error) {
    requestError.value = error.message || 'Não foi possível carregar as atividades.'
    showToast(requestError.value, 'error')
  } finally {
    loading.value = false
  }
}

function openAddModal() {
  if (disciplines.value.length === 0) {
    requestError.value = 'Cadastre uma disciplina antes de criar uma atividade.'
    showToast(requestError.value, 'error')
    return
  }

  saveFeedback.value = ''
  requestError.value = ''
  editingActivity.value = null
  showActivityModal.value = true
}

function openEditModal(activity) {
  saveFeedback.value = ''
  editingActivity.value = activity
  showActivityModal.value = true
}

function closeActivityModal() {
  showActivityModal.value = false
  editingActivity.value = null
}

async function saveActivity(formData) {
  if (saving.value) return

  requestError.value = ''
  saving.value = true

  try {
    const disciplineId = editingActivity.value?.disciplineId || formData.disciplineId
    const activityId = editingActivity.value?.id

    const path = activityId
      ? `/api/v1/dashboards/${dashboardId.value}/disciplines/${disciplineId}/activities/${activityId}`
      : `/api/v1/dashboards/${dashboardId.value}/disciplines/${disciplineId}/activities`

    const savedActivity = await apiRequest(path, {
      method: activityId ? 'PUT' : 'POST',
      body: JSON.stringify({
        title: formData.title,
        description: formData.description,
        dueDate: formData.dueDate,
        status: formData.status,
      }),
    })

    const normalizedActivity = normalizeActivity(savedActivity)

    const index = activities.value.findIndex(item => item.id === normalizedActivity.id)

    if (index >= 0) {
      activities.value[index] = normalizedActivity
    } else {
      activities.value.push(normalizedActivity)
    }

    saveFeedback.value = activityId
      ? 'Atividade atualizada com sucesso.'
      : 'Atividade adicionada com sucesso.'
    showToast(saveFeedback.value)
    closeActivityModal()
  } catch (error) {
    requestError.value = error.message || 'Não foi possível salvar a atividade.'
    showToast(requestError.value, 'error')
  } finally {
    saving.value = false
  }
}

function askToDeleteActivity(activity) {
  activityToDelete.value = activity
}

function closeDeleteModal() {
  activityToDelete.value = null
}

async function confirmDeleteActivity() {
  if (!activityToDelete.value) return

  requestError.value = ''

  try {
    const activity = activityToDelete.value

    await apiRequest(
      `/api/v1/dashboards/${dashboardId.value}/disciplines/${activity.disciplineId}/activities/${activity.id}`,
      { method: 'DELETE' },
    )

    activities.value = activities.value.filter(item => item.id !== activity.id)
    saveFeedback.value = 'Atividade excluída com sucesso.'
    showToast(saveFeedback.value)
    closeDeleteModal()
  } catch (error) {
    requestError.value = error.message || 'Não foi possível excluir a atividade.'
    showToast(requestError.value, 'error')
  }
}

async function completeActivity(activity) {
  if (activity.status === 'COMPLETED') return

  requestError.value = ''

  try {
    const updatedActivity = await apiRequest(
      `/api/v1/dashboards/${dashboardId.value}/disciplines/${activity.disciplineId}/activities/${activity.id}`,
      {
        method: 'PUT',
        body: JSON.stringify({
          title: activity.title,
          description: activity.description,
          dueDate: activity.dueDate,
          status: 'COMPLETED',
        }),
      },
    )

    const index = activities.value.findIndex(item => item.id === activity.id)

    if (index >= 0) {
      activities.value[index] = normalizeActivity(updatedActivity)
    }

    saveFeedback.value = 'Atividade marcada como concluída.'
    showToast(saveFeedback.value)
  } catch (error) {
    requestError.value = error.message || 'Não foi possível concluir a atividade.'
    showToast(requestError.value, 'error')
  }
}

function clearFilters() {
  searchTerm.value = ''
  activeFilter.value = 'all'
  sortOrder.value = 'dueAsc'
}

const filteredActivities = computed(() => {
  const search = searchTerm.value.trim().toLocaleLowerCase('pt-BR')

  return activities.value
    .filter(activity => {
      const matchesSearch = !search
        || activity.title.toLocaleLowerCase('pt-BR').includes(search)
        || (activity.description || '').toLocaleLowerCase('pt-BR').includes(search)
        || activity.disciplineName.toLocaleLowerCase('pt-BR').includes(search)

      const matchesFilter = activeFilter.value === 'all'
        || (activeFilter.value === 'pending' && activity.status === 'PENDING')
        || (activeFilter.value === 'progress' && activity.status === 'IN_PROGRESS')
        || (activeFilter.value === 'completed' && activity.status === 'COMPLETED')

      return matchesSearch && matchesFilter
    })
    .sort((first, second) => {
      if (sortOrder.value === 'dueDesc') {
        return second.dueDate.localeCompare(first.dueDate)
      }

      if (sortOrder.value === 'titleAsc') {
        return first.title.localeCompare(second.title, 'pt-BR')
      }

      if (sortOrder.value === 'titleDesc') {
        return second.title.localeCompare(first.title, 'pt-BR')
      }

      return first.dueDate.localeCompare(second.dueDate)
    })
})

const totalPending = computed(() => (
  activities.value.filter(activity => activity.status === 'PENDING').length
))

const totalInProgress = computed(() => (
  activities.value.filter(activity => activity.status === 'IN_PROGRESS').length
))

const totalCompleted = computed(() => (
  activities.value.filter(activity => activity.status === 'COMPLETED').length
))

function statusDetails(status) {
  const statuses = {
    PENDING: { label: 'Pendente', className: 'is-pending' },
    IN_PROGRESS: { label: 'Em andamento', className: 'is-progress' },
    COMPLETED: { label: 'Concluída', className: 'is-completed' },
  }

  return statuses[status] || statuses.PENDING
}

function formatDate(date) {
  if (!date) return '—'

  return new Intl.DateTimeFormat('pt-BR', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  }).format(new Date(`${date}T12:00:00`))
}

function isOverdue(activity) {
  if (activity.status === 'COMPLETED' || !activity.dueDate) return false

  const today = new Date()
  const todayIso = [
    today.getFullYear(),
    String(today.getMonth() + 1).padStart(2, '0'),
    String(today.getDate()).padStart(2, '0'),
  ].join('-')

  return activity.dueDate < todayIso
}

onMounted(loadActivities)
onBeforeUnmount(() => clearTimeout(toastTimer))
</script>

<template>
  <section class="activities-page" aria-labelledby="activities-title">
    <header class="activities-header">
      <div class="activities-heading">
        <span class="activities-heading-icon" aria-hidden="true">
          <svg viewBox="0 0 24 24">
            <rect x="5" y="4" width="14" height="17" rx="2" />
            <path d="M9 4V2m6 2V2M8 9h8m-8 4 2 2 4-4" />
          </svg>
        </span>

        <div>
          <h1 id="activities-title">Atividades</h1>
          <p>Organize tarefas, trabalhos e prazos das suas disciplinas.</p>
        </div>
      </div>

      <div class="activities-actions">
        <label class="activities-search">
          <span class="sr-only">Buscar atividade</span>
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <circle cx="11" cy="11" r="7" />
            <path d="m20 20-4-4" />
          </svg>
          <input
            v-model="searchTerm"
            type="search"
            placeholder="Buscar atividade..."
          >
        </label>

        <button
          class="activities-add-button"
          type="button"
          :disabled="loading || !dashboardId || disciplines.length === 0"
          @click="openAddModal"
        >
          <span aria-hidden="true">＋</span>
          Nova atividade
        </button>
      </div>
    </header>

    <div class="activities-summary-grid">
      <article class="activities-summary-card is-purple">
        <span class="activities-summary-icon" aria-hidden="true">
          <svg viewBox="0 0 24 24">
            <rect x="5" y="4" width="14" height="17" rx="2" />
            <path d="M9 4V2m6 2V2M8 9h8" />
          </svg>
        </span>
        <div>
          <p>Total de atividades</p>
          <strong>{{ activities.length }}</strong>
          <small>Todas as disciplinas</small>
        </div>
      </article>

      <article class="activities-summary-card is-orange">
        <span class="activities-summary-icon" aria-hidden="true">
          <svg viewBox="0 0 24 24">
            <circle cx="12" cy="12" r="9" />
            <path d="M12 7v5l3 2" />
          </svg>
        </span>
        <div>
          <p>Pendentes</p>
          <strong>{{ totalPending }}</strong>
          <small>Aguardando início</small>
        </div>
      </article>

      <article class="activities-summary-card is-blue">
        <span class="activities-summary-icon" aria-hidden="true">
          <svg viewBox="0 0 24 24">
            <path d="M4 12h16M12 4v16" />
          </svg>
        </span>
        <div>
          <p>Em andamento</p>
          <strong>{{ totalInProgress }}</strong>
          <small>Em execução</small>
        </div>
      </article>

      <article class="activities-summary-card is-green">
        <span class="activities-summary-icon" aria-hidden="true">
          <svg viewBox="0 0 24 24">
            <circle cx="12" cy="12" r="9" />
            <path d="m8 12 3 3 5-6" />
          </svg>
        </span>
        <div>
          <p>Concluídas</p>
          <strong>{{ totalCompleted }}</strong>
          <small>Finalizadas</small>
        </div>
      </article>
    </div>

    <section class="activities-content">
      <div class="activities-toolbar">
        <div class="activities-filters" aria-label="Filtrar atividades">
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

        <label class="activities-sort">
          <span>Ordenar por</span>
          <select v-model="sortOrder">
            <option value="dueAsc">Prazo mais próximo</option>
            <option value="dueDesc">Prazo mais distante</option>
            <option value="titleAsc">Título A–Z</option>
            <option value="titleDesc">Título Z–A</option>
          </select>
        </label>
      </div>

      <div v-if="loading" class="activities-state-card">
        <span class="activities-loader" aria-hidden="true"></span>
        <h2>Carregando atividades...</h2>
        <p>Aguarde enquanto buscamos os dados.</p>
      </div>

      <div v-else-if="disciplines.length === 0" class="activities-state-card">
        <span class="activities-state-icon" aria-hidden="true">
          <svg viewBox="0 0 24 24">
            <path d="M4 5.5A3.5 3.5 0 0 1 7.5 2H11v17H7.5A3.5 3.5 0 0 0 4 22V5.5Z" />
            <path d="M20 5.5A3.5 3.5 0 0 0 16.5 2H13v17h3.5A3.5 3.5 0 0 1 20 22V5.5Z" />
          </svg>
        </span>
        <h2>Cadastre uma disciplina primeiro</h2>
        <p>As atividades precisam estar vinculadas a uma disciplina.</p>
        <button class="activities-empty-button" type="button" @click="emit('navigate', 'disciplines')">
          <span aria-hidden="true">＋</span>
          Cadastrar disciplina
        </button>
      </div>

      <div v-else-if="activities.length === 0" class="activities-state-card">
        <span class="activities-state-icon is-green" aria-hidden="true">
          <svg viewBox="0 0 24 24">
            <rect x="5" y="4" width="14" height="17" rx="2" />
            <path d="M9 4V2m6 2V2M8 9h8m-8 4 2 2 4-4" />
          </svg>
        </span>
        <h2>Nenhuma atividade cadastrada</h2>
        <p>Crie sua primeira atividade para começar a organizar seus prazos.</p>
        <button class="activities-empty-button" type="button" @click="openAddModal">
          <span aria-hidden="true">＋</span>
          Nova atividade
        </button>
      </div>

      <template v-else>
        <div v-if="filteredActivities.length > 0" class="activities-list">
          <article
            v-for="activity in filteredActivities"
            :key="activity.id"
            class="activity-card"
            :class="{ 'is-overdue': isOverdue(activity) }"
          >
            <span
              class="activity-discipline-bar"
              :style="{ background: activity.disciplineColor }"
              aria-hidden="true"
            ></span>

            <div class="activity-main">
              <div class="activity-title-row">
                <div>
                  <span
                    class="activity-discipline"
                    :style="{
                      '--discipline-color': activity.disciplineColor,
                    }"
                  >
                    <span aria-hidden="true"></span>
                    {{ activity.disciplineName }}
                  </span>

                  <h2>{{ activity.title }}</h2>
                </div>

                <span
                  class="activity-status"
                  :class="statusDetails(activity.status).className"
                >
                  {{ statusDetails(activity.status).label }}
                </span>
              </div>

              <p v-if="activity.description" class="activity-description">
                {{ activity.description }}
              </p>

              <div class="activity-meta">
                <span :class="{ overdue: isOverdue(activity) }">
                  <svg viewBox="0 0 24 24" aria-hidden="true">
                    <rect x="3" y="5" width="18" height="16" rx="2" />
                    <path d="M7 3v4m10-4v4M3 10h18" />
                  </svg>
                  {{ isOverdue(activity) ? 'Atrasada: ' : 'Entrega: ' }}
                  {{ formatDate(activity.dueDate) }}
                </span>
              </div>
            </div>

            <div class="activity-card-actions">
              <button
                v-if="activity.status !== 'COMPLETED'"
                class="activity-complete"
                type="button"
                title="Marcar como concluída"
                @click="completeActivity(activity)"
              >
                <svg viewBox="0 0 24 24" aria-hidden="true">
                  <path d="m5 12 4 4 10-10" />
                </svg>
                Concluir
              </button>

              <button
                class="activity-icon-button"
                type="button"
                title="Editar atividade"
                aria-label="Editar atividade"
                @click="openEditModal(activity)"
              >
                <svg viewBox="0 0 24 24" aria-hidden="true">
                  <path d="m4 20 4-1 11-11-3-3L5 16l-1 4Z" />
                  <path d="m14 7 3 3" />
                </svg>
              </button>

              <button
                class="activity-icon-button is-delete"
                type="button"
                title="Excluir atividade"
                aria-label="Excluir atividade"
                @click="askToDeleteActivity(activity)"
              >
                <svg viewBox="0 0 24 24" aria-hidden="true">
                  <path d="M4 7h16M9 7V4h6v3m3 0-1 13H7L6 7m4 4v5m4-5v5" />
                </svg>
              </button>
            </div>
          </article>
        </div>

        <div v-else class="activities-no-results">
          <h2>Nenhuma atividade encontrada</h2>
          <p>Altere a busca ou os filtros para visualizar outros resultados.</p>
          <button type="button" @click="clearFilters">Limpar filtros</button>
        </div>
      </template>
    </section>

    <AppToast :message="toast.message" :type="toast.type" @close="closeToast" />

    <ActivityModal
      v-if="showActivityModal"
      :activity="editingActivity"
      :disciplines="disciplines"
      :saving="saving"
      @close="closeActivityModal"
      @save="saveActivity"
    />

    <DeleteActivityModal
      v-if="activityToDelete"
      :activity-title="activityToDelete.title"
      @close="closeDeleteModal"
      @confirm="confirmDeleteActivity"
    />
  </section>
</template>

<style scoped>
.activities-page {
  display: grid;
  gap: 22px;
}

.activities-header {
  align-items: center;
  display: flex;
  gap: 22px;
  justify-content: space-between;
}

.activities-heading,
.activities-heading-icon,
.activities-actions,
.activities-search,
.activities-add-button,
.activities-summary-card,
.activities-summary-icon,
.activities-toolbar,
.activities-filters,
.activity-title-row,
.activity-discipline,
.activity-meta,
.activity-card-actions,
.activity-complete,
.activity-icon-button {
  align-items: center;
  display: flex;
}

.activities-heading {
  gap: 13px;
}

.activities-heading-icon {
  background: #e9f8ef;
  border-radius: 11px;
  color: #2daf68;
  height: 48px;
  justify-content: center;
  width: 48px;
}

.activities-heading-icon svg,
.activities-summary-icon svg,
.activities-search svg,
.activity-meta svg,
.activity-card-actions svg,
.activities-state-icon svg {
  fill: none;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 1.8;
}

.activities-heading-icon svg {
  height: 25px;
  width: 25px;
}

.activities-heading h1 {
  color: #171c30;
  font-size: 1.65rem;
  font-weight: 800;
  letter-spacing: -.04em;
  line-height: 1.1;
  margin: 0 0 6px;
}

.activities-heading p {
  color: #70778b;
  font-size: .76rem;
}

.activities-actions {
  gap: 11px;
}

.activities-search {
  background: #fff;
  border: 1px solid #dfe1e8;
  border-radius: 8px;
  gap: 8px;
  min-width: 245px;
  padding: 0 12px;
}

.activities-search:focus-within {
  border-color: #7544eb;
  box-shadow: 0 0 0 3px rgba(117, 68, 235, .1);
}

.activities-search svg {
  color: #82889a;
  height: 18px;
  width: 18px;
}

.activities-search input {
  background: transparent;
  border: 0;
  color: #262c40;
  font-size: .72rem;
  min-width: 0;
  outline: 0;
  padding: 11px 0;
  width: 100%;
}

.activities-search input::placeholder {
  color: #9ba0af;
}

.activities-add-button,
.activities-empty-button {
  background: linear-gradient(100deg, #5d20df, #7419f5);
  border: 0;
  border-radius: 7px;
  box-shadow: 0 8px 18px rgba(101, 31, 225, .18);
  color: #fff;
  font-size: .72rem;
  font-weight: 700;
  gap: 7px;
  padding: 11px 15px;
}

.activities-add-button:disabled {
  cursor: not-allowed;
  opacity: .5;
}

.activities-add-button span,
.activities-empty-button span {
  font-size: 1.05rem;
  line-height: .8;
}

.activities-summary-grid {
  display: grid;
  gap: 14px;
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.activities-summary-card {
  background: #fff;
  border: 1px solid #ebeaf1;
  border-radius: 12px;
  box-shadow: 0 5px 16px rgba(30, 36, 65, .035);
  gap: 13px;
  min-height: 105px;
  padding: 17px;
}

.activities-summary-icon {
  background: #f1edff;
  border-radius: 50%;
  color: #6739e7;
  flex: 0 0 46px;
  height: 46px;
  justify-content: center;
}

.activities-summary-icon svg {
  height: 23px;
  width: 23px;
}

.activities-summary-card p {
  color: #596078;
  font-size: .67rem;
}

.activities-summary-card strong {
  color: #171c30;
  display: block;
  font-size: 1.3rem;
  line-height: 1;
  margin: 7px 0 5px;
}

.activities-summary-card small {
  color: #858b9e;
  display: block;
  font-size: .61rem;
}

.activities-summary-card.is-orange .activities-summary-icon {
  background: #fff0e2;
  color: #ee831e;
}

.activities-summary-card.is-blue .activities-summary-icon {
  background: #eaf2ff;
  color: #347bd8;
}

.activities-summary-card.is-green .activities-summary-icon {
  background: #e8f8ef;
  color: #2daf68;
}

.activities-content {
  background: #fff;
  border: 1px solid #ebeaf1;
  border-radius: 12px;
  box-shadow: 0 5px 16px rgba(30, 36, 65, .035);
  overflow: hidden;
}

.activities-toolbar {
  border-bottom: 1px solid #ececf2;
  gap: 18px;
  justify-content: space-between;
  padding: 16px 18px;
}

.activities-filters {
  background: #f6f5fa;
  border-radius: 8px;
  gap: 4px;
  padding: 4px;
}

.activities-filters button {
  background: transparent;
  border: 0;
  border-radius: 6px;
  color: #6e7488;
  font-size: .67rem;
  font-weight: 700;
  padding: 8px 11px;
}

.activities-filters button.active {
  background: #fff;
  box-shadow: 0 2px 7px rgba(28, 32, 57, .08);
  color: #602bd4;
}

.activities-sort {
  align-items: center;
  color: #767c90;
  display: flex;
  font-size: .65rem;
  gap: 8px;
}

.activities-sort select {
  background: #fff;
  border: 1px solid #dfe1e8;
  border-radius: 7px;
  color: #353b50;
  font-size: .67rem;
  outline: 0;
  padding: 8px 30px 8px 10px;
}

.activities-list {
  display: grid;
}

.activity-card {
  align-items: stretch;
  border-bottom: 1px solid #efeff4;
  display: grid;
  grid-template-columns: 4px minmax(0, 1fr) auto;
  min-height: 112px;
  position: relative;
}

.activity-card:last-child {
  border-bottom: 0;
}

.activity-card:hover {
  background: #fcfbff;
}

.activity-card.is-overdue {
  background: #fffafa;
}

.activity-discipline-bar {
  width: 4px;
}

.activity-main {
  min-width: 0;
  padding: 15px 20px;
}

.activity-title-row {
  align-items: flex-start;
  gap: 15px;
  justify-content: space-between;
}

.activity-title-row > div {
  min-width: 0;
}

.activity-discipline {
  color: #6b7185;
  font-size: .62rem;
  font-weight: 700;
  gap: 6px;
}

.activity-discipline > span {
  background: var(--discipline-color);
  border-radius: 50%;
  height: 7px;
  width: 7px;
}

.activity-card h2 {
  color: #202538;
  font-size: .87rem;
  font-weight: 800;
  letter-spacing: -.015em;
  margin: 7px 0 0;
}

.activity-status {
  border-radius: 999px;
  flex: 0 0 auto;
  font-size: .59rem;
  font-weight: 750;
  padding: 6px 9px;
}

.activity-status.is-pending {
  background: #fff0e2;
  color: #b76110;
}

.activity-status.is-progress {
  background: #eaf2ff;
  color: #2f6fc3;
}

.activity-status.is-completed {
  background: #e8f8ef;
  color: #218950;
}

.activity-description {
  color: #747b8e;
  font-size: .68rem;
  line-height: 1.55;
  margin: 10px 0 0;
  max-width: 760px;
  overflow-wrap: anywhere;
}

.activity-meta {
  color: #7b8295;
  font-size: .63rem;
  gap: 14px;
  margin-top: 13px;
}

.activity-meta span {
  align-items: center;
  display: flex;
  gap: 6px;
}

.activity-meta span.overdue {
  color: #ca403c;
  font-weight: 700;
}

.activity-meta svg {
  height: 15px;
  width: 15px;
}

.activity-card-actions {
  gap: 7px;
  justify-content: flex-end;
  padding: 15px 18px;
}

.activity-complete {
  background: #eff9f3;
  border: 1px solid #cfeeda;
  border-radius: 7px;
  color: #248f55;
  font-size: .64rem;
  font-weight: 750;
  gap: 6px;
  padding: 8px 10px;
}

.activity-complete svg {
  height: 15px;
  width: 15px;
}

.activity-icon-button {
  background: #fff;
  border: 1px solid #dfe1e8;
  border-radius: 7px;
  color: #62697d;
  height: 34px;
  justify-content: center;
  width: 34px;
}

.activity-icon-button svg {
  height: 16px;
  width: 16px;
}

.activity-icon-button:hover {
  border-color: #cfc4ef;
  color: #6231cf;
}

.activity-icon-button.is-delete:hover {
  background: #fff5f4;
  border-color: #efc7c3;
  color: #cf4035;
}

.activities-state-card,
.activities-no-results {
  align-items: center;
  display: flex;
  flex-direction: column;
  min-height: 330px;
  justify-content: center;
  padding: 35px;
  text-align: center;
}

.activities-state-icon {
  align-items: center;
  background: #f1edff;
  border-radius: 50%;
  color: #6739e7;
  display: flex;
  height: 58px;
  justify-content: center;
  width: 58px;
}

.activities-state-icon.is-green {
  background: #e8f8ef;
  color: #2daf68;
}

.activities-state-icon svg {
  height: 29px;
  width: 29px;
}

.activities-state-card h2,
.activities-no-results h2 {
  color: #202538;
  font-size: 1rem;
  font-weight: 800;
  margin: 16px 0 7px;
}

.activities-state-card p,
.activities-no-results p {
  color: #777e91;
  font-size: .7rem;
  line-height: 1.5;
  max-width: 410px;
}

.activities-empty-button {
  align-items: center;
  display: flex;
  margin-top: 18px;
}

.activities-no-results button {
  background: transparent;
  border: 0;
  color: #6330d8;
  font-size: .68rem;
  font-weight: 750;
  margin-top: 13px;
}

.activities-loader {
  animation: activities-spin .8s linear infinite;
  border: 3px solid #e7e0fb;
  border-radius: 50%;
  border-top-color: #6b37df;
  height: 34px;
  width: 34px;
}

.activities-request-error,
.activities-save-feedback {
  border-radius: 8px;
  font-size: .68rem;
  padding: 11px 14px;
}

.activities-request-error {
  background: #fff1f0;
  border: 1px solid #f1d3d0;
  color: #ad3834;
}

.activities-request-error button {
  background: transparent;
  border: 0;
  color: inherit;
  font-weight: 800;
  margin-left: 5px;
  text-decoration: underline;
}

.activities-save-feedback {
  background: #edf9f2;
  border: 1px solid #cdebd9;
  color: #237b4a;
}

.sr-only {
  clip: rect(0, 0, 0, 0);
  clip-path: inset(50%);
  height: 1px;
  overflow: hidden;
  position: absolute;
  white-space: nowrap;
  width: 1px;
}

button {
  cursor: pointer;
}

button:focus-visible,
select:focus-visible,
input:focus-visible {
  outline: 3px solid rgba(105, 54, 224, .24);
  outline-offset: 2px;
}

@keyframes activities-spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 1100px) {
  .activities-summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .activities-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .activities-actions {
    width: 100%;
  }

  .activities-search {
    flex: 1;
  }
}

@media (max-width: 760px) {
  .activities-actions,
  .activities-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .activities-search {
    min-width: 0;
    width: 100%;
  }

  .activities-add-button {
    justify-content: center;
  }

  .activities-filters {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
  }

  .activities-sort {
    justify-content: space-between;
  }

  .activity-card {
    grid-template-columns: 4px minmax(0, 1fr);
  }

  .activity-card-actions {
    grid-column: 2;
    justify-content: flex-start;
    padding: 0 20px 17px;
  }
}

@media (max-width: 520px) {
  .activities-summary-grid {
    grid-template-columns: 1fr;
  }

  .activity-title-row {
    align-items: flex-start;
    flex-direction: column;
  }

  .activity-card-actions {
    flex-wrap: wrap;
  }
}
</style>
