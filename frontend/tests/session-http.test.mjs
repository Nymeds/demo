import assert from 'node:assert/strict'
import test, { beforeEach } from 'node:test'
import {
  beginDeliberateLogout,
  clearSession,
  getSession,
  getSessionGeneration,
  onSessionExpired,
  onSessionRemovedElsewhere,
  saveSession,
  SESSION_STORAGE_KEY,
} from '../src/shared/auth/session.js'
import { apiRequest, SessionExpiredError } from '../src/shared/http/apiRequest.js'

class MemoryStorage {
  #data = new Map()
  get length() { return this.#data.size }
  key(index) { return [...this.#data.keys()][index] ?? null }
  getItem(key) { return this.#data.has(key) ? this.#data.get(key) : null }
  setItem(key, value) { this.#data.set(key, String(value)) }
  removeItem(key) { this.#data.delete(key) }
}

let listeners
const realFetch = globalThis.fetch

beforeEach(() => {
  listeners = []
  globalThis.window = {
    localStorage: new MemoryStorage(),
    sessionStorage: new MemoryStorage(),
    addEventListener: (type, handler) => listeners.push({ type, handler }),
    removeEventListener: (type, handler) => { listeners = listeners.filter(item => item.handler !== handler) },
  }
  // Uma nova sessão zera o estado global de logout deliberado deixado por testes anteriores.
  saveSession({ accessToken: 'reset', refreshToken: 'reset' })
  clearSession()
})

const json = (status, data = {}) => ({ ok: status >= 200 && status < 300, status, json: async () => data })
const bearer = init => init?.headers?.Authorization
const tick = () => new Promise(resolve => setImmediate(resolve))

function restoreFetch() {
  globalThis.fetch = realFetch
}

test('401s simultâneos disparam uma única renovação e todas as requisições são repetidas', async t => {
  t.after(restoreFetch)
  saveSession({ accessToken: 'old', refreshToken: 'r1', expiresIn: 60 })
  let refreshCalls = 0

  globalThis.fetch = async (path, init) => {
    if (path === '/api/v1/auth/refresh') {
      refreshCalls += 1
      return json(200, { accessToken: 'new', refreshToken: 'r2', expiresIn: 60 })
    }
    return bearer(init) === 'Bearer new' ? json(200, { ok: path }) : json(401)
  }

  const results = await Promise.all([
    apiRequest('/api/v1/a'),
    apiRequest('/api/v1/b'),
    apiRequest('/api/v1/c'),
  ])

  assert.equal(refreshCalls, 1)
  assert.deepEqual(results.map(item => item.ok), ['/api/v1/a', '/api/v1/b', '/api/v1/c'])
  assert.equal(getSession().refreshToken, 'r2')
})

test('falha na renovação limpa a sessão, lança SessionExpiredError e avisa uma vez', async t => {
  t.after(restoreFetch)
  saveSession({ accessToken: 'old', refreshToken: 'r1' })
  let expiredEvents = 0
  const stop = onSessionExpired(() => { expiredEvents += 1 })
  t.after(stop)

  globalThis.fetch = async () => json(401)

  const outcomes = await Promise.allSettled([apiRequest('/api/v1/a'), apiRequest('/api/v1/b')])

  assert.ok(outcomes.every(item => item.status === 'rejected' && item.reason instanceof SessionExpiredError))
  assert.equal(getSession(), null)
  assert.equal(expiredEvents, 1)
})

test('logout durante a renovação não ressuscita a sessão nem mostra "sessão expirou"', async t => {
  t.after(restoreFetch)
  saveSession({ accessToken: 'old', refreshToken: 'r1' })
  let expiredEvents = 0
  const stop = onSessionExpired(() => { expiredEvents += 1 })
  t.after(stop)
  let releaseRefresh

  globalThis.fetch = async path => {
    if (path === '/api/v1/auth/refresh') {
      await new Promise(resolve => { releaseRefresh = resolve })
      return json(200, { accessToken: 'new', refreshToken: 'r2' })
    }
    return json(401)
  }

  const pending = apiRequest('/api/v1/a')
  await tick()

  beginDeliberateLogout()
  clearSession()
  releaseRefresh()

  await assert.rejects(pending, SessionExpiredError)
  assert.equal(getSession(), null)
  assert.equal(expiredEvents, 0)
})

test('resposta tardia de uma sessão anterior não derruba um login novo', async t => {
  t.after(restoreFetch)
  saveSession({ accessToken: 'old', refreshToken: 'r1' })
  let releaseFirst

  globalThis.fetch = async (path, init) => {
    if (path === '/api/v1/slow') {
      await new Promise(resolve => { releaseFirst = resolve })
      return json(401)
    }
    return bearer(init) ? json(200) : json(401)
  }

  const pending = apiRequest('/api/v1/slow')
  await tick()
  clearSession()
  saveSession({ accessToken: 'fresh', refreshToken: 'r9' })
  releaseFirst()

  await assert.rejects(pending, SessionExpiredError)
  assert.equal(getSession()?.accessToken, 'fresh')
})

test('a chave antiga de sessão é migrada uma única vez', () => {
  window.localStorage.setItem('acad-organize.access-token', 'legacy-token')

  const migrated = getSession()

  assert.equal(migrated.accessToken, 'legacy-token')
  assert.equal(migrated.rememberMe, true)
  assert.equal(window.localStorage.getItem('acad-organize.access-token'), null)
  assert.equal(JSON.parse(window.localStorage.getItem(SESSION_STORAGE_KEY)).accessToken, 'legacy-token')
})

test('clearSession remove cenários do simulador por prefixo e avança a geração', () => {
  saveSession({ accessToken: 'a', refreshToken: 'b' }, { rememberMe: true })
  window.localStorage.setItem('studdy:simulator:user-1', '[]')
  window.localStorage.setItem('studdy:simulator:user-2', '[]')
  window.sessionStorage.setItem('studdy:simulator:tmp', '[]')
  window.localStorage.setItem('outra-chave', 'fica')
  const before = getSessionGeneration()

  clearSession()

  assert.equal(window.localStorage.getItem('studdy:simulator:user-1'), null)
  assert.equal(window.localStorage.getItem('studdy:simulator:user-2'), null)
  assert.equal(window.sessionStorage.getItem('studdy:simulator:tmp'), null)
  assert.equal(window.localStorage.getItem('outra-chave'), 'fica')
  assert.equal(getSessionGeneration(), before + 1)
})

test('remoção da sessão em outra aba dispara o logout local', () => {
  saveSession({ accessToken: 'a', refreshToken: 'b' }, { rememberMe: true })
  let calls = 0
  const stop = onSessionRemovedElsewhere(() => { calls += 1 })
  const { handler } = listeners.find(item => item.type === 'storage')

  handler({ key: 'outra-chave', newValue: null })
  assert.equal(calls, 0)

  window.localStorage.removeItem(SESSION_STORAGE_KEY)
  handler({ key: SESSION_STORAGE_KEY, newValue: null })
  assert.equal(calls, 1)

  stop()
  assert.equal(listeners.some(item => item.type === 'storage'), false)
})
