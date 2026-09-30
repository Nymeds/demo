<script setup>
import { barClass, formatPercentage, SITUATION_DETAILS } from './frequencyPresentation.js'

defineProps({
  rows: { type: Array, required: true },
})

defineEmits(['configure'])
</script>

<template>
  <section class="frequency-table-card" aria-labelledby="frequency-table-title">
    <header class="table-card-header">
      <h2 id="frequency-table-title">Frequência por disciplina</h2>
    </header>

    <div class="frequency-table-scroll">
      <table>
        <colgroup>
          <col class="column-discipline">
          <col class="column-minimum">
          <col class="column-loss">
          <col class="column-absences">
          <col class="column-attendance">
          <col class="column-situation">
          <col class="column-actions">
        </colgroup>
        <thead>
          <tr>
            <th>Disciplina</th>
            <th>Frequência mínima</th>
            <th>Perda por falta</th>
            <th>Faltas</th>
            <th>Frequência atual</th>
            <th>Situação</th>
            <th>Ações</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in rows" :key="row.id">
            <td>
              <div class="discipline-name-cell">
                <span
                  class="discipline-color"
                  :style="{ backgroundColor: `${row.color}1f`, color: row.color }"
                  aria-hidden="true"
                >
                  <svg viewBox="0 0 24 24"><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2Z" /><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20M8 7h8M8 10h6" /></svg>
                </span>
                <span>
                  <span class="discipline-name">{{ row.name }}</span>
                  <small>{{ row.subtitle }}</small>
                </span>
              </div>
            </td>
            <td data-label="Frequência mínima">{{ formatPercentage(row.minimumPercentage) }}</td>
            <td data-label="Perda por falta">{{ row.lossPerAbsence }}%</td>
            <td class="absences-cell" data-label="Faltas">{{ row.absences }}</td>
            <td data-label="Frequência atual">
              <div class="attendance-cell">
                <span :class="['attendance-value', barClass(row)]">
                  {{ row.attendancePercentage === null ? 'Sem frequência cadastrada' : formatPercentage(row.attendancePercentage) }}
                </span>
                <span class="attendance-track" aria-hidden="true">
                  <span
                    v-if="row.attendancePercentage !== null"
                    :class="barClass(row)"
                    :style="{ width: `${Math.min(row.attendancePercentage, 100)}%` }"
                  ></span>
                </span>
              </div>
            </td>
            <td data-label="Situação">
              <span :class="['frequency-status', SITUATION_DETAILS[row.situation].className]">
                {{ SITUATION_DETAILS[row.situation].label }}
              </span>
            </td>
            <td class="actions-cell">
              <div class="frequency-actions-cell">
                <button
                  type="button"
                  aria-label="Configurar frequência da disciplina"
                  data-tooltip="Configurar frequência"
                  @click="$emit('configure', row.id)"
                >
                  <svg viewBox="0 0 24 24" aria-hidden="true"><path d="m4 20 4-1 11-11-3-3L5 16l-1 4Z" /><path d="m14 7 3 3" /></svg>
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <p v-if="rows.length === 0" class="frequency-no-results">
      Nenhuma disciplina encontrada com esses filtros.
    </p>
  </section>
</template>
