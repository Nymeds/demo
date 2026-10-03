import assert from 'node:assert/strict'
import test from 'node:test'
import { resolveActiveDashboard } from '../src/shared/dashboards/useActiveDashboard.js'
import { exceedsPasswordBytes, passwordByteLength } from '../src/shared/auth/passwordRules.js'
import { ATTENTION_MARGIN, loadPreferences, normalizePreferences } from '../src/shared/settings/preferences.js'

test('loadPreferences trata null e undefined como ausentes, não como zero', async () => {
  const preferences = await loadPreferences(async () => ({
    attendanceAlertMargin: null,
    deadlineAlertDays: undefined,
    startSection: null,
  }))

  assert.equal(preferences.attendanceAlertMargin, ATTENTION_MARGIN)
  assert.equal(preferences.deadlineAlertDays, 3)
  assert.equal(preferences.startSection, 'DASHBOARD')
  assert.equal(preferences.loaded, true)
})

test('loadPreferences preserva zero válido e usa os padrões quando a API falha', async () => {
  const zero = await loadPreferences(async () => ({ attendanceAlertMargin: 0, deadlineAlertDays: 7, startSection: 'EXAMS' }))
  assert.deepEqual(
    { margin: zero.attendanceAlertMargin, days: zero.deadlineAlertDays, section: zero.startSection },
    { margin: 0, days: 7, section: 'EXAMS' },
  )

  const failed = await loadPreferences(async () => { throw new Error('offline') })
  assert.equal(failed.loaded, false)
  assert.equal(failed.attendanceAlertMargin, ATTENTION_MARGIN)
  assert.equal(normalizePreferences('lixo').deadlineAlertDays, 3)
})

test('resolveActiveDashboard prefere ACTIVE, depois o mais recente, depois o maior id', () => {
  assert.equal(resolveActiveDashboard([]), null)
  assert.equal(resolveActiveDashboard(null), null)

  const picked = resolveActiveDashboard([
    { id: '1', status: 'ARCHIVED', updatedAt: '2026-09-01T00:00:00Z' },
    { id: '2', status: 'ACTIVE', updatedAt: '2026-01-01T00:00:00Z' },
    { id: '3', status: 'ACTIVE', updatedAt: '2026-05-01T00:00:00Z' },
  ])
  assert.equal(picked.id, '3')

  assert.equal(resolveActiveDashboard([
    { id: '9', status: 'ACTIVE' },
    { id: '10', status: 'ACTIVE' },
  ]).id, '10')

  assert.equal(resolveActiveDashboard([
    { id: '5', status: 'ACTIVE' },
    { id: '4', status: 'ACTIVE', createdAt: '2026-02-01T00:00:00Z' },
  ]).id, '4')

  assert.equal(resolveActiveDashboard([
    { id: '1', status: 'ARCHIVED', updatedAt: '2026-01-01T00:00:00Z' },
    { id: '2', status: 'ARCHIVED', updatedAt: '2026-03-01T00:00:00Z' },
  ]).id, '2')
})

test('limite de senha em bytes conta acentos em dobro', () => {
  assert.equal(passwordByteLength('ação'), 6)
  assert.equal(exceedsPasswordBytes('a'.repeat(72)), false)
  assert.equal(exceedsPasswordBytes('é'.repeat(37)), true)
})
