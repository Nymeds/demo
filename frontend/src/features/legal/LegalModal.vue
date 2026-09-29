<script setup>
import { ref } from 'vue'
import { useFocusTrap } from '../../shared/a11y/useFocusTrap.js'
import PrivacyPolicy from './PrivacyPolicy.vue'
import TermsOfUse from './TermsOfUse.vue'

const props = defineProps({
  document: { type: String, required: true }, // 'terms' | 'privacy'
  version: { type: String, default: '' },
})
const emit = defineEmits(['close'])

const dialog = ref(null)
const closeButton = ref(null)

useFocusTrap(() => true, dialog, {
  onClose: () => emit('close'),
  initialFocus: () => closeButton.value,
})
</script>

<template>
  <Teleport to="body">
  <div class="legal-backdrop" @click.self="emit('close')">
    <div ref="dialog" class="legal-dialog" role="dialog" aria-modal="true" aria-labelledby="legal-dialog-title">
      <header class="legal-header">
        <h2 id="legal-dialog-title">{{ props.document === 'terms' ? 'Termos de Uso' : 'Política de Privacidade' }}</h2>
        <button ref="closeButton" type="button" class="legal-close" @click="emit('close')">Fechar</button>
      </header>
      <div class="legal-body" tabindex="0">
        <TermsOfUse v-if="props.document === 'terms'" :version="version" />
        <PrivacyPolicy v-else :version="version" />
      </div>
    </div>
  </div>
  </Teleport>
</template>

<style scoped>
.legal-backdrop { align-items: center; background: rgba(35, 28, 66, .55); display: flex; inset: 0; justify-content: center; padding: 16px; position: fixed; z-index: 1000; }
.legal-dialog { background: #fff; border-radius: 12px; box-shadow: 0 20px 50px rgba(35, 28, 66, .3); display: flex; flex-direction: column; max-height: min(85vh, 720px); max-width: 640px; width: 100%; }
.legal-header { align-items: center; border-bottom: 1px solid #e6e2f2; display: flex; justify-content: space-between; padding: 14px 20px; }
.legal-header h2 { color: #302d41; font-size: 1.05rem; margin: 0; }
.legal-close { background: #6748d7; border: 0; border-radius: 8px; color: #fff; cursor: pointer; font-size: .8rem; font-weight: 650; padding: 8px 14px; }
.legal-close:focus-visible, .legal-body:focus-visible { outline: 3px solid #b9a7f5; outline-offset: 2px; }
.legal-body { color: #464256; font-size: .85rem; line-height: 1.6; overflow-y: auto; padding: 16px 20px 22px; }
.legal-body :deep(h3) { color: #302d41; font-size: 1.1rem; margin: 12px 0 4px; }
.legal-body :deep(h4) { color: #302d41; font-size: .9rem; margin: 16px 0 4px; }
.legal-body :deep(p), .legal-body :deep(ul) { margin: 4px 0; }
.legal-body :deep(ul) { padding-left: 20px; }
.legal-body :deep(.legal-draft) { background: #fff6dd; border-left: 4px solid #d99a1c; border-radius: 6px; color: #6b4a00; padding: 10px 12px; }
.legal-body :deep(.legal-version) { color: #77728a; font-size: .75rem; }
</style>
