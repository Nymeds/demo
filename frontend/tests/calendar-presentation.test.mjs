import assert from 'node:assert/strict'
import test from 'node:test'
import {
  CATEGORIES,
  appendCategoryParams,
  buildCalendarDay,
  buildMiniCalendarDay,
  displayedEventsOf,
  eventSaveModeOf,
  eventsOfDay,
  monthGridOf,
  periodLabelOf,
  upcomingCombinedOf,
  upcomingDayLabel,
  upcomingQueryOf,
  visibleDaysOf,
  withoutDuplicateExamEvents,
} from '../src/features/calendar/calendarPresentation.js'

const ALL = CATEGORIES.map(category => category.value)
const DISCIPLINES = [{ id: 'calc', name: 'Cálculo' }]

test('prova sempre vira atividade: nova é criada, e evento editado para prova é convertido', () => {
  assert.equal(eventSaveModeOf(false, 'EXAM'), 'create-exam')
  assert.equal(eventSaveModeOf(true, 'EXAM'), 'convert-to-exam')
  assert.equal(eventSaveModeOf(false, 'CLASS'), 'event')
  assert.equal(eventSaveModeOf(true, 'ASSIGNMENT'), 'event')
})

function event(id, category, startsAt, extra = {}) {
  return { id, title: id, category, startsAt, endsAt: null, disciplineId: null, ...extra }
}

function examActivity(id, dueDate, extra = {}) {
  return { id, title: `Prova ${id}`, description: null, dueDate, disciplineId: 'calc', ...extra }
}

test('grade do mês cobre semanas completas de domingo a sábado', () => {
  const grid = monthGridOf(new Date(2026, 8, 15))

  assert.equal(grid.length % 7, 0)
  assert.equal(grid[0].getDay(), 0)
  assert.equal(grid.at(-1).getDay(), 6)
  assert.equal(grid[0].getDate(), 30)
  assert.equal(grid[0].getMonth(), 7)
  assert.equal(grid.at(-1).getDate(), 3)
  assert.equal(grid.at(-1).getMonth(), 9)
})

test('visão de semana e de dia são recortes da referência', () => {
  const reference = new Date(2026, 8, 16)
  const grid = monthGridOf(reference)

  assert.equal(visibleDaysOf('month', reference, grid), grid)
  assert.equal(visibleDaysOf('day', reference, grid).length, 1)

  const week = visibleDaysOf('week', reference, grid)
  assert.equal(week.length, 7)
  assert.equal(week[0].getDate(), 13)
  assert.equal(week[6].getDate(), 19)
  assert.equal(periodLabelOf('month', reference), 'Setembro de 2026')
  assert.match(periodLabelOf('week', reference), /de 2026$/)
})

test('evento aparece em todos os dias entre início e fim, e prazo só no próprio dia', () => {
  const trip = event('trip', 'OTHER', '2026-09-14T20:00:00', { endsAt: '2026-09-16T10:00:00' })
  const deadline = event('deadline', 'ASSIGNMENT', '2026-09-15T23:00:00')

  assert.deepEqual(eventsOfDay([trip, deadline], new Date(2026, 8, 14)).map(item => item.id), ['trip'])
  assert.deepEqual(eventsOfDay([trip, deadline], new Date(2026, 8, 15)).map(item => item.id), ['trip', 'deadline'])
  assert.deepEqual(eventsOfDay([trip, deadline], new Date(2026, 8, 16)).map(item => item.id), ['trip'])
  assert.deepEqual(eventsOfDay([trip, deadline], new Date(2026, 8, 17)), [])
})

test('dias do calendário marcam hoje, mês corrente e seleção', () => {
  const reference = new Date(2026, 8, 16)
  const today = new Date(2026, 8, 15)
  const items = [event('a', 'CLASS', '2026-09-15T08:00:00'), event('b', 'EXAM', '2026-09-15T09:00:00'), event('c', 'CLASS', '2026-09-15T10:00:00')]

  const day = buildCalendarDay(new Date(2026, 8, 15), items, reference, today)
  assert.equal(day.isToday, true)
  assert.equal(day.isSelected, false)
  assert.equal(day.isCurrentMonth, true)
  assert.equal(day.events.length, 3)

  const mini = buildMiniCalendarDay(new Date(2026, 8, 15), items, reference, today)
  assert.deepEqual(mini.dots, ['CLASS', 'EXAM'])
  assert.equal(buildMiniCalendarDay(new Date(2026, 9, 1), [], reference, today).isCurrentMonth, false)
})

test('visão Mês mostra 3 eventos por dia e o resto vira "mais N"; Semana mostra todos', () => {
  const reference = new Date(2026, 8, 16)
  const today = new Date(2026, 8, 15)
  const items = ['a', 'b', 'c', 'd', 'e'].map((id, index) => event(id, 'CLASS', `2026-09-24T0${index + 1}:00:00`))

  const month = buildCalendarDay(new Date(2026, 8, 24), items, reference, today, 'month')
  assert.deepEqual(month.events.map(item => item.id), ['a', 'b', 'c'])
  assert.equal(month.hiddenCount, 2)

  const week = buildCalendarDay(new Date(2026, 8, 24), items, reference, today, 'week')
  assert.equal(week.events.length, 5)
  assert.equal(week.hiddenCount, 0)
})

test('prova legada duplicada por atividade EXAM é escondida, e a atividade vira item somente leitura', () => {
  const legacy = event('legacy', 'EXAM', '2026-09-20T14:00:00', { title: 'Prova 1', disciplineId: 'calc' })
  const other = event('other', 'EXAM', '2026-09-21T14:00:00', { title: 'Prova 1', disciplineId: 'calc' })
  const activities = [examActivity('1', '2026-09-20')]

  assert.deepEqual(withoutDuplicateExamEvents([legacy, other], activities).map(item => item.id), ['other'])

  const displayed = displayedEventsOf([legacy, other], activities, DISCIPLINES)
  assert.deepEqual(displayed.map(item => item.id), ['other', 'activity-exam-1'])
  assert.equal(displayed[1].isExamActivity, true)
  assert.equal(displayed[1].disciplineName, 'Cálculo')
  assert.equal(displayed[1].startsAt, '2026-09-20T08:00:00')
})

test('próximos eventos: provas passadas saem, duplicadas somem e a lista é ordenada', () => {
  const today = new Date(2026, 8, 15)
  const upcoming = [
    event('legacy', 'EXAM', '2026-09-20T14:00:00', { title: 'Prova 1', disciplineId: 'calc' }),
    event('class', 'CLASS', '2026-09-18T10:00:00'),
  ]
  const activities = [
    examActivity('1', '2026-09-20'),
    examActivity('2', '2026-09-16'),
    examActivity('old', '2026-09-10'),
  ]

  const result = upcomingCombinedOf(upcoming, activities, DISCIPLINES, ALL, today)

  assert.deepEqual(result.map(item => item.id), ['activity-exam-2', 'class', 'activity-exam-1'])
})

test('próximos eventos: filtro de categorias vale para eventos e provas, mesmo com a lista da API desatualizada', () => {
  const today = new Date(2026, 8, 15)
  const upcoming = [event('class', 'CLASS', '2026-09-18T10:00:00')]
  const activities = [examActivity('1', '2026-09-20')]

  // Lista da API ainda com uma categoria já desmarcada (requisição em andamento ou que falhou).
  assert.deepEqual(
    upcomingCombinedOf(upcoming, activities, DISCIPLINES, ['EXAM'], today).map(item => item.id),
    ['activity-exam-1'],
  )

  assert.deepEqual(
    upcomingCombinedOf(upcoming, activities, DISCIPLINES, ['CLASS'], today).map(item => item.id),
    ['class'],
  )
  assert.deepEqual(
    upcomingCombinedOf([], activities, DISCIPLINES, ['EXAM'], today).map(item => item.id),
    ['activity-exam-1'],
  )
  assert.deepEqual(upcomingCombinedOf([], activities, DISCIPLINES, [], today), [])
})

test('consulta de próximos eventos manda categorias só quando o filtro restringe', () => {
  // 50 é o teto da API em /upcoming: acima disso ela responde 400.
  assert.equal(upcomingQueryOf(ALL).toString(), 'limit=50')
  assert.equal(
    upcomingQueryOf(['EXAM', 'ASSIGNMENT']).toString(),
    'limit=50&categories=EXAM&categories=ASSIGNMENT',
  )

  const params = appendCategoryParams(new URLSearchParams(), ['CLASS'])
  assert.deepEqual(params.getAll('categories'), ['CLASS'])
  assert.deepEqual(appendCategoryParams(new URLSearchParams(), ALL).getAll('categories'), [])
})

test('rótulo do dia usa o relógio recebido: hoje, amanhã e data curta', () => {
  const today = new Date(2026, 8, 15)

  assert.equal(upcomingDayLabel('2026-09-15T23:00:00', today), 'Hoje')
  assert.equal(upcomingDayLabel('2026-09-16T08:00:00', today), 'Amanhã')
  assert.equal(upcomingDayLabel('2026-09-20T08:00:00', today), '20/09')
  // Depois da meia-noite o mesmo evento deixa de ser "Hoje".
  assert.equal(upcomingDayLabel('2026-09-15T23:00:00', new Date(2026, 8, 16)), '15/09')
})
