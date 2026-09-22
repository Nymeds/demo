import assert from 'node:assert/strict'
import test from 'node:test'
import {
  frequencySituation,
  LOSS_PER_ABSENCE,
  attendanceAfterAbsences,
  maximumAbsencesFor,
} from '../src/features/frequency/frequencyRules.js'

test('cada falta reduz cinco pontos percentuais', () => {
  assert.equal(LOSS_PER_ABSENCE, 5)
  assert.equal(attendanceAfterAbsences(0), 100)
  assert.equal(attendanceAfterAbsences(1), 95)
  assert.equal(attendanceAfterAbsences(5), 75)
  assert.equal(attendanceAfterAbsences(6), 70)
})

test('a frequência nunca fica negativa', () => {
  assert.equal(attendanceAfterAbsences(20), 0)
  assert.equal(attendanceAfterAbsences(21), 0)
})

test('o mínimo de 75% permite cinco faltas', () => {
  assert.equal(maximumAbsencesFor(75), 5)
  assert.equal(maximumAbsencesFor(76), 4)
})

test('a situação muda de verde para laranja e vermelho conforme a frequência cai', () => {
  assert.equal(frequencySituation(100, 75, 5), 'good')
  assert.equal(frequencySituation(80, 75, 1), 'warning')
  assert.equal(frequencySituation(75, 75, 0), 'warning')
  assert.equal(frequencySituation(70, 75, -1), 'bad')
})
