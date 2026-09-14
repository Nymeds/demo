import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'
import { compileScript, parse } from '@vue/compiler-sfc'
import { createRenderer, nextTick } from 'vue'

test('a aba Frequência abre a página com autenticação e permite voltar ao dashboard', async t => {
  const source = await readFile(new URL('../src/features/dashboard/DashboardScreen.vue', import.meta.url), 'utf8')
  const { descriptor } = parse(source)
  const compiled = compileScript(descriptor, {
    id: 'navigation-test',
    inlineTemplate: true,
    templateOptions: { compilerOptions: { hoistStatic: false } },
  })
  // Isola as telas filhas; a navegação e os eventos usam o componente real.
  const code = compiled.content
    .replace(/import (\w+) from ['"][^'"]+\.vue['"]/g, (_, name) =>
      `const ${name} = { props: ['accessToken'], render() { return testH('${name}', { token: this.accessToken }) } }`)
    .replace(/from ['"]vue['"]/g, `from ${JSON.stringify(import.meta.resolve('vue'))}`)
  const { default: DashboardScreen } = await import(`data:text/javascript;base64,${Buffer.from(
    `import { h as testH } from ${JSON.stringify(import.meta.resolve('vue'))};\n${code}`,
  ).toString('base64')}`)

  t.mock.method(globalThis, 'fetch', async () => ({ ok: true, json: async () => [] }))
  const previousWindow = globalThis.window
  let app
  globalThis.window = { setInterval: () => 1, clearInterval: () => {} }
  t.after(() => {
    app?.unmount()
    if (previousWindow === undefined) delete globalThis.window
    else globalThis.window = previousWindow
  })

  const node = (type, text = '') => ({ type, text, props: {}, children: [], parent: null })
  const renderer = createRenderer({
    createElement: type => node(type),
    createText: text => node('text', text),
    createComment: text => node('comment', text),
    setText: (item, text) => { item.text = text },
    setElementText: (item, text) => { item.text = text; item.children = [] },
    patchProp: (item, key, previous, value) => { item.props[key] = value },
    parentNode: item => item.parent,
    nextSibling: item => item.parent?.children[item.parent.children.indexOf(item) + 1] || null,
    insert(item, parent, anchor = null) {
      item.parent = parent
      const index = anchor ? parent.children.indexOf(anchor) : -1
      if (index < 0) parent.children.push(item)
      else parent.children.splice(index, 0, item)
    },
    remove(item) {
      const siblings = item.parent?.children
      if (siblings) siblings.splice(siblings.indexOf(item), 1)
    },
  })
  const root = node('root')
  app = renderer.createApp(DashboardScreen, { user: { name: 'Estudante' }, accessToken: 'test-token' })
  app.mount(root)
  const all = item => [item, ...item.children.flatMap(all)]
  const textOf = item => item.text + item.children.map(textOf).join('')
  const button = label => all(root).find(item => item.type === 'button' && textOf(item).trim() === label)

  const frequencyButton = button('Frequência')
  assert.ok(frequencyButton, 'Frequência deve estar disponível no menu')
  assert.equal(all(root).some(item => item.type === 'FrequencyPage'), false)
  frequencyButton.props.onClick()
  await nextTick()
  assert.equal(frequencyButton.props['aria-current'], 'page')
  assert.equal(all(root).find(item => item.type === 'FrequencyPage')?.props.token, 'test-token')
  assert.equal(all(root).some(item => item.props.class === 'dashboard-overview'), false)

  button('Dashboard').props.onClick()
  await nextTick()
  assert.equal(all(root).some(item => item.type === 'FrequencyPage'), false)
  assert.equal(button('Dashboard').props['aria-current'], 'page')
})
