# Sistema de Organização Acadêmica
# RAFAEL É MUITO FODA
API para apoiar estudantes na organização de atividades acadêmicas, desenvolvida como projeto integrador da disciplina de Interação Humano-Computador (IHC).

## Estado atual

O repositório contém uma API Spring Boot e uma aplicação Vue. A API possui cadastro, login por JWT e consulta do usuário autenticado; o frontend oferece uma tela para testar esse fluxo com o banco H2 em memória.

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

Para uma implantação real, configure a conexão com PostgreSQL e os segredos da aplicação usando variáveis de ambiente ou um perfil de produção.

## Estrutura

```text
src/main/java/studdy/example/demo/   Código-fonte da aplicação
src/main/resources/                  Configurações da aplicação
src/test/java/studdy/example/demo/   Testes automatizados
frontend/src/                        Componentes e estilos Vue
frontend/public/                     Arquivos estáticos usados pela interface
```

## Autenticação

| Método | Rota | Finalidade |
| --- | --- | --- |
| `POST` | `/api/v1/auth/register` | Cria um usuário com senha criptografada. |
| `POST` | `/api/v1/auth/login` | Valida as credenciais e retorna um token JWT. |
| `GET` | `/api/v1/users/me` | Retorna o usuário do token enviado em `Authorization: Bearer <token>`. |

## Tela de teste

A tela inicial do Vue permite criar conta e entrar usando a API. Após o login, ela consulta `/api/v1/users/me` e apresenta os dados devolvidos pelo H2.

O botão vermelho `?` ativa um efeito visual independente do fluxo de autenticação. Os arquivos de áudio, GIF e slideshow ficam em `frontend/public/`; o componente responsável é `frontend/src/components/SurpriseButton.vue`.

## Próximos incrementos sugeridos

1. Definir personas, problema e histórias de usuário.
2. Modelar os dados e os endpoints para organização acadêmica.
3. Implementar autenticação, persistência PostgreSQL e validações.
4. Criar e validar os fluxos de interface com usuários.
5. Instrumentar métricas de uso para orientar melhorias.
