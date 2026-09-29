<script setup>
import { formatAverage, formatScore } from '../../shared/format/grade.js'
import { projectedAverage } from './simulatorRules.js'

defineProps({
  gradesStatus: { type: String, required: true },
  scenarios: { type: Array, required: true },
  noteValues: { type: Array, required: true },
  desiredAverage: { type: [Number, String], default: '' },
})

const emit = defineEmits(['retry-grades'])
</script>

<template>
  <!-- CENÁRIOS -->
  <section class="card scenarios-card">

    <h2>
      Simular diferentes cenários
    </h2>

    <p class="description">
      Cenários hipotéticos calculados localmente — não substituem o
      resultado da simulação acima.
    </p>


    <!-- ERRO AO CARREGAR NOTAS -->
    <div
      v-if="gradesStatus === 'error'"
      class="load-error"
    >
      <p role="alert">
        Não foi possível carregar as notas desta disciplina.
      </p>
      <button
        type="button"
        class="retry-button"
        @click="emit('retry-grades')"
      >
        Tentar novamente
      </button>
    </div>


    <div v-else class="scenario-table">

      <div class="scenario-header">

        <span>
          Nota na próxima avaliação
        </span>

        <span>
          Média final projetada
        </span>

      </div>


      <div
        v-for="scenario in scenarios"
        :key="scenario"
        class="scenario-row"
      >

        <span>
          {{ formatScore(scenario) }}
        </span>

        <strong
          :class="{
            positive:
              projectedAverage(noteValues, scenario) >= desiredAverage
          }"
        >

          {{ formatAverage(projectedAverage(noteValues, scenario)) }}

        </strong>

      </div>

    </div>

  </section>
</template>
