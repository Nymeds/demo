export const MIN_AVERAGE = 0
export const MAX_AVERAGE = 10

// Valida o cenário lido do localStorage: dados corrompidos ou fora de 0..10 são descartados.
export function parseSavedScenario(raw) {
  if (!raw) return null
  try {
    const data = JSON.parse(raw)
    const value = data?.desiredAverage
    if (typeof value !== 'number' || !Number.isFinite(value)) return null
    if (value < MIN_AVERAGE || value > MAX_AVERAGE) return null
    return { desiredAverage: value }
  } catch {
    return null
  }
}

// Média projetada caso a próxima nota seja `nextGrade`. Nota inválida resulta em null.
export function projectedAverage(noteValues, nextGrade) {
  const grade = Number(nextGrade)
  if (nextGrade === null || nextGrade === '' || !Number.isFinite(grade)) return null
  const values = noteValues.map(Number)
  if (values.some(value => !Number.isFinite(value))) return null
  return (values.reduce((sum, value) => sum + value, 0) + grade) / (values.length + 1)
}
