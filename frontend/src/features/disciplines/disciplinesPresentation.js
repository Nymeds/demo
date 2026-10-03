import { frequencySituation } from '../frequency/frequencyRules.js'

// Regras de apresentação da tela Disciplinas: normalização, filtro/ordenação, médias e situações.

export const DEFAULT_COLOR = '#6432df'
export const MINIMUM_ATTENDANCE = 75

export const FILTERS = Object.freeze([
  { value: 'all', label: 'Todas' },
  { value: 'active', label: 'Em andamento' },
  { value: 'finished', label: 'Concluídas' },
  { value: 'locked', label: 'Trancadas' },
])

export const SORT_OPTIONS = Object.freeze([
  { value: 'nameAsc', label: 'Nome (A-Z)' },
  { value: 'nameDesc', label: 'Nome (Z-A)' },
  { value: 'newest', label: 'Mais recentes' },
])

export const STATUS_OPTIONS = Object.freeze([
  { value: 'IN_PROGRESS', label: 'Em andamento', className: 'is-progress' },
  { value: 'COMPLETED', label: 'Concluída', className: 'is-success' },
  { value: 'LOCKED', label: 'Trancada', className: 'is-neutral' },
])

export const DAY_LABELS = Object.freeze({
  MONDAY: 'Seg',
  TUESDAY: 'Ter',
  WEDNESDAY: 'Qua',
  THURSDAY: 'Qui',
  FRIDAY: 'Sex',
  SATURDAY: 'Sáb',
  SUNDAY: 'Dom',
})

const STATUS_FILTER = Object.freeze({
  active: 'IN_PROGRESS',
  finished: 'COMPLETED',
  locked: 'LOCKED',
})

export const STATUS_MENU_WIDTH = 190
export const STATUS_MENU_HEIGHT = 156

export function normalizeDiscipline(discipline) {
  return {
    ...discipline,
    color: discipline.color || DEFAULT_COLOR,
    status: discipline.status || 'IN_PROGRESS',
    schedules: discipline.schedules.map(schedule => ({
      ...schedule,
      startTime: schedule.startTime.slice(0, 5),
      endTime: schedule.endTime.slice(0, 5),
    })),
  }
}

export function filterAndSortDisciplines(disciplines, { searchTerm, activeFilter, sortOrder }) {
  const search = searchTerm.trim().toLocaleLowerCase('pt-BR')

  return disciplines
    .filter(discipline => {
      const matchesSearch = !search
        || discipline.name.toLocaleLowerCase('pt-BR').includes(search)
        || (discipline.professorName ?? '').toLocaleLowerCase('pt-BR').includes(search)

      const matchesFilter = activeFilter === 'all'
        || discipline.status === STATUS_FILTER[activeFilter]

      return matchesSearch && matchesFilter
    })
    .sort((first, second) => {
      if (sortOrder === 'nameDesc') {
        return second.name.localeCompare(first.name, 'pt-BR')
      }

      if (sortOrder === 'newest') {
        return new Date(second.createdAt ?? 0).getTime() - new Date(first.createdAt ?? 0).getTime()
      }

      return first.name.localeCompare(second.name, 'pt-BR')
    })
}

// Média simples dos valores numéricos do campo; null quando nenhuma disciplina tem o dado.
export function averageOf(disciplines, field) {
  const values = disciplines
    .map(discipline => discipline[field])
    .filter(value => typeof value === 'number')

  return values.length
    ? values.reduce((total, value) => total + value, 0) / values.length
    : null
}

export function disciplineColor(discipline) {
  return discipline.color || DEFAULT_COLOR
}

export function attendanceLabel(value) {
  return typeof value === 'number' ? `${Math.round(value)}%` : 'Sem frequência cadastrada'
}

export function attendanceSituation(attendance, minimum, remainingAbsences, margin) {
  if (typeof attendance !== 'number') return 'neutral'
  return frequencySituation(attendance, Number(minimum ?? MINIMUM_ATTENDANCE), remainingAbsences, margin)
}

export function disciplineAttendanceSituation(discipline, margin) {
  const maximumAbsences = Number(discipline.maximumAbsences)
  const absences = Number(discipline.absences ?? 0)
  const remainingAbsences = Number.isFinite(maximumAbsences)
    ? maximumAbsences - absences
    : Number.POSITIVE_INFINITY

  return attendanceSituation(
    discipline.attendancePercentage,
    discipline.minimumAttendancePercentage,
    remainingAbsences,
    margin,
  )
}

export function statusDetails(status) {
  return STATUS_OPTIONS.find(option => option.value === status) ?? STATUS_OPTIONS[0]
}

// Posição do menu de situação: alinhado à direita do gatilho, abaixo dele quando couber.
export function statusMenuPosition(bounds, viewport) {
  const left = Math.max(12, Math.min(bounds.right - STATUS_MENU_WIDTH, viewport.width - STATUS_MENU_WIDTH - 12))
  const spaceBelow = viewport.height - bounds.bottom
  const top = spaceBelow >= STATUS_MENU_HEIGHT + 12
    ? bounds.bottom + 7
    : Math.max(12, bounds.top - STATUS_MENU_HEIGHT - 7)

  return { left, top }
}
