#!/usr/bin/env node
// Pipeline "init": valida pré-requisitos, sobe o PostgreSQL, garante tabelas e dados de demonstração,
// e mantém backend e frontend rodando. Sem dependências além do Node. Veja o README (Início rápido).
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { DEMO_EMAIL, DEMO_PASSWORD, REQUIRED_TABLES, inspectDatabase, startContainer, waitForSeed, waitUntilHealthy } from './init/database.mjs';
import { ENV_HELP, loadEnv } from './init/env.mjs';
import { createLogger } from './init/log.mjs';
import { checkPrerequisites } from './init/prerequisites.mjs';
import { hasExited, hasRunningChildren, killAllSync, stopAll } from './init/proc.mjs';
import { ensureFrontendDependencies, resolveEndpoints, startBackend, startFrontend, waitForBackend, waitForFrontend } from './init/services.mjs';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const init = createLogger('INIT');
const db = createLogger('DB');
const back = createLogger('BACK');
const front = createLogger('FRONT');

let stopping = false;

async function shutdown(exitCode) {
  if (stopping) return;
  stopping = true;
  if (hasRunningChildren()) init.info('Encerrando backend e frontend (o PostgreSQL continua rodando)...');
  await stopAll();
  process.exit(exitCode);
}

function abort(problems) {
  problems.forEach((problem) => init.error(problem));
  return shutdown(1);
}

// Só é chamado depois que o serviço ficou pronto; até lá, quem detecta a queda é a própria espera.
function watchService(child, name, log) {
  const onExit = (code) => {
    if (stopping) return;
    log.error(`${name} encerrou inesperadamente (código ${code}).`);
    shutdown(1);
  };
  if (hasExited(child)) return onExit(child.exitCode);
  child.on('close', onExit);
}

function describeBefore(state) {
  if (state.tablesReady && state.demoUserExists) {
    db.ok(`Tabelas (${REQUIRED_TABLES.length}/${REQUIRED_TABLES.length}) e usuário demo já existem.`);
    init.warn('Tabelas e dados de demonstração já existem: nada será recriado. Seguindo para backend e frontend.');
    return;
  }
  if (state.tablesReady) {
    db.info('Tabelas já existem; o usuário demo ainda não. O backend vai inserir os dados de demonstração.');
    return;
  }
  const found = REQUIRED_TABLES.length - state.missing.length;
  db.info(`Tabelas encontradas: ${found}/${REQUIRED_TABLES.length}. O backend cria as que faltam ao iniciar (Hibernate, ddl-auto=update).`);
}

function describeAfter(before, after) {
  if (!after.tablesReady || !after.demoUserExists) {
    throw new Error('Verificação final do banco falhou: tabelas ou usuário demo ausentes após iniciar o backend.');
  }
  if (!before.tablesReady) db.ok(`Tabelas criadas pelo backend (${REQUIRED_TABLES.length}/${REQUIRED_TABLES.length}).`);
  if (!before.demoUserExists) db.ok(`Dados de demonstração inseridos (usuário ${DEMO_EMAIL}).`);
  if (before.tablesReady && before.demoUserExists) db.ok('Verificação final: tabelas e usuário demo continuam intactos.');
}

function printSummary({ frontendUrl, apiUrl }) {
  const line = '='.repeat(60);
  init.ok(line);
  init.ok('Tudo pronto!');
  init.info(`  Frontend:   ${frontendUrl}`);
  init.info(`  API:        ${apiUrl}`);
  init.info(`  Login demo: ${DEMO_EMAIL}  /  senha: ${DEMO_PASSWORD}`);
  init.info('  Para parar: Ctrl+C (backend e frontend; o PostgreSQL continua rodando).');
  init.ok(line);
}

async function run() {
  init.info('Pipeline de inicialização do AcadOrganize.');

  const { env, problems: envProblems, generatedJwtSecret } = loadEnv(root);
  const endpoints = resolveEndpoints(env);
  if (generatedJwtSecret) init.info('JWT_SECRET estava vazio: um segredo aleatório foi gravado no .env.');
  if (envProblems.length) return abort([...envProblems, ...ENV_HELP]);
  init.ok('.env encontrado com DB_PASSWORD e JWT_SECRET.');

  const prerequisiteProblems = await checkPrerequisites();
  if (prerequisiteProblems.length) return abort(prerequisiteProblems);
  init.ok('Pré-requisitos ok: Docker, Java, Node e npm.');

  await startContainer({ root, env, log: db });
  await waitUntilHealthy({ log: db });

  const before = await inspectDatabase(env);
  describeBefore(before);

  const backend = startBackend({ root, env, log: back });
  await ensureFrontendDependencies({ root, env, log: front });
  await waitForBackend(backend, init, endpoints);
  watchService(backend, 'O backend', back);

  const after = await waitForSeed(env, { isAlive: () => !hasExited(backend) });
  describeAfter(before, after);

  const frontend = startFrontend({ root, env, log: front, endpoints });
  await waitForFrontend(frontend, init, endpoints);
  watchService(frontend, 'O frontend', front);
  printSummary(endpoints);
}

for (const signal of ['SIGINT', 'SIGTERM', 'SIGHUP']) {
  process.on(signal, () => shutdown(0));
}
process.on('exit', killAllSync);

run().catch((error) => {
  if (!stopping) init.error(error.message);
  return shutdown(1);
});
