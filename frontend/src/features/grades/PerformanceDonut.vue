<script setup>
import { computed } from 'vue'
import { bandInfo, formatGrade } from './gradesPresentation'

const RADIUS = 46
const CIRCUMFERENCE = 2 * Math.PI * RADIUS

const props = defineProps({
  distribution: { type: Array, required: true },
  generalAverage: { type: Number, default: null },
})

const total = computed(() => props.distribution.reduce((sum, item) => sum + item.count, 0))

// Cada faixa vira um arco do anel, começando no topo e seguindo em sentido horário.
const segments = computed(() => {
  let startsAt = 0

  return props.distribution
    .filter(item => item.count > 0)
    .map(item => {
      const length = (item.count / total.value) * CIRCUMFERENCE
      const segment = {
        band: item.band,
        tone: bandInfo(item.band).tone,
        dashArray: `${length} ${CIRCUMFERENCE - length}`,
        dashOffset: -startsAt,
      }
      startsAt += length
      return segment
    })
})

const description = computed(() => props.distribution
  .map(item => `${bandInfo(item.band).label}: ${item.count} disciplina(s), ${item.percent}%`)
  .join('; '))
</script>

<template>
  <p v-if="total === 0" class="donut-empty">Lance notas para ver como suas disciplinas se distribuem por desempenho.</p>

  <div v-else class="donut">
    <svg class="donut-chart" viewBox="0 0 120 120" role="img" :aria-label="`Distribuição das disciplinas por desempenho. ${description}`">
      <circle class="donut-track" cx="60" cy="60" :r="RADIUS" />
      <g transform="rotate(-90 60 60)">
        <circle
          v-for="segment in segments"
          :key="segment.band"
          :class="['donut-segment', `is-${segment.tone}`]"
          cx="60"
          cy="60"
          :r="RADIUS"
          :stroke-dasharray="segment.dashArray"
          :stroke-dashoffset="segment.dashOffset"
        />
      </g>
      <text class="donut-value" x="60" y="58">{{ formatGrade(generalAverage) }}</text>
      <text class="donut-caption" x="60" y="75">Média geral</text>
    </svg>

    <ul class="donut-legend">
      <li v-for="item in distribution" :key="item.band">
        <span :class="['donut-dot', `is-${bandInfo(item.band).tone}`]" aria-hidden="true"></span>
        <span class="donut-label">{{ bandInfo(item.band).label }} <small>({{ bandInfo(item.band).range }})</small></span>
        <strong>{{ item.percent }}%</strong>
      </li>
    </ul>
  </div>
</template>

<style scoped>
.donut { align-items: center; display: grid; gap: 14px; grid-template-columns: 124px minmax(0, 1fr); }
.donut-chart { height: 124px; width: 124px; }
.donut-track { fill: none; stroke: #ecebf3; stroke-width: 13; }
.donut-segment { fill: none; stroke-width: 13; }
.donut-segment.is-excellent { stroke: #1f9d57; }
.donut-segment.is-good { stroke: #2f6fdb; }
.donut-segment.is-regular { stroke: #f08c1c; }
.donut-segment.is-insufficient { stroke: #e0443a; }
.donut-value { dominant-baseline: middle; fill: #171c30; font-size: 21px; font-weight: 800; text-anchor: middle; }
.donut-caption { fill: #7b8192; font-size: 8.5px; text-anchor: middle; }
.donut-legend { display: grid; gap: 9px; list-style: none; margin: 0; padding: 0; }
.donut-legend li { align-items: center; display: grid; gap: 8px; grid-template-columns: 9px minmax(0, 1fr) auto; }
.donut-label { color: #30364a; font-size: .68rem; }
.donut-label small { color: #7b8192; font-size: .6rem; }
.donut-legend strong { color: #30364a; font-size: .68rem; font-variant-numeric: tabular-nums; }
.donut-dot { border-radius: 50%; height: 9px; width: 9px; }
.donut-dot.is-excellent { background: #1f9d57; }
.donut-dot.is-good { background: #2f6fdb; }
.donut-dot.is-regular { background: #f08c1c; }
.donut-dot.is-insufficient { background: #e0443a; }
.donut-empty { color: #7b8192; font-size: .72rem; line-height: 1.5; margin: 0; }

@media (max-width: 380px) {
  .donut { grid-template-columns: 1fr; justify-items: center; }
}
</style>
