// O BCrypt do backend rejeita senhas com mais de 72 bytes (UTF-8): acentos contam em dobro.
export const PASSWORD_MAX_BYTES = 72
export const PASSWORD_TOO_LONG_MESSAGE = 'A senha pode ter no máximo 72 bytes (caracteres acentuados contam em dobro).'

const encoder = new TextEncoder()

export function passwordByteLength(password) {
  return encoder.encode(String(password ?? '')).length
}

export function exceedsPasswordBytes(password) {
  return passwordByteLength(password) > PASSWORD_MAX_BYTES
}
