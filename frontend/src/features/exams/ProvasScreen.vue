<script setup>
import { computed, ref } from 'vue'

const props = defineProps({
  accessToken: { type: String, required: true },
})

const emit = defineEmits(['navigate'])

const search = ref('')
const selectedDiscipline = ref('all')
const selectedStatus = ref('all')
const selectedPeriod = ref('all')
const page = ref(1)
const pageSize = 6
const showModal = ref(false)
const selectedExam = ref(null)
const openActionMenu = ref(null)
const showDetailsModal = ref(false)

const disciplines = [
  'Estruturas de Dados',
  'Banco de Dados',
  'Engenharia de Software',
  'Redes de Computadores',
  'Matemática Discreta',
]

/*
 * A API de provas ainda não está fechada no backend.
 * Enquanto ela é revisada, esta tela usa dados locais para validar o layout
 * e os comportamentos. Depois, basta substituir exams pelo retorno da API.
 */
const exams = ref([
  {
    id: 1,
    name: 'Prova 1 - Estruturas',
    type: 'Avaliação teórica',
    discipline: 'Estruturas de Dados',
    code: 'CC601',
    date: '2025-05-20',
    time: '09:00',
    content: 'Arrays, Listas, Pilhas, Filas, Recursão',
    status: 'scheduled',
    color: 'purple',
  },
  {
    id: 2,
    name: 'Prova Bimestral',
    type: 'Avaliação teórica',
    discipline: 'Banco de Dados',
    code: 'CC602',
    date: '2025-05-23',
    time: '14:00',
    content: 'Modelagem, SQL, Normalização',
    status: 'scheduled',
    color: 'green',
  },
  {
    id: 3,
    name: 'Prova Prática',
    type: 'Avaliação prática',
    discipline: 'Engenharia de Software',
    code: 'CC603',
    date: '2025-05-28',
    time: '08:00',
    content: 'UML, Casos de uso, Diagramas',
    status: 'scheduled',
    color: 'orange',
  },
  {
    id: 4,
    name: 'Prova de Redes',
    type: 'Avaliação teórica',
    discipline: 'Redes de Computadores',
    code: 'CC604',
    date: '2025-06-02',
    time: '10:00',
    content: 'TCP/IP, Camadas, Endereçamento',
    status: 'scheduled',
    color: 'blue',
  },
  {
    id: 5,
    name: 'Prova 1 - BD',
    type: 'Avaliação teórica',
    discipline: 'Banco de Dados',
    code: 'CC602',
    date: '2025-05-12',
    time: '08:00',
    content: 'Modelagem, MER, SQL Básico',
    status: 'completed',
    color: 'green',
  },
  {
    id: 6,
    name: 'Prova 1 - Lógica',
    type: 'Avaliação teórica',
    discipline: 'Matemática Discreta',
    code: 'CC605',
    date: '2025-05-05',
    time: '08:00',
    content: 'Lógica proposicional, Conjuntos',
    status: 'completed',
    color: 'purple',
  },
  {
    id: 7,
    name: 'Prova Final - Redes',
    type: 'Avaliação teórica',
    discipline: 'Redes de Computadores',
    code: 'CC604',
    date: '2025-06-18',
    time: '14:00',
    content: 'Roteamento, Segurança e protocolos',
    status: 'scheduled',
    color: 'blue',
  },
  {
    id: 8,
    name: 'Avaliação de Projeto',
    type: 'Avaliação prática',
    discipline: 'Engenharia de Software',
    code: 'CC603',
    date: '2025-06-24',
    time: '19:00',
    content: 'Projeto, documentação e apresentação',
    status: 'scheduled',
    color: 'orange',
  },
])

const form = ref({
  name: '',
  discipline: '',
  date: '',
  time: '',
  content: '',
})

const today = new Date('2025-05-12T12:00:00')

const monthLabel = computed(() => new Intl.DateTimeFormat('pt-BR', {
  month: 'long',
  year: 'numeric',
}).format(today).replace(/^./, value => value.toUpperCase()))

const totalExams = computed(() => exams.value.length)
const completedExams = computed(() => exams.value.filter(exam => exam.status === 'completed').length)
const scheduledExams = computed(() => exams.value.filter(exam => exam.status === 'scheduled'))

const upcomingExams = computed(() => [...scheduledExams.value]
  .sort((a, b) => a.date.localeCompare(b.date) || a.time.localeCompare(b.time))
  .slice(0, 3))

const thisMonthExams = computed(() => exams.value.filter(exam => {
  const date = new Date(`${exam.date}T12:00:00`)
  return date.getMonth() === today.getMonth() && date.getFullYear() === today.getFullYear()
}).length)

const filteredExams = computed(() => {
  const term = search.value.trim().toLowerCase()

  return exams.value.filter(exam => {
    const matchesSearch = !term
      || exam.name.toLowerCase().includes(term)
      || exam.discipline.toLowerCase().includes(term)
      || exam.content.toLowerCase().includes(term)

    const matchesDiscipline = selectedDiscipline.value === 'all'
      || exam.discipline === selectedDiscipline.value

    const matchesStatus = selectedStatus.value === 'all'
      || exam.status === selectedStatus.value

    const matchesPeriod = selectedPeriod.value === 'all'
      || (selectedPeriod.value === 'month' && new Date(`${exam.date}T12:00:00`).getMonth() === today.getMonth())
      || (selectedPeriod.value === 'next7' && isWithinNextDays(exam.date, 7))

    return matchesSearch && matchesDiscipline && matchesStatus && matchesPeriod
  }).sort((a, b) => a.date.localeCompare(b.date))
})

const pageCount = computed(() => Math.max(1, Math.ceil(filteredExams.value.length / pageSize)))

const visibleExams = computed(() => filteredExams.value.slice(
  (page.value - 1) * pageSize,
  page.value * pageSize,
))

function isWithinNextDays(dateString, days) {
  const date = new Date(`${dateString}T12:00:00`)
  const diff = Math.round((date - today) / 86400000)
  return diff >= 0 && diff <= days
}

function formatDate(dateString) {
  return new Intl.DateTimeFormat('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  }).format(new Date(`${dateString}T12:00:00`))
}

function formatWeekday(dateString) {
  return new Intl.DateTimeFormat('pt-BR', {
    weekday: 'long',
  }).format(new Date(`${dateString}T12:00:00`))
}

function statusLabel(status) {
  return status === 'completed' ? 'Concluída' : 'Agendada'
}

function statusClass(status) {
  return status === 'completed' ? 'is-completed' : 'is-scheduled'
}

function resetPage() {
  page.value = 1
}

function previousPage() {
  if (page.value > 1) page.value -= 1
}

function nextPage() {
  if (page.value < pageCount.value) page.value += 1
}

function resetFilters() {
  search.value = ''
  selectedDiscipline.value = 'all'
  selectedStatus.value = 'all'
  selectedPeriod.value = 'all'
  resetPage()
}

function openNewExam() {
  form.value = {
    name: '',
    discipline: '',
    date: '',
    time: '',
    content: '',
  }
  showModal.value = true
}

function closeModal() {
  showModal.value = false
}

function toggleActionMenu(examId) {
  openActionMenu.value = openActionMenu.value === examId ? null : examId
}

function viewExam(exam) {
  selectedExam.value = exam
  openActionMenu.value = null
  showDetailsModal.value = true
}

function closeDetailsModal() {
  showDetailsModal.value = false
  selectedExam.value = null
}

function toggleExamStatus(exam) {
  exam.status = exam.status === 'completed' ? 'scheduled' : 'completed'
  openActionMenu.value = null
}

function deleteExam(examId) {
  exams.value = exams.value.filter(exam => exam.id !== examId)
  openActionMenu.value = null
  if (page.value > pageCount.value) page.value = pageCount.value
}

function saveExam() {
  if (!form.value.name || !form.value.discipline || !form.value.date || !form.value.time) return

  const colorMap = {
    'Estruturas de Dados': 'purple',
    'Banco de Dados': 'green',
    'Engenharia de Software': 'orange',
    'Redes de Computadores': 'blue',
    'Matemática Discreta': 'purple',
  }

  exams.value.push({
    id: Date.now(),
    name: form.value.name,
    type: 'Avaliação teórica',
    discipline: form.value.discipline,
    code: '—',
    date: form.value.date,
    time: form.value.time,
    content: form.value.content || 'Conteúdo não informado',
    status: 'scheduled',
    color: colorMap[form.value.discipline] || 'purple',
  })

  showModal.value = false
  resetPage()
}

const calendarDays = computed(() => {
  const year = today.getFullYear()
  const month = today.getMonth()
  const firstDay = new Date(year, month, 1).getDay()
  const daysInMonth = new Date(year, month + 1, 0).getDate()

  return Array.from({ length: firstDay + daysInMonth }, (_, index) => {
    if (index < firstDay) {
      return { number: '', key: `empty-${index}`, state: '' }
    }

    const number = index - firstDay + 1
    const iso = `${year}-${String(month + 1).padStart(2, '0')}-${String(number).padStart(2, '0')}`
    const exam = exams.value.find(item => item.date === iso)
    const state = number === 12 ? 'today' : exam?.status === 'scheduled' ? 'upcoming' : exam ? 'important' : ''

    return { number, key: iso, state }
  })
})
</script>

<template>
  <section class="exams-page" aria-labelledby="exams-title">
    <header class="exams-header">
      <div class="exams-heading">
        <span class="exams-title-icon" aria-hidden="true">
          <svg viewBox="0 0 24 24">
            <rect x="5" y="3" width="14" height="18" rx="2" />
            <path d="M9 3v3h6V3M8 11h8M8 15h5" />
          </svg>
        </span>
        <div>
          <h1 id="exams-title">Provas</h1>
          <p>Acompanhe suas provas e revise os conteúdos.</p>
        </div>
      </div>

      <div class="exams-header-actions">
        <button class="exams-icon-button" type="button" aria-label="Notificações">
          <svg viewBox="0 0 24 24"><path d="M18 9a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9M10 21h4" /></svg>
          <span class="notification-badge">3</span>
        </button>
        <button class="exams-button is-primary" type="button" @click="openNewExam">
          <span aria-hidden="true">＋</span>
          Nova prova
        </button>
      </div>
    </header>

    <div class="exams-summary-grid">
      <article class="exams-summary-card is-purple">
        <span class="summary-icon" aria-hidden="true">
          <svg viewBox="0 0 24 24"><rect x="4" y="5" width="16" height="16" rx="2" /><path d="M8 3v4m8-4v4M4 10h16" /></svg>
        </span>
        <div>
          <p>Total de provas</p>
          <strong>{{ totalExams }}</strong>
          <small>Todas as disciplinas</small>
        </div>
      </article>

      <article class="exams-summary-card is-green">
        <span class="summary-icon" aria-hidden="true">
          <svg viewBox="0 0 24 24"><rect x="4" y="5" width="16" height="16" rx="2" /><path d="M8 3v4m8-4v4M4 10h16M9 15l2 2 4-4" /></svg>
        </span>
        <div>
          <p>Próximas provas</p>
          <strong>{{ upcomingExams.length }}</strong>
          <small>Nos próximos 7 dias</small>
        </div>
      </article>

      <article class="exams-summary-card is-orange">
        <span class="summary-icon" aria-hidden="true">
          <svg viewBox="0 0 24 24"><path class="hourglass-shape" d="M6.5 3h11M6.5 21h11M8 3h8c0 4-1.6 5.7-4 7.5-2.4 1.8-4 3.5-4 7.5h8c0-4-1.6-5.7-4-7.5C9.6 8.7 8 7 8 3z" /><path class="hourglass-sand" d="M9.1 5.1h5.8c-.5 1.9-1.4 2.8-2.9 4-.4-.3-.8-.6-1.2-.9-1-.8-1.5-1.6-1.7-3.1zM9.1 18.9h5.8c-.5-1.9-1.4-2.8-2.9-4-.4.3-.8.6-1.2.9-1 .8-1.5 1.6-1.7 3.1z" /></svg>
        </span>
        <div>
          <p>Este mês</p>
          <strong>{{ thisMonthExams }}</strong>
          <small>{{ monthLabel }}</small>
        </div>
      </article>

      <article class="exams-summary-card is-blue">
        <span class="summary-icon" aria-hidden="true">
          <svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="9" /><path d="m8 12 3 3 5-6" /></svg>
        </span>
        <div>
          <p>Provas concluídas</p>
          <strong>{{ completedExams }}</strong>
          <small>{{ totalExams ? Math.round((completedExams / totalExams) * 100) : 0 }}% do total</small>
        </div>
      </article>
    </div>

    <div class="exams-layout">
      <div class="exams-main-column">
        <section class="exams-panel">
          <div class="exams-filters">
            <label class="exams-search">
              <span class="sr-only">Buscar prova</span>
              <svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="11" cy="11" r="7" /><path d="m20 20-3.5-3.5" /></svg>
              <input v-model="search" type="search" placeholder="Buscar prova..." @input="resetPage">
            </label>

            <label>
              <span>Disciplinas</span>
              <select v-model="selectedDiscipline" @change="resetPage">
                <option value="all">Todas</option>
                <option v-for="discipline in disciplines" :key="discipline" :value="discipline">{{ discipline }}</option>
              </select>
            </label>

            <label>
              <span>Status</span>
              <select v-model="selectedStatus" @change="resetPage">
                <option value="all">Todos</option>
                <option value="scheduled">Agendadas</option>
                <option value="completed">Concluídas</option>
              </select>
            </label>

            <label>
              <span>Período</span>
              <select v-model="selectedPeriod" @change="resetPage">
                <option value="all">Todos</option>
                <option value="next7">Próximos 7 dias</option>
                <option value="month">Este mês</option>
              </select>
            </label>
          </div>

          <div class="exams-table-wrap">
            <table class="exams-table">
              <thead>
                <tr>
                  <th>Prova</th>
                  <th>Disciplina</th>
                  <th>Data</th>
                  <th>Conteúdo</th>
                  <th>Status</th>
                  <th>Ações</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="exam in visibleExams" :key="exam.id">
                  <td>
                    <div class="exam-name">
                      <span class="exam-row-icon" :class="`is-${exam.color}`" aria-hidden="true">
                        <svg viewBox="0 0 24 24"><rect x="5" y="3" width="14" height="18" rx="2" /><path d="M9 3v3h6V3M8 11h8M8 15h5" /></svg>
                      </span>
                      <div>
                        <strong>{{ exam.name }}</strong>
                        <small>{{ exam.type }}</small>
                      </div>
                    </div>
                  </td>
                  <td>
                    <strong class="discipline-name">{{ exam.discipline }}</strong>
                    <small>{{ exam.code }}</small>
                  </td>
                  <td>
                    <strong>{{ formatDate(exam.date) }}</strong>
                    <small>{{ formatWeekday(exam.date) }}</small>
                  </td>
                  <td class="content-cell">{{ exam.content }}</td>
                  <td>
                    <span class="status-pill" :class="statusClass(exam.status)">
                      <svg v-if="exam.status === 'scheduled'" viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="8.5" /><path d="M12 10v5" /><circle cx="12" cy="7.2" r=".7" fill="currentColor" stroke="none" /></svg>
                      <svg v-else viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="8.5" /><path d="m8.5 12 2.3 2.4 4.8-5" /></svg>
                      {{ statusLabel(exam.status) }}
                    </span>
                  </td>
                  <td>
                    <div class="row-actions">
                      <button type="button" aria-label="Visualizar prova" title="Visualizar prova" @click="viewExam(exam)">
                        <svg viewBox="0 0 24 24"><path d="M2.5 12s3.5-6 9.5-6 9.5 6 9.5 6-3.5 6-9.5 6-9.5-6-9.5-6Z" /><circle cx="12" cy="12" r="2.5" /></svg>
                      </button>
                      <div class="row-action-menu-wrap">
                        <button
                          type="button"
                          aria-label="Mais opções"
                          title="Mais opções"
                          :aria-expanded="openActionMenu === exam.id"
                          @click="toggleActionMenu(exam.id)"
                        >
                          <svg viewBox="0 0 24 24"><circle cx="5" cy="12" r="1" /><circle cx="12" cy="12" r="1" /><circle cx="19" cy="12" r="1" /></svg>
                        </button>
                        <div v-if="openActionMenu === exam.id" class="row-action-menu">
                          <button type="button" @click="viewExam(exam)">Visualizar</button>
                          <button type="button" @click="toggleExamStatus(exam)">
                            {{ exam.status === 'completed' ? 'Marcar como agendada' : 'Marcar como concluída' }}
                          </button>
                          <button type="button" class="is-danger" @click="deleteExam(exam.id)">Excluir</button>
                        </div>
                      </div>
                    </div>
                  </td>
                </tr>
                <tr v-if="visibleExams.length === 0">
                  <td colspan="7" class="empty-row">
                    <span aria-hidden="true">🔎</span>
                    <strong>Nenhuma prova encontrada</strong>
                    <small>Tente ajustar os filtros ou cadastrar uma nova prova.</small>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <footer class="exams-table-footer">
            <p>Mostrando {{ visibleExams.length }} de {{ filteredExams.length }} provas</p>
            <nav aria-label="Paginação">
              <button type="button" :disabled="page === 1" @click="previousPage">‹</button>
              <button v-for="number in pageCount" :key="number" type="button" :class="{ active: page === number }" @click="page = number">{{ number }}</button>
              <button type="button" :disabled="page === pageCount" @click="nextPage">›</button>
            </nav>
          </footer>
        </section>

        <aside class="exams-tip">
          <span class="tip-icon" aria-hidden="true">💡</span>
          <div>
            <strong>Dica</strong>
            <p>Revise o conteúdo com antecedência e consulte o simulador de notas para acompanhar seu desempenho.</p>
          </div>
          <button type="button" @click="emit('navigate', 'simulator')">Simulador de Notas</button>
        </aside>
      </div>

      <aside class="exams-side-column">
        <section class="upcoming-card">
          <header>
            <strong>Próximas provas</strong>
            <button type="button" @click="selectedStatus = 'scheduled'; resetPage()">Ver todas</button>
          </header>

          <button v-for="(exam, index) in upcomingExams" :key="exam.id" class="upcoming-item" type="button">
            <span class="upcoming-number" :class="`is-${exam.color}`">{{ index + 1 }}</span>
            <span class="upcoming-details">
              <strong>{{ exam.name }}</strong>
              <small>{{ exam.discipline }}</small>
            </span>
            <span class="upcoming-date">
              <strong>{{ new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: '2-digit' }).format(new Date(`${exam.date}T12:00:00`)) }}</strong>
              <small>{{ exam.time }}</small>
            </span>
          </button>
        </section>

        <section class="mini-calendar">
          <header>
            <button type="button" aria-label="Mês anterior">‹</button>
            <strong>{{ monthLabel }}</strong>
            <button type="button" aria-label="Próximo mês">›</button>
          </header>

          <div class="calendar-weekdays">
            <span v-for="day in ['Dom', 'Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb']" :key="day">{{ day }}</span>
          </div>

          <div class="calendar-grid">
            <span
              v-for="day in calendarDays"
              :key="day.key"
              :class="['calendar-day', day.state]"
            >{{ day.number }}</span>
          </div>

          <div class="calendar-legend">
            <span><i class="legend-dot is-today"></i>Hoje</span>
            <span><i class="legend-dot is-upcoming"></i>Próximas provas</span>
            <span><i class="legend-dot is-important"></i>Provas importantes</span>
          </div>
        </section>
      </aside>
    </div>

    <div v-if="showDetailsModal && selectedExam" class="exam-modal-overlay" @click.self="closeDetailsModal">
      <section class="exam-modal exam-details-modal" role="dialog" aria-modal="true" aria-labelledby="exam-details-title">
        <header>
          <div>
            <span class="modal-kicker">Detalhes da prova</span>
            <h2 id="exam-details-title">{{ selectedExam.name }}</h2>
          </div>
          <button type="button" aria-label="Fechar" @click="closeDetailsModal">×</button>
        </header>

        <div class="exam-details-grid">
          <div>
            <span>Disciplina</span>
            <strong>{{ selectedExam.discipline }}</strong>
          </div>
          <div>
            <span>Data</span>
            <strong>{{ formatDate(selectedExam.date) }}</strong>
          </div>
          <div>
            <span>Horário</span>
            <strong>{{ selectedExam.time }}</strong>
          </div>
          <div>
            <span>Status</span>
            <strong>{{ statusLabel(selectedExam.status) }}</strong>
          </div>
          <div class="exam-details-full">
            <span>Conteúdo</span>
            <strong>{{ selectedExam.content }}</strong>
          </div>
        </div>

        <footer>
          <button class="exams-button is-secondary" type="button" @click="closeDetailsModal">Fechar</button>
        </footer>
      </section>
    </div>

    <div v-if="showModal" class="exam-modal-overlay" @click.self="closeModal">
      <section class="exam-modal" role="dialog" aria-modal="true" aria-labelledby="new-exam-title">
        <header>
          <div>
            <span class="modal-kicker">Nova avaliação</span>
            <h2 id="new-exam-title">Cadastrar prova</h2>
          </div>
          <button type="button" aria-label="Fechar" @click="closeModal">×</button>
        </header>

        <form @submit.prevent="saveExam">
          <label>
            Nome da prova
            <input v-model.trim="form.name" required placeholder="Ex.: Prova 2 - Estruturas">
          </label>

          <div class="modal-grid">
            <label>
              Disciplina
              <select v-model="form.discipline" required>
                <option value="" disabled>Selecione</option>
                <option v-for="discipline in disciplines" :key="discipline" :value="discipline">{{ discipline }}</option>
              </select>
            </label>
            <label>
              Data
              <input v-model="form.date" type="date" required>
            </label>
            <label>
              Horário
              <input v-model="form.time" type="time" required>
            </label>
          </div>

          <label>
            Conteúdo
            <textarea v-model.trim="form.content" rows="3" placeholder="Conteúdos que serão cobrados"></textarea>
          </label>

          <footer>
            <button class="exams-button is-secondary" type="button" @click="closeModal">Cancelar</button>
            <button class="exams-button is-primary" type="submit">Salvar prova</button>
          </footer>
        </form>
      </section>
    </div>
  </section>
</template>

<style scoped>
.exams-page {
  --purple: #6330e0;
  --purple-dark: #5c20de;
  --text: #151a2d;
  --muted: #73798e;
  --border: #ebeaf1;
  --surface: #fff;
  display: grid;
  gap: 16px;
  min-width: 0;
}

.exams-header,
.exams-heading,
.exams-header-actions {
  align-items: center;
  display: flex;
}

.exams-header {
  justify-content: space-between;
  margin-bottom: 2px;
}

.exams-heading { gap: 14px; }
.exams-title-icon {
  align-items: center;
  background: #f0ebff;
  border-radius: 11px;
  color: var(--purple);
  display: flex;
  flex: 0 0 46px;
  height: 46px;
  justify-content: center;
}
.exams-title-icon svg { fill: none; height: 25px; stroke: currentColor; stroke-linecap: round; stroke-linejoin: round; stroke-width: 1.7; width: 25px; }
.exams-heading h1 { color: #13182a; font-size: 1.75rem; font-weight: 800; letter-spacing: -.04em; margin: 0 0 4px; }
.exams-heading p { color: #687086; font-size: .78rem; margin: 0; }

.exams-header-actions { gap: 12px; }
.exams-icon-button {
  align-items: center;
  background: transparent;
  border: 0;
  color: #657087;
  display: flex;
  height: 40px;
  justify-content: center;
  position: relative;
  width: 40px;
}
.exams-icon-button svg { fill: none; height: 22px; stroke: currentColor; stroke-linecap: round; stroke-linejoin: round; stroke-width: 1.6; width: 22px; }
.notification-badge {
  align-items: center;
  background: #6330e0;
  border: 2px solid #f5f6fb;
  border-radius: 50%;
  color: white;
  display: flex;
  font-size: .58rem;
  font-weight: 800;
  height: 17px;
  justify-content: center;
  position: absolute;
  right: 1px;
  top: 1px;
  width: 17px;
}

.exams-button {
  align-items: center;
  border: 0;
  border-radius: 7px;
  display: inline-flex;
  font-size: .74rem;
  font-weight: 750;
  gap: 7px;
  justify-content: center;
  padding: 11px 16px;
  transition: transform .15s, box-shadow .15s, background .15s;
}
.exams-button.is-primary { background: linear-gradient(100deg, #5c20de, #741dff); box-shadow: 0 8px 19px rgba(102, 36, 225, .2); color: #fff; }
.exams-button.is-primary:hover { box-shadow: 0 11px 24px rgba(102, 36, 225, .28); transform: translateY(-1px); }
.exams-button.is-secondary { background: #f3f1f8; color: #4c4560; }

.exams-summary-grid { display: grid; gap: 14px; grid-template-columns: repeat(4, minmax(0, 1fr)); }
.exams-summary-card {
  align-items: center;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 12px;
  box-shadow: 0 5px 16px rgba(30, 36, 65, .035);
  display: flex;
  gap: 13px;
  min-height: 112px;
  padding: 17px;
}
.summary-icon {
  align-items: center;
  background: #f1edff;
  border-radius: 50%;
  color: #6739e7;
  display: flex;
  flex: 0 0 48px;
  height: 48px;
  justify-content: center;
}
.summary-icon svg { fill: none; height: 24px; stroke: currentColor; stroke-linecap: round; stroke-linejoin: round; stroke-width: 1.8; width: 24px; }
.summary-icon svg .hourglass-sand { fill: currentColor; stroke: none; }
.exams-summary-card p { color: #596078; font-size: .67rem; margin: 0; }
.exams-summary-card strong { color: #171c30; display: block; font-size: 1.28rem; line-height: 1; margin: 7px 0 5px; }
.exams-summary-card small { color: #858b9e; display: block; font-size: .61rem; }
.exams-summary-card.is-green .summary-icon { background: #e8f8ef; color: #2daf68; }
.exams-summary-card.is-orange .summary-icon { background: #fff0e2; color: #ee831e; }
.exams-summary-card.is-blue .summary-icon { background: #eaf2ff; color: #347bd8; }

.exams-layout { display: grid; gap: 16px; grid-template-columns: minmax(0, 1fr) 292px; align-items: start; }
.exams-main-column { min-width: 0; overflow: hidden; }
.exams-panel,
.upcoming-card,
.mini-calendar {
  background: #fff;
  border: 1px solid var(--border);
  border-radius: 12px;
  box-shadow: 0 5px 16px rgba(30, 36, 65, .035);
}

.exams-filters {
  align-items: start;
  border-bottom: 1px solid #f0eff4;
  display: grid;
  gap: 14px;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  min-width: 0;
  padding: 16px;
}
.exams-filters label {
  color: #596078;
  display: grid;
  font-size: .61rem;
  font-weight: 700;
  gap: 6px;
  min-width: 0;
  width: 100%;
}
.exams-filters label > span {
  height: 15px;
  line-height: 15px;
}
.exams-search { align-self: end; height: 32px; min-width: 0; position: relative; }
.exams-search svg { color: #8b91a1; fill: none; height: 17px; left: 11px; position: absolute; stroke: currentColor; stroke-linecap: round; stroke-width: 1.7; top: 50%; transform: translateY(-50%); width: 17px; }
.exams-search input {
  background: #fff;
  border: 1px solid #dedfe5;
  border-radius: 6px;
  box-shadow: 0 1px 3px rgba(32, 37, 56, .06);
  box-sizing: border-box;
  height: 32px;
  padding-left: 34px;
  width: 100%;
}
.exams-filters select {
  appearance: auto;
  background: #fff;
  border: 1px solid #dedfe5;
  border-radius: 6px;
  box-shadow: 0 1px 3px rgba(32, 37, 56, .06);
  box-sizing: border-box;
  height: 32px;
  min-height: 32px;
  padding: 0 10px;
}
.exams-filters select:focus,
.exams-search input:focus { border-color: #8b6cf1; box-shadow: 0 0 0 3px rgba(99, 48, 224, .08); }

.exams-table-wrap { overflow-x: auto; overflow-y: visible; }
.exams-table { border-collapse: collapse; min-width: 870px; width: 100%; }
.exams-table th {
  background: #fbfbfd;
  color: #6d7386;
  font-size: .59rem;
  font-weight: 750;
  padding: 13px 11px;
  text-align: left;
  white-space: nowrap;
}
.exams-table td { border-top: 1px solid #f0eff4; color: #454b60; font-size: .65rem; padding: 13px 11px; vertical-align: middle; }
.exams-table tbody tr:hover { background: #fcfbff; }
.exam-name { align-items: center; display: flex; gap: 10px; min-width: 190px; }
.exam-row-icon {
  align-items: center;
  background: #f1edff;
  border-radius: 9px;
  color: #6330e0;
  display: flex;
  flex: 0 0 34px;
  height: 34px;
  justify-content: center;
}
.exam-row-icon svg { fill: none; height: 18px; stroke: currentColor; stroke-linecap: round; stroke-linejoin: round; stroke-width: 1.7; width: 18px; }
.exam-row-icon.is-green { background: #e8f8ef; color: #2daf68; }
.exam-row-icon.is-orange { background: #fff0e2; color: #ee831e; }
.exam-row-icon.is-blue { background: #eaf2ff; color: #347bd8; }
.exam-name strong,
.discipline-name { color: #202538; display: block; font-size: .68rem; font-weight: 750; }
.exam-name small,
.exams-table td > small,
.exams-table td small { color: #858b9e; display: block; font-size: .57rem; margin-top: 3px; }
.content-cell { max-width: 180px; line-height: 1.35; }
.status-pill {
  align-items: center;
  border-radius: 999px;
  display: inline-flex;
  font-size: .57rem;
  font-weight: 750;
  gap: 5px;
  padding: 5px 9px;
  white-space: nowrap;
}
.status-pill { background: #f1edff; color: #6330e0; }
.status-pill.is-completed { background: #e8f8ef; color: #20945a; }
.status-pill svg { fill: none; height: 13px; stroke: currentColor; stroke-linecap: round; stroke-linejoin: round; stroke-width: 1.8; width: 13px; }
.row-actions { align-items: center; display: flex; gap: 3px; }
.row-actions button { background: transparent; border: 0; color: #7d8394; height: 30px; padding: 5px; width: 30px; }
.row-actions button:hover { background: #f2efff; border-radius: 6px; color: #6330e0; }
.row-actions svg { fill: none; height: 17px; stroke: currentColor; stroke-linecap: round; stroke-linejoin: round; stroke-width: 1.6; width: 17px; }
.row-action-menu-wrap { position: relative; }
.row-action-menu {
  background: #fff;
  border: 1px solid #e4e2ec;
  border-radius: 8px;
  box-shadow: 0 10px 28px rgba(20, 24, 48, .12);
  min-width: 176px;
  padding: 5px;
  position: absolute;
  right: 0;
  top: 34px;
  z-index: 20;
}
.row-action-menu button {
  align-items: center;
  border-radius: 6px;
  color: #42485b;
  display: flex;
  font-size: .62rem;
  height: auto;
  justify-content: flex-start;
  padding: 8px 9px;
  width: 100%;
}
.row-action-menu button:hover { background: #f6f4fb; color: #6330e0; }
.row-action-menu button.is-danger { color: #c54848; }
.row-action-menu button.is-danger:hover { background: #fff1f1; color: #b83e3e; }

.exams-table-footer { align-items: center; display: flex; justify-content: space-between; padding: 13px 16px; }
.exams-table-footer p { color: #747a8d; font-size: .61rem; margin: 0; }
.exams-table-footer nav { display: flex; gap: 4px; }
.exams-table-footer button {
  background: #fff;
  border: 1px solid #dedee7;
  border-radius: 6px;
  color: #535a6e;
  font-size: .65rem;
  height: 30px;
  min-width: 30px;
}
.exams-table-footer button.active { background: #6330e0; border-color: #6330e0; color: #fff; }
.exams-table-footer button:disabled { cursor: not-allowed; opacity: .4; }

.exams-side-column { display: grid; gap: 16px; }
.upcoming-card { padding: 16px; }
.upcoming-card header,
.mini-calendar header { align-items: center; display: flex; justify-content: space-between; }
.upcoming-card header { margin-bottom: 12px; }
.upcoming-card header strong { color: #202538; font-size: .75rem; }
.upcoming-card header button { background: none; border: 0; color: #6330e0; font-size: .61rem; font-weight: 750; }
.upcoming-item { align-items: center; background: transparent; border: 0; border-top: 1px solid #f0eff4; display: grid; gap: 9px; grid-template-columns: 31px 1fr auto; padding: 11px 0; text-align: left; width: 100%; }
.upcoming-number { align-items: center; border-radius: 7px; display: flex; font-size: .66rem; font-weight: 800; height: 30px; justify-content: center; width: 30px; }
.upcoming-number.is-purple { background: #f0ebff; color: #6330e0; }
.upcoming-number.is-green { background: #e8f8ef; color: #2daf68; }
.upcoming-number.is-orange { background: #fff0e2; color: #ee831e; }
.upcoming-number.is-blue { background: #eaf2ff; color: #347bd8; }
.upcoming-details strong { color: #252a3c; display: block; font-size: .64rem; }
.upcoming-details small,
.upcoming-date small { color: #858b9e; display: block; font-size: .56rem; margin-top: 3px; }
.upcoming-date { text-align: right; }
.upcoming-date strong { color: #252a3c; display: block; font-size: .61rem; }

.mini-calendar { padding: 16px; }
.mini-calendar header strong { color: #252a3c; font-size: .76rem; }
.mini-calendar header button { background: none; border: 0; color: #747a8d; font-size: 1.35rem; line-height: 1; padding: 3px 6px; }
.calendar-weekdays,
.calendar-grid { display: grid; grid-template-columns: repeat(7, 1fr); text-align: center; }
.calendar-weekdays { color: #7f8596; font-size: .55rem; margin: 17px 0 8px; }
.calendar-grid { gap: 5px 2px; }
.calendar-day { align-items: center; border-radius: 50%; color: #52586c; display: flex; font-size: .58rem; height: 27px; justify-content: center; margin: auto; width: 27px; }
.calendar-day.today { background: #6330e0; color: #fff; font-weight: 800; }
.calendar-day.upcoming { background: #e8f8ef; color: #20945a; font-weight: 750; }
.calendar-day.important { background: #fff0e2; color: #d87412; font-weight: 750; }
.calendar-legend { border-top: 1px solid #f0eff4; display: grid; gap: 7px; margin-top: 14px; padding-top: 13px; }
.calendar-legend span { align-items: center; color: #777d90; display: flex; font-size: .54rem; gap: 6px; }
.legend-dot { border-radius: 50%; display: inline-block; height: 6px; width: 6px; }
.legend-dot.is-today { background: #6330e0; }
.legend-dot.is-upcoming { background: #2daf68; }
.legend-dot.is-important { background: #ee831e; }

.exams-tip { align-items: center; background: #f2efff; border-radius: 9px; display: flex; gap: 12px; margin-top: 15px; padding: 12px 16px; }
.tip-icon { align-items: center; background: #e5dcff; border-radius: 50%; display: flex; flex: 0 0 34px; height: 34px; justify-content: center; }
.exams-tip div { min-width: 0; }
.exams-tip strong { color: #30364a; font-size: .66rem; }
.exams-tip p { color: #6c7287; font-size: .59rem; margin: 3px 0 0; }
.exams-tip button { background: #fff; border: 1px solid #ddd7ee; border-radius: 6px; color: #6330e0; font-size: .59rem; font-weight: 750; margin-left: auto; padding: 8px 11px; white-space: nowrap; }

.exam-modal-overlay { align-items: center; background: rgba(8, 13, 27, .58); display: flex; inset: 0; justify-content: center; padding: 20px; position: fixed; z-index: 100; }
.exam-modal { background: #fff; border-radius: 14px; box-shadow: 0 24px 70px rgba(10, 15, 35, .25); max-width: 620px; padding: 22px; width: 100%; }
.exam-modal > header { align-items: flex-start; display: flex; justify-content: space-between; margin-bottom: 18px; }
.modal-kicker { color: #6330e0; font-size: .6rem; font-weight: 800; text-transform: uppercase; }
.exam-modal h2 { color: #181d30; font-size: 1.2rem; margin: 4px 0 0; }
.exam-modal > header button { background: #f4f2f8; border: 0; border-radius: 7px; color: #646a7b; font-size: 1.3rem; height: 32px; width: 32px; }
.exam-modal form { display: grid; gap: 13px; }
.exam-modal label { color: #555b6e; display: grid; font-size: .65rem; font-weight: 700; gap: 6px; }
.exam-modal input,
.exam-modal select,
.exam-modal textarea { border: 1px solid #dedfe7; border-radius: 7px; color: #272c40; font: inherit; font-size: .72rem; outline: none; padding: 10px 11px; }
.exam-modal textarea { resize: vertical; }
.exam-modal input:focus,
.exam-modal select:focus,
.exam-modal textarea:focus { border-color: #8b6cf1; box-shadow: 0 0 0 3px rgba(99, 48, 224, .08); }
.modal-grid { display: grid; gap: 12px; grid-template-columns: repeat(2, 1fr); }
.exam-modal footer { display: flex; justify-content: flex-end; gap: 8px; margin-top: 4px; }
.exam-details-grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}
.exam-details-grid > div {
  background: #faf9fd;
  border: 1px solid #eeeaf5;
  border-radius: 8px;
  padding: 11px 12px;
}
.exam-details-grid span {
  color: #858b9e;
  display: block;
  font-size: .58rem;
  margin-bottom: 5px;
}
.exam-details-grid strong {
  color: #292e41;
  display: block;
  font-size: .68rem;
  line-height: 1.35;
}
.exam-details-full { grid-column: 1 / -1; }


.sr-only { height: 1px; margin: -1px; overflow: hidden; position: absolute; width: 1px; clip: rect(0,0,0,0); }
@media (max-width: 1050px) {
  .exams-layout { grid-template-columns: 1fr; }
  .exams-side-column { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media (max-width: 980px) {
  .exams-summary-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .exams-filters { grid-template-columns: 1fr 1fr; }
}
@media (max-width: 650px) {
  .exams-header { align-items: flex-start; gap: 12px; }
  .exams-header-actions { flex-shrink: 0; }
  .exams-summary-grid { grid-template-columns: 1fr; }
  .exams-filters { grid-template-columns: 1fr; }
  .exams-side-column { grid-template-columns: 1fr; }
  .exam-details-grid { grid-template-columns: 1fr; }
  .exam-details-full { grid-column: auto; }
  .exams-tip { align-items: flex-start; flex-wrap: wrap; }
  .exams-tip button { margin-left: 46px; }
  .modal-grid { grid-template-columns: 1fr; }
}
</style>
