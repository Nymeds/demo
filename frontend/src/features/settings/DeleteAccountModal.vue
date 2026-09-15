<script setup>
import { computed, onMounted, ref } from 'vue'

// Digitar a palavra evita que a exclusão aconteça por um clique distraído.
const CONFIRMATION_WORD = 'EXCLUIR'

const props = defineProps({
  deleting: { type: Boolean, default: false },
})

const emit = defineEmits(['close', 'confirm'])

const confirmation = ref('')
const currentPassword = ref('')
const confirmationInput = ref(null)
const dialog = ref(null)

// aria-modal promete que o foco não sai do diálogo; sem isto, o Tab alcançaria a tela de trás.
function keepFocusInside(event) {
  const focusable = [...dialog.value.querySelectorAll('input:not([disabled]), button:not([disabled])')]

  if (focusable.length === 0) return

  const first = focusable[0]
  const last = focusable[focusable.length - 1]

  if (event.shiftKey && document.activeElement === first) {
    event.preventDefault()
    last.focus()
  } else if (!event.shiftKey && document.activeElement === last) {
    event.preventDefault()
    first.focus()
  }
}

const canConfirm = computed(() => (
  confirmation.value.trim().toUpperCase() === CONFIRMATION_WORD
  && currentPassword.value.length > 0
  && !props.deleting
))

function submit() {
  if (canConfirm.value) emit('confirm', currentPassword.value)
}

onMounted(() => confirmationInput.value?.focus())
</script>

<template>
  <div class="account-delete-backdrop" @mousedown.self="emit('close')" @keydown.esc="emit('close')">
    <section
      ref="dialog"
      class="account-delete-modal"
      role="alertdialog"
      @keydown.tab="keepFocusInside"
      aria-modal="true"
      aria-labelledby="account-delete-title"
      aria-describedby="account-delete-description"
    >
      <span class="account-delete-icon" aria-hidden="true">
        <svg viewBox="0 0 24 24"><path d="M12 9v4m0 4h.01M10.3 3.9 2.4 17.5A2 2 0 0 0 4.1 20.5h15.8a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0Z" /></svg>
      </span>

      <h2 id="account-delete-title">Excluir sua conta?</h2>
      <p id="account-delete-description">
        Todos os seus dados acadêmicos serão apagados definitivamente. Esta ação não pode ser desfeita.
      </p>

      <form class="account-delete-form" @submit.prevent="submit">
        <label class="settings-field">
          <span>Digite <strong>{{ CONFIRMATION_WORD }}</strong> para confirmar</span>
          <input
            ref="confirmationInput"
            v-model="confirmation"
            type="text"
            autocomplete="off"
            autocapitalize="characters"
            spellcheck="false"
          >
        </label>

        <label class="settings-field">
          Senha atual
          <input v-model="currentPassword" type="password" autocomplete="current-password" required maxlength="72">
        </label>

        <div class="account-delete-actions">
          <button class="settings-button is-secondary" type="button" :disabled="deleting" @click="emit('close')">
            Cancelar
          </button>
          <button class="settings-button is-danger" type="submit" :disabled="!canConfirm">
            {{ deleting ? 'Excluindo…' : 'Excluir definitivamente' }}
          </button>
        </div>
      </form>
    </section>
  </div>
</template>

<style scoped>
.account-delete-backdrop {
  align-items: center;
  background: rgba(16, 20, 34, .58);
  display: flex;
  inset: 0;
  justify-content: center;
  padding: 20px;
  position: fixed;
  z-index: 110;
}

.account-delete-modal {
  background: #fff;
  border-radius: 15px;
  box-shadow: 0 24px 70px rgba(15, 18, 35, .28);
  max-width: 440px;
  padding: 28px;
  text-align: center;
  width: 100%;
}

.account-delete-icon {
  align-items: center;
  background: #fff0ee;
  border-radius: 50%;
  color: #df3f32;
  display: inline-flex;
  height: 56px;
  justify-content: center;
  width: 56px;
}

.account-delete-icon svg {
  fill: none;
  height: 27px;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 1.8;
  width: 27px;
}

.account-delete-modal h2 {
  color: #191e31;
  font-size: 1.2rem;
  margin: 16px 0 8px;
}

.account-delete-modal > p {
  color: #70778a;
  font-size: .76rem;
  line-height: 1.55;
  margin: 0;
}

.account-delete-form {
  display: grid;
  gap: 13px;
  margin-top: 20px;
  text-align: left;
}

.account-delete-actions {
  display: flex;
  gap: 10px;
  justify-content: center;
  margin-top: 8px;
}

.account-delete-actions .settings-button {
  flex: 1;
}

@media (max-width: 460px) {
  .account-delete-modal {
    padding: 24px 18px;
  }

  .account-delete-actions {
    flex-direction: column-reverse;
  }
}
</style>
