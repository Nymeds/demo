# Sistema de Organização Acadêmica
# RAFAEL É MUITO FODA
API para apoiar estudantes na organização de atividades acadêmicas, desenvolvida como projeto integrador da disciplina de Interação Humano-Computador (IHC).

## Estado atual

O repositório contém a base da API em Spring Boot. Ao iniciar, a aplicação registra `Ola, mundo!` no console para confirmar que o serviço foi carregado.

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
- PostgreSQL
- H2 em memória para desenvolvimento local
- Lombok
- Maven Wrapper

## Como executar

No terminal integrado do VS Code, na pasta do projeto:

```powershell
.\mvnw.cmd spring-boot:run
```

A execução local usa H2 em memória, dispensando configuração inicial de banco. Para a implantação, configure a conexão PostgreSQL por variáveis de ambiente ou por um perfil de produção.

Para executar os testes:

```powershell
.\mvnw.cmd test
```

## Estrutura

```text
src/main/java/studdy/example/demo/   Código-fonte da aplicação
src/main/resources/                  Configurações da aplicação
src/test/java/studdy/example/demo/   Testes automatizados
```

## Próximos incrementos sugeridos

1. Definir personas, problema e histórias de usuário.
2. Modelar os dados e os endpoints para organização acadêmica.
3. Implementar autenticação, persistência PostgreSQL e validações.
4. Criar e validar os fluxos de interface com usuários.
5. Instrumentar métricas de uso para orientar melhorias.
