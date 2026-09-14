<script setup>
defineProps({
  eventTitle: { type: String, required: true },
})

const emit = defineEmits(['close', 'confirm'])
</script>

<template>
  <div class="delete-backdrop" @mousedown.self="emit('close')">
    <section
      class="delete-modal"
      role="alertdialog"
      aria-modal="true"
      aria-labelledby="delete-event-title"
      aria-describedby="delete-event-description"
    >
      <span class="delete-icon" aria-hidden="true">
        <svg viewBox="0 0 24 24">
          <path d="M4 7h16M9 7V4h6v3m3 0-1 13H7L6 7m4 4v5m4-5v5" />
        </svg>
      </span>

      <h2 id="delete-event-title">Excluir evento?</h2>
      <p id="delete-event-description">
        O evento <span class="event-title">“{{ eventTitle }}”</span> será excluído do seu calendário. Esta ação não pode ser desfeita.
      </p>

      <div class="delete-modal-actions">
        <button class="cancel-delete" type="button" autofocus @click="emit('close')">Cancelar</button>
        <button class="confirm-delete" type="button" @click="emit('confirm')">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M4 7h16M9 7V4h6v3m3 0-1 13H7L6 7m4 4v5m4-5v5" />
          </svg>
          Excluir evento
        </button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.delete-backdrop {
  align-items: center;
  background: rgba(16, 20, 34, .58);
  display: flex;
  inset: 0;
  justify-content: center;
  padding: 20px;
  position: fixed;
  z-index: 120;
}

.delete-modal {
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

.delete-icon {
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

.delete-icon svg { fill: none; height: 26px; stroke: currentColor; stroke-linecap: round; stroke-linejoin: round; stroke-width: 1.8; width: 26px; }

.delete-modal h2 { color: #171c30; font-size: 1.08rem; font-weight: 800; letter-spacing: -.025em; margin-bottom: 9px; }
.delete-modal p { color: #6f7589; font-size: .76rem; line-height: 1.6; }
.event-title { color: #2a3048; font-weight: 700; }

.delete-modal-actions { display: flex; gap: 10px; margin-top: 22px; width: 100%; }
.delete-modal-actions button { border-radius: 8px; flex: 1; font-size: .75rem; font-weight: 700; padding: 12px 16px; }
.cancel-delete { background: #fff; border: 1px solid #dfe1ea; color: #4a5066; }
.cancel-delete:hover { background: #f5f5fa; }
.confirm-delete { align-items: center; background: linear-gradient(100deg, #d63c58, #e2537c); border: 0; box-shadow: 0 8px 19px rgba(206, 60, 92, .2); color: #fff; display: flex; gap: 8px; justify-content: center; }
.confirm-delete:hover { box-shadow: 0 11px 24px rgba(206, 60, 92, .28); transform: translateY(-1px); }
.confirm-delete svg { fill: none; height: 16px; stroke: currentColor; stroke-linecap: round; stroke-linejoin: round; stroke-width: 1.8; width: 16px; }

@media (max-width: 420px) {
  .delete-modal-actions { flex-direction: column-reverse; }
}
</style>
