// Única classe de "sessão encerrada" do frontend: também é a lançada por shared/http/apiRequest.js.
export class SessionExpiredError extends Error {
  constructor(message = 'Sua sessão expirou. Entre novamente.', status = 401) {
    super(message)
    this.name = 'SessionExpiredError'
    this.status = status
  }
}

export async function sessionRequest(action) {
  const response = await fetch(`/api/v1/auth/${action}`, {
    method: 'POST',
    credentials: 'same-origin',
    headers: { 'Content-Type': 'application/json', 'X-Session-Request': '1' },
    body: '{}',
    signal: AbortSignal.timeout(15000),
  })
  if ([401, 403].includes(response.status)) {
    throw new SessionExpiredError('Sua sessão expirou ou não tem permissão. Entre novamente.', response.status)
  }
  if (!response.ok) throw new Error('Não foi possível atualizar sua sessão. Verifique a conexão.')
  return response.status === 204 ? null : response.json()
}
