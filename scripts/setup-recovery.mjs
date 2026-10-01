import { readFileSync, writeFileSync, existsSync } from 'node:fs'
import { randomBytes } from 'node:crypto'
import { fileURLToPath } from 'node:url'
import { resolve } from 'node:path'

const root = fileURLToPath(new URL('../', import.meta.url))
const backendPath = resolve(root, '.env')
const mailerPath = resolve(root, 'mailer/.env')
let backend = readFileSync(existsSync(backendPath) ? backendPath : resolve(root, '.env.example'), 'utf8')
let mailer = readFileSync(existsSync(mailerPath) ? mailerPath : resolve(root, 'mailer/.env.example'), 'utf8')

function get(text, key) {
  return text.match(new RegExp(`^${key}=(.*)$`, 'm'))?.[1]?.trim() || ''
}

function set(text, key, value) {
  const pattern = new RegExp(`^${key}=.*$`, 'm')
  return pattern.test(text) ? text.replace(pattern, `${key}=${value}`) : `${text.trimEnd()}\n${key}=${value}\n`
}

const backendSecret = get(backend, 'MAILER_API_SECRET')
const mailerSecret = get(mailer, 'MAILER_API_SECRET')
if (backendSecret && mailerSecret && backendSecret !== mailerSecret) {
  throw new Error('MAILER_API_SECRET difere entre .env e mailer/.env. Ajuste os arquivos localmente antes de continuar.')
}
const sharedSecret = backendSecret || mailerSecret || randomBytes(32).toString('hex')
if (Buffer.byteLength(sharedSecret) < 32) throw new Error('MAILER_API_SECRET existente deve ter pelo menos 32 bytes.')
backend = set(backend, 'MAILER_API_SECRET', sharedSecret)
mailer = set(mailer, 'MAILER_API_SECRET', sharedSecret)
if (!get(backend, 'RECOVERY_HASH_SECRET')) backend = set(backend, 'RECOVERY_HASH_SECRET', randomBytes(32).toString('hex'))
if (!get(backend, 'MAILER_URL')) backend = set(backend, 'MAILER_URL', 'http://127.0.0.1:3001')
if (!get(backend, 'MAILER_AUTO_START')) backend = set(backend, 'MAILER_AUTO_START', 'true')
writeFileSync(backendPath, backend, { mode: 0o600 })
writeFileSync(mailerPath, mailer, { mode: 0o600 })
console.log('Configuração local criada: .env e mailer/.env. As chaves foram sincronizadas sem exibi-las.')
console.log('Preencha SMTP_USER e SMTP_PASSWORD em mailer/.env com seu Gmail e sua senha de app.')
console.log('Após npm ci na pasta mailer, a API inicia o Nodemailer automaticamente ao subir.')
