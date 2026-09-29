// Rotas de /api/v1/settings, usando o cliente HTTP compartilhado.
import { createApiClient, SessionExpiredError } from '../../api/apiClient.js'

export { SessionExpiredError }

export const START_SECTIONS = Object.freeze([
  { value: 'DASHBOARD', section: 'dashboard', label: 'Dashboard' },
  { value: 'DISCIPLINES', section: 'disciplines', label: 'Disciplinas' },
  { value: 'ACTIVITIES', section: 'activities', label: 'Atividades' },
  { value: 'EXAMS', section: 'exams', label: 'Provas' },
  { value: 'FREQUENCY', section: 'frequency', label: 'Frequência' },
  { value: 'GRADES', section: 'grades', label: 'Notas' },
  { value: 'SIMULATOR', section: 'simulator', label: 'Simulador de Notas' },
  { value: 'CALENDAR', section: 'calendar', label: 'Calendário' },
])

export function sectionFromPreference(startSection) {
  return START_SECTIONS.find(option => option.value === startSection)?.section || 'dashboard'
}

export function createSettingsApi() {
  const client = createApiClient()

  function request(path, options) {
    return client.request(`/api/v1/settings${path}`, options)
  }

  return Object.freeze({
    getProfile: () => request('/profile'),
    changePassword: payload => request('/password', { method: 'PUT', body: payload }),
    getPreferences: () => request('/preferences'),
    updatePreferences: payload => request('/preferences', { method: 'PUT', body: payload }),
    deleteAccount: payload => request('/account', { method: 'DELETE', body: payload }),
  })
}

export function formatLongDate(isoDate) {
  if (!isoDate) return ''

  return new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: 'long', year: 'numeric' })
    .format(new Date(isoDate))
}
