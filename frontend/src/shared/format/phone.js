const MAX_DIGITS = 11
const COUNTRY_CODE_WITH_MOBILE_LENGTH = 13

export const PHONE_MESSAGES = {
  areaCode: 'DDD inválido: use um DDD entre 11 e 99.',
  notMobile: 'Número de celular inválido: após o DDD o número deve começar com 9.',
  incomplete: 'Informe o celular completo com DDD: (DD) 9XXXX-XXXX.',
}

/** Só dígitos; descarta o 55 inicial apenas com 13 dígitos; limita a 11. */
export function normalizePhoneDigits(raw) {
  let digits = String(raw ?? '').replace(/\D/g, '')
  if (digits.length === COUNTRY_CODE_WITH_MOBILE_LENGTH && digits.startsWith('55')) {
    digits = digits.slice(2)
  }
  return digits.slice(0, MAX_DIGITS)
}

/** "(DD) 9XXXX-XXXX" progressivo: 2 dígitos ficam em "(DD" e o ")" surge com o 3º (apagar continua natural). */
export function formatPhoneBR(raw) {
  const digits = normalizePhoneDigits(raw)
  if (!digits) return ''
  if (digits.length <= 2) return `(${digits}`
  const rest = digits.slice(2)
  const local = rest.length > 5 ? `${rest.slice(0, 5)}-${rest.slice(5)}` : rest
  return `(${digits.slice(0, 2)}) ${local}`
}

/** Posição do cursor logo após o n-ésimo dígito do texto formatado. */
export function caretAfterDigits(formatted, digitCount) {
  if (digitCount <= 0) return 0
  let seen = 0
  for (let index = 0; index < formatted.length; index += 1) {
    if (/\d/.test(formatted[index])) {
      seen += 1
      if (seen === digitCount) return index + 1
    }
  }
  return formatted.length
}

/** { valid, code, message }. Vazio é válido (campo opcional). */
export function validatePhoneBR(raw) {
  const digits = normalizePhoneDigits(raw)
  if (!digits) return { valid: true, code: null, message: '' }
  if (digits[0] === '0' || (digits.length >= 2 && digits[1] === '0')) {
    return { valid: false, code: 'area-code', message: PHONE_MESSAGES.areaCode }
  }
  if (digits.length >= 3 && digits[2] !== '9') {
    return { valid: false, code: 'not-mobile', message: PHONE_MESSAGES.notMobile }
  }
  if (digits.length < MAX_DIGITS) {
    return { valid: false, code: 'incomplete', message: PHONE_MESSAGES.incomplete }
  }
  return { valid: true, code: null, message: '' }
}
