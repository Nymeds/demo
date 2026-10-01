import { readConfig } from './config.js'
import { selectSmtp } from './select-smtp.js'

let transport
try {
  const selected = await selectSmtp(readConfig(), { report: message => console.log(message) })
  transport = selected.transport
  console.log('Conexão e autenticação SMTP verificadas. Nenhum e-mail foi enviado.')
} catch {
  console.error('Verificação SMTP falhou. Confira configuração, rede e senha de app do Gmail.')
  process.exitCode = 1
} finally {
  transport?.close()
}
