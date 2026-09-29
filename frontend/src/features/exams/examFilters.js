import { daysBetween, parseLocalDate } from '../../shared/date/localDate.js'

export const NEXT_DAYS_WINDOW = 7

export function isWithinNextDays(dateString, days, today) {
  const date = parseLocalDate(dateString)
  if (!date) return false
  const diff = daysBetween(today, date)
  return diff >= 0 && diff <= days
}

export function isInMonthOf(dateString, reference) {
  const date = parseLocalDate(dateString)
  return Boolean(date)
    && date.getMonth() === reference.getMonth()
    && date.getFullYear() === reference.getFullYear()
}

export function filterExams(exams, { search = '', discipline = 'all', status = 'all', period = 'all', today }) {
  const term = search.trim().toLowerCase()

  return exams.filter(exam => {
    const matchesSearch = !term
      || (exam.title || '').toLowerCase().includes(term)
      || (exam.disciplineName || '').toLowerCase().includes(term)
      || (exam.description || '').toLowerCase().includes(term)
    const matchesDiscipline = discipline === 'all' || exam.disciplineId === discipline
    const matchesStatus = status === 'all'
      || (status === 'completed' && exam.status === 'COMPLETED')
      || (status === 'scheduled' && exam.status !== 'COMPLETED')
    const matchesPeriod = period === 'all'
      || (period === 'month' && isInMonthOf(exam.dueDate, today))
      || (period === 'next7' && isWithinNextDays(exam.dueDate, NEXT_DAYS_WINDOW, today))
    return matchesSearch && matchesDiscipline && matchesStatus && matchesPeriod
  }).sort((a, b) => a.dueDate.localeCompare(b.dueDate))
}
