export const LOSS_PER_ABSENCE = 5
export const ATTENTION_MARGIN = 10

export function attendanceAfterAbsences(absences) {
  return Math.max(0, 100 - absences * LOSS_PER_ABSENCE)
}

export function maximumAbsencesFor(minimumPercentage) {
  return Math.floor((100 - minimumPercentage) / LOSS_PER_ABSENCE)
}

export function frequencySituation(attendance, minimum, remainingAbsences) {
  if (attendance < minimum) return 'bad'
  if (remainingAbsences <= 1 || attendance < minimum + ATTENTION_MARGIN) return 'warning'
  return 'good'
}
