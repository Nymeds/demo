// Reescreve imports relativos de um SFC compilado para URLs file:// absolutas,
// pois modulos data: nao conseguem resolver especificadores relativos.
// Especificadores sem extensao recebem ".js" (o Vite faz isso; o Node nao).
export function rewriteRelativeImports(code, sfcUrl) {
  return code.replace(/(from\s+|import\s+)(['"])(\.{1,2}\/[^'"]+)\2/g, (_, prefix, quote, path) => {
    const file = /\.[a-z]+$/i.test(path) ? path : `${path}.js`
    return `${prefix}${JSON.stringify(new URL(file, sfcUrl).href)}`
  })
}
