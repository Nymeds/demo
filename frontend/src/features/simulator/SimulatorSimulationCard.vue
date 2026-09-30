<script setup>
import { computed, ref } from 'vue'
import { formatAverage, formatScore } from '../../shared/format/grade.js'
import { impossibleMessage, requiredScoreLabel } from './simulatorRules.js'

const props = defineProps({
  gradesStatus: { type: String, required: true },
  selectedDiscipline: { type: [String, Number], default: '' },
  desiredAverage: { type: [Number, String], default: '' },
  desiredAverageError: { type: String, default: '' },
  maxGrade: { type: Number, required: true },
  simulationStatus: { type: String, required: true },
  simulationError: { type: String, default: '' },
  simulationResult: { type: Object, default: null },
  showResult: { type: Boolean, default: false },
  hasSavedSimulation: { type: Boolean, default: false },
})

const emit = defineEmits(['update:desiredAverage', 'retry-grades', 'simulate', 'clear-simulation'])

const desiredAverageInput = ref(null)

const desiredAverageModel = computed({
  get: () => props.desiredAverage,
  set: value => emit('update:desiredAverage', value)
})

// Notas e médias são positivas: "-", "+" e "e" (notação científica) não são digitáveis.
function blockInvalidNumberKeys(event) {
  if (['-', '+', 'e', 'E'].includes(event.key)) {
    event.preventDefault()
  }
}

defineExpose({ focusDesiredAverageInput: () => desiredAverageInput.value?.focus() })
</script>

<template>
  <!-- QUANTO PRECISO TIRAR -->
  <section class="card simulation-card">

    <h2>
      Quanto preciso tirar?
    </h2>

    <p class="description">
      Informe a média que deseja alcançar para calcular.
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


    <div v-else class="input-grid">

      <!-- MÉDIA DESEJADA -->
      <div class="field">

        <label>
          Média desejada
        </label>

        <input
          ref="desiredAverageInput"
          v-model.number="desiredAverageModel"
          type="number"
          min="0"
          max="10"
          step="0.1"
          @keydown="blockInvalidNumberKeys"
          :aria-invalid="Boolean(desiredAverageError)"
          aria-describedby="desired-average-error"
          placeholder="8,5"
        />

        <p
          v-if="desiredAverageError"
          id="desired-average-error"
          class="field-error"
          role="alert"
        >
          {{ desiredAverageError }}
        </p>

      </div>


      <!-- NOTA MÁXIMA -->
      <div class="field">

        <label>
          Nota máxima
        </label>

        <input
          type="number"
          :value="maxGrade"
          disabled
        />

      </div>

    </div>


    <!-- BOTÃO -->
    <button
      v-if="gradesStatus !== 'error'"
      class="simulate-button"
      :disabled="!selectedDiscipline || simulationStatus === 'loading'"
      @click="emit('simulate')"
    >
      {{ simulationStatus === 'loading' ? 'Simulando...' : 'Simular' }}
    </button>


    <!-- ERRO NA SIMULAÇÃO -->
    <div
      v-if="simulationStatus === 'error' && gradesStatus !== 'error'"
      class="load-error simulation-load-error"
    >
      <p role="alert">
        {{ simulationError || 'Não foi possível calcular a simulação.' }}
      </p>
      <button
        type="button"
        class="retry-button"
        @click="emit('simulate')"
      >
        Tentar novamente
      </button>
    </div>


    <!-- RESULTADO -->
    <div
      v-if="showResult && simulationResult && simulationStatus !== 'error' && gradesStatus !== 'error'"
      class="result-box"
    >

      <div class="result-side">

        <span>
          {{ ['ALREADY_REACHED', 'IMPOSSIBLE'].includes(simulationResult.status) ? 'Situação' : 'Você precisa tirar' }}
        </span>

        <strong>
          {{ requiredScoreLabel(simulationResult, maxGrade) }}
        </strong>

      </div>


      <div class="result-separator"></div>


      <div class="result-side">

        <span>
          para alcançar sua meta
        </span>

        <strong>
          {{ formatAverage(simulationResult.targetAverage) }}
        </strong>

      </div>

    </div>


    <!-- MENSAGEM RESULTADO -->
    <p
      v-if="showResult && simulationResult && simulationStatus !== 'error' && gradesStatus !== 'error'"
      class="result-message"
      :class="{ 'result-warning': simulationResult.status === 'IMPOSSIBLE' }"
      :role="simulationResult.status === 'IMPOSSIBLE' ? 'status' : undefined"
    >

      <template v-if="simulationResult.status === 'ALREADY_REACHED'">

        Você já alcançou sua média desejada.

      </template>

      <template v-else-if="simulationResult.status === 'IMPOSSIBLE'">

        {{ impossibleMessage(simulationResult) }}

      </template>

      <template v-else>

        Se tirar
        {{ formatScore(simulationResult.requiredScoreRaw) }}
        na próxima avaliação, você alcançará sua meta.

      </template>

    </p>

    <p
      v-if="gradesStatus !== 'error'"
      class="simulation-local-note"
    >
      A simulação é salva apenas neste dispositivo.
    </p>

    <button
      v-if="gradesStatus !== 'error' && hasSavedSimulation"
      type="button"
      class="clear-simulation-button"
      @click="emit('clear-simulation')"
    >
      Limpar simulação
    </button>

  </section>
</template>
