<script setup>
import { computed, ref } from 'vue'

const { user } = defineProps({
  user: { type: Object, required: true },
  accessToken: { type: String, required: true },
})

const emit = defineEmits(['logout'])
const activeSection = ref('dashboard')

const firstName = computed(() => user.name?.trim().split(/\s+/)[0] || 'estudante')
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
      <header class="dashboard-topbar">
        <div>
          <template v-if="activeSection === 'dashboard'">
            <h1>Olá, {{ firstName }}! <span aria-hidden="true">👋</span></h1>
            <p>Este é o seu resumo acadêmico.</p>
          </template>
          <template v-else>
            <h1>Disciplinas</h1>
            <p>Organize as disciplinas do seu período.</p>
          </template>
        </div>

        <time v-if="activeSection === 'dashboard'" class="dashboard-date" :datetime="todayIso">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <rect x="3" y="5" width="18" height="16" rx="2" />
            <path d="M7 3v4m10-4v4M3 10h18" />
          </svg>
          Hoje, {{ todayLabel }}
        </time>
      </header>

      <section v-if="activeSection === 'dashboard'" class="dashboard-overview" aria-labelledby="dashboard-empty-title">
        <div class="dashboard-summary-grid" aria-label="Resumo sem dados">
          <article class="dashboard-summary-card is-purple">
            <span class="dashboard-summary-icon" aria-hidden="true">
              <svg viewBox="0 0 24 24"><path d="M4 5.5A3.5 3.5 0 0 1 7.5 2H11v17H7.5A3.5 3.5 0 0 0 4 22V5.5Z" /><path d="M20 5.5A3.5 3.5 0 0 0 16.5 2H13v17h3.5A3.5 3.5 0 0 1 20 22V5.5Z" /></svg>
            </span>
            <div><p>Disciplinas</p><strong>—</strong><small>Nenhuma ainda</small></div>
          </article>

          <article class="dashboard-summary-card is-green">
            <span class="dashboard-summary-icon" aria-hidden="true">
              <svg viewBox="0 0 24 24"><rect x="5" y="4" width="14" height="17" rx="2" /><path d="M9 4V2m6 2V2M8 9h8m-8 4 2 2 4-4" /></svg>
            </span>
            <div><p>Atividades</p><strong>—</strong><small>Nenhuma ainda</small></div>
          </article>

          <article class="dashboard-summary-card is-orange">
            <span class="dashboard-summary-icon" aria-hidden="true">
              <svg viewBox="0 0 24 24"><rect x="3" y="5" width="18" height="16" rx="2" /><path d="M7 3v4m10-4v4M3 10h18m5 4h4" /></svg>
            </span>
            <div><p>Provas</p><strong>—</strong><small>Nenhuma ainda</small></div>
          </article>

          <article class="dashboard-summary-card is-violet">
            <span class="dashboard-summary-icon" aria-hidden="true">
              <svg viewBox="0 0 24 24"><path d="M4 19v-5m5 5V9m5 10v-7m5 7V5" /><path d="m4 10 5-4 5 3 6-6" /></svg>
            </span>
            <div><p>Média geral</p><strong>—</strong><small>Sem dados</small></div>
          </article>

          <article class="dashboard-summary-card is-blue">
            <span class="dashboard-summary-icon" aria-hidden="true">
              <svg viewBox="0 0 24 24"><path d="M12 3a9 9 0 1 1-7.8 4.5" /><path d="M12 3v9l6 4" /></svg>
            </span>
            <div><p>Frequência média</p><strong>—</strong><small>Sem dados</small></div>
          </article>
        </div>

        <article class="dashboard-empty-hero">
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

        <div class="dashboard-guide-grid" aria-label="Próximos passos">
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
            <div><h3>Crie atividades</h3><p>Organize tarefas, trabalhos e compromissos.</p><span class="dashboard-planned-action">Em breve</span></div>
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

        <p class="dashboard-tip">
          <span aria-hidden="true">💡</span>
          <strong>Dica:</strong> quanto mais você usar o AcadOrganize, mais completo será o seu dashboard.
        </p>
      </section>

      <section v-else class="dashboard-disciplines-empty" aria-labelledby="disciplines-empty-title">
        <span class="dashboard-disciplines-icon" aria-hidden="true">
          <svg viewBox="0 0 24 24"><path d="M4 5.5A3.5 3.5 0 0 1 7.5 2H11v17H7.5A3.5 3.5 0 0 0 4 22V5.5Z" /><path d="M20 5.5A3.5 3.5 0 0 0 16.5 2H13v17h3.5A3.5 3.5 0 0 1 20 22V5.5Z" /></svg>
        </span>
        <h2 id="disciplines-empty-title">Nenhuma disciplina cadastrada</h2>
        <p>O cadastro de disciplinas será conectado nesta área na próxima etapa.</p>
      </section>
    </main>
  </div>
</template>

<style scoped>
.dashboard-shell {
  background: #f7f7fc;
  color: #151a2d;
  display: grid;
  grid-template-columns: 242px minmax(0, 1fr);
  min-height: 100svh;
  width: 100%;
}

.dashboard-sidebar {
  background: linear-gradient(180deg, #0d1728 0%, #071225 100%);
  color: #fff;
  display: flex;
  flex-direction: column;
  height: 100svh;
  padding: 28px 14px 18px;
  position: sticky;
  top: 0;
}

.dashboard-brand { align-items: center; border-bottom: 1px solid rgba(255, 255, 255, .06); display: flex; gap: 11px; margin: 0 -14px 20px; padding: 0 20px 27px; }
.dashboard-brand-icon { align-items: center; color: #7547ff; display: flex; flex: 0 0 42px; height: 42px; justify-content: center; }
.dashboard-brand-icon svg { fill: #6d3cf2; height: 38px; stroke: #7d55f2; stroke-linecap: round; stroke-linejoin: round; stroke-width: 1.4; width: 38px; }
.dashboard-brand strong { display: block; font-size: 1rem; letter-spacing: -.025em; }
.dashboard-brand small { color: #adb5c4; display: block; font-size: .63rem; margin-top: 3px; }
.dashboard-navigation { display: grid; gap: 7px; }

.dashboard-navigation button,
.dashboard-logout {
  align-items: center;
  background: transparent;
  border: 0;
  border-radius: 8px;
  color: #d4d9e3;
  display: flex;
  font-size: .82rem;
  gap: 13px;
  padding: 12px 13px;
  text-align: left;
  transition: background-color .18s, color .18s;
  width: 100%;
}

.dashboard-navigation button:hover,
.dashboard-logout:hover { background: rgba(255, 255, 255, .07); color: #fff; }
.dashboard-navigation button.active { background: linear-gradient(100deg, #302a69, #422b88); box-shadow: 0 7px 18px rgba(12, 7, 47, .25); color: #fff; font-weight: 700; }
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

.dashboard-main { min-width: 0; padding: 29px clamp(24px, 3vw, 48px) 30px; }
.dashboard-topbar { align-items: center; display: flex; justify-content: space-between; margin-bottom: 25px; }
.dashboard-topbar h1 { color: #13182a; font-size: clamp(1.65rem, 2.5vw, 2rem); font-weight: 800; letter-spacing: -.04em; line-height: 1.15; margin: 0 0 7px; }
.dashboard-topbar p { color: #687086; font-size: .82rem; }
.dashboard-date { align-items: center; background: #fff; border: 1px solid #e1e3eb; border-radius: 8px; color: #343a50; display: flex; font-size: .72rem; font-weight: 650; gap: 9px; padding: 12px 14px; }
.dashboard-date svg { fill: none; height: 18px; stroke: #657087; stroke-linecap: round; stroke-linejoin: round; stroke-width: 1.7; width: 18px; }

.dashboard-overview { display: grid; gap: 17px; }
.dashboard-summary-grid { display: grid; gap: 14px; grid-template-columns: repeat(5, minmax(0, 1fr)); }
.dashboard-summary-card { align-items: center; background: #fff; border: 1px solid #ebeaf1; border-radius: 12px; box-shadow: 0 5px 16px rgba(30, 36, 65, .035); display: flex; gap: 13px; min-height: 112px; min-width: 0; padding: 17px; }
.dashboard-summary-icon { align-items: center; background: #f1edff; border-radius: 50%; color: #6739e7; display: flex; flex: 0 0 48px; height: 48px; justify-content: center; }
.dashboard-summary-icon svg { fill: none; height: 24px; stroke: currentColor; stroke-linecap: round; stroke-linejoin: round; stroke-width: 1.8; width: 24px; }
.dashboard-summary-card div { min-width: 0; }
.dashboard-summary-card p { color: #596078; font-size: .67rem; line-height: 1.25; }
.dashboard-summary-card strong { color: #171c30; display: block; font-size: 1.25rem; line-height: 1; margin: 7px 0 5px; }
.dashboard-summary-card small { color: #858b9e; display: block; font-size: .61rem; line-height: 1.25; }
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

.dashboard-disciplines-empty { align-items: center; background: #fff; border: 1px solid #e7e4f0; border-radius: 12px; display: flex; flex-direction: column; justify-content: center; min-height: calc(100svh - 145px); padding: 40px; text-align: center; }
.dashboard-disciplines-icon { align-items: center; background: #f1edff; border-radius: 50%; color: #6330e0; display: flex; height: 64px; justify-content: center; width: 64px; }
.dashboard-disciplines-icon svg { fill: none; height: 29px; stroke: currentColor; stroke-linecap: round; stroke-linejoin: round; stroke-width: 1.7; width: 29px; }
.dashboard-disciplines-empty h2 { color: #202538; font-size: 1.1rem; margin: 18px 0 7px; }
.dashboard-disciplines-empty p { color: #7a8194; font-size: .76rem; }

@media (max-width: 1180px) {
  .dashboard-summary-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }
  .dashboard-guide-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}

@media (max-width: 760px) {
  .dashboard-shell { grid-template-columns: 76px minmax(0, 1fr); }
  .dashboard-sidebar { padding-inline: 10px; }
  .dashboard-brand { justify-content: center; margin-inline: -10px; padding-inline: 10px; }
  .dashboard-brand > span:last-child,
  .dashboard-user-details { display: none; }
  .dashboard-user-card { background: transparent; justify-content: center; padding-inline: 0; }
  .dashboard-navigation button,
  .dashboard-logout { font-size: 0; justify-content: center; padding-inline: 10px; }
  .dashboard-main { padding: 24px 18px; }
  .dashboard-summary-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}

@media (max-width: 520px) {
  .dashboard-topbar { align-items: flex-start; gap: 15px; }
  .dashboard-topbar h1 { font-size: 1.35rem; }
  .dashboard-date { font-size: 0; padding: 10px; }
  .dashboard-summary-grid,
  .dashboard-guide-grid { grid-template-columns: 1fr; }
  .dashboard-summary-card { min-height: 100px; }
  .dashboard-empty-hero { min-height: 370px; padding-inline: 20px; }
  .dashboard-empty-illustration { height: auto; }
}
</style>
