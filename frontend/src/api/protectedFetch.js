export const ACCESS_DENIED_EVENT = 'acad-organize:access-denied'

// Avisa o AuthScreen que a sessão foi recusada: ele troca o dashboard pela tela de acesso negado.
export function notifyAccessDenied(status) {
  window.dispatchEvent(new CustomEvent(ACCESS_DENIED_EVENT, {
    detail: { status },
  }))
}

// Somente chamadas protegidas passam aqui. Login e recuperação têm erros próprios.
export async function protectedFetch(url, options) {
  const response = await fetch(url, options)
  if (response.status === 401 || response.status === 403) notifyAccessDenied(response.status)
  return response
}
