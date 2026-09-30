// Passo 1: valida Docker, Java e Node/npm antes de mexer em qualquer coisa.
import { existsSync } from 'node:fs';
import { join } from 'node:path';
import { capture, IS_WINDOWS } from './proc.mjs';

const MIN_JAVA_MAJOR = 21;
const MIN_NODE = [20, 19]; // exigência do Vite 8

async function checkDocker() {
  const info = await capture('docker', ['info', '--format', '{{.ServerVersion}}'], { timeoutMs: 30_000 });
  if (info.error?.code === 'ENOENT') {
    return 'Docker não encontrado. Instale o Docker Desktop: https://www.docker.com/products/docker-desktop';
  }
  if (info.code !== 0) {
    return 'Docker não está em execução. Abra o Docker Desktop, aguarde iniciar e rode o init novamente.';
  }
  const compose = await capture('docker', ['compose', 'version']);
  return compose.code === 0 ? null : 'Docker Compose v2 não encontrado (comando "docker compose").';
}

function javaExecutable() {
  const home = process.env.JAVA_HOME;
  const candidate = home && join(home, 'bin', IS_WINDOWS ? 'java.exe' : 'java');
  return candidate && existsSync(candidate) ? candidate : 'java';
}

async function checkJava() {
  const result = await capture(javaExecutable(), ['-version']);
  if (result.error || result.code !== 0) {
    return `Java ${MIN_JAVA_MAJOR} não encontrado. Instale um JDK ${MIN_JAVA_MAJOR} e configure JAVA_HOME ou o PATH.`;
  }
  const major = Number((result.stderr + result.stdout).match(/version "(\d+)/)?.[1]);
  return major >= MIN_JAVA_MAJOR ? null : `Java ${major || '?'} encontrado, mas o projeto exige Java ${MIN_JAVA_MAJOR}.`;
}

function checkNode() {
  const [major, minor] = process.versions.node.split('.').map(Number);
  const ok = major > MIN_NODE[0] || (major === MIN_NODE[0] && minor >= MIN_NODE[1]);
  return ok ? null : `Node.js ${process.versions.node} é antigo demais; use ${MIN_NODE.join('.')} ou superior.`;
}

async function checkNpm() {
  const result = await capture(IS_WINDOWS ? 'npm.cmd' : 'npm', ['--version']);
  return result.code === 0 ? null : 'npm não encontrado. Instale o Node.js (que inclui o npm).';
}

/** Retorna a lista de problemas encontrados (vazia quando tudo está disponível). */
export async function checkPrerequisites() {
  const results = [await checkDocker(), await checkJava(), checkNode(), await checkNpm()];
  return results.filter(Boolean);
}
