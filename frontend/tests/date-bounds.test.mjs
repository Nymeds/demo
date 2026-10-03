import assert from 'node:assert/strict'
import test from 'node:test'
import { clampToBounds, isOutOfBounds, monthHasDaysInBounds, yearsInBounds } from '../src/shared/date/dateBounds.js'

test('isOutOfBounds respects min and max and ignores empty bounds', () => {
  assert.equal(isOutOfBounds('2026-05-10', '2026-05-11', ''), true)
  assert.equal(isOutOfBounds('2026-05-11', '2026-05-11', '2026-05-20'), false)
  assert.equal(isOutOfBounds('2026-05-21', '2026-05-11', '2026-05-20'), true)
  assert.equal(isOutOfBounds('1999-01-01', '', ''), false)
})

test('clampToBounds returns nearest allowed date', () => {
  assert.equal(clampToBounds('2026-01-01', '2026-05-11', '2026-05-20'), '2026-05-11')
  assert.equal(clampToBounds('2026-12-01', '2026-05-11', '2026-05-20'), '2026-05-20')
  assert.equal(clampToBounds('2026-05-15', '2026-05-11', '2026-05-20'), '2026-05-15')
})

test('monthHasDaysInBounds blocks months fully outside bounds', () => {
  assert.equal(monthHasDaysInBounds(2026, 3, '2026-05-11', ''), false)
  assert.equal(monthHasDaysInBounds(2026, 4, '2026-05-11', ''), true)
  assert.equal(monthHasDaysInBounds(2026, 5, '', '2026-05-31'), false)
  assert.equal(monthHasDaysInBounds(2026, 4, '', '2026-05-31'), true)
})

test('yearsInBounds lists years from max down to min', () => {
  assert.deepEqual(yearsInBounds('2020-05-01', '2023-01-31'), [2023, 2022, 2021, 2020])
  assert.deepEqual(yearsInBounds('2024-01-01', '2024-12-31'), [2024])
})

test('yearsInBounds falls back to today and a 100-year span', () => {
  const today = new Date(2026, 8, 30)
  const years = yearsInBounds('', '2026-09-29', today)
  assert.equal(years.length, 101)
  assert.equal(years[0], 2026)
  assert.equal(years.at(-1), 1926)
  assert.deepEqual(yearsInBounds('', '', today, 2), [2026, 2025, 2024])
  assert.deepEqual(yearsInBounds('2030-01-01', '', today), [2030])
})
