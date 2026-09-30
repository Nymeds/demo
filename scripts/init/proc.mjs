// Execução de processos: comandos curtos (capturados) e processos longos (saída em streaming).
import { spawn, spawnSync } from 'node:child_process';

export const IS_WINDOWS = process.platform === 'win32';

const children = new Set();

// No Windows, .cmd/.bat (mvnw.cmd, npm.cmd) só rodam via shell. A linha é montada em uma única
// string (com aspas quando preciso) para evitar o aviso de argumentos + shell do Node.
function buildSpawnArgs(command, args, options) {
  const useShell = IS_WINDOWS && /^(npm|npx)$|\.(cmd|bat)$/i.test(command);
  if (!useShell) return [command, args, options];
  const quote = (arg) => (/\s/.test(arg) ? `"${arg}"` : arg);
  return [[command, ...args].map(quote).join(' '), [], { ...options, shell: true }];
}

function spawnProcess(command, args, options = {}) {
  const [cmd, cmdArgs, cmdOptions] = buildSpawnArgs(command, args, {
    stdio: ['ignore', 'pipe', 'pipe'],
    windowsHide: true,
    detached: !IS_WINDOWS, // POSIX: grupo próprio, para encerrar a árvore inteira
    ...options,
  });
  return spawn(cmd, cmdArgs, cmdOptions);
}

/** Executa um comando curto e devolve `{ code, stdout, stderr, error }` sem lançar exceção. */
export function capture(command, args, { cwd, env, timeoutMs = 30_000 } = {}) {
  return new Promise((resolve) => {
    let stdout = '';
    let stderr = '';
    const child = spawnProcess(command, args, { cwd, env, detached: false });
    const timer = setTimeout(() => child.kill(), timeoutMs);
    child.stdout.on('data', (chunk) => (stdout += chunk));
    child.stderr.on('data', (chunk) => (stderr += chunk));
    child.on('error', (error) => {
      clearTimeout(timer);
      resolve({ code: null, stdout, stderr, error });
    });
    child.on('close', (code) => {
      clearTimeout(timer);
      resolve({ code, stdout, stderr, error: null });
    });
  });
}

function forwardLines(stream, isStderr, log, streamOptions) {
  let pending = '';
  const emit = (line) => line.trim() && log.stream(line.replace(/\s+$/, ''), { isStderr, ...streamOptions });
  stream.setEncoding('utf8');
  stream.on('data', (chunk) => {
    const lines = (pending + chunk).split(/\r?\n/);
    pending = lines.pop();
    lines.forEach(emit);
  });
  stream.on('end', () => emit(pending));
}

/** Inicia um processo longo, repassando cada linha ao logger. Registra o processo para o encerramento. */
export function startStreamed(command, args, { cwd, env, log, stderrIsError = true }) {
  const child = spawnProcess(command, args, { cwd, env });
  children.add(child);
  child.on('close', () => children.delete(child));
  child.on('error', (error) => log.error(`Não foi possível iniciar "${command}": ${error.message}`));
  forwardLines(child.stdout, false, log, { stderrIsError });
  forwardLines(child.stderr, true, log, { stderrIsError });
  return child;
}

/** Executa um comando com saída em streaming e resolve com o código de saída. */
export function runStreamed(command, args, options) {
  return new Promise((resolve, reject) => {
    const child = startStreamed(command, args, options);
    child.on('error', reject);
    child.on('close', (code) => resolve(code));
  });
}

export function hasRunningChildren() {
  return [...children].some((child) => !hasExited(child));
}

export function hasExited(child) {
  return child.exitCode !== null || child.signalCode !== null;
}

function killTree(child, signal) {
  if (hasExited(child) || !child.pid) return;
  try {
    if (IS_WINDOWS) {
      spawnSync('taskkill', ['/pid', String(child.pid), '/T', '/F'], { stdio: 'ignore', windowsHide: true });
    } else {
      process.kill(-child.pid, signal);
    }
  } catch {
    // o processo já terminou entre a checagem e o kill
  }
}

/** Encerra todos os processos iniciados por `startStreamed`. No Windows o kill é imediato e forçado. */
export async function stopAll({ graceMs = 8_000 } = {}) {
  const running = [...children];
  running.forEach((child) => killTree(child, 'SIGTERM'));
  const deadline = Date.now() + graceMs;
  while (running.some((child) => !hasExited(child)) && Date.now() < deadline) {
    await new Promise((resolve) => setTimeout(resolve, 200));
  }
  running.forEach((child) => killTree(child, 'SIGKILL'));
}

/** Rede de segurança síncrona para quando o Node sai sem passar pelo encerramento normal. */
export function killAllSync() {
  children.forEach((child) => killTree(child, 'SIGKILL'));
}
