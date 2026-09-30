import assert from 'node:assert/strict'
import test from 'node:test'
import {
  caretAfterDigits,
  formatPhoneBR,
  normalizePhoneDigits,
  validatePhoneBR,
} from '../src/shared/format/phone.js'

test('formata progressivamente durante a digitação', () => {
  assert.equal(formatPhoneBR(''), '')
  assert.equal(formatPhoneBR('1'), '(1')
  assert.equal(formatPhoneBR('11'), '(11')
  assert.equal(formatPhoneBR('119'), '(11) 9')
  assert.equal(formatPhoneBR('1191234'), '(11) 91234')
  assert.equal(formatPhoneBR('11912345'), '(11) 91234-5')
  assert.equal(formatPhoneBR('11912345678'), '(11) 91234-5678')
})

test('remove não dígitos e limita a 11 dígitos', () => {
  assert.equal(formatPhoneBR('abc(11)9-1234x5678999'), '(11) 91234-5678')
  assert.equal(normalizePhoneDigits('119123456789999'), '11912345678')
})

test('colagem normaliza +55 só com 13 dígitos', () => {
  assert.equal(normalizePhoneDigits('+55 11 91234-5678'), '11912345678')
  assert.equal(normalizePhoneDigits('11912345678'), '11912345678')
  assert.equal(normalizePhoneDigits('5511912345678'), '11912345678')
  assert.equal(normalizePhoneDigits('55912345678'), '55912345678')
})

test('validação do celular brasileiro', () => {
  assert.equal(validatePhoneBR('').valid, true)
  assert.equal(validatePhoneBR('(11) 91234-5678').valid, true)
  assert.equal(validatePhoneBR('(11) 8').code, 'not-mobile')
  assert.equal(
    validatePhoneBR('11812345678').message,
    'Número de celular inválido: após o DDD o número deve começar com 9.',
  )
  assert.equal(validatePhoneBR('(11) 9123').code, 'incomplete')
  assert.equal(
    validatePhoneBR('11').message,
    'Informe o celular completo com DDD: (DD) 9XXXX-XXXX.',
  )
  assert.equal(validatePhoneBR('01912345678').code, 'area-code')
  assert.equal(validatePhoneBR('10912345678').code, 'area-code')
})

test('cursor acompanha a contagem de dígitos', () => {
  assert.equal(caretAfterDigits('(11) 91234-5678', 0), 0)
  assert.equal(caretAfterDigits('(11) 91234-5678', 2), 3)
  assert.equal(caretAfterDigits('(11) 91234-5678', 3), 6)
  assert.equal(caretAfterDigits('(11) 91234-5678', 8), 12)
  assert.equal(caretAfterDigits('(11) 91234-5678', 11), 15)
})
