import assert from 'node:assert/strict'
import test from 'node:test'
import { readFile } from 'node:fs/promises'
import { compileScript, parse } from '@vue/compiler-sfc'
import { createRenderer, nextTick } from 'vue'

async function component(name) {
  const url = new URL(`../src/features/disciplines/${name}.vue`, import.meta.url)
  const { descriptor } = parse(await readFile(url, 'utf8'))
  const compiled = compileScript(descriptor, { id: name }).content
    .replace(/import (\w+) from ['"][^'"]+\.vue['"]/g, 'const $1 = {}')
    .replace(/from ['"]vue['"]/g, `from ${JSON.stringify(import.meta.resolve('vue'))}`)
    .replace(/from ['"](\.[^'"]+)['"]/g, (_, path) => `from ${JSON.stringify(new URL(path.endsWith('.js') ? path : `${path}.js`, url).href)}`)
  const { default: result } = await import(`data:text/javascript;base64,${Buffer.from(compiled).toString('base64')}`)
  return { ...result, render: () => null }
}

async function mount(t, name, props) {
  const renderer = createRenderer({ createComment: () => ({}), insert() {}, remove() {}, parentNode() {}, nextSibling() {} })
  const app = renderer.createApp(await component(name), props)
  app.mount({})
  t.after(() => app.unmount())
  await new Promise(resolve => setImmediate(resolve))
  await nextTick()
  return app._instance.setupState
}

function fakeBrowser() {
  globalThis.document = { addEventListener() {}, removeEventListener() {} }
  globalThis.window = { addEventListener() {}, removeEventListener() {} }
}

function response(data, status = 200) {
  return { ok: status >= 200 && status < 300, status, json: async () => data }
}

test('modal normaliza período antigo e envia a média de aprovação editada', async t => {
  const saved = []
  const state = await mount(t, 'DisciplineModal', {
    discipline: {
      name: 'Banco de Dados', professorName: 'Ana', color: '#6432df',
      periodo: '2', semester: '2026.2', passingAverage: 6, minimumAttendancePercentage: 75,
      schedules: [{ dayOfWeek: 'MONDAY', startTime: '08:00', endTime: '10:00' }],
    },
    onSave: value => saved.push(value),
  })

  assert.equal(state.periodo, '2026')
  assert.equal(state.semester, '2')
  state.passingAverage = 7.5
  state.submitForm()
  assert.equal(saved[0].periodo, '2026')
  assert.equal(saved[0].semester, '2')
  assert.equal(saved[0].passingAverage, 7.5)
  assert.equal(saved[0].minimumAttendancePercentage, 75)

  state.schedules.push({ id: 2, dayOfWeek: 'MONDAY', startTime: '09:00', endTime: '11:00' })
  state.submitForm()
  assert.equal(saved.length, 1)
  assert.match(state.timeError, /sobrepor/)
})

test('erro de carregamento permanece até uma nova tentativa bem-sucedida', async t => {
  fakeBrowser()
  let fail = true
  t.mock.method(globalThis, 'fetch', async path => {
    if (fail) throw new Error('API indisponível')
    return response(path.endsWith('/dashboards') ? [{ id: 'dashboard', status: 'ACTIVE' }] : [])
  })

  const state = await mount(t, 'DisciplinesEmpty', { accessToken: 'token' })
  assert.match(state.loadError, /API indisponível/)
  fail = false
  await state.loadDisciplines()
  assert.equal(state.loadError, '')
  assert.equal(state.disciplines.length, 0)
})

test('duplo envio cria a disciplina uma única vez', async t => {
  fakeBrowser()
  let finishPost
  const pendingPost = new Promise(resolve => { finishPost = resolve })
  let posts = 0
  t.mock.method(globalThis, 'fetch', async (path, options) => {
    if (options.method === 'POST') {
      posts++
      await pendingPost
      return response({ id: 'd1', name: 'Cálculo', professorName: '', color: '#6432df',
        schedules: [{ dayOfWeek: 'MONDAY', startTime: '08:00', endTime: '10:00' }] }, 201)
    }
    return response(path.endsWith('/dashboards') ? [{ id: 'dashboard', status: 'ACTIVE' }] : [])
  })

  const state = await mount(t, 'DisciplinesEmpty', { accessToken: 'token' })
  const form = { name: 'Cálculo', schedules: [] }
  const first = state.saveDiscipline(form)
  const second = state.saveDiscipline(form)
  assert.equal(posts, 1)
  finishPost()
  await Promise.all([first, second])
  assert.equal(state.disciplines.length, 1)
  assert.equal(state.saving, false)
})
