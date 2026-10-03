import assert from 'node:assert/strict'
import test from 'node:test'
import {
  attendanceLabel,
  attendanceSituation,
  averageOf,
  disciplineAttendanceSituation,
  filterAndSortDisciplines,
  normalizeDiscipline,
  statusDetails,
  statusMenuPosition,
} from '../src/features/disciplines/disciplinesPresentation.js'

const discipline = (id, name, extra = {}) => ({ id, name, status: 'IN_PROGRESS', professorName: null, createdAt: '2026-01-01T00:00:00', ...extra })
const names = list => list.map(item => item.name)
const view = (overrides = {}) => ({ searchTerm: '', activeFilter: 'all', sortOrder: 'nameAsc', ...overrides })

test('normaliza cor, situação e horários sem segundos', () => {
  const normalized = normalizeDiscipline({
    id: 'd1',
    color: null,
    status: null,
    schedules: [{ dayOfWeek: 'MONDAY', startTime: '08:00:00', endTime: '10:00:00' }],
  })

  assert.equal(normalized.color, '#6432df')
  assert.equal(normalized.status, 'IN_PROGRESS')
  assert.deepEqual(normalized.schedules, [{ dayOfWeek: 'MONDAY', startTime: '08:00', endTime: '10:00' }])
})

test('busca por nome ou professor, ignorando maiúsculas', () => {
  const list = [
    discipline('1', 'Cálculo I', { professorName: 'Ana Souza' }),
    discipline('2', 'Física'),
    discipline('3', 'Álgebra', { professorName: 'Carlos' }),
  ]

  assert.deepEqual(names(filterAndSortDisciplines(list, view({ searchTerm: ' cálc ' }))), ['Cálculo I'])
  assert.deepEqual(names(filterAndSortDisciplines(list, view({ searchTerm: 'SOUZA' }))), ['Cálculo I'])
  assert.deepEqual(names(filterAndSortDisciplines(list, view({ searchTerm: 'inexistente' }))), [])
})

test('filtra por situação', () => {
  const list = [
    discipline('1', 'A', { status: 'IN_PROGRESS' }),
    discipline('2', 'B', { status: 'COMPLETED' }),
    discipline('3', 'C', { status: 'LOCKED' }),
  ]

  assert.deepEqual(names(filterAndSortDisciplines(list, view({ activeFilter: 'all' }))), ['A', 'B', 'C'])
  assert.deepEqual(names(filterAndSortDisciplines(list, view({ activeFilter: 'active' }))), ['A'])
  assert.deepEqual(names(filterAndSortDisciplines(list, view({ activeFilter: 'finished' }))), ['B'])
  assert.deepEqual(names(filterAndSortDisciplines(list, view({ activeFilter: 'locked' }))), ['C'])
})

test('ordena por nome (A-Z e Z-A) e por mais recentes, sem alterar a lista original', () => {
  const list = [
    discipline('1', 'Banco de Dados', { createdAt: '2026-02-01T00:00:00' }),
    discipline('2', 'Álgebra', { createdAt: '2026-03-01T00:00:00' }),
    discipline('3', 'Redes', { createdAt: '2026-01-01T00:00:00' }),
    discipline('4', 'Sem data', { createdAt: undefined }),
  ]

  assert.deepEqual(names(filterAndSortDisciplines(list, view())), ['Álgebra', 'Banco de Dados', 'Redes', 'Sem data'])
  assert.deepEqual(names(filterAndSortDisciplines(list, view({ sortOrder: 'nameDesc' }))), ['Sem data', 'Redes', 'Banco de Dados', 'Álgebra'])
  assert.deepEqual(names(filterAndSortDisciplines(list, view({ sortOrder: 'newest' }))), ['Álgebra', 'Banco de Dados', 'Redes', 'Sem data'])
  assert.deepEqual(names(list), ['Banco de Dados', 'Álgebra', 'Redes', 'Sem data'])
})

test('média considera só valores numéricos e devolve null sem dados', () => {
  const list = [{ average: 8 }, { average: null }, { average: 6 }, {}]

  assert.equal(averageOf(list, 'average'), 7)
  assert.equal(averageOf([{ average: null }], 'average'), null)
  assert.equal(averageOf([], 'attendancePercentage'), null)
})

test('rótulo de frequência arredonda ou avisa que não há dado', () => {
  assert.equal(attendanceLabel(86.6), '87%')
  assert.equal(attendanceLabel(null), 'Sem frequência cadastrada')
})

test('situação da frequência: neutra sem dado, ruim abaixo do mínimo, alerta perto dele', () => {
  const margin = 5

  assert.equal(attendanceSituation(null, 75, Number.POSITIVE_INFINITY, margin), 'neutral')
  assert.equal(attendanceSituation(70, 75, Number.POSITIVE_INFINITY, margin), 'bad')
  assert.equal(attendanceSituation(77, 75, Number.POSITIVE_INFINITY, margin), 'warning')
  assert.equal(attendanceSituation(90, 75, Number.POSITIVE_INFINITY, margin), 'good')
  assert.equal(attendanceSituation(90, undefined, Number.POSITIVE_INFINITY, margin), 'good')
})

test('situação da disciplina usa as faltas restantes', () => {
  const base = { attendancePercentage: 90, minimumAttendancePercentage: 75 }

  assert.equal(disciplineAttendanceSituation({ ...base, maximumAbsences: 10, absences: 2 }, 5), 'good')
  assert.equal(disciplineAttendanceSituation({ ...base, maximumAbsences: 10, absences: 9 }, 5), 'warning')
  assert.equal(disciplineAttendanceSituation({ ...base, maximumAbsences: undefined, absences: 9 }, 5), 'good')
  assert.equal(disciplineAttendanceSituation({ attendancePercentage: null }, 5), 'neutral')
})

test('detalhes da situação caem em "Em andamento" quando desconhecida', () => {
  assert.equal(statusDetails('COMPLETED').label, 'Concluída')
  assert.equal(statusDetails('LOCKED').className, 'is-neutral')
  assert.equal(statusDetails('OUTRA').label, 'Em andamento')
})

test('menu de situação abre abaixo do gatilho quando cabe e acima quando não cabe', () => {
  const viewport = { width: 1000, height: 800 }

  assert.deepEqual(statusMenuPosition({ right: 500, top: 100, bottom: 132 }, viewport), { left: 310, top: 139 })
  assert.deepEqual(statusMenuPosition({ right: 500, top: 700, bottom: 732 }, viewport), { left: 310, top: 537 })
})

test('menu de situação respeita as margens laterais da janela', () => {
  const viewport = { width: 400, height: 800 }

  assert.equal(statusMenuPosition({ right: 50, top: 100, bottom: 132 }, viewport).left, 12)
  assert.equal(statusMenuPosition({ right: 900, top: 100, bottom: 132 }, viewport).left, 198)
})
