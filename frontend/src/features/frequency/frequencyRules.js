export const LOSS_PER_ABSENCE = 5

export function attendanceAfterAbsences(absences) {
  return Math.max(0, 100 - absences * LOSS_PER_ABSENCE)
}

export function maximumAbsencesFor(minimumPercentage) {
  return Math.floor((100 - minimumPercentage) / LOSS_PER_ABSENCE)
}
