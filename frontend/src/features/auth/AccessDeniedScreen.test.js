import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import AccessDeniedScreen from './AccessDeniedScreen.vue'

let wrapper, play, pause
beforeEach(() => {
  vi.useFakeTimers()
  play = vi.spyOn(HTMLMediaElement.prototype, 'play').mockResolvedValue()
  pause = vi.spyOn(HTMLMediaElement.prototype, 'pause').mockImplementation(() => {})
})
afterEach(() => {
  wrapper?.unmount()
  wrapper = null
  vi.restoreAllMocks()
  vi.useRealTimers()
})
const phase = () => wrapper.find('.buddy-stage').attributes('data-phase')
async function mountScreen() {
  wrapper = mount(AccessDeniedScreen)
  await flushPromises()
}
async function reachTalking() {
  await mountScreen()
  await vi.advanceTimersByTimeAsync(5530)
}

describe('Sequência do Buddy', () => {
  it('espera o áudio da fala terminar e o backflip e o rojão antes da despedida', async () => {
    await mountScreen()
    expect(phase()).toBe('arrive')
    await vi.advanceTimersByTimeAsync(1820)
    expect(phase()).toBe('glasses')
    await vi.advanceTimersByTimeAsync(3710)
    expect(phase()).toBe('talking')
    expect(wrapper.text()).toContain('olha só pra vc hacker , criatura patética eu tenho nojo de você')
    expect(play).toHaveBeenCalledTimes(1)
    await vi.advanceTimersByTimeAsync(60000)
    expect(phase()).toBe('talking')
    await wrapper.findAll('audio')[0].trigger('ended')
    expect(phase()).toBe('backflip')
    expect(play).toHaveBeenCalledTimes(2)
    await vi.advanceTimersByTimeAsync(980)
    expect(phase()).toBe('backflip')
    await wrapper.findAll('audio')[1].trigger('ended')
    expect(phase()).toBe('goodbye')
    await vi.advanceTimersByTimeAsync(1750)
    expect(phase()).toBe('done')
    expect(wrapper.find('img').exists()).toBe(false)
  })

  it('não corta o backflip quando o rojão acaba primeiro', async () => {
    await reachTalking()
    await wrapper.findAll('audio')[0].trigger('ended')
    await wrapper.findAll('audio')[1].trigger('ended')
    expect(phase()).toBe('backflip')
    await vi.advanceTimersByTimeAsync(980)
    expect(phase()).toBe('goodbye')
  })

  it('oferece um clique para liberar autoplay bloqueado sem pular a fala', async () => {
    play.mockRejectedValueOnce(new DOMException('Bloqueado', 'NotAllowedError'))
    await reachTalking()
    expect(phase()).toBe('talking')
    const button = wrapper.findAll('button').find(button => button.text() === 'Continuar com áudio')
    expect(button).toBeDefined()
    await button.trigger('click')
    await flushPromises()
    expect(play).toHaveBeenCalledTimes(2)
    expect(wrapper.text()).not.toContain('Continuar com áudio')
    expect(phase()).toBe('talking')
  })

  it('continua a animação se um arquivo de áudio falhar', async () => {
    await reachTalking()
    await wrapper.findAll('audio')[0].trigger('error')
    await vi.advanceTimersByTimeAsync(5000)
    expect(phase()).toBe('backflip')
    await wrapper.findAll('audio')[1].trigger('error')
    await vi.advanceTimersByTimeAsync(980)
    expect(phase()).toBe('goodbye')
    expect(wrapper.text()).toContain('Não foi possível reproduzir')
  })

  it('silencia ambos os áudios e interrompe a sequência ao sair', async () => {
    await reachTalking()
    await wrapper.findAll('button').find(button => button.text() === 'Silenciar').trigger('click')
    expect(wrapper.findAll('audio').every(audio => audio.element.muted)).toBe(true)
    await wrapper.findAll('button').find(button => button.text() === 'Voltar ao login').trigger('click')
    expect(wrapper.emitted('login')).toHaveLength(1)
    expect(pause).toHaveBeenCalledTimes(2)
    expect(vi.getTimerCount()).toBe(0)
  })
})
