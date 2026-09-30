<script setup>
import { apiRequest } from '../../shared/http/apiRequest.js'
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import AppToast from '../../components/ui/AppToast.vue'
import { ATTENTION_MARGIN } from '../frequency/frequencyRules.js'
import DisciplineModal from './DisciplineModal.vue'
import DeleteDisciplineModal from './DeleteDisciplineModal.vue'
import DisciplinesHeader from './DisciplinesHeader.vue'
import DisciplinesSummaryCards from './DisciplinesSummaryCards.vue'
import DisciplinesToolbar from './DisciplinesToolbar.vue'
import DisciplinesEmptyState from './DisciplinesEmptyState.vue'
import DisciplinesTable from './DisciplinesTable.vue'
import DisciplinesCardGrid from './DisciplinesCardGrid.vue'
import DisciplineStatusMenu from './DisciplineStatusMenu.vue'
import {
  MINIMUM_ATTENDANCE,
  attendanceSituation,
  averageOf,
  filterAndSortDisciplines,
  normalizeDiscipline,
  statusDetails,
  statusMenuPosition,
} from './disciplinesPresentation.js'
import { loadActiveDashboard } from '../../shared/dashboards/useActiveDashboard.js'
import { loadAttendanceAlertMargin } from '../../shared/settings/loadAttendanceAlertMargin.js'
import './disciplines.css'

const activeFilter = ref('all')
const viewMode = ref('list')
const searchTerm = ref('')
const sortOrder = ref('nameAsc')
const showAddModal = ref(false)
const editingDiscipline = ref(null)
const disciplineToDelete = ref(null)
const statusMenu = ref(null)
const statusMenuRef = ref(null)
const statusUpdating = ref(false)
const savingDiscipline = ref(false)
const deletingDiscipline = ref(false)
const saveFeedback = ref('')
const requestError = ref('')
const loading = ref(true)
const dashboardId = ref('')
const disciplines = ref([])
const toast = ref({ message: '', type: 'success' })
const attendanceAlertMargin = ref(ATTENTION_MARGIN)
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

function openAddModal() {
  saveFeedback.value = ''
  editingDiscipline.value = null
  showAddModal.value = true
}

function openEditModal(discipline) {
  closeStatusMenu()
  saveFeedback.value = ''
  editingDiscipline.value = discipline
  showAddModal.value = true
}

function closeAddModal() {
  showAddModal.value = false
  editingDiscipline.value = null
}

async function loadDisciplines() {
  loading.value = true
  requestError.value = ''

  try {
    let dashboard = await loadActiveDashboard(apiRequest)

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
  if (savingDiscipline.value) return

  requestError.value = ''
  savingDiscipline.value = true

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
  } finally {
    savingDiscipline.value = false
  }
}

function askToDeleteDiscipline(discipline) {
  closeStatusMenu()
  disciplineToDelete.value = discipline
}

function closeStatusMenu() {
  statusMenu.value = null
}

async function toggleStatusMenu(discipline, event) {
  if (statusMenu.value?.disciplineId === discipline.id) {
    closeStatusMenu()
    return
  }

  const bounds = event.currentTarget.getBoundingClientRect()
  const { left, top } = statusMenuPosition(bounds, { width: window.innerWidth, height: window.innerHeight })

  statusMenu.value = {
    disciplineId: discipline.id,
    disciplineName: discipline.name,
    currentStatus: discipline.status,
    left,
    top,
  }

  await nextTick()
  statusMenuRef.value?.focusFirstOption()
}

async function changeDisciplineStatus(status) {
  const menu = statusMenu.value
  if (!menu || statusUpdating.value) return

  if (status === menu.currentStatus) {
    closeStatusMenu()
    return
  }

  statusUpdating.value = true
  requestError.value = ''

  try {
    const updated = normalizeDiscipline(await apiRequest(
      `/api/v1/dashboards/${dashboardId.value}/disciplines/${menu.disciplineId}/status`,
      {
        method: 'PATCH',
        body: JSON.stringify({ status }),
      },
    ))
    const index = disciplines.value.findIndex(item => item.id === menu.disciplineId)
    if (index !== -1) disciplines.value[index] = updated
    showToast(`Situação alterada para ${statusDetails(status).label.toLocaleLowerCase('pt-BR')}.`)
    closeStatusMenu()
  } catch (error) {
    requestError.value = error.message || 'Não foi possível alterar a situação da disciplina.'
    showToast(requestError.value, 'error')
  } finally {
    statusUpdating.value = false
  }
}

function handleStatusMenuKeydown(event) {
  if (event.key === 'Escape') closeStatusMenu()
}

function closeDeleteModal() {
  disciplineToDelete.value = null
}

async function confirmDeleteDiscipline() {
  if (!disciplineToDelete.value || deletingDiscipline.value) return

  requestError.value = ''
  deletingDiscipline.value = true

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
  } finally {
    deletingDiscipline.value = false
  }
}

onMounted(() => {
  loadDisciplines()
  loadAttendanceAlertMargin(apiRequest).then(margin => {
    attendanceAlertMargin.value = margin
  })
  document.addEventListener('click', closeStatusMenu)
  window.addEventListener('resize', closeStatusMenu)
  window.addEventListener('keydown', handleStatusMenuKeydown)
})
onBeforeUnmount(() => {
  clearTimeout(toastTimer)
  document.removeEventListener('click', closeStatusMenu)
  window.removeEventListener('resize', closeStatusMenu)
  window.removeEventListener('keydown', handleStatusMenuKeydown)
})

function clearFilters() {
  searchTerm.value = ''
  activeFilter.value = 'all'
  sortOrder.value = 'nameAsc'
}

const filteredDisciplines = computed(() => filterAndSortDisciplines(disciplines.value, {
  searchTerm: searchTerm.value,
  activeFilter: activeFilter.value,
  sortOrder: sortOrder.value,
}))

const generalAverage = computed(() => averageOf(disciplines.value, 'average'))

const averageAttendance = computed(() => averageOf(disciplines.value, 'attendancePercentage'))

const averageAttendanceSituation = computed(() => attendanceSituation(averageAttendance.value, MINIMUM_ATTENDANCE, Number.POSITIVE_INFINITY, attendanceAlertMargin.value))
</script>

<template>
  <section class="disciplines-page" aria-labelledby="disciplines-title">
    <DisciplinesHeader
      v-model:search-term="searchTerm"
      :add-disabled="loading || !dashboardId"
      @add="openAddModal"
    />

    <DisciplinesSummaryCards
      :total="disciplines.length"
      :general-average="generalAverage"
      :average-attendance="averageAttendance"
      :attendance-situation="averageAttendanceSituation"
    />

    <DisciplinesToolbar
      v-model:active-filter="activeFilter"
      v-model:sort-order="sortOrder"
      v-model:view-mode="viewMode"
      @clear="clearFilters"
    />

    <article v-if="loading" class="disciplines-loading-card" aria-live="polite">
      <span class="loading-spinner" aria-hidden="true"></span>
      <p>Carregando disciplinas...</p>
    </article>

    <DisciplinesEmptyState v-else-if="disciplines.length === 0" @add="openAddModal" />

    <DisciplinesTable
      v-else-if="viewMode === 'list'"
      :disciplines="filteredDisciplines"
      :open-menu-id="statusMenu?.disciplineId ?? null"
      :attendance-alert-margin="attendanceAlertMargin"
      @toggle-status="toggleStatusMenu"
      @edit="openEditModal"
      @delete="askToDeleteDiscipline"
    />

    <DisciplinesCardGrid
      v-else
      :disciplines="filteredDisciplines"
      :open-menu-id="statusMenu?.disciplineId ?? null"
      :attendance-alert-margin="attendanceAlertMargin"
      @toggle-status="toggleStatusMenu"
      @edit="openEditModal"
      @delete="askToDeleteDiscipline"
    />

    <AppToast :message="toast.message" :type="toast.type" @close="closeToast" />

    <DisciplineStatusMenu
      v-if="statusMenu"
      ref="statusMenuRef"
      :menu="statusMenu"
      :updating="statusUpdating"
      @select="changeDisciplineStatus"
    />

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
      :saving="savingDiscipline"
      @close="closeAddModal"
      @save="saveDiscipline"
    />

    <DeleteDisciplineModal
      v-if="disciplineToDelete"
      :discipline-name="disciplineToDelete.name"
      :deleting="deletingDiscipline"
      @close="closeDeleteModal"
      @confirm="confirmDeleteDiscipline"
    />
  </section>
</template>
