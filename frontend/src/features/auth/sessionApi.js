export class SessionExpiredError extends Error {}

export async function sessionRequest(action) {
  const response = await fetch(`/api/v1/auth/${action}`, {
    method: 'POST',
    credentials: 'same-origin',
    headers: { 'Content-Type': 'application/json', 'X-Session-Request': '1' },
    body: '{}',
    signal: AbortSignal.timeout(15000),
  })
  if (response.status === 401) throw new SessionExpiredError('Sua sessão expirou. Entre novamente.')
  if (!response.ok) throw new Error('Não foi possível atualizar sua sessão. Verifique a conexão.')
  return response.status === 204 ? null : response.json()
}
