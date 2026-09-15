<script setup>
import { computed, onMounted, ref } from 'vue'

// Duração de cada passo na reprodução automática, como um vídeo curto.
const STEP_DURATION_MS = 7000

// Exemplo fictício usado nas animações; os números saem da mesma conta do simulador.
const EXAMPLE = Object.freeze({ grades: [5, 7], goal: 7 })
const exampleSum = EXAMPLE.grades.reduce((total, grade) => total + grade, 0)
const exampleCount = EXAMPLE.grades.length

function formatGrade(value) {
  return value.toFixed(1).replace('.', ',')
}

const EXAMPLE_AVERAGE = formatGrade(exampleSum / exampleCount)
const EXAMPLE_GOAL = formatGrade(EXAMPLE.goal)
const EXAMPLE_SUM = formatGrade(exampleSum)
const EXAMPLE_REQUIRED = formatGrade(EXAMPLE.goal * (exampleCount + 1) - exampleSum)
const SCENARIO_ROWS = Object.freeze([5, 6, 7, 8, 9, 10].map(grade => {
  const average = (exampleSum + grade) / (exampleCount + 1)
  return { grade: formatGrade(grade), average: formatGrade(average), reachesGoal: average >= EXAMPLE.goal }
}))

const STEPS = Object.freeze([
  {
    scene: 'filters',
    title: 'Escolha o período e a disciplina',
    text: 'No topo do simulador, selecione o Período/Ano e a disciplina. A média de aprovação cadastrada na disciplina já entra como sua meta inicial.',
  },
  {
    scene: 'grades',
    title: 'Lance as notas que você já tirou',
    text: 'Em “Notas lançadas”, clique em “Adicionar avaliação lançada”, escolha a prova ou trabalho cadastrado em Atividades e informe a nota de 0 a 10 e a data em que ela saiu. A média atual é a média simples dessas notas.',
  },
  {
    scene: 'simulate',
    title: 'Informe a média que quer alcançar',
    text: 'Em “Quanto preciso tirar?”, digite a média desejada e clique em Simular. A conta considera que a próxima avaliação tem o mesmo peso das anteriores.',
  },
  {
    scene: 'result',
    title: 'Veja quanto precisa tirar',
    text: 'O resultado mostra a nota necessária na próxima avaliação. Se passar de 10, a meta não é alcançável só com ela; se for 0 ou menos, você já chegou lá.',
  },
  {
    scene: 'scenarios',
    title: 'Compare outros cenários',
    text: 'A tabela “Simular diferentes cenários” mostra a média final para notas de 5 a 10 na próxima avaliação. As que alcançam sua meta aparecem em verde.',
  },
])

const emit = defineEmits(['close'])

const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches
const current = ref(0)
const playing = ref(!prefersReducedMotion)
const finished = ref(false)
const dialog = ref(null)

const step = computed(() => STEPS[current.value])
const isLast = computed(() => current.value === STEPS.length - 1)

function goTo(index) {
  current.value = Math.min(Math.max(index, 0), STEPS.length - 1)
  finished.value = false
}

function next() {
  if (!isLast.value) goTo(current.value + 1)
}

function previous() {
  goTo(current.value - 1)
}

function togglePlaying() {
  if (!playing.value && finished.value) goTo(0)
  playing.value = !playing.value
}

function onStepFinished() {
  // Com animações reduzidas a barra termina na hora; o avanço fica só nos botões.
  if (prefersReducedMotion || !playing.value) return

  if (isLast.value) {
    playing.value = false
    finished.value = true
    return
  }

  next()
}

function keepFocusInside(event) {
  const focusable = [...dialog.value.querySelectorAll('button:not([disabled])')]
  if (focusable.length === 0) return

  const first = focusable[0]
  const last = focusable[focusable.length - 1]
  const active = document.activeElement

  if (event.shiftKey && (active === first || active === dialog.value)) {
    event.preventDefault()
    last.focus()
  } else if (!event.shiftKey && active === last) {
    event.preventDefault()
    first.focus()
  }
}

function handleKeydown(event) {
  if (event.key === 'Escape') emit('close')
  else if (event.key === 'ArrowRight') next()
  else if (event.key === 'ArrowLeft') previous()
  else if (event.key === 'Tab') keepFocusInside(event)
}

onMounted(() => dialog.value?.focus())
</script>

<template>
  <div class="help-backdrop" @mousedown.self="emit('close')">
    <section
      ref="dialog"
      class="help-modal"
      role="dialog"
      aria-modal="true"
      aria-labelledby="simulator-help-title"
      aria-describedby="simulator-help-text"
      tabindex="-1"
      @keydown="handleKeydown"
    >
      <header class="help-header">
        <div>
          <span class="help-eyebrow">Como funciona o simulador</span>
          <p class="help-counter">Passo {{ current + 1 }} de {{ STEPS.length }}</p>
        </div>
        <button class="help-close" type="button" aria-label="Fechar explicação" @click="emit('close')">×</button>
      </header>

      <div class="help-stage" aria-hidden="true">
        <div :key="current" class="help-scene">
          <template v-if="step.scene === 'filters'">
            <div class="mock-field help-rise"><small>Período/Ano</small><strong>2026.2</strong></div>
            <div class="mock-field is-focused help-rise" style="--delay: .6s"><small>Disciplina</small><strong>Algoritmos e Estruturas de Dados</strong></div>
            <span class="mock-chip help-pop" style="--delay: 1.5s">Meta inicial: {{ EXAMPLE_GOAL }}</span>
          </template>

          <div v-else-if="step.scene === 'grades'" class="mock-table">
            <div class="mock-row is-head"><span>Avaliação</span><span>Nota obtida</span></div>
            <div class="mock-row help-rise" style="--delay: .4s"><span>Prova 1</span><strong>{{ formatGrade(EXAMPLE.grades[0]) }}</strong></div>
            <div class="mock-row help-rise" style="--delay: 1.2s"><span>Trabalho prático</span><strong>{{ formatGrade(EXAMPLE.grades[1]) }}</strong></div>
            <div class="mock-row is-total help-rise" style="--delay: 2s"><strong>Média atual</strong><strong>{{ EXAMPLE_AVERAGE }}</strong></div>
          </div>

          <template v-else-if="step.scene === 'simulate'">
            <div class="mock-field help-rise"><small>Média desejada</small><strong>{{ EXAMPLE_GOAL }}</strong></div>
            <span class="mock-button help-press" style="--delay: 1.1s">Simular</span>
            <p class="mock-formula help-rise" style="--delay: 2s">
              {{ EXAMPLE_GOAL }} × ({{ exampleCount }} notas + 1) − {{ EXAMPLE_SUM }} = <b>{{ EXAMPLE_REQUIRED }}</b>
            </p>
          </template>

          <template v-else-if="step.scene === 'result'">
            <div class="mock-result help-pop">
              <div><small>Você precisa tirar</small><strong>{{ EXAMPLE_REQUIRED }}</strong></div>
              <span class="mock-divider"></span>
              <div><small>para alcançar sua meta</small><strong>{{ EXAMPLE_GOAL }}</strong></div>
            </div>
            <p class="mock-caption help-rise" style="--delay: 1s">
              Se tirar {{ EXAMPLE_REQUIRED }} na próxima avaliação, você alcançará sua meta.
            </p>
          </template>

          <div v-else class="mock-table is-compact">
            <div class="mock-row is-head"><span>Nota na próxima</span><span>Média final</span></div>
            <div
              v-for="(row, index) in SCENARIO_ROWS"
              :key="row.grade"
              :class="['mock-row', 'help-rise', { 'is-goal': row.reachesGoal }]"
              :style="{ '--delay': `${0.25 + index * 0.3}s` }"
            >
              <span>{{ row.grade }}</span><strong>{{ row.average }}</strong>
            </div>
          </div>
        </div>
        <p class="help-example">Exemplo com notas fictícias</p>
      </div>

      <div class="help-progress" aria-hidden="true">
        <span v-for="(item, index) in STEPS" :key="item.scene" class="help-progress-track">
          <span
            v-if="index === current"
            :key="`progress-${current}`"
            :class="['help-progress-bar', { 'is-paused': !playing }]"
            :style="{ animationDuration: `${STEP_DURATION_MS}ms` }"
            @animationend="onStepFinished"
          ></span>
          <span v-else-if="index < current" class="help-progress-bar is-done"></span>
        </span>
      </div>

      <div class="help-body" aria-live="polite">
        <h2 id="simulator-help-title">{{ step.title }}</h2>
        <p id="simulator-help-text">{{ step.text }}</p>
      </div>

      <footer class="help-controls">
        <button class="help-button is-ghost" type="button" @click="togglePlaying">
          <svg v-if="playing" viewBox="0 0 12 12" aria-hidden="true"><rect x="2" y="1.5" width="2.8" height="9" rx=".6" /><rect x="7.2" y="1.5" width="2.8" height="9" rx=".6" /></svg>
          <svg v-else viewBox="0 0 12 12" aria-hidden="true"><path d="M3 1.6v8.8a.6.6 0 0 0 .9.5l7-4.4a.6.6 0 0 0 0-1L3.9 1.1a.6.6 0 0 0-.9.5Z" /></svg>
          {{ playing ? 'Pausar' : 'Reproduzir' }}
        </button>

        <div class="help-nav">
          <button class="help-button is-ghost" type="button" :disabled="current === 0" @click="previous">Anterior</button>
          <button v-if="!isLast" class="help-button is-primary" type="button" @click="next">Próximo</button>
          <button v-else class="help-button is-primary" type="button" @click="emit('close')">Entendi</button>
        </div>
      </footer>
    </section>
  </div>
</template>

<style scoped>
.help-backdrop { align-items: center; background: rgba(16, 20, 34, .58); display: flex; inset: 0; justify-content: center; padding: 20px; position: fixed; z-index: 120; }
.help-modal { background: #fff; border-radius: 16px; box-shadow: 0 24px 70px rgba(15, 18, 35, .28); display: grid; gap: 16px; max-height: calc(100svh - 40px); max-width: 560px; overflow-y: auto; padding: 22px 24px 20px; width: 100%; }
.help-modal:focus { outline: none; }

.help-header { align-items: flex-start; display: flex; justify-content: space-between; }
.help-eyebrow { color: #7240df; display: block; font-size: .62rem; font-weight: 800; letter-spacing: .09em; text-transform: uppercase; }
.help-counter { color: #7b8192; font-size: .7rem; font-variant-numeric: tabular-nums; margin: 3px 0 0; }
.help-close { background: transparent; border: 0; border-radius: 8px; color: #6c7287; cursor: pointer; font-size: 1.4rem; line-height: 1; padding: 4px 9px; }
.help-close:hover { background: #f4f1ff; color: #30364a; }

.help-stage { background: linear-gradient(160deg, #f4f1ff, #ebe5ff); border: 1px solid #e3dbff; border-radius: 12px; display: grid; min-height: 224px; padding: 22px 22px 28px; place-items: center; position: relative; }
.help-scene { display: grid; gap: 12px; justify-items: center; width: min(100%, 360px); }
.help-example { bottom: 8px; color: #8b7fb0; font-size: .58rem; margin: 0; position: absolute; right: 12px; }

.mock-field { background: #fff; border: 1px solid #e1dcf3; border-radius: 10px; display: grid; gap: 2px; padding: 9px 12px; width: 100%; }
.mock-field small { color: #7b8192; font-size: .6rem; font-weight: 700; }
.mock-field strong { color: #1d2236; font-size: .8rem; }
.mock-field.is-focused { border-color: #7a4de6; box-shadow: 0 0 0 3px rgba(105, 54, 224, .16); }
.mock-chip { background: #e8f8ef; border-radius: 999px; color: #1f7a48; font-size: .68rem; font-weight: 800; padding: 5px 11px; }

.mock-table { background: #fff; border: 1px solid #e1dcf3; border-radius: 10px; overflow: hidden; width: 100%; }
.mock-row { align-items: center; border-top: 1px solid #efedf6; color: #30364a; display: flex; font-size: .74rem; justify-content: space-between; padding: 8px 12px; }
.mock-row:first-child { border-top: 0; }
.mock-row strong { font-variant-numeric: tabular-nums; }
.mock-row.is-head { background: #faf9fd; color: #7b8192; font-size: .6rem; font-weight: 700; letter-spacing: .05em; text-transform: uppercase; }
.mock-row.is-total { background: #f4f1ff; color: #5f2bd5; }
.mock-row.is-goal { background: #f1fbf5; }
.mock-row.is-goal strong { color: #1f8a4c; }
.mock-table.is-compact .mock-row { padding: 5px 12px; }

.mock-button { background: linear-gradient(100deg, #5c20de, #741dff); border-radius: 8px; color: #fff; font-size: .74rem; font-weight: 750; padding: 8px 22px; }
.mock-formula { background: #fff; border: 1px dashed #cbbdf5; border-radius: 8px; color: #3b4053; font-size: .74rem; font-variant-numeric: tabular-nums; margin: 0; padding: 8px 12px; text-align: center; }
.mock-formula b { color: #5f2bd5; }

.mock-result { align-items: center; background: #fff; border: 1px solid #e1dcf3; border-radius: 12px; display: flex; gap: 18px; padding: 14px 20px; }
.mock-result div { display: grid; gap: 2px; text-align: center; }
.mock-result small { color: #7b8192; font-size: .62rem; }
.mock-result strong { color: #5f2bd5; font-size: 1.6rem; font-variant-numeric: tabular-nums; line-height: 1.1; }
.mock-divider { align-self: stretch; background: #e6e1f5; width: 1px; }
.mock-caption { color: #4b5064; font-size: .72rem; margin: 0; text-align: center; }

.help-progress { display: grid; gap: 5px; grid-template-columns: repeat(5, minmax(0, 1fr)); }
.help-progress-track { background: #ece9f5; border-radius: 999px; height: 4px; overflow: hidden; }
.help-progress-bar { animation: help-progress linear forwards; background: #6832df; display: block; height: 100%; transform-origin: left; width: 100%; }
.help-progress-bar.is-paused { animation-play-state: paused; }
.help-progress-bar.is-done { animation: none; }

.help-body h2 { color: #171c30; font-size: 1.1rem; font-weight: 800; letter-spacing: -.02em; margin: 0 0 6px; }
.help-body p { color: #5f6678; font-size: .78rem; line-height: 1.6; margin: 0; }

.help-controls { align-items: center; display: flex; flex-wrap: wrap; gap: 10px; justify-content: space-between; }
.help-nav { display: flex; gap: 8px; }
.help-button { align-items: center; border-radius: 8px; cursor: pointer; display: inline-flex; font-size: .72rem; font-weight: 750; gap: 7px; justify-content: center; min-height: 38px; padding: 8px 14px; }
.help-button.is-ghost { background: #fff; border: 1px solid #dfe1e8; color: #3e4458; }
.help-button.is-primary { background: linear-gradient(100deg, #5c20de, #741dff); border: 0; color: #fff; }
.help-button:disabled { cursor: not-allowed; opacity: .5; }
.help-button svg { fill: currentColor; height: 12px; width: 12px; }
.help-close:focus-visible,
.help-button:focus-visible { outline: 3px solid rgba(105, 54, 224, .28); outline-offset: 2px; }

.help-rise { animation: help-rise .45s ease both; animation-delay: var(--delay, 0s); }
.help-pop { animation: help-pop .5s cubic-bezier(.2, .8, .3, 1.2) both; animation-delay: var(--delay, 0s); }
.help-press { animation: help-press .7s ease both; animation-delay: var(--delay, 0s); }

@keyframes help-progress { from { transform: scaleX(0); } to { transform: scaleX(1); } }
@keyframes help-rise { from { opacity: 0; transform: translateY(8px); } to { opacity: 1; transform: none; } }
@keyframes help-pop { from { opacity: 0; transform: scale(.92); } to { opacity: 1; transform: none; } }
@keyframes help-press {
  0%, 100% { box-shadow: 0 0 0 0 rgba(105, 54, 224, 0); transform: none; }
  45% { box-shadow: 0 0 0 7px rgba(105, 54, 224, .18); transform: scale(.94); }
}

@media (max-width: 520px) {
  .help-modal { padding: 18px 16px; }
  .help-stage { min-height: 210px; padding: 16px 16px 26px; }
  .help-controls { align-items: stretch; flex-direction: column-reverse; }
  .help-nav { width: 100%; }
  .help-nav .help-button { flex: 1; }
}
</style>
