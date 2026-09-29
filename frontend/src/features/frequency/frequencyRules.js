import { ATTENTION_MARGIN } from '../../shared/settings/preferences.js'

export const LOSS_PER_ABSENCE = 5
export { ATTENTION_MARGIN }

export function attendanceAfterAbsences(absences) {
  return Math.max(0, 100 - absences * LOSS_PER_ABSENCE)
}

export function maximumAbsencesFor(minimumPercentage) {
  return Math.floor((100 - minimumPercentage) / LOSS_PER_ABSENCE)
}

// `margin` é a margem do aviso de frequência configurada em Preferências
// (settings.attendanceAlertMargin). Quando não informada, indisponível ou
// inválida, usa-se o valor fixo ATTENTION_MARGIN como alternativa.
export function frequencySituation(attendance, minimum, remainingAbsences, margin = ATTENTION_MARGIN) {
  const effectiveMargin = Number.isFinite(margin) ? margin : ATTENTION_MARGIN

  if (attendance < minimum) return 'bad'
  if (remainingAbsences <= 1 || attendance < minimum + effectiveMargin) return 'warning'
  return 'good'
}
