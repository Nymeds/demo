// Rotas de /api/v1/settings, usando o cliente HTTP compartilhado.
import { createApiClient, SessionExpiredError } from '../../api/apiClient.js'

export { SessionExpiredError }

export const START_SECTIONS = Object.freeze([
  { value: 'DASHBOARD', section: 'dashboard', label: 'Dashboard' },
  { value: 'DISCIPLINES', section: 'disciplines', label: 'Disciplinas' },
  { value: 'ACTIVITIES', section: 'activities', label: 'Atividades' },
  { value: 'GRADES', section: 'grades', label: 'Notas' },
  { value: 'SIMULATOR', section: 'simulator', label: 'Simulador de Notas' },
])

export function sectionFromPreference(startSection) {
  return START_SECTIONS.find(option => option.value === startSection)?.section || 'dashboard'
}

export function createSettingsApi(getAccessToken) {
  const client = createApiClient(getAccessToken)

  function request(path, options) {
    return client.request(`/api/v1/settings${path}`, options)
  }

  function uploadAvatar(image) {
    const formData = new FormData()
    formData.append('file', image, 'foto-de-perfil.jpg')
    return request('/avatar', { method: 'POST', formData })
  }

  return Object.freeze({
    getProfile: () => request('/profile'),
    updateProfile: payload => request('/profile', { method: 'PUT', body: payload }),
    changePassword: payload => request('/password', { method: 'PUT', body: payload }),
    getPreferences: () => request('/preferences'),
    updatePreferences: payload => request('/preferences', { method: 'PUT', body: payload }),
    deleteAccount: payload => request('/account', { method: 'DELETE', body: payload }),
    getAvatar: () => request('/avatar', { as: 'blob' }),
    uploadAvatar,
    deleteAvatar: () => request('/avatar', { method: 'DELETE' }),
  })
}

export function formatLongDate(isoDate) {
  if (!isoDate) return ''

  return new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: 'long', year: 'numeric' })
    .format(new Date(isoDate))
}
