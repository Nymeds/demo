import { getCurrentScope, nextTick, onScopeDispose, toValue, watch } from 'vue'

export const FOCUSABLE_SELECTOR = [
  'a[href]',
  'button:not([disabled])',
  'textarea:not([disabled])',
  'input:not([disabled]):not([type="hidden"])',
  'select:not([disabled])',
  '[tabindex]:not([tabindex="-1"])',
].join(',')

export function isVisible(element) {
  if (!element) return false
  if (typeof element.getClientRects === 'function' && element.getClientRects().length === 0) return false
  const style = typeof getComputedStyle === 'function' ? getComputedStyle(element) : null
  return !(style && (style.visibility === 'hidden' || style.display === 'none'))
}

export function getFocusableElements(container) {
  if (!container) return []
  return Array.from(container.querySelectorAll(FOCUSABLE_SELECTOR)).filter(isVisible)
}

/**
 * Focus trap + Escape handling + focus restoration for modal dialogs.
 *
 * @param {import('vue').Ref<boolean>|(() => boolean)} isOpen
 * @param {import('vue').Ref<HTMLElement|null>} containerRef
 * @param {object} options
 * @param {() => void} options.onClose
 * @param {() => (HTMLElement|null|undefined)} [options.initialFocus]
 * @param {boolean} [options.returnFocus=true]
 * @param {boolean|import('vue').Ref<boolean>|(() => boolean)} [options.closeOnEscape=true]
 */
export function useFocusTrap(isOpen, containerRef, options = {}) {
  const { onClose, initialFocus, returnFocus = true, closeOnEscape = true } = options
  let previouslyFocused = null
  let listening = false

  function handleKeydown(event) {
    if (event.key === 'Escape') {
      if (!toValue(closeOnEscape)) return
      event.stopPropagation()
      onClose?.()
      return
    }
    if (event.key !== 'Tab') return

    const container = containerRef.value
    if (!container) return
    const elements = getFocusableElements(container)
    if (elements.length === 0) {
      event.preventDefault()
      container.focus?.()
      return
    }
    const first = elements[0]
    const last = elements[elements.length - 1]
    const active = document.activeElement
    if (event.shiftKey && (active === first || !container.contains(active))) {
      event.preventDefault()
      last.focus()
    } else if (!event.shiftKey && (active === last || !container.contains(active))) {
      event.preventDefault()
      first.focus()
    }
  }

  function attach() {
    if (listening || typeof document === 'undefined') return
    listening = true
    document.addEventListener('keydown', handleKeydown, true)
  }

  function detach() {
    if (!listening) return
    listening = false
    document.removeEventListener('keydown', handleKeydown, true)
  }

  function release() {
    detach()
    if (returnFocus && previouslyFocused?.isConnected) previouslyFocused.focus?.()
    previouslyFocused = null
  }

  watch(() => !!toValue(isOpen), async open => {
    if (typeof document === 'undefined') return
    if (open) {
      previouslyFocused = document.activeElement
      attach()
      await nextTick()
      if (!toValue(isOpen)) return
      const container = containerRef.value
      const target = initialFocus?.() || getFocusableElements(container)[0] || container
      target?.focus?.()
    } else {
      release()
    }
  }, { immediate: true, flush: 'post' })

  if (getCurrentScope()) onScopeDispose(release)

  return { release }
}
