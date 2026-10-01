// Cliente HTTP único: token Bearer, JSON, erros padronizados e renovação da sessão pelo cookie
// HttpOnly da API (um único pedido de renovação por aba, mesmo com vários 401 simultâneos).
import { notifyAccessDenied } from '../../api/protectedFetch.js'
import { sessionRequest, SessionExpiredError } from '../../features/auth/sessionApi.js'
import {
  clearSession,
  getAccessToken,
  getSessionGeneration,
  isDeliberateLogout,
  saveSession,
} from '../auth/session.js'

export { SessionExpiredError }

const DEFAULT_ERROR = 'Não foi possível concluir a solicitação.'
const GATEWAY_STATUSES = [502, 503, 504]

let renewal = null

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

/**
 * Pede um access token novo usando o cookie da sessão e o guarda na memória. Chamadas simultâneas
 * compartilham o mesmo pedido. Lança SessionExpiredError se o servidor recusar a sessão.
 */
export function renewSession() {
  if (!renewal) {
    const generation = getSessionGeneration()

    renewal = sessionRequest('refresh')
      .then(auth => {
        // Logout (ou novo login) durante a renovação: nunca regravar o token de uma sessão encerrada.
        if (generation !== getSessionGeneration() || isDeliberateLogout()) throw new SessionExpiredError()
        saveSession(auth)
        return auth
      })
      .finally(() => {
        renewal = null
      })
  }

  return renewal
}

// Só encerra a sessão se ela ainda for a mesma que iniciou a requisição; uma resposta tardia
// de uma sessão anterior não pode derrubar um login novo.
function expireSession(generation, status = 401) {
  if (generation === getSessionGeneration() && !isDeliberateLogout()) {
    clearSession()
    notifyAccessDenied(status)
  }
  return new SessionExpiredError(undefined, status)
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

// skipAuth: rotas públicas (login, cadastro, versões legais). Os erros delas são da própria tela.
export async function apiRequest(path, options = {}) {
  const fallback = options.fallbackMessage || DEFAULT_ERROR
  const isProtected = !options.skipAuth
  const generation = getSessionGeneration()
  let response = await fetch(path, buildInit(options, isProtected ? getAccessToken() : ''))

  if (isProtected && response.status === 401) {
    // A sessão que iniciou esta requisição já foi encerrada: não repete com o token de outra.
    if (generation !== getSessionGeneration() || isDeliberateLogout()) throw new SessionExpiredError()

    let token
    try {
      token = (await renewSession()).accessToken
    } catch (error) {
      throw error instanceof SessionExpiredError ? expireSession(generation, error.status) : error
    }

    response = await fetch(path, buildInit(options, token))
  }

  if (isProtected && (response.status === 401 || response.status === 403)) {
    throw expireSession(generation, response.status)
  }

  if (!response.ok) throw await parseError(response, fallback)
  if (response.status === 204) return null
  if (options.as === 'blob') return response.blob()

  return response.json().catch(() => ({}))
}
