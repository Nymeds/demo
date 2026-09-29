<script setup>
import { formatAverage } from '../../shared/format/grade.js'

defineProps({
  gradesStatus: { type: String, required: true },
  notesCount: { type: Number, required: true },
  currentAverage: { type: Number, required: true },
  passingAverage: { type: Number, required: true },
  desiredAverage: { type: [Number, String], default: '' },
})

const emit = defineEmits(['retry-grades', 'edit-target'])
</script>

<template>
  <!-- SITUAÇÃO ATUAL -->
  <section class="card situation-card">

    <div class="card-heading">

      <div class="heading-left">

        <h2>Situação atual</h2>

        <span class="badge">
          Média Normal
        </span>

      </div>

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


    <div v-else class="situation-body">

      <!-- CÍRCULO -->
      <div class="average-circle">

        <div class="circle-content">

          <strong>
            {{ formatAverage(currentAverage) }}
          </strong>

          <span>
            Média atual
          </span>

        </div>

      </div>


      <!-- INFORMAÇÕES -->
      <div class="situation-info">

        <div class="info-row">

          <span>
            Média atual
          </span>

          <strong>
            {{ formatAverage(currentAverage) }}
          </strong>

        </div>


        <div class="info-row">

          <span>
            Média de aprovação
          </span>

          <strong>
            {{ formatAverage(passingAverage) }}
          </strong>

        </div>


        <div class="info-row">

          <span>
            Meta desejada
          </span>

          <div class="target-value">

            <strong>
              {{ formatAverage(desiredAverage) }}
            </strong>

            <button
              type="button"
              class="edit-button"
              title="Editar meta desejada"
              aria-label="Editar meta desejada"
              @click="emit('edit-target')"
            >

              <svg
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
              >
                <path d="M12 20h9"/>
                <path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L8 18l-4 1 1-4Z"/>
              </svg>

            </button>

          </div>

        </div>

      </div>

    </div>


    <!-- MENSAGEM -->
    <div
      v-if="gradesStatus !== 'error'"
      class="status-message"
      :class="{
        success:
          notesCount > 0 &&
          currentAverage >= passingAverage,

        warning:
          notesCount > 0 &&
          currentAverage < passingAverage,

        neutral:
          notesCount === 0
      }"
    >

      <div class="status-icon">

        <svg
          v-if="notesCount > 0 && currentAverage >= passingAverage"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
        >
          <path d="m5 12 4 4L19 6"/>
        </svg>

        <span v-else>!</span>

      </div>

      <div>

        <strong>
          <template v-if="notesCount === 0">
            Nenhuma avaliação lançada.
          </template>

          <template v-else-if="currentAverage >= passingAverage">
            Você está acima da sua média de aprovação!
          </template>

          <template v-else>
            Você ainda está abaixo da média de aprovação.
          </template>
        </strong>

        <span>
          <template v-if="notesCount === 0">
            Adicione suas avaliações para começar.
          </template>

          <template v-else-if="currentAverage >= passingAverage">
            Continue mantendo um bom desempenho.
          </template>

          <template v-else>
            Use o simulador para descobrir quanto precisa tirar.
          </template>
        </span>

      </div>

    </div>

  </section>
</template>
