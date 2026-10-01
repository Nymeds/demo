export const ACCESS_DENIED_EVENT = 'acad-organize:access-denied'

// Somente chamadas protegidas passam aqui. Login e recuperação têm erros próprios.
export async function protectedFetch(url, options) {
  const response = await fetch(url, options)
  if (response.status === 401 || response.status === 403) {
    window.dispatchEvent(new CustomEvent(ACCESS_DENIED_EVENT, {
      detail: { status: response.status },
    }))
  }
  return response
}
