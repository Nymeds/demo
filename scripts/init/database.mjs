// Passos 2, 3 e 5: sobe o PostgreSQL (Docker), espera ficar saudável e inspeciona tabelas e seed via psql.
import { capture, runStreamed } from './proc.mjs';

export const CONTAINER = 'studdy-postgres';
export const DEMO_EMAIL = 'desenvolvedor@dev.com';
export const DEMO_PASSWORD = 'desenvolvedor@dev.com';

// Tabelas criadas pelo Hibernate (spring.jpa.hibernate.ddl-auto=update) ao iniciar o backend.
export const REQUIRED_TABLES = [
  'app_users',
  'dashboards',
  'disciplines',
  'activities',
  'grades',
  'frequency',
  'absence_records',
  'calendar_events',
  'browser_sessions',
  'password_recoveries',
  'user_preferences',
  'user_profile_photos',
];

const HEALTH_TIMEOUT_MS = 120_000;
const HEALTH_INTERVAL_MS = 2_000;
const HEALTH_NOTICE_EVERY_MS = 10_000;

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

export async function startContainer({ root, env, log }) {
  log.info('Subindo o PostgreSQL (docker compose up -d postgres)...');
  const code = await runStreamed('docker', ['compose', 'up', '-d', 'postgres'], {
    cwd: root,
    env,
    log,
    stderrIsError: false, // o docker compose escreve o progresso normal em stderr
  });
  if (code !== 0) throw new Error(`"docker compose up" falhou (código ${code}).`);
}

async function healthStatus() {
  const result = await capture('docker', ['inspect', '-f', '{{.State.Health.Status}}', CONTAINER]);
  return result.code === 0 ? result.stdout.trim() : 'missing';
}

export async function waitUntilHealthy({ log }) {
  const startedAt = Date.now();
  let lastNotice = 0;
  while (Date.now() - startedAt < HEALTH_TIMEOUT_MS) {
    const status = await healthStatus();
    if (status === 'healthy') {
      log.ok('PostgreSQL saudável.');
      return;
    }
    if (status === 'unhealthy') {
      throw new Error(`O contêiner ${CONTAINER} ficou "unhealthy". Veja: docker logs ${CONTAINER}`);
    }
    if (Date.now() - lastNotice >= HEALTH_NOTICE_EVERY_MS) {
      log.info(`Aguardando o healthcheck do banco (status: ${status})...`);
      lastNotice = Date.now();
    }
    await sleep(HEALTH_INTERVAL_MS);
  }
  throw new Error(`Tempo esgotado (${HEALTH_TIMEOUT_MS / 1000}s) esperando ${CONTAINER} ficar saudável. Veja: docker logs ${CONTAINER}`);
}

async function psql(env, sql) {
  const args = ['exec', CONTAINER, 'psql', '-U', env.DB_USERNAME, '-d', env.DB_NAME, '-tA', '-v', 'ON_ERROR_STOP=1', '-c', sql];
  const result = await capture('docker', args);
  if (result.code !== 0) {
    throw new Error(`Consulta ao banco falhou: ${(result.stderr || result.error?.message || '').trim()}`);
  }
  return result.stdout.trim();
}

/** Consulta information_schema e o usuário demo. Só lê; nunca altera o banco. */
export async function inspectDatabase(env) {
  const names = REQUIRED_TABLES.map((table) => `'${table}'`).join(',');
  const found = await psql(
    env,
    `SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' AND table_name IN (${names})`,
  );
  const present = found ? found.split(/\r?\n/) : [];
  const missing = REQUIRED_TABLES.filter((table) => !present.includes(table));
  const demoUserExists = present.includes('app_users')
    && (await psql(env, `SELECT count(*) FROM app_users WHERE email = '${DEMO_EMAIL}'`)) !== '0';
  return { present: present.length, missing, tablesReady: missing.length === 0, demoUserExists };
}

/** Espera o seed terminar: ele roda como ApplicationRunner, depois que a API já responde. */
export async function waitForSeed(env, { timeoutMs = 60_000, isAlive = () => true } = {}) {
  const deadline = Date.now() + timeoutMs;
  let state = await inspectDatabase(env);
  while (!state.demoUserExists && isAlive() && Date.now() < deadline) {
    await sleep(2_000);
    state = await inspectDatabase(env);
  }
  return state;
}
