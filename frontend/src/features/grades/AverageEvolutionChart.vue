<script setup>
import { computed } from 'vue'
import { formatGrade } from './gradesPresentation'

const WIDTH = 320
const HEIGHT = 196
const PADDING = Object.freeze({ top: 26, right: 24, bottom: 30, left: 32 })
const MAX_GRADE = 10
const Y_TICKS = Object.freeze([0, 2, 4, 6, 8, 10])
const VALUE_LABEL_GAP = 9

const props = defineProps({
  points: { type: Array, required: true },
  goal: { type: Number, default: null },
})

const plotWidth = WIDTH - PADDING.left - PADDING.right
const plotHeight = HEIGHT - PADDING.top - PADDING.bottom
const baseline = PADDING.top + plotHeight

function yOf(value) {
  return PADDING.top + plotHeight - (value / MAX_GRADE) * plotHeight
}

const positioned = computed(() => props.points.map((point, index) => ({
  ...point,
  x: props.points.length === 1
    ? PADDING.left + plotWidth / 2
    : PADDING.left + (index / (props.points.length - 1)) * plotWidth,
  y: yOf(point.average),
})))

const linePath = computed(() => positioned.value
  .map((point, index) => `${index === 0 ? 'M' : 'L'}${point.x.toFixed(1)} ${point.y.toFixed(1)}`)
  .join(' '))

const areaPath = computed(() => {
  const points = positioned.value
  if (points.length < 2) return ''
  return `${linePath.value} L${points.at(-1).x.toFixed(1)} ${baseline} L${points[0].x.toFixed(1)} ${baseline} Z`
})

const description = computed(() => props.points
  .map(point => `${point.period}: ${formatGrade(point.average)}`)
  .join('; '))
</script>

<template>
  <p v-if="points.length === 0" class="evolution-empty">A evolução aparece assim que houver notas lançadas em algum período.</p>

  <svg
    v-else
    class="evolution-chart"
    :viewBox="`0 0 ${WIDTH} ${HEIGHT}`"
    role="img"
    :aria-label="`Evolução da média geral por período. ${description}${goal === null ? '' : `. Meta: ${formatGrade(goal)}`}`"
  >
    <g v-for="tick in Y_TICKS" :key="tick">
      <line class="evolution-grid" :x1="PADDING.left" :x2="WIDTH - PADDING.right" :y1="yOf(tick)" :y2="yOf(tick)" />
      <text class="evolution-axis" :x="PADDING.left - 8" :y="yOf(tick)" text-anchor="end" dominant-baseline="middle">{{ tick }}</text>
    </g>

    <g v-if="goal !== null">
      <line class="evolution-goal" :x1="PADDING.left" :x2="WIDTH - PADDING.right" :y1="yOf(goal)" :y2="yOf(goal)" />
      <text class="evolution-goal-label" :x="WIDTH - PADDING.right" :y="yOf(goal) - 5" text-anchor="end">Meta {{ formatGrade(goal) }}</text>
    </g>

    <path v-if="areaPath" class="evolution-area" :d="areaPath" />
    <path class="evolution-line" :d="linePath" />

    <g v-for="point in positioned" :key="point.period">
      <circle class="evolution-point" :cx="point.x" :cy="point.y" r="4" />
      <text class="evolution-value" :x="point.x" :y="point.y - VALUE_LABEL_GAP" text-anchor="middle">{{ formatGrade(point.average) }}</text>
      <text class="evolution-axis" :x="point.x" :y="HEIGHT - 9" text-anchor="middle">{{ point.period }}</text>
    </g>
  </svg>
</template>

<style scoped>
.evolution-chart { display: block; height: auto; width: 100%; }
.evolution-grid { stroke: #eceef4; stroke-width: 1; }
.evolution-axis { fill: #8a90a2; font-size: 9px; }
.evolution-goal { stroke: #1f9d57; stroke-dasharray: 4 4; stroke-width: 1.2; }
.evolution-goal-label { fill: #1f8a4c; font-size: 8.5px; font-weight: 700; }
.evolution-area { fill: #6832df; fill-opacity: .08; }
.evolution-line { fill: none; stroke: #6832df; stroke-linecap: round; stroke-linejoin: round; stroke-width: 2.2; }
.evolution-point { fill: #6832df; stroke: #fff; stroke-width: 2; }
.evolution-value { fill: #30364a; font-size: 9.5px; font-weight: 700; }
.evolution-empty { color: #7b8192; font-size: .72rem; line-height: 1.5; margin: 0; }
</style>
