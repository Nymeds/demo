<script setup>
import { onBeforeUnmount, ref, watch } from 'vue'
import { bandInfo, formatGrade, safeColor } from './gradesPresentation'

const props = defineProps({
  entries: { type: Array, required: true },
  loadGrades: { type: Function, required: true },
})

const emit = defineEmits(['open-simulator', 'failed'])

const expandedId = ref(null)
const openMenuId = ref(null)
// Notas já buscadas por disciplina, para não repetir a requisição ao abrir de novo.
const gradesByDiscipline = ref({})

function progressOf(entry) {
  return entry.average === null ? 0 : Math.round(Number(entry.average) * 10)
}

function initialOf(name) {
  return name.trim().charAt(0).toUpperCase()
}

function formatDate(isoDate) {
  return new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: 'short', year: 'numeric' })
    .format(new Date(`${isoDate}T12:00:00`))
}

function gradesState(disciplineId) {
  return gradesByDiscipline.value[disciplineId] ?? { status: 'idle', items: [] }
}

function setGradesState(disciplineId, state) {
  gradesByDiscipline.value = { ...gradesByDiscipline.value, [disciplineId]: state }
}

async function toggleGrades(entry) {
  openMenuId.value = null

  if (expandedId.value === entry.disciplineId) {
    expandedId.value = null
    return
  }

  expandedId.value = entry.disciplineId
  if (gradesState(entry.disciplineId).status === 'ready') return

  setGradesState(entry.disciplineId, { status: 'loading', items: [] })

  try {
    setGradesState(entry.disciplineId, { status: 'ready', items: await props.loadGrades(entry.disciplineId) })
  } catch (error) {
    setGradesState(entry.disciplineId, { status: 'error', items: [] })
    emit('failed', error)
  }
}

function toggleMenu(disciplineId) {
  openMenuId.value = openMenuId.value === disciplineId ? null : disciplineId
}

function openSimulator() {
  openMenuId.value = null
  emit('open-simulator')
}

// Quem navega pelo teclado e sai do menu com Tab também fecha o menu.
function closeMenuWhenFocusLeaves(event) {
  if (!event.currentTarget.contains(event.relatedTarget)) openMenuId.value = null
}

function closeMenuOnOutsidePointer(event) {
  if (!event.target.closest?.('.grades-row-actions')) openMenuId.value = null
}

watch(openMenuId, disciplineId => {
  if (disciplineId) document.addEventListener('pointerdown', closeMenuOnOutsidePointer)
  else document.removeEventListener('pointerdown', closeMenuOnOutsidePointer)
})

onBeforeUnmount(() => document.removeEventListener('pointerdown', closeMenuOnOutsidePointer))
</script>

<template>
  <div class="grades-table-wrap">
    <table class="grades-table">
      <thead>
        <tr>
          <th scope="col">Disciplina</th>
          <th scope="col" class="is-number">Avaliações</th>
          <th scope="col" class="is-number">Média parcial</th>
          <th scope="col">Situação</th>
          <th scope="col">Progresso</th>
          <th scope="col"><span class="grades-visually-hidden">Ações</span></th>
        </tr>
      </thead>
      <tbody>
        <tr v-if="entries.length === 0">
          <td colspan="6" class="grades-table-empty">Nenhuma disciplina encontrada com esses filtros.</td>
        </tr>

        <template v-for="entry in entries" :key="entry.disciplineId">
          <tr :class="{ 'is-expanded': expandedId === entry.disciplineId }">
            <td>
              <div class="grades-discipline">
                <span class="grades-discipline-badge" :style="{ '--discipline-color': safeColor(entry.color) }" aria-hidden="true">
                  {{ initialOf(entry.name) }}
                </span>
                <div>
                  <strong>{{ entry.name }}</strong>
                  <small>{{ entry.professorName || 'Professor não informado' }}</small>
                </div>
              </div>
            </td>
            <td class="is-number">{{ entry.gradeCount }}</td>
            <td class="is-number">
              <strong :class="['grades-average', `is-${bandInfo(entry.band).tone}`]">{{ formatGrade(entry.average) }}</strong>
            </td>
            <td>
              <span :class="['grades-chip', `is-${bandInfo(entry.band).tone}`]">{{ bandInfo(entry.band).label }}</span>
            </td>
            <td>
              <div class="grades-progress">
                <span class="grades-progress-track" aria-hidden="true">
                  <span :class="['grades-progress-bar', `is-${bandInfo(entry.band).tone}`]" :style="{ width: `${progressOf(entry)}%` }"></span>
                </span>
                <span class="grades-progress-value">{{ entry.average === null ? '—' : `${progressOf(entry)}%` }}</span>
              </div>
            </td>
            <td
              class="grades-row-actions"
              @keydown.esc="openMenuId = null"
              @focusout="closeMenuWhenFocusLeaves"
            >
              <button
                class="grades-icon-button"
                type="button"
                aria-haspopup="menu"
                :aria-expanded="openMenuId === entry.disciplineId"
                :aria-label="`Ações de ${entry.name}`"
                @click="toggleMenu(entry.disciplineId)"
              >
                <svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="5" r="1.6" /><circle cx="12" cy="12" r="1.6" /><circle cx="12" cy="19" r="1.6" /></svg>
              </button>
              <div v-if="openMenuId === entry.disciplineId" class="grades-menu" role="menu">
                <button type="button" role="menuitem" @click="toggleGrades(entry)">
                  {{ expandedId === entry.disciplineId ? 'Ocultar notas lançadas' : 'Ver notas lançadas' }}
                </button>
                <button type="button" role="menuitem" @click="openSimulator">Abrir no Simulador de Notas</button>
              </div>
            </td>
          </tr>

          <tr v-if="expandedId === entry.disciplineId" class="grades-detail-row">
            <td colspan="6">
              <p v-if="gradesState(entry.disciplineId).status === 'loading'" class="grades-detail-status" role="status">Carregando notas…</p>
              <p v-else-if="gradesState(entry.disciplineId).status === 'error'" class="grades-detail-status">Não foi possível carregar as notas desta disciplina.</p>
              <p v-else-if="gradesState(entry.disciplineId).items.length === 0" class="grades-detail-status">Nenhuma nota lançada nesta disciplina ainda.</p>
              <template v-else>
                <p class="grades-detail-status">
                  Média de aprovação {{ formatGrade(entry.passingAverage) }} ·
                  {{ entry.passing ? 'você está acima dela' : 'você ainda está abaixo dela' }}
                </p>
                <ul class="grades-detail-list">
                  <li v-for="grade in gradesState(entry.disciplineId).items" :key="grade.id">
                    <span>{{ grade.assessmentName }}</span>
                    <time :datetime="grade.recordedAt">{{ formatDate(grade.recordedAt) }}</time>
                    <strong>{{ formatGrade(grade.score) }}</strong>
                  </li>
                </ul>
              </template>
            </td>
          </tr>
        </template>
      </tbody>
    </table>
  </div>
</template>

<style scoped>
.grades-table-wrap { overflow-x: auto; }
.grades-table { border-collapse: collapse; min-width: 760px; width: 100%; }
.grades-table th { border-bottom: 1px solid #eff0f5; color: #596078; font-size: .66rem; font-weight: 700; padding: 13px 16px; text-align: left; white-space: nowrap; }
.grades-table td { border-bottom: 1px solid #eff0f5; color: #30364a; font-size: .74rem; padding: 12px 16px; vertical-align: middle; }
.grades-table .is-number { font-variant-numeric: tabular-nums; text-align: center; }
.grades-table-empty { color: #7b8192; padding: 32px 16px; text-align: center; }
.grades-table tr.is-expanded td { border-bottom-color: transparent; }

.grades-discipline { align-items: center; display: flex; gap: 12px; min-width: 0; }
.grades-discipline strong { color: #171c30; display: block; font-size: .76rem; }
.grades-discipline small { color: #7b8192; display: block; font-size: .64rem; margin-top: 2px; }
.grades-discipline-badge { align-items: center; background: color-mix(in srgb, var(--discipline-color) 15%, transparent); border-radius: 50%; color: var(--discipline-color); display: flex; flex: 0 0 38px; font-size: .8rem; font-weight: 800; height: 38px; justify-content: center; }

.grades-average { font-size: .95rem; font-weight: 800; }
.grades-average.is-excellent { color: #1f8a4c; }
.grades-average.is-good { color: #2f65b8; }
.grades-average.is-regular { color: #c26a12; }
.grades-average.is-insufficient { color: #c4463e; }
.grades-average.is-empty { color: #8a90a2; }

.grades-chip { border-radius: 999px; display: inline-block; font-size: .62rem; font-weight: 700; padding: 4px 10px; white-space: nowrap; }
.grades-chip.is-excellent { background: #e8f8ef; color: #1f8a4c; }
.grades-chip.is-good { background: #eaf2ff; color: #2f65b8; }
.grades-chip.is-regular { background: #fff2e5; color: #b76315; }
.grades-chip.is-insufficient { background: #fff0ef; color: #c4463e; }
.grades-chip.is-empty { background: #f1f2f6; color: #6c7287; }

.grades-progress { align-items: center; display: flex; gap: 10px; min-width: 130px; }
.grades-progress-track { background: #edeaf5; border-radius: 999px; flex: 1; height: 7px; overflow: hidden; }
.grades-progress-bar { border-radius: inherit; display: block; height: 100%; }
.grades-progress-bar.is-excellent { background: #1f9d57; }
.grades-progress-bar.is-good { background: #2f6fdb; }
.grades-progress-bar.is-regular { background: #f08c1c; }
.grades-progress-bar.is-insufficient { background: #e0443a; }
.grades-progress-value { color: #596078; flex: 0 0 34px; font-size: .64rem; font-variant-numeric: tabular-nums; text-align: right; }

.grades-row-actions { position: relative; text-align: right; width: 48px; }
.grades-icon-button { align-items: center; background: transparent; border: 0; border-radius: 8px; color: #6c7287; cursor: pointer; display: inline-flex; height: 32px; justify-content: center; width: 32px; }
.grades-icon-button:hover { background: #f4f1ff; color: #30364a; }
.grades-icon-button:focus-visible { outline: 2px solid rgba(105, 54, 224, .45); }
.grades-icon-button svg { fill: currentColor; height: 18px; width: 18px; }
.grades-menu { background: #fff; border: 1px solid #e7e8f0; border-radius: 10px; box-shadow: 0 14px 34px rgba(30, 36, 65, .14); display: grid; min-width: 210px; padding: 5px; position: absolute; right: 12px; top: calc(100% - 6px); z-index: 20; }
.grades-menu button { background: transparent; border: 0; border-radius: 7px; color: #30364a; cursor: pointer; font-size: .72rem; padding: 9px 10px; text-align: left; }
.grades-menu button:hover,
.grades-menu button:focus-visible { background: #f4f1ff; outline: none; }

.grades-detail-row td { background: #faf9fd; padding: 4px 16px 16px 66px; }
.grades-detail-status { color: #6c7287; font-size: .68rem; margin: 8px 0; }
.grades-detail-list { display: grid; gap: 6px; list-style: none; margin: 0; padding: 0; }
.grades-detail-list li { align-items: center; background: #fff; border: 1px solid #eceaf3; border-radius: 9px; display: grid; gap: 12px; grid-template-columns: minmax(0, 1fr) auto 48px; padding: 8px 12px; }
.grades-detail-list time { color: #7b8192; font-size: .64rem; }
.grades-detail-list strong { color: #171c30; font-variant-numeric: tabular-nums; text-align: right; }
</style>
