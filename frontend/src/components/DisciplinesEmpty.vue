<script setup>
import { ref } from 'vue'

const activeFilter = ref('all')
const viewMode = ref('list')

const filters = [
  { value: 'all', label: 'Todas' },
  { value: 'active', label: 'Em andamento' },
  { value: 'finished', label: 'Concluídas' },
  { value: 'locked', label: 'Trancadas' },
]
</script>

<template>
  <section class="disciplines-page" aria-labelledby="disciplines-title">
    <header class="disciplines-header">
      <div class="disciplines-heading">
        <span class="disciplines-heading-icon" aria-hidden="true">
          <svg viewBox="0 0 24 24">
            <path d="M4 5.5A3.5 3.5 0 0 1 7.5 2H11v17H7.5A3.5 3.5 0 0 0 4 22V5.5Z" />
            <path d="M20 5.5A3.5 3.5 0 0 0 16.5 2H13v17h3.5A3.5 3.5 0 0 1 20 22V5.5Z" />
          </svg>
        </span>
        <div>
          <h1 id="disciplines-title">Disciplinas</h1>
          <p>Gerencie suas disciplinas e acompanhe seu desempenho.</p>
        </div>
      </div>

      <div class="disciplines-actions">
        <label class="disciplines-search">
          <span class="sr-only">Buscar disciplina</span>
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <circle cx="11" cy="11" r="7" />
            <path d="m20 20-4-4" />
          </svg>
          <input type="search" placeholder="Buscar disciplina...">
        </label>

        <!-- O modal de cadastro será conectado a este botão depois. -->
        <button class="disciplines-add-button" type="button">
          <span aria-hidden="true">＋</span>
          Nova disciplina
        </button>
      </div>
    </header>

    <article class="disciplines-total-card">
      <span aria-hidden="true">
        <svg viewBox="0 0 24 24">
          <path d="M7 7h10v13H7z" />
          <path d="M9 7V4h6v3M10 12l2 2 3-4" />
        </svg>
      </span>
      <div>
        <p>Total de disciplinas</p>
        <strong>0</strong>
      </div>
    </article>

    <div class="disciplines-toolbar">
      <div class="disciplines-filters" aria-label="Filtrar disciplinas">
        <button
          v-for="filter in filters"
          :key="filter.value"
          type="button"
          :class="{ active: activeFilter === filter.value }"
          @click="activeFilter = filter.value"
        >
          {{ filter.label }}
        </button>
      </div>

      <div class="disciplines-view-options">
        <label class="disciplines-sort">
          <span>Ordenar por:</span>
          <select aria-label="Ordenar disciplinas">
            <option>Nome (A-Z)</option>
            <option>Nome (Z-A)</option>
            <option>Mais recentes</option>
          </select>
        </label>

        <div class="disciplines-view-buttons" aria-label="Modo de visualização">
          <button type="button" :class="{ active: viewMode === 'list' }" aria-label="Visualização em lista" @click="viewMode = 'list'">
            <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M9 6h11M9 12h11M9 18h11" /><circle cx="4" cy="6" r="1" /><circle cx="4" cy="12" r="1" /><circle cx="4" cy="18" r="1" /></svg>
          </button>
          <button type="button" :class="{ active: viewMode === 'grid' }" aria-label="Visualização em grade" @click="viewMode = 'grid'">
            <svg viewBox="0 0 24 24" aria-hidden="true"><rect x="4" y="4" width="6" height="6" /><rect x="14" y="4" width="6" height="6" /><rect x="4" y="14" width="6" height="6" /><rect x="14" y="14" width="6" height="6" /></svg>
          </button>
        </div>
      </div>
    </div>

    <article class="disciplines-empty-card" aria-labelledby="disciplines-empty-title">
      <svg class="disciplines-empty-illustration" viewBox="0 0 360 220" role="img" aria-label="Livros, caderno aberto e uma planta">
        <defs>
          <linearGradient id="book-cover" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0" stop-color="#a98df0" />
            <stop offset="1" stop-color="#7650d4" />
          </linearGradient>
        </defs>
        <circle cx="190" cy="105" r="86" fill="#f4f0ff" />
        <path d="m289 42 4 10 10 4-10 4-4 10-4-10-10-4 10-4 4-10Z" fill="#e5dcff" />
        <path d="m82 54 3 7 7 3-7 3-3 7-3-7-7-3 7-3 3-7Z" fill="#ede7ff" />

        <rect x="171" y="64" width="101" height="20" rx="5" fill="url(#book-cover)" />
        <rect x="164" y="84" width="112" height="20" rx="5" fill="#8f70df" />
        <rect x="173" y="104" width="105" height="20" rx="5" fill="#b39bea" />
        <path d="M184 69h69M177 89h80M185 109h74" stroke="#dcd1fa" stroke-width="4" stroke-linecap="round" />

        <path d="M122 178h43l-5-49h-33l-5 49Z" fill="#bdaaf0" />
        <path d="M143 133c-5-25 2-43 19-55 5 24-2 42-19 55Z" fill="#8463dc" />
        <path d="M141 137c-22-11-34-26-34-44 22 9 34 24 34 44Z" fill="#987be3" />
        <path d="M146 140c18-18 36-24 54-17-15 20-33 26-54 17Z" fill="#7655d2" />

        <path d="M158 140c22-13 43-13 63 0v52c-20-14-41-14-63 0v-52Z" fill="#fff" stroke="#8d6cdd" stroke-width="4" stroke-linejoin="round" />
        <path d="M221 140c22-13 43-13 63 0v52c-20-14-41-14-63 0v-52Z" fill="#fff" stroke="#8d6cdd" stroke-width="4" stroke-linejoin="round" />
        <path d="M221 140v52M170 153c13-5 25-5 38 0m-38 12c13-5 25-5 38 0m26-12c13-5 25-5 38 0m-38 12c13-5 25-5 38 0" fill="none" stroke="#d5c8f5" stroke-width="3" stroke-linecap="round" />
      </svg>

      <h2 id="disciplines-empty-title">Nenhuma disciplina cadastrada ainda</h2>
      <p>Adicione sua primeira disciplina para começar a organizar seus estudos e acompanhar seu desempenho.</p>

      <!-- Este botão abrirá o mesmo modal de cadastro no próximo passo. -->
      <button class="disciplines-empty-button" type="button">
        <span aria-hidden="true">＋</span>
        Adicionar disciplina
      </button>
    </article>

    <footer class="disciplines-footer">
      <p>Mostrando 0 de 0 disciplinas</p>
      <nav aria-label="Paginação das disciplinas">
        <button type="button" disabled>Anterior</button>
        <span aria-current="page">1</span>
        <button type="button" disabled>Próxima</button>
      </nav>
    </footer>
  </section>
</template>

<style scoped>
.disciplines-page {
  display: grid;
  gap: 22px;
}

.disciplines-header {
  align-items: center;
  display: flex;
  justify-content: space-between;
}

.disciplines-heading,
.disciplines-actions,
.disciplines-toolbar,
.disciplines-view-options,
.disciplines-footer,
.disciplines-footer nav {
  align-items: center;
  display: flex;
}

.disciplines-heading {
  gap: 13px;
}

.disciplines-heading-icon {
  align-items: center;
  background: #f0eaff;
  border-radius: 9px;
  color: #6b37e8;
  display: flex;
  height: 43px;
  justify-content: center;
  width: 43px;
}

.disciplines-heading-icon svg,
.disciplines-total-card svg,
.disciplines-search svg,
.disciplines-view-buttons svg {
  fill: none;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 1.8;
}

.disciplines-heading-icon svg {
  height: 25px;
  width: 25px;
}

.disciplines-heading h1 {
  color: #151a2d;
  font-size: 1.65rem;
  font-weight: 800;
  letter-spacing: -.04em;
  line-height: 1.1;
  margin: 0 0 6px;
}

.disciplines-heading p {
  color: #697086;
  font-size: .78rem;
}

.disciplines-actions {
  gap: 16px;
}

.disciplines-search {
  color: #747b90;
  position: relative;
}

.disciplines-search svg {
  height: 18px;
  left: 14px;
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 18px;
}

.disciplines-search input {
  background: #fff;
  border: 1px solid #dedfe8;
  border-radius: 8px;
  color: #252a3e;
  min-width: 255px;
  outline: none;
  padding: 12px 14px 12px 42px;
}

.disciplines-search input:focus {
  border-color: #7544eb;
  box-shadow: 0 0 0 3px rgba(117, 68, 235, .12);
}

.disciplines-add-button,
.disciplines-empty-button {
  align-items: center;
  background: linear-gradient(100deg, #5d20df, #7419f5);
  border: 0;
  border-radius: 7px;
  box-shadow: 0 8px 18px rgba(101, 31, 225, .2);
  color: #fff;
  display: flex;
  font-size: .76rem;
  font-weight: 700;
  gap: 8px;
  justify-content: center;
  padding: 12px 18px;
}

.disciplines-add-button span,
.disciplines-empty-button span {
  font-size: 1.12rem;
  font-weight: 400;
  line-height: .8;
}

.disciplines-add-button:hover,
.disciplines-empty-button:hover {
  box-shadow: 0 11px 23px rgba(101, 31, 225, .28);
  transform: translateY(-1px);
}

.disciplines-add-button:focus-visible,
.disciplines-empty-button:focus-visible {
  outline: 3px solid rgba(105, 54, 224, .28);
  outline-offset: 3px;
}

.disciplines-total-card {
  align-items: center;
  background: #fff;
  border: 1px solid #ebeaf1;
  border-radius: 10px;
  box-shadow: 0 5px 16px rgba(30, 36, 65, .035);
  display: flex;
  gap: 16px;
  padding: 18px;
  width: 340px;
}

.disciplines-total-card > span {
  align-items: center;
  background: #f0eaff;
  border-radius: 50%;
  color: #6d38e8;
  display: flex;
  height: 52px;
  justify-content: center;
  width: 52px;
}

.disciplines-total-card svg {
  height: 26px;
  width: 26px;
}

.disciplines-total-card p {
  color: #51586c;
  font-size: .7rem;
  font-weight: 650;
}

.disciplines-total-card strong {
  color: #151a2d;
  display: block;
  font-size: 1.35rem;
  line-height: 1;
  margin-top: 7px;
}

.disciplines-toolbar {
  gap: 20px;
  justify-content: space-between;
}

.disciplines-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.disciplines-filters button,
.disciplines-sort,
.disciplines-view-buttons button {
  background: #fff;
  border: 1px solid #e1e2e9;
  color: #34394c;
}

.disciplines-filters button {
  border-radius: 7px;
  font-size: .7rem;
  padding: 10px 17px;
}

.disciplines-filters button.active {
  border-color: #6f36e7;
  color: #6126d8;
  font-weight: 700;
}

.disciplines-view-options {
  gap: 12px;
}

.disciplines-sort {
  align-items: center;
  border-radius: 7px;
  display: flex;
  font-size: .68rem;
  gap: 8px;
  padding: 0 9px 0 14px;
}

.disciplines-sort span {
  font-weight: 700;
}

.disciplines-sort select {
  background: transparent;
  border: 0;
  color: #43495e;
  outline: none;
  padding: 10px 4px;
}

.disciplines-view-buttons {
  display: flex;
}

.disciplines-view-buttons button {
  align-items: center;
  display: flex;
  height: 40px;
  justify-content: center;
  width: 44px;
}

.disciplines-view-buttons button:first-child {
  border-radius: 7px 0 0 7px;
}

.disciplines-view-buttons button:last-child {
  border-left: 0;
  border-radius: 0 7px 7px 0;
}

.disciplines-view-buttons button.active {
  background: #f0eaff;
  color: #6932df;
}

.disciplines-view-buttons svg {
  height: 19px;
  width: 19px;
}

.disciplines-empty-card {
  align-items: center;
  background: #fff;
  border: 1px solid #e9e8ef;
  border-radius: 11px;
  box-shadow: 0 5px 16px rgba(30, 36, 65, .035);
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-height: 455px;
  padding: 35px;
  text-align: center;
}

.disciplines-empty-illustration {
  display: block;
  height: 200px;
  max-width: 360px;
  width: 100%;
}

.disciplines-empty-card h2 {
  color: #171c30;
  font-size: 1.16rem;
  font-weight: 800;
  letter-spacing: -.025em;
  margin: 5px 0 8px;
}

.disciplines-empty-card p {
  color: #73798e;
  font-size: .76rem;
  line-height: 1.55;
  max-width: 480px;
}

.disciplines-empty-button {
  margin-top: 20px;
}

.disciplines-footer {
  color: #73798e;
  font-size: .68rem;
  justify-content: space-between;
}

.disciplines-footer nav {
  gap: 8px;
}

.disciplines-footer button,
.disciplines-footer span {
  border-radius: 7px;
  font-size: .68rem;
  padding: 9px 14px;
}

.disciplines-footer button {
  background: #fff;
  border: 1px solid #e6e7ed;
  color: #a5a9b7;
}

.disciplines-footer button:disabled {
  cursor: not-allowed;
  opacity: .65;
}

.disciplines-footer span {
  background: #641be2;
  color: #fff;
  min-width: 38px;
  text-align: center;
}

.sr-only {
  height: 1px;
  margin: -1px;
  overflow: hidden;
  padding: 0;
  position: absolute;
  width: 1px;
  clip: rect(0, 0, 0, 0);
}

@media (max-width: 980px) {
  .disciplines-header,
  .disciplines-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .disciplines-actions,
  .disciplines-view-options {
    justify-content: space-between;
  }

  .disciplines-search {
    flex: 1;
  }

  .disciplines-search input {
    min-width: 0;
    width: 100%;
  }
}

@media (max-width: 620px) {
  .disciplines-actions,
  .disciplines-view-options,
  .disciplines-footer {
    align-items: stretch;
    flex-direction: column;
  }

  .disciplines-add-button,
  .disciplines-total-card {
    width: 100%;
  }

  .disciplines-sort {
    justify-content: space-between;
  }

  .disciplines-view-buttons button {
    flex: 1;
  }

  .disciplines-view-buttons button:first-child,
  .disciplines-view-buttons button:last-child {
    width: 50%;
  }

  .disciplines-empty-card {
    min-height: 410px;
    padding: 24px 18px;
  }

  .disciplines-empty-illustration {
    height: auto;
  }

  .disciplines-footer nav {
    justify-content: center;
  }
}
</style>
