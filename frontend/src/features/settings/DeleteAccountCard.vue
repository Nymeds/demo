<script setup>
import { nextTick, ref } from 'vue'
import DeleteAccountModal from './DeleteAccountModal.vue'

const props = defineProps({
  api: { type: Object, required: true },
})

const emit = defineEmits(['deleted', 'failed'])

const modalOpen = ref(false)
const deleting = ref(false)
const openButton = ref(null)

async function closeModal() {
  if (deleting.value) return

  modalOpen.value = false
  await nextTick()
  openButton.value?.focus()
}

async function confirmDeletion(currentPassword) {
  deleting.value = true

  try {
    await props.api.deleteAccount({ currentPassword })
    modalOpen.value = false
    emit('deleted')
  } catch (error) {
    emit('failed', error)
  } finally {
    deleting.value = false
  }
}
</script>

<template>
  <section class="settings-card is-wide settings-danger" aria-labelledby="settings-delete-title">
    <header class="settings-card-header">
      <span class="settings-card-icon" aria-hidden="true">
        <svg viewBox="0 0 24 24"><path d="M4 7h16M9 7V4h6v3m3 0-1 13H7L6 7m4 4v5m4-5v5" /></svg>
      </span>
      <div>
        <h2 id="settings-delete-title">Excluir conta</h2>
        <p>Remove sua conta e todas as informações acadêmicas cadastradas.</p>
      </div>
    </header>

    <div class="settings-danger-body">
      <p>
        Dashboards, disciplinas, notas, frequências, atividades, eventos do calendário e preferências
        serão apagados definitivamente. Não é possível recuperar os dados depois.
      </p>
      <button ref="openButton" class="settings-button is-danger" type="button" @click="modalOpen = true">
        Excluir minha conta
      </button>
    </div>

    <DeleteAccountModal
      v-if="modalOpen"
      :deleting="deleting"
      @close="closeModal"
      @confirm="confirmDeletion"
    />
  </section>
</template>
