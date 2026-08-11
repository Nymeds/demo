<script setup>
import { ref } from 'vue'
import SurpriseButton from './SurpriseButton.vue'

const { user, accessToken } = defineProps({
  user: { type: Object, required: true },
  accessToken: { type: String, required: true },
})

const emit = defineEmits(['logout'])

const name = ref('')
const status = ref('ACTIVE')
const disciplineInput = ref('')
const disciplines = ref([])
const loading = ref(false)
const feedback = ref('')
const feedbackType = ref('')
const createdDashboard = ref(null)

function addDiscipline() {
  const discipline = disciplineInput.value.trim()

  if (!discipline || disciplines.value.includes(discipline)) {
    return
  }

  disciplines.value.push(discipline)
  disciplineInput.value = ''
}

function removeDiscipline(index) {
  disciplines.value.splice(index, 1)
}

async function createDashboard() {
  loading.value = true
  feedback.value = ''

  try {
    const response = await fetch('/api/v1/dashboards', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${accessToken}`,
      },
      body: JSON.stringify({
        name: name.value,
        status: status.value,
        disciplines: disciplines.value,
      }),
    })

    const data = await response.json().catch(() => ({}))

    if (!response.ok) {
      throw new Error(data.detail || data.message || 'Não foi possível criar o dashboard.')
    }

    createdDashboard.value = data
    feedback.value = 'Dashboard criado com sucesso.'
    feedbackType.value = 'success'
    name.value = ''
    status.value = 'ACTIVE'
    disciplines.value = []
  } catch (error) {
    feedback.value = error.message || 'Não foi possível conectar à API.'
    feedbackType.value = 'error'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="dashboard-page">
    <SurpriseButton />
    <header class="dashboard-header">
      <a class="dashboard-brand" href="#">Studdy</a>
      <div class="dashboard-user">
        <span>{{ user.name }}</span>
        <button type="button" @click="emit('logout')">Sair</button>
      </div>
    </header>

    <section class="dashboard-content" aria-labelledby="dashboard-title">
      <div class="dashboard-intro">
        <p class="dashboard-kicker">Seu espaço acadêmico</p>
        <h1 id="dashboard-title">Crie seu dashboard.</h1>
        <p>Organize um semestre, período ou objetivo de estudo e inclua as disciplinas que farão parte dele.</p>
      </div>

      <form class="dashboard-form" @submit.prevent="createDashboard">
        <label>
          Nome do dashboard
          <input v-model.trim="name" type="text" required maxlength="120" placeholder="Ex.: Semestre 2026.1">
        </label>

        <label>
          Status
          <select v-model="status">
            <option value="ACTIVE">Ativo</option>
            <option value="INACTIVE">Inativo</option>
          </select>
        </label>

        <fieldset>
          <legend>Disciplinas</legend>
          <div class="discipline-entry">
            <input
              v-model="disciplineInput"
              type="text"
              maxlength="120"
              placeholder="Ex.: Interação Humano-Computador"
              @keydown.enter.prevent="addDiscipline"
            >
            <button type="button" @click="addDiscipline">Adicionar</button>
          </div>
          <p v-if="!disciplines.length" class="empty-disciplines">Adicione as disciplinas que pertencem a este dashboard.</p>
          <ul v-else class="discipline-list">
            <li v-for="(discipline, index) in disciplines" :key="discipline">
              {{ discipline }}
              <button type="button" :aria-label="`Remover ${discipline}`" @click="removeDiscipline(index)">×</button>
            </li>
          </ul>
        </fieldset>

        <button class="create-dashboard-button" type="submit" :disabled="loading">
          {{ loading ? 'Criando...' : 'Criar dashboard' }}
        </button>

        <p v-if="feedback" class="dashboard-feedback" :class="feedbackType" role="status">{{ feedback }}</p>
      </form>

      <aside v-if="createdDashboard" class="created-dashboard" aria-live="polite">
        <p class="dashboard-kicker">Dashboard criado</p>
        <h2>{{ createdDashboard.name }}</h2>
        <p><strong>Status:</strong> {{ createdDashboard.status === 'ACTIVE' ? 'Ativo' : 'Inativo' }}</p>
        <p><strong>ID:</strong> {{ createdDashboard.id }}</p>
        <p><strong>Disciplinas:</strong> {{ createdDashboard.disciplines.length || 'Nenhuma' }}</p>
      </aside>
    </section>
  </main>
</template>

<style scoped>
.dashboard-page { background: #f7f7f0; color: #28372d; min-height: 100vh; }
.dashboard-header { align-items: center; border-bottom: 1px solid #dfe4d9; display: flex; justify-content: space-between; padding: 22px clamp(24px, 6vw, 90px); }
.dashboard-brand { color: #2c513c; font-size: 1.2rem; font-weight: 800; letter-spacing: -.04em; text-decoration: none; }.dashboard-brand::before { color: #e4743d; content: '•'; font-size: 1.6rem; margin-right: 4px; }
.dashboard-user { align-items: center; display: flex; gap: 18px; }.dashboard-user span { color: #68766a; font-size: .9rem; }.dashboard-user button { background: none; border: 0; color: #2c513c; font-weight: 700; padding: 6px; }
.dashboard-content { display: grid; gap: 28px; grid-template-columns: minmax(250px, .75fr) minmax(360px, 1fr); margin: 0 auto; max-width: 1120px; padding: clamp(42px, 7vw, 96px) 28px; }
.dashboard-intro { padding: 28px 24px 0 0; }.dashboard-kicker { color: #70816c; font-size: .72rem; font-weight: 700; letter-spacing: .1em; margin: 0 0 15px; text-transform: uppercase; }.dashboard-intro h1 { color: #27372d; font-family: 'Playfair Display', Georgia, serif; font-size: clamp(2.8rem, 5vw, 4.8rem); letter-spacing: -.06em; line-height: .93; margin: 0 0 25px; }.dashboard-intro > p:last-child { color: #69776b; line-height: 1.7; max-width: 340px; }
.dashboard-form, .created-dashboard { background: #fffefa; border: 1px solid #dfe4d9; border-radius: 16px; box-shadow: 0 18px 45px rgba(42, 61, 44, .08); padding: clamp(24px, 4vw, 38px); }.dashboard-form { display: grid; gap: 20px; }.dashboard-form label, fieldset { color: #435143; display: grid; font-size: .88rem; font-weight: 700; gap: 9px; }.dashboard-form input, select { background: #fffefa; border: 1px solid #d7ddd2; border-radius: 8px; color: #27372d; outline: none; padding: 13px 14px; }.dashboard-form input:focus, select:focus { border-color: #66834f; box-shadow: 0 0 0 3px rgba(102, 131, 79, .13); }fieldset { border: 0; margin: 0; padding: 0; }legend { margin-bottom: 9px; padding: 0; }.discipline-entry { display: flex; gap: 8px; }.discipline-entry input { flex: 1; min-width: 0; }.discipline-entry button { background: #e3eadf; border: 0; border-radius: 8px; color: #31513a; font-weight: 700; padding: 0 14px; }.empty-disciplines { color: #8a9589; font-size: .84rem; font-weight: 400; margin: 0; }.discipline-list { display: flex; flex-wrap: wrap; gap: 8px; list-style: none; margin: 0; padding: 0; }.discipline-list li { align-items: center; background: #e7f0e3; border-radius: 99px; color: #416240; display: flex; font-size: .82rem; font-weight: 600; gap: 6px; padding: 7px 8px 7px 12px; }.discipline-list button { background: #cfe0c9; border: 0; border-radius: 50%; color: #325033; font-size: 1.05rem; height: 19px; line-height: 1; padding: 0; width: 19px; }.create-dashboard-button { background: #304b37; border: 0; border-radius: 8px; color: #fff; font-weight: 700; margin-top: 4px; padding: 15px; }.create-dashboard-button:disabled { cursor: wait; opacity: .65; }.dashboard-feedback { border-left: 3px solid; font-size: .88rem; margin: 0; padding: 10px 12px; }.dashboard-feedback.success { background: #edf6e9; border-color: #628a53; color: #386135; }.dashboard-feedback.error { background: #fff0eb; border-color: #d95b35; color: #9c3d26; }.created-dashboard { grid-column: 2; }.created-dashboard h2 { color: #2c513c; margin: 0 0 20px; }.created-dashboard p { color: #657565; overflow-wrap: anywhere; }
@media (max-width: 760px) { .dashboard-content { grid-template-columns: 1fr; }.dashboard-intro { padding: 0; }.created-dashboard { grid-column: auto; }.dashboard-header { padding: 18px 24px; }.dashboard-user span { display: none; } }
</style>
