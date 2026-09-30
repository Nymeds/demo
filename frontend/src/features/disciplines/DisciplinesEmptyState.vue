<script setup>
const emit = defineEmits(['add'])
</script>

<template>
  <article class="disciplines-empty-card" aria-labelledby="disciplines-empty-title">
    <svg class="disciplines-empty-illustration" viewBox="0 0 360 220" role="img" aria-label="Livros, caderno aberto e uma planta">
      <defs>
        <linearGradient id="book-cover" x1="0" y1="0" x2="1" y2="1">
          <stop class="de-sca98df0" offset="0" />
          <stop class="de-sc7650d4" offset="1" />
        </linearGradient>
      </defs>
      <circle class="de-ff4f0ff" cx="190" cy="105" r="86" />
      <path class="de-fe5dcff" d="m289 42 4 10 10 4-10 4-4 10-4-10-10-4 10-4 4-10Z" />
      <path class="de-fede7ff" d="m82 54 3 7 7 3-7 3-3 7-3-7-7-3 7-3 3-7Z" />

      <rect x="171" y="64" width="101" height="20" rx="5" fill="url(#book-cover)" />
      <rect class="de-f8f70df" x="164" y="84" width="112" height="20" rx="5" />
      <rect class="de-fb39bea" x="173" y="104" width="105" height="20" rx="5" />
      <path class="de-sdcd1fa" d="M184 69h69M177 89h80M185 109h74" stroke-width="4" stroke-linecap="round" />

      <path class="de-fbdaaf0" d="M122 178h43l-5-49h-33l-5 49Z" />
      <path class="de-f8463dc" d="M143 133c-5-25 2-43 19-55 5 24-2 42-19 55Z" />
      <path class="de-f987be3" d="M141 137c-22-11-34-26-34-44 22 9 34 24 34 44Z" />
      <path class="de-f7655d2" d="M146 140c18-18 36-24 54-17-15 20-33 26-54 17Z" />

      <path class="de-ffff de-s8d6cdd" d="M158 140c22-13 43-13 63 0v52c-20-14-41-14-63 0v-52Z" stroke-width="4" stroke-linejoin="round" />
      <path class="de-ffff de-s8d6cdd" d="M221 140c22-13 43-13 63 0v52c-20-14-41-14-63 0v-52Z" stroke-width="4" stroke-linejoin="round" />
      <path class="de-sd5c8f5" d="M221 140v52M170 153c13-5 25-5 38 0m-38 12c13-5 25-5 38 0m26-12c13-5 25-5 38 0m-38 12c13-5 25-5 38 0" fill="none" stroke-width="3" stroke-linecap="round" />
    </svg>

    <h2 id="disciplines-empty-title">Nenhuma disciplina cadastrada ainda</h2>
    <p>Adicione sua primeira disciplina para começar a organizar seus estudos e acompanhar seu desempenho.</p>

    <button class="disciplines-empty-button" type="button" @click="emit('add')">
      <span aria-hidden="true">＋</span>
      Adicionar disciplina
    </button>
  </article>
</template>
