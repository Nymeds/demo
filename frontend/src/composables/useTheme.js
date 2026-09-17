import { computed, ref, watch } from 'vue'

// Enquanto o modo noite está em teste, a escolha fica guardada só neste navegador.
const STORAGE_KEY = 'acadorganize:theme'
const THEMES = Object.freeze({ DAY: 'day', NIGHT: 'night' })

function readStoredTheme() {
  try {
    return localStorage.getItem(STORAGE_KEY) === THEMES.NIGHT ? THEMES.NIGHT : THEMES.DAY
  } catch {
    // Navegador bloqueando o armazenamento: começa sempre no modo dia.
    return THEMES.DAY
  }
}

function applyTheme(value) {
  document.documentElement.dataset.theme = value
}

const theme = ref(readStoredTheme())

// Aplicado já na importação (em main.js), antes de montar o app, para a tela não piscar.
applyTheme(theme.value)

watch(theme, value => {
  applyTheme(value)

  try {
    localStorage.setItem(STORAGE_KEY, value)
  } catch {
    // Sem armazenamento disponível, o tema vale só até a página ser recarregada.
  }
})

export function useTheme() {
  const isNight = computed(() => theme.value === THEMES.NIGHT)

  function toggleTheme() {
    theme.value = isNight.value ? THEMES.DAY : THEMES.NIGHT
  }

  return { isNight, toggleTheme }
}
