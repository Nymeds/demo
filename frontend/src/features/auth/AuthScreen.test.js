import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import AuthScreen from './AuthScreen.vue'
import AccessDeniedScreen from './AccessDeniedScreen.vue'
import { ACCESS_DENIED_EVENT } from '../../api/protectedFetch'
// O token de acesso fica na memória do módulo de sessão, não em props do dashboard.
import { getAccessToken } from '../../shared/auth/session.js'

vi.mock('../dashboard/DashboardScreen.vue', () => ({ default: {
  props: ['accessToken', 'user'], emits: ['logout', 'token-refreshed'],
  template: '<div data-testid="dashboard"><span>{{ user.name }}</span><button @click="$emit(\'logout\')">Sair</button></div>',
} }))
let wrapper, fetchMock
const user = { id: 'student', name: 'Estudante', email: 'student@example.com' }
const auth = { accessToken: 'short-access-token', expiresIn: 900, rememberMe: true }

function reply(data, status = 200) {
  fetchMock.mockResolvedValueOnce({ ok: status < 400, status, json: async () => data })
}
async function startLogin() {
  reply({}, 401)
  wrapper = mount(AuthScreen)
  await flushPromises()
  await wrapper.find('input[type="email"]').setValue(user.email)
  await wrapper.find('input[autocomplete="current-password"]').setValue('senha-atual-123')
}
async function login(rememberMe) {
  await startLogin()
  await wrapper.find('input[type="checkbox"]').setValue(rememberMe)
  reply(auth)
  reply(user)
  await wrapper.find('form').trigger('submit')
  await flushPromises()
}

beforeEach(() => {
  vi.useFakeTimers()
  localStorage.clear()
  sessionStorage.clear()
  window.history.replaceState(null, '', '/')
  fetchMock = vi.fn()
  vi.stubGlobal('fetch', fetchMock)
})
afterEach(() => {
  wrapper?.unmount()
  wrapper = null
  vi.unstubAllGlobals()
  vi.useRealTimers()
})

describe('Lembrar de mim', () => {
  it.each([true, false])('envia a escolha %s ao backend e mantém o token somente na memória', async rememberMe => {
    await login(rememberMe)
    const [, options] = fetchMock.mock.calls.find(([url]) => url === '/api/v1/auth/login')
    expect(JSON.parse(options.body).rememberMe).toBe(rememberMe)
    expect(options.credentials).toBe('same-origin')
    expect(wrapper.find('[data-testid="dashboard"]').text()).toContain('Estudante')
    expect(localStorage.length).toBe(0)
    expect(sessionStorage.length).toBe(0)
  })

  it('restaura a sessão por cookie sem depender de tokens no Web Storage', async () => {
    localStorage.setItem('acad-organize.access-token', 'legacy-token')
    sessionStorage.setItem('acad-organize.session-token', 'legacy-token')
    reply(auth)
    reply(user)
    wrapper = mount(AuthScreen)
    await flushPromises()
    expect(fetchMock.mock.calls[0][0]).toBe('/api/v1/auth/refresh')
    expect(fetchMock.mock.calls[0][1].headers['X-Session-Request']).toBe('1')
    expect(wrapper.find('[data-testid="dashboard"]').exists()).toBe(true)
    expect(localStorage.length).toBe(0)
    expect(sessionStorage.length).toBe(0)
    wrapper.unmount()
    wrapper = null
    reply({ ...auth, accessToken: 'renewed-after-reopen' })
    reply(user)
    wrapper = mount(AuthScreen)
    await flushPromises()
    expect(getAccessToken()).toBe('renewed-after-reopen')
  })

  it('renova o token antes de 15 minutos e mantém o dashboard aberto', async () => {
    await login(true)
    reply({ ...auth, accessToken: 'renewed-token' })
    await vi.advanceTimersByTimeAsync(840000)
    expect(wrapper.find('[data-testid="dashboard"]').exists()).toBe(true)
    expect(getAccessToken()).toBe('renewed-token')
    expect(fetchMock.mock.calls.filter(([url]) => url === '/api/v1/auth/refresh')).toHaveLength(2)
  })

  it('revoga a sessão salva ao sair e permite login sem lembrar da próxima vez', async () => {
    await login(true)
    reply(null, 204)
    await wrapper.find('[data-testid="dashboard"] button').trigger('click')
    await flushPromises()
    expect(fetchMock.mock.calls.at(-1)[0]).toBe('/api/v1/auth/logout')
    expect(wrapper.find('input[type="checkbox"]').element.checked).toBe(false)
    expect(localStorage.length).toBe(0)
    expect(sessionStorage.length).toBe(0)
  })

  it('não restaura uma sessão quando a saída anterior falhou por falta de conexão', async () => {
    await login(true)
    fetchMock.mockRejectedValueOnce(new TypeError('offline'))
    await wrapper.find('[data-testid="dashboard"] button').trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('Você saiu neste navegador')
    expect(localStorage.getItem('acad-organize.logout-pending')).toBe('1')
    wrapper.unmount()
    wrapper = null
    reply(null, 204)
    wrapper = mount(AuthScreen)
    await flushPromises()
    expect(fetchMock.mock.calls.at(-1)[0]).toBe('/api/v1/auth/logout')
    expect(wrapper.find('input[type="email"]').exists()).toBe(true)
    expect(localStorage.length).toBe(0)
  })

  it('não apaga a sessão em falha temporária e encerra quando a renovação é recusada', async () => {
    await login(true)
    fetchMock.mockRejectedValueOnce(new TypeError('offline'))
    await vi.advanceTimersByTimeAsync(840000)
    expect(wrapper.find('[data-testid="dashboard"]').exists()).toBe(true)
    reply({}, 401)
    await vi.advanceTimersByTimeAsync(10000)
    expect(wrapper.findComponent(AccessDeniedScreen).exists()).toBe(true)
    expect(wrapper.find('[data-testid="dashboard"]').exists()).toBe(false)
  })

  it('desabilita o login enquanto restaura e limpa temporizadores ao desmontar', async () => {
    let resolveRefresh
    fetchMock.mockImplementationOnce(() => new Promise(resolve => { resolveRefresh = resolve }))
    wrapper = mount(AuthScreen)
    expect(wrapper.find('button[type="submit"]').attributes('disabled')).toBeDefined()
    await wrapper.find('form').trigger('submit')
    expect(fetchMock).toHaveBeenCalledTimes(1)
    wrapper.unmount()
    wrapper = null
    reply(user)
    resolveRefresh({ ok: true, status: 200, json: async () => auth })
    await flushPromises()
    expect(vi.getTimerCount()).toBe(0)
  })
})

describe('Acesso a rotas protegidas', () => {
  it.each(['/disciplinas', '/dashboard', '/qualquer-rota', '/#/notas'])('bloqueia %s após recusa da sessão', async path => {
    window.history.replaceState(null, '', path)
    reply({}, 401)
    wrapper = mount(AuthScreen)
    expect(wrapper.findComponent(AccessDeniedScreen).exists()).toBe(false)
    await flushPromises()
    expect(wrapper.findComponent(AccessDeniedScreen).props('status')).toBe(401)
    expect(wrapper.find('[data-testid="dashboard"]').exists()).toBe(false)
  })

  it('libera a rota apenas depois de validar sessão e perfil', async () => {
    window.history.replaceState(null, '', '/disciplinas')
    reply(auth)
    reply(user)
    wrapper = mount(AuthScreen)
    expect(wrapper.find('[data-testid="dashboard"]').exists()).toBe(false)
    await flushPromises()
    expect(wrapper.find('[data-testid="dashboard"]').exists()).toBe(true)
    expect(wrapper.findComponent(AccessDeniedScreen).exists()).toBe(false)
  })

  it.each([401, 403])('recusa uma sessão cujo perfil retorna %s', async status => {
    window.history.replaceState(null, '', '/dashboard')
    reply(auth)
    reply({}, status)
    wrapper = mount(AuthScreen)
    await flushPromises()
    expect(wrapper.findComponent(AccessDeniedScreen).exists()).toBe(true)
    expect(wrapper.find('[data-testid="dashboard"]').exists()).toBe(false)
  })

  it('mostra 403 quando a restauração da sessão é proibida', async () => {
    window.history.replaceState(null, '', '/dashboard')
    reply({}, 403)
    wrapper = mount(AuthScreen)
    await flushPromises()
    expect(wrapper.findComponent(AccessDeniedScreen).props('status')).toBe(403)
  })

  it('bloqueia uma rota alterada pelo histórico sem autenticação', async () => {
    reply({}, 401)
    wrapper = mount(AuthScreen)
    await flushPromises()
    window.history.pushState(null, '', '/perfil')
    window.dispatchEvent(new PopStateEvent('popstate'))
    await flushPromises()
    expect(wrapper.findComponent(AccessDeniedScreen).exists()).toBe(true)
  })

  it.each([401, 403])('remove o dashboard quando uma chamada protegida recebe %s', async status => {
    await login(true)
    window.dispatchEvent(new CustomEvent(ACCESS_DENIED_EVENT, { detail: { status } }))
    await flushPromises()
    expect(wrapper.findComponent(AccessDeniedScreen).props('status')).toBe(status)
    expect(wrapper.find('[data-testid="dashboard"]').exists()).toBe(false)
    expect(vi.getTimerCount()).toBe(1) // Apenas a animação do Buddy, sem renovação de token.
  })

  it('não trata credenciais incorretas no login como tentativa de acesso a rota protegida', async () => {
    await startLogin()
    reply({ message: 'E-mail ou senha inválidos.' }, 401)
    await wrapper.find('form').trigger('submit')
    await flushPromises()
    expect(wrapper.findComponent(AccessDeniedScreen).exists()).toBe(false)
    expect(wrapper.text()).toContain('E-mail ou senha inválidos.')
  })

  it('volta ao login e revoga o cookie sem restaurar o acesso automaticamente', async () => {
    window.history.replaceState(null, '', '/dashboard')
    reply({}, 401)
    wrapper = mount(AuthScreen)
    await flushPromises()
    reply(null, 204)
    await wrapper.findComponent(AccessDeniedScreen).findAll('button').find(button => button.text() === 'Voltar ao login').trigger('click')
    await flushPromises()
    expect(window.location.pathname).toBe('/login')
    expect(wrapper.find('input[type="email"]').exists()).toBe(true)
    expect(wrapper.findComponent(AccessDeniedScreen).exists()).toBe(false)
  })
})
