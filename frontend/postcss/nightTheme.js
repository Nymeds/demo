// Gera, durante o build, a versão "modo noite" de cada regra CSS que usa cor: as superfícies
// claras viram o azul-marinho da barra lateral e os tons escuros viram os claros do próprio site.
// Assim o modo noite vale para todas as telas sem reescrever o CSS de cada componente.
// Os ajustes que a troca automática não resolve ficam em src/styles/theme-night.css.

const NIGHT_ROOT = ':root[data-theme="night"]'
const COLOR_PATTERN = /#(?:[0-9a-f]{6}|[0-9a-f]{3})(?![0-9a-f])|\bwhite\b/gi
const HAS_COLOR = new RegExp(COLOR_PATTERN.source, 'i')

// Troca direta entre as quatro cores-base do site.
const BASE_SWAPS = new Map([
  ['#ffffff', '#111a2f'],
  ['#f5f6fb', '#091326'],
  ['#f7f7fc', '#091326'],
  ['#111a2f', '#f5f6fb'],
  ['#091326', '#ffffff'],
])

const TEXT_PROPERTIES = new Set(['color', 'fill', 'stroke', 'caret-color'])
const IGNORED_PROPERTIES = new Set(['box-shadow', 'text-shadow', 'filter', '-webkit-tap-highlight-color'])

const NAVY_HUE = 222
const DARKEST = 0.09
const LIGHTEST = 0.95
const MIN_TEXT_LIGHTNESS = 0.62
const MIN_BORDER_LIGHTNESS = 0.24
const MIN_TINT_LIGHTNESS = 0.2
const ACCENT_TEXT_LIGHTNESS = 0.72

function expandHex(color) {
  const hex = color.toLowerCase()

  if (hex === 'white') return '#ffffff'
  if (hex.length === 4) return `#${[...hex.slice(1)].map(digit => digit + digit).join('')}`

  return hex
}

function hexToHsl(hex) {
  const value = Number.parseInt(hex.slice(1), 16)
  const red = ((value >> 16) & 255) / 255
  const green = ((value >> 8) & 255) / 255
  const blue = (value & 255) / 255
  const max = Math.max(red, green, blue)
  const min = Math.min(red, green, blue)
  const lightness = (max + min) / 2
  const delta = max - min

  if (delta === 0) return { hue: 0, saturation: 0, lightness }

  const saturation = Math.min(1, delta / (1 - Math.abs(2 * lightness - 1)))
  let hue

  if (max === red) hue = ((green - blue) / delta) % 6
  else if (max === green) hue = (blue - red) / delta + 2
  else hue = (red - green) / delta + 4

  return { hue: (hue * 60 + 360) % 360, saturation, lightness }
}

function hslToHex({ hue, saturation, lightness }) {
  const chroma = (1 - Math.abs(2 * lightness - 1)) * saturation
  const second = chroma * (1 - Math.abs(((hue / 60) % 2) - 1))
  const match = lightness - chroma / 2
  const sector = Math.floor(hue / 60) % 6
  const channels = [
    [chroma, second, 0],
    [second, chroma, 0],
    [0, chroma, second],
    [0, second, chroma],
    [second, 0, chroma],
    [chroma, 0, second],
  ][sector]

  return `#${channels
    .map(channel => Math.round((channel + match) * 255).toString(16).padStart(2, '0'))
    .join('')}`
}

function nightColor(color, property) {
  const hex = expandHex(color)
  const isText = TEXT_PROPERTIES.has(property)

  // Texto branco fica sobre botões e faixas coloridas, que continuam coloridos à noite.
  if (isText && hex === '#ffffff') return hex
  if (BASE_SWAPS.has(hex)) return BASE_SWAPS.get(hex)

  const { hue, saturation, lightness } = hexToHsl(hex)
  const isNeutral = saturation < 0.3
  const isExtreme = lightness > 0.86 || lightness < 0.24

  if (isNeutral || isExtreme) {
    let nightLightness = DARKEST + (1 - lightness) * (LIGHTEST - DARKEST)

    if (isText) nightLightness = Math.max(nightLightness, MIN_TEXT_LIGHTNESS)
    else if (property.startsWith('border') || property.startsWith('outline')) {
      nightLightness = Math.max(nightLightness, MIN_BORDER_LIGHTNESS)
    } else if (!isNeutral) nightLightness = Math.max(nightLightness, MIN_TINT_LIGHTNESS)

    // Cinzas ganham o tom azul-marinho da barra lateral; tons coloridos mantêm a própria cor.
    return hslToHex({
      hue: isNeutral ? NAVY_HUE : hue,
      saturation: isNeutral ? (nightLightness < 0.5 ? 0.38 : 0.2) : saturation,
      lightness: nightLightness,
    })
  }

  // Textos coloridos escuros ficam mais claros para continuar legíveis no fundo escuro.
  if (isText && lightness < 0.55) return hslToHex({ hue, saturation, lightness: ACCENT_TEXT_LIGHTNESS })

  return hex
}

function toNightSelector(selector) {
  const trimmed = selector.trim()

  if (trimmed.startsWith(':root')) return `${NIGHT_ROOT}${trimmed.slice(':root'.length)}`
  if (/^html\b/.test(trimmed)) return `html[data-theme="night"]${trimmed.slice('html'.length)}`

  return `${NIGHT_ROOT} ${trimmed}`
}

function isInsideKeyframes(rule) {
  for (let parent = rule.parent; parent; parent = parent.parent) {
    if (parent.type === 'atrule' && /keyframes$/i.test(parent.name)) return true
  }

  return false
}

export default function nightTheme() {
  return {
    postcssPlugin: 'acadorganize-night-theme',
    OnceExit(root, { Rule }) {
      root.walkRules(rule => {
        if (rule.selector.includes('data-theme') || isInsideKeyframes(rule)) return

        const colorDeclarations = (rule.nodes || []).filter(node => (
          node.type === 'decl' && !IGNORED_PROPERTIES.has(node.prop) && HAS_COLOR.test(node.value)
        ))

        if (colorDeclarations.length === 0) return

        // Todas as cores da regra são repetidas (mesmo as que não mudam) para manter a mesma
        // precedência entre variações como :hover e .active também no modo noite.
        const nightRule = new Rule({ selectors: rule.selectors.map(toNightSelector) })

        colorDeclarations.forEach(declaration => {
          nightRule.append(declaration.clone({
            value: declaration.value.replace(COLOR_PATTERN, color => nightColor(color, declaration.prop)),
          }))
        })

        rule.after(nightRule)
      })
    },
  }
}

nightTheme.postcss = true
