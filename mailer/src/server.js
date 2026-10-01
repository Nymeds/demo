import { createServer } from 'node:http'
import { timingSafeEqual } from 'node:crypto'

export function createMailerServer({ secret, transport, user, fromName, smtpPort, onError = () => {} }) {
  const expected = Buffer.from(`Bearer ${secret}`)
  let inFlight = 0
  const server = createServer(async (request, response) => {
    response.setHeader('Cache-Control', 'no-store')
    const health = request.method === 'GET' && request.url === '/internal/health'
    if (!health && (request.method !== 'POST' || request.url !== '/internal/recovery-email')) {
      response.writeHead(404).end()
      return
    }
    const actual = Buffer.from(request.headers.authorization || '')
    if (actual.length !== expected.length || !timingSafeEqual(actual, expected)) {
      response.writeHead(401).end()
      return
    }
    if (health) {
      response.writeHead(200, { 'Content-Type': 'application/json' }).end(JSON.stringify({ status: 'ready', smtpPort }))
      return
    }
    if (!request.headers['content-type']?.startsWith('application/json')) {
      response.writeHead(415).end()
      return
    }
    if (inFlight >= 5) {
      response.writeHead(503).end()
      return
    }
    inFlight++
    try {
      const chunks = []
      let size = 0
      for await (const chunk of request) {
        size += chunk.length
        if (size > 2048) {
          response.writeHead(413, { Connection: 'close' }).end()
          return
        }
        chunks.push(chunk)
      }
      let body
      try {
        body = JSON.parse(Buffer.concat(chunks).toString('utf8'))
      } catch {
        response.writeHead(400).end()
        return
      }
      if (!body || typeof body.email !== 'string' || typeof body.code !== 'string') {
        response.writeHead(400).end()
        return
      }
      const email = body.email
      if (email.length > 150 || !/^[^\s<>@,;]+@[^\s<>@,;]+\.[^\s<>@,;]+$/.test(email)
          || !/^[0-9]{6}$/.test(body.code)) {
        response.writeHead(400).end()
        return
      }
      await transport.sendMail({
        from: { name: fromName, address: user },
        to: { address: email },
        subject: 'Seu código para recuperar a senha — AcadOrganize',
        text: `Recebemos uma solicitação para recuperar sua senha no AcadOrganize.\n\nSeu código é: ${body.code}\n\nEle expira em 10 minutos e só pode ser usado uma vez. Não compartilhe este código.\n\nSe você não fez esta solicitação, ignore este e-mail. Sua senha permanece a mesma.`,
        html: `<div style="font-family:Arial,sans-serif;max-width:520px;margin:auto;color:#302d41"><h1 style="font-size:24px;color:#6849d7">Recupere sua senha</h1><p>Recebemos uma solicitação para recuperar sua senha no AcadOrganize.</p><p>Digite este código na tela de recuperação:</p><p style="font-size:32px;font-weight:bold;letter-spacing:6px;background:#f5f3ff;padding:20px;text-align:center">${body.code}</p><p>O código expira em <strong>10 minutos</strong> e só pode ser usado uma vez. Não compartilhe este código.</p><p>Se você não fez esta solicitação, ignore este e-mail. Sua senha permanece a mesma.</p></div>`,
      })
      response.writeHead(204).end()
    } catch {
      onError() // Não exponha códigos, destinatários, credenciais ou erros SMTP completos.
      response.writeHead(503).end()
    } finally {
      inFlight--
    }
  })
  server.requestTimeout = 10000
  server.headersTimeout = 5000
  return server
}
