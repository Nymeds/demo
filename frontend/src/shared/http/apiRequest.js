// Cliente HTTP único: token Bearer, JSON, erros padronizados e renovação de sessão
// (single-flight por aba + trava entre abas quando disponível).
import {
  clearSession,
  emitSessionExpired,
  getAccessToken,
  getRefreshToken,
  getSessionGeneration,
  saveSession,
} from '../auth/session.js'

export class SessionExpiredError extends Error {
  constructor(message = 'Sua sessão expirou. Entre novamente.') {
    super(message)
    this.name = 'SessionExpiredError'
    this.status = 401
  }
}

const DEFAULT_ERROR = 'Não foi possível concluir a solicitação.'
const GATEWAY_STATUSES = [502, 503, 504]
const REFRESH_PATH = '/api/v1/auth/refresh'
const LOCK_NAME = 'studdy-refresh'

let refreshInFlight = null

function isAuthPath(path) {
  return typeof path === 'string' && (path.includes('/auth/login') || path.includes('/auth/refresh') || path.includes('/auth/logout'))
}

export function errorMessage(data, fallback = DEFAULT_ERROR) {
  const body = data && typeof data === 'object' ? data : {}
  const fieldErrors = body.errors && typeof body.errors === 'object'
    ? Object.values(body.errors).filter(Boolean).join(' ')
    : ''

  return fieldErrors || body.detail || body.message || fallback
}

function buildInit(options, token) {
  const { body, headers, as, fallbackMessage, skipAuth, ...init } = options
  const isRaw = typeof body === 'string' || body instanceof FormData || body instanceof Blob
  const payload = body == null ? undefined : (isRaw ? body : JSON.stringify(body))
  const isJson = payload !== undefined && !(body instanceof FormData) && !(body instanceof Blob)

  return {
    ...init,
    body: payload,
    headers: {
      ...(token && !skipAuth ? { Authorization: `Bearer ${token}` } : {}),
      ...(isJson ? { 'Content-Type': 'application/json' } : {}),
      ...headers,
    },
  }
}

async function doRefresh(failedToken) {
  const current = getAccessToken()

  // Outra aba já renovou: basta repetir com o token novo.
  if (current && current !== failedToken) return current

  const generation = getSessionGeneration()
  const refreshToken = getRefreshToken()
  if (!refreshToken) throw new SessionExpiredError()

  const response = await fetch(REFRESH_PATH, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken }),
  })

  // Logout (ou novo login) durante a renovação: nunca regravar tokens de uma sessão encerrada.
  if (generation !== getSessionGeneration()) throw new SessionExpiredError()

  // Outra aba rotacionou o refresh token enquanto esperávamos: usa o resultado dela.
  if (getRefreshToken() !== refreshToken) {
    const rotated = getAccessToken()
    if (rotated) return rotated
    throw new SessionExpiredError()
  }

  if (response.ok) {
    const data = await response.json()
    if (generation !== getSessionGeneration() || getRefreshToken() !== refreshToken) {
      throw new SessionExpiredError()
    }
    saveSession(data)
    return data.accessToken
  }

  if (response.status >= 500) {
    throw new Error('Não foi possível renovar sua sessão agora. Tente novamente.')
  }

  throw new SessionExpiredError()
}

function refreshAccessToken(failedToken) {
  if (!refreshInFlight) {
    const run = () => doRefresh(failedToken)
    const locked = typeof navigator !== 'undefined' && navigator.locks?.request
      ? navigator.locks.request(LOCK_NAME, run)
      : run()

    refreshInFlight = Promise.resolve(locked).finally(() => {
      refreshInFlight = null
    })
  }

  return refreshInFlight
}

// Só encerra a sessão se ela ainda for a mesma que iniciou a requisição; uma resposta tardia
// de uma sessão anterior não pode derrubar um login novo.
function expireSession(generation) {
  if (generation === getSessionGeneration()) {
    clearSession()
    emitSessionExpired()
  }
  return new SessionExpiredError()
}

async function parseError(response, fallback) {
  const data = await response.json().catch(() => ({}))
  const message = GATEWAY_STATUSES.includes(response.status)
    ? 'O servidor está indisponível. Verifique se a API está iniciada e tente novamente.'
    : errorMessage(data, fallback)
  const error = new Error(message)

  error.status = response.status
  error.body = data
  error.fieldErrors = data?.errors && typeof data.errors === 'object' ? data.errors : {}
  return error
}

export async function apiRequest(path, options = {}) {
  const fallback = options.fallbackMessage || DEFAULT_ERROR
  const canRefresh = !options.skipAuth && !isAuthPath(path)
  const generation = getSessionGeneration()
  let token = options.skipAuth ? '' : getAccessToken()
  let response = await fetch(path, buildInit(options, token))

  if (response.status === 401 && canRefresh) {
    // A sessão que iniciou esta requisição já foi encerrada: não repete com o token de outra.
    if (generation !== getSessionGeneration()) throw new SessionExpiredError()

    try {
      token = await refreshAccessToken(token)
    } catch (error) {
      throw error instanceof SessionExpiredError ? expireSession(generation) : error
    }

    response = await fetch(path, buildInit(options, token))
    if (response.status === 401) throw expireSession(generation)
  }

  if (!response.ok) throw await parseError(response, fallback)
  if (response.status === 204) return null
  if (options.as === 'blob') return response.blob()

  return response.json().catch(() => ({}))
}
