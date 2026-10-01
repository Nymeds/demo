import nodemailer from 'nodemailer'

export function readConfig(env = process.env) {
  const secret = env.MAILER_API_SECRET || ''
  const user = env.SMTP_USER || ''
  const password = (env.SMTP_PASSWORD || '').replace(/\s/g, '')
  const port = !env.SMTP_PORT || env.SMTP_PORT === 'auto' ? 'auto' : Number(env.SMTP_PORT)
  const listenPort = Number(env.MAILER_PORT || 3001)
  if (Buffer.byteLength(secret) < 32 || /[\r\n]/.test(secret)) {
    throw new Error('Configure MAILER_API_SECRET com pelo menos 32 bytes.')
  }
  if (!/^[^\s<>@]+@[^\s<>@]+\.[^\s<>@]+$/.test(user) || !password || password === 'senha-de-app-do-google') {
    throw new Error('Configure SMTP_USER e SMTP_PASSWORD com seu Gmail e uma senha de app do Google.')
  }
  if (!['auto', 465, 587].includes(port) || !Number.isInteger(listenPort) || listenPort < 1 || listenPort > 65535) {
    throw new Error('SMTP_PORT deve ser auto, 465 ou 587; MAILER_PORT deve ser uma porta válida.')
  }
  return {
    secret, user, password, port,
    smtpHost: env.SMTP_HOST || 'smtp.gmail.com',
    host: env.MAILER_HOST || '127.0.0.1',
    listenPort,
    fromName: env.MAIL_FROM_NAME || 'AcadOrganize',
  }
}

export function createTransport(config) {
  if (![465, 587].includes(config.port)) throw new Error('Selecione uma porta SMTP antes de criar o transporte.')
  return nodemailer.createTransport({
    host: config.smtpHost,
    port: config.port,
    secure: config.port === 465,
    requireTLS: true,
    auth: { user: config.user, pass: config.password },
    tls: { minVersion: 'TLSv1.2', rejectUnauthorized: true },
    connectionTimeout: 5000,
    greetingTimeout: 5000,
    socketTimeout: 15000,
    disableFileAccess: true,
    disableUrlAccess: true,
    logger: false,
    debug: false,
  })
}
