import { test } from 'node:test'
import assert from 'node:assert/strict'
import { once } from 'node:events'
import nodemailer from 'nodemailer'
import { createMailerServer } from '../src/server.js'
import { readConfig, createTransport } from '../src/config.js'

const secret = 'test-secret-with-at-least-32-bytes'

async function withServer(transport, run) {
  const server = createMailerServer({ secret, transport, user: 'sender@gmail.com', fromName: 'AcadOrganize' })
  server.listen(0, '127.0.0.1')
  await once(server, 'listening')
  try {
    await run(`http://127.0.0.1:${server.address().port}`)
  } finally {
    server.closeAllConnections()
    await new Promise(resolve => server.close(resolve))
  }
}

function send(url, body, authorization = `Bearer ${secret}`) {
  return fetch(`${url}/internal/recovery-email`, {
    method: 'POST', headers: { 'Content-Type': 'application/json', Authorization: authorization },
    body: typeof body === 'string' ? body : JSON.stringify(body),
  })
}

test('requires internal authentication and rejects unsafe payloads before SMTP', async () => {
  let calls = 0
  await withServer({ sendMail: async () => { calls++ } }, async url => {
    const valid = { email: 'student@example.com', code: '012345' }
    assert.equal((await send(url, valid, '')).status, 401)
    assert.equal((await send(url, valid, 'Bearer wrong')).status, 401)
    assert.equal((await send(url, { ...valid, email: 'a@example.com,b@example.com' })).status, 400)
    assert.equal((await send(url, { ...valid, email: 'a@example.com\r\nBcc: b@example.com' })).status, 400)
    assert.equal((await send(url, { ...valid, code: '<script>' })).status, 400)
    assert.equal((await send(url, '{')).status, 400)
    assert.equal((await send(url, null)).status, 400)
    assert.equal((await send(url, 'x'.repeat(3000))).status, 413)
    assert.equal(calls, 0)
  })
})

test('renders actual Nodemailer message with leading-zero code and expiry in text and HTML', async () => {
  const transport = nodemailer.createTransport({ jsonTransport: true })
  let message
  await withServer({ sendMail: async input => { message = JSON.parse((await transport.sendMail(input)).message) } }, async url => {
    const response = await send(url, { email: 'student@example.com', code: '012345' })
    assert.equal(response.status, 204)
    assert.equal(response.headers.get('cache-control'), 'no-store')
    assert.equal(await response.text(), '')
    assert.match(message.text, /012345/)
    assert.match(message.html, /012345/)
    assert.match(message.text, /10 minutos/)
    assert.equal(message.to[0].address, 'student@example.com')
    assert.equal(message.from.address, 'sender@gmail.com')
  })
})

test('SMTP failure exposes no provider errors or codes', async () => {
  await withServer({ sendMail: async () => { throw new Error('sensitive SMTP credentials') } }, async url => {
    const response = await send(url, { email: 'student@example.com', code: '012345' })
    assert.equal(response.status, 503)
    assert.equal(await response.text(), '')
  })
})

test('Gmail defaults require secrets and enforce verified TLS', () => {
  assert.throws(() => readConfig({}), /MAILER_API_SECRET/)
  assert.throws(() => readConfig({ MAILER_API_SECRET: secret }), /SMTP_USER/)
  const config = readConfig({ MAILER_API_SECRET: secret, SMTP_USER: 'sender@gmail.com', SMTP_PASSWORD: 'abcdefghijklmnop' })
  assert.equal(config.port, 'auto')
  const transport = createTransport({ ...config, port: 465 })
  assert.equal(transport.options.host, 'smtp.gmail.com')
  assert.equal(transport.options.secure, true)
  assert.equal(transport.options.tls.rejectUnauthorized, true)
  assert.equal(transport.options.disableFileAccess, true)
  assert.equal(transport.options.disableUrlAccess, true)
  transport.close()
  const startTls = createTransport({ ...config, port: 587 })
  assert.equal(startTls.options.secure, false)
  assert.equal(startTls.options.requireTLS, true)
  assert.equal(startTls.options.tls.rejectUnauthorized, true)
  startTls.close()
})

test('health requires authentication and does not send email', async () => {
  await withServer({ sendMail: () => assert.fail('Health must not send mail') }, async url => {
    assert.equal((await fetch(`${url}/internal/health`)).status, 401)
    const response = await fetch(`${url}/internal/health`, { headers: { Authorization: `Bearer ${secret}` } })
    assert.equal(response.status, 200)
    assert.equal((await response.json()).status, 'ready')
    assert.equal(response.headers.get('cache-control'), 'no-store')
  })
})
