# Sistema de Organização Acadêmica
# RAFAEL É MUITO FODA
API para apoiar estudantes na organização de atividades acadêmicas, desenvolvida como projeto integrador da disciplina de Interação Humano-Computador (IHC).

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

### Pré-requisitos

- Java 21;
- Node.js com npm;
- dois terminais abertos na pasta raiz do projeto.

Não é necessário instalar o Maven, pois o repositório inclui o Maven Wrapper. A execução local também usa o banco H2 em memória, portanto não exige uma instalação do PostgreSQL.

### PostgreSQL com Docker

Com o Docker Desktop aberto, inicie o banco:

```powershell
docker compose up -d postgres
```

Confira se o container está saudável:

```powershell
docker compose ps
```

Para iniciar a API usando PostgreSQL:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=postgres"
```

Por padrão, o banco usa `studdy` como database e usuário, `studdy_dev` como senha e a porta `5432`. Para personalizar, copie `.env.example` para `.env` e altere os valores. O arquivo `.env` não é versionado.

Para parar o banco sem apagar os dados:

```powershell
docker compose stop postgres
```

O volume `studdy_postgres_data` mantém os dados entre reinicializações.

### 1. Iniciar o backend

No primeiro terminal, na pasta raiz do projeto, execute:

```powershell
.\mvnw.cmd spring-boot:run
```

Quando a inicialização terminar, a API estará disponível em `http://localhost:8080`. Mantenha esse terminal aberto enquanto estiver usando o sistema.

> No PowerShell, use `./mvnw.cmd` ou `.\mvnw.cmd`. O comando `\mvnw.cmd` não procura o arquivo na pasta atual.

### 2. Iniciar o frontend

Abra um segundo terminal na pasta raiz do projeto e execute:

```powershell
cd frontend
npm install
npm run dev
```

O `npm install` instala as dependências e normalmente só é necessário na primeira execução ou quando elas forem alteradas. Mantenha esse segundo terminal aberto também.

O proxy do Vite lê o endereço da API pela variável `VITE_API_PROXY_TARGET`. Quando ela não é definida, o frontend usa `http://localhost:8080`, adequado para executar backend e frontend na mesma máquina.

Para usar outro backend, crie uma configuração local que não será versionada:

```powershell
Copy-Item .env.example .env.local
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

```powershell
.\mvnw.cmd test
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
