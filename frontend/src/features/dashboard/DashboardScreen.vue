<script setup>
import { apiRequest as sharedApiRequest } from '../../shared/http/apiRequest.js'
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import ActivitiesScreen from '../activities/ActivitiesScreen.vue'
import ProvasScreen from '../exams/ProvasScreen.vue'
import DisciplinesScreen from '../disciplines/DisciplinesScreen.vue'
import FrequencyScreen from '../frequency/FrequencyScreen.vue'
import ProfileScreen from '../profile/ProfileScreen.vue'
import SimulatorScreen from '../simulator/SimulatorScreen.vue'
import CalendarScreen from '../calendar/CalendarScreen.vue'
import GradesScreen from '../grades/GradesScreen.vue'
import SettingsScreen from '../settings/SettingsScreen.vue'
import { sectionFromPreference } from '../settings/settingsApi'
import DashboardActivitiesPanel from './DashboardActivitiesPanel.vue'
import DashboardClassesPanel from './DashboardClassesPanel.vue'
import DashboardFrequencyPanel from './DashboardFrequencyPanel.vue'
import DashboardGuideCards from './DashboardGuideCards.vue'
import DashboardProgressPanel from './DashboardProgressPanel.vue'
import DashboardRemindersPanel from './DashboardRemindersPanel.vue'
import DashboardSidebar from './DashboardSidebar.vue'
import DashboardStatusHero from './DashboardStatusHero.vue'
import DashboardSummaryCards from './DashboardSummaryCards.vue'
import {
  averageOf,
  compareByDueDate,
  firstNameOf,
  frequencyDetailsOf,
  isOverdue,
  nextClassOf,
  overdueRemindersOf,
  reminderLimitIsoOf,
  upcomingRemindersOf,
  visibleReminderGroupsOf,
} from './dashboardPresentation.js'
import './dashboard.css'
import { loadActiveDashboard } from '../../shared/dashboards/useActiveDashboard.js'
import {
  ATTENTION_MARGIN,
  DEFAULT_DEADLINE_ALERT_DAYS,
  DEFAULT_START_SECTION,
  loadPreferences,
  normalizePreferences,
} from '../../shared/settings/preferences.js'

const { user, routeSection } = defineProps({
  user: { type: Object, required: true },
  // Seção pedida pela URL (/disciplinas, /#/notas...), resolvida em features/auth/routeAccess.js.
  routeSection: { type: String, default: '' },
})

const emit = defineEmits(['logout', 'user-updated'])
const activeSection = ref(routeSection || 'dashboard')
watch(() => routeSection, section => { if (section) activeSection.value = section })
const sidebarAvatarUrl = ref('')
const dashboardLoading = ref(true)
const dashboardError = ref('')
const activitiesError = ref('')
const currentDashboardId = ref('')
const disciplines = ref([])
const activities = ref([])
const selectedFrequencyDisciplineId = ref('')
const currentDateTime = ref(new Date())
const attendanceAlertMargin = ref(ATTENTION_MARGIN)
const deadlineAlertDays = ref(DEFAULT_DEADLINE_ALERT_DAYS)
const startSection = ref(DEFAULT_START_SECTION)
let hasLoadedDashboard = false
let dashboardRequestSeq = 0
let activitiesRequestSeq = 0

let clockTimer

const firstName = computed(() => firstNameOf(user.name))
const userInitial = computed(() => firstName.value.charAt(0).toUpperCase())
const todayIso = computed(() => [
  currentDateTime.value.getFullYear(),
  String(currentDateTime.value.getMonth() + 1).padStart(2, '0'),
  String(currentDateTime.value.getDate()).padStart(2, '0'),
].join('-'))
const todayLabel = computed(() => new Intl.DateTimeFormat('pt-BR', {
  day: 'numeric',
  month: 'long',
}).format(currentDateTime.value))

function apiRequest(path) {
  return sharedApiRequest(path, { fallbackMessage: 'Não foi possível carregar o dashboard.' })
}

async function loadActivitiesForDashboard(dashboardId) {
  const requestId = ++activitiesRequestSeq
  activitiesError.value = ''

  try {
    const loaded = await apiRequest(`/api/v1/dashboards/${dashboardId}/activities`)
    if (requestId !== activitiesRequestSeq) return
    activities.value = loaded
  } catch (error) {
    if (requestId !== activitiesRequestSeq) return
    // Falha nas atividades não deve esconder as disciplinas já carregadas;
    // mostramos um erro específico da seção, sem misturar dados antigos.
    activities.value = []
    activitiesError.value = error.message || 'Não foi possível carregar as atividades.'
  }
}

async function retryActivities() {
  if (!currentDashboardId.value) return
  await loadActivitiesForDashboard(currentDashboardId.value)
}

// Só a primeira carga mostra o esqueleto; recargas mantêm os dados atuais visíveis.
// Respostas de uma carga superada por outra mais nova são descartadas.
async function loadDashboard() {
  const requestId = ++dashboardRequestSeq
  const isStale = () => requestId !== dashboardRequestSeq

  if (!hasLoadedDashboard) dashboardLoading.value = true
  dashboardError.value = ''
  activitiesError.value = ''

  try {
    const dashboard = await loadActiveDashboard(apiRequest)
    if (isStale()) return

    if (!dashboard) {
      disciplines.value = []
      activities.value = []
      selectedFrequencyDisciplineId.value = ''
      currentDashboardId.value = ''
      hasLoadedDashboard = true
      return
    }

    currentDashboardId.value = dashboard.id

    const loadedDisciplines = await apiRequest(`/api/v1/dashboards/${dashboard.id}/disciplines`)
    if (isStale()) return

    disciplines.value = loadedDisciplines
    if (!disciplines.value.some(discipline => discipline.id === selectedFrequencyDisciplineId.value)) {
      selectedFrequencyDisciplineId.value = disciplines.value[0]?.id || ''
    }

    await loadActivitiesForDashboard(dashboard.id)
    if (isStale()) return
    hasLoadedDashboard = true
  } catch (error) {
    if (isStale()) return
    // Em recarga, os dados já exibidos continuam valendo; o erro só aparece na primeira carga.
    if (!hasLoadedDashboard) {
      disciplines.value = []
      activities.value = []
      dashboardError.value = error.message || 'Não foi possível carregar o dashboard.'
    }
  } finally {
    if (!isStale()) dashboardLoading.value = false
  }
}

function clearSidebarAvatar() {
  if (sidebarAvatarUrl.value) URL.revokeObjectURL(sidebarAvatarUrl.value)
  sidebarAvatarUrl.value = ''
}

async function loadSidebarAvatar() {
  clearSidebarAvatar()
  if (!user.hasProfilePhoto || !user.profilePhotoUrl) return

  try {
    const photo = await sharedApiRequest(user.profilePhotoUrl, { as: 'blob', cache: 'no-store' })
    sidebarAvatarUrl.value = URL.createObjectURL(photo)
  } catch {
    // O avatar com as iniciais permanece como alternativa quando a imagem falhar.
  }
}


const pendingActivities = computed(() => activities.value.filter(activity => activity.status !== 'COMPLETED'))
const generalAverage = computed(() => averageOf(disciplines.value, 'average'))
const averageAttendance = computed(() => averageOf(disciplines.value, 'attendancePercentage'))
const completedActivities = computed(() => activities.value.filter(activity => activity.status === 'COMPLETED').length)
const overdueActivities = computed(() => pendingActivities.value.filter(activity => isOverdue(activity, todayIso.value)).length)
const completionPercentage = computed(() => activities.value.length
  ? Math.round((completedActivities.value / activities.value.length) * 100)
  : 0)
const dashboardActivities = computed(() => [...activities.value]
  .filter(activity => activity.status !== 'COMPLETED')
  .sort(compareByDueDate)
  .slice(0, 3))
// Avisos: provas e atividades pendentes cujo prazo entra na antecedência configurada em Preferências.
const reminderLimitIso = computed(() => reminderLimitIsoOf(currentDateTime.value, deadlineAlertDays.value))
const upcomingReminders = computed(() => upcomingRemindersOf(activities.value, todayIso.value, reminderLimitIso.value))
const overdueReminders = computed(() => overdueRemindersOf(activities.value, todayIso.value))
const visibleReminderGroups = computed(() => visibleReminderGroupsOf(overdueReminders.value, upcomingReminders.value))
const totalReminders = computed(() => overdueReminders.value.length + upcomingReminders.value.length)
const hiddenReminderCount = computed(() => totalReminders.value
  - visibleReminderGroups.value.reduce((sum, group) => sum + group.items.length, 0))
const nextClass = computed(() => nextClassOf(disciplines.value, currentDateTime.value))

function applyPreferences(raw) {
  const preferences = normalizePreferences(raw)
  attendanceAlertMargin.value = preferences.attendanceAlertMargin
  deadlineAlertDays.value = preferences.deadlineAlertDays
  startSection.value = preferences.startSection
}

// Carrega as preferências uma única vez e abre a tela inicial escolhida em Configurações,
// a menos que o estudante já tenha navegado. Sem preferências, o dashboard segue como início.
async function initializePreferences() {
  const preferences = await loadPreferences(apiRequest)
  if (!preferences.loaded) return

  applyPreferences(preferences)
  if (!routeSection && activeSection.value === 'dashboard') {
    activeSection.value = sectionFromPreference(preferences.startSection)
  }
}

onMounted(() => {
  loadDashboard()
  initializePreferences()
  loadSidebarAvatar()
  clockTimer = window.setInterval(() => {
    currentDateTime.value = new Date()
  }, 60000)
})
const selectedFrequencyDiscipline = computed(() => disciplines.value.find(
  discipline => discipline.id === selectedFrequencyDisciplineId.value,
) || disciplines.value[0] || null)
const selectedFrequencyDetails = computed(() => frequencyDetailsOf(
  selectedFrequencyDiscipline.value,
  attendanceAlertMargin.value,
))
watch(activeSection, section => {
  if (section === 'dashboard') loadDashboard()
  // Cada tela começa do topo; sem isto, no celular a tela nova abre na altura em que a anterior estava.
  window.scrollTo({ top: 0 })
})
watch(
  () => [user.hasProfilePhoto, user.profilePhotoUrl, user.updatedAt],
  loadSidebarAvatar,
)
onBeforeUnmount(() => {
  clearSidebarAvatar()
  window.clearInterval(clockTimer)
})
</script>
<template>
  <div class="dashboard-shell">
    <DashboardSidebar
      :name="user.name"
      :initial="userInitial"
      :avatar-url="sidebarAvatarUrl"
      :active-section="activeSection"
      @navigate="activeSection = $event"
      @logout="emit('logout')"
    />
    <main class="dashboard-main">
      <header v-if="activeSection === 'dashboard'" class="dashboard-topbar">
        <div class="dashboard-welcome">
          <span class="dashboard-welcome-label">Visão geral</span>
          <h1>Olá, {{ firstName }}! <span aria-hidden="true">👋</span></h1>
          <p>Acompanhe seus estudos e mantenha os próximos prazos sob controle.</p>
          <div class="dashboard-welcome-actions">
            <button type="button" @click="activeSection = 'activities'"><span aria-hidden="true">＋</span> Nova atividade</button>
            <button type="button" @click="activeSection = 'disciplines'">Ver disciplinas</button>
          </div>
        </div>
        <time class="dashboard-date" :datetime="todayIso">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <rect x="3" y="5" width="18" height="16" rx="2" />
            <path d="M7 3v4m10-4v4M3 10h18" />
          </svg>
          Hoje, {{ todayLabel }}
        </time>
      </header>
      <section v-if="activeSection === 'dashboard'" class="dashboard-overview" aria-labelledby="dashboard-empty-title">
        <DashboardSummaryCards
          :loading="dashboardLoading"
          :discipline-count="disciplines.length"
          :activity-count="activities.length"
          :pending-count="pendingActivities.length"
          :general-average="generalAverage"
          :average-attendance="averageAttendance"
        />
        <DashboardStatusHero
          v-if="dashboardLoading || dashboardError || disciplines.length === 0"
          :loading="dashboardLoading"
          :error="dashboardError"
          @retry="loadDashboard"
          @navigate="activeSection = $event"
        />
        <div v-else class="dashboard-content-grid">
          <div class="dashboard-main-column">
            <DashboardClassesPanel
              :next-class="nextClass"
              :now="currentDateTime"
              @navigate="activeSection = $event"
            />
            <DashboardActivitiesPanel
              :activities="dashboardActivities"
              :activities-error="activitiesError"
              :disciplines="disciplines"
              :today-iso="todayIso"
              @navigate="activeSection = $event"
              @retry="retryActivities"
            />
          </div>

          <aside class="dashboard-side-column">
            <DashboardRemindersPanel
              :groups="visibleReminderGroups"
              :hidden-count="hiddenReminderCount"
              :disciplines="disciplines"
              @navigate="activeSection = $event"
            />
            <DashboardFrequencyPanel
              v-if="selectedFrequencyDetails"
              :details="selectedFrequencyDetails"
              :disciplines="disciplines"
              :selected-discipline-id="selectedFrequencyDisciplineId"
              @navigate="activeSection = $event"
              @select="selectedFrequencyDisciplineId = $event"
            />
            <DashboardProgressPanel
              :completion-percentage="completionPercentage"
              :completed-count="completedActivities"
              :total-count="activities.length"
              :pending-count="pendingActivities.length"
              :overdue-count="overdueActivities"
            />
          </aside>
        </div>
        <DashboardGuideCards
          v-if="!dashboardLoading && disciplines.length === 0"
          @navigate="activeSection = $event"
        />
        <p v-if="disciplines.length === 0" class="dashboard-tip">
          <span aria-hidden="true">💡</span>
          <strong>Dica:</strong> quanto mais você usar o AcadOrganize, mais completo será o seu dashboard.
        </p>
      </section>

      <DisciplinesScreen
        v-if="activeSection === 'disciplines'"
      />

      <ActivitiesScreen
        v-if="activeSection === 'activities'"
        @navigate="activeSection = $event"
      />
      <ProvasScreen
        v-if="activeSection === 'exams'"
        @navigate="activeSection = $event"
      />
      <FrequencyScreen
        v-if="activeSection === 'frequency'"
      />
      <GradesScreen
        v-if="activeSection === 'grades'"
        @navigate="activeSection = $event"
        @session-expired="emit('logout', 'session-expired')"
      />
      <SimulatorScreen
        v-if="activeSection === 'simulator'"
        @navigate="activeSection = $event"
      />
      <ProfileScreen
        v-if="activeSection === 'profile'"
        :user="user"
        @updated="emit('user-updated', $event)"
      />
      <SettingsScreen
        v-if="activeSection === 'settings'"
        @preferences-updated="applyPreferences"
        @account-deleted="emit('logout', 'account-deleted')"
      />
      <CalendarScreen
        v-if="activeSection === 'calendar'"
      />
    </main>
  </div>
</template>
