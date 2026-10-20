import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import PasswordRecoveryScreen from './PasswordRecoveryScreen.vue'
import AuthScreen from './AuthScreen.vue'

vi.mock('../dashboard/DashboardScreen.vue', () => ({ default: { template: '<div>Dashboard</div>' } }))
let wrapper
let fetchMock

beforeEach(() => {
  localStorage.clear()
  sessionStorage.clear()
  fetchMock = vi.fn()
  vi.stubGlobal('fetch', fetchMock)
})

afterEach(() => {
  wrapper?.unmount()
  wrapper = null
  vi.unstubAllGlobals()
  vi.useRealTimers()
})

function reply(data, status = 200) {
  fetchMock.mockResolvedValueOnce({ ok: status < 400, status, json: async () => data })
}

function start() {
  wrapper = mount(PasswordRecoveryScreen, { props: { initialEmail: 'student@example.com' }, attachTo: document.body })
}

async function requestCode() {
  reply({ message: 'Se este e-mail estiver cadastrado, você receberá um código.' }, 202)
  await wrapper.find('form').trigger('submit')
  await flushPromises()
}

async function verifyCode() {
  reply({ resetToken: 'a'.repeat(43), expiresIn: 300 })
  await wrapper.find('input[autocomplete="one-time-code"]').setValue('001234')
  await wrapper.find('form').trigger('submit')
  await flushPromises()
}

describe('recuperação de senha', () => {
  it('abre o fluxo pelo login e retorna com o e-mail preenchido', async () => {
    wrapper = mount(AuthScreen)
    await wrapper.find('.auth-form-options .auth-text-button').trigger('click')
    expect(wrapper.findComponent(PasswordRecoveryScreen).exists()).toBe(true)
    wrapper.findComponent(PasswordRecoveryScreen).vm.$emit('completed', 'student@example.com')
    await flushPromises()
    expect(wrapper.find('input[type="email"]').element.value).toBe('student@example.com')
    expect(wrapper.text()).toContain('Senha recuperada com sucesso')
  })

  it('conclui as três etapas, preserva zeros no código e não persiste os segredos', async () => {
    start()
    await requestCode()
    expect(wrapper.text()).toContain('Confira seu e-mail')
    expect(document.activeElement.id).toBe('auth-title')
    await verifyCode()
    expect(JSON.parse(fetchMock.mock.calls[1][1].body)).toEqual({ email: 'student@example.com', code: '001234' })
    const fields = wrapper.findAll('input[autocomplete="new-password"]')
    await fields[0].setValue('senha-nova-123')
    await fields[1].setValue('senha-nova-123')
    reply({ message: 'Senha alterada com sucesso.' })
    await wrapper.find('form').trigger('submit')
    await flushPromises()
    expect(JSON.parse(fetchMock.mock.calls[2][1].body)).toEqual({ email: 'student@example.com', resetToken: 'a'.repeat(43), newPassword: 'senha-nova-123' })
    expect(wrapper.text()).toContain('Senha recuperada!')
    expect(localStorage.length).toBe(0)
    expect(sessionStorage.length).toBe(0)
    await wrapper.find('.recovery-success-button').trigger('click')
    expect(wrapper.emitted('completed')).toEqual([['student@example.com']])
  })

  it('impede envio de senhas diferentes e exibe mensagem acessível', async () => {
    start()
    await requestCode()
    await verifyCode()
    const fields = wrapper.findAll('input[autocomplete="new-password"]')
    await fields[0].setValue('senha-nova-123')
    await fields[1].setValue('senha-diferente-123')
    await wrapper.find('form').trigger('submit')
    await flushPromises()
    expect(wrapper.find('[role="alert"]').text()).toContain('não são iguais')
    expect(document.activeElement.getAttribute('role')).toBe('alert')
    expect(fetchMock).toHaveBeenCalledTimes(2)
  })

  it('permite corrigir código inválido sem sair da etapa', async () => {
    start()
    await requestCode()
    reply({ detail: 'Código inválido ou expirado.' }, 400)
    await wrapper.find('input[autocomplete="one-time-code"]').setValue('001234')
    await wrapper.find('form').trigger('submit')
    await flushPromises()
    expect(wrapper.find('[role="alert"]').text()).toContain('Código inválido')
    expect(document.activeElement.getAttribute('role')).toBe('alert')
    expect(wrapper.find('input[autocomplete="one-time-code"]').exists()).toBe(true)
    expect(wrapper.find('button[type="submit"]').attributes('disabled')).toBeUndefined()
  })

  it('controla reenvio por 60 segundos e reinicia fluxo se autorização expirar', async () => {
    vi.useFakeTimers()
    start()
    await requestCode()
    expect(wrapper.find('.auth-text-button').attributes('disabled')).toBeDefined()
    await vi.advanceTimersByTimeAsync(60000)
    expect(wrapper.find('.auth-text-button').attributes('disabled')).toBeUndefined()
    await verifyCode()
    await vi.advanceTimersByTimeAsync(300000)
    expect(wrapper.text()).toContain('O prazo para criar a senha terminou')
    expect(wrapper.find('input[type="email"]').exists()).toBe(true)
  })

  it('apresenta falha de conexão e reabilita formulário', async () => {
    start()
    fetchMock.mockRejectedValueOnce(new TypeError('offline'))
    await wrapper.find('form').trigger('submit')
    await flushPromises()
    expect(wrapper.find('[role="alert"]').text()).toContain('Não foi possível conectar')
    expect(wrapper.find('button[type="submit"]').attributes('disabled')).toBeUndefined()
  })

  it('bloqueia requisições duplicadas enquanto aguarda e aborta ao desmontar', async () => {
    start()
    fetchMock.mockReturnValueOnce(new Promise(() => {}))
    await wrapper.find('form').trigger('submit')
    await wrapper.find('form').trigger('submit')
    expect(fetchMock).toHaveBeenCalledTimes(1)
    expect(wrapper.find('button[type="submit"]').attributes('disabled')).toBeDefined()
    const signal = fetchMock.mock.calls[0][1].signal
    wrapper.unmount()
    wrapper = null
    expect(signal.aborted).toBe(true)
  })
})
