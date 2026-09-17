import { readonly, ref } from 'vue'

// Endereço temporário (object URL) da foto de perfil do usuário conectado. Fica num lugar só
// para a barra lateral e a tela de Configurações mostrarem sempre a mesma imagem.
const avatarUrl = ref('')

function replaceAvatarUrl(nextUrl) {
  if (avatarUrl.value) URL.revokeObjectURL(avatarUrl.value)
  avatarUrl.value = nextUrl
}

export function useAvatar() {
  async function loadAvatar(api) {
    const image = await api.getAvatar()
    replaceAvatarUrl(image ? URL.createObjectURL(image) : '')
  }

  function showAvatar(image) {
    replaceAvatarUrl(URL.createObjectURL(image))
  }

  function clearAvatar() {
    replaceAvatarUrl('')
  }

  return { avatarUrl: readonly(avatarUrl), loadAvatar, showAvatar, clearAvatar }
}
