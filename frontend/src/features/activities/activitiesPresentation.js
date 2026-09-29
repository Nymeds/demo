import { isOverdue as isDateOverdue, parseLocalDate } from '../../shared/date/localDate.js'

export const sortOptions = [
  { value: 'dueAsc', label: 'Prazo mais próximo' },
  { value: 'dueDesc', label: 'Prazo mais distante' },
  { value: 'titleAsc', label: 'Título A–Z' },
  { value: 'titleDesc', label: 'Título Z–A' },
]

export const filters = [
  { value: 'all', label: 'Todas' },
  { value: 'pending', label: 'Pendentes' },
  { value: 'progress', label: 'Em andamento' },
  { value: 'completed', label: 'Concluídas' },
]

export function normalizeActivity(activity, disciplines) {
  const discipline = disciplines.find(item => item.id === activity.disciplineId)

  return {
    ...activity,
    disciplineName: discipline?.name || 'Disciplina',
    disciplineColor: discipline?.color || '#6432df',
  }
}

export function filterActivities(activities, searchTerm, activeFilter, sortOrder) {
  const search = searchTerm.trim().toLocaleLowerCase('pt-BR')

  return activities
    .filter(activity => {
      const matchesSearch = !search
        || activity.title.toLocaleLowerCase('pt-BR').includes(search)
        || (activity.description || '').toLocaleLowerCase('pt-BR').includes(search)
        || activity.disciplineName.toLocaleLowerCase('pt-BR').includes(search)

      const matchesFilter = activeFilter === 'all'
        || (activeFilter === 'pending' && activity.status === 'PENDING')
        || (activeFilter === 'progress' && activity.status === 'IN_PROGRESS')
        || (activeFilter === 'completed' && activity.status === 'COMPLETED')

      return matchesSearch && matchesFilter
    })
    .sort((first, second) => {
      if (sortOrder === 'dueDesc') {
        return second.dueDate.localeCompare(first.dueDate)
      }

      if (sortOrder === 'titleAsc') {
        return first.title.localeCompare(second.title, 'pt-BR')
      }

      if (sortOrder === 'titleDesc') {
        return second.title.localeCompare(first.title, 'pt-BR')
      }

      return first.dueDate.localeCompare(second.dueDate)
    })
}

export function countByStatus(activities, status) {
  return activities.filter(activity => activity.status === status).length
}

export function statusDetails(status) {
  const statuses = {
    PENDING: { label: 'Pendente', className: 'is-pending' },
    IN_PROGRESS: { label: 'Em andamento', className: 'is-progress' },
    COMPLETED: { label: 'Concluída', className: 'is-completed' },
  }

  return statuses[status] || statuses.PENDING
}

export function formatDate(value) {
  const date = parseLocalDate(value)
  if (!date) return '—'

  return new Intl.DateTimeFormat('pt-BR', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  }).format(date)
}

export function isOverdue(activity) {
  if (activity.status === 'COMPLETED' || !activity.dueDate) return false

  return isDateOverdue(activity.dueDate)
}
