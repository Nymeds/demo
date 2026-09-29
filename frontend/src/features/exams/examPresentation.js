import { isOverdue as isDateOverdue, parseLocalDate } from '../../shared/date/localDate.js'

const COLOR_PALETTE = ['purple', 'green', 'orange', 'blue']

export function colorForDiscipline(disciplines, disciplineId) {
  const index = disciplines.findIndex(discipline => discipline.id === disciplineId)
  if (index < 0) return 'purple'
  return COLOR_PALETTE[index % COLOR_PALETTE.length]
}

export function disciplineLabel(disciplines, discipline) {
  if (!discipline) return 'Disciplina'

  const hasHomonym = disciplines.some(other => (
    other.id !== discipline.id && other.name === discipline.name
  ))

  if (!hasHomonym) return discipline.name

  const distinguisher = discipline.professorName || discipline.periodo || discipline.semester
  return distinguisher ? `${discipline.name} (${distinguisher})` : discipline.name
}

export function normalizeExam(exam, disciplines) {
  const discipline = disciplines.find(item => item.id === exam.disciplineId)

  return {
    ...exam,
    disciplineName: disciplineLabel(disciplines, discipline),
    color: colorForDiscipline(disciplines, exam.disciplineId),
  }
}

export function formatDate(dateString) {
  const date = parseLocalDate(dateString)
  if (!date) return '—'
  return new Intl.DateTimeFormat('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  }).format(date)
}

export function formatShortDate(dateString) {
  const date = parseLocalDate(dateString)
  if (!date) return '—'
  return new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: '2-digit' }).format(date)
}

export function formatWeekday(dateString) {
  const date = parseLocalDate(dateString)
  if (!date) return '—'
  return new Intl.DateTimeFormat('pt-BR', { weekday: 'long' }).format(date)
}

export function formatMonthLabel(date) {
  return new Intl.DateTimeFormat('pt-BR', {
    month: 'long',
    year: 'numeric',
  }).format(date).replace(/^./, value => value.toUpperCase())
}

export function isOverdue(exam, now) {
  if (exam.status === 'COMPLETED') return false
  return isDateOverdue(exam.dueDate, now)
}

export function examState(exam, now) {
  if (exam.status === 'COMPLETED') return 'completed'
  if (isOverdue(exam, now)) return 'overdue'
  return 'scheduled'
}

export function statusLabel(exam, now) {
  const state = examState(exam, now)
  if (state === 'completed') return 'Concluída'
  if (state === 'overdue') return 'Atrasada'
  return 'Agendada'
}

export function statusClass(exam, now) {
  const state = examState(exam, now)
  return state === 'completed' ? 'is-completed' : state === 'overdue' ? 'is-overdue' : 'is-scheduled'
}

export function buildCalendarDays(exams, viewedMonthDate, today, isViewingCurrentMonth, now) {
  const year = viewedMonthDate.getFullYear()
  const month = viewedMonthDate.getMonth()
  const firstDay = new Date(year, month, 1).getDay()
  const daysInMonth = new Date(year, month + 1, 0).getDate()

  return Array.from({ length: firstDay + daysInMonth }, (_, index) => {
    if (index < firstDay) {
      return { number: '', key: `empty-${index}`, state: '' }
    }

    const number = index - firstDay + 1
    const iso = `${year}-${String(month + 1).padStart(2, '0')}-${String(number).padStart(2, '0')}`
    const dayExams = exams.filter(item => item.dueDate === iso)
    const isToday = isViewingCurrentMonth && number === today.getDate()
    const hasOverdue = dayExams.some(item => isOverdue(item, now))
    const hasScheduled = dayExams.some(item => item.status !== 'COMPLETED' && !isOverdue(item, now))
    const hasCompleted = dayExams.some(item => item.status === 'COMPLETED')

    const state = isToday
      ? 'today'
      : hasOverdue
        ? 'overdue'
        : hasScheduled
          ? 'upcoming'
          : hasCompleted
            ? 'completed'
            : ''

    return { number, key: iso, state }
  })
}
