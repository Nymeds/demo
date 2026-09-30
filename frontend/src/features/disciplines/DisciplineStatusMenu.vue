<script setup>
import { ref } from 'vue'
import { STATUS_OPTIONS } from './disciplinesPresentation.js'

defineProps({
  menu: { type: Object, required: true },
  updating: { type: Boolean, default: false },
})

const emit = defineEmits(['select'])

const menuElement = ref(null)

defineExpose({ focusFirstOption: () => menuElement.value?.querySelector('button')?.focus() })
</script>

<template>
  <Teleport to="body">
    <div
      ref="menuElement"
      class="discipline-status-menu"
      :style="{ left: `${menu.left}px`, top: `${menu.top}px` }"
      role="menu"
      :aria-label="`Alterar situação de ${menu.disciplineName}`"
      @click.stop
    >
      <p>Alterar situação</p>
      <button
        v-for="option in STATUS_OPTIONS"
        :key="option.value"
        type="button"
        role="menuitemradio"
        :aria-checked="menu.currentStatus === option.value"
        :class="{ active: menu.currentStatus === option.value }"
        :disabled="updating"
        @click="emit('select', option.value)"
      >
        <span :class="['status-option-dot', option.className]" aria-hidden="true"></span>
        {{ option.label }}
        <svg v-if="menu.currentStatus === option.value" viewBox="0 0 24 24" aria-hidden="true"><path d="m5 12 4 4L19 6" /></svg>
      </button>
    </div>
  </Teleport>
</template>

<style scoped>
.discipline-status-menu {
  background: #fff;
  border: 1px solid #e1e2e9;
  border-radius: 10px;
  box-shadow: 0 14px 35px rgba(26, 30, 48, .18);
  padding: 7px;
  position: fixed;
  width: 190px;
  z-index: 300;
}

.discipline-status-menu p {
  color: #747b8e;
  font-size: .63rem;
  font-weight: 700;
  padding: 7px 9px 6px;
}

.discipline-status-menu button {
  align-items: center;
  background: transparent;
  border: 0;
  border-radius: 7px;
  color: #343a4e;
  display: flex;
  font-size: .72rem;
  gap: 9px;
  padding: 9px;
  text-align: left;
  width: 100%;
}

.discipline-status-menu button:hover,
.discipline-status-menu button:focus-visible,
.discipline-status-menu button.active {
  background: #f4f1ff;
  color: #5f2bcf;
}

.discipline-status-menu button:focus-visible {
  outline: 2px solid rgba(105, 54, 224, .28);
  outline-offset: -2px;
}

.discipline-status-menu button:disabled {
  cursor: wait;
}

.discipline-status-menu button > svg {
  fill: none;
  height: 15px;
  margin-left: auto;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 2.2;
  width: 15px;
}

.status-option-dot {
  background: #8b91a1;
  border-radius: 50%;
  flex: 0 0 8px;
  height: 8px;
  width: 8px;
}

.status-option-dot.is-progress {
  background: #7544eb;
}

.status-option-dot.is-success {
  background: #25a65d;
}

/* Breakpoint padrão 760: fonte mínima de 12 px e itens de toque de 44 px. O menu é compactado para
   caber nos 156 px que statusMenuPosition reserva: 2 (borda) + 4 (padding) + 16 (título) + 3 × 44 = 154. */
@media (max-width: 760px) {
  .discipline-status-menu {
    padding: 2px;
  }

  .discipline-status-menu p {
    font-size: .75rem;
    line-height: 1;
    padding: 2px 9px;
  }

  .discipline-status-menu button {
    font-size: .875rem;
    min-height: 44px;
  }
}
</style>
