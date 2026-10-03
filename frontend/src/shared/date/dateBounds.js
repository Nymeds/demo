// Limites de datas "AAAA-MM-DD" (min/max opcionais) usados pelo AppDatePicker.
// Strings ISO comparam corretamente em ordem alfabética.

const ISO_DATE = /^\d{4}-\d{2}-\d{2}$/

export function isIsoDate(value) {
  return ISO_DATE.test(value ?? '')
}

export function isOutOfBounds(iso, min = '', max = '') {
  return (isIsoDate(min) && iso < min) || (isIsoDate(max) && iso > max)
}

export function clampToBounds(iso, min = '', max = '') {
  if (isIsoDate(min) && iso < min) return min
  if (isIsoDate(max) && iso > max) return max
  return iso
}

// Mês (year, monthIndex 0-11) tem algum dia dentro dos limites?
export function monthHasDaysInBounds(year, monthIndex, min = '', max = '') {
  const pad = value => String(value).padStart(2, '0')
  const first = new Date(year, monthIndex, 1)
  const last = new Date(year, monthIndex + 1, 0)
  const firstIso = `${first.getFullYear()}-${pad(first.getMonth() + 1)}-01`
  const lastIso = `${last.getFullYear()}-${pad(last.getMonth() + 1)}-${pad(last.getDate())}`
  return !(isIsoDate(min) && lastIso < min) && !(isIsoDate(max) && firstIso > max)
}

// Anos do seletor de ano, do mais recente ao mais antigo: do ano de max (ou o atual)
// até o ano de min (ou `span` anos antes).
export function yearsInBounds(min = '', max = '', today = new Date(), span = 100) {
  const minYear = isIsoDate(min) ? Number(min.slice(0, 4)) : null
  let last = isIsoDate(max) ? Number(max.slice(0, 4)) : today.getFullYear()
  if (minYear !== null && minYear > last) last = minYear
  const first = minYear ?? last - span
  return Array.from({ length: last - first + 1 }, (_, index) => last - index)
}
