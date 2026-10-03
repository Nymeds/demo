import { readConfig } from './config.js'
import { selectSmtp } from './select-smtp.js'
import { createMailerServer } from './server.js'

try {
  const config = readConfig()
  const { transport, port } = await selectSmtp(config, { report: message => console.log(message) })
  const server = createMailerServer({
    ...config, transport, smtpPort: port,
    onError: () => console.error('Falha no envio de recuperação. Verifique a disponibilidade do SMTP.'),
  })
  server.on('error', () => {
    transport.close()
    console.error('Não foi possível iniciar o serviço de e-mail. Verifique a porta configurada.')
    process.exitCode = 1
  })
  server.listen(config.listenPort, config.host, () => {
    console.log(`Serviço privado de e-mail ativo em ${config.host}:${config.listenPort}.`)
  })
  for (const signal of ['SIGTERM', 'SIGINT']) {
    process.on(signal, () => server.close(() => transport.close()))
  }
} catch {
  console.error('Serviço de e-mail não iniciado. Verifique MAILER_API_SECRET, configuração SMTP e senha de app do Gmail.')
  process.exitCode = 1
}
