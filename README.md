# AcadOrganize — Sistema de Organização Acadêmica

## Visão geral

API Spring Boot e aplicação Vue 3 para estudantes organizarem a vida acadêmica: dashboards (semestres), disciplinas, atividades e provas, notas, frequência, calendário, simulador de notas, preferências e perfil.

O acesso é isolado por usuário: toda requisição parte do dashboard do usuário autenticado, e recursos de outra pessoa respondem `404` (e não `403`) para não revelar que existem.

O projeto segue um processo centrado no usuário (IHC): entender necessidades, definir requisitos, projetar fluxos acessíveis e validar com usuários. Um endpoint pronto não significa que o fluxo de interface foi validado.

## Início rápido (init)

Um único comando sobe o banco, cria as tabelas, insere dados de demonstração e inicia backend e frontend.

**Pré-requisitos** (todos precisam estar instalados antes do init):

| Ferramenta | Versão | Observação |
| --- | --- | --- |
| Java | 21 | O Maven não precisa ser instalado: há o Maven Wrapper |
| Node.js (com npm) | 20.19 ou superior | Usado pelo init e pelo frontend |
| Docker com Compose v2 | comando `docker compose` | Docker Desktop aberto e em execução (no Windows) |

O Docker é usado **só para o banco**: o `npm run init` sobe o PostgreSQL no Docker e roda a API e o frontend diretamente na sua máquina.

1. **Crie o `.env`** copiando `.env.example` (`Copy-Item .env.example .env` no PowerShell, `cp .env.example .env` no Git Bash/Linux/macOS) e **preencha `DB_PASSWORD`**. O `.env.example` vem com `DB_PASSWORD`, `JWT_SECRET` e `RECOVERY_HASH_SECRET` vazios de propósito. Se `JWT_SECRET` ou `RECOVERY_HASH_SECRET` estiverem vazios, o init gera valores aleatórios e grava no `.env`; se você preencher, ele precisa ter pelo menos 32 bytes (o `.env.example` traz comandos para gerar um). Se o `.env` não existir, se o `DB_PASSWORD` estiver vazio ou se o secret tiver menos de 32 bytes, o init para e avisa. O backend também recusa segredos que já foram publicados no repositório.
2. **Rode o init:** no VS Code, `Terminal > Run Task... > init` (ou `npm run init` na raiz do projeto).

**O que acontece:** valida o `.env`, Docker, Java e Node; sobe o PostgreSQL (`docker compose up -d postgres`) e espera o healthcheck; consulta as tabelas no banco; inicia o backend com o perfil `seed` (o Hibernate cria as tabelas e o usuário demo é inserido); confere o resultado no banco; roda `npm install` em `frontend/` só se faltar `node_modules` e inicia o frontend. No fim, mostra um resumo com as URLs. O serviço de e-mail da recuperação de senha (`mailer/`) é opcional: com `MAILER_AUTO_START=true` (padrão do `.env.example`), o próprio backend o inicia se ele estiver configurado; se não estiver, o backend registra o motivo no log e o resto do sistema funciona normalmente (veja [Recuperação de senha](#recuperação-de-senha-e-mail)).

**Cores do terminal:** cada linha começa com a origem. `[INIT]` (branco) é o pipeline, `[DB]` (ciano) é o banco, `[BACK]` (verde) é o backend e `[FRONT]` (magenta) é o frontend. Linhas de erro (saída de erro ou com ERROR/Exception/failed) ficam em vermelho, mantendo o prefixo de quem as gerou; avisos (WARN) ficam em amarelo.

**Segunda execução:** se as tabelas e o usuário demo já existem, o init avisa "Tabelas e dados de demonstração já existem: nada será recriado" e segue direto para backend e frontend. Nada é apagado ou duplicado.

**Acesso:** frontend em `http://localhost:5173` e API em `http://localhost:8080`. Login demo: `desenvolvedor@dev.com`, senha `desenvolvedor@dev.com`. Esse usuário tem senha conhecida: use o init só para avaliação e desenvolvimento local.

**Para parar:** `Ctrl+C` no terminal da tarefa encerra backend e frontend. O PostgreSQL continua rodando; para pará-lo, `docker compose down`.

**Para zerar o banco** (apaga todos os dados): `docker compose down -v` e rode o init de novo.

**Se algo falhar:** porta ocupada (8080 ou 5173)? Defina `SERVER_PORT` e/ou `FRONTEND_PORT` no `.env` ou no shell (veja a tabela de variáveis em "Como rodar"). Erro de autenticação no banco depois de trocar `DB_PASSWORD`? O volume antigo guarda a senha anterior; recrie com `docker compose down -v`.

O perfil `seed` só existe para o init (e para quem o ativar de propósito): o perfil padrão nunca insere dados de demonstração.

## Tecnologias

- Java 21, Spring Boot 4.1, Spring Web MVC, Spring Data JPA, Bean Validation, Spring Security com JWT, Lombok
- PostgreSQL (padrão) e H2 em memória (perfil `dev` e testes)
- Maven Wrapper
- Vue 3 e Vite (frontend em `frontend/`)

## Estrutura

O backend e o frontend ficam em pastas separadas, e cada um pode ser aberto e analisado sozinho: o backend na IDE Java pelo `backend/pom.xml` e o frontend pelo `frontend/package.json`. Cada pasta tem o próprio README ([backend](backend/README.md), [frontend](frontend/README.md)).

```text
backend/                               API Spring Boot (Java 21)
  pom.xml                              Dependências e build do backend
  mvnw, mvnw.cmd, .mvn/                Maven Wrapper (não precisa instalar o Maven)
  src/main/java/studdy/example/demo/   Código-fonte, um pacote por funcionalidade:
    activities/                        Atividades e provas (tipo ACTIVITY ou EXAM)
    auth/                              Cadastro, login, limitadores de tentativas
      session/                         Sessão do navegador por cookie HttpOnly ("Lembrar de mim")
      recovery/                        Recuperação de senha por código enviado por e-mail
    calendar/                          Eventos do calendário
    config/                            Relógio, validação, seed de demonstração (perfis dev e seed)
    dashboard/                         Dashboards e correção de dashboards ativos na inicialização
    discipline/                        Disciplinas, frequência, faltas, desempenho acadêmico
    grade/                             Notas
    gradebook/                         Visão consolidada da tela Notas
    legal/                             Versões vigentes dos Termos e da Política de Privacidade
    security/                          Configuração de segurança e JWT
    settings/                          Preferências, senha, exclusão de conta
    simulator/                         Simulador de notas (cálculo no servidor)
    user/                              Usuário, perfil e foto de perfil
  src/main/resources/                  application.properties e application-dev.properties
  src/test/java/studdy/example/demo/   Testes automatizados (espelham os pacotes acima)
  src/test/resources/                  Configuração dos testes (H2)
frontend/                              Interface Vue 3 + Vite
  package.json                         Dependências e scripts do frontend
  src/features/<feature>/              Uma pasta por funcionalidade: tela + modais + CSS da feature
  src/components/ui/                   Componentes compartilhados (AppSelect, AppDatePicker...)
  src/components/misc/                 Componentes avulsos
  src/api/                             apiClient.js (adaptador sobre o cliente HTTP único, src/shared/http/apiRequest.js) e protectedFetch.js (aviso de acesso negado)
  src/composables/                     Composables compartilhados (useTheme.js)
  src/shared/                          Código compartilhado (cliente HTTP, sessão, datas, formatação...)
  src/styles/                          CSS global
  src/assets/images/                   Imagens usadas pelas telas
  public/                              Arquivos estáticos usados pela interface (inclui public/buddy/)
  tests/                               Testes de lógica do frontend (node:test)
  src/**/*.test.js                     Testes de componentes (Vitest + Vue Test Utils)
mailer/                                Serviço Node privado (Nodemailer) que envia os códigos de recuperação de senha
scripts/                               Pipeline "init" (init.mjs e init/, Node sem dependências) e setup-recovery.mjs
docs/                                  Regras de negócio, API de perfil e migrações SQL
README.md                              Este arquivo: visão geral, como rodar, domínio e API
AGENTS.md                              Orientações para agentes de IA
compose.yaml                           PostgreSQL 16 para desenvolvimento (Docker)
.env.example                           Modelo do .env, que fica na raiz (usado pelo init e pelo Docker Compose)
package.json                           Só o script "init" (npm run init)
.vscode/tasks.json                     Tarefa "init" do VS Code
.gitignore, .gitattributes             Regras do Git (arquivos ignorados e fins de linha)
```

## Como rodar

### Pré-requisitos

Veja a tabela de pré-requisitos do [Início rápido](#início-rápido-init). Docker só é necessário para o PostgreSQL; para o perfil `dev` bastam Java 21 e Node.js. Os comandos do backend rodam dentro da pasta `backend/` (`cd backend` a partir da raiz). No PowerShell, use `./mvnw.cmd` ou `.\mvnw.cmd`; no Linux/macOS, `./mvnw`.

### Backend, perfil padrão (PostgreSQL)

O perfil padrão usa PostgreSQL e exige configuração por variáveis de ambiente:

| Variável | Obrigatória | Padrão | Descrição |
| --- | --- | --- | --- |
| `DB_URL` | não | `jdbc:postgresql://localhost:5432/academic_organizer` | URL JDBC |
| `DB_USERNAME` | não | `academic_organizer` | Usuário do banco |
| `DB_PASSWORD` | sim | vazio | Sem ela a conexão falha |
| `JWT_SECRET` | sim | vazio | Mínimo de 32 bytes |
| `JWT_EXPIRATION_MS` | não | `900000` (15 min) | Validade do token de acesso |
| `AUTH_COOKIE_SECURE` | não (sim em produção) | `false` | `true` em produção HTTPS (inclusive com TLS terminando no proxy): o cookie da sessão só trafega por HTTPS |
| `RECOVERY_HASH_SECRET` | não | o `JWT_SECRET` | Chave (mínimo 32 bytes) do HMAC dos códigos de recuperação. Use uma chave própria; o init gera uma se estiver vazia |
| `MAILER_API_SECRET` | para enviar e-mails | vazio | Chave interna (mínimo 32 bytes) entre a API e o `mailer/`; a mesma de `mailer/.env`. `node scripts/setup-recovery.mjs` gera e sincroniza |
| `MAILER_URL` | não | `http://127.0.0.1:3001` | Endereço do `mailer/`; fora de localhost exige HTTPS |
| `MAILER_AUTO_START` | não | `false` (`true` no `.env.example`) | A API inicia o `mailer/` ao subir e o encerra ao parar |
| `MAILER_DIRECTORY` | não | `../mailer` | Pasta do `mailer/`, relativa a `backend/` (de onde a API roda) |
| `NODE_EXECUTABLE` | não | `node` | Caminho do Node.js usado para iniciar o `mailer/` |
| `FORWARD_HEADERS_STRATEGY` | não | `none` | Use `native` (ou `framework`) só atrás de um proxy confiável, para o limitador de login usar o `X-Forwarded-For`; nunca sem proxy (o cabeçalho seria forjável) |
| `SERVER_PORT` | não | `8080` | Porta da API no `npm run init` (contorna porta ocupada) |
| `FRONTEND_PORT` | não | `5173` | Porta do frontend no `npm run init` |
| `VITE_API_PROXY_TARGET` | não | `http://localhost:8080` | Alvo do proxy `/api` do Vite; o init ajusta sozinho se `SERVER_PORT` mudar |
| `LEGAL_TERMS_VERSION` | não | `2026-09-29` | Versão vigente dos Termos de Uso |
| `LEGAL_PRIVACY_VERSION` | não | `2026-09-29` | Versão vigente da Política de Privacidade |

#### Usando PostgreSQL com Docker Compose

O `compose.yaml` sobe um PostgreSQL 16 para desenvolvimento, alinhado aos padrões da aplicação (`academic_organizer` / `academic_organizer`). O contêiner usa as mesmas variáveis da aplicação: `DB_USERNAME`, `DB_PASSWORD`, `DB_NAME` (padrão `academic_organizer`) e `DB_PORT` (padrão `5432`, mapeada localmente). Sem `DB_PASSWORD` o `docker compose` falha. Se mudar `DB_NAME` ou `DB_PORT`, ajuste `DB_URL` também.

O antigo perfil `postgres` foi removido: use o perfil padrão com as variáveis `DB_*`.

1. **Copie `.env.example` para `.env`** (ignorado pelo Git):
   ```bash
   cp .env.example .env
   ```

2. **Edite `.env`** e configure as senhas:
   ```env
   DB_PASSWORD=sua-senha-escolhida
   JWT_SECRET=uma-chave-com-pelo-menos-32-bytes-................................
   ```

3. **Suba o PostgreSQL**:
   ```bash
   docker compose up -d postgres
   ```

4. **Rode a aplicação** com as variáveis de ambiente definidas:
   ```powershell
   # PowerShell
   $env:DB_PASSWORD = "sua-senha-escolhida"
   $env:JWT_SECRET = "uma-chave-com-pelo-menos-32-bytes-................................"
   cd backend
   ./mvnw.cmd spring-boot:run
   ```
   ou via `.env` (o `.env` fica na raiz; rode a partir dela):
   ```bash
   set -a && source .env && set +a  # Linux/macOS
   cd backend
   ./mvnw spring-boot:run
   ```

5. **Pare o container** quando terminar (na raiz do projeto, onde está o `compose.yaml`):
   ```bash
   docker compose down
   ```

**Aviso: volume existente com credenciais antigas.** Se você tinha um volume Docker criado com o `compose.yaml` antigo (usuário/senha `studdy`/`studdy_dev`), ele ainda possui essas credenciais. Para usar a nova configuração (`academic_organizer`), escolha um de:
- **Recrie o volume** (APAGA dados): `docker compose down -v && docker compose up -d postgres`
- **Crie o novo usuário/banco manualmente** no container existente:
  ```sql
  -- Dentro de psql conectado como postgres
  CREATE ROLE academic_organizer WITH LOGIN PASSWORD 'sua-senha-escolhida';
  CREATE DATABASE academic_organizer OWNER academic_organizer;
  ```

Nunca exponha a porta `5432` publicamente.

### Backend, perfil `dev` (H2 em memória)

Não exige PostgreSQL. O banco é H2 em memória: **os dados são perdidos a cada reinicialização**. O perfil cria o usuário demo `desenvolvedor@dev.com` (senha idêntica ao e-mail) com dashboard, disciplinas, atividades, notas e faltas de exemplo. Use somente em desenvolvimento local, nunca em produção.

```powershell
# PowerShell
cd backend
./mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

```bash
# Linux/macOS
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Nesse perfil `JWT_SECRET` é opcional (há um segredo exclusivo de desenvolvimento; se definido, tem prioridade). A API fica em `http://localhost:8080`.

### Backend, perfil `seed` (PostgreSQL com dados de demonstração)

É o perfil usado pelo `init`. Combine-o com as variáveis do perfil padrão (`DB_PASSWORD`, `JWT_SECRET`, ...) e rode em `backend/`: `./mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=seed"` (no Linux/macOS, `./mvnw spring-boot:run -Dspring-boot.run.profiles=seed`). Ele reaproveita o mesmo seed do perfil `dev` (`DemoDataSeed`), mas grava no PostgreSQL configurado, e é idempotente: se `desenvolvedor@dev.com` já existe, nada é criado. Nunca ative em produção.

### Recuperação de senha (e-mail)

O link **Esqueci minha senha** do login abre o fluxo em três etapas: e-mail, código de 6 dígitos e nova senha. A API Spring controla a recuperação (`auth/recovery/`) e um serviço Node privado em `mailer/` envia os e-mails com Nodemailer pelo Gmail. As credenciais SMTP ficam só nesse serviço, sem variáveis `VITE_` nem código no navegador.

**Configurar (uma vez, na raiz do projeto):**

```powershell
node scripts/setup-recovery.mjs   # cria/atualiza .env e mailer/.env e sincroniza MAILER_API_SECRET
cd mailer
npm ci
```

Depois edite **somente no seu computador** o `mailer/.env` (ignorado pelo Git):

```dotenv
SMTP_HOST=smtp.gmail.com
SMTP_PORT=auto
SMTP_USER=seuemail@gmail.com
SMTP_PASSWORD=sua-senha-de-app
MAIL_FROM_NAME=AcadOrganize
```

Use uma **senha de app** do Google (exige verificação em duas etapas); não use a senha normal da conta. Veja [a orientação do Google](https://support.google.com/accounts/answer/185833?hl=pt-BR) e [a configuração do Gmail no Nodemailer](https://nodemailer.com/guides/using-gmail). Confira a conexão sem enviar e-mail com `npm run verify` em `mailer/`.

**Rodar:** com `MAILER_AUTO_START=true` no `.env`, o `npm run init` (ou `./mvnw.cmd spring-boot:run` em `backend/`, com as variáveis do `.env` no ambiente) inicia o mailer junto com a API e o encerra ao parar. Reinicie a API depois de mudar a configuração. Para rodar o mailer separado (ou em produção com supervisor de processos), use `MAILER_AUTO_START=false` e `npm start` em `mailer/`. Se o Node ou o SMTP falharem, a API continua atendendo e registra a causa no log.

Na inicialização, o mailer testa a porta **465 (TLS)** e, se falhar, a **587 (STARTTLS)**, com limite de 15 segundos cada; o log informa a porta escolhida. Para fixar uma, use `SMTP_PORT=465` ou `587`. Autenticação recusada interrompe as tentativas (trocar a porta não corrige a senha). O endereço HTTP do mailer fica em `127.0.0.1:3001` (`MAILER_URL`); 465/587 são as portas do servidor SMTP do Gmail.

**Testar localmente:** cadastre uma conta com um e-mail que você controla, saia e use **Esqueci minha senha**. Receba o código, valide, defina a nova senha e entre com ela. Confira também código incorreto, reenvio (liberado após 60 s), expiração e o retorno ao login, no celular e só com teclado. Sem o mailer configurado, a tela responde normalmente (a resposta é a mesma para qualquer e-mail), mas nenhum e-mail chega e o log da API mostra a falha de envio. No perfil `dev` (H2 em memória), contas e códigos somem ao reiniciar.

**Rotas públicas:** `POST /api/v1/auth/password-recovery/request` (e-mail; `202`), `/verify` (e-mail e `code`; devolve `resetToken` e `expiresIn`) e `/reset` (e-mail, `resetToken` e `newPassword`). A recuperação não faz login automático e encerra as sessões abertas da conta.

**Proteções:**

- Resposta idêntica para conta existente, inexistente e envio bloqueado; a busca da conta e o SMTP rodam em filas limitadas, fora da resposta HTTP.
- Código de seis dígitos (`SecureRandom`), válido por 10 minutos, com 5 tentativas. Só o HMAC (com `RECOVERY_HASH_SECRET` e nonce) é gravado; código e autorização não vão para os logs.
- No máximo um envio por minuto e cinco por hora por conta (contadores no banco). Um reenvio substitui o código e a autorização anteriores.
- O código válido é trocado por uma autorização aleatória de 256 bits, válida por 5 minutos e de uso único, mantida só na memória da tela.
- Transações com bloqueio da conta serializam verificações e trocas simultâneas; tentativas erradas ficam gravadas mesmo quando a API responde erro.
- Nova senha com o BCrypt existente e limite de 72 bytes UTF-8; tokens e sessões anteriores deixam de valer. Trocar e-mail ou senha também invalida uma recuperação pendente. Os dados de recuperação são apagados junto com a conta.
- Limite extra de 30 requisições por hora por IP (em memória, por instância), com `429`.
- Mailer autenticado por chave interna, corpo limitado, destinatário único, TLS obrigatório e certificado validado.

Em produção: PostgreSQL, HTTPS no site (`AUTH_COOKIE_SECURE=true`) e o mailer acessível só pela API. Com proxy ou várias instâncias, configure `FORWARD_HEADERS_STRATEGY` e um limite compartilhado no gateway: o limite por IP em memória reinicia com a API. As filas de envio são locais: se o processo cair ou o SMTP falhar, a pessoa solicita reenvio depois do intervalo.

### Frontend

```bash
cd frontend
npm ci
npm run dev
```

Abra `http://localhost:5173`. O Vite encaminha as requisições de `/api` para o alvo definido em `VITE_API_PROXY_TARGET` (padrão `http://localhost:8080`). Para outro backend, copie `frontend/.env.example` para `frontend/.env.local` (ignorado pelo Git) e ajuste a variável; reinicie o `npm run dev`. Não coloque segredos nem URLs de ambiente no `vite.config.js`.

### Testes e build

```powershell
cd backend
./mvnw.cmd test        # backend: H2 em memória, configuração em backend/src/test/resources/application.properties
                       # (no Linux/macOS: ./mvnw test)
cd ../frontend
npm run build          # build de produção do frontend
npm test               # Vitest (componentes, src/**/*.test.js) e node:test (lógica, tests/*.test.mjs)
cd ../mailer
npm test               # serviço de e-mail (node:test); nenhum teste envia e-mail real
```

## Domínio e regras

- **Dashboards**: cada usuário tem no máximo um dashboard `ACTIVE`. Ao ativar um, os demais são desativados. Na inicialização, uma correção idempotente mantém, para quem tiver mais de um ativo, o que tem mais disciplinas (empate: maior id) e desativa os outros, sem apagar dados.
- **Disciplinas**: semestre/período editáveis; professor é opcional (`null`). Cada disciplina guarda a média de aprovação (`passingAverage`, 0 a 10) e a frequência mínima (`minimumAttendancePercentage`, 0 a 100). A situação (`IN_PROGRESS`, `COMPLETED`, `LOCKED`) é escolhida pelo estudante e não é calculada (veja `docs/discipline-status.md`).
- **Atividades**: têm tipo `ACTIVITY` ou `EXAM`. Provas são atividades `EXAM`, exibidas na tela Provas e no Calendário, e podem receber nota na tela Notas.
- **Notas**: podem se vincular a uma atividade/prova da mesma disciplina, que precisa já ter acontecido, e cada uma aceita uma única nota (`409` se repetir). A data da nota não pode ser anterior à data da atividade. Não é possível mover o prazo (`dueDate`) de uma atividade para depois da data da nota vinculada (`409`); altere a data da nota primeiro.
- **Arredondamento**: a média da disciplina é simples, com 2 casas (`HALF_UP`). A faixa de desempenho da tela Notas (`EXCELLENT` a partir de 9,0, `GOOD` 7,0, `REGULAR` 5,0, `INSUFFICIENT`, `NO_GRADES`) usa a média arredondada para 1 casa, `HALF_UP`, igual ao exibido na interface.
- **Frequência**: começa em 100% e cada falta registrada reduz 5 pontos percentuais, com mínimo de 0% (5 faltas = 75%). O sistema não pede nem estima o total de aulas. O teto de faltas é `floor((100 - frequência mínima) / 5)`. Faltas são registradas com data (não pode ser futura), quantidade, motivo e observação; o histórico persiste. O total de faltas também pode ser editado manualmente (`PUT .../frequency`); essa edição e o histórico usam lock pessimista na linha de frequência para não se sobrescreverem. Veja `docs/frequency-rule.md`. O percentual é uma orientação e não substitui o registro oficial da instituição.
- **Simulador**: o cálculo é feito no servidor. Informa a média atual e a nota necessária para atingir a meta, com `status` `ALREADY_REACHED` (nota necessária menor ou igual a 0), `ACHIEVABLE` (entre 0 e 10) ou `IMPOSSIBLE` (maior que 10). `requiredScoreRaw` traz a nota necessária sem limitação à faixa 0 a 10; `requiredGrade` é a versão limitada, mantida por compatibilidade.
- **Preferências**: `deadlineAlertDays` (1 a 30, alimenta o painel de Avisos), `attendanceAlertMargin` (0 a 30 pontos), `gradeGoal` (0 a 10, uma casa decimal, opcional) e `startSection` (`DASHBOARD`, `DISCIPLINES`, `ACTIVITIES`, `EXAMS`, `FREQUENCY`, `GRADES`, `SIMULATOR`, `CALENDAR`).
- **Foto de perfil**: modelo único. O frontend recorta a imagem; o servidor aceita PNG ou JPG de até 2 MB, identifica o formato pela assinatura real e regrava como JPEG com no máximo 512 px no maior lado. Avatares do modelo antigo são migrados na inicialização.
- **Autenticação**: token de acesso JWT (15 min por padrão), guardado só na memória da aba, e sessão do navegador em cookie HttpOnly (`acad-organize.session`, `SameSite=Strict`, caminho `/api`), que o JavaScript não lê. O banco guarda só o hash da sessão. "Lembrar de mim" mantém a sessão por 30 dias (cookie persistente); sem ele, o cookie some ao fechar o navegador e vale no máximo 12 horas. O frontend renova o token pelo cookie (`POST /auth/refresh` com o cabeçalho `X-Session-Request: 1`) um minuto antes de vencer, ao voltar para a aba perto do vencimento e ao receber `401`. Sair revoga a sessão no servidor (`POST /auth/logout`). Trocar senha ou e-mail derruba as sessões dos outros navegadores e renova a do navegador atual. Sessão recusada (`401`/`403`) leva à tela de acesso negado. `/auth/refresh` e `/auth/logout` têm limite por IP. O login tem limitador em memória contra força bruta: 5 falhas por e-mail e 20 por IP a cada 15 minutos; o estado é perdido ao reiniciar e não é compartilhado entre instâncias. O cadastro exige aceite dos Termos e da Política e registra data e versões aceitas. A recuperação de senha usa código por e-mail (veja [Recuperação de senha](#recuperação-de-senha-e-mail)).

## API principal

Todas as rotas, exceto `auth` e `legal`, exigem `Authorization: Bearer <token>`. Prefixo `/api/v1`. `D` = `/dashboards/{dashboardId}`, `S` = `D/disciplines/{disciplineId}`.

| Área | Método e rota | Finalidade |
| --- | --- | --- |
| Auth | `POST /auth/register`, `/auth/login`, `/auth/refresh`, `/auth/logout` | Cadastro, login, renovação e saída (sessão por cookie HttpOnly) |
| Recuperação | `POST /auth/password-recovery/request`, `/verify`, `/reset` | Código por e-mail e nova senha |
| Legal | `GET /legal/versions` | Versões vigentes dos textos legais |
| Usuário | `GET`/`PUT /users/me` | Consultar e atualizar perfil |
| Foto | `GET`/`PUT`/`DELETE /users/me/profile-photo` | Foto de perfil (`PUT` multipart, campo `file`) |
| Configurações | `GET /settings/profile`, `PUT /settings/password`, `DELETE /settings/account` | Perfil, troca de senha, exclusão de conta |
| Preferências | `GET`/`PUT /settings/preferences` | Preferências do estudante |
| Dashboards | `POST`/`GET /dashboards` | Criar e listar |
| Disciplinas | `POST`/`GET D/disciplines`; `GET`/`PUT`/`DELETE D/disciplines/{id}`; `PATCH D/disciplines/{id}/status` | CRUD e situação |
| Atividades | `GET D/activities` | Todas as atividades e provas do dashboard |
| Atividades | `POST`/`GET S/activities`; `GET`/`PUT`/`DELETE S/activities/{activityId}` | CRUD por disciplina |
| Notas | `POST`/`GET S/grades`; `GET S/grades/summary`; `GET`/`PUT`/`DELETE S/grades/{gradeId}` | CRUD e resumo |
| Notas | `GET D/gradebook` | Visão consolidada da tela Notas |
| Frequência | `POST`/`PUT`/`GET S/frequency` | Total de faltas e percentuais (`POST` responde `409` se já existir) |
| Faltas | `POST`/`GET S/frequency/absences`; `DELETE S/frequency/absences/{recordId}` | Histórico de faltas |
| Simulador | `POST S/simulator` | Nota necessária para a meta |
| Calendário | `POST`/`GET D/calendar/events`; `GET D/calendar/events/upcoming` (filtro opcional `categories`); `GET`/`PUT`/`DELETE D/calendar/events/{eventId}` | Eventos |

## Migrações

O projeto usa `spring.jpa.hibernate.ddl-auto=update`: o Hibernate cria tabelas e colunas novas, mas não remove colunas, não altera restrições existentes, não cria índices parciais e não corrige dados. Mudanças desse tipo em bancos já existentes exigem scripts manuais. Os scripts (PostgreSQL) estão em `docs/migrations/`; veja `docs/migrations/README.md` para quando e como aplicar.

Pontos de atenção ao atualizar um banco existente:
- Faça **backup antes do primeiro boot** da nova versão: o migrador de avatares legados apaga as linhas antigas de `user_avatars` após converter as fotos.
- `activities.type`, as tabelas `browser_sessions` e `password_recoveries` e as colunas `terms_*`/`privacy_version` são criadas automaticamente pelo `ddl-auto`; não precisam de script. A antiga tabela `refresh_tokens` deixou de ser usada e **precisa ser removida antes de subir a nova versão** com `docs/migrations/2026-10-01-drop-refresh-tokens.sql` (pré-boot, obrigatório): a chave estrangeira dela não tem `ON DELETE CASCADE` e faria a exclusão de conta falhar com erro 500. O `npm run init` aplica esse script sozinho.
- Ordem: backup, scripts pré-boot com a aplicação parada, subir a aplicação, script pós-boot.
- Qualquer mudança em enum exige um script (a restrição CHECK do banco não é atualizada pelo Hibernate).
- Sessões vencidas ou revogadas de uma conta são apagadas no próximo login dela (no máximo 10 por conta).
- Trabalho futuro recomendado: Flyway com `spring.jpa.hibernate.ddl-auto=validate`.

## Pendências conhecidas

- Textos legais (Termos de Uso e Política de Privacidade) são rascunhos, com campos "a definir" (contato, encarregado, local e prazo de retenção).
- Decisões de produto em aberto: situação acadêmica combinada (nota e frequência), horários das aulas no calendário e mais tipos de avaliação.
- O frontend tem testes de componente só para login, sessão e recuperação de senha (Vitest); as demais telas têm apenas testes de lógica (`npm test` em `frontend/`).
- Limitador de login em memória (não distribuído).
