import { periodKeyOf } from '../grades/gradesPresentation.js'
import { formatAverage, formatScore } from '../../shared/format/grade.js'

export const MIN_AVERAGE = 0
export const MAX_AVERAGE = 10

// Valida o cenário lido do localStorage: dados corrompidos ou fora de 0..10 são descartados.
export function parseSavedScenario(raw) {
  if (!raw) return null
  try {
    const data = JSON.parse(raw)
    const value = data?.desiredAverage
    if (typeof value !== 'number' || !Number.isFinite(value)) return null
    if (value < MIN_AVERAGE || value > MAX_AVERAGE) return null
    return { desiredAverage: value }
  } catch {
    return null
  }
}

// Média projetada caso a próxima nota seja `nextGrade`. Nota inválida resulta em null.
export function projectedAverage(noteValues, nextGrade) {
  const grade = Number(nextGrade)
  if (nextGrade === null || nextGrade === '' || !Number.isFinite(grade)) return null
  const values = noteValues.map(Number)
  if (values.some(value => !Number.isFinite(value))) return null
  return (values.reduce((sum, value) => sum + value, 0) + grade) / (values.length + 1)
}

// Mesma regra de período da tela Notas: o ano pode estar em "periodo" ou em "semester"
// (disciplinas antigas gravam "2026.2" em semester e "2" em periodo).
export function getDisciplinePeriod(discipline) {
  const key = periodKeyOf(discipline)

  if (!key) {
    return null
  }

  const [year, semester] = key.split('.').map(Number)

  return { year, semester, value: key }
}

// Períodos das disciplinas, sem repetição, do mais recente para o mais antigo.
export function periodsOf(disciplines) {
  const periods = disciplines
    .map(getDisciplinePeriod)
    .filter(item => item !== null)

  const uniquePeriods = [
    ...new Map(
      periods.map(item => [item.value, item])
    ).values()
  ]

  return uniquePeriods.sort((a, b) => {
    if (a.year !== b.year) {
      return b.year - a.year
    }

    return b.semester - a.semester
  })
}

export function filterDisciplinesByPeriod(disciplines, period) {
  if (!period) {
    return disciplines
  }

  return disciplines.filter(discipline => getDisciplinePeriod(discipline)?.value === period)
}

// Notas lançadas no formato da tela, da mais recente para a mais antiga.
export function notesFromGrades(grades) {
  return grades
    .map(grade => ({
      id: grade.id,
      name: grade.assessmentName,
      value: Number(grade.score),
      recordedAt: grade.recordedAt,
      activityId: grade.activityId ?? null
    }))
    .sort((a, b) => {
      return (
        new Date(b.recordedAt).getTime()
        -
        new Date(a.recordedAt).getTime()
      )
    })
}

// Média atual das notas lançadas (0 quando não há nenhuma).
export function currentAverageOf(notes) {
  if (notes.length === 0) {
    return 0
  }

  const total = notes.reduce(
    (sum, note) => sum + Number(note.value),
    0
  )

  return total / notes.length
}

export function linkedActivityLabel(note, activities) {
  if (!note.activityId) {
    return 'Sem avaliação vinculada'
  }

  const activity = activities.find(item => item.id === note.activityId)

  return activity ? `Vinculada a ${activity.title}` : 'Avaliação vinculada'
}

export function validateDesiredAverage(value, maxGrade) {
  if (value === '' || value === null || Number.isNaN(Number(value))) {
    return 'Informe a média desejada.'
  }

  if (Number(value) < 0) {
    return 'A média desejada não pode ser negativa.'
  }

  if (Number(value) > maxGrade) {
    return 'A média desejada deve ser de no máximo 10.'
  }

  return ''
}

export function simulationStorageKey(dashboardId, disciplineId) {
  return `studdy:simulator:${dashboardId}:${disciplineId}`
}

export const UNREACHABLE_LABEL = 'Inalcançável'

// Nota necessária para exibição: nunca mostra valor acima da nota máxima.
export function requiredScoreLabel(result, maxGrade = MAX_AVERAGE) {
  if (!result) return '—'
  if (result.status === 'ALREADY_REACHED') return 'Meta já alcançada'
  const required = Number(result.requiredScoreRaw)
  if (result.status === 'IMPOSSIBLE' || (Number.isFinite(required) && required > maxGrade)) {
    return UNREACHABLE_LABEL
  }
  return formatScore(result.requiredScoreRaw)
}

// Mensagem exibida quando a meta não pode ser alcançada na próxima avaliação.
export function impossibleMessage(result) {
  const base = 'Essa média não é alcançável na próxima avaliação.'
  const max = result?.maxAchievableAverage
  if (max === null || max === undefined || max === '' || !Number.isFinite(Number(max))) return base
  return `${base} Mesmo tirando 10, sua média chega a ${formatAverage(max)}.`
}

// Rótulo de uma nota necessária de cenário: acima da nota máxima é inalcançável.
export function scenarioRequiredLabel(value, maxGrade = MAX_AVERAGE) {
  const number = Number(value)
  if (!Number.isFinite(number)) return '—'
  return number > maxGrade ? UNREACHABLE_LABEL : formatScore(number)
}
