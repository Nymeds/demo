<script setup>
import { computed } from 'vue'
import { bandInfo, bandOf, formatGrade } from './gradesPresentation'

const props = defineProps({
  summary: { type: Object, required: true },
  periodLabel: { type: String, required: true },
})


const generalBand = computed(() => bandInfo(bandOf(props.summary.generalAverage)))
</script>

<template>
  <div class="grades-summary" aria-label="Resumo das notas">
    <article class="grades-summary-card is-purple">
      <span class="grades-summary-icon" aria-hidden="true">
        <svg viewBox="0 0 24 24"><path d="M4 19V9m5 10V5m5 14v-7m5 7V8" /><path d="M3 21h18" /></svg>
      </span>
      <div>
        <p>Média geral</p>
        <strong>{{ formatGrade(summary.generalAverage) }}</strong>
        <small>{{ generalBand.summary }}</small>
      </div>
    </article>

    <article class="grades-summary-card is-green">
      <span class="grades-summary-icon" aria-hidden="true">
        <svg viewBox="0 0 24 24"><path d="M3 17l6-6 4 4 8-8" /><path d="M14 7h7v7" /></svg>
      </span>
      <div>
        <p>Disciplinas cursadas</p>
        <strong>{{ summary.disciplineCount }}</strong>
        <small>{{ periodLabel }}</small>
      </div>
    </article>

    <article class="grades-summary-card is-orange">
      <span class="grades-summary-icon" aria-hidden="true">
        <svg viewBox="0 0 24 24"><path d="M8 21h8M12 17v4M7 4h10v5a5 5 0 0 1-10 0V4Z" /><path d="M17 5h3a3 3 0 0 1-3 4M7 5H4a3 3 0 0 0 3 4" /></svg>
      </span>
      <div>
        <p>Melhor média</p>
        <strong>{{ summary.best ? formatGrade(summary.best.average) : '—' }}</strong>
        <small>{{ summary.best ? summary.best.name : 'Aguardando notas' }}</small>
      </div>
    </article>


  </div>
</template>

<style scoped>
.grades-summary { display: grid; gap: 16px; grid-template-columns: repeat(auto-fit, minmax(min(210px, 100%), 1fr)); }
.grades-summary-card { align-items: center; background: #fff; border: 1px solid #e7e8f0; border-radius: 10px; box-shadow: 0 5px 16px rgba(30, 36, 65, .035); display: flex; gap: 16px; min-width: 0; padding: 18px; }
.grades-summary-card > div { min-width: 0; }
.grades-summary-icon { align-items: center; background: #f1edff; border-radius: 50%; color: #6739e7; display: flex; flex: 0 0 52px; height: 52px; justify-content: center; }
.grades-summary-icon svg { fill: none; height: 26px; stroke: currentColor; stroke-linecap: round; stroke-linejoin: round; stroke-width: 1.8; width: 26px; }
.grades-summary-card p { color: #596078; font-size: .7rem; font-weight: 650; margin: 0; }
.grades-summary-card strong { color: #171c30; display: block; font-size: 1.35rem; font-variant-numeric: tabular-nums; line-height: 1; margin: 7px 0 6px; }
.grades-summary-card small { color: #6330e0; display: block; font-size: .62rem; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.grades-summary-card.is-green .grades-summary-icon { background: #e8f8ef; color: #2daf68; }
.grades-summary-card.is-green small { color: #23894f; }
.grades-summary-card.is-orange .grades-summary-icon { background: #fff0e2; color: #ee831e; }
.grades-summary-card.is-orange small { color: #b76315; }
.grades-summary-card.is-blue .grades-summary-icon { background: #eaf2ff; color: #347bd8; }
.grades-summary-card.is-blue small { color: #2f65b8; }
.grades-summary-link { background: none; border: 0; color: #2f65b8; cursor: pointer; font-size: .64rem; font-weight: 700; padding: 0; text-align: left; text-decoration: underline; }
.grades-summary-link:focus-visible { outline: 2px solid rgba(105, 54, 224, .4); outline-offset: 2px; }

</style>
