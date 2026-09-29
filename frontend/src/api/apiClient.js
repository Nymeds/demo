// Adaptador de compatibilidade: telas novas usam { request(url, { method, body, formData, as }) }
// sobre o cliente HTTP compartilhado.
import { apiRequest, SessionExpiredError } from '../shared/http/apiRequest.js'

export { SessionExpiredError }

// O token vem do módulo de sessão (shared/auth/session.js).
export function createApiClient() {
  async function request(url, { method = 'GET', body, formData, as = 'json' } = {}) {
    try {
      return await apiRequest(url, { method, body: formData ?? body, as })
    } catch (error) {
      if (error instanceof TypeError) {
        throw new Error('Não foi possível conectar ao servidor. Confirme que a API está iniciada.')
      }
      throw error
    }
  }

  return Object.freeze({ request })
}
