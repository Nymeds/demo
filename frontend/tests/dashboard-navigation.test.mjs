import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'
import { compileScript, parse } from '@vue/compiler-sfc'
import { createRenderer, nextTick, ref } from 'vue'
import { rewriteRelativeImports } from './helpers/rewriteImports.mjs'

test('as abas Frequência e Perfil abrem páginas separadas com autenticação', async t => {
  // Isola as telas filhas; a navegação e os eventos usam o componente real,
  // assim como os subcomponentes do próprio dashboard (irmãos Dashboard*.vue).
  const parts = ['DashboardSidebar', 'DashboardSummaryCards', 'DashboardStatusHero', 'DashboardClassesPanel',
    'DashboardActivitiesPanel', 'DashboardRemindersPanel', 'DashboardFrequencyPanel', 'DashboardProgressPanel',
    'DashboardGuideCards']
  const load = async name => {
    const sfcUrl = new URL(`../src/features/dashboard/${name}.vue`, import.meta.url)
    const { descriptor } = parse(await readFile(sfcUrl, 'utf8'))
    const compiled = compileScript(descriptor, {
      id: `navigation-test-${name}`,
      inlineTemplate: true,
      templateOptions: { compilerOptions: { hoistStatic: false } },
    })
    const code = compiled.content
      .replace(/import ['"][^'"]+\.css['"]/g, '')
      .replace(/import (\w+) from ['"]\.\/(Dashboard\w+)\.vue['"]/g, (_, local, file) =>
        `const ${local} = globalThis.__dashboardParts.${file}`)
      // O tema mexe em document/localStorage ao ser importado; o teste usa um tema em memória.
      .replace(/import \{ useTheme \} from ['"][^'"]+useTheme['"]/, 'const useTheme = () => globalThis.__dashboardTheme')
      .replace(/import (\w+) from ['"][^'"]+\.vue['"]/g, (_, local) =>
        `const ${local} = { render() { return testH('${local}') } }`)
      .replace(/from ['"]vue['"]/g, `from ${JSON.stringify(import.meta.resolve('vue'))}`)
    const { default: component } = await import(`data:text/javascript;base64,${Buffer.from(
      `import { h as testH } from ${JSON.stringify(import.meta.resolve('vue'))};
${rewriteRelativeImports(code, sfcUrl)}`,
    ).toString('base64')}`)
    return component
  }
  globalThis.__dashboardParts = {}
  const night = ref(false)
  globalThis.__dashboardTheme = { isNight: night, toggleTheme: () => { night.value = !night.value } }
  t.after(() => {
    delete globalThis.__dashboardParts
    delete globalThis.__dashboardTheme
  })
  for (const name of parts) globalThis.__dashboardParts[name] = await load(name)
  const DashboardScreen = await load('DashboardScreen')

  t.mock.method(globalThis, 'fetch', async input => {
    const path = String(input)
    if (path === '/api/v1/settings/avatar') return { ok: true, status: 204 }
    let data = []

    if (path === '/api/v1/dashboards') {
      data = [{ id: 'dashboard-1', status: 'ACTIVE' }]
    } else if (path === '/api/v1/dashboards/dashboard-1/disciplines') {
      data = [{
        id: 'discipline-1',
        name: 'Banco de Dados',
        professorName: 'Professora Ana',
        color: '#f59a17',
        schedules: [
          { dayOfWeek: 'MONDAY', startTime: '08:00:00', endTime: '10:00:00' },
          { dayOfWeek: 'TUESDAY', startTime: '19:00:00', endTime: '21:00:00' },
        ],
        average: null,
        attendancePercentage: 95,
        minimumAttendancePercentage: 75,
        absences: 1,
        lossPerAbsence: 5,
        maximumAbsences: 5,
      }]
    } else if (path === '/api/v1/dashboards/dashboard-1/activities') {
      data = [{ id: 'activity-1', title: 'Trabalho de banco', disciplineId: 'discipline-1', status: 'PENDING', dueDate: '2099-09-30' }]
    }

    return { ok: true, json: async () => data }
  })
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
  let logoutCount = 0
  app = renderer.createApp(DashboardScreen, { user: { name: 'Estudante' }, onLogout: () => { logoutCount += 1 } })
  // Componente usado no template sem import vira warning do Vue; o teste deve falhar nesse caso.
  const warnings = []
  app.config.warnHandler = message => { warnings.push(message) }
  app.mount(root)
  const all = item => [item, ...item.children.flatMap(all)]
  const textOf = item => item.text + item.children.map(textOf).join('')
  const buttonIn = (container, label) => all(container)
    .find(item => item.type === 'button' && textOf(item).trim() === label)
  const button = label => buttonIn(root, label)
  const moreSheet = () => all(root).find(item => item.props.id === 'dashboard-more-sheet')
  const moreButton = () => all(root).find(item => item.props.class?.includes('dashboard-more-button'))

  await new Promise(resolve => setImmediate(resolve))
  await nextTick()
  assert.deepEqual(warnings.filter(message => message.includes('Failed to resolve component')), [])
  const activityIcon = all(root).find(item => item.props.class === 'dashboard-compact-activity-icon')
  assert.equal(activityIcon?.props.style['--discipline-color'], '#f59a17')
  const frequencyRing = all(root).find(item => item.props.class === 'dashboard-frequency-ring')
  assert.ok(frequencyRing, 'o dashboard deve mostrar os detalhes da frequência')
  assert.match(textOf(frequencyRing), /95%\s*Frequência/)
  assert.equal(frequencyRing.props.style['--frequency-color'], '#20aa60')
  assert.match(textOf(root), /Faltas registradas\s*1/)
  assert.equal(textOf(root).includes('Resumo do período'), false)
  const nextClassList = all(root).find(item => item.props.class === 'dashboard-class-list')
  assert.ok(nextClassList, 'o dashboard deve mostrar a próxima aula')
  assert.equal(nextClassList.children.filter(item => item.type === 'li').length, 1)
  assert.match(textOf(root), /Sua próxima aula/)

  const frequencyButton = button('Frequência')
  assert.ok(frequencyButton, 'Frequência deve estar disponível no menu')
  assert.equal(all(root).some(item => item.type === 'FrequencyScreen'), false)
  frequencyButton.props.onClick()
  await nextTick()
  assert.equal(frequencyButton.props['aria-current'], 'page')
  assert.ok(all(root).find(item => item.type === 'FrequencyScreen'), 'FrequencyScreen deve ser exibida')
  assert.equal(all(root).some(item => item.props.class === 'dashboard-overview'), false)

  button('Dashboard').props.onClick()
  await nextTick()
  assert.equal(all(root).some(item => item.type === 'FrequencyScreen'), false)
  assert.equal(button('Dashboard').props['aria-current'], 'page')

  button('Notas').props.onClick()
  await nextTick()
  assert.ok(all(root).find(item => item.type === 'GradesScreen'), 'GradesScreen deve ser exibida')

  const userMenu = all(root).find(item => item.type === 'SidebarUserMenu')
  userMenu.props.onOpenSettings()
  await nextTick()
  assert.ok(all(root).find(item => item.type === 'SettingsScreen'), 'SettingsScreen deve ser exibida')

  const profileButton = button('Perfil')
  assert.ok(profileButton, 'Perfil deve estar disponível como uma aba do menu')
  profileButton.props.onClick()
  await nextTick()
  assert.equal(profileButton.props['aria-current'], 'page')
  assert.ok(all(root).find(item => item.type === 'ProfileScreen'), 'ProfileScreen deve ser exibida')
  assert.equal(
    all(root).some(item => item.type === 'button' && textOf(item).includes('Estudante')),
    false,
    'o cartão do usuário não deve mais abrir o perfil',
  )

  // Barra compacta (761–1100 px) esconde os rótulos: cada item precisa de title e aria-label.
  for (const label of ['Dashboard', 'Disciplinas', 'Atividades', 'Provas', 'Frequência', 'Notas',
    'Simulador de Notas', 'Calendário', 'Perfil', 'Sair']) {
    const item = button(label)
    assert.equal(item.props.title, label, `${label} deve ter title`)
    assert.equal(item.props['aria-label'], label, `${label} deve ter aria-label`)
  }

  // Celular: 4 destinos fixos na barra; os demais ficam marcados para a folha "Mais".
  const navigation = all(root).find(item => item.props['aria-label'] === 'Navegação principal')
  const primaryLabels = navigation.children
    .filter(item => item.type === 'button' && !item.props.class.includes('is-secondary'))
    .map(item => textOf(item).trim())
  assert.deepEqual(primaryLabels, ['Dashboard', 'Disciplinas', 'Atividades', 'Calendário', 'Mais'])

  const more = moreButton()
  assert.equal(more.props['aria-haspopup'], 'dialog')
  assert.equal(more.props['aria-expanded'], false)
  assert.equal(more.props['aria-controls'], undefined)
  assert.equal(moreSheet(), undefined, 'a folha "Mais" começa fechada')
  assert.ok(more.props.class.includes('active'), '"Mais" fica ativo quando a seção atual (Perfil) está nele')

  button('Calendário').props.onClick()
  await nextTick()
  assert.equal(moreButton().props.class.includes('active'), false)

  more.props.onClick()
  await nextTick()
  const sheet = moreSheet()
  assert.ok(sheet, 'a folha "Mais" deve abrir')
  assert.equal(sheet.props.role, 'dialog')
  assert.equal(sheet.props['aria-modal'], 'true')
  assert.equal(more.props['aria-expanded'], true)
  assert.equal(more.props['aria-controls'], 'dashboard-more-sheet')
  assert.deepEqual(
    sheet.children.filter(item => item.type === 'button').map(item => textOf(item).trim()),
    ['Provas', 'Frequência', 'Notas', 'Simulador de Notas', 'Perfil', 'Configurações', 'Modo noite', 'Sair'],
  )

  buttonIn(sheet, 'Modo noite').props.onClick()
  await nextTick()
  assert.equal(night.value, true)
  assert.equal(buttonIn(moreSheet(), 'Modo noite').props['aria-pressed'], true)

  buttonIn(sheet, 'Provas').props.onClick()
  await nextTick()
  assert.equal(moreSheet(), undefined, 'escolher um item fecha a folha')
  assert.equal(more.props['aria-expanded'], false)
  assert.ok(all(root).find(item => item.type === 'ProvasScreen'), 'ProvasScreen deve ser exibida')
  assert.ok(moreButton().props.class.includes('active'))

  more.props.onClick()
  await nextTick()
  buttonIn(moreSheet(), 'Configurações').props.onClick()
  await nextTick()
  assert.equal(moreSheet(), undefined)
  assert.ok(all(root).find(item => item.type === 'SettingsScreen'), 'SettingsScreen deve abrir pela folha')

  more.props.onClick()
  await nextTick()
  const backdrop = all(root).find(item => item.props.class === 'dashboard-more-backdrop')
  backdrop.props.onClick()
  await nextTick()
  assert.equal(moreSheet(), undefined, 'clicar fora fecha a folha')

  more.props.onClick()
  await nextTick()
  buttonIn(moreSheet(), 'Sair').props.onClick()
  await nextTick()
  assert.equal(logoutCount, 1)
  assert.equal(moreSheet(), undefined)

  assert.deepEqual(warnings.filter(message => message.includes('Failed to resolve component')), [])
})
