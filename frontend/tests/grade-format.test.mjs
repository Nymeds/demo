import assert from 'node:assert/strict'
import test from 'node:test'
import { formatAverage, formatScore, roundGrade } from '../src/shared/format/grade.js'
import { bandOf, formatGrade } from '../src/features/grades/gradesPresentation.js'

test('médias têm 1 casa decimal com arredondamento para cima na metade', () => {
  assert.equal(formatAverage(6.95), '7,0')
  assert.equal(formatAverage(6.94), '6,9')
  assert.equal(formatAverage(8), '8,0')
  assert.equal(roundGrade(6.95), 7)
  assert.equal(formatGrade(6.95), '7,0')
})

test('nota necessária tem 2 casas decimais', () => {
  assert.equal(formatScore(7.256), '7,26')
  assert.equal(formatScore(10), '10,00')
})

test('valores inválidos aparecem como travessão, nunca 0,00', () => {
  for (const value of [null, undefined, '', 'abc', NaN, Infinity]) {
    assert.equal(formatAverage(value), '—')
    assert.equal(formatScore(value), '—')
  }
})

test('faixa e exibição usam o mesmo arredondamento', () => {
  assert.equal(bandOf(6.95), bandOf(7))
})
