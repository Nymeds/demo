<script setup>
import { computed } from 'vue'
import AppSelect from '../../components/ui/AppSelect.vue'

const props = defineProps({
  details: { type: Object, required: true },
  disciplines: { type: Array, required: true },
  selectedDisciplineId: { type: String, default: '' },
})

const emit = defineEmits(['navigate', 'select'])

const disciplineOptions = computed(() => props.disciplines.map(discipline => ({ value: discipline.id, label: discipline.name })))
</script>

<template>
  <section
    v-if="details"
    class="dashboard-panel dashboard-frequency-panel"
    aria-labelledby="dashboard-frequency-title"
  >
    <header class="dashboard-panel-header">
      <div>
        <span class="dashboard-eyebrow">Acompanhamento</span>
        <h2 id="dashboard-frequency-title">Detalhes da frequência</h2>
      </div>
      <button type="button" @click="emit('navigate', 'frequency')">Ver todas <span aria-hidden="true">→</span></button>
    </header>

    <div class="dashboard-frequency-content" aria-live="polite">
      <AppSelect
        class="dashboard-frequency-select"
        aria-label="Selecionar disciplina para consultar a frequência"
        :model-value="selectedDisciplineId"
        :options="disciplineOptions"
        placeholder="Selecione uma disciplina"
        @update:model-value="emit('select', $event)"
      />

      <div class="dashboard-frequency-details">
        <div
          class="dashboard-frequency-ring"
          :style="{
            '--frequency-angle': `${(details.attendance ?? 0) * 3.6}deg`,
            '--frequency-color': details.ringColor,
          }"
          role="img"
          :aria-label="details.attendance === null
            ? 'Sem frequência cadastrada'
            : `Frequência atual de ${Math.round(details.attendance)}%`"
        >
          <div>
            <strong v-if="details.attendance === null" class="dashboard-frequency-empty">—</strong>
            <strong v-else>{{ Math.round(details.attendance) }}%</strong>
            <span>Frequência</span>
          </div>
        </div>

        <dl class="dashboard-frequency-metrics">
          <div>
            <dt><span class="is-red" aria-hidden="true"></span>Faltas registradas</dt>
            <dd>{{ details.absences }}</dd>
          </div>
          <div>
            <dt><span class="is-purple" aria-hidden="true"></span>Limite de faltas</dt>
            <dd>{{ details.maximumAbsences }}</dd>
          </div>
          <div>
            <dt><span class="is-orange" aria-hidden="true"></span>Perda por falta</dt>
            <dd>{{ details.lossPerAbsence }}%</dd>
          </div>
        </dl>
      </div>

      <div class="dashboard-frequency-minimum">
        <span>Limite mínimo: {{ Math.round(details.minimum) }}%</span>
        <svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="9" /><path d="M12 11v5m0-8v.01" /></svg>
      </div>

      <p :class="['dashboard-frequency-message', details.messageClass]">
        <svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="9" /><path d="M12 8v5m0 3v.01" /></svg>
        {{ details.message }}
      </p>
    </div>
  </section>
</template>
