# Backend — AcadOrganize

API do Sistema de Organização Acadêmica, em Spring Boot. O frontend, a visão geral do projeto, as regras de domínio e a lista de rotas estão no [README da raiz](../README.md).

## Stack

- Java 21 e Spring Boot (Web MVC, Data JPA, Bean Validation, Security com JWT), Lombok
- PostgreSQL no perfil padrão; H2 em memória no perfil `dev` e nos testes
- Maven Wrapper (`mvnw`/`mvnw.cmd`): não precisa instalar o Maven

Para abrir na IDE Java, importe o `pom.xml` desta pasta.

## Como rodar

Os comandos abaixo partem da raiz do repositório. No Linux/macOS, troque `.\mvnw.cmd` por `./mvnw`.

```powershell
cd backend
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"   # H2 em memória, sem PostgreSQL
.\mvnw.cmd spring-boot:run                                    # perfil padrão (PostgreSQL)
```

- **Perfil `dev`**: os dados são perdidos a cada reinicialização. Cria o usuário demo `desenvolvedor@dev.com` (senha idêntica ao e-mail). Só para desenvolvimento local.
- **Perfil padrão**: exige PostgreSQL e as variáveis `DB_PASSWORD` e `JWT_SECRET` (mínimo de 32 bytes). O `.env` e o `compose.yaml` ficam na raiz do projeto; a tabela de variáveis e o passo a passo com Docker estão no README da raiz.

A API fica em `http://localhost:8080`. Para subir banco, backend e frontend juntos, use o `npm run init` na raiz.

## Testes

```powershell
cd backend
.\mvnw.cmd test        # H2 em memória, configuração em src/test/resources/application.properties
```

## Organização

```text
src/main/java/studdy/example/demo/   Código-fonte, um pacote por funcionalidade (auth, discipline, grade...)
src/main/resources/                  application.properties e application-dev.properties
src/test/java/studdy/example/demo/   Testes automatizados (espelham os pacotes acima)
src/test/resources/                  Configuração dos testes (H2)
```
