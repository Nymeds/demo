import assert from 'node:assert/strict'
import test from 'node:test'
import { effectScope, nextTick, ref } from 'vue'
import { useFocusTrap } from '../src/shared/a11y/useFocusTrap.js'

function setup() {
  const listeners = new Set()
  const fakeDocument = {
    activeElement: null,
    addEventListener: (type, fn) => { if (type === 'keydown') listeners.add(fn) },
    removeEventListener: (type, fn) => { if (type === 'keydown') listeners.delete(fn) },
  }
  const makeEl = name => ({ name, getClientRects: () => [1], focus() { fakeDocument.activeElement = this } })
  const opener = { name: 'opener', isConnected: true, focus() { fakeDocument.activeElement = this } }
  const first = makeEl('first')
  const last = makeEl('last')
  const container = {
    querySelectorAll: () => [first, last],
    contains: element => element === first || element === last,
    focus() { fakeDocument.activeElement = this },
  }
  fakeDocument.activeElement = opener
  globalThis.document = fakeDocument
  const press = (key, extra = {}) => {
    const event = {
      key,
      shiftKey: false,
      prevented: false,
      preventDefault() { this.prevented = true },
      stopPropagation() {},
      ...extra,
    }
    for (const fn of [...listeners]) fn(event)
    return event
  }
  return { listeners, first, last, opener, container, fakeDocument, press }
}

test.afterEach(() => { delete globalThis.document })

test('registra o listener ao abrir, foca o primeiro item e remove ao fechar devolvendo o foco', async () => {
  const env = setup()
  const isOpen = ref(false)
  const scope = effectScope()
  scope.run(() => useFocusTrap(isOpen, ref(env.container), { onClose() {} }))
  assert.equal(env.listeners.size, 0)

  isOpen.value = true
  await nextTick()
  assert.equal(env.listeners.size, 1)
  await nextTick()
  assert.equal(env.fakeDocument.activeElement, env.first)

  isOpen.value = false
  await nextTick()
  assert.equal(env.listeners.size, 0)
  assert.equal(env.fakeDocument.activeElement, env.opener)
  scope.stop()
})

test('remove o listener quando o escopo é destruído com o modal aberto', () => {
  const env = setup()
  const scope = effectScope()
  scope.run(() => useFocusTrap(() => true, ref(env.container), { onClose() {} }))
  assert.equal(env.listeners.size, 1)
  scope.stop()
  assert.equal(env.listeners.size, 0)
})

test('Escape fecha, exceto quando closeOnEscape está desativado', () => {
  const env = setup()
  const busy = ref(false)
  let closed = 0
  const scope = effectScope()
  scope.run(() => useFocusTrap(() => true, ref(env.container), {
    onClose: () => { closed += 1 },
    closeOnEscape: () => !busy.value,
  }))
  env.press('Escape')
  assert.equal(closed, 1)
  busy.value = true
  env.press('Escape')
  assert.equal(closed, 1)
  scope.stop()
})

test('Tab e Shift+Tab dão a volta dentro do modal', () => {
  const env = setup()
  const scope = effectScope()
  scope.run(() => useFocusTrap(() => true, ref(env.container), { onClose() {} }))
  env.fakeDocument.activeElement = env.last
  const forward = env.press('Tab')
  assert.equal(forward.prevented, true)
  assert.equal(env.fakeDocument.activeElement, env.first)

  const backward = env.press('Tab', { shiftKey: true })
  assert.equal(backward.prevented, true)
  assert.equal(env.fakeDocument.activeElement, env.last)

  env.fakeDocument.activeElement = env.first
  assert.equal(env.press('Tab').prevented, false)
  scope.stop()
})
