export async function recoveryRequest(action, payload, signal) {
  let response
  try {
    response = await fetch(`/api/v1/auth/password-recovery/${action}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
      signal: AbortSignal.any([signal, AbortSignal.timeout(25000)]),
      cache: 'no-store',
    })
  } catch (error) {
    if (signal.aborted) throw error
    throw new Error('Não foi possível conectar ao servidor. Verifique sua conexão e tente novamente.')
  }
  const data = await response.json().catch(() => ({}))
  if (!response.ok) {
    const errors = data.errors && typeof data.errors === 'object'
      ? Object.values(data.errors).filter(Boolean).join(' ') : ''
    throw new Error(errors || data.detail || data.message || 'Não foi possível concluir a recuperação. Tente novamente.')
  }
  return data
}
