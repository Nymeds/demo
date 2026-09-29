import assert from 'node:assert/strict'
import test from 'node:test'
import {
  currentAverageOf,
  filterDisciplinesByPeriod,
  linkedActivityLabel,
  notesFromGrades,
  parseSavedScenario,
  periodsOf,
  projectedAverage,
  simulationStorageKey,
  validateDesiredAverage
} from '../src/features/simulator/simulatorRules.js'

test('cenário salvo válido é restaurado', () => {
  assert.deepEqual(parseSavedScenario('{"desiredAverage":7.5}'), { desiredAverage: 7.5 })
  assert.deepEqual(parseSavedScenario('{"desiredAverage":0}'), { desiredAverage: 0 })
})

test('cenário salvo corrompido ou fora de 0..10 é descartado', () => {
  const invalid = [null, '', 'não é json', '{}', '{"desiredAverage":"7"}', '{"desiredAverage":11}', '{"desiredAverage":-1}', '{"desiredAverage":null}']
  for (const raw of invalid) assert.equal(parseSavedScenario(raw), null, String(raw))
})

test('média projetada considera as notas atuais e rejeita valores inválidos', () => {
  assert.equal(projectedAverage([6, 8], 10), 8)
  assert.equal(projectedAverage([], 7), 7)
  assert.equal(projectedAverage([6], 'abc'), null)
  assert.equal(projectedAverage([6, 'x'], 5), null)
  assert.equal(projectedAverage([6], null), null)
})

test('períodos saem sem repetição, do mais recente para o mais antigo', () => {
  const disciplines = [
    { id: 1, semester: '2025.2' },
    { id: 2, semester: '2026.1' },
    { id: 3, periodo: '2', semester: '2026.2' },
    { id: 4, semester: '2026.1' },
    { id: 5, semester: '' }
  ]
  assert.deepEqual(periodsOf(disciplines).map(item => item.value), ['2026.2', '2026.1', '2025.2'])
  assert.deepEqual(periodsOf([]), [])
})

test('filtro por período mantém tudo quando vazio e ignora disciplinas sem período', () => {
  const disciplines = [{ id: 1, semester: '2026.1' }, { id: 2, semester: '2026.2' }, { id: 3, semester: '' }]
  assert.equal(filterDisciplinesByPeriod(disciplines, ''), disciplines)
  assert.deepEqual(filterDisciplinesByPeriod(disciplines, '2026.2').map(item => item.id), [2])
})

test('notas da API viram notas da tela, da mais recente para a mais antiga', () => {
  const notes = notesFromGrades([
    { id: 1, assessmentName: 'P1', score: '7.5', recordedAt: '2026-03-01', activityId: 9 },
    { id: 2, assessmentName: 'P2', score: 8, recordedAt: '2026-04-01' }
  ])
  assert.deepEqual(notes, [
    { id: 2, name: 'P2', value: 8, recordedAt: '2026-04-01', activityId: null },
    { id: 1, name: 'P1', value: 7.5, recordedAt: '2026-03-01', activityId: 9 }
  ])
})

test('média atual é a média simples das notas e zero sem notas', () => {
  assert.equal(currentAverageOf([]), 0)
  assert.equal(currentAverageOf([{ value: 6 }, { value: '8' }]), 7)
})

test('rótulo da avaliação vinculada', () => {
  const activities = [{ id: 3, title: 'Prova 1' }]
  assert.equal(linkedActivityLabel({ activityId: null }, activities), 'Sem avaliação vinculada')
  assert.equal(linkedActivityLabel({ activityId: 3 }, activities), 'Vinculada a Prova 1')
  assert.equal(linkedActivityLabel({ activityId: 4 }, activities), 'Avaliação vinculada')
})

test('validação da média desejada explica o que corrigir', () => {
  assert.equal(validateDesiredAverage('', 10), 'Informe a média desejada.')
  assert.equal(validateDesiredAverage(null, 10), 'Informe a média desejada.')
  assert.equal(validateDesiredAverage(-1, 10), 'A média desejada não pode ser negativa.')
  assert.equal(validateDesiredAverage(10.5, 10), 'A média desejada deve ser de no máximo 10.')
  assert.equal(validateDesiredAverage(8.5, 10), '')
  assert.equal(validateDesiredAverage(0, 10), '')
})

test('chave de armazenamento local é por painel e disciplina', () => {
  assert.equal(simulationStorageKey('d1', 'x2'), 'studdy:simulator:d1:x2')
})
