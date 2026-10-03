// Leitura e validação do .env (sem dependências). Os segredos internos da API (JWT_SECRET e
// RECOVERY_HASH_SECRET) são gerados aqui quando estão vazios: assim ninguém fica com uma chave copiada
// de exemplo. O DB_PASSWORD continua com o usuário; o MAILER_API_SECRET, com scripts/setup-recovery.mjs
// (precisa ser igual em mailer/.env).
import { randomBytes } from 'node:crypto';
import { existsSync, readFileSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';

const MIN_JWT_SECRET_BYTES = 32;
const GENERATED_SECRETS = ['JWT_SECRET', 'RECOVERY_HASH_SECRET'];

const DEFAULTS = {
  DB_NAME: 'academic_organizer',
  DB_USERNAME: 'academic_organizer',
  DB_PORT: '5432',
};

export function parseEnvFile(text) {
  const values = {};
  for (const rawLine of text.replace(/^﻿/, '').split(/\r?\n/)) {
    const match = rawLine.match(/^\s*(?:export\s+)?([A-Za-z_][A-Za-z0-9_]*)\s*=(.*)$/);
    if (!match) continue;
    values[match[1]] = match[2].trim().replace(/^(['"])(.*)\1$/, '$2');
  }
  return values;
}

export function envFilePath(root) {
  return join(root, '.env');
}

/** Variáveis efetivas: o ambiente do shell tem prioridade (mesma regra do docker compose). */
function buildEnv(root, baseEnv = process.env) {
  const fromFile = parseEnvFile(readFileSync(envFilePath(root), 'utf8'));
  const env = { ...baseEnv };
  for (const [key, value] of Object.entries(fromFile)) {
    if (!env[key]) env[key] = value;
  }
  for (const [key, value] of Object.entries(DEFAULTS)) {
    if (!env[key]) env[key] = value;
  }
  if (!env.DB_URL) {
    env.DB_URL = `jdbc:postgresql://localhost:${env.DB_PORT}/${env.DB_NAME}`;
  }
  return env;
}

function validateEnv(env) {
  const problems = [];
  if (!env.DB_PASSWORD) problems.push('DB_PASSWORD está vazio.');
  if (!env.JWT_SECRET) {
    problems.push('JWT_SECRET está vazio.');
  } else if (Buffer.byteLength(env.JWT_SECRET) < MIN_JWT_SECRET_BYTES) {
    problems.push(`JWT_SECRET precisa ter pelo menos ${MIN_JWT_SECRET_BYTES} bytes.`);
  }
  return problems;
}

/**
 * Grava valores aleatórios no .env para os segredos de GENERATED_SECRETS que estão vazios (no shell e
 * no arquivo). Troca a linha `NOME=` existente ou acrescenta uma no fim. Retorna os nomes gerados.
 */
export function ensureSecrets(root, baseEnv = process.env) {
  const file = envFilePath(root);
  let text = readFileSync(file, 'utf8');
  const generated = [];

  for (const key of GENERATED_SECRETS) {
    if (baseEnv[key] || parseEnvFile(text)[key]) continue;
    const pattern = new RegExp(`^\\s*(?:export\\s+)?${key}\\s*=.*$`, 'm');
    const line = `${key}=${randomBytes(48).toString('base64url')}`;
    text = pattern.test(text) ? text.replace(pattern, line) : `${text.replace(/\s*$/, '')}\n${line}\n`;
    generated.push(key);
  }

  if (generated.length > 0) writeFileSync(file, text, 'utf8');
  return generated;
}

/** Retorna `{ env, problems, generatedSecrets }`; `problems` vazio significa que o .env está pronto. */
export function loadEnv(root, baseEnv = process.env) {
  if (!existsSync(envFilePath(root))) {
    return { env: { ...baseEnv }, problems: ['Arquivo .env não encontrado na raiz do projeto.'], generatedSecrets: [] };
  }
  const generatedSecrets = ensureSecrets(root, baseEnv);
  const env = buildEnv(root, baseEnv);
  return { env, problems: validateEnv(env), generatedSecrets };
}

export const ENV_HELP = [
  'Copie .env.example para .env e preencha DB_PASSWORD (JWT_SECRET e RECOVERY_HASH_SECRET vazios são gerados sozinhos):',
  '  Windows (PowerShell): Copy-Item .env.example .env',
  '  Linux/macOS/Git Bash: cp .env.example .env',
];
