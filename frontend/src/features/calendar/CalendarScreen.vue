<script setup>
import { apiRequest } from '../../shared/http/apiRequest.js'
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import CalendarEventModal from './CalendarEventModal.vue'
import DeleteCalendarEventModal from './DeleteCalendarEventModal.vue'
import { parseLocalDateTime, startOfDay, toLocalDateTimeIso as toLocalIso } from '../../shared/date/localDate.js'
import { loadActiveDashboard } from '../../shared/dashboards/useActiveDashboard.js'

const categories = [
  { value: 'CLASS', label: 'Aulas' },
  { value: 'ACTIVITY', label: 'Atividades' },
  { value: 'EXAM', label: 'Provas' },
  { value: 'ASSIGNMENT', label: 'Trabalhos' },
  { value: 'OTHER', label: 'Outros' },
]

const weekDayLabels = ['Dom', 'Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb']
const miniWeekDayLabels = ['D', 'S', 'T', 'Q', 'Q', 'S', 'S']

const dashboardId = ref('')
const disciplines = ref([])
const events = ref([])
const examActivities = ref([])
const upcomingEvents = ref([])
const UPCOMING_FETCH_LIMIT = 100
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
const selectedCategories = ref(categories.map(category => category.value))

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

function pad(value) {
  return String(value).padStart(2, '0')
}

function endOfDay(date) {
  const copy = new Date(date)
  copy.setHours(23, 59, 59, 0)
  return copy
}

function addDays(date, amount) {
  const copy = new Date(date)
  copy.setDate(copy.getDate() + amount)
  return copy
}

function addMonths(date, amount) {
  const copy = new Date(date.getFullYear(), date.getMonth() + amount, 1)
  return copy
}

function startOfWeek(date) {
  return addDays(startOfDay(date), -date.getDay())
}

function isSameDay(first, second) {
  return first.getFullYear() === second.getFullYear()
    && first.getMonth() === second.getMonth()
    && first.getDate() === second.getDate()
}

function capitalize(text) {
  return text.charAt(0).toUpperCase() + text.slice(1)
}

function dayCreateLabel(date) {
  const formatted = new Intl.DateTimeFormat('pt-BR', {
    day: 'numeric',
    month: 'long',
    year: 'numeric',
  }).format(date)

  return `Criar evento em ${formatted}`
}

function formatTime(value) {
  return value ? value.slice(11, 16) : ''
}

function categoryClass(category) {
  return `is-${category.toLowerCase()}`
}

// A consulta sempre cobre o mês inteiro na tela (com os dias vizinhos que completam
// as semanas). As visões Semana e Dia são recortes desse mesmo resultado, então
// trocar de visão não custa uma requisição nova e o mini calendário nunca fica sem pontos.
const monthGrid = computed(() => {
  const firstOfMonth = new Date(referenceDate.value.getFullYear(), referenceDate.value.getMonth(), 1)
  const lastOfMonth = new Date(referenceDate.value.getFullYear(), referenceDate.value.getMonth() + 1, 0)
  const start = startOfWeek(firstOfMonth)
  const end = addDays(startOfWeek(lastOfMonth), 6)
  const total = Math.round((end - start) / 86400000) + 1

  return Array.from({ length: total }, (_, index) => addDays(start, index))
})

const queryKey = computed(() => toLocalIso(monthGrid.value[0]))

const visibleDays = computed(() => {
  if (viewMode.value === 'day') {
    return [referenceDate.value]
  }

  if (viewMode.value === 'week') {
    const start = startOfWeek(referenceDate.value)
    return Array.from({ length: 7 }, (_, index) => addDays(start, index))
  }

  return monthGrid.value
})

const weeks = computed(() => {
  const result = []

  for (let index = 0; index < monthGrid.value.length; index += 7) {
    result.push(monthGrid.value.slice(index, index + 7))
  }

  return result
})

const periodLabel = computed(() => {
  if (viewMode.value === 'day') {
    return capitalize(new Intl.DateTimeFormat('pt-BR', {
      weekday: 'long',
      day: 'numeric',
      month: 'long',
      year: 'numeric',
    }).format(referenceDate.value))
  }

  if (viewMode.value === 'week') {
    const start = startOfWeek(referenceDate.value)
    const end = addDays(start, 6)
    const dayMonth = new Intl.DateTimeFormat('pt-BR', { day: 'numeric', month: 'short' })
    return `${dayMonth.format(start)} – ${dayMonth.format(end)} de ${start.getFullYear()}`
  }

  return capitalize(new Intl.DateTimeFormat('pt-BR', {
    month: 'long',
    year: 'numeric',
  }).format(referenceDate.value))
})

const miniMonthLabel = computed(() => capitalize(new Intl.DateTimeFormat('pt-BR', {
  month: 'long',
  year: 'numeric',
}).format(referenceDate.value)))

function disciplineName(id) {
  return disciplines.value.find(discipline => discipline.id === id)?.name || ''
}

// Provas viram Atividades (tipo EXAM) e passam a ser compartilhadas com a tela Provas.
// Aqui elas entram como itens somente leitura, no dia da entrega (dueDate), a partir
// das 8h — hora fixa só para posicionar o item no dia certo, sem hora real de prova.
function examActivityToEvent(activity) {
  return {
    id: `activity-exam-${activity.id}`,
    title: activity.title,
    description: activity.description || '',
    category: 'EXAM',
    startsAt: `${activity.dueDate}T08:00:00`,
    endsAt: null,
    disciplineId: activity.disciplineId,
    disciplineName: disciplineName(activity.disciplineId),
    disciplineDeleted: false,
    isExamActivity: true,
  }
}

// Eventos EXAM antigos (criados antes desta mudança, quando prova era só um CalendarEvent)
// que já correspondem a uma Atividade de prova não devem aparecer duplicados.
function isDuplicateExamEvent(event, activity) {
  return event.category === 'EXAM'
    && event.disciplineId === activity.disciplineId
    && event.title === activity.title
    && event.startsAt.slice(0, 10) === activity.dueDate
}

const displayedEvents = computed(() => {
  const duplicateIds = new Set()

  examActivities.value.forEach(activity => {
    events.value.forEach(event => {
      if (isDuplicateExamEvent(event, activity)) duplicateIds.add(event.id)
    })
  })

  return [
    ...events.value.filter(event => !duplicateIds.has(event.id)),
    ...examActivities.value.map(examActivityToEvent),
  ]
})

// "Próximos eventos" também respeita o filtro de categorias e inclui as provas
// (Atividades do tipo EXAM), que antes só apareciam no grid do calendário.
const UPCOMING_DISPLAY_LIMIT = 5

const upcomingCombined = computed(() => {
  const duplicateIds = new Set()

  examActivities.value.forEach(activity => {
    upcomingEvents.value.forEach(event => {
      if (isDuplicateExamEvent(event, activity)) duplicateIds.add(event.id)
    })
  })

  const todayIso = toLocalIso(today.value).slice(0, 10)

  const examAsUpcoming = examActivities.value
    .filter(activity => activity.dueDate >= todayIso)
    .map(examActivityToEvent)

  return [
    ...upcomingEvents.value.filter(event => !duplicateIds.has(event.id)),
    ...examAsUpcoming,
  ]
    .filter(event => selectedCategories.value.includes(event.category))
    .sort((first, second) => first.startsAt.localeCompare(second.startsAt))
})

const upcomingDisplayed = computed(() => (
  upcomingShowAll.value
    ? upcomingCombined.value
    : upcomingCombined.value.slice(0, UPCOMING_DISPLAY_LIMIT)
))

function eventsOfDay(day) {
  const dayStart = startOfDay(day)
  const dayEnd = endOfDay(day)

  // Mesma regra de sobreposição da API: o evento aparece no dia enquanto não terminou.
  return displayedEvents.value
    .filter(event => {
      const start = parseLocalDateTime(event.startsAt)
      if (!start) return false
      const end = (event.endsAt && parseLocalDateTime(event.endsAt)) || start
      return start <= dayEnd && end >= dayStart
    })
    .sort((first, second) => first.startsAt.localeCompare(second.startsAt))
}

function buildDay(day) {
  return {
    date: day,
    key: toLocalIso(day),
    number: day.getDate(),
    isToday: isSameDay(day, today.value),
    isCurrentMonth: day.getMonth() === referenceDate.value.getMonth(),
    isSelected: isSameDay(day, referenceDate.value),
    events: eventsOfDay(day),
  }
}

const calendarDays = computed(() => visibleDays.value.map(buildDay))

const miniCalendarDays = computed(() => monthGrid.value.map(day => {
  const dayEvents = eventsOfDay(day)
  const dots = [...new Set(dayEvents.map(event => event.category))].slice(0, 3)

  return {
    date: day,
    key: toLocalIso(day),
    number: day.getDate(),
    isToday: isSameDay(day, today.value),
    isCurrentMonth: day.getMonth() === referenceDate.value.getMonth(),
    isSelected: isSameDay(day, referenceDate.value),
    dots,
  }
}))

const dayViewEvents = computed(() => eventsOfDay(referenceDate.value))

function upcomingDayLabel(value) {
  const date = startOfDay(parseLocalDateTime(value) ?? new Date())

  if (isSameDay(date, today.value)) {
    return 'Hoje'
  }

  if (isSameDay(date, addDays(today.value, 1))) {
    return 'Amanhã'
  }

  return `${pad(date.getDate())}/${pad(date.getMonth() + 1)}`
}

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

  if (selectedCategories.value.length < categories.length) {
    selectedCategories.value.forEach(category => params.append('categories', category))
  }

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

  try {
    const result = await apiRequest(eventsPath(`/upcoming?limit=${UPCOMING_FETCH_LIMIT}`))
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

// Prova nova (sem edição em andamento) vira Atividade do tipo EXAM, não um CalendarEvent —
// assim ela passa a ser a mesma prova mostrada na tela Provas. Editar/excluir eventos EXAM
// já existentes continua no fluxo antigo, pois esses registros continuam sendo CalendarEvents.
async function saveEvent(formData) {
  if (savingEvent.value) return

  modalServerError.value = ''
  savingEvent.value = true

  const eventId = editingEvent.value?.id

  if (!eventId && formData.category === 'EXAM') {
    try {
      await apiRequest(`/api/v1/dashboards/${dashboardId.value}/disciplines/${formData.disciplineId}/activities`, {
        method: 'POST',
        body: JSON.stringify({
          title: formData.title,
          description: formData.description,
          dueDate: formData.startsAt.slice(0, 10),
          status: 'PENDING',
          type: 'EXAM',
        }),
      })

      feedback.value = 'Prova criada com sucesso.'
      closeEventModal()
      await loadExamActivities()
    } catch (error) {
      // Erro fica visível dentro do modal (perto do formulário) e o modal continua aberto.
      modalServerError.value = error.message || 'Não foi possível criar a prova.'
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
        <div class="calendar-toolbar">
          <button class="today-button" type="button" @click="goToToday">Hoje</button>

          <div class="period-navigation">
            <button type="button" aria-label="Período anterior" @click="movePeriod(-1)">‹</button>
            <strong>{{ periodLabel }}</strong>
            <button type="button" aria-label="Próximo período" @click="movePeriod(1)">›</button>
          </div>

          <div class="view-switch" role="group" aria-label="Modo de visualização">
            <button type="button" :class="{ active: viewMode === 'month' }" @click="viewMode = 'month'">Mês</button>
            <button type="button" :class="{ active: viewMode === 'week' }" @click="viewMode = 'week'">Semana</button>
            <button type="button" :class="{ active: viewMode === 'day' }" @click="viewMode = 'day'">Dia</button>
          </div>
        </div>

        <p v-if="loading" class="calendar-loading">Carregando seu calendário…</p>

        <div v-else-if="eventsError && !events.length && !eventsStale" class="calendar-section-error" role="alert">
          <p>{{ eventsError }}</p>
          <button type="button" class="retry-button" @click="loadEvents">Tentar novamente</button>
        </div>

        <template v-else>
        <div v-if="eventsError" class="calendar-section-error is-inline" role="alert">
          <p>{{ eventsError }}<template v-if="eventsStale"> Os eventos exibidos podem estar desatualizados.</template></p>
          <button type="button" class="retry-button" @click="loadEvents">Tentar novamente</button>
        </div>

        <template v-if="viewMode === 'day'">
          <div class="day-view">
            <h2>{{ periodLabel }}</h2>

            <ul v-if="dayViewEvents.length" class="day-view-list">
              <li v-for="event in dayViewEvents" :key="event.id">
                <component
                  :is="event.isExamActivity ? 'div' : 'button'"
                  :type="event.isExamActivity ? undefined : 'button'"
                  :class="['day-view-event', categoryClass(event.category), { 'is-readonly': event.isExamActivity }]"
                  @click="event.isExamActivity ? null : openEditEventModal(event)"
                >
                  <span class="day-view-time">
                    {{ formatTime(event.startsAt) }}<template v-if="event.endsAt"> – {{ formatTime(event.endsAt) }}</template>
                  </span>
                  <span class="day-view-body">
                    <strong>{{ event.title }}<small v-if="event.isExamActivity" class="readonly-tag">Prova</small></strong>
                    <small v-if="event.disciplineName" :class="{ 'is-deleted': event.disciplineDeleted }">{{ event.disciplineName }}</small>
                    <small v-if="event.description" class="day-view-description">{{ event.description }}</small>
                  </span>
                </component>
              </li>
            </ul>

            <p v-else class="calendar-empty">Nenhum evento neste dia.</p>
          </div>
        </template>

        <template v-else>
          <div class="calendar-weekdays" aria-hidden="true">
            <span v-for="label in weekDayLabels" :key="label">{{ label }}</span>
          </div>

          <div :class="['calendar-grid', `is-${viewMode}`]">
            <div
              v-for="day in calendarDays"
              :key="day.key"
              :class="['calendar-day', { 'is-outside': !day.isCurrentMonth, 'is-today': day.isToday }]"
            >
              <button
                type="button"
                class="calendar-day-create"
                :aria-label="dayCreateLabel(day.date)"
                @click="openNewEventModal(day.date)"
              ></button>

              <span class="calendar-day-number">{{ day.number }}</span>

              <ul class="calendar-day-events">
                <li v-for="event in day.events" :key="event.id">
                  <component
                    :is="event.isExamActivity ? 'div' : 'button'"
                    :type="event.isExamActivity ? undefined : 'button'"
                    :class="['calendar-event', categoryClass(event.category), { 'is-readonly': event.isExamActivity }]"
                    @click="event.isExamActivity ? null : openEditEventModal(event)"
                  >
                    <strong>{{ event.title }}<small v-if="event.isExamActivity" class="readonly-tag">Prova</small></strong>
                    <small v-if="event.disciplineName" :class="{ 'is-deleted': event.disciplineDeleted }">{{ event.disciplineName }}</small>
                    <small class="calendar-event-time">{{ formatTime(event.startsAt) }}</small>
                  </component>
                </li>
              </ul>
            </div>
          </div>
        </template>
        </template>

        <footer class="calendar-legend">
          <span v-for="category in categories" :key="category.value" :class="['legend-item', categoryClass(category.value)]">
            <span class="legend-dot" aria-hidden="true"></span>
            {{ category.label }}
          </span>
        </footer>
      </section>

      <aside class="calendar-side">
        <section class="side-card">
          <header class="side-card-header">
            <h2>Próximos eventos</h2>
            <button
              v-if="upcomingCombined.length > UPCOMING_DISPLAY_LIMIT"
              type="button"
              class="side-card-action"
              @click="toggleUpcomingLimit"
            >
              {{ upcomingShowAll ? 'Ver menos' : 'Ver todos' }}
            </button>
          </header>

          <div v-if="upcomingError" class="calendar-section-error is-inline" role="alert">
            <p>{{ upcomingError }}<template v-if="upcomingStale"> Esta lista pode estar desatualizada.</template></p>
            <button type="button" class="retry-button" @click="loadUpcoming">Tentar novamente</button>
          </div>

          <ul v-if="upcomingDisplayed.length" class="upcoming-list">
            <li v-for="event in upcomingDisplayed" :key="event.id">
              <component
                :is="event.isExamActivity ? 'div' : 'button'"
                :type="event.isExamActivity ? undefined : 'button'"
                :class="['upcoming-item', { 'is-readonly': event.isExamActivity }]"
                @click="event.isExamActivity ? null : openEditEventModal(event)"
              >
                <span :class="['upcoming-dot', categoryClass(event.category)]" aria-hidden="true"></span>
                <span class="upcoming-body">
                  <strong>{{ event.title }}<small v-if="event.isExamActivity" class="readonly-tag">Prova</small></strong>
                  <small v-if="event.disciplineName" :class="{ 'is-deleted': event.disciplineDeleted }">{{ event.disciplineName }}</small>
                </span>
                <span class="upcoming-when">
                  <strong>{{ upcomingDayLabel(event.startsAt) }}</strong>
                  <small>{{ formatTime(event.startsAt) }}</small>
                </span>
              </component>
            </li>
          </ul>

          <p v-else-if="!upcomingError" class="calendar-empty">Nenhum evento programado.</p>

          <p v-if="upcomingCombined.length > upcomingDisplayed.length" class="upcoming-count-hint">
            Mostrando {{ upcomingDisplayed.length }} de {{ upcomingCombined.length }}
          </p>
        </section>

        <section class="side-card">
          <header class="mini-calendar-header">
            <button type="button" aria-label="Mês anterior" @click="moveMiniMonth(-1)">‹</button>
            <strong>{{ miniMonthLabel }}</strong>
            <button type="button" aria-label="Próximo mês" @click="moveMiniMonth(1)">›</button>
          </header>

          <div class="mini-calendar-weekdays" aria-hidden="true">
            <span v-for="(label, index) in miniWeekDayLabels" :key="index">{{ label }}</span>
          </div>

          <div class="mini-calendar-grid">
            <button
              v-for="day in miniCalendarDays"
              :key="day.key"
              type="button"
              :class="['mini-calendar-day', {
                'is-outside': !day.isCurrentMonth,
                'is-today': day.isToday,
                'is-selected': day.isSelected,
              }]"
              @click="selectDay(day.date)"
            >
              {{ day.number }}
              <span class="mini-calendar-dots" aria-hidden="true">
                <span v-for="dot in day.dots" :key="dot" :class="['mini-dot', categoryClass(dot)]"></span>
              </span>
            </button>
          </div>
        </section>

        <section class="side-card">
          <header class="side-card-header">
            <h2>Filtros</h2>
          </header>

          <div class="filter-options">
            <label
              v-for="category in categories"
              :key="category.value"
              :class="['filter-option', categoryClass(category.value)]"
            >
              <input
                type="checkbox"
                :checked="selectedCategories.includes(category.value)"
                @change="toggleCategory(category.value)"
              >
              {{ category.label }}
            </label>
          </div>
        </section>
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

<style scoped>
.calendar-screen { color: #151a2d; display: grid; gap: 15px; }

.calendar-topbar { align-items: center; display: flex; gap: 16px; justify-content: space-between; }
.calendar-heading { align-items: center; display: flex; gap: 13px; }
.calendar-heading-icon { align-items: center; background: #f1edff; border-radius: 11px; color: #6330e0; display: flex; flex: 0 0 44px; height: 44px; justify-content: center; }
.calendar-heading-icon svg { fill: none; height: 24px; stroke: currentColor; stroke-linecap: round; stroke-linejoin: round; stroke-width: 1.8; width: 24px; }
.calendar-heading h1 { color: #13182a; font-size: clamp(1.5rem, 2.3vw, 1.9rem); font-weight: 800; letter-spacing: -.04em; line-height: 1.15; }
.calendar-heading p { color: #687086; font-size: .8rem; margin-top: 4px; }

.new-event-button { align-items: center; background: linear-gradient(100deg, #5c20de, #741dff); border: 0; border-radius: 8px; box-shadow: 0 8px 19px rgba(102, 36, 225, .2); color: #fff; display: flex; font-size: .8rem; font-weight: 700; gap: 8px; padding: 13px 19px; }
.new-event-button span { font-size: 1.1rem; font-weight: 400; line-height: .8; }
.new-event-button:hover { box-shadow: 0 11px 24px rgba(102, 36, 225, .28); transform: translateY(-1px); }
.new-event-button:focus-visible { outline: 3px solid rgba(105, 54, 224, .28); outline-offset: 3px; }

.calendar-alert { align-items: center; background: #fff0f3; border-radius: 8px; color: #c2415f; display: flex; flex-wrap: wrap; font-size: .74rem; font-weight: 600; gap: 10px; justify-content: space-between; padding: 12px 15px; }
.calendar-feedback { background: #e9f8ef; border-radius: 8px; color: #1f8a52; font-size: .74rem; font-weight: 600; padding: 12px 15px; }

.calendar-section-error { align-items: center; background: #fff0f3; border-radius: 8px; color: #c2415f; display: flex; flex-wrap: wrap; font-size: .73rem; font-weight: 600; gap: 10px; justify-content: space-between; margin: 14px 18px 0; padding: 12px 15px; }
.calendar-section-error.is-inline { margin: 0 18px 12px; }
.calendar-section-error p { flex: 1; min-width: 160px; }

.retry-button { background: #fff; border: 1px solid #f3c1cd; border-radius: 7px; color: #c2415f; flex: 0 0 auto; font-size: .71rem; font-weight: 700; padding: 8px 14px; }
.retry-button:hover { background: #fff5f7; }
.retry-button:focus-visible { outline: 2px solid rgba(194, 65, 95, .35); outline-offset: 2px; }

.calendar-layout { align-items: start; display: grid; gap: 15px; grid-template-columns: minmax(0, 1fr) 306px; }

.calendar-board { background: #fff; border: 1px solid #ebeaf1; border-radius: 12px; box-shadow: 0 5px 16px rgba(30, 36, 65, .035); overflow: hidden; }

.calendar-toolbar { align-items: center; display: flex; gap: 14px; justify-content: space-between; padding: 16px 18px; }
.today-button { background: #fff; border: 1px solid #e1e3eb; border-radius: 8px; color: #343a50; font-size: .74rem; font-weight: 700; padding: 10px 18px; }
.today-button:hover { background: #f5f5fa; }

.period-navigation { align-items: center; display: flex; gap: 14px; }
.period-navigation strong { color: #1b2036; font-size: .92rem; font-weight: 750; letter-spacing: -.02em; min-width: 172px; text-align: center; }
.period-navigation button { background: #fff; border: 1px solid #e1e3eb; border-radius: 8px; color: #4a5066; font-size: 1rem; height: 36px; line-height: 1; width: 36px; }
.period-navigation button:hover { background: #f5f5fa; }

.view-switch { background: #f4f4f9; border-radius: 9px; display: flex; gap: 3px; padding: 3px; }
.view-switch button { background: transparent; border: 0; border-radius: 7px; color: #5c6379; font-size: .74rem; font-weight: 700; padding: 9px 17px; }
.view-switch button.active { background: linear-gradient(100deg, #5c20de, #741dff); box-shadow: 0 5px 13px rgba(102, 36, 225, .22); color: #fff; }

.calendar-loading,
.calendar-empty { color: #838a9e; font-size: .76rem; padding: 26px 18px; text-align: center; }

.calendar-weekdays { border-block: 1px solid #eeecf5; display: grid; grid-template-columns: repeat(7, minmax(0, 1fr)); }
.calendar-weekdays span { color: #666d84; font-size: .72rem; font-weight: 700; padding: 12px 0; text-align: center; }

.calendar-grid { display: grid; grid-template-columns: repeat(7, minmax(0, 1fr)); }
.calendar-day { border-bottom: 1px solid #f0eff6; border-right: 1px solid #f0eff6; display: flex; flex-direction: column; gap: 5px; min-height: 118px; padding: 8px; position: relative; }
.calendar-grid.is-week .calendar-day { min-height: 330px; }
.calendar-day:nth-child(7n) { border-right: 0; }
.calendar-day.is-outside { background: #fbfbfd; }
.calendar-day.is-outside .calendar-day-number { color: #b6bac9; }
.calendar-day-create { background: transparent; border: 0; cursor: pointer; inset: 0; padding: 0; position: absolute; z-index: 0; }
.calendar-day-create:focus-visible { outline: 2px solid #6936e0; outline-offset: -2px; }
.calendar-day-number,
.calendar-day-events { position: relative; z-index: 1; }
.calendar-day-number { color: #2b3149; font-size: .76rem; font-weight: 700; padding: 2px; pointer-events: none; }
.calendar-day.is-today .calendar-day-number { align-items: center; background: linear-gradient(135deg, #7749f7, #5320da); border-radius: 50%; color: #fff; display: flex; height: 26px; justify-content: center; width: 26px; }
.calendar-day-events { display: grid; gap: 4px; }

.calendar-event { border: 0; border-left: 3px solid; border-radius: 5px; display: block; padding: 5px 7px; text-align: left; width: 100%; }
.calendar-event strong { display: block; font-size: .66rem; font-weight: 700; line-height: 1.25; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.calendar-event small { color: #5f6579; display: block; font-size: .6rem; margin-top: 2px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.calendar-event .calendar-event-time { color: #737a8e; font-weight: 650; }
.calendar-event small.is-deleted { color: #c2415f; font-style: italic; }
.calendar-event:hover { filter: brightness(.97); }

.calendar-event.is-class { background: #e8f8ef; border-left-color: #2daf68; color: #1c7a48; }
.calendar-event.is-activity { background: #f1edff; border-left-color: #7a4ced; color: #4f2ab5; }
.calendar-event.is-exam { background: #eaf2ff; border-left-color: #3a7fd9; color: #26599c; }
.calendar-event.is-assignment { background: #fff4e6; border-left-color: #ef8b1f; color: #a85f10; }
.calendar-event.is-other { background: #ffeef3; border-left-color: #e2537c; color: #ac3357; }

.calendar-event.is-readonly,
.day-view-event.is-readonly { cursor: default; }
.calendar-event.is-readonly:hover,
.day-view-event.is-readonly:hover { filter: none; }
.readonly-tag { background: rgba(38, 89, 156, .14); border-radius: 999px; color: #26599c; display: inline-block; font-size: .55rem; font-weight: 750; margin-left: 6px; padding: 1px 6px; vertical-align: middle; }

.calendar-legend { display: flex; flex-wrap: wrap; gap: 18px; justify-content: center; padding: 15px 18px; }
.legend-item { align-items: center; color: #5f6579; display: flex; font-size: .69rem; font-weight: 650; gap: 7px; }
.legend-dot,
.upcoming-dot,
.mini-dot { border-radius: 50%; display: inline-block; }
.legend-dot { height: 8px; width: 8px; }
.legend-item.is-class .legend-dot { background: #2daf68; }
.legend-item.is-activity .legend-dot { background: #7a4ced; }
.legend-item.is-exam .legend-dot { background: #3a7fd9; }
.legend-item.is-assignment .legend-dot { background: #ef8b1f; }
.legend-item.is-other .legend-dot { background: #e2537c; }

.day-view { padding: 4px 18px 8px; }
.day-view h2 { color: #1b2036; font-size: .88rem; font-weight: 750; margin-bottom: 14px; }
.day-view-list { display: grid; gap: 9px; }
.day-view-event { align-items: flex-start; border: 0; border-left: 3px solid; border-radius: 8px; display: flex; gap: 14px; padding: 13px 15px; text-align: left; width: 100%; }
.day-view-time { font-size: .72rem; font-weight: 750; min-width: 92px; }
.day-view-body strong { display: block; font-size: .8rem; font-weight: 700; }
.day-view-body small { color: #5f6579; display: block; font-size: .68rem; margin-top: 3px; }
.day-view-body small.is-deleted { color: #c2415f; font-style: italic; }
.day-view-description { color: #7b8194; }
.day-view-event.is-class { background: #e8f8ef; border-left-color: #2daf68; color: #1c7a48; }
.day-view-event.is-activity { background: #f1edff; border-left-color: #7a4ced; color: #4f2ab5; }
.day-view-event.is-exam { background: #eaf2ff; border-left-color: #3a7fd9; color: #26599c; }
.day-view-event.is-assignment { background: #fff4e6; border-left-color: #ef8b1f; color: #a85f10; }
.day-view-event.is-other { background: #ffeef3; border-left-color: #e2537c; color: #ac3357; }

.calendar-side { display: grid; gap: 15px; }
.side-card { background: #fff; border: 1px solid #ebeaf1; border-radius: 12px; box-shadow: 0 5px 16px rgba(30, 36, 65, .035); padding: 17px; }
.side-card-header { align-items: center; display: flex; justify-content: space-between; margin-bottom: 13px; }
.side-card-header h2 { color: #1b2036; font-size: .84rem; font-weight: 750; letter-spacing: -.02em; }
.side-card-action { background: none; border: 0; color: #6429db; font-size: .69rem; font-weight: 700; padding: 3px 0; }
.side-card-action:hover { text-decoration: underline; }

.upcoming-list { display: grid; gap: 3px; }
.upcoming-item { align-items: flex-start; background: none; border: 0; border-radius: 8px; cursor: pointer; display: flex; gap: 10px; padding: 9px 7px; text-align: left; width: 100%; }
.upcoming-item:hover { background: #f7f6fc; }
.upcoming-item.is-readonly { cursor: default; }
.upcoming-item.is-readonly:hover { background: none; }
.upcoming-count-hint { color: #9096a8; font-size: .66rem; margin-top: 8px; text-align: center; }
.upcoming-dot { flex: 0 0 8px; height: 8px; margin-top: 5px; width: 8px; }
.upcoming-dot.is-class { background: #2daf68; }
.upcoming-dot.is-activity { background: #7a4ced; }
.upcoming-dot.is-exam { background: #3a7fd9; }
.upcoming-dot.is-assignment { background: #ef8b1f; }
.upcoming-dot.is-other { background: #e2537c; }
.upcoming-body { flex: 1; min-width: 0; }
.upcoming-body strong { color: #23283d; display: block; font-size: .73rem; font-weight: 700; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.upcoming-body small { color: #767d92; display: block; font-size: .65rem; margin-top: 2px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.upcoming-body small.is-deleted { color: #c2415f; font-style: italic; }
.upcoming-when { text-align: right; }
.upcoming-when strong { color: #3a4058; display: block; font-size: .68rem; font-weight: 700; }
.upcoming-when small { color: #868da1; display: block; font-size: .63rem; margin-top: 2px; }

.mini-calendar-header { align-items: center; display: flex; justify-content: space-between; margin-bottom: 11px; }
.mini-calendar-header strong { color: #1b2036; font-size: .78rem; font-weight: 750; }
.mini-calendar-header button { background: none; border: 0; color: #5c6379; font-size: 1rem; line-height: 1; padding: 3px 8px; }
.mini-calendar-header button:hover { color: #6429db; }
.mini-calendar-weekdays,
.mini-calendar-grid { display: grid; grid-template-columns: repeat(7, minmax(0, 1fr)); }
.mini-calendar-weekdays span { color: #949aae; font-size: .62rem; font-weight: 700; padding-bottom: 6px; text-align: center; }
.mini-calendar-day { background: none; border: 0; border-radius: 50%; color: #3a4058; font-size: .68rem; font-weight: 650; height: 30px; padding: 0; position: relative; }
.mini-calendar-day:hover { background: #f2effc; }
.mini-calendar-day.is-outside { color: #bcc0cd; }
.mini-calendar-day.is-today { color: #6429db; font-weight: 800; }
.mini-calendar-day.is-selected { background: linear-gradient(135deg, #7749f7, #5320da); color: #fff; }
.mini-calendar-dots { bottom: 2px; display: flex; gap: 2px; justify-content: center; left: 0; position: absolute; right: 0; }
.mini-dot { height: 4px; width: 4px; }
.mini-dot.is-class { background: #2daf68; }
.mini-dot.is-activity { background: #7a4ced; }
.mini-dot.is-exam { background: #3a7fd9; }
.mini-dot.is-assignment { background: #ef8b1f; }
.mini-dot.is-other { background: #e2537c; }

.filter-options { display: flex; flex-wrap: wrap; gap: 9px 14px; }
.filter-option { align-items: center; color: #4a5066; cursor: pointer; display: flex; font-size: .71rem; font-weight: 650; gap: 8px; }
.filter-option input { accent-color: #6429db; height: 15px; width: 15px; }
.filter-option.is-class input { accent-color: #2daf68; }
.filter-option.is-activity input { accent-color: #7a4ced; }
.filter-option.is-exam input { accent-color: #3a7fd9; }
.filter-option.is-assignment input { accent-color: #ef8b1f; }
.filter-option.is-other input { accent-color: #e2537c; }

@media (max-width: 1180px) {
  .calendar-layout { grid-template-columns: minmax(0, 1fr); }
  .calendar-side { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}

@media (max-width: 860px) {
  .calendar-toolbar { flex-wrap: wrap; justify-content: center; }
  .calendar-side { grid-template-columns: minmax(0, 1fr); }
  .calendar-day { min-height: 92px; }
  .calendar-grid.is-week .calendar-day { min-height: 200px; }
}

@media (max-width: 520px) {
  .calendar-topbar { align-items: flex-start; flex-direction: column; }
  .period-navigation strong { font-size: .8rem; min-width: 130px; }
  .calendar-weekdays span { font-size: .62rem; }
  .calendar-day { min-height: 74px; padding: 5px; }
  .calendar-event small { display: none; }
}
</style>
