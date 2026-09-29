import assert from 'node:assert/strict'
import test from 'node:test'
import { filterExams } from '../src/features/exams/examFilters.js'

const today = new Date(2026, 5, 10)
const exam = (id, dueDate, extra = {}) => ({
  id, title: `Prova ${id}`, disciplineName: 'Cálculo', disciplineId: 'd1', status: 'PENDING', dueDate, ...extra,
})
const ids = list => list.map(item => item.id)

test('próximos 7 dias inclui hoje e o sétimo dia, mas não o oitavo nem o passado', () => {
  const exams = [exam('ontem', '2026-06-09'), exam('hoje', '2026-06-10'), exam('d7', '2026-06-17'), exam('d8', '2026-06-18')]
  assert.deepEqual(ids(filterExams(exams, { period: 'next7', today })), ['hoje', 'd7'])
})

test('período mês considera mês e ano', () => {
  const exams = [exam('junho', '2026-06-30'), exam('junho-outro-ano', '2025-06-15'), exam('julho', '2026-07-01')]
  assert.deepEqual(ids(filterExams(exams, { period: 'month', today })), ['junho'])
})

test('filtros de status, disciplina e busca combinam', () => {
  const exams = [
    exam('a', '2026-06-11', { status: 'COMPLETED' }),
    exam('b', '2026-06-12', { disciplineId: 'd2', disciplineName: 'Física' }),
    exam('c', '2026-06-13'),
  ]
  assert.deepEqual(ids(filterExams(exams, { status: 'completed', today })), ['a'])
  assert.deepEqual(ids(filterExams(exams, { status: 'scheduled', discipline: 'd2', today })), ['b'])
  assert.deepEqual(ids(filterExams(exams, { search: 'físi', today })), ['b'])
})
