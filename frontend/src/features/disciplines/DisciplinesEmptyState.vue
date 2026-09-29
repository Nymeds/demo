<script setup>
const emit = defineEmits(['add'])
</script>

<template>
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

    <button class="disciplines-empty-button" type="button" @click="emit('add')">
      <span aria-hidden="true">＋</span>
      Adicionar disciplina
    </button>
  </article>
</template>
