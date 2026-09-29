// Sessão do usuário: tokens de acesso/renovação guardados em localStorage ("Lembrar de mim")
// ou sessionStorage. Nunca lança erro se o armazenamento estiver indisponível.

export const SESSION_STORAGE_KEY = 'studdy.session'
const LEGACY_PERSISTENT_KEY = 'acad-organize.access-token'
const LEGACY_SESSION_KEY = 'acad-organize.session-token'

// Dias de sessão persistente ("Lembrar de mim"). Deve acompanhar a propriedade de validade do
// refresh token no backend (application.properties, app.security.refresh-token.*days*).
export const REMEMBER_ME_DAYS = 30

const SIMULATOR_KEY_PREFIX = 'studdy:simulator:'

const expiredListeners = new Set()

// Incrementa a cada limpeza de sessão: operações assíncronas iniciadas antes de um logout
// comparam a geração para não ressuscitar nem apagar uma sessão que não é mais a delas.
let sessionGeneration = 0
let deliberateLogout = false

function storageFor(name) {
  try {
    return window[name]
  } catch {
    return null
  }
}

function readFrom(storage) {
  try {
    const raw = storage?.getItem(SESSION_STORAGE_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

function removeFrom(storage, key) {
  try {
    storage?.removeItem(key)
  } catch {
    // Armazenamento indisponível: nada a limpar.
  }
}

function writeSession(session) {
  const target = storageFor(session.rememberMe ? 'localStorage' : 'sessionStorage')
  const other = storageFor(session.rememberMe ? 'sessionStorage' : 'localStorage')

  removeFrom(other, SESSION_STORAGE_KEY)

  try {
    target?.setItem(SESSION_STORAGE_KEY, JSON.stringify(session))
  } catch {
    // Sem armazenamento a sessão vale só até recarregar a página.
  }
}

// Lê a chave antiga uma única vez (só havia access token) e a remove.
function migrateLegacy() {
  const local = storageFor('localStorage')
  const session = storageFor('sessionStorage')
  let legacyToken = null
  let rememberMe = false

  try {
    const persistent = local?.getItem(LEGACY_PERSISTENT_KEY)
    const temporary = session?.getItem(LEGACY_SESSION_KEY)
    legacyToken = persistent || temporary || null
    rememberMe = Boolean(persistent)
  } catch {
    return null
  }

  removeFrom(local, LEGACY_PERSISTENT_KEY)
  removeFrom(session, LEGACY_SESSION_KEY)

  if (!legacyToken) return null

  const migrated = {
    accessToken: legacyToken,
    refreshToken: '',
    expiresAt: null,
    refreshTokenExpiresAt: null,
    rememberMe,
  }
  writeSession(migrated)
  return migrated
}

export function getSession() {
  return readFrom(storageFor('localStorage'))
    || readFrom(storageFor('sessionStorage'))
    || migrateLegacy()
}

export function saveSession(authResponse, { rememberMe } = {}) {
  const previous = getSession()
  deliberateLogout = false
  const expiresIn = Number(authResponse.expiresIn)

  writeSession({
    accessToken: authResponse.accessToken,
    refreshToken: authResponse.refreshToken || '',
    expiresAt: Number.isFinite(expiresIn) ? Date.now() + expiresIn * 1000 : null,
    refreshTokenExpiresAt: authResponse.refreshTokenExpiresAt || null,
    rememberMe: rememberMe ?? previous?.rememberMe ?? false,
  })
}

export function getAccessToken() {
  return getSession()?.accessToken || ''
}

export function getRefreshToken() {
  return getSession()?.refreshToken || ''
}

export function isRememberedSession() {
  return Boolean(getSession()?.rememberMe)
}

function clearPrefixedKeys(storage, prefix) {
  try {
    if (!storage) return
    for (let index = storage.length - 1; index >= 0; index -= 1) {
      const key = storage.key(index)
      if (key?.startsWith(prefix)) storage.removeItem(key)
    }
  } catch {
    // Armazenamento indisponível: nada a limpar.
  }
}

export function getSessionGeneration() {
  return sessionGeneration
}

// Marca que o encerramento é intencional (botão "Sair"): não deve exibir "sessão expirou".
export function beginDeliberateLogout() {
  deliberateLogout = true
}

export function clearSession() {
  sessionGeneration += 1
  // Cenários do simulador são dados do usuário: não podem sobrar para a próxima pessoa do aparelho.
  clearPrefixedKeys(storageFor('localStorage'), SIMULATOR_KEY_PREFIX)
  clearPrefixedKeys(storageFor('sessionStorage'), SIMULATOR_KEY_PREFIX)
  removeFrom(storageFor('localStorage'), SESSION_STORAGE_KEY)
  removeFrom(storageFor('sessionStorage'), SESSION_STORAGE_KEY)
  removeFrom(storageFor('localStorage'), LEGACY_PERSISTENT_KEY)
  removeFrom(storageFor('sessionStorage'), LEGACY_SESSION_KEY)
}

export function onSessionExpired(callback) {
  expiredListeners.add(callback)
  return () => expiredListeners.delete(callback)
}

// Chamado quando outra aba encerra a sessão persistente (chave removida do localStorage).
export function onSessionRemovedElsewhere(callback) {
  const target = typeof window !== 'undefined' ? window : null

  function handleStorage(event) {
    if (event.key !== null && event.key !== SESSION_STORAGE_KEY) return
    if (event.newValue !== null && event.key !== null) return
    // Esta aba ainda pode ter uma sessão própria (sessionStorage): então não há o que encerrar.
    if (getSession()) return
    callback()
  }

  target?.addEventListener?.('storage', handleStorage)
  return () => {
    target?.removeEventListener?.('storage', handleStorage)
  }
}

export function emitSessionExpired() {
  if (deliberateLogout) return
  expiredListeners.forEach((callback) => {
    try {
      callback()
    } catch {
      // Um ouvinte com erro não pode impedir os demais.
    }
  })
}
