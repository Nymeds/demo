import { ref } from 'vue'
import { apiRequest } from '../../shared/http/apiRequest.js'

// Versões vigentes dos documentos legais, buscadas na API pública.
export function useLegalVersions() {
  const versions = ref(null)
  const loading = ref(false)
  const error = ref('')

  async function load() {
    loading.value = true
    error.value = ''
    try {
      versions.value = await apiRequest('/api/v1/legal/versions', { skipAuth: true })
    } catch {
      versions.value = null
      error.value = 'Não foi possível carregar a versão dos Termos de Uso.'
    } finally {
      loading.value = false
    }
  }

  return { versions, loading, error, load }
}
