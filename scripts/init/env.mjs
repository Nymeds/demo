// Leitura e validação do .env (sem dependências). O único segredo gerado aqui é o JWT_SECRET, quando
// está vazio: assim ninguém fica com uma chave copiada de exemplo. O DB_PASSWORD continua com o usuário.
import { randomBytes } from 'node:crypto';
import { existsSync, readFileSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';

const MIN_JWT_SECRET_BYTES = 32;

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
 * Grava um JWT_SECRET aleatório no .env quando ele está vazio (no shell e no arquivo).
 * Troca a linha `JWT_SECRET=` existente ou acrescenta uma no fim. Retorna true se gerou.
 */
export function ensureJwtSecret(root, baseEnv = process.env) {
  const file = envFilePath(root);
  const text = readFileSync(file, 'utf8');
  if (baseEnv.JWT_SECRET || parseEnvFile(text).JWT_SECRET) return false;

  const line = `JWT_SECRET=${randomBytes(48).toString('base64url')}`;
  const updated = /^\s*(?:export\s+)?JWT_SECRET\s*=.*$/m.test(text)
    ? text.replace(/^\s*(?:export\s+)?JWT_SECRET\s*=.*$/m, line)
    : `${text.replace(/\s*$/, '')}\n${line}\n`;
  writeFileSync(file, updated, 'utf8');
  return true;
}

/** Retorna `{ env, problems, generatedJwtSecret }`; `problems` vazio significa que o .env está pronto. */
export function loadEnv(root, baseEnv = process.env) {
  if (!existsSync(envFilePath(root))) {
    return { env: { ...baseEnv }, problems: ['Arquivo .env não encontrado na raiz do projeto.'], generatedJwtSecret: false };
  }
  const generatedJwtSecret = ensureJwtSecret(root, baseEnv);
  const env = buildEnv(root, baseEnv);
  return { env, problems: validateEnv(env), generatedJwtSecret };
}

export const ENV_HELP = [
  'Copie .env.example para .env e preencha DB_PASSWORD (o JWT_SECRET vazio é gerado sozinho):',
  '  Windows (PowerShell): Copy-Item .env.example .env',
  '  Linux/macOS/Git Bash: cp .env.example .env',
];
