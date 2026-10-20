// Escolha determinística do dashboard ATIVO do estudante.
//
// Regras (item 4 do plano de frontend):
// - O backend garante no máximo um dashboard ACTIVE por usuário, mas esta função
//   também cobre o caso legado de haver mais de um: entre os ACTIVE, escolhe o
//   mais recentemente atualizado/criado; sem esses campos, cai para o maior id.
// - Se não houver nenhum dashboard ACTIVE, usa o mesmo critério sobre todos os
//   dashboards retornados (mantendo compatibilidade com o comportamento anterior
//   de cada tela, que usava `dashboards[0]` como alternativa).
// - Se não houver nenhum dashboard, retorna null e cada tela decide o que fazer
//   (por exemplo, criar automaticamente um dashboard, como já faziam antes).

function timestampOf(dashboard) {
  const raw = dashboard?.updatedAt ?? dashboard?.createdAt ?? null
  const parsed = raw ? Date.parse(raw) : NaN
  return Number.isNaN(parsed) ? null : parsed
}

function compareByRecencyThenId(a, b) {
  const aTime = timestampOf(a)
  const bTime = timestampOf(b)

  if (aTime !== null && bTime !== null && aTime !== bTime) {
    return bTime - aTime
  }
  if (aTime !== null && bTime === null) return -1
  if (aTime === null && bTime !== null) return 1

  // Sem datas utilizáveis (ou empatadas): desempata pelo maior id.
  return String(b?.id ?? '').localeCompare(String(a?.id ?? ''), undefined, { numeric: true })
}

/**
 * Recebe a lista de dashboards (resposta de GET /api/v1/dashboards) e
 * devolve o dashboard ativo escolhido de forma determinística, ou null
 * quando a lista está vazia.
 */
export function resolveActiveDashboard(dashboards) {
  if (!Array.isArray(dashboards) || dashboards.length === 0) return null

  const activeDashboards = dashboards.filter(dashboard => dashboard?.status === 'ACTIVE')
  const pool = activeDashboards.length > 0 ? activeDashboards : dashboards

  return [...pool].sort(compareByRecencyThenId)[0] ?? null
}

/**
 * Busca /api/v1/dashboards usando a função `request` fornecida pela tela
 * (cada tela mantém seu próprio mecanismo de autenticação/erro) e devolve
 * o dashboard ativo já resolvido por `resolveActiveDashboard`.
 */
export async function loadActiveDashboard(request) {
  const dashboards = await request('/api/v1/dashboards')
  return resolveActiveDashboard(dashboards)
}
