import { ATTENTION_MARGIN, frequencySituation, LOSS_PER_ABSENCE, maximumAbsencesFor } from './frequencyRules.js'
import { parseLocalDate } from '../../shared/date/localDate.js'

export const HISTORY_PREVIEW_SIZE = 3

export const SITUATION_FILTERS = [
  { value: 'all', label: 'Todas' },
  { value: 'good', label: 'Ótimo' },
  { value: 'warning', label: 'Atenção' },
  { value: 'bad', label: 'Ruim' },
]

export const SITUATION_DETAILS = {
  good: { label: 'Ótimo', className: 'is-success' },
  warning: { label: 'Atenção', className: 'is-warning' },
  bad: { label: 'Ruim', className: 'is-danger' },
  neutral: { label: 'Sem frequência cadastrada', className: 'is-neutral' },
}

// attendancePercentage chega como BigDecimal e pode ser serializado como string.
export function normalizeFrequency(frequency) {
  return {
    ...frequency,
    attendancePercentage: Number(frequency.attendancePercentage),
  }
}

export function normalizeDiscipline(discipline) {
  return {
    ...discipline,
    color: discipline.color || '#6432df',
    schedules: (discipline.schedules ?? []).map(schedule => ({
      ...schedule,
      startTime: schedule.startTime.slice(0, 5),
      endTime: schedule.endTime.slice(0, 5),
    })),
  }
}

// O GET de disciplinas já traz absences, attendancePercentage e maximumAbsences.
// attendancePercentage só vem nulo quando a disciplina ainda não tem frequência
// cadastrada (o mesmo caso em que GET .../frequency responde 404).
export function frequencyOf(discipline) {
  if (discipline.attendancePercentage == null) return null

  return normalizeFrequency({
    absences: discipline.absences,
    attendancePercentage: discipline.attendancePercentage,
    maximumAbsences: discipline.maximumAbsences,
  })
}

export function withFrequency(discipline) {
  return { ...normalizeDiscipline(discipline), frequency: frequencyOf(discipline) }
}

export function sortAbsenceHistory(entries) {
  return [...entries].sort((first, second) => {
    const dateOrder = second.date.localeCompare(first.date)
    return dateOrder || second.createdAt.localeCompare(first.createdAt)
  })
}

export function visibleHistoryOf(entries, showAll) {
  return showAll ? entries : entries.slice(0, HISTORY_PREVIEW_SIZE)
}

export function periodOf(discipline) {
  if (!discipline.periodo) return '—'
  return `${discipline.periodo}.${discipline.semester ?? 1}`
}

export function buildRow(discipline, attendanceAlertMargin = ATTENTION_MARGIN) {
  const frequency = discipline.frequency
  const minimumPercentage = Number(discipline.minimumAttendancePercentage ?? 0)

  const base = {
    id: discipline.id,
    name: discipline.name,
    // Troque por discipline.code caso você adicione o campo na entidade Discipline.
    subtitle: discipline.professorName || periodOf(discipline),
    color: discipline.color,
    period: periodOf(discipline),
    minimumPercentage,
    discipline,
  }

  if (!frequency) {
    return {
      ...base,
      configured: false,
      absences: 0,
      attendancePercentage: null,
      lossPerAbsence: LOSS_PER_ABSENCE,
      remainingAbsences: maximumAbsencesFor(minimumPercentage),
      situation: 'neutral',
    }
  }

  const remainingAbsences = frequency.maximumAbsences - frequency.absences

  return {
    ...base,
    configured: true,
    absences: frequency.absences,
    attendancePercentage: frequency.attendancePercentage,
    lossPerAbsence: LOSS_PER_ABSENCE,
    remainingAbsences,
    situation: frequencySituation(
      frequency.attendancePercentage,
      minimumPercentage,
      remainingAbsences,
      attendanceAlertMargin,
    ),
  }
}

export function periodOptionsOf(rows) {
  const periods = new Set(rows.map(row => row.period).filter(period => period !== '—'))
  return [...periods].sort().reverse()
}

export function filterRows(rows, { searchTerm, periodFilter, situationFilter }) {
  const search = searchTerm.trim().toLocaleLowerCase('pt-BR')

  return rows.filter(row => {
    const matchesSearch = !search
      || row.name.toLocaleLowerCase('pt-BR').includes(search)
      || row.subtitle.toLocaleLowerCase('pt-BR').includes(search)
    const matchesPeriod = periodFilter === 'all' || row.period === periodFilter
    const matchesSituation = situationFilter === 'all' || row.situation === situationFilter

    return matchesSearch && matchesPeriod && matchesSituation
  })
}

export function averageAttendanceOf(rows) {
  const values = rows
    .map(row => row.attendancePercentage)
    .filter(value => typeof value === 'number')

  return values.length
    ? values.reduce((total, value) => total + value, 0) / values.length
    : null
}

export function totalAbsencesOf(rows) {
  return rows.reduce((total, row) => total + row.absences, 0)
}

export function averageLabelOf(average) {
  if (average === null) return 'Aguardando configuração'
  if (average >= 90) return 'Boa frequência'
  if (average >= 75) return 'Atenção às faltas'
  return 'Frequência crítica'
}

export function formatPercentage(value, digits = 0) {
  return typeof value === 'number'
    ? `${value.toLocaleString('pt-BR', { maximumFractionDigits: digits })}%`
    : '—'
}

export function formatDate(isoDate) {
  const date = parseLocalDate(isoDate)
  if (!date) return '—'
  const weekday = date.toLocaleDateString('pt-BR', { weekday: 'short' }).replace('.', '')

  return `${date.toLocaleDateString('pt-BR')} (${weekday.charAt(0).toUpperCase()}${weekday.slice(1)})`
}

export function barClass(row) {
  return `is-${row.situation}`
}
