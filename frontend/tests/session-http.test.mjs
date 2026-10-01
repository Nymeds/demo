import assert from 'node:assert/strict'
import test, { beforeEach } from 'node:test'
import { ACCESS_DENIED_EVENT } from '../src/api/protectedFetch.js'
import {
  beginDeliberateLogout,
  clearSession,
  forgetLegacyTokens,
  getSession,
  getSessionGeneration,
  LOGOUT_PENDING_KEY,
  onSessionRemovedElsewhere,
  saveSession,
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
let deniedEvents
const realFetch = globalThis.fetch

beforeEach(() => {
  listeners = []
  deniedEvents = []
  globalThis.window = {
    localStorage: new MemoryStorage(),
    sessionStorage: new MemoryStorage(),
    addEventListener: (type, handler) => listeners.push({ type, handler }),
    removeEventListener: (type, handler) => { listeners = listeners.filter(item => item.handler !== handler) },
    dispatchEvent: event => {
      if (event.type === ACCESS_DENIED_EVENT) deniedEvents.push(event.detail.status)
      return true
    },
  }
  // Uma nova sessão zera o estado global de logout deliberado deixado por testes anteriores.
  saveSession({ accessToken: 'reset' })
  clearSession()
})

const json = (status, data = {}) => ({ ok: status >= 200 && status < 300, status, json: async () => data })
const bearer = init => init?.headers?.Authorization
const tick = () => new Promise(resolve => setImmediate(resolve))

function restoreFetch() {
  globalThis.fetch = realFetch
}

test('401s simultâneos disparam uma única renovação pelo cookie e todas as requisições são repetidas', async t => {
  t.after(restoreFetch)
  saveSession({ accessToken: 'old', expiresIn: 60 }, { rememberMe: true })
  const refreshCalls = []

  globalThis.fetch = async (path, init) => {
    if (path === '/api/v1/auth/refresh') {
      refreshCalls.push(init)
      return json(200, { accessToken: 'new', expiresIn: 900, rememberMe: true })
    }
    return bearer(init) === 'Bearer new' ? json(200, { ok: path }) : json(401)
  }

  const results = await Promise.all([
    apiRequest('/api/v1/a'),
    apiRequest('/api/v1/b'),
    apiRequest('/api/v1/c'),
  ])

  assert.equal(refreshCalls.length, 1)
  // A renovação usa só o cookie HttpOnly: nenhum token vai no corpo.
  assert.equal(refreshCalls[0].credentials, 'same-origin')
  assert.equal(refreshCalls[0].headers['X-Session-Request'], '1')
  assert.equal(refreshCalls[0].body, '{}')
  assert.deepEqual(results.map(item => item.ok), ['/api/v1/a', '/api/v1/b', '/api/v1/c'])
  assert.equal(getSession().accessToken, 'new')
  assert.equal(getSession().rememberMe, true)
})

test('nenhum token é gravado no localStorage ou no sessionStorage', () => {
  saveSession({ accessToken: 'a', expiresIn: 900 }, { rememberMe: true })

  assert.equal(window.localStorage.length, 0)
  assert.equal(window.sessionStorage.length, 0)
  assert.equal(getSession().accessToken, 'a')
})

test('renovação recusada limpa a sessão, lança SessionExpiredError e avisa uma vez', async t => {
  t.after(restoreFetch)
  saveSession({ accessToken: 'old' })

  globalThis.fetch = async () => json(401)

  const outcomes = await Promise.allSettled([apiRequest('/api/v1/a'), apiRequest('/api/v1/b')])

  assert.ok(outcomes.every(item => item.status === 'rejected' && item.reason instanceof SessionExpiredError))
  assert.equal(getSession(), null)
  assert.deepEqual(deniedEvents, [401])
})

test('403 em rota protegida é erro comum e mantém a sessão', async t => {
  t.after(restoreFetch)
  saveSession({ accessToken: 'token' })
  const paths = []

  globalThis.fetch = async path => {
    paths.push(path)
    return json(403)
  }

  await assert.rejects(apiRequest('/api/v1/a'), error => error.status === 403 && !(error instanceof SessionExpiredError))
  assert.deepEqual(paths, ['/api/v1/a'])
  assert.deepEqual(deniedEvents, [])
  assert.notEqual(getSession(), null)
})

test('erros de rotas públicas não encerram a sessão', async t => {
  t.after(restoreFetch)
  globalThis.fetch = async () => json(401, { message: 'E-mail ou senha inválidos.' })

  await assert.rejects(
    apiRequest('/api/v1/auth/login', { method: 'POST', body: {}, skipAuth: true }),
    error => error.message === 'E-mail ou senha inválidos.' && !(error instanceof SessionExpiredError),
  )
  assert.deepEqual(deniedEvents, [])
})

test('logout durante a renovação não ressuscita a sessão nem mostra acesso negado', async t => {
  t.after(restoreFetch)
  saveSession({ accessToken: 'old' })
  let releaseRefresh

  globalThis.fetch = async path => {
    if (path === '/api/v1/auth/refresh') {
      await new Promise(resolve => { releaseRefresh = resolve })
      return json(200, { accessToken: 'new' })
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
  assert.deepEqual(deniedEvents, [])
})

test('resposta tardia de uma sessão anterior não derruba um login novo', async t => {
  t.after(restoreFetch)
  saveSession({ accessToken: 'old' })
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
  saveSession({ accessToken: 'fresh' })
  releaseFirst()

  await assert.rejects(pending, SessionExpiredError)
  assert.equal(getSession()?.accessToken, 'fresh')
  assert.deepEqual(deniedEvents, [])
})

test('tokens guardados por versões anteriores são apagados', () => {
  window.localStorage.setItem('studdy.session', '{"refreshToken":"antigo"}')
  window.localStorage.setItem('acad-organize.access-token', 'legacy-token')
  window.sessionStorage.setItem('acad-organize.session-token', 'legacy-token')

  forgetLegacyTokens()

  assert.equal(window.localStorage.length, 0)
  assert.equal(window.sessionStorage.length, 0)
})

test('clearSession remove cenários do simulador por prefixo e avança a geração', () => {
  saveSession({ accessToken: 'a' }, { rememberMe: true })
  window.localStorage.setItem('studdy:simulator:user-1', '[]')
  window.localStorage.setItem('studdy:simulator:user-2', '[]')
  window.sessionStorage.setItem('studdy:simulator:tmp', '[]')
  window.localStorage.setItem('outra-chave', 'fica')
  const before = getSessionGeneration()

  clearSession()

  assert.equal(getSession(), null)
  assert.equal(window.localStorage.getItem('studdy:simulator:user-1'), null)
  assert.equal(window.localStorage.getItem('studdy:simulator:user-2'), null)
  assert.equal(window.sessionStorage.getItem('studdy:simulator:tmp'), null)
  assert.equal(window.localStorage.getItem('outra-chave'), 'fica')
  assert.equal(getSessionGeneration(), before + 1)
})

test('saída iniciada em outra aba dispara o logout local', () => {
  let calls = 0
  const stop = onSessionRemovedElsewhere(() => { calls += 1 })
  const { handler } = listeners.find(item => item.type === 'storage')

  handler({ key: 'outra-chave', newValue: '1' })
  handler({ key: LOGOUT_PENDING_KEY, newValue: null })
  assert.equal(calls, 0)

  handler({ key: LOGOUT_PENDING_KEY, newValue: '1' })
  assert.equal(calls, 1)

  stop()
  assert.equal(listeners.some(item => item.type === 'storage'), false)
})
