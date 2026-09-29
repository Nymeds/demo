<script setup>
import { computed } from 'vue'
import { formatDate, HISTORY_PREVIEW_SIZE, visibleHistoryOf } from './frequencyPresentation.js'

const props = defineProps({
  entries: { type: Array, required: true },
  showAll: { type: Boolean, default: false },
})

defineEmits(['toggle-show-all', 'delete'])

const visibleHistory = computed(() => visibleHistoryOf(props.entries, props.showAll))
</script>

<template>
  <section class="frequency-history-card" aria-labelledby="frequency-history-title">
    <header class="table-card-header">
      <h2 id="frequency-history-title">Histórico recente de faltas</h2>

      <button
        v-if="entries.length > HISTORY_PREVIEW_SIZE"
        class="see-all"
        type="button"
        @click="$emit('toggle-show-all')"
      >
        {{ showAll ? 'Ver menos' : 'Ver todas' }}
        <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M2 12s4-7 10-7 10 7 10 7-4 7-10 7S2 12 2 12Z" /><circle cx="12" cy="12" r="3" /></svg>
      </button>
    </header>

    <p v-if="entries.length === 0" class="frequency-no-results">
      Nenhuma falta registrada. Os lançamentos aparecem aqui assim que você registrar uma falta.
    </p>

    <div v-else class="frequency-table-scroll">
      <table>
        <thead>
          <tr>
            <th>Data</th>
            <th>Disciplina</th>
            <th>Motivo</th>
            <th>Observação</th>
            <th>Impacto</th>
            <th>Ações</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="entry in visibleHistory" :key="entry.id">
            <td>{{ formatDate(entry.date) }}</td>
            <td>{{ entry.disciplineName }}</td>
            <td>{{ entry.reason }}</td>
            <td>{{ entry.note || '—' }}</td>
            <td><span class="impact-badge">-{{ entry.impact.toLocaleString('pt-BR') }}%</span></td>
            <td>
              <div class="frequency-actions-cell">
                <button
                  class="undo-action"
                  type="button"
                  aria-label="Desfazer lançamento de falta"
                  title="Desfazer lançamento"
                  @click="$emit('delete', entry)"
                >
                  <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 10h10a5 5 0 0 1 0 10H9" /><path d="m8 6-4 4 4 4" /></svg>
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>
