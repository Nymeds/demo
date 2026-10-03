import assert from 'node:assert/strict'
import test from 'node:test'
import {
  activityStatus,
  averageOf,
  classDateTime,
  compareByDueDate,
  disciplineColorOf,
  disciplineNameOf,
  firstNameOf,
  formatAverage,
  formatClassSchedule,
  formatCompactDate,
  frequencyDetailsOf,
  isOverdue,
  nextClassOf,
  overdueRemindersOf,
  reminderKindLabel,
  reminderLimitIsoOf,
  reminderSectionFor,
  upcomingRemindersOf,
  visibleReminderGroupsOf,
} from '../src/features/dashboard/dashboardPresentation.js'

test('firstNameOf usa o primeiro nome, o começo do e-mail ou um padrão', () => {
  assert.equal(firstNameOf('maria souza'), 'Maria')
  assert.equal(firstNameOf('  joao@exemplo.com '), 'Joao')
  assert.equal(firstNameOf(''), 'Estudante')
  assert.equal(firstNameOf(undefined), 'Estudante')
})

test('averageOf ignora valores não numéricos e devolve null sem dados', () => {
  assert.equal(averageOf([{ average: 8 }, { average: null }, { average: 6 }], 'average'), 7)
  assert.equal(averageOf([{ average: null }], 'average'), null)
  assert.equal(averageOf([], 'average'), null)
})

test('formatAverage e formatCompactDate seguem o padrão pt-BR', () => {
  assert.equal(formatAverage(null), '—')
  assert.equal(formatAverage(7.5), '7,50')
  assert.equal(formatCompactDate(''), '—')
  assert.equal(formatCompactDate('2099-09-30'), '30/09')
})

test('atividades sem prazo não são atrasadas e vão para o fim da ordenação', () => {
  const today = '2026-09-29'
  assert.equal(isOverdue({ dueDate: '2026-09-28' }, today), true)
  assert.equal(isOverdue({ dueDate: '2026-09-29' }, today), false)
  assert.equal(isOverdue({ dueDate: null }, today), false)

  const sorted = [{ id: 'sem' }, { id: 'b', dueDate: '2026-10-02' }, { id: 'a', dueDate: '2026-10-01' }]
    .sort(compareByDueDate)
  assert.deepEqual(sorted.map(item => item.id), ['a', 'b', 'sem'])
})

test('activityStatus prioriza concluída, atrasada, em andamento e pendente', () => {
  const today = '2026-09-29'
  assert.equal(activityStatus({ status: 'COMPLETED', dueDate: '2020-01-01' }, today).className, 'is-completed')
  assert.equal(activityStatus({ status: 'PENDING', dueDate: '2020-01-01' }, today).label, 'Atrasada')
  assert.equal(activityStatus({ status: 'IN_PROGRESS', dueDate: '2099-01-01' }, today).label, 'Em andamento')
  assert.equal(activityStatus({ status: 'PENDING' }, today).className, 'is-pending')
})

test('avisos separam atrasadas de próximos prazos dentro da antecedência', () => {
  const now = new Date(2026, 8, 29, 10, 0)
  const limit = reminderLimitIsoOf(now, 3)
  assert.equal(limit, '2026-10-02')

  const activities = [
    { id: 'late', status: 'PENDING', dueDate: '2026-09-20' },
    { id: 'soon', status: 'PENDING', dueDate: '2026-10-01' },
    { id: 'far', status: 'PENDING', dueDate: '2026-11-01' },
    { id: 'done', status: 'COMPLETED', dueDate: '2026-10-01' },
    { id: 'none', status: 'PENDING' },
  ]
  const overdue = overdueRemindersOf(activities, '2026-09-29')
  const upcoming = upcomingRemindersOf(activities, '2026-09-29', limit)
  assert.deepEqual(overdue.map(item => item.id), ['late'])
  assert.deepEqual(upcoming.map(item => item.id), ['soon'])

  const groups = visibleReminderGroupsOf(overdue, upcoming)
  assert.deepEqual(groups.map(group => group.key), ['overdue', 'upcoming'])
  assert.equal(groups[0].label, 'Atrasadas')
  assert.equal(groups[1].label, 'Próximos prazos')
})

test('visibleReminderGroupsOf limita a 8 avisos reservando espaço para os próximos prazos', () => {
  const make = (prefix, count) => Array.from({ length: count }, (_, index) => ({ id: `${prefix}${index}` }))

  const mixed = visibleReminderGroupsOf(make('o', 10), make('u', 10))
  assert.equal(mixed[0].items.length, 4)
  assert.equal(mixed[1].items.length, 4)

  const onlyOverdue = visibleReminderGroupsOf(make('o', 10), [])
  assert.equal(onlyOverdue.length, 1)
  assert.equal(onlyOverdue[0].items.length, 8)

  assert.deepEqual(visibleReminderGroupsOf([], []), [])
})

test('avisos usam a seção e o rótulo conforme o tipo', () => {
  assert.equal(reminderSectionFor({ type: 'EXAM' }), 'exams')
  assert.equal(reminderSectionFor({ type: 'HOMEWORK' }), 'activities')
  assert.equal(reminderKindLabel({ type: 'EXAM' }), 'Prova')
  assert.equal(reminderKindLabel({}), 'Atividade')
})

test('nextClassOf escolhe o próximo horário e formatClassSchedule rotula o dia', () => {
  // 2026-09-29 é uma terça-feira.
  const now = new Date(2026, 8, 29, 10, 0)
  const disciplines = [{
    id: 'd1',
    name: 'Banco de Dados',
    schedules: [
      { dayOfWeek: 'TUESDAY', startTime: '08:00:00', endTime: '10:00:00' },
      { dayOfWeek: 'TUESDAY', startTime: '19:00:00', endTime: '21:00:00' },
      { dayOfWeek: 'INVALID', startTime: '19:00:00', endTime: '21:00:00' },
    ],
  }]

  const next = nextClassOf(disciplines, now)
  assert.equal(next.id, 'd1-1')
  assert.equal(formatClassSchedule(next, now), 'Hoje 19:00–21:00')
  assert.equal(classDateTime(next), '2026-09-29T19:00')

  const tomorrow = nextClassOf([{
    id: 'd2',
    schedules: [{ dayOfWeek: 'WEDNESDAY', startTime: '07:30', endTime: '09:00' }],
  }], now)
  assert.equal(formatClassSchedule(tomorrow, now), 'Amanhã 07:30–09:00')

  const later = nextClassOf([{
    id: 'd3',
    schedules: [{ dayOfWeek: 'FRIDAY', startTime: '07:30', endTime: '09:00' }],
  }], now)
  assert.equal(formatClassSchedule(later, now), 'Sex 07:30–09:00')

  assert.equal(nextClassOf([{ id: 'd4' }], now), null)
})

test('frequencyDetailsOf descreve a situação da frequência', () => {
  assert.equal(frequencyDetailsOf(null, 5), null)

  const empty = frequencyDetailsOf({ id: 'd' }, 5)
  assert.equal(empty.attendance, null)
  assert.equal(empty.messageClass, 'is-neutral')
  assert.equal(empty.maximumAbsences, 5)

  const good = frequencyDetailsOf({
    id: 'd',
    attendancePercentage: 95,
    minimumAttendancePercentage: 75,
    absences: 1,
    lossPerAbsence: 5,
    maximumAbsences: 5,
  }, 5)
  assert.equal(good.messageClass, 'is-success')
  assert.equal(good.ringColor, '#20aa60')
  assert.match(good.message, /ainda pode registrar 4 faltas/)

  const bad = frequencyDetailsOf({
    id: 'd',
    attendancePercentage: 60,
    minimumAttendancePercentage: 75,
    absences: 8,
    lossPerAbsence: 5,
    maximumAbsences: 5,
  }, 5)
  assert.equal(bad.messageClass, 'is-danger')
  assert.equal(bad.ringColor, '#e04433')
})

test('disciplineNameOf e disciplineColorOf têm valores padrão', () => {
  const disciplines = [{ id: 'd1', name: 'Redes', color: '#123456' }]
  assert.equal(disciplineNameOf(disciplines, 'd1'), 'Redes')
  assert.equal(disciplineColorOf(disciplines, 'd1'), '#123456')
  assert.equal(disciplineNameOf(disciplines, 'x'), 'Disciplina')
  assert.equal(disciplineColorOf(disciplines, 'x'), '#6631db')
})
