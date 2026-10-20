// Sessão do usuário nesta aba. O access token (JWT curto) fica só na memória; a sessão de longa
// duração é o cookie HttpOnly da API ("Lembrar de mim"), que o JavaScript não lê nem grava.
// Ao recarregar a página, o AuthScreen pede um token novo pelo cookie (features/auth/sessionApi.js).
// Nenhum token é guardado em localStorage ou sessionStorage.

// Deve acompanhar a validade da sessão lembrada no backend (BrowserSessionService.REMEMBERED_LIFETIME).
export const REMEMBER_ME_DAYS = 30

// Marca uma saída ainda não confirmada pelo servidor. Também avisa as outras abas pelo evento
// 'storage' (o evento só chega às demais abas, nunca à que gravou).
export const LOGOUT_PENDING_KEY = 'acad-organize.logout-pending'

// Chaves de versões anteriores, que guardavam tokens no navegador. São apagadas ao abrir o app.
const LEGACY_TOKEN_KEYS = ['studdy.session', 'acad-organize.access-token', 'acad-organize.session-token']
const SIMULATOR_KEY_PREFIX = 'studdy:simulator:'

let current = null
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

function removeFrom(storage, key) {
  try {
    storage?.removeItem(key)
  } catch {
    // Armazenamento indisponível: nada a limpar.
  }
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

export function forgetLegacyTokens() {
  for (const key of LEGACY_TOKEN_KEYS) {
    removeFrom(storageFor('localStorage'), key)
    removeFrom(storageFor('sessionStorage'), key)
  }
}

export function getSession() {
  return current
}

// authResponse: { accessToken, expiresIn, rememberMe? } devolvido por login, refresh ou troca de credenciais.
export function saveSession(authResponse, { rememberMe } = {}) {
  deliberateLogout = false
  const expiresIn = Number(authResponse.expiresIn)

  current = {
    accessToken: authResponse.accessToken,
    expiresAt: Number.isFinite(expiresIn) ? Date.now() + expiresIn * 1000 : null,
    rememberMe: rememberMe ?? authResponse.rememberMe ?? current?.rememberMe ?? false,
  }
}

export function getAccessToken() {
  return current?.accessToken || ''
}

export function isRememberedSession() {
  return Boolean(current?.rememberMe)
}

export function getSessionGeneration() {
  return sessionGeneration
}

// Marca que o encerramento é intencional (botão "Sair"): respostas 401 em voo não viram "acesso negado".
export function beginDeliberateLogout() {
  deliberateLogout = true
}

export function isDeliberateLogout() {
  return deliberateLogout
}

export function clearSession() {
  sessionGeneration += 1
  current = null
  // Cenários do simulador são dados do usuário: não podem sobrar para a próxima pessoa do aparelho.
  clearPrefixedKeys(storageFor('localStorage'), SIMULATOR_KEY_PREFIX)
  clearPrefixedKeys(storageFor('sessionStorage'), SIMULATOR_KEY_PREFIX)
  forgetLegacyTokens()
}

// Chamado quando outra aba deste navegador começa a sair da conta (o cookie é o mesmo para todas).
export function onSessionRemovedElsewhere(callback) {
  const target = typeof window !== 'undefined' ? window : null

  function handleStorage(event) {
    if (event.key === LOGOUT_PENDING_KEY && event.newValue) callback()
  }

  target?.addEventListener?.('storage', handleStorage)
  return () => {
    target?.removeEventListener?.('storage', handleStorage)
  }
}
