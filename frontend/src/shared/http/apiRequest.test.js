import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { apiRequest } from './apiRequest.js'
import { ACCESS_DENIED_EVENT } from '../../api/protectedFetch.js'

let fetchMock
const json = (data, status = 200) => ({ ok: status < 400, status, json: async () => data })

beforeEach(() => {
  fetchMock = vi.fn()
  vi.stubGlobal('fetch', fetchMock)
})
afterEach(() => vi.unstubAllGlobals())

describe('apiRequest', () => {
  it('trata 403 como erro comum, sem encerrar a sessão', async () => {
    const listener = vi.fn()
    window.addEventListener(ACCESS_DENIED_EVENT, listener)
    fetchMock.mockResolvedValueOnce(json({ message: 'Sem permissão.' }, 403))
    await expect(apiRequest('/api/x')).rejects.toMatchObject({ status: 403, message: 'Sem permissão.' })
    expect(fetchMock).toHaveBeenCalledTimes(1)
    expect(listener).not.toHaveBeenCalled()
    window.removeEventListener(ACCESS_DENIED_EVENT, listener)
  })

  it('envia um sinal de timeout e converte o estouro em mensagem amigável', async () => {
    fetchMock.mockRejectedValueOnce(new DOMException('timeout', 'TimeoutError'))
    await expect(apiRequest('/api/x')).rejects.toThrow('O servidor demorou para responder. Tente novamente.')
    expect(fetchMock.mock.calls[0][1].signal).toBeInstanceOf(AbortSignal)
  })

  it('mantém o cancelamento do chamador', async () => {
    const controller = new AbortController()
    fetchMock.mockImplementationOnce((_, { signal }) => new Promise((_, reject) => {
      signal.addEventListener('abort', () => reject(new DOMException('abort', 'AbortError')))
    }))
    const pending = apiRequest('/api/x', { signal: controller.signal })
    controller.abort()
    await expect(pending).rejects.toMatchObject({ name: 'AbortError' })
  })
})
