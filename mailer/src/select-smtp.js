import { createTransport } from './config.js'

export function smtpFailureReason(error) {
  if (error?.code === 'EAUTH') return 'Gmail recusou a autenticação; confira a senha de aplicativo e a conta.'
  if (error?.code === 'ETIMEDOUT') return 'tempo de conexão esgotado; verifique bloqueio de rede ou firewall.'
  if (error?.code === 'EDNS') return 'não foi possível resolver o endereço do servidor SMTP.'
  return 'conexão SMTP segura indisponível; confira rede e configuração.'
}

// verify() valida conexão, TLS e autenticação, sem enviar mensagem.
export async function selectSmtp(config, { factory = createTransport, report = () => {} } = {}) {
  const ports = config.port === 'auto' ? [465, 587] : [config.port]
  for (const port of ports) {
    const transport = factory({ ...config, port })
    let timer
    try {
      await Promise.race([
        transport.verify(),
        new Promise((_, reject) => {
          timer = setTimeout(() => reject(Object.assign(new Error('Timeout SMTP'), { code: 'ETIMEDOUT' })), 15000)
        }),
      ])
      report(`SMTP verificado na porta ${port} (${port === 465 ? 'TLS' : 'STARTTLS'}).`)
      return { transport, port }
    } catch (error) {
      transport.close()
      report(`SMTP na porta ${port}: ${smtpFailureReason(error)}`)
      // A mesma credencial será recusada em ambas as portas. Não repita logins inválidos.
      if (error?.code === 'EAUTH') break
    } finally {
      clearTimeout(timer)
    }
  }
  throw new Error('Nenhuma porta SMTP configurada completou conexão segura e autenticação.')
}
