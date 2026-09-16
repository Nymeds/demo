import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'
import { compileScript, parse } from '@vue/compiler-sfc'
import { createRenderer, nextTick } from 'vue'

const source = await readFile(new URL('../src/features/profile/ProfileScreen.vue', import.meta.url), 'utf8')
const { descriptor } = parse(source)
const compiled = compileScript(descriptor, { id: 'profile-photo-test' }).content
  .replace(/import (\w+) from ['"][^'"]+\.vue['"]/g, 'const $1 = {}')
  .replace(/from ['"]vue['"]/g, `from ${JSON.stringify(import.meta.resolve('vue'))}`)
const { default: ProfileScreen } = await import(`data:text/javascript;base64,${Buffer.from(compiled).toString('base64')}`)
ProfileScreen.render = () => null

async function mountProfile(t, hasPhoto = false) {
  let user = { name: 'Teste', email: 'teste@teste.com', hasProfilePhoto: hasPhoto, profilePhotoUrl: hasPhoto ? '/photo' : null }
  const writes = []
  let failPhoto = false
  t.mock.method(globalThis, 'fetch', async (path, options = {}) => {
    if (path === '/photo') return { ok: true, blob: async () => new Blob(['photo']) }
    if (options.method) {
      writes.push({ path, ...options })
      if (path.endsWith('/profile-photo')) {
        if (failPhoto) return { ok: false, status: 500, json: async () => ({ message: 'Falha no envio' }) }
        user = { ...user, hasProfilePhoto: options.method === 'PUT', profilePhotoUrl: '/photo' }
        if (options.method === 'DELETE') return { ok: true, status: 204 }
      } else user = { ...user, ...JSON.parse(options.body) }
    }
    return { ok: true, status: 200, json: async () => ({ ...user }) }
  })
  const renderer = createRenderer({
    createComment: () => ({}), insert() {}, remove() {}, parentNode() {}, nextSibling() {},
  })
  const app = renderer.createApp(ProfileScreen, { user, accessToken: 'test-token' })
  app.mount({})
  t.after(() => app.unmount())
  await new Promise(resolve => setImmediate(resolve))
  await nextTick()
  return {
    state: app._instance.setupState,
    writes,
    failPhoto: value => { failPhoto = value },
    user: () => user,
  }
}

function selectPhoto(state) {
  state.uploadPhoto({ target: { files: [new File(['image'], 'photo.png', { type: 'image/png' })], value: 'photo.png' } })
  assert.ok(state.cropFile)
  state.confirmPhotoCrop(new Blob(['cropped image'], { type: 'image/jpeg' }))
}

test('cancelar recorte nao altera a foto nem envia requisicoes', async t => {
  const { state, writes } = await mountProfile(t, true)
  const originalUrl = state.displayedAvatarUrl
  state.uploadPhoto({ target: { files: [new File(['image'], 'photo.webp', { type: 'image/webp' })], value: '' } })
  assert.ok(state.cropFile)
  assert.equal(state.isDirty, false)
  await state.closeCrop()
  assert.equal(state.displayedAvatarUrl, originalUrl)
  assert.equal(writes.length, 0)
})

test('selecionar foto mostra previa e so grava ao salvar', async t => {
  const { state, writes, user } = await mountProfile(t)
  assert.equal(state.isDirty, false)
  selectPhoto(state)
  assert.equal(state.isDirty, true)
  assert.ok(state.displayedAvatarUrl)
  assert.equal(writes.length, 0)
  assert.equal(user().hasProfilePhoto, false)
  await state.saveProfile()
  assert.equal(writes.length, 1)
  assert.equal(writes[0].method, 'PUT')
  assert.ok(writes[0].body instanceof FormData)
  assert.equal(user().hasProfilePhoto, true)
  assert.equal(state.isDirty, false)
})

test('remover e cancelar restauram a foto; remover e salvar envia DELETE', async t => {
  const { state, writes } = await mountProfile(t, true)
  const originalUrl = state.displayedAvatarUrl
  state.removePhoto()
  assert.equal(state.displayedAvatarUrl, '')
  assert.equal(state.isDirty, true)
  assert.equal(writes.length, 0)
  state.cancelChanges()
  assert.equal(state.displayedAvatarUrl, originalUrl)
  assert.equal(state.isDirty, false)
  selectPhoto(state)
  state.cancelChanges()
  assert.equal(state.displayedAvatarUrl, originalUrl)
  state.removePhoto()
  await state.saveProfile()
  assert.equal(writes.length, 1)
  assert.equal(writes[0].method, 'DELETE')
  assert.equal(state.hasProfilePhoto, false)
  assert.equal(state.isDirty, false)
})

test('adicionar e remover uma foto nova nao deixa alteracoes pendentes', async t => {
  const { state, writes } = await mountProfile(t)
  selectPhoto(state)
  state.removePhoto()
  assert.equal(state.isDirty, false)
  await state.saveProfile()
  assert.equal(writes.length, 0)
})

test('falha no envio preserva a foto pendente para tentar salvar novamente', async t => {
  const { state, writes, failPhoto } = await mountProfile(t)
  state.form.name = 'Novo nome'
  selectPhoto(state)
  failPhoto(true)
  await state.saveProfile()
  assert.equal(state.isDirty, true)
  assert.ok(state.displayedAvatarUrl)
  assert.equal(state.saving, false)
  assert.equal(state.toast.type, 'error')
  failPhoto(false)
  await state.saveProfile()
  assert.equal(state.isDirty, false)
  assert.equal(writes.filter(request => request.path === '/api/v1/users/me').length, 1)
})
