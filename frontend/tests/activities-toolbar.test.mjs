import assert from 'node:assert/strict'
import test from 'node:test'
import { readFile } from 'node:fs/promises'
import { compileScript, parse } from '@vue/compiler-sfc'
import { createRenderer, nextTick } from 'vue'
import { rewriteRelativeImports } from './helpers/rewriteImports.mjs'

test('activity toolbar switches views and clears filters while preserving the view', async t => {
  const rows = [
    { id: '1', title: 'Zoologia', status: 'PENDING', dueDate: '2099-01-01', disciplineId: 'd1' },
    { id: '2', title: 'Algebra', status: 'COMPLETED', dueDate: '2099-02-01', disciplineId: 'd1' },
  ]
  const requested = []
  t.mock.method(globalThis, 'fetch', async path => {
    requested.push(path)
    return { ok: true, json: async () => (
      path.includes('/activities') ? rows : path.endsWith('/disciplines') ? [{ id: 'd1', name: 'Curso' }] : [{ id: 'dash', status: 'ACTIVE' }]
    ) }
  })
  const source = await readFile(new URL('../src/features/activities/ActivitiesScreen.vue', import.meta.url), 'utf8')
  const { descriptor } = parse(source)
  const cardUrl = new URL('../src/features/activities/ActivityCard.vue', import.meta.url)
  const card = compileScript(parse(await readFile(cardUrl, 'utf8')).descriptor, { id: 'activity-card', inlineTemplate: true }).content
    .replace(/from ['"]vue['"]/g, `from ${JSON.stringify(import.meta.resolve('vue'))}`)
  const cardModule = `data:text/javascript;base64,${Buffer.from(rewriteRelativeImports(card, cardUrl)).toString('base64')}`
  const compiled = compileScript(descriptor, { id: 'activities', inlineTemplate: true }).content
    .replace(/import ['"][^'"]+\.css['"]/, '')
    .replace(/import ActivityCard from ['"][^'"]+['"]/, `import ActivityCard from ${JSON.stringify(cardModule)}`)
    .replace(/import (\w+) from ['"][^'"]+\.vue['"]/g, 'const $1 = { render: () => null }')
    .replace(/from ['"]vue['"]/g, `from ${JSON.stringify(import.meta.resolve('vue'))}`)
  const { default: component } = await import(`data:text/javascript;base64,${Buffer.from(rewriteRelativeImports(compiled, new URL('../src/features/activities/ActivitiesScreen.vue', import.meta.url))).toString('base64')}`)
  const node = (type, text = '') => ({ type, text, props: {}, children: [], addEventListener() {}, removeEventListener() {} })
  const renderer = createRenderer({
    insertStaticContent(text, parent) { const el = node('static', text); el.parent = parent; parent.children.push(el); return [el, el] },
    createElement: node, createText: text => node('text', text), createComment: () => node('comment'),
    setText: (el, text) => { el.text = text }, setElementText: (el, text) => { el.text = text },
    patchProp: (el, key, prev, value) => { el.props[key] = value },
    insert(el, parent, anchor) { if (el.parent) el.parent.children.splice(el.parent.children.indexOf(el), 1); el.parent = parent; const index = parent.children.indexOf(anchor); parent.children.splice(index < 0 ? parent.children.length : index, 0, el) },
    remove(el) { if (el.parent) el.parent.children.splice(el.parent.children.indexOf(el), 1) },
    parentNode: el => el.parent, nextSibling: el => el.parent?.children[el.parent.children.indexOf(el) + 1],
  })
  const root = node('root')
  const app = renderer.createApp(component, { accessToken: 'test' })
  app.mount(root)
  t.after(() => app.unmount())
  await new Promise(resolve => setImmediate(resolve))
  await nextTick()
  // A tela Atividades lista só atividades; as provas ficam na tela Provas.
  assert.ok(requested.some(path => path.endsWith('/activities?type=ACTIVITY')))
  const all = (el = root) => [el, ...el.children.flatMap(child => all(child))]
  const find = predicate => all().find(predicate)
  const click = async el => { el.props.onClick(); await nextTick() }
  const cards = () => all().filter(el => el.type === 'article' && el.props.class?.split(' ').includes('activity-card'))
  const textOf = el => el.text + el.children.map(textOf).join('')
  const titles = () => all().filter(el => el.type === 'h2').map(el => textOf(el).trim())
  const grid = find(el => el.props['aria-label'] === 'Visualizar em cards')
  await click(grid)
  assert.equal(grid.props['aria-pressed'], true)
  assert.equal(cards().length, 2)
  assert.ok(find(el => el.props.class === 'activities-list is-grid'))
  await click(find(el => el.type === 'button' && el.text === 'Conclu\u00eddas'))
  assert.equal(cards().length, 1)
  assert.deepEqual(titles(), ['Algebra'])
  await click(find(el => el.props.class === 'activities-clear-filters'))
  assert.deepEqual(titles(), ['Zoologia', 'Algebra'])
  assert.equal(grid.props['aria-pressed'], true)
  await click(find(el => el.props['aria-label'] === 'Visualizar em lista'))
  assert.equal(grid.props['aria-pressed'], false)
  assert.equal(cards().length, 2)
})
