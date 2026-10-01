<script setup>
import { onBeforeUnmount, ref, watch } from 'vue'
import { formatDate, formatWeekday, statusClass, statusLabel } from './examPresentation.js'

const props = defineProps({
  exams: { type: Array, required: true },
  filteredCount: { type: Number, required: true },
  page: { type: Number, required: true },
  pageCount: { type: Number, required: true },
  openActionMenu: { type: [String, Number], default: null },
  busyExamIds: { type: Set, required: true },
  now: { type: Date, required: true },
})

const emit = defineEmits([
  'view',
  'edit',
  'toggle-status',
  'delete',
  'toggle-menu',
  'previous-page',
  'next-page',
  'go-to-page',
])

function isExamBusy(id) {
  return props.busyExamIds.has(id)
}

// O menu "⋯" fica fixo na tela, junto do botão: dentro da tabela (que tem rolagem própria) ele era
// cortado. Abre para baixo quando cabe; senão, para cima.
const MENU_HEIGHT = 180
const MENU_GAP = 4
const menuStyle = ref({})

function toggleMenu(event, examId) {
  if (props.openActionMenu !== examId) {
    const rect = event.currentTarget.getBoundingClientRect()
    const opensUp = window.innerHeight - rect.bottom < MENU_HEIGHT + MENU_GAP && rect.top > MENU_HEIGHT
    menuStyle.value = {
      right: `${Math.max(8, window.innerWidth - rect.right)}px`,
      ...(opensUp
        ? { bottom: `${window.innerHeight - rect.top + MENU_GAP}px` }
        : { top: `${rect.bottom + MENU_GAP}px` }),
    }
  }
  emit('toggle-menu', examId)
}

// Com o menu fixo, rolar a página o deixaria longe do botão: fecha.
function closeMenuOnScroll() {
  if (props.openActionMenu !== null) emit('toggle-menu', props.openActionMenu)
}

watch(() => props.openActionMenu, (open) => {
  const method = open === null ? 'removeEventListener' : 'addEventListener'
  window[method]('scroll', closeMenuOnScroll, true)
  window[method]('resize', closeMenuOnScroll)
})

onBeforeUnmount(() => {
  window.removeEventListener('scroll', closeMenuOnScroll, true)
  window.removeEventListener('resize', closeMenuOnScroll)
})
</script>

<template>
  <div class="exams-table-wrap">
    <table class="exams-table" role="table">
      <thead role="rowgroup">
        <tr role="row">
          <th role="columnheader">Prova</th>
          <th role="columnheader">Disciplina</th>
          <th role="columnheader">Data</th>
          <th role="columnheader">Conteúdo</th>
          <th role="columnheader">Status</th>
          <th role="columnheader">Ações</th>
        </tr>
      </thead>
      <tbody role="rowgroup">
        <tr v-for="exam in exams" :key="exam.id" role="row">
          <td role="cell">
            <div class="exam-name">
              <span class="exam-row-icon" :class="`is-${exam.color}`" aria-hidden="true">
                <svg viewBox="0 0 24 24"><rect x="5" y="3" width="14" height="18" rx="2" /><path d="M9 3v3h6V3M8 11h8M8 15h5" /></svg>
              </span>
              <div>
                <strong>{{ exam.title }}</strong>
                <small>Avaliação</small>
              </div>
            </div>
          </td>
          <td role="cell" data-label="Disciplina">
            <strong class="discipline-name">{{ exam.disciplineName }}</strong>
          </td>
          <td role="cell" data-label="Data">
            <strong>{{ formatDate(exam.dueDate) }}</strong>
            <small>{{ formatWeekday(exam.dueDate) }}</small>
          </td>
          <td role="cell" class="content-cell" data-label="Conteúdo">
            <span class="content-text">{{ exam.description || 'Conteúdo não informado' }}</span>
          </td>
          <td role="cell" data-label="Status">
            <span class="status-pill" :class="statusClass(exam, now)">
              <svg v-if="exam.status !== 'COMPLETED'" viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="8.5" /><path d="M12 10v5" /><circle cx="12" cy="7.2" r=".7" fill="currentColor" stroke="none" /></svg>
              <svg v-else viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="8.5" /><path d="m8.5 12 2.3 2.4 4.8-5" /></svg>
              {{ statusLabel(exam, now) }}
            </span>
          </td>
          <td role="cell" class="actions-cell">
            <div class="row-actions">
              <button type="button" aria-label="Visualizar prova" data-tooltip="Visualizar" @click="emit('view', exam)">
                <svg viewBox="0 0 24 24"><path d="M2.5 12s3.5-6 9.5-6 9.5 6 9.5 6-3.5 6-9.5 6-9.5-6-9.5-6Z" /><circle cx="12" cy="12" r="2.5" /></svg>
              </button>
              <div class="row-action-menu-wrap">
                <button
                  type="button"
                  aria-label="Mais opções"
                  data-tooltip="Mais opções"
                  :aria-expanded="openActionMenu === exam.id"
                  @click="toggleMenu($event, exam.id)"
                >
                  <svg viewBox="0 0 24 24"><circle cx="5" cy="12" r="1" /><circle cx="12" cy="12" r="1" /><circle cx="19" cy="12" r="1" /></svg>
                </button>
                <div v-if="openActionMenu === exam.id" class="row-action-menu" :style="menuStyle">
                  <button type="button" @click="emit('view', exam)">Visualizar</button>
                  <button type="button" @click="emit('edit', exam)">Editar</button>
                  <button type="button" :disabled="isExamBusy(exam.id)" @click="emit('toggle-status', exam)">
                    {{ exam.status === 'COMPLETED' ? 'Marcar como agendada' : 'Marcar como concluída' }}
                  </button>
                  <button type="button" class="is-danger" @click="emit('delete', exam)">Excluir</button>
                </div>
              </div>
            </div>
          </td>
        </tr>
        <tr v-if="exams.length === 0" role="row">
          <td role="cell" colspan="6" class="empty-row">
            <div class="app-state-card is-empty"><span aria-hidden="true">🔎</span><h2>Nenhuma prova encontrada</h2><p>Tente ajustar os filtros ou cadastrar uma nova prova.</p></div>
          </td>
        </tr>
      </tbody>
    </table>
  </div>

  <footer class="exams-table-footer">
    <p>Mostrando {{ exams.length }} de {{ filteredCount }} provas</p>
    <nav aria-label="Paginação">
      <button type="button" :disabled="page === 1" @click="emit('previous-page')">‹</button>
      <button v-for="number in pageCount" :key="number" type="button" :class="{ active: page === number }" @click="emit('go-to-page', number)">{{ number }}</button>
      <button type="button" :disabled="page === pageCount" @click="emit('next-page')">›</button>
    </nav>
  </footer>
</template>
