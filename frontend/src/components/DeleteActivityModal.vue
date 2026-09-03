<script setup>
defineProps({
  activityTitle: { type: String, required: true },
})

const emit = defineEmits(['close', 'confirm'])
</script>

<template>
  <div class="delete-activity-backdrop" @mousedown.self="emit('close')">
    <section
      class="delete-activity-modal"
      role="alertdialog"
      aria-modal="true"
      aria-labelledby="delete-activity-title"
      aria-describedby="delete-activity-description"
    >
      <span class="delete-activity-icon" aria-hidden="true">
        <svg viewBox="0 0 24 24">
          <path d="M4 7h16M9 7V4h6v3m3 0-1 13H7L6 7m4 4v5m4-5v5" />
        </svg>
      </span>

      <h2 id="delete-activity-title">Excluir atividade?</h2>

      <p id="delete-activity-description">
        A atividade
        <span class="activity-name">“{{ activityTitle }}”</span>
        será excluída. Esta ação não pode ser desfeita.
      </p>

      <div class="delete-activity-actions">
        <button
          class="cancel-delete"
          type="button"
          autofocus
          @click="emit('close')"
        >
          Cancelar
        </button>

        <button
          class="confirm-delete"
          type="button"
          @click="emit('confirm')"
        >
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M4 7h16M9 7V4h6v3m3 0-1 13H7L6 7m4 4v5m4-5v5" />
          </svg>
          Excluir atividade
        </button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.delete-activity-backdrop {
  align-items: center;
  background: rgba(16, 20, 34, .58);
  display: flex;
  inset: 0;
  justify-content: center;
  padding: 20px;
  position: fixed;
  z-index: 110;
}

.delete-activity-modal {
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

.delete-activity-icon {
  align-items: center;
  background: #fff0ee;
  border-radius: 50%;
  color: #df3f32;
  display: flex;
  height: 56px;
  justify-content: center;
  width: 56px;
}

.delete-activity-icon svg,
.confirm-delete svg {
  fill: none;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 1.8;
}

.delete-activity-icon svg {
  height: 27px;
  width: 27px;
}

.delete-activity-modal h2 {
  color: #191e31;
  font-size: 1.2rem;
  margin: 17px 0 9px;
}

.delete-activity-modal p {
  color: #70778a;
  font-size: .76rem;
  line-height: 1.55;
  margin: 0;
}

.activity-name {
  color: #3b4053;
  font-weight: 600;
}

.delete-activity-actions {
  display: flex;
  gap: 11px;
  justify-content: center;
  margin-top: 25px;
  width: 100%;
}

.delete-activity-actions button {
  border-radius: 7px;
  cursor: pointer;
  font-size: .73rem;
  font-weight: 700;
  min-height: 42px;
  padding: 10px 17px;
}

.cancel-delete {
  background: #fff;
  border: 1px solid #dfe1e8;
  color: #3e4458;
}

.confirm-delete {
  align-items: center;
  background: #df3f32;
  border: 1px solid #df3f32;
  color: #fff;
  display: flex;
  gap: 8px;
  justify-content: center;
}

.confirm-delete:hover {
  background: #c93429;
  border-color: #c93429;
}

.confirm-delete svg {
  height: 17px;
  width: 17px;
}

.delete-activity-actions button:focus-visible {
  outline: 3px solid rgba(105, 54, 224, .25);
  outline-offset: 2px;
}

@media (max-width: 460px) {
  .delete-activity-modal {
    padding: 25px 20px;
  }

  .delete-activity-actions {
    flex-direction: column-reverse;
  }
}
</style>
