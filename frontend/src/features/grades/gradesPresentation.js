// Regras de apresentação da tela Notas. As faixas repetem as do backend (PerformanceBand) para
// também classificar a média geral, que é calculada aqui a partir das disciplinas.

export const BANDS = Object.freeze({
  EXCELLENT: { label: 'Excelente', range: '9 a 10', summary: 'Desempenho excelente', tone: 'excellent' },
  GOOD: { label: 'Bom', range: '7 a 8,9', summary: 'Bom desempenho', tone: 'good' },
  REGULAR: { label: 'Regular', range: '5 a 6,9', summary: 'Desempenho regular', tone: 'regular' },
  INSUFFICIENT: { label: 'Insuficiente', range: 'abaixo de 5', summary: 'Desempenho insuficiente', tone: 'insufficient' },
  NO_GRADES: { label: 'Sem notas', range: 'nenhuma avaliação', summary: 'Sem notas lançadas', tone: 'empty' },
})

export const GRADED_BANDS = Object.freeze(['EXCELLENT', 'GOOD', 'REGULAR', 'INSUFFICIENT'])

export const SORT_OPTIONS = Object.freeze([
  { value: 'average-desc', label: 'Média (maior)' },
  { value: 'average-asc', label: 'Média (menor)' },
  { value: 'name', label: 'Nome (A–Z)' },
  { value: 'grades-desc', label: 'Mais avaliações' },
])

const FALLBACK_COLOR = '#6832df'
const HEX_COLOR = /^#[0-9a-f]{6}$/i
// Um texto começando com estes caracteres vira fórmula ao abrir o CSV no Excel.
const CSV_FORMULA_PREFIX = /^[=+\-@\t\r]/
const EXCELLENT_FROM = 9
const GOOD_FROM = 7
const REGULAR_FROM = 5

function hasAverage(entry) {
  return entry.average !== null && entry.average !== undefined
}

function averageOf(values) {
  return values.length ? values.reduce((total, value) => total + value, 0) / values.length : null
}

export function bandInfo(band) {
  return BANDS[band] ?? BANDS.NO_GRADES
}

export function bandOf(average) {
  if (average === null || average === undefined) return 'NO_GRADES'
  if (average >= EXCELLENT_FROM) return 'EXCELLENT'
  if (average >= GOOD_FROM) return 'GOOD'
  return average >= REGULAR_FROM ? 'REGULAR' : 'INSUFFICIENT'
}

export function formatGrade(value) {
  if (value === null || value === undefined || Number.isNaN(Number(value))) return '—'

  return Number(value).toLocaleString('pt-BR', { minimumFractionDigits: 1, maximumFractionDigits: 1 })
}

// A cor vem da disciplina; só um hexadecimal válido chega ao estilo da página.
export function safeColor(color) {
  return HEX_COLOR.test(color ?? '') ? color : FALLBACK_COLOR
}

// Período no formato "2026.2". Aceita os dois jeitos que as disciplinas já foram salvas: ano em
// "periodo" e semestre em "semester", ou o contrário ("2026.2" em semester e "2" em periodo).
export function periodKeyOf(entry) {
  const values = [String(entry.periodo ?? ''), String(entry.semester ?? '')]
  const year = values.map(value => value.match(/\d{4}/)?.[0]).find(Boolean)

  if (!year) return null

  const directSemester = values.map(Number).find(value => value === 1 || value === 2)
  const partSemester = values
    .flatMap(value => value.split(/[.\-/]/))
    .map(Number)
    .find(value => value === 1 || value === 2)

  return `${year}.${directSemester ?? partSemester ?? 1}`
}

function comparePeriodKeys(first, second) {
  const [firstYear, firstSemester] = first.split('.').map(Number)
  const [secondYear, secondSemester] = second.split('.').map(Number)
  return firstYear - secondYear || firstSemester - secondSemester
}

export function periodKeysOf(entries) {
  return [...new Set(entries.map(periodKeyOf).filter(Boolean))].sort(comparePeriodKeys).reverse()
}

export function latestPeriodKey(entries) {
  return periodKeysOf(entries)[0] ?? null
}

export function summarize(entries) {
  const graded = entries.filter(hasAverage)
  const best = graded.reduce((top, entry) => (
    !top || Number(entry.average) > Number(top.average) ? entry : top
  ), null)

  return Object.freeze({
    generalAverage: averageOf(graded.map(entry => Number(entry.average))),
    disciplineCount: entries.length,
    gradedCount: graded.length,
    best: best ? { name: best.name, average: Number(best.average) } : null,
  })
}

export function distributionOf(entries) {
  const graded = entries.filter(hasAverage)

  return GRADED_BANDS.map(band => {
    const count = graded.filter(entry => entry.band === band).length
    return { band, count, percent: graded.length ? Math.round((count / graded.length) * 100) : 0 }
  })
}

// Média geral de cada período que já tem notas, do mais antigo para o mais recente.
export function evolutionOf(entries) {
  const averagesByPeriod = new Map()

  entries.filter(hasAverage).forEach(entry => {
    const period = periodKeyOf(entry)
    if (!period) return
    averagesByPeriod.set(period, [...(averagesByPeriod.get(period) ?? []), Number(entry.average)])
  })

  return [...averagesByPeriod.keys()]
    .sort(comparePeriodKeys)
    .map(period => ({ period, average: averageOf(averagesByPeriod.get(period)) }))
}

function normalizeText(text) {
  return String(text ?? '').normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase()
}

export function matchesSearch(entry, term) {
  const query = normalizeText(term).trim()
  return !query || normalizeText(entry.name).includes(query) || normalizeText(entry.professorName).includes(query)
}

const SORTERS = Object.freeze({
  'average-desc': (first, second) => (second.average ?? -1) - (first.average ?? -1),
  'average-asc': (first, second) => (first.average ?? 11) - (second.average ?? 11),
  name: () => 0,
  'grades-desc': (first, second) => second.gradeCount - first.gradeCount,
})

export function sortEntries(entries, order) {
  const sorter = SORTERS[order] ?? SORTERS.name
  return [...entries].sort((first, second) => sorter(first, second) || first.name.localeCompare(second.name, 'pt-BR'))
}

function csvCell(value) {
  const text = String(value ?? '')
  const safeText = CSV_FORMULA_PREFIX.test(text) ? `'${text}` : text
  return `"${safeText.replace(/"/g, '""')}"`
}

export function toCsv(entries) {
  const header = ['Disciplina', 'Professor', 'Período', 'Avaliações', 'Média parcial', 'Média de aprovação', 'Situação']
  const rows = entries.map(entry => [
    entry.name,
    entry.professorName,
    periodKeyOf(entry) ?? '',
    entry.gradeCount,
    formatGrade(entry.average),
    formatGrade(entry.passingAverage),
    bandInfo(entry.band).label,
  ])

  return [header, ...rows].map(row => row.map(csvCell).join(';')).join('\r\n')
}
