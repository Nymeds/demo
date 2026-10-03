import assert from 'node:assert/strict'
import test from 'node:test'
import {
  countByStatus,
  filterActivities,
  formatDate,
  isOverdue,
  normalizeActivity,
  statusDetails,
} from '../src/features/activities/activitiesPresentation.js'

const activities = [
  { id: '1', title: 'Zoologia', description: 'Relatório de campo', status: 'PENDING', dueDate: '2099-03-01', disciplineName: 'Biologia' },
  { id: '2', title: 'Álgebra', description: null, status: 'COMPLETED', dueDate: '2099-01-01', disciplineName: 'Matemática' },
  { id: '3', title: 'Cálculo', description: '', status: 'IN_PROGRESS', dueDate: '2099-02-01', disciplineName: 'Matemática' },
]

const titles = list => list.map(activity => activity.title)

test('atividades são ordenadas por prazo ou título', () => {
  assert.deepEqual(titles(filterActivities(activities, '', 'all', 'dueAsc')), ['Álgebra', 'Cálculo', 'Zoologia'])
  assert.deepEqual(titles(filterActivities(activities, '', 'all', 'dueDesc')), ['Zoologia', 'Cálculo', 'Álgebra'])
  assert.deepEqual(titles(filterActivities(activities, '', 'all', 'titleAsc')), ['Álgebra', 'Cálculo', 'Zoologia'])
  assert.deepEqual(titles(filterActivities(activities, '', 'all', 'titleDesc')), ['Zoologia', 'Cálculo', 'Álgebra'])
})

test('busca considera título, descrição e disciplina sem diferenciar maiúsculas', () => {
  assert.deepEqual(titles(filterActivities(activities, '  ZOO ', 'all', 'dueAsc')), ['Zoologia'])
  assert.deepEqual(titles(filterActivities(activities, 'campo', 'all', 'dueAsc')), ['Zoologia'])
  assert.deepEqual(titles(filterActivities(activities, 'matemática', 'all', 'dueAsc')), ['Álgebra', 'Cálculo'])
  assert.deepEqual(titles(filterActivities(activities, 'inexistente', 'all', 'dueAsc')), [])
})

test('filtros de status combinam com a busca', () => {
  assert.deepEqual(titles(filterActivities(activities, '', 'pending', 'dueAsc')), ['Zoologia'])
  assert.deepEqual(titles(filterActivities(activities, '', 'progress', 'dueAsc')), ['Cálculo'])
  assert.deepEqual(titles(filterActivities(activities, '', 'completed', 'dueAsc')), ['Álgebra'])
  assert.deepEqual(titles(filterActivities(activities, 'matemática', 'progress', 'dueAsc')), ['Cálculo'])
})

test('contagem por status e detalhes de status', () => {
  assert.equal(countByStatus(activities, 'PENDING'), 1)
  assert.equal(countByStatus(activities, 'COMPLETED'), 1)
  assert.deepEqual(statusDetails('IN_PROGRESS'), { label: 'Em andamento', className: 'is-progress' })
  assert.deepEqual(statusDetails('DESCONHECIDO'), { label: 'Pendente', className: 'is-pending' })
})

test('normalizeActivity usa disciplina e cor padrão quando não encontra', () => {
  const disciplines = [{ id: 'd1', name: 'Curso', color: '#123456' }]
  assert.deepEqual(normalizeActivity({ id: '1', disciplineId: 'd1' }, disciplines), {
    id: '1', disciplineId: 'd1', disciplineName: 'Curso', disciplineColor: '#123456',
  })
  assert.deepEqual(normalizeActivity({ id: '2', disciplineId: 'x' }, disciplines), {
    id: '2', disciplineId: 'x', disciplineName: 'Disciplina', disciplineColor: '#6432df',
  })
})

test('atraso ignora atividades concluídas e sem prazo; data inválida vira travessão', () => {
  assert.equal(isOverdue({ status: 'PENDING', dueDate: '2000-01-01' }), true)
  assert.equal(isOverdue({ status: 'COMPLETED', dueDate: '2000-01-01' }), false)
  assert.equal(isOverdue({ status: 'PENDING', dueDate: '' }), false)
  assert.equal(isOverdue({ status: 'PENDING', dueDate: '2099-01-01' }), false)
  assert.equal(formatDate(''), '—')
  assert.match(formatDate('2026-03-05'), /05/)
})
