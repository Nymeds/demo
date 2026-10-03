<script setup>
defineProps({
  loading: { type: Boolean, default: false },
  error: { type: String, default: '' },
})

const emit = defineEmits(['retry', 'navigate'])
</script>

<template>
  <article v-if="loading" class="dashboard-empty-hero">
    <h2>Atualizando seu dashboard…</h2>
    <p>Estamos reunindo suas disciplinas e atividades.</p>
  </article>
  <article v-else-if="error" class="dashboard-empty-hero">
    <h2>Não foi possível carregar o dashboard</h2>
    <p>{{ error }}</p>
    <button type="button" @click="emit('retry')">Tentar novamente</button>
  </article>
  <article v-else class="dashboard-empty-hero">
    <svg class="dashboard-empty-illustration" viewBox="0 0 420 190" role="img" aria-label="Ilustração de um painel acadêmico vazio">
      <defs>
        <linearGradient id="window-gradient" x1="0" y1="0" x2="1" y2="1">
          <stop class="dh-scf3efff" offset="0" />
          <stop class="dh-sce3dcff" offset="1" />
        </linearGradient>
        <linearGradient id="purple-gradient" x1="0" y1="0" x2="1" y2="1">
          <stop class="dh-sc8b6cf1" offset="0" />
          <stop class="dh-sc6642d8" offset="1" />
        </linearGradient>
      </defs>
      <path class="dh-se5dfff" d="M45 164h330" stroke-width="3" stroke-linecap="round" />
      <rect class="dh-sd7cefb" x="112" y="25" width="205" height="137" rx="10" fill="url(#window-gradient)" stroke-width="2" />
      <path d="M112 36a11 11 0 0 1 11-11h183a11 11 0 0 1 11 11v15H112V36Z" fill="url(#purple-gradient)" />
      <circle class="dh-fddd5ff" cx="128" cy="38" r="4" /><circle class="dh-fddd5ff" cx="141" cy="38" r="4" /><circle class="dh-fddd5ff" cx="154" cy="38" r="4" />
      <rect class="dh-fdcd4fb" x="132" y="66" width="47" height="7" rx="3.5" /><rect class="dh-fe5dffd" x="132" y="84" width="34" height="6" rx="3" />
      <circle class="dh-fd7cff8" cx="137" cy="110" r="4" /><rect class="dh-fe3dcfc" x="149" y="107" width="27" height="6" rx="3" />
      <circle class="dh-fd7cff8" cx="137" cy="130" r="4" /><rect class="dh-fe3dcfc" x="149" y="127" width="22" height="6" rx="3" />
      <rect class="dh-ff9f8ff dh-sddd6fb" x="195" y="65" width="101" height="78" rx="6" />
      <path class="dh-s9d84ed" d="m211 126 22-19 18 10 30-35" fill="none" stroke-width="4" stroke-linecap="round" stroke-linejoin="round" />
      <circle class="dh-f8567e6" cx="211" cy="126" r="5" /><circle class="dh-f8567e6" cx="233" cy="107" r="5" /><circle class="dh-f8567e6" cx="251" cy="117" r="5" /><circle class="dh-f8567e6" cx="281" cy="82" r="5" />
      <path class="dh-fb9a7f0" d="M70 160h45l-5-38H75l-5 38Z" /><path class="dh-f8062dc" d="M92 123c-1-21 7-36 20-45 2 21-6 36-20 45Zm-2 1c-18-13-25-28-21-44 17 12 25 27 21 44Zm4 0c15-11 30-13 43-7-12 15-27 19-43 7Z" />
      <rect class="dh-f7655dc" x="323" y="139" width="55" height="11" rx="3" /><rect class="dh-fa78eea" x="316" y="150" width="62" height="11" rx="3" /><rect class="dh-fc1aff3" x="328" y="128" width="49" height="11" rx="3" />
      <path class="dh-fc6b4f8" d="m72 43 4 9 9 4-9 4-4 9-4-9-9-4 9-4 4-9Zm280 21 3 7 7 3-7 3-3 7-3-7-7-3 7-3 3-7Z" />
    </svg>
    <h2 id="dashboard-empty-title">Seu dashboard está vazio por enquanto</h2>
    <p>Cadastre suas disciplinas para começar a montar seu resumo acadêmico.</p>
    <button type="button" @click="emit('navigate', 'disciplines')">
      <span aria-hidden="true">＋</span>
      Cadastrar primeira disciplina
    </button>
  </article>
</template>
