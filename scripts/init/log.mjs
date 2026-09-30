// Saída do pipeline: cada linha recebe um prefixo colorido pela origem ([INIT], [DB], [BACK], [FRONT]).
// Linhas de erro ficam em vermelho, mas mantêm o prefixo para mostrar de onde vieram.

const useColor = !process.env.NO_COLOR && (Boolean(process.stdout.isTTY) || Boolean(process.env.FORCE_COLOR));

const COLORS = { bold: 1, red: 31, green: 32, yellow: 33, magenta: 35, cyan: 36, white: 37 };

const SOURCE_STYLES = {
  INIT: ['bold', 'white'],
  DB: ['cyan'],
  BACK: ['green'],
  FRONT: ['magenta'],
};

const PREFIX_WIDTH = 7; // "[FRONT]"
const ERROR_PATTERN = /ERROR|FATAL|Exception|failed|FAILED/;
const WARN_PATTERN = /\bWARN(ING)?\b/;

function paint(styles, text) {
  if (!useColor) return text;
  const codes = styles.map((style) => COLORS[style]).join(';');
  return `\x1b[${codes}m${text}\x1b[0m`;
}

function formatLine(source, text, styles) {
  const prefix = paint(SOURCE_STYLES[source], `[${source}]`.padEnd(PREFIX_WIDTH));
  return `${prefix} ${styles.length ? paint(styles, text) : text}`;
}

/** Cria um logger para uma origem. `stream(text, isStderr)` classifica a linha pelo conteúdo. */
export function createLogger(source) {
  const write = (text, styles = []) => console.log(formatLine(source, text, styles));

  return {
    info: (text) => write(text),
    ok: (text) => write(text, ['green']),
    warn: (text) => write(text, ['yellow']),
    error: (text) => write(text, ['red']),
    stream(text, { isStderr = false, stderrIsError = true } = {}) {
      if (ERROR_PATTERN.test(text) || (isStderr && stderrIsError)) return write(text, ['red']);
      if (WARN_PATTERN.test(text)) return write(text, ['yellow']);
      return write(text);
    },
  };
}
