<script setup>
import { apiRequest } from '../../shared/http/apiRequest.js'
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import CalendarCategoryFilter from './CalendarCategoryFilter.vue'
import CalendarDayView from './CalendarDayView.vue'
import CalendarEventModal from './CalendarEventModal.vue'
import CalendarMiniMonth from './CalendarMiniMonth.vue'
import CalendarMonthGrid from './CalendarMonthGrid.vue'
import CalendarToolbar from './CalendarToolbar.vue'
import CalendarUpcomingCard from './CalendarUpcomingCard.vue'
import DeleteCalendarEventModal from './DeleteCalendarEventModal.vue'
import {
  CATEGORIES,
  UPCOMING_DISPLAY_LIMIT,
  addDays,
  addMonths,
  appendCategoryParams,
  buildCalendarDay,
  buildMiniCalendarDay,
  categoryClass,
  displayedEventsOf,
  endOfDay,
  eventSaveModeOf,
  eventsOfDay,
  monthGridOf,
  monthLabelOf,
  periodLabelOf,
  upcomingCombinedOf,
  upcomingQueryOf,
  visibleDaysOf,
} from './calendarPresentation.js'
import './calendar.css'
import { startOfDay, toLocalDateTimeIso as toLocalIso } from '../../shared/date/localDate.js'
import { loadActiveDashboard } from '../../shared/dashboards/useActiveDashboard.js'

const dashboardId = ref('')
const disciplines = ref([])
const events = ref([])
const examActivities = ref([])
const upcomingEvents = ref([])
const upcomingShowAll = ref(false)
const loading = ref(true)
const requestError = ref('')
const feedback = ref('')

// Cada seção carregada de forma independente (eventos, próximos eventos, provas) tem seu
// próprio erro e sua própria marca de "dados desatualizados", para que uma falha em uma
// não esconda nem misture com o estado das outras.
const eventsError = ref('')
const eventsStale = ref(false)
const upcomingError = ref('')
const upcomingStale = ref(false)
const examError = ref('')
const examStale = ref(false)

// Contadores de sequência: evitam que uma resposta antiga (de uma navegação ou filtro
// já abandonado) sobrescreva o resultado de uma requisição mais recente.
let eventsRequestSeq = 0
let upcomingRequestSeq = 0
let examRequestSeq = 0

const savingEvent = ref(false)
const deletingEvent = ref(false)
const modalServerError = ref('')
const deleteServerError = ref('')

const viewMode = ref('month')
const referenceDate = ref(startOfDay(new Date()))
const selectedCategories = ref(CATEGORIES.map(category => category.value))

const showEventModal = ref(false)
const editingEvent = ref(null)
const modalDefaultDate = ref('')
const eventToDelete = ref(null)

// "Hoje" precisa se mover sozinho depois da meia-noite, então é derivado de um relógio
// que avança em segundo plano em vez de uma data fixa calculada uma vez só.
const now = ref(Date.now())
let clockTimer = null

onMounted(() => {
  clockTimer = setInterval(() => {
    now.value = Date.now()
  }, 30000)
})

onUnmounted(() => {
  if (clockTimer) clearInterval(clockTimer)
})

const today = computed(() => startOfDay(new Date(now.value)))

const monthGrid = computed(() => monthGridOf(referenceDate.value))

const queryKey = computed(() => toLocalIso(monthGrid.value[0]))

const visibleDays = computed(() => visibleDaysOf(viewMode.value, referenceDate.value, monthGrid.value))

const periodLabel = computed(() => periodLabelOf(viewMode.value, referenceDate.value))

const miniMonthLabel = computed(() => monthLabelOf(referenceDate.value))

const displayedEvents = computed(() => displayedEventsOf(events.value, examActivities.value, disciplines.value, selectedCategories.value))

// "Próximos eventos" também respeita o filtro de categorias e inclui as provas
// (Atividades do tipo EXAM), que antes só apareciam no grid do calendário.
const upcomingCombined = computed(() => upcomingCombinedOf(
  upcomingEvents.value,
  examActivities.value,
  disciplines.value,
  selectedCategories.value,
  today.value,
))

const upcomingDisplayed = computed(() => (
  upcomingShowAll.value
    ? upcomingCombined.value
    : upcomingCombined.value.slice(0, UPCOMING_DISPLAY_LIMIT)
))

const calendarDays = computed(() => visibleDays.value.map(
  day => buildCalendarDay(day, displayedEvents.value, referenceDate.value, today.value, viewMode.value),
))

const miniCalendarDays = computed(() => monthGrid.value.map(
  day => buildMiniCalendarDay(day, displayedEvents.value, referenceDate.value, today.value),
))

const dayViewEvents = computed(() => eventsOfDay(displayedEvents.value, referenceDate.value))

function eventsPath(suffix = '') {
  return `/api/v1/dashboards/${dashboardId.value}/calendar/events${suffix}`
}

async function loadEvents() {
  if (!dashboardId.value) return

  const requestId = ++eventsRequestSeq

  // Nenhuma categoria marcada: a tela fica vazia sem precisar perguntar nada à API,
  // porque o parâmetro vazio significa "sem filtro" do lado do servidor.
  if (selectedCategories.value.length === 0) {
    events.value = []
    eventsError.value = ''
    eventsStale.value = false
    return
  }

  const params = new URLSearchParams()
  params.set('start', toLocalIso(monthGrid.value[0]))
  params.set('end', toLocalIso(endOfDay(monthGrid.value[monthGrid.value.length - 1])))
  appendCategoryParams(params, selectedCategories.value)

  try {
    const result = await apiRequest(`${eventsPath()}?${params}`)

    // Uma resposta antiga (de um mês ou filtro já abandonados) nunca deve sobrescrever
    // o resultado mais recente.
    if (requestId !== eventsRequestSeq) return

    events.value = result
    eventsError.value = ''
    eventsStale.value = false
  } catch (error) {
    if (requestId !== eventsRequestSeq) return

    // Mantém os eventos antigos visíveis, mas marcados como possivelmente desatualizados,
    // em vez de esvaziar a tela e parecer que não há nenhum evento.
    eventsError.value = error.message || 'Não foi possível carregar os eventos do calendário.'
    eventsStale.value = events.value.length > 0
  }
}

async function loadUpcoming() {
  if (!dashboardId.value) return

  const requestId = ++upcomingRequestSeq

  // Mesma regra de loadEvents: sem categoria marcada não há o que perguntar à API.
  if (selectedCategories.value.length === 0) {
    upcomingEvents.value = []
    upcomingError.value = ''
    upcomingStale.value = false
    return
  }

  try {
    const result = await apiRequest(eventsPath(`/upcoming?${upcomingQueryOf(selectedCategories.value)}`))
    if (requestId !== upcomingRequestSeq) return

    upcomingEvents.value = result
    upcomingError.value = ''
    upcomingStale.value = false
  } catch (error) {
    if (requestId !== upcomingRequestSeq) return

    upcomingError.value = error.message || 'Não foi possível carregar os próximos eventos.'
    upcomingStale.value = upcomingEvents.value.length > 0
  }
}

// Provas ficam compartilhadas com a tela Provas: aqui elas são carregadas como Atividades
// do tipo EXAM e exibidas no calendário, sem duplicar o que já existe como CalendarEvent.
async function loadExamActivities() {
  if (!dashboardId.value) return

  const requestId = ++examRequestSeq

  try {
    const result = await apiRequest(`/api/v1/dashboards/${dashboardId.value}/activities?type=EXAM`)
    if (requestId !== examRequestSeq) return

    examActivities.value = result
    examError.value = ''
    examStale.value = false
  } catch (error) {
    if (requestId !== examRequestSeq) return

    examError.value = error.message || 'Não foi possível carregar as provas.'
    examStale.value = examActivities.value.length > 0
  }
}

async function loadCalendar() {
  loading.value = true
  requestError.value = ''

  try {
    let dashboard = await loadActiveDashboard(apiRequest)

    // Conta nova que abre o Calendário antes de Disciplinas ainda não tem dashboard.
    // Mesmo caminho usado na tela de disciplinas.
    if (!dashboard) {
      dashboard = await apiRequest('/api/v1/dashboards', {
        method: 'POST',
        body: JSON.stringify({ name: 'Organização acadêmica', status: 'ACTIVE' }),
      })
    }

    dashboardId.value = dashboard.id
    disciplines.value = await apiRequest(`/api/v1/dashboards/${dashboard.id}/disciplines`)
    await Promise.all([loadEvents(), loadUpcoming(), loadExamActivities()])
  } catch (error) {
    requestError.value = error.message || 'Não foi possível carregar o calendário.'
  } finally {
    loading.value = false
  }
}

onMounted(loadCalendar)

watch([queryKey, selectedCategories], loadEvents)
watch(selectedCategories, loadUpcoming)

function goToToday() {
  referenceDate.value = startOfDay(new Date())
}

function movePeriod(direction) {
  if (viewMode.value === 'day') {
    referenceDate.value = addDays(referenceDate.value, direction)
    return
  }

  if (viewMode.value === 'week') {
    referenceDate.value = addDays(referenceDate.value, direction * 7)
    return
  }

  referenceDate.value = addMonths(referenceDate.value, direction)
}

function moveMiniMonth(direction) {
  referenceDate.value = addMonths(referenceDate.value, direction)
}

function selectDay(date) {
  referenceDate.value = startOfDay(date)
}

// O "mais N" da célula cheia: abre aquele dia na visão Dia, onde a lista não tem limite.
function openDayView(date) {
  referenceDate.value = startOfDay(date)
  viewMode.value = 'day'
}

function toggleCategory(value) {
  selectedCategories.value = selectedCategories.value.includes(value)
    ? selectedCategories.value.filter(category => category !== value)
    : [...selectedCategories.value, value]
}

function toggleUpcomingLimit() {
  upcomingShowAll.value = !upcomingShowAll.value
}

function openNewEventModal(date = referenceDate.value) {
  const start = startOfDay(date)
  start.setHours(8, 0, 0, 0)

  feedback.value = ''
  modalServerError.value = ''
  editingEvent.value = null
  modalDefaultDate.value = toLocalIso(start).slice(0, 16)
  showEventModal.value = true
}

function openEditEventModal(event) {
  feedback.value = ''
  modalServerError.value = ''
  editingEvent.value = event
  modalDefaultDate.value = ''
  showEventModal.value = true
}

function closeEventModal() {
  showEventModal.value = false
  editingEvent.value = null
  modalServerError.value = ''
}

function createExamActivity(formData) {
  return apiRequest(`/api/v1/dashboards/${dashboardId.value}/disciplines/${formData.disciplineId}/activities`, {
    method: 'POST',
    body: JSON.stringify({
      title: formData.title,
      description: formData.description,
      dueDate: formData.startsAt.slice(0, 10),
      status: 'PENDING',
      type: 'EXAM',
    }),
  })
}

// Prova é sempre uma Atividade do tipo EXAM, não um CalendarEvent — assim ela é a mesma prova
// mostrada na tela Provas. Isso vale para prova nova e para evento que passa a ser prova ao ser
// editado (inclusive eventos de prova antigos): a atividade é criada e o evento sai do calendário.
async function saveEvent(formData) {
  if (savingEvent.value) return

  modalServerError.value = ''
  savingEvent.value = true

  const eventId = editingEvent.value?.id
  const mode = eventSaveModeOf(Boolean(eventId), formData.category)

  if (mode !== 'event') {
    let created = false
    try {
      await createExamActivity(formData)
      created = true
      // Só apaga o evento depois que a prova existe. Se esta exclusão falhar, a tela já esconde o
      // evento de prova repetido (withoutDuplicateExamEvents), então nada some nem aparece em dobro.
      if (mode === 'convert-to-exam') await apiRequest(eventsPath(`/${eventId}`), { method: 'DELETE' })

      feedback.value = mode === 'convert-to-exam' ? 'Evento movido para Provas.' : 'Prova criada com sucesso.'
      closeEventModal()
      await Promise.all([loadExamActivities(), loadEvents(), loadUpcoming()])
    } catch (error) {
      if (created) {
        feedback.value = 'A prova foi criada, mas o evento antigo não pôde ser removido do calendário.'
        closeEventModal()
        await Promise.all([loadExamActivities(), loadEvents(), loadUpcoming()])
      } else {
        // Erro fica visível dentro do modal (perto do formulário) e o modal continua aberto.
        modalServerError.value = error.message || 'Não foi possível criar a prova.'
      }
    } finally {
      savingEvent.value = false
    }

    return
  }

  try {
    await apiRequest(eventId ? eventsPath(`/${eventId}`) : eventsPath(), {
      method: eventId ? 'PUT' : 'POST',
      body: JSON.stringify(formData),
    })

    feedback.value = eventId ? 'Evento atualizado com sucesso.' : 'Evento criado com sucesso.'
    closeEventModal()
    await Promise.all([loadEvents(), loadUpcoming()])
  } catch (error) {
    modalServerError.value = error.message || 'Não foi possível salvar o evento.'
  } finally {
    savingEvent.value = false
  }
}

function askToDeleteEvent() {
  eventToDelete.value = editingEvent.value
  deleteServerError.value = ''
  showEventModal.value = false
}

function closeDeleteModal() {
  eventToDelete.value = null
  editingEvent.value = null
  deleteServerError.value = ''
}

async function confirmDeleteEvent() {
  if (!eventToDelete.value || deletingEvent.value) return

  deleteServerError.value = ''
  deletingEvent.value = true

  try {
    await apiRequest(eventsPath(`/${eventToDelete.value.id}`), { method: 'DELETE' })
    feedback.value = 'Evento excluído com sucesso.'
    closeDeleteModal()
    await Promise.all([loadEvents(), loadUpcoming()])
  } catch (error) {
    // Fica visível dentro do próprio modal de confirmação, que continua aberto.
    deleteServerError.value = error.message || 'Não foi possível excluir o evento.'
  } finally {
    deletingEvent.value = false
  }
}
</script>

<template>
  <div class="calendar-screen">
    <header class="calendar-topbar">
      <div class="calendar-heading">
        <span class="calendar-heading-icon" aria-hidden="true">
          <svg viewBox="0 0 24 24">
            <rect x="3" y="5" width="18" height="16" rx="2" />
            <path d="M7 3v4m10-4v4M3 10h18" />
          </svg>
        </span>
        <div>
          <h1>Calendário</h1>
          <p>Visualize seus compromissos e prazos.</p>
        </div>
      </div>

      <button class="new-event-button" type="button" @click="openNewEventModal()">
        <span aria-hidden="true">＋</span>
        Novo evento
      </button>
    </header>

    <div v-if="requestError" class="calendar-alert" role="alert">
      <span>{{ requestError }}</span>
      <button type="button" class="retry-button" @click="loadCalendar">Tentar novamente</button>
    </div>
    <p v-if="feedback" class="calendar-feedback" role="status">{{ feedback }}</p>

    <div v-if="examError" class="calendar-alert" role="alert">
      <span>{{ examError }}<template v-if="examStale"> As provas exibidas podem estar desatualizadas.</template></span>
      <button type="button" class="retry-button" @click="loadExamActivities">Tentar novamente</button>
    </div>

    <div class="calendar-layout">
      <section class="calendar-board" aria-label="Calendário de eventos">
        <CalendarToolbar
          :period-label="periodLabel"
          :view-mode="viewMode"
          @today="goToToday"
          @move="movePeriod"
          @change-view="viewMode = $event"
        />

        <article v-if="loading" class="app-state-card is-loading" aria-live="polite"><span class="app-spinner" aria-hidden="true"></span><p>Carregando seu calendário…</p></article>

        <div v-else-if="eventsError && !events.length && !eventsStale" class="calendar-section-error" role="alert">
          <p>{{ eventsError }}</p>
          <button type="button" class="retry-button" @click="loadEvents">Tentar novamente</button>
        </div>

        <template v-else>
        <div v-if="eventsError" class="calendar-section-error is-inline" role="alert">
          <p>{{ eventsError }}<template v-if="eventsStale"> Os eventos exibidos podem estar desatualizados.</template></p>
          <button type="button" class="retry-button" @click="loadEvents">Tentar novamente</button>
        </div>

        <CalendarDayView
          v-if="viewMode === 'day'"
          :period-label="periodLabel"
          :events="dayViewEvents"
          @edit-event="openEditEventModal"
        />

        <CalendarMonthGrid
          v-else
          :days="calendarDays"
          :view-mode="viewMode"
          @create-event="openNewEventModal"
          @edit-event="openEditEventModal"
          @open-day="openDayView"
        />
        </template>

        <footer class="calendar-legend">
          <span v-for="category in CATEGORIES" :key="category.value" :class="['legend-item', categoryClass(category.value)]">
            <span class="legend-dot" aria-hidden="true"></span>
            {{ category.label }}
          </span>
        </footer>
      </section>

      <aside class="calendar-side">
        <CalendarUpcomingCard
          :events="upcomingDisplayed"
          :total-count="upcomingCombined.length"
          :show-all="upcomingShowAll"
          :error="upcomingError"
          :stale="upcomingStale"
          :today="today"
          @toggle-limit="toggleUpcomingLimit"
          @retry="loadUpcoming"
          @edit-event="openEditEventModal"
        />

        <CalendarMiniMonth
          :label="miniMonthLabel"
          :days="miniCalendarDays"
          @move="moveMiniMonth"
          @select-day="selectDay"
        />

        <CalendarCategoryFilter
          :selected="selectedCategories"
          @toggle="toggleCategory"
        />
      </aside>
    </div>

    <CalendarEventModal
      v-if="showEventModal"
      :event="editingEvent"
      :disciplines="disciplines"
      :default-date="modalDefaultDate"
      :saving="savingEvent"
      :server-error="modalServerError"
      @close="closeEventModal"
      @save="saveEvent"
      @delete="askToDeleteEvent"
    />

    <DeleteCalendarEventModal
      v-if="eventToDelete"
      :event-title="eventToDelete.title"
      :deleting="deletingEvent"
      :server-error="deleteServerError"
      @close="closeDeleteModal"
      @confirm="confirmDeleteEvent"
    />
  </div>
</template>
