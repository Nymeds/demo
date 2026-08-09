# Orientações para agentes

## Contexto

Este repositório é a API do **Sistema de Organização Acadêmica**, um projeto integrador orientado por práticas de IHC. O público principal são estudantes que precisam organizar compromissos e informações acadêmicas.

O código atual é uma base Spring Boot; não presuma que funcionalidades de domínio já estejam implementadas. Antes de alterar comportamento, confirme a necessidade no código, nas tarefas do projeto ou com o responsável.

## Stack e convenções

- Use Java 21 e Maven Wrapper (`mvnw.cmd` no Windows).
- Mantenha o código de produção em `src/main/java/studdy/example/demo` e os testes espelhados em `src/test/java/studdy/example/demo`.
- Prefira injeção por construtor; evite estado mutável e lógica de negócio em controllers.
- Valide entradas HTTP com Bean Validation e retorne respostas HTTP coerentes.
- Armazene configurações em `src/main/resources/application.properties`; nunca versione segredos.
- Use PostgreSQL para a persistência de execução e JPA para o mapeamento de entidades.

## Fluxo de trabalho

1. Leia os arquivos relevantes e verifique alterações locais antes de editar.
2. Faça alterações pequenas, focadas e acompanhadas de testes quando o comportamento mudar.
3. Execute `./mvnw.cmd test` antes de concluir alterações de código.
4. Para iniciar localmente, execute `./mvnw.cmd spring-boot:run` no terminal integrado do VS Code.

## Qualidade e IHC

- Escreva fluxos e mensagens pensando em clareza, feedback e acessibilidade.
- Não trate um requisito de interface como implementado apenas porque há um endpoint: valide o fluxo completo com usuários quando aplicável.
- Considere heurísticas de usabilidade, respostas de erro compreensíveis e estados de carregamento/sucesso ao propor integrações com o cliente.
- Documente decisões de produto que afetem estudantes, privacidade ou organização de dados.
