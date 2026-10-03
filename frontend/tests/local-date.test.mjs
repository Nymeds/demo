import assert from 'node:assert/strict'
import test from 'node:test'
import {
  daysBetween, isOverdue, normalizeLocalDateTime, parseLocalDate, parseLocalDateTime, startOfDay, todayIso, toLocalIso,
} from '../src/shared/date/localDate.js'

test('parseLocalDate interpreta a data no fuso local e rejeita datas inválidas', () => {
  const date = parseLocalDate('2026-03-05')
  assert.deepEqual([date.getFullYear(), date.getMonth(), date.getDate(), date.getHours()], [2026, 2, 5, 0])
  assert.equal(parseLocalDate('2026-02-31'), null)
  assert.equal(parseLocalDate(''), null)
  assert.equal(parseLocalDate(undefined), null)
})

test('toLocalIso e todayIso usam a data local, não UTC', () => {
  const lateNight = new Date(2026, 0, 31, 23, 59, 59)
  assert.equal(toLocalIso(lateNight), '2026-01-31')
  assert.equal(todayIso(lateNight), '2026-01-31')
})

test('daysBetween e startOfDay contam dias de calendário', () => {
  assert.equal(daysBetween(new Date(2026, 0, 1, 23, 0), new Date(2026, 0, 2, 1, 0)), 1)
  assert.equal(daysBetween(new Date(2026, 0, 10), new Date(2026, 0, 3)), -7)
  assert.equal(startOfDay(new Date(2026, 0, 1, 15, 30)).getHours(), 0)
})

test('isOverdue só considera vencida a data anterior a hoje', () => {
  const now = new Date(2026, 5, 10, 22, 0)
  assert.equal(isOverdue('2026-06-09', now), true)
  assert.equal(isOverdue('2026-06-10', now), false)
  assert.equal(isOverdue('2026-06-11', now), false)
  assert.equal(isOverdue('', now), false)
})

test('LocalDateTime da API é lido como horário local e normalizado', () => {
  const date = parseLocalDateTime('2026-06-10T08:30')
  assert.deepEqual([date.getDate(), date.getHours(), date.getMinutes()], [10, 8, 30])
  assert.equal(normalizeLocalDateTime('2026-06-10T08:30'), '2026-06-10T08:30:00')
  assert.equal(normalizeLocalDateTime('2026-06-10T08:30:15.123'), '2026-06-10T08:30:15')
  assert.equal(normalizeLocalDateTime('lixo'), '')
})
