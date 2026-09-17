import assert from 'node:assert/strict'
import test from 'node:test'
import { readFile } from 'node:fs/promises'
import { compileScript, parse } from '@vue/compiler-sfc'
import { createRenderer, nextTick } from 'vue'

async function component(name) {
  const url = new URL(`../src/features/grades/${name}.vue`, import.meta.url)
  const { descriptor } = parse(await readFile(url, 'utf8'))
  const compiled = compileScript(descriptor, { id: name }).content
    .replace(/import (\w+) from ['"][^'"]+\.vue['"]/g, 'const $1 = {}')
    .replace(/import ['"][^'"]+\.css['"]/g, '')
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

test('notas filtra por situação real, pagina e limpa filtros', async t => {
  const rows = Array.from({ length: 11 }, (_, i) => ({ disciplineId: String(i), name: `Disciplina ${String(i).padStart(2, '0')}`, average: null, gradeCount: 0, periodo: '2026.2' }))
  t.mock.method(globalThis, 'fetch', async path => ({ ok: true, status: 200, json: async () => (
    path.endsWith('/gradebook') ? rows : path.endsWith('/disciplines')
      ? rows.map((row, i) => ({ id: row.disciplineId, status: i < 9 ? 'IN_PROGRESS' : 'LOCKED' }))
      : [{ id: 'dashboard', status: 'ACTIVE' }]
  ) }))
  const state = await mount(t, 'GradesScreen', { accessToken: 'token' })
  assert.equal(state.pagedEntries.length, 8)
  state.page = 2
  assert.equal(state.pagedEntries.length, 3)
  state.activeStatus = 'LOCKED'
  await nextTick()
  assert.equal(state.page, 1)
  assert.equal(state.visibleEntries.length, 2)
  state.search = '10'
  assert.equal(state.visibleEntries.length, 1)
  state.clearFilters()
  assert.equal(state.visibleEntries.length, 11)
  assert.equal(state.sortOrder, 'name')
})

test('modal envia observação e bloqueia nota inválida ou avaliação futura', async t => {
  const emitted = []
  const state = await mount(t, 'GradeEntryModal', {
    disciplineId: 'd1', disciplines: [{ id: 'd1', name: 'Banco' }], activitiesStatus: 'ready',
    activities: [{ id: 'a1', title: 'Prova', dueDate: '2020-01-01' }, { id: 'future', title: 'Futura', dueDate: '2099-01-01' }],
    onSave: value => emitted.push(value),
  })
  state.activityId = 'a1'
  state.score = 11
  state.submit()
  assert.equal(emitted.length, 0)
  state.score = 8.5
  state.observation = ' Trabalho em grupo '
  state.submit()
  assert.equal(emitted[0].observation, 'Trabalho em grupo')
  assert.equal(emitted[0].score, 8.5)
  state.activityId = 'future'
  state.submit()
  assert.equal(emitted.length, 1)
})

test('edição mantém a própria avaliação disponível e carrega a observação', async t => {
  const emitted = []
  const state = await mount(t, 'GradeEntryModal', {
    disciplineId: 'd1', activitiesStatus: 'ready', gradedActivityIds: ['a1'],
    grade: { id: 'g1', activityId: 'a1', score: 7, recordedAt: '2020-01-02', observation: 'Revisar' },
    activities: [{ id: 'a1', title: 'Prova', dueDate: '2020-01-01' }],
    onSave: value => emitted.push(value),
  })
  assert.equal(state.observation, 'Revisar')
  assert.equal(state.activityOptions[0].disabled, false)
  state.score = 9
  state.submit()
  assert.equal(emitted[0].score, 9)
})

test('salvar e excluir usam a disciplina correta e atualizam as médias', async t => {
  const writes = []
  let gradebookReads = 0
  t.mock.method(globalThis, 'fetch', async (path, options) => {
    let data = []
    if (options.method !== 'GET') writes.push({ path, method: options.method, body: options.body && JSON.parse(options.body) })
    if (path.endsWith('/dashboards')) data = [{ id: 'dashboard', status: 'ACTIVE' }]
    if (path.endsWith('/gradebook')) {
      gradebookReads++
      data = [{ disciplineId: 'd1', name: 'Banco', average: 8, gradeCount: 1 }]
    }
    if (path.endsWith('/disciplines')) data = [{ id: 'd1', status: 'IN_PROGRESS' }]
    return { ok: true, status: options.method === 'DELETE' ? 204 : 200, json: async () => data }
  })
  const state = await mount(t, 'GradesScreen', { accessToken: 'token' })
  const grade = { id: 'g1', assessmentName: 'Prova', score: 9, recordedAt: '2020-01-01', observation: 'Revisada' }
  await state.editGrade({ disciplineId: 'd1' }, grade)
  await state.saveGrade(grade)
  assert.equal(writes[0].method, 'PUT')
  assert.match(writes[0].path, /\/disciplines\/d1\/grades\/g1$/)
  assert.equal(writes[0].body.observation, 'Revisada')
  state.deletion = { entry: { disciplineId: 'd1' }, grade }
  await state.deleteGrade()
  assert.equal(writes[1].method, 'DELETE')
  assert.equal(gradebookReads, 3)
})
