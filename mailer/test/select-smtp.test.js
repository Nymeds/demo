import { test } from 'node:test'
import assert from 'node:assert/strict'
import { selectSmtp } from '../src/select-smtp.js'

function fixture(failures) {
  const attempts = [], closed = [], logs = []
  return { attempts, closed, logs, factory: config => ({
    verify: async () => {
      attempts.push(config.port)
      if (failures[config.port]) throw Object.assign(new Error('PRIVATE PASSWORD'), { code: failures[config.port] })
    },
    close: () => closed.push(config.port),
  }), report: message => logs.push(message) }
}

test('selects the first verified port and does not attempt the next one', async () => {
  const f = fixture({})
  assert.equal((await selectSmtp({ port: 'auto' }, f)).port, 465)
  assert.deepEqual(f.attempts, [465])
  assert.deepEqual(f.closed, [])
})

test('timeout on 465 falls back to authenticated 587 and closes failed transport', async () => {
  const f = fixture({ 465: 'ETIMEDOUT' })
  assert.equal((await selectSmtp({ port: 'auto' }, f)).port, 587)
  assert.deepEqual(f.attempts, [465, 587])
  assert.deepEqual(f.closed, [465])
  assert.ok(f.logs.every(message => !message.includes('PRIVATE PASSWORD')))
})

test('failure on both ports closes transports and does not return a ready mailer', async () => {
  const f = fixture({ 465: 'ETIMEDOUT', 587: 'ESOCKET' })
  await assert.rejects(selectSmtp({ port: 'auto' }, f), /Nenhuma porta/)
  assert.deepEqual(f.closed, [465, 587])
})

test('authentication rejection aborts retries without exposing provider errors', async () => {
  const f = fixture({ 465: 'EAUTH' })
  await assert.rejects(selectSmtp({ port: 'auto' }, f), /Nenhuma porta/)
  assert.deepEqual(f.attempts, [465])
  assert.ok(f.logs.every(message => !message.includes('PRIVATE PASSWORD')))
})

test('an explicitly configured SMTP port is respected', async () => {
  const f = fixture({ 587: 'ETIMEDOUT' })
  await assert.rejects(selectSmtp({ port: 587 }, f))
  assert.deepEqual(f.attempts, [587])
})
