// Carregamento único das preferências do estudante (GET /api/v1/settings/preferences).
export const ATTENTION_MARGIN = 10
export const DEFAULT_DEADLINE_ALERT_DAYS = 3
export const DEFAULT_START_SECTION = 'DASHBOARD'

// null/undefined/'' significam "ausente" (Number(null) seria 0, um valor válido e enganoso).
function numberOrDefault(value, fallback) {
  if (value === null || value === undefined || value === '') return fallback
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : fallback
}

export function normalizePreferences(raw) {
  const source = raw && typeof raw === 'object' ? raw : {}

  return {
    attendanceAlertMargin: numberOrDefault(source.attendanceAlertMargin, ATTENTION_MARGIN),
    deadlineAlertDays: numberOrDefault(source.deadlineAlertDays, DEFAULT_DEADLINE_ALERT_DAYS),
    startSection: typeof source.startSection === 'string' && source.startSection
      ? source.startSection
      : DEFAULT_START_SECTION,
  }
}

// Nunca lança: em falha devolve os padrões e `loaded: false`.
export async function loadPreferences(apiRequest) {
  try {
    const preferences = await apiRequest('/api/v1/settings/preferences')
    return { ...normalizePreferences(preferences), loaded: true }
  } catch {
    return { ...normalizePreferences(null), loaded: false }
  }
}
