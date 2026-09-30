import assert from 'node:assert/strict'
import test from 'node:test'
import { clampToBounds, isOutOfBounds, monthHasDaysInBounds } from '../src/shared/date/dateBounds.js'

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
