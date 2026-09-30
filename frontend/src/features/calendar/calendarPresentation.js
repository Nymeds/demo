import { parseLocalDateTime, startOfDay, toLocalDateTimeIso as toLocalIso } from '../../shared/date/localDate.js'

export const CATEGORIES = [
  { value: 'CLASS', label: 'Aulas' },
  { value: 'ACTIVITY', label: 'Atividades' },
  { value: 'EXAM', label: 'Provas' },
  { value: 'ASSIGNMENT', label: 'Trabalhos' },
  { value: 'OTHER', label: 'Outros' },
]

export const WEEK_DAY_LABELS = ['Dom', 'Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb']
export const MINI_WEEK_DAY_LABELS = ['D', 'S', 'T', 'Q', 'Q', 'S', 'S']

// Teto aceito pela API em /upcoming (acima disso responde 400).
export const UPCOMING_FETCH_LIMIT = 50
export const UPCOMING_DISPLAY_LIMIT = 5

// Quantos eventos cabem na célula da visão Mês antes de virar "mais N".
export const MONTH_EVENTS_PER_DAY = 3

// O que o "salvar" do modal faz. A prova de verdade é a Atividade do tipo EXAM (é ela que aparece
// em Provas e em Notas): prova nova vira atividade, e um evento que passa a ser prova (ou um evento
// de prova antigo, salvo de novo) também vira atividade e deixa de ser evento do calendário.
export function eventSaveModeOf(isEditing, category) {
  if (category !== 'EXAM') return 'event'
  return isEditing ? 'convert-to-exam' : 'create-exam'
}

export function pad(value) {
  return String(value).padStart(2, '0')
}

export function endOfDay(date) {
  const copy = new Date(date)
  copy.setHours(23, 59, 59, 0)
  return copy
}

export function addDays(date, amount) {
  const copy = new Date(date)
  copy.setDate(copy.getDate() + amount)
  return copy
}

export function addMonths(date, amount) {
  const copy = new Date(date.getFullYear(), date.getMonth() + amount, 1)
  return copy
}

export function startOfWeek(date) {
  return addDays(startOfDay(date), -date.getDay())
}

export function isSameDay(first, second) {
  return first.getFullYear() === second.getFullYear()
    && first.getMonth() === second.getMonth()
    && first.getDate() === second.getDate()
}

export function capitalize(text) {
  return text.charAt(0).toUpperCase() + text.slice(1)
}

export function dayCreateLabel(date) {
  const formatted = new Intl.DateTimeFormat('pt-BR', {
    day: 'numeric',
    month: 'long',
    year: 'numeric',
  }).format(date)

  return `Criar evento em ${formatted}`
}

export function formatTime(value) {
  return value ? value.slice(11, 16) : ''
}

export function categoryClass(category) {
  return `is-${category.toLowerCase()}`
}

// A consulta sempre cobre o mês inteiro na tela (com os dias vizinhos que completam
// as semanas). As visões Semana e Dia são recortes desse mesmo resultado, então
// trocar de visão não custa uma requisição nova e o mini calendário nunca fica sem pontos.
export function monthGridOf(referenceDate) {
  const firstOfMonth = new Date(referenceDate.getFullYear(), referenceDate.getMonth(), 1)
  const lastOfMonth = new Date(referenceDate.getFullYear(), referenceDate.getMonth() + 1, 0)
  const start = startOfWeek(firstOfMonth)
  const end = addDays(startOfWeek(lastOfMonth), 6)
  const total = Math.round((end - start) / 86400000) + 1

  return Array.from({ length: total }, (_, index) => addDays(start, index))
}

export function visibleDaysOf(viewMode, referenceDate, monthGrid) {
  if (viewMode === 'day') {
    return [referenceDate]
  }

  if (viewMode === 'week') {
    const start = startOfWeek(referenceDate)
    return Array.from({ length: 7 }, (_, index) => addDays(start, index))
  }

  return monthGrid
}

export function periodLabelOf(viewMode, referenceDate) {
  if (viewMode === 'day') {
    return capitalize(new Intl.DateTimeFormat('pt-BR', {
      weekday: 'long',
      day: 'numeric',
      month: 'long',
      year: 'numeric',
    }).format(referenceDate))
  }

  if (viewMode === 'week') {
    const start = startOfWeek(referenceDate)
    const end = addDays(start, 6)
    const dayMonth = new Intl.DateTimeFormat('pt-BR', { day: 'numeric', month: 'short' })
    return `${dayMonth.format(start)} – ${dayMonth.format(end)} de ${start.getFullYear()}`
  }

  return monthLabelOf(referenceDate)
}

export function monthLabelOf(referenceDate) {
  return capitalize(new Intl.DateTimeFormat('pt-BR', {
    month: 'long',
    year: 'numeric',
  }).format(referenceDate))
}

// Provas viram Atividades (tipo EXAM) e passam a ser compartilhadas com a tela Provas.
// Aqui elas entram como itens somente leitura, no dia da entrega (dueDate), a partir
// das 8h — hora fixa só para posicionar o item no dia certo, sem hora real de prova.
export function examActivityToEvent(activity, disciplines) {
  return {
    id: `activity-exam-${activity.id}`,
    title: activity.title,
    description: activity.description || '',
    category: 'EXAM',
    startsAt: `${activity.dueDate}T08:00:00`,
    endsAt: null,
    disciplineId: activity.disciplineId,
    disciplineName: disciplines.find(discipline => discipline.id === activity.disciplineId)?.name || '',
    disciplineDeleted: false,
    isExamActivity: true,
  }
}

// Eventos EXAM antigos (criados antes desta mudança, quando prova era só um CalendarEvent)
// que já correspondem a uma Atividade de prova não devem aparecer duplicados.
export function isDuplicateExamEvent(event, activity) {
  return event.category === 'EXAM'
    && event.disciplineId === activity.disciplineId
    && event.title === activity.title
    && event.startsAt.slice(0, 10) === activity.dueDate
}

export function withoutDuplicateExamEvents(events, examActivities) {
  const duplicateIds = new Set()

  examActivities.forEach(activity => {
    events.forEach(event => {
      if (isDuplicateExamEvent(event, activity)) duplicateIds.add(event.id)
    })
  })

  return events.filter(event => !duplicateIds.has(event.id))
}

export function displayedEventsOf(events, examActivities, disciplines) {
  return [
    ...withoutDuplicateExamEvents(events, examActivities),
    ...examActivities.map(activity => examActivityToEvent(activity, disciplines)),
  ]
}

// "Próximos eventos": a API já filtra os eventos pelas categorias marcadas, mas as provas
// (Atividades do tipo EXAM) vêm de outra rota. O filtro é reaplicado sobre a lista inteira
// para não mostrar categoria desmarcada enquanto a requisição não volta ou se ela falhar.
export function upcomingCombinedOf(upcomingEvents, examActivities, disciplines, selectedCategories, today) {
  const todayIso = toLocalIso(today).slice(0, 10)

  const examAsUpcoming = examActivities
    .filter(activity => activity.dueDate >= todayIso)
    .map(activity => examActivityToEvent(activity, disciplines))

  return [
    ...withoutDuplicateExamEvents(upcomingEvents, examActivities),
    ...examAsUpcoming,
  ]
    .filter(event => selectedCategories.includes(event.category))
    .sort((first, second) => first.startsAt.localeCompare(second.startsAt))
}

// Todas as categorias marcadas equivale a "sem filtro" para a API, então o parâmetro
// só vai quando o filtro realmente restringe alguma categoria.
export function appendCategoryParams(params, selectedCategories) {
  if (selectedCategories.length < CATEGORIES.length) {
    selectedCategories.forEach(category => params.append('categories', category))
  }

  return params
}

export function upcomingQueryOf(selectedCategories) {
  const params = new URLSearchParams()
  params.set('limit', UPCOMING_FETCH_LIMIT)

  return appendCategoryParams(params, selectedCategories)
}

export function eventsOfDay(events, day) {
  const dayStart = startOfDay(day)
  const dayEnd = endOfDay(day)

  // Mesma regra de sobreposição da API: o evento aparece no dia enquanto não terminou.
  return events
    .filter(event => {
      const start = parseLocalDateTime(event.startsAt)
      if (!start) return false
      const end = (event.endsAt && parseLocalDateTime(event.endsAt)) || start
      return start <= dayEnd && end >= dayStart
    })
    .sort((first, second) => first.startsAt.localeCompare(second.startsAt))
}

export function buildCalendarDay(day, events, referenceDate, today, viewMode = 'week') {
  const dayEvents = eventsOfDay(events, day)

  // A célula da visão Mês é baixa: mostrar tudo faria a semana que tem um dia cheio ficar
  // muito mais alta que as outras. O que passa do limite vira o "mais N", que abre a visão
  // Dia daquele dia. A visão Semana tem célula alta, então lá cabem todos.
  const limit = viewMode === 'month' ? MONTH_EVENTS_PER_DAY : dayEvents.length

  return {
    date: day,
    key: toLocalIso(day),
    number: day.getDate(),
    isToday: isSameDay(day, today),
    isCurrentMonth: day.getMonth() === referenceDate.getMonth(),
    isSelected: isSameDay(day, referenceDate),
    events: dayEvents.slice(0, limit),
    hiddenCount: Math.max(dayEvents.length - limit, 0),
  }
}

export function buildMiniCalendarDay(day, events, referenceDate, today) {
  const dayEvents = eventsOfDay(events, day)
  const dots = [...new Set(dayEvents.map(event => event.category))].slice(0, 3)

  return {
    date: day,
    key: toLocalIso(day),
    number: day.getDate(),
    isToday: isSameDay(day, today),
    isCurrentMonth: day.getMonth() === referenceDate.getMonth(),
    isSelected: isSameDay(day, referenceDate),
    dots,
  }
}

export function upcomingDayLabel(value, today) {
  const date = startOfDay(parseLocalDateTime(value) ?? new Date())

  if (isSameDay(date, today)) {
    return 'Hoje'
  }

  if (isSameDay(date, addDays(today, 1))) {
    return 'Amanhã'
  }

  return `${pad(date.getDate())}/${pad(date.getMonth() + 1)}`
}
