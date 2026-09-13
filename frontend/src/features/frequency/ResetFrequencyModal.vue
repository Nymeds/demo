<script setup>
defineProps({
  disciplineName: { type: String, required: true },
  absences: { type: Number, required: true },
})

const emit = defineEmits(['close', 'confirm'])
</script>

<template>
  <div class="reset-backdrop" @mousedown.self="emit('close')">
    <section
      class="reset-modal"
      role="alertdialog"
      aria-modal="true"
      aria-labelledby="reset-title"
      aria-describedby="reset-description"
    >
      <span class="reset-icon" aria-hidden="true">
        <svg viewBox="0 0 24 24">
          <path d="M4 7h16M9 7V4h6v3m3 0-1 13H7L6 7m4 4v5m4-5v5" />
        </svg>
      </span>

      <h2 id="reset-title">Zerar as faltas?</h2>
      <p id="reset-description">
        As {{ absences }} {{ absences === 1 ? 'falta' : 'faltas' }} de
        <span class="discipline-name">“{{ disciplineName }}”</span> voltam para zero e a frequência retorna a 100%.
        A disciplina e o total de aulas continuam como estão.
      </p>

      <div class="reset-modal-actions">
        <button class="cancel-reset" type="button" autofocus @click="emit('close')">Cancelar</button>
        <button class="confirm-reset" type="button" @click="emit('confirm')">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M4 10h10a5 5 0 0 1 0 10H9" />
            <path d="m8 6-4 4 4 4" />
          </svg>
          Zerar faltas
        </button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.reset-backdrop {
  align-items: center;
  background: rgba(16, 20, 34, .58);
  display: flex;
  inset: 0;
  justify-content: center;
  padding: 20px;
  position: fixed;
  z-index: 110;
}

.reset-modal {
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

.reset-icon {
  align-items: center;
  background: #fff0ee;
  border-radius: 50%;
  color: #df3f32;
  display: flex;
  height: 56px;
  justify-content: center;
  width: 56px;
}

.reset-icon svg,
.confirm-reset svg {
  fill: none;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 1.8;
}

.reset-icon svg {
  height: 27px;
  width: 27px;
}

.reset-modal h2 {
  color: #191e31;
  font-size: 1.2rem;
  margin: 17px 0 9px;
}

.reset-modal p {
  color: #70778a;
  font-size: .76rem;
  line-height: 1.55;
  margin: 0;
}

.reset-modal .discipline-name {
  color: #3b4053;
  font-weight: 400;
}

.reset-modal-actions {
  display: flex;
  gap: 11px;
  justify-content: center;
  margin-top: 25px;
  width: 100%;
}

.reset-modal-actions button {
  border-radius: 7px;
  font-size: .73rem;
  font-weight: 700;
  min-height: 42px;
  padding: 10px 17px;
}

.cancel-reset {
  background: #fff;
  border: 1px solid #dfe1e8;
  color: #3e4458;
}

.confirm-reset {
  align-items: center;
  background: #df3f32;
  border: 1px solid #df3f32;
  color: #fff;
  display: flex;
  gap: 8px;
  justify-content: center;
}

.confirm-reset:hover {
  background: #c93429;
  border-color: #c93429;
}

.confirm-reset svg {
  height: 17px;
  width: 17px;
}

.reset-modal-actions button:focus-visible {
  outline: 3px solid rgba(105, 54, 224, .25);
  outline-offset: 2px;
}

@media (max-width: 460px) {
  .reset-modal {
    padding: 25px 20px;
  }

  .reset-modal-actions {
    flex-direction: column-reverse;
  }
}
</style>
