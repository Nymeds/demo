import { frequencySituation } from '../frequency/frequencyRules.js'

export const REMINDER_LIMIT = 8
export const REMINDER_OVERDUE_SHARE = 4

const weekDayNumbers = {
  SUNDAY: 0,
  MONDAY: 1,
  TUESDAY: 2,
  WEDNESDAY: 3,
  THURSDAY: 4,
  FRIDAY: 5,
  SATURDAY: 6,
}

const weekDayLabels = {
  SUNDAY: 'Dom',
  MONDAY: 'Seg',
  TUESDAY: 'Ter',
  WEDNESDAY: 'Qua',
  THURSDAY: 'Qui',
  FRIDAY: 'Sex',
  SATURDAY: 'Sáb',
}

export function firstNameOf(name) {
  const rawName = name?.trim() || 'estudante'
  const firstPart = rawName.includes('@') ? rawName.split('@')[0] : rawName.split(/\s+/)[0]
  return firstPart.charAt(0).toUpperCase() + firstPart.slice(1)
}

// Média simples dos valores numéricos de uma propriedade; null quando nenhuma disciplina tem o dado.
export function averageOf(disciplines, key) {
  const values = disciplines.map(discipline => discipline[key]).filter(value => typeof value === 'number')
  return values.length ? values.reduce((total, value) => total + value, 0) / values.length : null
}

// Atividades sem prazo não são atrasadas e vão para o fim das listas ordenadas por data.
export function dueDateOf(activity) {
  return typeof activity?.dueDate === 'string' ? activity.dueDate : ''
}

export function compareByDueDate(first, second) {
  const firstDate = dueDateOf(first)
  const secondDate = dueDateOf(second)
  if (!firstDate || !secondDate) return Number(!firstDate) - Number(!secondDate)
  return firstDate.localeCompare(secondDate)
}

export function isOverdue(activity, todayIso) {
  const dueDate = dueDateOf(activity)
  return Boolean(dueDate) && dueDate < todayIso
}

export function activityStatus(activity, todayIso) {
  if (activity.status === 'COMPLETED') return { label: 'Concluída', className: 'is-completed' }
  if (isOverdue(activity, todayIso)) return { label: 'Atrasada', className: 'is-overdue' }
  if (activity.status === 'IN_PROGRESS') return { label: 'Em andamento', className: 'is-progress' }
  return { label: 'Pendente', className: 'is-pending' }
}

export function formatAverage(value) {
  return value === null ? '—' : value.toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

export function formatCompactDate(date) {
  if (!date) return '—'
  return new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: '2-digit' })
    .format(new Date(`${date}T12:00:00`))
}

export function reminderSectionFor(activity) {
  return activity.type === 'EXAM' ? 'exams' : 'activities'
}

export function reminderKindLabel(activity) {
  return activity.type === 'EXAM' ? 'Prova' : 'Atividade'
}

// Data limite (AAAA-MM-DD) da antecedência configurada em Preferências, a partir de agora.
export function reminderLimitIsoOf(now, deadlineAlertDays) {
  const limit = new Date(now)
  limit.setDate(limit.getDate() + deadlineAlertDays)
  const year = limit.getFullYear()
  const month = String(limit.getMonth() + 1).padStart(2, '0')
  const day = String(limit.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

export function upcomingRemindersOf(activities, todayIso, limitIso) {
  return activities
    .filter(activity => activity.status !== 'COMPLETED'
      && dueDateOf(activity)
      && !isOverdue(activity, todayIso)
      && dueDateOf(activity) <= limitIso)
    .sort(compareByDueDate)
}

export function overdueRemindersOf(activities, todayIso) {
  return activities
    .filter(activity => activity.status !== 'COMPLETED' && isOverdue(activity, todayIso))
    .sort(compareByDueDate)
}

export function visibleReminderGroupsOf(overdue, upcoming) {
  const overdueCount = upcoming.length
    ? Math.min(overdue.length, REMINDER_OVERDUE_SHARE)
    : Math.min(overdue.length, REMINDER_LIMIT)
  const upcomingCount = Math.min(upcoming.length, REMINDER_LIMIT - overdueCount)

  return [
    { key: 'overdue', label: 'Atrasadas', items: overdue.slice(0, overdueCount) },
    { key: 'upcoming', label: 'Próximos prazos', items: upcoming.slice(0, upcomingCount) },
  ].filter(group => group.items.length > 0)
}

// Próxima aula a partir de agora, considerando todos os horários semanais das disciplinas.
export function nextClassOf(disciplines, now) {
  return disciplines
    .flatMap(discipline => (discipline.schedules || []).map((schedule, scheduleIndex) => {
      const dayNumber = weekDayNumbers[schedule.dayOfWeek]
      const [hours, minutes] = String(schedule.startTime || '').split(':').map(Number)

      if (dayNumber === undefined || !Number.isInteger(hours) || !Number.isInteger(minutes)) return null

      const startsAt = new Date(now)
      const daysUntilClass = (dayNumber - startsAt.getDay() + 7) % 7
      startsAt.setDate(startsAt.getDate() + daysUntilClass)
      startsAt.setHours(hours, minutes, 0, 0)

      if (startsAt <= now) startsAt.setDate(startsAt.getDate() + 7)

      return {
        id: `${discipline.id}-${scheduleIndex}`,
        discipline,
        schedule,
        startsAt,
      }
    }))
    .filter(Boolean)
    .sort((first, second) => first.startsAt - second.startsAt)
    .at(0) || null
}

export function formatClassSchedule(upcomingClass, now) {
  const { schedule, startsAt } = upcomingClass
  const todayStart = new Date(now)
  todayStart.setHours(0, 0, 0, 0)
  const classStart = new Date(startsAt)
  classStart.setHours(0, 0, 0, 0)
  const daysUntilClass = Math.round((classStart - todayStart) / 86400000)
  const dayLabel = daysUntilClass === 0
    ? 'Hoje'
    : daysUntilClass === 1
      ? 'Amanhã'
      : weekDayLabels[schedule.dayOfWeek]
  const startTime = String(schedule.startTime).slice(0, 5)
  const endTime = String(schedule.endTime).slice(0, 5)

  return `${dayLabel} ${startTime}–${endTime}`
}

export function classDateTime(upcomingClass) {
  const date = upcomingClass.startsAt
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}T${String(upcomingClass.schedule.startTime).slice(0, 5)}`
}

// Detalhes exibidos no painel de frequência; a margem de alerta vem de Preferências.
export function frequencyDetailsOf(discipline, attendanceAlertMargin) {
  if (!discipline) return null

  const hasAttendance = typeof discipline.attendancePercentage === 'number'
  const absences = Math.max(0, Number(discipline.absences ?? 0))
  const lossPerAbsence = Number(discipline.lossPerAbsence ?? 5)
  const minimum = Number(discipline.minimumAttendancePercentage ?? 75)
  const maximumAbsences = Math.max(0, Number(
    discipline.maximumAbsences ?? Math.floor((100 - minimum) / lossPerAbsence),
  ))
  const remainingAbsences = Math.max(0, maximumAbsences - absences)

  if (!hasAttendance) {
    return {
      discipline,
      attendance: null,
      absences,
      lossPerAbsence,
      minimum,
      maximumAbsences,
      remainingAbsences,
      message: 'Sem frequência cadastrada para esta disciplina.',
      messageClass: 'is-neutral',
      ringColor: 'var(--frequency-ring-neutral)',
    }
  }

  const attendance = Math.max(0, Math.min(100, Number(discipline.attendancePercentage)))
  const situation = frequencySituation(attendance, minimum, maximumAbsences - absences, attendanceAlertMargin)

  if (situation === 'bad') {
    return {
      discipline,
      attendance,
      absences,
      lossPerAbsence,
      minimum,
      maximumAbsences,
      remainingAbsences,
      message: 'Sua frequência está abaixo do mínimo exigido para aprovação.',
      messageClass: 'is-danger',
      ringColor: '#e04433',
    }
  }

  if (situation === 'warning') {
    return {
      discipline,
      attendance,
      absences,
      lossPerAbsence,
      minimum,
      maximumAbsences,
      remainingAbsences,
      message: remainingAbsences === 0
        ? 'Você está no limite mínimo. Uma nova falta deixará a frequência abaixo do exigido.'
        : 'Atenção: resta apenas uma falta antes de atingir o limite mínimo.',
      messageClass: 'is-warning',
      ringColor: '#f0951f',
    }
  }

  return {
    discipline,
    attendance,
    absences,
    lossPerAbsence,
    minimum,
    maximumAbsences,
    remainingAbsences,
    message: `Você está acima do mínimo exigido e ainda pode registrar ${remainingAbsences} ${remainingAbsences === 1 ? 'falta' : 'faltas'}.`,
    messageClass: 'is-success',
    ringColor: '#20aa60',
  }
}

export function disciplineNameOf(disciplines, disciplineId) {
  return disciplines.find(discipline => discipline.id === disciplineId)?.name || 'Disciplina'
}

export function disciplineColorOf(disciplines, disciplineId) {
  return disciplines.find(discipline => discipline.id === disciplineId)?.color || '#6631db'
}
