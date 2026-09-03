<script setup>
defineProps({
  message: { type: String, default: '' },
  type: { type: String, default: 'success' },
})

defineEmits(['close'])
</script>

<template>
  <Transition name="toast">
    <aside v-if="message" :class="['app-toast', `is-${type}`]" role="status" aria-live="polite">
      <span class="app-toast-icon" aria-hidden="true">{{ type === 'success' ? '✓' : '!' }}</span>
      <p>{{ message }}</p>
      <button type="button" aria-label="Fechar mensagem" @click="$emit('close')">×</button>
    </aside>
  </Transition>
</template>

<style scoped>
.app-toast {
  align-items: center;
  background: #fff;
  border: 1px solid #dce5df;
  border-radius: 12px;
  box-shadow: 0 16px 42px rgba(24, 31, 50, .18);
  display: flex;
  gap: 10px;
  max-width: min(420px, calc(100vw - 32px));
  padding: 12px 12px 12px 14px;
  position: fixed;
  right: 22px;
  top: 22px;
  z-index: 200;
}

.app-toast-icon { align-items: center; background: #e8f8ef; border-radius: 50%; color: #218950; display: flex; flex: 0 0 22px; font-size: .78rem; font-weight: 900; height: 22px; justify-content: center; }
.app-toast p { color: #2e3d35; font-size: .78rem; line-height: 1.35; margin: 0; }
.app-toast button { background: transparent; border: 0; color: #758078; cursor: pointer; font-size: 1.15rem; line-height: 1; margin-left: 4px; padding: 3px; }
.app-toast.is-error { border-color: #f0d2cf; }
.app-toast.is-error .app-toast-icon { background: #fff0ef; color: #c5423a; }
.app-toast.is-error p { color: #8e302b; }
.toast-enter-active, .toast-leave-active { transition: opacity .18s ease, transform .18s ease; }
.toast-enter-from, .toast-leave-to { opacity: 0; transform: translateY(-10px); }
@media (max-width: 620px) { .app-toast { left: 16px; right: 16px; top: 16px; max-width: none; } }
</style>
