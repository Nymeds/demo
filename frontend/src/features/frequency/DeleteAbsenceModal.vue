<script setup>
import { ref } from 'vue'
import { useFocusTrap } from '../../shared/a11y/useFocusTrap.js'

const props = defineProps({
  entry: { type: Object, required: true },
  deleting: { type: Boolean, default: false },
  serverError: { type: String, default: '' },
})

const emit = defineEmits(['close', 'confirm'])

const modalRef = ref(null)
const cancelButtonRef = ref(null)
useFocusTrap(() => true, modalRef, {
  onClose: () => emit('close'),
  initialFocus: () => cancelButtonRef.value,
  closeOnEscape: () => !props.deleting,
})
</script>

<template>
  <div class="delete-absence-backdrop" @mousedown.self="!deleting && emit('close')">
    <section
      ref="modalRef"
      class="delete-absence-modal"
      role="alertdialog"
      aria-modal="true"
      aria-labelledby="delete-absence-title"
      aria-describedby="delete-absence-description"
      tabindex="-1"
    >
      <span class="delete-absence-icon" aria-hidden="true">
        <svg viewBox="0 0 24 24">
          <path d="M4 7h16M9 7V4h6v3m3 0-1 13H7L6 7m4 4v5m4-5v5" />
        </svg>
      </span>

      <h2 id="delete-absence-title">Desfazer lançamento?</h2>
      <p id="delete-absence-description">
        A falta em <span class="absence-highlight">{{ entry.disciplineName }}</span> será removida do histórico.
        Esta ação não pode ser desfeita.
      </p>

      <p v-if="serverError" class="delete-absence-error" role="alert">{{ serverError }}</p>

      <div class="delete-absence-actions">
        <button ref="cancelButtonRef" class="cancel-delete" type="button" :disabled="deleting" @click="emit('close')">
          Cancelar
        </button>
        <button class="confirm-delete" type="button" :disabled="deleting" @click="emit('confirm')">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M4 7h16M9 7V4h6v3m3 0-1 13H7L6 7m4 4v5m4-5v5" />
          </svg>
          {{ deleting ? 'Removendo…' : 'Desfazer lançamento' }}
        </button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.delete-absence-backdrop {
  align-items: center;
  background: rgba(16, 20, 34, .58);
  display: flex;
  inset: 0;
  justify-content: center;
  padding: 20px;
  position: fixed;
  z-index: 120;
}

.delete-absence-modal:focus {
  outline: none;
}

.delete-absence-error {
  background: #fff0f3;
  border-radius: 7px;
  color: #c2415f;
  font-size: .72rem;
  font-weight: 600;
  margin-top: 14px;
  padding: 10px 12px;
  width: 100%;
}

.delete-absence-modal {
  align-items: center;
  background: #fff;
  border-radius: 15px;
  box-shadow: 0 24px 70px rgba(15, 18, 35, .28);
  display: flex;
  flex-direction: column;
  max-width: 430px;
  padding: 30px;
  text-align: center;
  width: 100%;
}

.delete-absence-icon {
  align-items: center;
  background: #fff0ee;
  border-radius: 50%;
  color: #d94a5f;
  display: flex;
  flex: 0 0 56px;
  height: 56px;
  justify-content: center;
  margin-bottom: 17px;
  width: 56px;
}

.delete-absence-icon svg { fill: none; height: 26px; stroke: currentColor; stroke-linecap: round; stroke-linejoin: round; stroke-width: 1.8; width: 26px; }

.delete-absence-modal h2 { color: #171c30; font-size: 1.08rem; font-weight: 800; letter-spacing: -.025em; margin-bottom: 9px; }
.delete-absence-modal p { color: #6f7589; font-size: .76rem; line-height: 1.6; margin: 0; }
.absence-highlight { color: #2a3048; font-weight: 700; }

.delete-absence-actions { display: flex; gap: 10px; margin-top: 22px; width: 100%; }
.delete-absence-actions button { border-radius: 8px; cursor: pointer; flex: 1; font-size: .75rem; font-weight: 700; padding: 12px 16px; }
.cancel-delete { background: #fff; border: 1px solid #dfe1ea; color: #4a5066; }
.cancel-delete:hover { background: #f5f5fa; }
.confirm-delete { align-items: center; background: linear-gradient(100deg, #d63c58, #e2537c); border: 0; box-shadow: 0 8px 19px rgba(206, 60, 92, .2); color: #fff; display: flex; gap: 8px; justify-content: center; }
.confirm-delete:hover { box-shadow: 0 11px 24px rgba(206, 60, 92, .28); transform: translateY(-1px); }
.confirm-delete svg { fill: none; height: 16px; stroke: currentColor; stroke-linecap: round; stroke-linejoin: round; stroke-width: 1.8; width: 16px; }

.delete-absence-actions button:disabled { cursor: not-allowed; opacity: .55; }

button:focus-visible {
  outline: 3px solid rgba(105, 54, 224, .28);
  outline-offset: 2px;
}

/* Breakpoints padrão 760 (fonte e toque) e 520 (layout; era 420). */
@media (max-width: 760px) {
  .delete-absence-modal p,
  .delete-absence-actions button { font-size: .875rem; }
  .delete-absence-error { font-size: .75rem; }
  .delete-absence-actions button { min-height: 44px; }
}

@media (max-width: 520px) {
  .delete-absence-actions { flex-direction: column-reverse; }
}
</style>
