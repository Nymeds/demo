import assert from 'node:assert/strict'
import test from 'node:test'
import {
  averageAttendanceOf,
  averageLabelOf,
  buildRow,
  filterRows,
  formatDate,
  formatPercentage,
  frequencyOf,
  HISTORY_PREVIEW_SIZE,
  periodOptionsOf,
  sortAbsenceHistory,
  totalAbsencesOf,
  visibleHistoryOf,
  withFrequency,
} from '../src/features/frequency/frequencyPresentation.js'

function apiDiscipline(overrides = {}) {
  return {
    id: 'd1',
    name: 'Cálculo I',
    professorName: 'Ana',
    color: '',
    minimumAttendancePercentage: '75',
    periodo: '2026',
    semester: '1',
    schedules: [{ dayOfWeek: 'MONDAY', startTime: '08:00:00', endTime: '10:00:00' }],
    attendancePercentage: null,
    absences: 0,
    maximumAbsences: 5,
    ...overrides,
  }
}

test('a frequência sai da própria lista de disciplinas, sem nova requisição', () => {
  const frequency = frequencyOf(apiDiscipline({ attendancePercentage: '85.00', absences: 3, maximumAbsences: 5 }))

  assert.deepEqual(frequency, { absences: 3, attendancePercentage: 85, maximumAbsences: 5 })
})

test('disciplina sem frequência cadastrada fica com frequency nula', () => {
  assert.equal(frequencyOf(apiDiscipline()), null)
  assert.equal(frequencyOf(apiDiscipline({ attendancePercentage: undefined })), null)
})

test('100% de frequência conta como cadastrada', () => {
  const frequency = frequencyOf(apiDiscipline({ attendancePercentage: 100, absences: 0 }))

  assert.equal(frequency.attendancePercentage, 100)
})

test('withFrequency normaliza cor padrão e horários e anexa a frequência', () => {
  const discipline = withFrequency(apiDiscipline({ attendancePercentage: '95', absences: 1 }))

  assert.equal(discipline.color, '#6432df')
  assert.equal(discipline.schedules[0].startTime, '08:00')
  assert.equal(discipline.schedules[0].endTime, '10:00')
  assert.equal(discipline.frequency.absences, 1)
})

test('buildRow sem frequência é neutra e usa o máximo teórico de faltas', () => {
  const row = buildRow(withFrequency(apiDiscipline()))

  assert.equal(row.configured, false)
  assert.equal(row.situation, 'neutral')
  assert.equal(row.attendancePercentage, null)
  assert.equal(row.remainingAbsences, 5)
  assert.equal(row.subtitle, 'Ana')
  assert.equal(row.period, '2026.1')
})

test('buildRow com frequência calcula faltas restantes e situação', () => {
  const good = buildRow(withFrequency(apiDiscipline({ attendancePercentage: 100, absences: 0 })))
  const bad = buildRow(withFrequency(apiDiscipline({ attendancePercentage: 65, absences: 7, maximumAbsences: 5 })))

  assert.equal(good.situation, 'good')
  assert.equal(good.remainingAbsences, 5)
  assert.equal(bad.situation, 'bad')
  assert.equal(bad.remainingAbsences, -2)
})

test('buildRow respeita a margem de aviso informada', () => {
  const discipline = withFrequency(apiDiscipline({ attendancePercentage: 85, absences: 3 }))

  assert.equal(buildRow(discipline, 5).situation, 'good')
  assert.equal(buildRow(discipline, 15).situation, 'warning')
})

test('sem professor o subtítulo cai para o período; sem período mostra travessão', () => {
  const noProfessor = buildRow(withFrequency(apiDiscipline({ professorName: '' })))
  const noPeriod = buildRow(withFrequency(apiDiscipline({ professorName: '', periodo: '' })))

  assert.equal(noProfessor.subtitle, '2026.1')
  assert.equal(noPeriod.subtitle, '—')
})

test('filtros combinam busca, período e situação', () => {
  const rows = [
    buildRow(withFrequency(apiDiscipline({ id: 'a', name: 'Cálculo I', attendancePercentage: 100 }))),
    buildRow(withFrequency(apiDiscipline({ id: 'b', name: 'Física', professorName: 'Bia', periodo: '2025', attendancePercentage: 65, absences: 7 }))),
  ]
  const all = { searchTerm: '', periodFilter: 'all', situationFilter: 'all' }

  assert.equal(filterRows(rows, all).length, 2)
  assert.deepEqual(filterRows(rows, { ...all, searchTerm: 'CÁLCULO' }).map(row => row.id), ['a'])
  assert.deepEqual(filterRows(rows, { ...all, searchTerm: ' cálculo ' }).map(row => row.id), ['a'])
  assert.deepEqual(filterRows(rows, { ...all, searchTerm: 'bia' }).map(row => row.id), ['b'])
  assert.deepEqual(filterRows(rows, { ...all, periodFilter: '2025.1' }).map(row => row.id), ['b'])
  assert.deepEqual(filterRows(rows, { ...all, situationFilter: 'bad' }).map(row => row.id), ['b'])
})

test('períodos disponíveis vêm sem repetição, do mais recente ao mais antigo', () => {
  const rows = [
    { period: '2025.2' },
    { period: '2026.1' },
    { period: '—' },
    { period: '2025.2' },
  ]

  assert.deepEqual(periodOptionsOf(rows), ['2026.1', '2025.2'])
})

test('média e total de faltas ignoram disciplinas sem frequência', () => {
  const rows = [
    { attendancePercentage: 100, absences: 0 },
    { attendancePercentage: 80, absences: 4 },
    { attendancePercentage: null, absences: 0 },
  ]

  assert.equal(averageAttendanceOf(rows), 90)
  assert.equal(totalAbsencesOf(rows), 4)
  assert.equal(averageAttendanceOf([{ attendancePercentage: null, absences: 0 }]), null)
})

test('o rótulo da média acompanha as faixas de frequência', () => {
  assert.equal(averageLabelOf(null), 'Aguardando configuração')
  assert.equal(averageLabelOf(90), 'Boa frequência')
  assert.equal(averageLabelOf(75), 'Atenção às faltas')
  assert.equal(averageLabelOf(74.9), 'Frequência crítica')
})

test('formatPercentage e formatDate usam o padrão brasileiro', () => {
  assert.equal(formatPercentage(85), '85%')
  assert.equal(formatPercentage(null), '—')
  assert.equal(formatPercentage(85.5, 1), '85,5%')
  assert.equal(formatDate('2026-03-02'), '02/03/2026 (Seg)')
  assert.equal(formatDate(''), '—')
})

test('histórico ordena por data e depois por criação, do mais novo ao mais antigo', () => {
  const entries = [
    { id: 1, date: '2026-03-01', createdAt: '2026-03-01T10:00:00Z' },
    { id: 2, date: '2026-03-05', createdAt: '2026-03-05T08:00:00Z' },
    { id: 3, date: '2026-03-05', createdAt: '2026-03-05T09:00:00Z' },
  ]
  const sorted = sortAbsenceHistory(entries)

  assert.deepEqual(sorted.map(entry => entry.id), [3, 2, 1])
  assert.deepEqual(entries.map(entry => entry.id), [1, 2, 3])
})

test('o histórico mostra só os primeiros lançamentos até pedir para ver todos', () => {
  const entries = Array.from({ length: HISTORY_PREVIEW_SIZE + 2 }, (_, index) => ({ id: index }))

  assert.equal(visibleHistoryOf(entries, false).length, HISTORY_PREVIEW_SIZE)
  assert.equal(visibleHistoryOf(entries, true).length, entries.length)
})
