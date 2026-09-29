<script setup>
import { apiRequest } from '../../shared/http/apiRequest.js'
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import ActivitiesSummaryCards from './ActivitiesSummaryCards.vue'
import ActivityCard from './ActivityCard.vue'
import ActivityModal from './ActivityModal.vue'
import AppSelect from '../../components/ui/AppSelect.vue'
import AppToast from '../../components/ui/AppToast.vue'
import DeleteActivityModal from './DeleteActivityModal.vue'
import {
  countByStatus,
  filterActivities,
  filters,
  normalizeActivity as normalizeActivityWith,
  sortOptions,
} from './activitiesPresentation.js'
import { loadActiveDashboard } from '../../shared/dashboards/useActiveDashboard.js'
import './activities.css'

const emit = defineEmits(['navigate'])

const dashboardId = ref('')
const disciplines = ref([])
const activities = ref([])
const loading = ref(true)
const loadError = ref('')
const requestError = ref('')
const saveFeedback = ref('')
let activitiesRequestId = 0

const searchTerm = ref('')
const activeFilter = ref('all')
const sortOrder = ref('dueAsc')
const viewMode = ref('list')

const showActivityModal = ref(false)
const editingActivity = ref(null)
const activityToDelete = ref(null)
const saving = ref(false)
const activityModalError = ref('')
const toast = ref({ message: '', type: 'success' })
let toastTimer

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

function normalizeActivity(activity) {
  return normalizeActivityWith(activity, disciplines.value)
}

async function loadActivities() {
  const requestId = ++activitiesRequestId
  loading.value = true
  loadError.value = ''

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

    if (requestId !== activitiesRequestId) return

    dashboardId.value = dashboard.id

    disciplines.value = await apiRequest(
      `/api/v1/dashboards/${dashboard.id}/disciplines`,
    )

    if (requestId !== activitiesRequestId) return

    const loadedActivities = await apiRequest(
      `/api/v1/dashboards/${dashboard.id}/activities`,
    )

    if (requestId !== activitiesRequestId) return

    activities.value = loadedActivities.map(activity => normalizeActivity(activity))
    loadError.value = ''
  } catch (error) {
    if (requestId !== activitiesRequestId) return

    loadError.value = error.message || 'Não foi possível carregar as atividades.'
  } finally {
    if (requestId === activitiesRequestId) {
      loading.value = false
    }
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
  activityModalError.value = ''
  editingActivity.value = null
  showActivityModal.value = true
}

function openEditModal(activity) {
  saveFeedback.value = ''
  activityModalError.value = ''
  editingActivity.value = activity
  showActivityModal.value = true
}

function closeActivityModal() {
  showActivityModal.value = false
  editingActivity.value = null
  activityModalError.value = ''
}

async function saveActivity(formData) {
  if (saving.value) return

  requestError.value = ''
  activityModalError.value = ''
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
        type: formData.type,
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
    activityModalError.value = error.message || 'Não foi possível salvar a atividade.'
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

const busyActivityIds = ref(new Set())

function isActivityBusy(id) {
  return busyActivityIds.value.has(id)
}

function setActivityBusy(id, busy) {
  const next = new Set(busyActivityIds.value)
  if (busy) next.add(id)
  else next.delete(id)
  busyActivityIds.value = next
}

async function completeActivity(activity) {
  if (activity.status === 'COMPLETED' || isActivityBusy(activity.id)) return

  requestError.value = ''
  setActivityBusy(activity.id, true)

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
  } finally {
    setActivityBusy(activity.id, false)
  }
}

function clearFilters() {
  searchTerm.value = ''
  activeFilter.value = 'all'
  sortOrder.value = 'dueAsc'
}

const filteredActivities = computed(() => (
  filterActivities(activities.value, searchTerm.value, activeFilter.value, sortOrder.value)
))

const totalPending = computed(() => countByStatus(activities.value, 'PENDING'))

const totalInProgress = computed(() => countByStatus(activities.value, 'IN_PROGRESS'))

const totalCompleted = computed(() => countByStatus(activities.value, 'COMPLETED'))

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
          :disabled="loading || !!loadError || !dashboardId || disciplines.length === 0"
          @click="openAddModal"
        >
          <span aria-hidden="true">＋</span>
          Nova atividade
        </button>
      </div>
    </header>

    <ActivitiesSummaryCards
      :total="activities.length"
      :pending="totalPending"
      :in-progress="totalInProgress"
      :completed="totalCompleted"
    />

    <div class="activities-toolbar">
      <div class="activities-filters" aria-label="Filtrar atividades">
        <button
          v-for="filter in filters"
          :key="filter.value"
          type="button"
          :class="{ active: activeFilter === filter.value }"
          :aria-pressed="activeFilter === filter.value"
          @click="activeFilter = filter.value"
        >
          {{ filter.label }}
        </button>
      </div>

      <div class="activities-view-options">
        <label class="activities-sort">
          <span>Ordenar por:</span>
          <AppSelect v-model="sortOrder" :options="sortOptions" variant="ghost" aria-label="Ordenar atividades" />
        </label>
        <button class="activities-clear-filters" type="button" @click="clearFilters">Limpar filtros</button>
        <div class="activities-view-buttons" role="group" aria-label="Modo de visualização">
          <button type="button" :class="{ active: viewMode === 'list' }" :aria-pressed="viewMode === 'list'" aria-label="Visualizar em lista" @click="viewMode = 'list'">
            <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M9 6h11M9 12h11M9 18h11" /><circle cx="4" cy="6" r="1" /><circle cx="4" cy="12" r="1" /><circle cx="4" cy="18" r="1" /></svg>
          </button>
          <button type="button" :class="{ active: viewMode === 'grid' }" :aria-pressed="viewMode === 'grid'" aria-label="Visualizar em cards" @click="viewMode = 'grid'">
            <svg viewBox="0 0 24 24" aria-hidden="true"><rect x="4" y="4" width="6" height="6" /><rect x="14" y="4" width="6" height="6" /><rect x="4" y="14" width="6" height="6" /><rect x="14" y="14" width="6" height="6" /></svg>
          </button>
        </div>
      </div>
    </div>

    <section class="activities-content" :class="{ 'is-grid': viewMode === 'grid' && !loading && filteredActivities.length > 0 }">
      <div v-if="loading" class="activities-state-card">
        <span class="activities-loader" aria-hidden="true"></span>
        <h2>Carregando atividades...</h2>
        <p>Aguarde enquanto buscamos os dados.</p>
      </div>

      <div v-else-if="loadError" class="activities-state-card">
        <span class="activities-state-icon is-error" aria-hidden="true">
          <svg viewBox="0 0 24 24">
            <circle cx="12" cy="12" r="9" />
            <path d="M12 8v5m0 3h.01" />
          </svg>
        </span>
        <h2>Não foi possível carregar as atividades</h2>
        <p>{{ loadError }}</p>
        <button class="activities-empty-button" type="button" @click="loadActivities">
          Tentar novamente
        </button>
      </div>

      <div v-else-if="disciplines.length === 0" class="activities-state-card">
        <span class="activities-state-icon" aria-hidden="true">
          <svg viewBox="0 0 24 24">
            <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2Z" />
            <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20M8 7h8M8 10h6" />
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
        <div v-if="filteredActivities.length > 0" class="activities-list" :class="{ 'is-grid': viewMode === 'grid' }">
          <ActivityCard
            v-for="activity in filteredActivities"
            :key="activity.id"
            :activity="activity"
            :busy="isActivityBusy(activity.id)"
            @complete="completeActivity"
            @edit="openEditModal"
            @delete="askToDeleteActivity"
          />
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
      :server-error="activityModalError"
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
