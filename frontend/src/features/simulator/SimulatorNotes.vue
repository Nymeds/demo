<template>
  <div class="simulator-page">

    <!-- CABEÇALHO -->
    <header class="page-header">

      <div class="title-area">

        <div class="title-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="5" y="3" width="14" height="18" rx="2"/>
            <line x1="8" y1="7" x2="16" y2="7"/>
            <line x1="8" y1="11" x2="10" y2="11"/>
            <line x1="14" y1="11" x2="16" y2="11"/>
            <line x1="8" y1="15" x2="10" y2="15"/>
            <line x1="14" y1="15" x2="16" y2="15"/>
          </svg>
        </div>

        <div>
          <h1>Simulador de Notas</h1>
          <p>
            Simule suas notas e descubra quanto precisa para alcançar sua meta.
          </p>
        </div>

      </div>

      <!-- AÇÕES DO CABEÇALHO -->
      <div class="header-actions">

        <button class="notification-button">

          <svg
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="1.8"
          >
            <path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9"/>
            <path d="M10 21h4"/>
          </svg>

          <span>3</span>

        </button>

        <button class="help-button">

          <span class="help-icon">?</span>

          Como funciona?

        </button>

      </div>

    </header>


    <!-- FILTROS -->
    <section class="filters-card">

      <!-- DISCIPLINA -->
      <div class="filter">

        <label>Disciplina</label>

        <div class="select-wrapper">

          <span class="field-icon">

            <svg
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <path d="M4 5.5A2.5 2.5 0 0 1 6.5 3H20v16H6.5A2.5 2.5 0 0 0 4 21.5z"/>
              <path d="M4 5.5v16"/>
            </svg>

          </span>

          <select v-model="selectedDiscipline">

            <option value="">
              Selecione uma disciplina
            </option>

            <option
              v-for="discipline in disciplines"
              :key="discipline.id"
              :value="discipline.id"
            >
              {{ discipline.name }}
            </option>

          </select>

        </div>

      </div>


      <!-- PERÍODO -->
      <div class="filter">

        <label>Período/Ano</label>

        <div class="select-wrapper">

          <span class="field-icon">

            <svg
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <rect x="3" y="5" width="18" height="16" rx="2"/>
              <line x1="16" y1="3" x2="16" y2="7"/>
              <line x1="8" y1="3" x2="8" y2="7"/>
              <line x1="3" y1="10" x2="21" y2="10"/>
            </svg>

          </span>

          <select v-model="period">

            <option>2025.1</option>
            <option>2025.2</option>
            <option>2026.1</option>
            <option>2026.2</option>

          </select>

        </div>

      </div>


      <!-- TIPO DE CÁLCULO -->
      <div class="filter">

        <label>Tipo de cálculo</label>

        <div class="select-wrapper">

          <span class="field-icon">

            <svg
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <rect x="5" y="3" width="14" height="18" rx="2"/>
              <rect x="8" y="6" width="8" height="3"/>
              <circle cx="9" cy="13" r="0.7"/>
              <circle cx="12" cy="13" r="0.7"/>
              <circle cx="15" cy="13" r="0.7"/>
              <circle cx="9" cy="16.5" r="0.7"/>
              <circle cx="12" cy="16.5" r="0.7"/>
              <circle cx="15" cy="16.5" r="0.7"/>
            </svg>

          </span>

          <select v-model="calculationType">

            <option>Média Normal</option>

          </select>

        </div>

      </div>

    </section>


    <!-- PRIMEIRA LINHA -->
    <div class="main-grid">


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


        <div class="situation-body">

          <!-- CÍRCULO -->
          <div class="average-circle">

            <div class="circle-content">

              <strong>
                {{ formatNumber(currentAverage) }}
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
                {{ formatNumber(currentAverage) }}
              </strong>

            </div>


            <div class="info-row">

              <span>
                Média de aprovação
              </span>

              <strong>
                {{ formatNumber(passingAverage) }}
              </strong>

            </div>


            <div class="info-row">

              <span>
                Meta desejada
              </span>

              <div class="target-value">

                <strong>
                  {{ formatNumber(desiredAverage) }}
                </strong>

                <button class="edit-button">

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
          class="status-message"
          :class="
            currentAverage >= passingAverage
              ? 'success'
              : 'warning'
          "
        >

          <div class="status-icon">

            <svg
              v-if="currentAverage >= passingAverage"
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

              {{
                currentAverage >= passingAverage
                  ? 'Você está acima da sua média de aprovação!'
                  : 'Você ainda está abaixo da média de aprovação.'
              }}

            </strong>

            <span>

              {{
                currentAverage >= passingAverage
                  ? 'Continue mantendo um bom desempenho.'
                  : 'Use o simulador para descobrir quanto precisa tirar.'
              }}

            </span>

          </div>

        </div>

      </section>


      <!-- QUANTO PRECISO TIRAR -->
      <section class="card simulation-card">

        <h2>
          Quanto preciso tirar?
        </h2>

        <p class="description">
          Informe a média que deseja alcançar para calcular.
        </p>


        <div class="input-grid">

          <!-- MÉDIA DESEJADA -->
          <div class="field">

            <label>
              Média desejada
            </label>

            <input
              v-model.number="desiredAverage"
              type="number"
              min="0"
              max="10"
              step="0.1"
              placeholder="8,5"
            />

          </div>


          <!-- NOTA MÁXIMA -->
          <div class="field">

            <label>
              Nota máxima
            </label>

            <input
              type="number"
              value="10"
              disabled
            />

          </div>

        </div>


        <!-- BOTÃO -->
        <button
          class="simulate-button"
          @click="simulate"
        >
          Simular
        </button>


        <!-- RESULTADO -->
        <div
          v-if="showResult"
          class="result-box"
        >

          <div class="result-side">

            <span>
              Você precisa tirar
            </span>

            <strong>
              {{ formatNumber(requiredGrade) }}
            </strong>

          </div>


          <div class="result-separator"></div>


          <div class="result-side">

            <span>
              para alcançar sua meta
            </span>

            <strong>
              {{ formatNumber(desiredAverage) }}
            </strong>

          </div>

        </div>


        <!-- MENSAGEM RESULTADO -->
        <p
          v-if="showResult"
          class="result-message"
        >

          <template v-if="requiredGrade > 10">

            A nota necessária ultrapassa a nota máxima.

          </template>

          <template v-else-if="requiredGrade <= 0">

            Você já alcançou sua média desejada.

          </template>

          <template v-else>

            Se tirar
            {{ formatNumber(requiredGrade) }}
            na próxima avaliação, você alcançará sua meta.

          </template>

        </p>

      </section>

    </div>


    <!-- SEGUNDA LINHA -->
    <div class="bottom-grid">


      <!-- NOTAS LANÇADAS -->
      <section class="card grades-card">

        <div class="card-heading">

          <h2>
            Notas lançadas
          </h2>

        </div>


        <div class="grades-table">

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
            v-for="(note, index) in notes"
            :key="index"
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

              <span>
                {{ note.name }}
              </span>

            </div>


            <strong>
              {{ formatNumber(note.value) }}
            </strong>


            <span>
              10,0
            </span>


            <button class="more-button">
              ⋮
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
              {{ formatNumber(currentAverage) }}
            </strong>

            <span></span>

            <span></span>

          </div>

        </div>


        <button class="add-grade-button">

          ＋ Adicionar avaliação lançada

        </button>

      </section>


      <!-- CENÁRIOS -->
      <section class="card scenarios-card">

        <h2>
          Simular diferentes cenários
        </h2>

        <p class="description">
          Veja como diferentes notas impactam sua média.
        </p>


        <div class="scenario-table">

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
              {{ formatNumber(scenario) }}
            </span>

            <strong
              :class="{
                positive:
                  projectedAverage(scenario) >= desiredAverage
              }"
            >

              {{ formatNumber(projectedAverage(scenario)) }}

            </strong>

          </div>

        </div>

      </section>

    </div>

  </div>
</template>


<script setup>

import { computed, ref } from 'vue'


/* =========================
   FILTROS
========================= */

const selectedDiscipline = ref('1')

const period = ref('2025.1')

const calculationType = ref('Média Normal')


/* =========================
   DISCIPLINAS
========================= */

const disciplines = ref([

  {
    id: 1,
    name: 'Estruturas de Dados'
  },

  {
    id: 2,
    name: 'Engenharia de Software'
  },

  {
    id: 3,
    name: 'Banco de Dados'
  }

])


/* =========================
   NOTAS
========================= */

const notes = ref([

  {
    name: 'Prova 1',
    value: 8.0
  },

  {
    name: 'Trabalho 1',
    value: 9.0
  }

])


/* =========================
   CONFIGURAÇÕES
========================= */

const passingAverage = ref(6.0)

const desiredAverage = ref(8.5)

const showResult = ref(true)

const requiredGrade = ref(8.5)


/* =========================
   MÉDIA ATUAL
========================= */

const currentAverage = computed(() => {

  if (notes.value.length === 0) {
    return 0
  }

  const total = notes.value.reduce(
    (sum, note) => sum + Number(note.value),
    0
  )

  return total / notes.value.length

})


/* =========================
   CENÁRIOS
========================= */

const scenarios = [
  5,
  6,
  7,
  8,
  9,
  10
]


/* =========================
   FORMATAR NÚMERO
========================= */

function formatNumber(value) {

  return Number(value)
    .toFixed(1)
    .replace('.', ',')

}


/* =========================
   SIMULAR
========================= */

function simulate() {

  const numberOfNotes = notes.value.length

  requiredGrade.value =
    desiredAverage.value * (numberOfNotes + 1)
    -
    currentAverage.value * numberOfNotes

  showResult.value = true

}


/* =========================
   PROJEÇÃO
========================= */

function projectedAverage(nextGrade) {

  const numberOfNotes = notes.value.length

  if (numberOfNotes === 0) {
    return Number(nextGrade)
  }

  return (
    (
      currentAverage.value * numberOfNotes
      +
      Number(nextGrade)
    )
    /
    (numberOfNotes + 1)
  )

}

</script>


<style scoped>

/* =========================
   PÁGINA
========================= */

.simulator-page {

  width: 100%;
  min-height: 100vh;

  box-sizing: border-box;

  padding: 28px 32px 40px;

  background: #f7f7fb;

  color: #202033;

}


/* =========================
   CABEÇALHO
========================= */

.page-header {

  display: flex;

  align-items: center;

  justify-content: space-between;

  margin-bottom: 26px;

}


.title-area {

  display: flex;

  align-items: center;

  gap: 14px;

}


.title-icon {

  width: 42px;
  height: 42px;

  display: flex;

  align-items: center;

  justify-content: center;

  border-radius: 10px;

  background: #f0eaff;

  color: #6330e0;

}


.title-icon svg {

  width: 23px;
  height: 23px;

}


.title-area h1 {

  margin: 0;

  color: #151525 !important;

  font-size: 28px;

  font-weight: 700;

}


.title-area p {

  margin: 6px 0 0;

  color: #555267 !important;

  font-size: 14px;

}


.header-actions {

  display: flex;

  align-items: center;

  gap: 14px;

}


.notification-button {

  position: relative;

  width: 42px;
  height: 42px;

  display: flex;

  align-items: center;

  justify-content: center;

  border: none;

  background: transparent;

  color: #555267;

}


.notification-button svg {

  width: 21px;
  height: 21px;

}


.notification-button span {

  position: absolute;

  top: -2px;
  right: -2px;

  width: 18px;
  height: 18px;

  display: flex;

  align-items: center;

  justify-content: center;

  border-radius: 50%;

  background: #6330e0;

  color: white;

  font-size: 10px;

}


.help-button {

  height: 42px;

  display: flex;

  align-items: center;

  gap: 8px;

  padding: 0 18px;

  border: 1px solid #dedce8;

  border-radius: 9px;

  background: white;

  color: #3d3a4d;

  font-size: 14px;

}


.help-icon {

  width: 17px;
  height: 17px;

  display: flex;

  align-items: center;

  justify-content: center;

  border: 1px solid #858292;

  border-radius: 50%;

}


/* =========================
   FILTROS
========================= */

.filters-card {

  display: grid;

  grid-template-columns: 1.3fr 1fr 1fr;

  margin-bottom: 20px;

  background: white;

  border: 1px solid #eceaf2;

  border-radius: 12px;

  overflow: hidden;

}


.filter {

  padding: 17px 22px;

  border-right: 1px solid #eceaf2;

}


.filter:last-child {

  border-right: none;

}


.filter label {

  display: block;

  margin-bottom: 8px;

  color: #686579 !important;

  font-size: 13px;

}


.select-wrapper {

  position: relative;

}


.field-icon {

  position: absolute;

  left: 14px;

  top: 50%;

  transform: translateY(-50%);

  color: #6330e0;

  pointer-events: none;

}


.field-icon svg {

  width: 18px;
  height: 18px;

  display: block;

}


.filter select {

  width: 100%;

  height: 42px;

  box-sizing: border-box;

  padding: 0 38px 0 43px;

  border: 1px solid #dddbe6;

  border-radius: 8px;

  background: #ffffff !important;

  color: #292638 !important;

  font-size: 14px;

  outline: none;

}


.filter select:focus {

  border-color: #6330e0;

  box-shadow: 0 0 0 2px rgba(99, 48, 224, .08);

}


/* =========================
   GRID
========================= */

.main-grid,
.bottom-grid {

  display: grid;

  grid-template-columns: 1.05fr .95fr;

  gap: 20px;

  margin-bottom: 20px;

}


.card {

  box-sizing: border-box;

  padding: 22px;

  background: #ffffff;

  border: 1px solid #eceaf2;

  border-radius: 12px;

  box-shadow: 0 2px 8px rgba(25, 20, 60, .03);

}


.card h2 {

  margin: 0;

  color: #202033 !important;

  font-size: 17px;

  font-weight: 700;

}


/* =========================
   TÍTULOS DOS CARDS
========================= */

.card-heading {

  margin-bottom: 18px;

}


.heading-left {

  display: flex;

  align-items: center;

  gap: 10px;

}


.badge {

  padding: 5px 9px;

  border-radius: 20px;

  background: #f0eaff;

  color: #6330e0 !important;

  font-size: 11px;

  font-weight: 600;

}


/* =========================
   SITUAÇÃO ATUAL
========================= */

.situation-body {

  display: flex;

  align-items: center;

  gap: 30px;

  min-height: 190px;

}


.average-circle {

  width: 170px;
  height: 170px;

  flex-shrink: 0;

  display: flex;

  align-items: center;

  justify-content: center;

  border-radius: 50%;

  background:

    radial-gradient(
      circle,
      white 0 61%,
      transparent 62%
    ),

    conic-gradient(
      #6330e0 0deg 306deg,
      #dedde6 306deg 360deg
    );

}


.circle-content {

  display: flex;

  flex-direction: column;

  align-items: center;

}


.circle-content strong {

  color: #6330e0 !important;

  font-size: 35px;

}


.circle-content span {

  color: #555267 !important;

  font-size: 12px;

}


.situation-info {

  flex: 1;

}


.info-row {

  min-height: 48px;

  display: flex;

  align-items: center;

  justify-content: space-between;

  border-bottom: 1px solid #eeeef3;

}


.info-row span {

  color: #686579 !important;

  font-size: 13px;

}


.info-row strong {

  color: #202033 !important;

}


.target-value {

  display: flex;

  align-items: center;

  gap: 10px;

}


.edit-button {

  width: 30px;
  height: 30px;

  display: flex;

  align-items: center;
  justify-content: center;

  border: 1px solid #dedce8;

  border-radius: 7px;

  background: #ffffff;

  color: #6330e0;

}


.edit-button svg {

  width: 15px;
  height: 15px;

}


/* =========================
   MENSAGEM VERDE
========================= */

.status-message {

  display: flex;

  align-items: center;

  gap: 12px;

  margin-top: 18px;

  padding: 14px 16px;

  border-radius: 9px;

}


.status-message.success {

  background: #edf9f1;

  color: #24834b !important;

}


.status-message.warning {

  background: #fff7e8;

  color: #a56c00 !important;

}


.status-icon {

  width: 25px;
  height: 25px;

  flex-shrink: 0;

  display: flex;

  align-items: center;

  justify-content: center;

  border: 1.5px solid currentColor;

  border-radius: 50%;

}


.status-icon svg {

  width: 15px;
  height: 15px;

}


.status-message strong {

  display: block;

  color: inherit !important;

  font-size: 12px;

}


.status-message span {

  display: block;

  margin-top: 3px;

  color: inherit !important;

  font-size: 11px;

}


/* =========================
   SIMULAÇÃO
========================= */

.description {

  margin: 6px 0 19px;

  color: #686579 !important;

  font-size: 12px;

}


.input-grid {

  display: grid;

  grid-template-columns: 1fr 1fr;

  gap: 14px;

}


.field label {

  display: block;

  margin-bottom: 8px;

  color: #555267 !important;

  font-size: 12px;

  font-weight: 600;

}


/* CAMPO CORRIGIDO */

.field input {

  width: 100%;

  height: 42px;

  box-sizing: border-box;

  padding: 0 13px;

  border: 1px solid #dedce8;

  border-radius: 8px;

  background: #ffffff !important;

  color: #252338 !important;

  font-size: 14px;

  outline: none;

  appearance: auto;

}


.field input:focus {

  background: #ffffff !important;

  color: #252338 !important;

  border-color: #6330e0;

  outline: none;

  box-shadow: 0 0 0 2px rgba(99, 48, 224, .08);

}


.field input:disabled {

  background: #f1f1f3 !important;

  color: #555267 !important;

  opacity: 1;

}


/* =========================
   BOTÃO SIMULAR
========================= */

.simulate-button {

  width: 100%;

  height: 42px;

  margin-top: 17px;

  border: none;

  border-radius: 8px;

  background: #6330e0;

  color: white;

  font-size: 14px;

  font-weight: 600;

  cursor: pointer;

}


.simulate-button:hover {

  background: #5726ce;

}


/* =========================
   RESULTADO
========================= */

.result-box {

  min-height: 110px;

  display: grid;

  grid-template-columns: 1fr 1px 1fr;

  align-items: center;

  margin-top: 18px;

  padding: 10px 18px;

  border: 1px solid #ddd2ff;

  border-radius: 9px;

  background: #f8f5ff;

}


.result-side {

  display: flex;

  flex-direction: column;

  align-items: center;

  text-align: center;

}


.result-side span {

  color: #686579 !important;

  font-size: 11px;

}


.result-side strong {

  margin-top: 5px;

  color: #6330e0 !important;

  font-size: 29px;

}


.result-separator {

  width: 1px;

  height: 55px;

  background: #dcd1f7;

}


.result-message {

  margin: 9px 0 0;

  color: #686579 !important;

  font-size: 11px;

  text-align: center;

}


/* =========================
   TABELA DE NOTAS
========================= */

.grades-table,
.scenario-table {

  border: 1px solid #eeeef3;

  border-radius: 8px;

  overflow: hidden;

}


.grades-header,
.grade-row,
.total-row {

  display: grid;

  grid-template-columns: 1.5fr .8fr .8fr 45px;

  align-items: center;

  padding: 11px 12px;

  font-size: 11px;

}


.grades-header {

  background: #fafafd;

  color: #686579 !important;

  font-weight: 600;

}


.grade-row {

  min-height: 48px;

  border-top: 1px solid #eeeef3;

}


.evaluation {

  display: flex;

  align-items: center;

  gap: 9px;

}


.evaluation-icon {

  width: 29px;
  height: 29px;

  display: flex;

  align-items: center;

  justify-content: center;

  border-radius: 6px;

  background: #f0eaff;

  color: #6330e0;

}


.evaluation-icon svg {

  width: 16px;
  height: 16px;

}


.more-button {

  border: none;

  background: transparent;

  color: #555267;

  font-size: 19px;

  cursor: pointer;

}


.total-row {

  min-height: 42px;

  border-top: 1px solid #eeeef3;

}


.total-row strong {

  color: #6330e0 !important;

}


.empty-row {

  padding: 25px;

  color: #9997a8 !important;

  text-align: center;

}


.add-grade-button {

  width: 100%;

  height: 40px;

  margin-top: 12px;

  border: 1px dashed #cfcbdc;

  border-radius: 8px;

  background: #ffffff;

  color: #6330e0 !important;

  cursor: pointer;

}


/* =========================
   CENÁRIOS
========================= */

.scenario-header,
.scenario-row {

  display: grid;

  grid-template-columns: 1fr 1fr;

  align-items: center;

  min-height: 35px;

  text-align: center;

  font-size: 11px;

}


.scenario-header {

  background: #fafafd;

  color: #686579 !important;

  font-weight: 600;

}


.scenario-row {

  border-top: 1px solid #eeeef3;

}


.scenario-row strong {

  color: #292638 !important;

}


.scenario-row strong.positive {

  color: #238b4e !important;

}


/* =========================
   RESPONSIVO
========================= */

@media (max-width: 1100px) {

  .main-grid,
  .bottom-grid {

    grid-template-columns: 1fr;

  }

}


@media (max-width: 800px) {

  .filters-card {

    grid-template-columns: 1fr;

  }

  .filter {

    border-right: none;

    border-bottom: 1px solid #eceaf2;

  }

  .header-actions {

    display: none;

  }

}


@media (max-width: 600px) {

  .simulator-page {

    padding: 18px;

  }

  .situation-body {

    flex-direction: column;

  }

  .input-grid {

    grid-template-columns: 1fr;

  }

}

</style>