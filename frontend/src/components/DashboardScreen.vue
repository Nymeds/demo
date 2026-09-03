<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import ActivitiesScreen from './ActivitiesScreen.vue'
import DisciplinesEmpty from './DisciplinesEmpty.vue'

const { user, accessToken } = defineProps({
  user: { type: Object, required: true },
  accessToken: { type: String, required: true },
})

const emit = defineEmits(['logout'])
const activeSection = ref('dashboard')
const dashboardLoading = ref(true)
const dashboardError = ref('')
const disciplines = ref([])
const activities = ref([])

const firstName = computed(() => {
  const rawName = user.name?.trim() || 'estudante'
  const firstPart = rawName.includes('@') ? rawName.split('@')[0] : rawName.split(/\s+/)[0]
  return firstPart.charAt(0).toUpperCase() + firstPart.slice(1)
})
const userInitial = computed(() => firstName.value.charAt(0).toUpperCase())
const today = new Date()
const todayIso = [
  today.getFullYear(),
  String(today.getMonth() + 1).padStart(2, '0'),
  String(today.getDate()).padStart(2, '0'),
].join('-')
const todayLabel = new Intl.DateTimeFormat('pt-BR', {
  day: 'numeric',
  month: 'long',
}).format(today)

async function apiRequest(path) {
  const response = await fetch(path, { headers: { Authorization: `Bearer ${accessToken}` } })
  const data = await response.json().catch(() => ({}))

  if (!response.ok) {
    throw new Error(data.detail || data.message || 'Não foi possível carregar o dashboard.')
  }

  return data
}

async function loadDashboard() {
  dashboardLoading.value = true
  dashboardError.value = ''

  try {
    const dashboards = await apiRequest('/api/v1/dashboards')
    const dashboard = dashboards.find(item => item.status === 'ACTIVE') || dashboards[0]

    if (!dashboard) {
      disciplines.value = []
      activities.value = []
      return
    }

    disciplines.value = await apiRequest(`/api/v1/dashboards/${dashboard.id}/disciplines`)
    const activityLists = await Promise.all(disciplines.value.map(discipline =>
      apiRequest(`/api/v1/dashboards/${dashboard.id}/disciplines/${discipline.id}/activities`),
    ))
    activities.value = activityLists.flat()
  } catch (error) {
    dashboardError.value = error.message || 'Não foi possível carregar o dashboard.'
  } finally {
    dashboardLoading.value = false
  }
}

const pendingActivities = computed(() => activities.value.filter(activity => activity.status !== 'COMPLETED'))
const generalAverage = computed(() => {
  const values = disciplines.value.map(discipline => discipline.average).filter(value => typeof value === 'number')
  return values.length ? values.reduce((total, value) => total + value, 0) / values.length : null
})
const averageAttendance = computed(() => {
  const values = disciplines.value.map(discipline => discipline.attendancePercentage).filter(value => typeof value === 'number')
  return values.length ? values.reduce((total, value) => total + value, 0) / values.length : null
})
const completedActivities = computed(() => activities.value.filter(activity => activity.status === 'COMPLETED').length)
const overdueActivities = computed(() => pendingActivities.value.filter(activity => activity.dueDate < todayIso).length)
const completionPercentage = computed(() => activities.value.length
  ? Math.round((completedActivities.value / activities.value.length) * 100)
  : 0)
const dashboardActivities = computed(() => [...activities.value]
  .sort((first, second) => {
    const statusOrder = Number(first.status === 'COMPLETED') - Number(second.status === 'COMPLETED')
    return statusOrder || first.dueDate.localeCompare(second.dueDate)
  })
  .slice(0, 5))

function formatAverage(value) {
  return value === null ? '—' : value.toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatDate(date) {
  return new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: 'short' })
    .format(new Date(`${date}T12:00:00`))
}

function disciplineName(disciplineId) {
  return disciplines.value.find(discipline => discipline.id === disciplineId)?.name || 'Disciplina'
}

function activityStatus(activity) {
  if (activity.status === 'COMPLETED') return { label: 'Concluída', className: 'is-completed' }
  if (activity.dueDate < todayIso) return { label: 'Atrasada', className: 'is-overdue' }
  if (activity.status === 'IN_PROGRESS') return { label: 'Em andamento', className: 'is-progress' }
  return { label: 'Pendente', className: 'is-pending' }
}

onMounted(loadDashboard)
watch(activeSection, section => {
  if (section === 'dashboard') loadDashboard()
})
</script>
<template>
  <div class="dashboard-shell">
    <aside class="dashboard-sidebar">
      <div class="dashboard-brand">
        <span class="dashboard-brand-icon" aria-hidden="true">
          <svg viewBox="0 0 24 24">
            <path d="m3 9 9-4 9 4-9 4-9-4Z" />
            <path d="M7 11v5c3 2 7 2 10 0v-5M21 9v6" />
          </svg>
        </span>
        <span>
          <strong>AcadOrganize</strong>
          <small>Organize seus estudos</small>
        </span>
      </div>
      <nav class="dashboard-navigation" aria-label="Navegação principal">
        <button
          type="button"
          :class="{ active: activeSection === 'dashboard' }"
          :aria-current="activeSection === 'dashboard' ? 'page' : undefined"
          @click="activeSection = 'dashboard'"
        >
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="m3 11 9-8 9 8" />
            <path d="M5 10v10h14V10M9 20v-6h6v6" />
          </svg>
          Dashboard
        </button>
        <button
          type="button"
          :class="{ active: activeSection === 'disciplines' }"
          :aria-current="activeSection === 'disciplines' ? 'page' : undefined"
          @click="activeSection = 'disciplines'"
        >
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M4 5.5A3.5 3.5 0 0 1 7.5 2H11v17H7.5A3.5 3.5 0 0 0 4 22V5.5Z" />
            <path d="M20 5.5A3.5 3.5 0 0 0 16.5 2H13v17h3.5A3.5 3.5 0 0 1 20 22V5.5Z" />
          </svg>
          Disciplinas
        </button>
        <button
          type="button"
          :class="{ active: activeSection === 'activities' }"
          :aria-current="activeSection === 'activities' ? 'page' : undefined"
          @click="activeSection = 'activities'"
        >
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <rect x="5" y="4" width="14" height="17" rx="2" />
            <path d="M9 4V2m6 2V2M8 9h8m-8 4 2 2 4-4" />
          </svg>
          Atividades
        </button>
      </nav>
      <div class="dashboard-sidebar-footer">
        <div class="dashboard-user-card" :title="user.name">
          <span class="dashboard-user-avatar" aria-hidden="true">{{ userInitial }}</span>
          <span class="dashboard-user-details">
            <strong>{{ user.name }}</strong>
            <small>Usuário conectado</small>
          </span>
        </div>
        <button class="dashboard-logout" type="button" @click="emit('logout')">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M10 5H5v14h5M14 8l4 4-4 4M8 12h10" />
          </svg>
          Sair
        </button>
      </div>
    </aside>
    <main class="dashboard-main">
      <header v-if="activeSection === 'dashboard'" class="dashboard-topbar">
        <div class="dashboard-welcome">
          <span class="dashboard-welcome-label">Visão geral</span>
          <h1>Olá, {{ firstName }}! <span aria-hidden="true">👋</span></h1>
          <p>Acompanhe seus estudos e mantenha os próximos prazos sob controle.</p>
          <div class="dashboard-welcome-actions">
            <button type="button" @click="activeSection = 'activities'"><span aria-hidden="true">＋</span> Nova atividade</button>
            <button type="button" @click="activeSection = 'disciplines'">Ver disciplinas</button>
          </div>
        </div>
        <time class="dashboard-date" :datetime="todayIso">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <rect x="3" y="5" width="18" height="16" rx="2" />
            <path d="M7 3v4m10-4v4M3 10h18" />
          </svg>
          Hoje, {{ todayLabel }}
        </time>
      </header>
      <section v-if="activeSection === 'dashboard'" class="dashboard-overview" aria-labelledby="dashboard-empty-title">
        <div class="dashboard-summary-grid" aria-label="Resumo acadêmico">
          <article class="dashboard-summary-card is-purple">
            <span class="dashboard-summary-icon" aria-hidden="true">
              <svg viewBox="0 0 24 24"><path d="M4 5.5A3.5 3.5 0 0 1 7.5 2H11v17H7.5A3.5 3.5 0 0 0 4 22V5.5Z" /><path d="M20 5.5A3.5 3.5 0 0 0 16.5 2H13v17h3.5A3.5 3.5 0 0 1 20 22V5.5Z" /></svg>
            </span>
            <div><p>Disciplinas</p><strong>{{ dashboardLoading ? '—' : disciplines.length }}</strong><small>{{ disciplines.length ? 'Cadastradas' : 'Nenhuma ainda' }}</small></div>
          </article>
          <article class="dashboard-summary-card is-green">
            <span class="dashboard-summary-icon" aria-hidden="true">
              <svg viewBox="0 0 24 24"><rect x="5" y="4" width="14" height="17" rx="2" /><path d="M9 4V2m6 2V2M8 9h8m-8 4 2 2 4-4" /></svg>
            </span>
            <div><p>Atividades</p><strong>{{ dashboardLoading ? '—' : activities.length }}</strong><small>{{ activities.length ? 'Em todas as disciplinas' : 'Nenhuma ainda' }}</small></div>
          </article>
          <article class="dashboard-summary-card is-orange">
            <span class="dashboard-summary-icon" aria-hidden="true">
              <svg viewBox="0 0 24 24"><rect x="3" y="5" width="18" height="16" rx="2" /><path d="M7 3v4m10-4v4M3 10h18m5 4h4" /></svg>
            </span>
            <div><p>Pendentes</p><strong>{{ dashboardLoading ? '—' : pendingActivities.length }}</strong><small>{{ pendingActivities.length ? 'Aguardando conclusão' : 'Tudo em dia' }}</small></div>
          </article>
          <article class="dashboard-summary-card is-violet">
            <span class="dashboard-summary-icon" aria-hidden="true">
              <svg viewBox="0 0 24 24"><path d="M4 19v-5m5 5V9m5 10v-7m5 7V5" /><path d="m4 10 5-4 5 3 6-6" /></svg>
            </span>
            <div><p>Média geral</p><strong>{{ dashboardLoading ? '—' : formatAverage(generalAverage) }}</strong><small>{{ generalAverage === null ? 'Aguardando notas' : 'Das disciplinas' }}</small></div>
          </article>
          <article class="dashboard-summary-card is-blue">
            <span class="dashboard-summary-icon" aria-hidden="true">
              <svg viewBox="0 0 24 24"><path d="M12 3a9 9 0 1 1-7.8 4.5" /><path d="M12 3v9l6 4" /></svg>
            </span>
            <div><p>Frequência média</p><strong>{{ dashboardLoading || averageAttendance === null ? '—' : `${Math.round(averageAttendance)}%` }}</strong><small>{{ averageAttendance === null ? 'Aguardando frequência' : 'Das disciplinas' }}</small></div>
          </article>
        </div>
        <article v-if="dashboardLoading" class="dashboard-empty-hero">
          <h2>Atualizando seu dashboard…</h2>
          <p>Estamos reunindo suas disciplinas e atividades.</p>
        </article>
        <article v-else-if="dashboardError" class="dashboard-empty-hero">
          <h2>Não foi possível carregar o dashboard</h2>
          <p>{{ dashboardError }}</p>
          <button type="button" @click="loadDashboard">Tentar novamente</button>
        </article>
        <article v-else-if="disciplines.length === 0" class="dashboard-empty-hero">
          <svg class="dashboard-empty-illustration" viewBox="0 0 420 190" role="img" aria-label="Ilustração de um painel acadêmico vazio">
            <defs>
              <linearGradient id="window-gradient" x1="0" y1="0" x2="1" y2="1">
                <stop offset="0" stop-color="#f3efff" />
                <stop offset="1" stop-color="#e3dcff" />
              </linearGradient>
              <linearGradient id="purple-gradient" x1="0" y1="0" x2="1" y2="1">
                <stop offset="0" stop-color="#8b6cf1" />
                <stop offset="1" stop-color="#6642d8" />
              </linearGradient>
            </defs>
            <path d="M45 164h330" stroke="#e5dfff" stroke-width="3" stroke-linecap="round" />
            <rect x="112" y="25" width="205" height="137" rx="10" fill="url(#window-gradient)" stroke="#d7cefb" stroke-width="2" />
            <path d="M112 36a11 11 0 0 1 11-11h183a11 11 0 0 1 11 11v15H112V36Z" fill="url(#purple-gradient)" />
            <circle cx="128" cy="38" r="4" fill="#ddd5ff" /><circle cx="141" cy="38" r="4" fill="#ddd5ff" /><circle cx="154" cy="38" r="4" fill="#ddd5ff" />
            <rect x="132" y="66" width="47" height="7" rx="3.5" fill="#dcd4fb" /><rect x="132" y="84" width="34" height="6" rx="3" fill="#e5dffd" />
            <circle cx="137" cy="110" r="4" fill="#d7cff8" /><rect x="149" y="107" width="27" height="6" rx="3" fill="#e3dcfc" />
            <circle cx="137" cy="130" r="4" fill="#d7cff8" /><rect x="149" y="127" width="22" height="6" rx="3" fill="#e3dcfc" />
            <rect x="195" y="65" width="101" height="78" rx="6" fill="#f9f8ff" stroke="#ddd6fb" />
            <path d="m211 126 22-19 18 10 30-35" fill="none" stroke="#9d84ed" stroke-width="4" stroke-linecap="round" stroke-linejoin="round" />
            <circle cx="211" cy="126" r="5" fill="#8567e6" /><circle cx="233" cy="107" r="5" fill="#8567e6" /><circle cx="251" cy="117" r="5" fill="#8567e6" /><circle cx="281" cy="82" r="5" fill="#8567e6" />
            <path d="M70 160h45l-5-38H75l-5 38Z" fill="#b9a7f0" /><path d="M92 123c-1-21 7-36 20-45 2 21-6 36-20 45Zm-2 1c-18-13-25-28-21-44 17 12 25 27 21 44Zm4 0c15-11 30-13 43-7-12 15-27 19-43 7Z" fill="#8062dc" />
            <rect x="323" y="139" width="55" height="11" rx="3" fill="#7655dc" /><rect x="316" y="150" width="62" height="11" rx="3" fill="#a78eea" /><rect x="328" y="128" width="49" height="11" rx="3" fill="#c1aff3" />
            <path d="m72 43 4 9 9 4-9 4-4 9-4-9-9-4 9-4 4-9Zm280 21 3 7 7 3-7 3-3 7-3-7-7-3 7-3 3-7Z" fill="#c6b4f8" />
          </svg>
          <h2 id="dashboard-empty-title">Seu dashboard está vazio por enquanto</h2>
          <p>Cadastre suas disciplinas para começar a montar seu resumo acadêmico.</p>
          <button type="button" @click="activeSection = 'disciplines'">
            <span aria-hidden="true">＋</span>
            Cadastrar primeira disciplina
          </button>
        </article>
        <div v-else class="dashboard-content-grid">
          <section class="dashboard-panel dashboard-activities-panel" aria-labelledby="dashboard-activities-title">
            <header class="dashboard-panel-header">
              <div>
                <span class="dashboard-eyebrow">Agenda acadêmica</span>
                <h2 id="dashboard-activities-title">Atividades recentes</h2>
              </div>
              <button type="button" @click="activeSection = 'activities'">Ver todas <span aria-hidden="true">→</span></button>
            </header>

            <div v-if="dashboardActivities.length === 0" class="dashboard-panel-empty">
              <span aria-hidden="true">✓</span>
              <div><strong>Nenhuma atividade cadastrada</strong><p>Crie uma atividade para acompanhar seus prazos por aqui.</p></div>
              <button type="button" @click="activeSection = 'activities'">Criar atividade</button>
            </div>

            <ul v-else class="dashboard-activity-list">
              <li v-for="activity in dashboardActivities" :key="activity.id">
                <time :datetime="activity.dueDate"><strong>{{ formatDate(activity.dueDate).split(' ')[0] }}</strong><small>{{ formatDate(activity.dueDate).split(' ')[1] }}</small></time>
                <div class="dashboard-activity-info">
                  <strong>{{ activity.title }}</strong>
                  <small>{{ disciplineName(activity.disciplineId) }}</small>
                </div>
                <span :class="['dashboard-activity-status', activityStatus(activity).className]">{{ activityStatus(activity).label }}</span>
              </li>
            </ul>
          </section>

          <aside class="dashboard-side-column">
            <section class="dashboard-panel dashboard-progress-panel" aria-labelledby="dashboard-progress-title">
              <span class="dashboard-eyebrow">Seu ritmo</span>
              <h2 id="dashboard-progress-title">Progresso das atividades</h2>
              <div class="dashboard-progress-value"><strong>{{ completionPercentage }}%</strong><span>{{ completedActivities }} de {{ activities.length }} concluídas</span></div>
              <div class="dashboard-progress-track" aria-hidden="true"><span :style="{ width: `${completionPercentage}%` }"></span></div>
              <div class="dashboard-progress-meta"><span><strong>{{ pendingActivities.length }}</strong> pendentes</span><span :class="{ 'has-overdue': overdueActivities > 0 }"><strong>{{ overdueActivities }}</strong> atrasadas</span></div>
            </section>

            <section class="dashboard-panel dashboard-actions-panel" aria-labelledby="dashboard-actions-title">
              <h2 id="dashboard-actions-title">Acesso rápido</h2>
              <button type="button" @click="activeSection = 'disciplines'"><span aria-hidden="true">＋</span><div><strong>Nova disciplina</strong><small>Organize uma nova matéria</small></div></button>
              <button type="button" @click="activeSection = 'activities'"><span aria-hidden="true">✓</span><div><strong>Nova atividade</strong><small>Registre um prazo acadêmico</small></div></button>
            </section>
          </aside>
        </div>
        <div v-if="!dashboardLoading && disciplines.length === 0" class="dashboard-guide-grid" aria-label="Próximos passos">
          <article class="dashboard-guide-card is-purple">
            <span class="dashboard-guide-icon" aria-hidden="true">
              <svg viewBox="0 0 24 24"><path d="M4 5.5A3.5 3.5 0 0 1 7.5 2H11v17H7.5A3.5 3.5 0 0 0 4 22V5.5Z" /><path d="M20 5.5A3.5 3.5 0 0 0 16.5 2H13v17h3.5A3.5 3.5 0 0 1 20 22V5.5Z" /></svg>
            </span>
            <div><h3>Adicione suas disciplinas</h3><p>Comece pelas matérias que você está cursando.</p><button type="button" @click="activeSection = 'disciplines'">Cadastrar disciplina <span aria-hidden="true">→</span></button></div>
          </article>
          <article class="dashboard-guide-card is-green">
            <span class="dashboard-guide-icon" aria-hidden="true">
              <svg viewBox="0 0 24 24"><rect x="5" y="4" width="14" height="17" rx="2" /><path d="M9 4V2m6 2V2M8 9h8m-8 4 2 2 4-4" /></svg>
            </span>
            <div><h3>Crie atividades</h3><p>Organize tarefas, trabalhos e compromissos.</p><button type="button" @click="activeSection = 'activities'">Gerenciar atividades <span aria-hidden="true">→</span></button></div>
          </article>
          <article class="dashboard-guide-card is-orange">
            <span class="dashboard-guide-icon" aria-hidden="true">
              <svg viewBox="0 0 24 24"><rect x="3" y="5" width="18" height="16" rx="2" /><path d="M7 3v4m10-4v4M3 10h18m5 4h4" /></svg>
            </span>
            <div><h3>Agende suas provas</h3><p>As datas das avaliações ficarão reunidas aqui.</p><span class="dashboard-planned-action">Em breve</span></div>
          </article>
          <article class="dashboard-guide-card is-violet">
            <span class="dashboard-guide-icon" aria-hidden="true">
              <svg viewBox="0 0 24 24"><path d="M4 19v-5m5 5V9m5 10v-7m5 7V5" /><path d="m4 10 5-4 5 3 6-6" /></svg>
            </span>
            <div><h3>Acompanhe seu progresso</h3><p>Gráficos e estatísticas aparecerão com seus dados.</p><span class="dashboard-planned-action">Em breve</span></div>
          </article>
        </div>
        <p v-if="disciplines.length === 0" class="dashboard-tip">
          <span aria-hidden="true">💡</span>
          <strong>Dica:</strong> quanto mais você usar o AcadOrganize, mais completo será o seu dashboard.
        </p>
      </section>

      <DisciplinesEmpty
        v-if="activeSection === 'disciplines'"
        :access-token="accessToken"
      />

      <ActivitiesScreen
        v-if="activeSection === 'activities'"
        :access-token="accessToken"
        @navigate="activeSection = $event"
      />
    </main>
  </div>
</template>
<style scoped>
.dashboard-shell {
  background: #f5f6fb;
  color: #151a2d;
  display: grid;
  grid-template-columns: 252px minmax(0, 1fr);
  min-height: 100svh;
  width: 100%;
}

.dashboard-sidebar {
  background: linear-gradient(180deg, #111a2f 0%, #091326 100%);
  color: #fff;
  display: flex;
  flex-direction: column;
  height: 100svh;
  padding: 26px 16px 18px;
  position: sticky;
  top: 0;
}
.dashboard-brand { align-items: center; border-bottom: 1px solid rgba(255, 255, 255, .08); display: flex; gap: 12px; margin: 0 -16px 22px; padding: 0 22px 25px; }
.dashboard-brand-icon { align-items: center; color: #7547ff; display: flex; flex: 0 0 42px; height: 42px; justify-content: center; }
.dashboard-brand-icon svg { fill: #6d3cf2; height: 38px; stroke: #7d55f2; stroke-linecap: round; stroke-linejoin: round; stroke-width: 1.4; width: 38px; }
.dashboard-brand strong { display: block; font-size: .96rem; letter-spacing: -.025em; }
.dashboard-brand small { color: #9faac0; display: block; font-size: .64rem; margin-top: 3px; }
.dashboard-navigation { display: grid; gap: 7px; }
.dashboard-navigation button,
.dashboard-logout {
  align-items: center;
  background: transparent;
  border: 0;
  border-radius: 8px;
  color: #d4d9e3;
  display: flex;
  font-size: .78rem;
  gap: 13px;
  padding: 12px 14px;
  text-align: left;
  transition: background-color .18s, color .18s;
  width: 100%;
}
.dashboard-navigation button:hover,
.dashboard-logout:hover { background: rgba(255, 255, 255, .07); color: #fff; }
.dashboard-navigation button.active { background: linear-gradient(100deg, #5431b5, #6b3ad6); box-shadow: 0 10px 24px rgba(32, 12, 88, .35); color: #fff; font-weight: 750; }
.dashboard-navigation svg,
.dashboard-logout svg { fill: none; flex: 0 0 20px; height: 20px; stroke: currentColor; stroke-linecap: round; stroke-linejoin: round; stroke-width: 1.8; width: 20px; }
.dashboard-navigation button:focus-visible,
.dashboard-logout:focus-visible { outline: 2px solid #947eff; outline-offset: 2px; }
.dashboard-sidebar-footer { border-top: 1px solid rgba(255, 255, 255, .07); margin-top: auto; padding-top: 16px; }
.dashboard-user-card { align-items: center; background: rgba(255, 255, 255, .045); border-radius: 9px; display: flex; gap: 10px; margin-bottom: 9px; min-width: 0; padding: 10px; }
.dashboard-user-avatar { align-items: center; background: linear-gradient(135deg, #7749f7, #5320da); border-radius: 50%; display: flex; flex: 0 0 36px; font-size: .78rem; font-weight: 800; height: 36px; justify-content: center; }
.dashboard-user-details { min-width: 0; }
.dashboard-user-details strong { display: block; font-size: .71rem; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.dashboard-user-details small { color: #a9b1c1; display: block; font-size: .61rem; margin-top: 2px; }
.dashboard-main { margin: 0 auto; min-width: 0; padding: 30px clamp(24px, 3vw, 48px) 48px; width: 100%; }
.dashboard-topbar { align-items: center; background: linear-gradient(120deg, #5730b7 0%, #7043d7 52%, #875ceb 100%); border-radius: 20px; box-shadow: 0 18px 42px rgba(91, 51, 184, .2); color: #fff; display: flex; justify-content: space-between; margin-bottom: 22px; overflow: hidden; padding: 28px 30px; position: relative; }
.dashboard-topbar::after { background: rgba(255, 255, 255, .08); border-radius: 50%; content: ''; height: 240px; position: absolute; right: -65px; top: -125px; width: 240px; }
.dashboard-welcome { position: relative; z-index: 1; }
.dashboard-welcome-label { color: #ded3ff; display: block; font-size: .62rem; font-weight: 800; letter-spacing: .12em; margin-bottom: 7px; text-transform: uppercase; }
.dashboard-topbar h1 { color: #fff; font-size: clamp(1.65rem, 2.5vw, 2.15rem); font-weight: 820; letter-spacing: -.04em; line-height: 1.1; margin: 0 0 8px; }
.dashboard-topbar p { color: #e5ddfa; font-size: .78rem; }
.dashboard-welcome-actions { display: flex; gap: 9px; margin-top: 19px; }
.dashboard-welcome-actions button { border: 1px solid rgba(255,255,255,.34); border-radius: 9px; cursor: pointer; font-size: .68rem; font-weight: 750; padding: 9px 13px; }
.dashboard-welcome-actions button:first-child { background: #fff; border-color: #fff; color: #5f34c0; }
.dashboard-welcome-actions button:last-child { background: rgba(255,255,255,.1); color: #fff; }
.dashboard-date { align-items: center; background: rgba(255, 255, 255, .14); border: 1px solid rgba(255, 255, 255, .25); border-radius: 10px; color: #fff; display: flex; font-size: .68rem; font-weight: 700; gap: 9px; padding: 11px 13px; position: relative; z-index: 1; }
.dashboard-date svg { fill: none; height: 18px; stroke: #fff; stroke-linecap: round; stroke-linejoin: round; stroke-width: 1.7; width: 18px; }
.dashboard-overview { display: grid; gap: 18px; }
.dashboard-summary-grid { display: grid; gap: 15px; grid-template-columns: repeat(5, minmax(0, 1fr)); }
.dashboard-summary-card { align-items: center; background: #fff; border: 1px solid #e5e7f0; border-radius: 15px; box-shadow: 0 8px 24px rgba(30, 36, 65, .05); display: flex; gap: 14px; min-height: 120px; min-width: 0; padding: 19px; transition: box-shadow .2s, transform .2s; }
.dashboard-summary-card:hover { box-shadow: 0 12px 30px rgba(30, 36, 65, .075); transform: translateY(-2px); }
.dashboard-summary-icon { align-items: center; background: #f1edff; border-radius: 50%; color: #6739e7; display: flex; flex: 0 0 48px; height: 48px; justify-content: center; }
.dashboard-summary-icon svg { fill: none; height: 24px; stroke: currentColor; stroke-linecap: round; stroke-linejoin: round; stroke-width: 1.8; width: 24px; }
.dashboard-summary-card div { min-width: 0; }
.dashboard-summary-card p { color: #626a7f; font-size: .71rem; line-height: 1.25; }
.dashboard-summary-card strong { color: #171c30; display: block; font-size: 1.48rem; line-height: 1; margin: 8px 0 6px; }
.dashboard-summary-card small { color: #8a90a2; display: block; font-size: .62rem; line-height: 1.25; }
.dashboard-summary-card.is-green .dashboard-summary-icon { background: #e8f8ef; color: #2daf68; }
.dashboard-summary-card.is-orange .dashboard-summary-icon { background: #fff0e2; color: #ee831e; }
.dashboard-summary-card.is-violet .dashboard-summary-icon { background: #f1edff; color: #6330e0; }
.dashboard-summary-card.is-blue .dashboard-summary-icon { background: #eaf2ff; color: #347bd8; }
.dashboard-empty-hero { align-items: center; background: #fff; border: 1px solid #ebeaf1; border-radius: 12px; box-shadow: 0 5px 16px rgba(30, 36, 65, .035); display: flex; flex-direction: column; min-height: 405px; padding: 25px 30px 31px; text-align: center; }
.dashboard-empty-illustration { display: block; height: 182px; max-width: 420px; width: min(100%, 420px); }
.dashboard-empty-hero h2 { color: #171c30; font-size: 1.22rem; font-weight: 800; letter-spacing: -.025em; margin: 3px 0 8px; }
.dashboard-empty-hero p { color: #73798e; font-size: .78rem; line-height: 1.55; max-width: 430px; }
.dashboard-empty-hero button { align-items: center; background: linear-gradient(100deg, #5c20de, #741dff); border: 0; border-radius: 7px; box-shadow: 0 8px 19px rgba(102, 36, 225, .2); color: #fff; display: flex; font-size: .78rem; font-weight: 700; gap: 8px; margin-top: 19px; padding: 12px 18px; }
.dashboard-empty-hero button span { font-size: 1.15rem; font-weight: 400; line-height: .8; }
.dashboard-empty-hero button:hover { box-shadow: 0 11px 24px rgba(102, 36, 225, .28); transform: translateY(-1px); }
.dashboard-empty-hero button:focus-visible { outline: 3px solid rgba(105, 54, 224, .28); outline-offset: 3px; }
.dashboard-guide-grid { display: grid; gap: 14px; grid-template-columns: repeat(4, minmax(0, 1fr)); }
.dashboard-guide-card { align-items: flex-start; background: #fff; border: 1px solid #e7e4f0; border-radius: 11px; display: flex; gap: 13px; min-height: 164px; padding: 20px 17px; }
.dashboard-guide-icon { align-items: center; background: #f1edff; border-radius: 50%; color: #6330e0; display: flex; flex: 0 0 43px; height: 43px; justify-content: center; }
.dashboard-guide-icon svg { fill: none; height: 21px; stroke: currentColor; stroke-linecap: round; stroke-linejoin: round; stroke-width: 1.8; width: 21px; }
.dashboard-guide-card > div { display: flex; flex: 1; flex-direction: column; min-height: 122px; }
.dashboard-guide-card h3 { color: #202538; font-size: .71rem; font-weight: 750; margin: 2px 0 7px; }
.dashboard-guide-card p { color: #7a8194; font-size: .64rem; line-height: 1.55; }
.dashboard-guide-card button { align-self: flex-start; background: none; border: 0; color: #6429db; font-size: .64rem; font-weight: 750; margin-top: auto; padding: 4px 0; }
.dashboard-guide-card button:hover { text-decoration: underline; }
.dashboard-planned-action { color: #8b819d; font-size: .63rem; font-weight: 700; margin-top: auto; }
.dashboard-guide-card.is-green .dashboard-guide-icon { background: #e8f8ef; color: #2daf68; }
.dashboard-guide-card.is-orange .dashboard-guide-icon { background: #fff0e2; color: #ee831e; }
.dashboard-guide-card.is-violet .dashboard-guide-icon { background: #f1edff; color: #6330e0; }
.dashboard-tip { background: #f2efff; border-radius: 8px; color: #6c7287; font-size: .7rem; padding: 12px 18px; text-align: center; }
.dashboard-tip strong { color: #30364a; }
.dashboard-content-grid { align-items: start; display: grid; gap: 18px; grid-template-columns: minmax(0, 1.65fr) minmax(300px, .8fr); }
.dashboard-panel { background: #fff; border: 1px solid #e7e8f0; border-radius: 14px; box-shadow: 0 8px 24px rgba(30, 36, 65, .04); }
.dashboard-panel-header { align-items: center; border-bottom: 1px solid #eff0f5; display: flex; justify-content: space-between; padding: 20px 22px 17px; }
.dashboard-panel h2 { color: #171c30; font-size: 1rem; font-weight: 800; letter-spacing: -.02em; margin: 3px 0 0; }
.dashboard-eyebrow { color: #7240df; display: block; font-size: .61rem; font-weight: 800; letter-spacing: .09em; text-transform: uppercase; }
.dashboard-panel-header button { background: transparent; border: 0; color: #6734d8; cursor: pointer; font-size: .7rem; font-weight: 750; padding: 7px; }
.dashboard-activity-list { list-style: none; margin: 0; padding: 0 22px; }
.dashboard-activity-list li { align-items: center; border-bottom: 1px solid #eff0f5; display: grid; gap: 13px; grid-template-columns: 48px minmax(0, 1fr) auto; padding: 14px 0; }
.dashboard-activity-list li:last-child { border-bottom: 0; }
.dashboard-activity-list time { align-items: center; background: #f4f1ff; border-radius: 10px; color: #6330d4; display: flex; flex-direction: column; height: 46px; justify-content: center; text-transform: uppercase; }
.dashboard-activity-list time strong { font-size: .84rem; line-height: 1; }
.dashboard-activity-list time small { font-size: .48rem; font-weight: 800; margin-top: 3px; }
.dashboard-activity-info { min-width: 0; }
.dashboard-activity-info strong { color: #22283b; display: block; font-size: .76rem; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.dashboard-activity-info small { color: #83899a; display: block; font-size: .63rem; margin-top: 4px; }
.dashboard-activity-status { border-radius: 999px; font-size: .55rem; font-weight: 800; padding: 5px 8px; }
.dashboard-activity-status.is-completed { background: #e9f8ef; color: #23894f; }
.dashboard-activity-status.is-overdue { background: #fff0ef; color: #c4463e; }
.dashboard-activity-status.is-progress { background: #eaf2ff; color: #3471c7; }
.dashboard-activity-status.is-pending { background: #fff2e5; color: #b76315; }
.dashboard-panel-empty { align-items: center; display: flex; gap: 12px; min-height: 180px; padding: 24px; }
.dashboard-panel-empty > span { align-items: center; background: #e9f8ef; border-radius: 50%; color: #249255; display: flex; flex: 0 0 42px; height: 42px; justify-content: center; }
.dashboard-panel-empty div { flex: 1; }
.dashboard-panel-empty strong { color: #252a3d; font-size: .75rem; }
.dashboard-panel-empty p { color: #7b8192; font-size: .63rem; margin-top: 3px; }
.dashboard-panel-empty button { background: #6832df; border: 0; border-radius: 8px; color: #fff; cursor: pointer; font-size: .62rem; font-weight: 750; padding: 9px 11px; }
.dashboard-side-column { display: grid; gap: 17px; }
.dashboard-progress-panel, .dashboard-actions-panel { padding: 20px; }
.dashboard-progress-value { align-items: flex-end; display: flex; gap: 9px; margin: 18px 0 10px; }
.dashboard-progress-value strong { color: #5f2bd5; font-size: 1.7rem; line-height: 1; }
.dashboard-progress-value span { color: #808698; font-size: .6rem; }
.dashboard-progress-track { background: #edeaf5; border-radius: 999px; height: 7px; overflow: hidden; }
.dashboard-progress-track span { background: linear-gradient(90deg, #7041df, #8b62ef); border-radius: inherit; display: block; height: 100%; }
.dashboard-progress-meta { color: #747b8e; display: flex; font-size: .6rem; justify-content: space-between; margin-top: 12px; }
.dashboard-progress-meta strong { color: #30364a; }
.dashboard-progress-meta .has-overdue, .dashboard-progress-meta .has-overdue strong { color: #c4463e; }
.dashboard-actions-panel { display: grid; gap: 9px; }
.dashboard-actions-panel h2 { margin-bottom: 5px; }
.dashboard-actions-panel button { align-items: center; background: #faf9fd; border: 1px solid #eceaf3; border-radius: 10px; color: #6330d8; cursor: pointer; display: flex; gap: 11px; padding: 11px; text-align: left; transition: background .18s, border-color .18s; }
.dashboard-actions-panel button:hover { background: #f4f0ff; border-color: #d9cff2; }
.dashboard-actions-panel button > span { align-items: center; background: #eee9ff; border-radius: 8px; display: flex; flex: 0 0 34px; font-size: 1rem; height: 34px; justify-content: center; }
.dashboard-actions-panel button strong { color: #30364a; display: block; font-size: .67rem; }
.dashboard-actions-panel button small { color: #858b9d; display: block; font-size: .55rem; margin-top: 2px; }
@media (max-width: 1180px) {
  .dashboard-summary-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }
  .dashboard-guide-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .dashboard-content-grid { grid-template-columns: 1fr; }
  .dashboard-side-column { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media (max-width: 760px) {
  .dashboard-shell { display: block; }
  .dashboard-sidebar { align-items: stretch; bottom: 0; display: grid; grid-template-columns: minmax(0, 1fr) 64px; height: auto; left: 0; padding: 7px 10px max(7px, env(safe-area-inset-bottom)); position: fixed; right: 0; top: auto; z-index: 80; }
  .dashboard-brand, .dashboard-user-card { display: none; }
  .dashboard-navigation { display: grid; gap: 4px; grid-template-columns: repeat(3, minmax(0, 1fr)); }
  .dashboard-navigation button, .dashboard-logout { flex-direction: column; font-size: .57rem; gap: 3px; justify-content: center; padding: 7px 5px; text-align: center; }
  .dashboard-navigation button.active { background: rgba(108, 65, 226, .42); box-shadow: none; }
  .dashboard-navigation svg, .dashboard-logout svg { height: 18px; width: 18px; }
  .dashboard-sidebar-footer { border: 0; margin: 0; padding: 0; }
  .dashboard-main { padding: 22px 16px 92px; }
  .dashboard-topbar { align-items: flex-start; padding: 24px; }
  .dashboard-welcome { max-width: calc(100% - 64px); }
  .dashboard-summary-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .dashboard-side-column { grid-template-columns: 1fr; }
}
@media (max-width: 520px) {
  .dashboard-topbar { align-items: flex-start; gap: 15px; padding: 22px 19px; }
  .dashboard-topbar h1 { font-size: 1.35rem; }
  .dashboard-topbar p { font-size: .7rem; max-width: 250px; }
  .dashboard-welcome { max-width: 100%; }
  .dashboard-welcome-actions { flex-wrap: wrap; }
  .dashboard-date { font-size: 0; padding: 9px; position: absolute; right: 16px; top: 16px; }
  .dashboard-summary-grid,
  .dashboard-guide-grid { grid-template-columns: 1fr; }
  .dashboard-summary-card { min-height: 100px; }
  .dashboard-empty-hero { min-height: 370px; padding-inline: 20px; }
  .dashboard-empty-illustration { height: auto; }
  .dashboard-panel-header { padding-inline: 16px; }
  .dashboard-activity-list { padding-inline: 16px; }
  .dashboard-activity-list li { grid-template-columns: 42px minmax(0, 1fr); }
  .dashboard-activity-list time { height: 42px; }
  .dashboard-activity-status { grid-column: 2; justify-self: start; }
  .dashboard-panel-empty { align-items: flex-start; flex-wrap: wrap; padding: 20px 16px; }
  .dashboard-panel-empty button { margin-left: 54px; }
}
</style>
