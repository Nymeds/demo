import assert from 'node:assert/strict'
import test from 'node:test'
import {
  buildCalendarDays,
  disciplineLabel,
  examState,
  formatDate,
  formatMonthLabel,
  normalizeExam,
  statusClass,
  statusLabel,
} from '../src/features/exams/examPresentation.js'

const now = new Date(2026, 5, 10, 12, 0)
const today = new Date(2026, 5, 10)
const exam = (id, dueDate, status = 'PENDING') => ({ id, dueDate, status, disciplineId: 'd1' })

test('estado da prova: concluída, atrasada ou agendada', () => {
  assert.equal(examState(exam('a', '2026-06-01', 'COMPLETED'), now), 'completed')
  assert.equal(examState(exam('b', '2026-06-01'), now), 'overdue')
  assert.equal(examState(exam('c', '2026-06-20'), now), 'scheduled')
  assert.equal(statusLabel(exam('b', '2026-06-01'), now), 'Atrasada')
  assert.equal(statusClass(exam('c', '2026-06-20'), now), 'is-scheduled')
})

test('disciplinas homônimas ganham o professor no rótulo', () => {
  const disciplines = [
    { id: 'd1', name: 'Cálculo', professorName: 'Ana' },
    { id: 'd2', name: 'Cálculo', professorName: 'Beto' },
    { id: 'd3', name: 'Física' },
  ]
  assert.equal(disciplineLabel(disciplines, disciplines[0]), 'Cálculo (Ana)')
  assert.equal(disciplineLabel(disciplines, disciplines[2]), 'Física')
  assert.equal(disciplineLabel(disciplines, undefined), 'Disciplina')
})

test('normalizeExam acrescenta nome e cor da disciplina', () => {
  const disciplines = [{ id: 'd0', name: 'A' }, { id: 'd1', name: 'B' }]
  assert.deepEqual(normalizeExam(exam('x', '2026-06-20'), disciplines), {
    ...exam('x', '2026-06-20'), disciplineName: 'B', color: 'green',
  })
  assert.equal(normalizeExam({ ...exam('y', '2026-06-20'), disciplineId: 'zz' }, disciplines).color, 'purple')
})

test('formatação de data e mês em pt-BR', () => {
  assert.equal(formatDate('2026-06-09'), '09/06/2026')
  assert.equal(formatDate(''), '—')
  assert.equal(formatMonthLabel(new Date(2026, 5, 1)), 'Junho de 2026')
})

test('calendário marca hoje, atrasada, agendada e concluída', () => {
  const exams = [exam('a', '2026-06-03'), exam('b', '2026-06-20'), exam('c', '2026-06-25', 'COMPLETED')]
  const days = buildCalendarDays(exams, new Date(2026, 5, 1), today, true, now)
  const byNumber = number => days.find(day => day.number === number).state
  assert.equal(days.length, 1 + 30) // 1º de junho de 2026 cai numa segunda
  assert.equal(byNumber(10), 'today')
  assert.equal(byNumber(3), 'overdue')
  assert.equal(byNumber(20), 'upcoming')
  assert.equal(byNumber(25), 'completed')
  assert.equal(byNumber(1), '')
})

test('calendário de outro mês não marca hoje', () => {
  const days = buildCalendarDays([], new Date(2026, 6, 1), today, false, now)
  assert.ok(days.every(day => day.state === ''))
})
