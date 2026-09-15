// Cliente HTTP compartilhado pelas telas novas: envia o token, trata sessão expirada (401),
// respostas sem conteúdo (204), servidor indisponível e junta as mensagens de validação.

export class SessionExpiredError extends Error {}

function errorMessage(response, data) {
  if ([502, 503, 504].includes(response.status)) {
    return 'O servidor está indisponível. Verifique se a API está iniciada e tente novamente.'
  }

  const fieldErrors = data.errors && typeof data.errors === 'object'
    ? Object.values(data.errors).filter(Boolean).join(' ')
    : ''

  return fieldErrors || data.detail || data.message
    || `Não foi possível concluir a solicitação (erro ${response.status}).`
}

// O token é lido a cada chamada porque muda depois de uma troca de senha.
export function createApiClient(getAccessToken) {
  // `formData` envia arquivos (o navegador define o Content-Type com o boundary sozinho) e
  // `as: 'blob'` devolve o corpo binário, usado para a foto de perfil.
  async function request(url, { method = 'GET', body, formData, as = 'json' } = {}) {
    let response

    try {
      response = await fetch(url, {
        method,
        headers: {
          Authorization: `Bearer ${getAccessToken()}`,
          ...(body ? { 'Content-Type': 'application/json' } : {}),
        },
        body: formData ?? (body ? JSON.stringify(body) : undefined),
      })
    } catch {
      throw new Error('Não foi possível conectar ao servidor. Confirme que a API está iniciada.')
    }

    if (response.status === 401) {
      throw new SessionExpiredError('Sua sessão expirou. Entre novamente para continuar.')
    }

    if (response.status === 204) {
      return null
    }

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}))
      throw new Error(errorMessage(response, errorData))
    }

    return as === 'blob' ? response.blob() : response.json().catch(() => ({}))
  }

  return Object.freeze({ request })
}
