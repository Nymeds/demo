// Passo 6: backend (Spring Boot) e frontend (Vite), ambos mantidos em execução com saída em streaming.
import { existsSync } from 'node:fs';
import { join } from 'node:path';
import { hasExited, IS_WINDOWS, runStreamed, startStreamed } from './proc.mjs';

const DEFAULT_API_PORT = '8080';
const DEFAULT_FRONTEND_PORT = '5173';

const BACKEND_TIMEOUT_MS = 600_000; // a primeira execução baixa o Maven e as dependências
const FRONTEND_TIMEOUT_MS = 90_000;
const NOTICE_EVERY_MS = 15_000;

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));
const npmCommand = IS_WINDOWS ? 'npm.cmd' : 'npm';

/** URLs e portas. Padrão 8080/5173; SERVER_PORT e FRONTEND_PORT existem para contornar portas ocupadas. */
export function resolveEndpoints(env) {
  const apiPort = env.SERVER_PORT || DEFAULT_API_PORT;
  const frontendPort = env.FRONTEND_PORT || DEFAULT_FRONTEND_PORT;
  return {
    apiPort,
    frontendPort,
    apiUrl: `http://localhost:${apiPort}`,
    frontendUrl: `http://localhost:${frontendPort}`,
  };
}

/** Inicia o backend com o perfil `seed` (cria o usuário demo no PostgreSQL se ele ainda não existir). */
export function startBackend({ root, env, log }) {
  // O mvnw fica em backend/; o caminho relativo é resolvido a partir desse cwd. O .env continua na raiz
  // e chega ao processo pelas variáveis de `env`.
  const backendDir = join(root, 'backend');
  const mvn = IS_WINDOWS ? '.\\mvnw.cmd' : './mvnw';
  log.info('Iniciando o backend (mvnw spring-boot:run em backend/, perfil "seed")...');
  return startStreamed(
    mvn,
    ['spring-boot:run', '-Dspring-boot.run.profiles=seed', '-Dspring-boot.run.jvmArguments=-Dstdout.encoding=UTF-8'],
    // O init é só local: confia no X-Forwarded-For do proxy do Vite para cada aparelho ter o próprio IP
    // nos limites de login (FORWARD_HEADERS_STRATEGY no .env ou no shell tem prioridade).
    { cwd: backendDir, env: { FORWARD_HEADERS_STRATEGY: 'framework', ...env }, log },
  );
}

export async function ensureFrontendDependencies({ root, env, log }) {
  const frontendDir = join(root, 'frontend');
  if (existsSync(join(frontendDir, 'node_modules'))) return;
  log.info('node_modules não encontrado; rodando npm install em frontend/ ...');
  const code = await runStreamed(npmCommand, ['install'], { cwd: frontendDir, env, log, stderrIsError: false });
  if (code !== 0) throw new Error(`"npm install" falhou (código ${code}).`);
}

export function startFrontend({ root, env, log, endpoints }) {
  log.info('Iniciando o frontend (npm run dev)...');
  // Vite lê VITE_API_PROXY_TARGET do ambiente; só o definimos se a API não estiver na porta padrão.
  const frontendEnv = { ...env };
  if (!frontendEnv.VITE_API_PROXY_TARGET && endpoints.apiPort !== DEFAULT_API_PORT) {
    frontendEnv.VITE_API_PROXY_TARGET = endpoints.apiUrl;
  }
  return startStreamed(npmCommand, ['run', 'dev', '--', '--strictPort', '--port', endpoints.frontendPort], {
    cwd: join(root, 'frontend'),
    env: frontendEnv,
    log,
  });
}

async function respondsOk(url) {
  try {
    const response = await fetch(url, { signal: AbortSignal.timeout(3_000) });
    return response.ok;
  } catch {
    return false;
  }
}

async function waitForUrl({ url, child, label, timeoutMs, log, hint }) {
  const startedAt = Date.now();
  let lastNotice = startedAt;
  while (Date.now() - startedAt < timeoutMs) {
    if (hasExited(child)) throw new Error(`${label} encerrou antes de ficar pronto (código ${child.exitCode}). ${hint}`);
    if (await respondsOk(url)) return;
    if (Date.now() - lastNotice >= NOTICE_EVERY_MS) {
      log.info(`Aguardando ${label}... (${Math.round((Date.now() - startedAt) / 1000)}s)`);
      lastNotice = Date.now();
    }
    await sleep(2_000);
  }
  throw new Error(`Tempo esgotado esperando ${label} em ${url}.`);
}

export function waitForBackend(child, log, endpoints) {
  return waitForUrl({
    url: `${endpoints.apiUrl}/api/v1/legal/versions`, // GET público, não exige login
    child,
    label: 'o backend',
    timeoutMs: BACKEND_TIMEOUT_MS,
    log,
    hint: 'Confira DB_PASSWORD (um volume antigo mantém a senha anterior: "docker compose down -v" recria o banco) e se a porta da API está livre.',
  });
}

export function waitForFrontend(child, log, endpoints) {
  return waitForUrl({
    url: endpoints.frontendUrl,
    child,
    label: 'o frontend',
    timeoutMs: FRONTEND_TIMEOUT_MS,
    log,
    hint: 'Confira se a porta do frontend está livre.',
  });
}
