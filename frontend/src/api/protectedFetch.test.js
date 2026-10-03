import { afterEach, describe, expect, it, vi } from 'vitest'
import { ACCESS_DENIED_EVENT, protectedFetch } from './protectedFetch'

afterEach(() => { vi.unstubAllGlobals() })
describe('Respostas de rotas protegidas', () => {
  it.each([401, 403])('notifica o bloqueio com status %s', async status => {
    const response = { status }
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(response))
    const listener = vi.fn()
    window.addEventListener(ACCESS_DENIED_EVENT, listener, { once: true })
    expect(await protectedFetch('/api/v1/dashboards')).toBe(response)
    expect(listener.mock.calls[0][0].detail.status).toBe(status)
  })

  it.each([200, 400, 404, 500])('não transforma status %s em erro de autorização', async status => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ status }))
    const listener = vi.fn()
    window.addEventListener(ACCESS_DENIED_EVENT, listener)
    try {
      await protectedFetch('/api/v1/dashboards')
      expect(listener).not.toHaveBeenCalled()
    } finally { window.removeEventListener(ACCESS_DENIED_EVENT, listener) }
  })
})
