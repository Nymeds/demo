// Padrão único de exibição de notas no frontend:
//  - médias (geral, da disciplina, projetada, meta): 1 casa decimal (formatAverage)
//  - nota necessária / notas individuais: 2 casas decimais, como a API (formatScore)
// Valores inválidos aparecem como "—", nunca como "0,00".

function toFiniteNumber(value) {
  if (value === null || value === undefined || value === '') return null
  const number = Number(value)
  return Number.isFinite(number) ? number : null
}

// Arredonda para 1 casa decimal (meio para cima; 6.95 -> 7.0). Único ponto de arredondamento das médias.
export function roundGrade(value) {
  const number = toFiniteNumber(value)
  if (number === null) return null
  return Math.round((number + Number.EPSILON) * 10) / 10
}

export function formatAverage(value) {
  const rounded = roundGrade(value)
  if (rounded === null) return '—'
  return rounded.toLocaleString('pt-BR', { minimumFractionDigits: 1, maximumFractionDigits: 1 })
}

export function formatScore(value) {
  const number = toFiniteNumber(value)
  if (number === null) return '—'
  return number.toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
