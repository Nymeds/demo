<script setup>
import { ref } from 'vue'
import { formatAverage, formatScore } from '../../shared/format/grade.js'
import { linkedActivityLabel } from './simulatorRules.js'

defineProps({
  gradesStatus: { type: String, required: true },
  notes: { type: Array, required: true },
  activities: { type: Array, required: true },
  currentAverage: { type: Number, required: true },
  maxGrade: { type: Number, required: true },
  addGradeHint: { type: String, default: '' },
})

const emit = defineEmits(['retry-grades', 'navigate', 'add-grade'])

const addGradeButton = ref(null)

defineExpose({ focusAddGradeButton: () => addGradeButton.value?.focus() })
</script>

<template>
  <!-- NOTAS LANÇADAS -->
  <section class="card grades-card">

    <div class="card-heading">

      <h2>
        Notas lançadas
      </h2>

    </div>


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


    <div v-else class="grades-table">

      <div class="grades-header">

        <span>
          Avaliação
        </span>

        <span>
          Nota obtida
        </span>

        <span>
          Nota máxima
        </span>

        <span>
          Ações
        </span>

      </div>


      <div
        v-for="note in notes"
        :key="note.id"
        class="grade-row"
      >

        <div class="evaluation">

          <div class="evaluation-icon">

            <svg
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="1.8"
            >
              <path d="M6 3h9l3 3v15H6z"/>
              <path d="M14 3v4h4"/>
              <path d="M9 12h6"/>
              <path d="M9 16h6"/>
            </svg>

          </div>

          <div class="evaluation-text">
            <span>{{ note.name }}</span>
            <small>{{ linkedActivityLabel(note, activities) }}</small>
          </div>

        </div>


        <strong>
          {{ formatScore(note.value) }}
        </strong>


        <span>
          {{ formatScore(maxGrade) }}
        </span>


        <button
          type="button"
          class="more-button"
          data-tooltip="Editar em Notas"
          aria-label="Editar esta nota na tela Notas"
          @click="emit('navigate', 'grades')"
        >
          Editar em Notas
        </button>

      </div>


      <div
        v-if="notes.length === 0"
        class="empty-row"
      >
        Nenhuma nota lançada.
      </div>


      <div
        v-if="notes.length > 0"
        class="total-row"
      >

        <strong>
          Média atual
        </strong>

        <strong>
          {{ formatAverage(currentAverage) }}
        </strong>

        <span></span>

        <span></span>

      </div>

    </div>


    <button
      ref="addGradeButton"
      class="add-grade-button"
      type="button"
      :aria-describedby="addGradeHint ? 'add-grade-hint' : undefined"
      @click="emit('add-grade')"
    >
      ＋ Adicionar avaliação lançada
    </button>

    <p
      v-if="addGradeHint"
      id="add-grade-hint"
      class="add-grade-hint"
      role="alert"
    >
      {{ addGradeHint }}
    </p>

  </section>
</template>
