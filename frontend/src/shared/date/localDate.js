// Helpers de data em fuso local. Datas "YYYY-MM-DD" da API são datas de calendário
// (sem horário) e nunca devem passar por `new Date('YYYY-MM-DD')`, que interpreta UTC.

const ISO_DATE = /^(\d{4})-(\d{2})-(\d{2})$/
const MS_PER_DAY = 86400000

export function parseLocalDate(value) {
  if (typeof value !== 'string') return null
  const match = ISO_DATE.exec(value.slice(0, 10))
  if (!match) return null
  const [year, month, day] = [Number(match[1]), Number(match[2]), Number(match[3])]
  const date = new Date(year, month - 1, day)
  if (date.getFullYear() !== year || date.getMonth() !== month - 1 || date.getDate() !== day) return null
  return date
}

export function toLocalIso(date) {
  const pad = value => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

export function todayIso(now = new Date()) {
  return toLocalIso(now)
}

export function startOfDay(date) {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate())
}

// Diferença em dias de calendário (robusta a horário de verão).
export function daysBetween(from, to) {
  return Math.round((startOfDay(to) - startOfDay(from)) / MS_PER_DAY)
}

// Vencida = data anterior a hoje. `due` pode ser 'YYYY-MM-DD' ou Date.
export function isOverdue(due, now = new Date()) {
  const date = due instanceof Date ? due : parseLocalDate(due)
  if (!date) return false
  return daysBetween(now, date) < 0
}

// LocalDateTime da API: 'YYYY-MM-DDTHH:mm[:ss[.fff]]', sem fuso (horário de parede local).
const ISO_DATE_TIME = /^(\d{4})-(\d{2})-(\d{2})[T ](\d{2}):(\d{2})(?::(\d{2}))?(?:\.\d+)?$/

export function parseLocalDateTime(value) {
  if (typeof value !== 'string') return null
  const match = ISO_DATE_TIME.exec(value)
  if (!match) return null
  const [year, month, day, hour, minute, second] = match.slice(1).map(part => Number(part ?? 0))
  const date = new Date(year, month - 1, day, hour, minute, second)
  return Number.isNaN(date.getTime()) ? null : date
}

export function toLocalDateTimeIso(date) {
  const pad = number => String(number).padStart(2, '0')
  return `${toLocalIso(date)}T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

// Normaliza para 'YYYY-MM-DDTHH:mm:ss' (o formato enviado e comparado como texto). '' se inválido.
export function normalizeLocalDateTime(value) {
  const date = parseLocalDateTime(value)
  return date ? toLocalDateTimeIso(date) : ''
}
