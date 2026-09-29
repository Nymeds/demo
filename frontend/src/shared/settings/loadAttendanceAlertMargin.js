import { loadPreferences } from './preferences.js'

// Mantido para telas que só precisam da margem; prefira loadPreferences.
export async function loadAttendanceAlertMargin(apiRequest) {
  return (await loadPreferences(apiRequest)).attendanceAlertMargin
}
