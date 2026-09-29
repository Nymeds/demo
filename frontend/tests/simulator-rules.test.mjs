import assert from 'node:assert/strict'
import test from 'node:test'
import { parseSavedScenario, projectedAverage } from '../src/features/simulator/simulatorRules.js'

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
