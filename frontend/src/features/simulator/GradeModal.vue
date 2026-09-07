```vue
<template>
  <div class="modal-overlay">
    <div class="modal-card">

      <h2>Adicionar avaliação</h2>

      <form @submit.prevent="submit">

        <div class="field">
          <label>Nome da avaliação</label>

          <input
            v-model="assessmentName"
            type="text"
            maxlength="120"
            placeholder="Ex: Prova 1"
            required
          >
        </div>


        <div class="field">
          <label>Nota obtida</label>

          <input
            v-model="score"
            type="number"
            min="0"
            max="10"
            step="0.01"
            placeholder="Ex: 8.5"
            required
          >
        </div>


        <div class="field">
          <label>Data</label>

          <input
            v-model="recordedAt"
            type="date"
            required
          >
        </div>


        <div class="actions">

          <button
            type="button"
            class="cancel-button"
            :disabled="saving"
            @click="emit('close')"
          >
            Cancelar
          </button>

          <button
            type="submit"
            class="save-button"
            :disabled="saving"
          >
            {{ saving ? 'Salvando...' : 'Adicionar avaliação' }}
          </button>

        </div>

      </form>

    </div>
  </div>
</template>


<script setup>
import { ref } from 'vue'


const props = defineProps({
  saving: {
    type: Boolean,
    default: false
  }
})


const emit = defineEmits([
  'close',
  'save'
])


const assessmentName = ref('')

const score = ref('')

const recordedAt = ref(
  new Date()
    .toISOString()
    .slice(0, 10)
)


function submit() {

  if (
    !assessmentName.value.trim() ||
    score.value === '' ||
    !recordedAt.value
  ) {
    return
  }


  emit('save', {

    assessmentName:
      assessmentName.value.trim(),

    score:
      Number(score.value),

    recordedAt:
      recordedAt.value

  })

}
</script>


<style scoped>

.modal-overlay {
  position: fixed;
  inset: 0;

  z-index: 9999;

  display: flex;
  align-items: center;
  justify-content: center;

  padding: 20px;

  background: rgba(20, 18, 35, 0.45);
}


.modal-card {
  width: 100%;
  max-width: 430px;

  box-sizing: border-box;

  padding: 26px;

  border-radius: 14px;

  background: #ffffff;

  box-shadow:
    0 20px 60px rgba(20, 18, 35, 0.2);
}


.modal-card h2 {
  margin: 0 0 22px;

  color: #202033;

  font-size: 21px;
}


.field {
  margin-bottom: 17px;
}


.field label {
  display: block;

  margin-bottom: 7px;

  color: #555267;

  font-size: 13px;
  font-weight: 600;
}


.field input {
  width: 100%;
  height: 42px;

  box-sizing: border-box;

  padding: 0 12px;

  border: 1px solid #dedce8;
  border-radius: 8px;

  background: #ffffff;

  color: #252338;

  font-size: 14px;

  outline: none;
}


.field input:focus {
  border-color: #6330e0;

  box-shadow:
    0 0 0 2px rgba(99, 48, 224, 0.08);
}


.actions {
  display: flex;
  justify-content: flex-end;

  gap: 10px;

  margin-top: 24px;
}


.actions button {
  height: 40px;

  padding: 0 18px;

  border-radius: 8px;

  font-size: 14px;
  font-weight: 600;

  cursor: pointer;
}


.cancel-button {
  border: 1px solid #dedce8;

  background: #ffffff;

  color: #555267;
}


.save-button {
  border: none;

  background: #6330e0;

  color: #ffffff;
}


.save-button:hover:not(:disabled) {
  background: #5726ce;
}


.actions button:disabled {
  opacity: 0.6;

  cursor: not-allowed;
}

</style>
```
