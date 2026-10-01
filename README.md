# Sistema de Organização Acadêmica

## Estado atual

O repositório contém uma API Spring Boot e uma aplicação Vue. A API cobre cadastro e login por JWT, dashboards por usuário e, dentro de cada dashboard, disciplinas com horários de aula, notas, frequência e atividades. O frontend oferece uma tela para testar o fluxo de autenticação com o banco H2 em memória.

Todo o acesso é isolado por usuário: cada requisição parte do dashboard do usuário autenticado, e recursos de outra pessoa respondem `404` em vez de `403`, para não revelar que existem.

## Visão do produto

O sistema deve reduzir a dificuldade de acompanhar compromissos acadêmicos e tornar as informações relevantes fáceis de localizar. A evolução do produto deve seguir um processo centrado no usuário:

- entender as necessidades de estudantes e demais usuários envolvidos;
- definir requisitos funcionais e não funcionais;
- projetar fluxos e interfaces acessíveis;
- validar as soluções por meio de protótipos, heurísticas e testes de usabilidade;
- acompanhar o uso após o lançamento e promover melhorias contínuas.

## Tecnologias

- Java 21
- Spring Boot 4.1
- Spring Web MVC
- Spring Data JPA
- Bean Validation
- Spring Security e JWT
- PostgreSQL
- H2 em memória para desenvolvimento local
- Lombok
- Maven Wrapper
- Vue 3
- Vite

## Como rodar o projeto

### Recuperação de senha por Gmail

O botão **Esqueci minha senha** abre o fluxo de e-mail, código e nova senha. A API Spring
controla a recuperação e um serviço Node privado em `mailer/` envia os e-mails com Nodemailer.
As credenciais SMTP ficam apenas nesse serviço, sem variáveis `VITE_` nem código no navegador.

Na raiz, prepare os arquivos locais e gere duas chaves aleatórias:

```powershell
node scripts/setup-recovery.mjs
```

O comando cria ou atualiza `.env` e `mailer/.env`, preserva as configurações existentes
e sincroniza `MAILER_API_SECRET`. Pode ser executado novamente sem trocar as chaves.
O backend lê o `.env` da raiz ao iniciar; variáveis do ambiente têm precedência.
O serviço Node lê `mailer/.env`. Esses arquivos são ignorados pelo Git.

Edite **somente no seu computador** `mailer/.env`:

```dotenv
SMTP_HOST=smtp.gmail.com
SMTP_PORT=auto
SMTP_USER=seuemail@gmail.com
SMTP_PASSWORD=sua-senha-de-app
MAIL_FROM_NAME=AcadOrganize
```

Use uma **senha de app** do Google, com a verificação em duas etapas ativada; não use
a senha normal da conta. Veja [a orientação oficial do Google](https://support.google.com/accounts/answer/185833?hl=pt-BR)
e [a configuração Gmail no Nodemailer](https://nodemailer.com/guides/using-gmail).
Se a conta não disponibilizar senhas de app, será necessário configurar OAuth2 para ela.

Instale as dependências do mailer uma vez:

```powershell
cd mailer
npm ci
npm run verify
cd ..
```

Com `MAILER_AUTO_START=true` no `.env` da raiz (configurado pelo script), iniciar a API com
`./mvnw.cmd spring-boot:run` no terminal integrado do VS Code também inicia o Nodemailer.
Não é necessário um terceiro terminal. Reinicie a API após alterar a configuração.
O frontend continua usando `npm run dev` dentro de `frontend/`. É necessário Node.js 20.19+.

O script `mailer/src/select-smtp.js` roda na inicialização e verifica **465 com TLS**, depois
**587 com STARTTLS** se a primeira falhar. Cada tentativa tem limite de 15 segundos.
Uma porta só é aceita após conexão, certificado TLS válido e autenticação no Gmail.
O log informa a porta escolhida. A escolha vale para o processo atual e é refeita a cada
inicialização, sem reescrever credenciais. Para fixar uma porta, use `SMTP_PORT=465` ou `587`.
Autenticação recusada interrompe as tentativas: trocar a porta não corrige uma senha inválida.
`npm run verify` executa a mesma seleção sem enviar e-mail.

O endereço HTTP privado permanece em `127.0.0.1:3001`, definido por `MAILER_URL` da API;
as portas 465/587 são do servidor SMTP do Gmail. A API verifica a saúde autenticada do mailer
e reutiliza um serviço já pronto. Ao desligar, encerra somente o processo que ela própria criou.
Se o Node ou SMTP falhar, a API continua atendendo e registra a causa da indisponibilidade do envio.
O SMTP não é testado em cada requisição nem nos testes Spring que usam servidor simulado.

Para executar o mailer separadamente ou em produção com supervisão de processos, use
`MAILER_AUTO_START=false` e `npm start` dentro de `mailer/`. `MAILER_DIRECTORY` permite definir
o caminho da pasta mailer e `NODE_EXECUTABLE` permite informar o caminho absoluto do Node.

Para validar manualmente, cadastre uma conta com um endereço que você controla,
saia da conta e use **Esqueci minha senha**. Receba o código, valide-o, defina a nova senha
e confirme que consegue entrar com ela. Confira também os fluxos de código incorreto,
reenvio, expiração e retorno ao login em celular e com navegação por teclado.

As rotas públicas são `POST /api/v1/auth/password-recovery/request` (e-mail, HTTP 202),
`/verify` (e-mail e `code`, devolve `resetToken` e `expiresIn`) e `/reset`
(e-mail, `resetToken` e `newPassword`). A recuperação não faz login automaticamente.

**Proteções implementadas:**

- Resposta idêntica para conta existente, inexistente e envio em intervalo bloqueado.
  A consulta da conta e o SMTP ocorrem em filas limitadas, fora da resposta HTTP.
- Código de seis dígitos gerado por `SecureRandom`, válido por dez minutos, com cinco
  tentativas. Apenas HMAC com chave privada e nonce é persistido; código e token não são registrados em logs.
- No máximo um envio por minuto e cinco por hora por conta; contadores ficam no banco.
  Um reenvio permitido substitui o código e a autorização anteriores.
- Código válido é consumido e trocado por autorização aleatória de 256 bits, válida por cinco
  minutos e usada uma vez. Ela fica apenas na memória da tela, sem URL ou armazenamento local.
- Transações e bloqueio da conta serializam verificações e trocas simultâneas. Tentativas
  incorretas são persistidas mesmo quando a API responde erro.
- Senha protegida com o BCrypt existente, validação do limite de 72 bytes UTF-8 e invalidação
  de JWTs anteriores. Mudança de e-mail ou senha também invalida a recuperação pendente.
- Dados de recuperação são excluídos pelo banco junto com a conta. Existe no máximo um
  registro por conta; registros expirados são inutilizáveis e substituídos no próximo envio.
- Limite adicional de 30 requisições por hora por IP, em memória por instância, com HTTP 429.
  O aplicativo usa o endereço do peer e não confia diretamente em `X-Forwarded-For`.
- Serviço de e-mail autenticado por chave interna, corpo limitado, destinatário único,
  TLS SMTP obrigatório e certificado validado. O remetente é o Gmail autenticado.

Em produção, execute com PostgreSQL (`postgres`), use HTTPS no site e mantenha o mailer
acessível apenas pela API. `MAILER_URL` exige HTTPS quando sai de localhost. Em ambientes
com proxy ou várias instâncias, configure o proxy confiável para o IP real e um limite
compartilhado no gateway: o limite de IP em memória reinicia com a API e não é global.
O contador por conta continua compartilhado no PostgreSQL.
As filas são locais: se o processo cair ou o SMTP falhar, a pessoa deve solicitar reenvio
após o intervalo. Monitore as mensagens de falha sem registrar payloads sensíveis.
O H2 local perde contas e códigos ao reiniciar; use PostgreSQL para persistência de execução.

Testes: `./mvnw.cmd test` na raiz, `npm test` em `mailer/`, e `npm test` e `npm run build`
em `frontend/`. Os testes automatizados não enviam mensagens reais pelo Gmail.

A interface foi conferida no navegador em 320×568, 390×844, 768×1024, 1024×768 e
1440×900, nas etapas de e-mail, código, nova senha, sucesso, login e cadastro (30
combinações). Não houve rolagem horizontal nem campos ou botões cortados após os ajustes.
E-mails longos quebram linha; telas altas usam rolagem vertical normal. No celular,
os campos de recuperação usam fonte de 16 px e os botões auxiliares têm área de toque
de pelo menos 44 px. Mensagens de erro recebem foco para ficarem visíveis e serem
anunciadas por leitores de tela. O teste visual usou API simulada, sem alterar contas
reais nem enviar e-mails; a entrega real ainda depende das credenciais do Gmail.

### Pré-requisitos

- Java 21;
- Node.js com npm;
- dois terminais abertos na pasta raiz do projeto.

Não é necessário instalar o Maven, pois o repositório inclui o Maven Wrapper. A execução local também usa o banco H2 em memória, portanto não exige uma instalação do PostgreSQL.

### PostgreSQL com Docker

Com o Docker Desktop aberto, inicie o banco:

**Windows:**
```powershell
docker compose up -d postgres
```

**Linux/Mac:**
```bash
docker compose up -d postgres
```

Confira se o container está saudável:

**Windows:**
```powershell
docker compose ps
```

**Linux/Mac:**
```bash
docker compose ps
```

Para iniciar a API usando PostgreSQL:

**Windows:**
```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=postgres"
```

**Linux/Mac:**
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres
```

Por padrão, o banco usa `studdy` como database e usuário, `studdy_dev` como senha e a porta `5432`. Para personalizar, copie `.env.example` para `.env` e altere os valores. O arquivo `.env` não é versionado.

Para parar o banco sem apagar os dados:

**Windows:**
```powershell
docker compose stop postgres
```

**Linux/Mac:**
```bash
docker compose stop postgres
```

O volume `studdy_postgres_data` mantém os dados entre reinicializações.

### 1. Iniciar o backend

No primeiro terminal, na pasta raiz do projeto, execute:

**Windows:**
```powershell
.\mvnw.cmd spring-boot:run
```

**Linux/Mac:**
```bash
./mvnw spring-boot:run
```

Quando a inicialização terminar, a API estará disponível em `http://localhost:8080`. Mantenha esse terminal aberto enquanto estiver usando o sistema.

> No PowerShell, use `./mvnw.cmd` ou `.\mvnw.cmd`. O comando `\mvnw.cmd` não procura o arquivo na pasta atual.

### 2. Iniciar o frontend

Abra um segundo terminal na pasta raiz do projeto e execute:

**Windows/Linux/Mac:**
```bash
cd frontend
npm install
npm run dev
```

O `npm install` instala as dependências e normalmente só é necessário na primeira execução ou quando elas forem alteradas. Mantenha esse segundo terminal aberto também.

O proxy do Vite lê o endereço da API pela variável `VITE_API_PROXY_TARGET`. Quando ela não é definida, o frontend usa `http://localhost:8080`, adequado para executar backend e frontend na mesma máquina.

Para usar outro backend, crie uma configuração local que não será versionada:

**Windows:**
```powershell
Copy-Item .env.example .env.local
```

**Linux/Mac:**
```bash
cp .env.example .env.local
```

Edite `frontend/.env.local` e informe somente o endereço do ambiente desejado:

```dotenv
VITE_API_PROXY_TARGET=https://endereco-do-backend
```

Reinicie `npm run dev` depois de alterar o arquivo. O `.env.local` é ignorado pelo Git e não deve ser adicionado ao repositório.

#### Frontend local conectado a uma API no GitHub Codespaces

1. No Codespace, mantenha a API em execução na porta `8080`.
2. Na aba **PORTS**, copie o **Forwarded Address** da porta `8080`.
3. Para um teste temporário sem túnel local, altere a visibilidade da porta `8080` para **Public**.
4. Copie `frontend/.env.example` para `frontend/.env.local` e coloque o endereço encaminhado em `VITE_API_PROXY_TARGET`.
5. Inicie ou reinicie o frontend local com `npm run dev`.
6. Ao terminar, retorne a porta `8080` para **Private**.

Nunca torne a porta `5432` pública. Não coloque URL de ambiente, chave JWT, token ou senha no `vite.config.js`; segredos do backend devem ser configurados como **Codespaces secrets**, e não em arquivos do frontend.

### 3. Acessar a aplicação

Abra no navegador:

```text
http://localhost:5173
```

Durante o desenvolvimento, o Vite encaminha automaticamente as requisições iniciadas por `/api` para o backend em `http://localhost:8080`.

### Executar os testes

Na pasta raiz do projeto, execute:

**Windows:**
```powershell
.\mvnw.cmd test
```

**Linux/Mac:**
```bash
./mvnw test
```

A suíte cobre as validações dos formulários, as regras de aprovação por nota e por frequência, o isolamento entre usuários e a persistência em cascata. Os testes de integração sobem o contexto Spring com o H2 em memória e desfazem as transações ao final, então não deixam dados para trás.

Para uma implantação real, configure a conexão com PostgreSQL e os segredos da aplicação usando variáveis de ambiente ou um perfil de produção. As variáveis lidas pela aplicação e pelo `compose.yaml` estão documentadas em `.env.example`.

## Estrutura

```text
src/main/java/studdy/example/demo/   Código-fonte da aplicação
src/main/resources/                  Configurações da aplicação
src/test/java/studdy/example/demo/   Testes automatizados
frontend/src/                        Componentes e estilos Vue
frontend/public/                     Arquivos estáticos usados pela interface
```

## API

Todas as rotas abaixo de `/api/v1/dashboards` exigem o cabeçalho `Authorization: Bearer <token>`.

### Autenticação

| Método | Rota | Finalidade |
| --- | --- | --- |
| `POST` | `/api/v1/auth/register` | Cria um usuário com senha criptografada. |
| `POST` | `/api/v1/auth/login` | Valida as credenciais e retorna um token JWT. |
| `GET` | `/api/v1/users/me` | Retorna o usuário do token enviado em `Authorization: Bearer <token>`. |

### Dashboards e disciplinas

| Método | Rota | Finalidade |
| --- | --- | --- |
| `POST` | `/api/v1/dashboards` | Cria um dashboard para o usuário autenticado. |
| `POST` | `/api/v1/dashboards/{dashboardId}/disciplines` | Cadastra uma disciplina com seus horários. |
| `GET` | `/api/v1/dashboards/{dashboardId}/disciplines` | Lista as disciplinas do dashboard em ordem alfabética. |
| `GET` | `/api/v1/dashboards/{dashboardId}/disciplines/{disciplineId}` | Detalha uma disciplina, já com média e situação. |
| `PUT` | `/api/v1/dashboards/{dashboardId}/disciplines/{disciplineId}` | Atualiza os dados e os horários da disciplina. |
| `DELETE` | `/api/v1/dashboards/{dashboardId}/disciplines/{disciplineId}` | Remove a disciplina e, em cascata, as notas dela. |

### Notas

Rotas relativas a `/api/v1/dashboards/{dashboardId}/disciplines/{disciplineId}/grades`.

| Método | Rota | Finalidade |
| --- | --- | --- |
| `POST` | `` | Registra uma avaliação com nota de 0 a 10. |
| `GET` | `` | Lista as notas da disciplina, da mais recente para a mais antiga. |
| `GET` | `/summary` | Devolve a média, a média de aprovação da disciplina e a situação. |
| `GET` | `/{gradeId}` | Detalha uma nota. |
| `PUT` | `/{gradeId}` | Atualiza uma nota. |
| `DELETE` | `/{gradeId}` | Remove uma nota. |

### Frequência

Cada disciplina tem no máximo um registro de frequência, em `/api/v1/dashboards/{dashboardId}/disciplines/{disciplineId}/frequency`.

| Método | Rota | Finalidade |
| --- | --- | --- |
| `POST` | `` | Registra o total de aulas e as faltas. Responde `409` se já houver um registro. |
| `GET` | `` | Devolve o percentual de presença, o mínimo de aulas exigido e o teto de faltas. |
| `PUT` | `` | Atualiza o total de aulas e as faltas, recalculando os limites. |

### Atividades

Rotas relativas a `/api/v1/dashboards/{dashboardId}/disciplines/{disciplineId}/activities`, com `POST`, `GET`, `GET /{activityId}`, `PUT /{activityId}` e `DELETE /{activityId}`.

## Critérios de aprovação da disciplina

Cada disciplina carrega os dois critérios, definidos no cadastro e usados em cálculos independentes:

| Campo | Escala | Onde é aplicado |
| --- | --- | --- |
| `passingAverage` | 0 a 10 | Compara com a média das notas e define `APPROVED` ou `FAILED_BY_GRADE`. |
| `minimumAttendancePercentage` | 0 a 100 | Define quantas aulas o aluno precisa cursar e, por consequência, o teto de faltas. |

Os dois são obrigatórios ao criar ou atualizar uma disciplina. Como cada matéria guarda o próprio critério, disciplinas do mesmo dashboard podem exigir médias diferentes — não existe valor global em arquivo de configuração.

## Tela de teste

A tela inicial do Vue permite criar conta e entrar usando a API. Após o login, ela consulta `/api/v1/users/me` e apresenta os dados devolvidos pelo H2.

O botão vermelho `?` ativa um efeito visual independente do fluxo de autenticação. Os arquivos de áudio, GIF e slideshow ficam em `frontend/public/`; o componente responsável é `frontend/src/components/SurpriseButton.vue`.

## Próximos incrementos sugeridos

1. Definir personas, problema e histórias de usuário.
2. Ligar a aplicação ao PostgreSQL do `compose.yaml`, hoje disponível mas não usado pelo perfil padrão.
3. Combinar nota e frequência em uma única situação da disciplina, cobrindo os estados `FAILED_BY_ATTENDANCE` e `FAILED_BY_GRADE_AND_ATTENDANCE` já previstos em `DisciplineStatus`.
4. Criar e validar os fluxos de interface com usuários.
5. Instrumentar métricas de uso para orientar melhorias.
