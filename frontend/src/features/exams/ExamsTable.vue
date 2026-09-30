<script setup>
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
</script>

<template>
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
        <tr v-for="exam in exams" :key="exam.id">
          <td>
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
          <td>
            <strong class="discipline-name">{{ exam.disciplineName }}</strong>
          </td>
          <td>
            <strong>{{ formatDate(exam.dueDate) }}</strong>
            <small>{{ formatWeekday(exam.dueDate) }}</small>
          </td>
          <td class="content-cell">{{ exam.description || 'Conteúdo não informado' }}</td>
          <td>
            <span class="status-pill" :class="statusClass(exam, now)">
              <svg v-if="exam.status !== 'COMPLETED'" viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="8.5" /><path d="M12 10v5" /><circle cx="12" cy="7.2" r=".7" fill="currentColor" stroke="none" /></svg>
              <svg v-else viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="8.5" /><path d="m8.5 12 2.3 2.4 4.8-5" /></svg>
              {{ statusLabel(exam, now) }}
            </span>
          </td>
          <td>
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
                  @click="emit('toggle-menu', exam.id)"
                >
                  <svg viewBox="0 0 24 24"><circle cx="5" cy="12" r="1" /><circle cx="12" cy="12" r="1" /><circle cx="19" cy="12" r="1" /></svg>
                </button>
                <div v-if="openActionMenu === exam.id" class="row-action-menu">
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
        <tr v-if="exams.length === 0">
          <td colspan="6" class="empty-row">
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
