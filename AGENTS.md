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
4. Para iniciar localmente sem PostgreSQL, use o perfil `dev` (H2 em memória, dados perdidos ao reiniciar; usuário demo `desenvolvedor@dev.com`): `./mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"`. O perfil padrão exige PostgreSQL e as variáveis `DB_PASSWORD` e `JWT_SECRET` (veja o README). Para subir tudo (PostgreSQL no Docker, tabelas, seed de demonstração, backend e frontend), use `npm run init` ou a tarefa "init" do VS Code (seção "Início rápido" do README); o perfil `seed` ativa o mesmo seed do `dev` no PostgreSQL e só deve ser usado pelo init.

## Qualidade e IHC

- Escreva fluxos e mensagens pensando em clareza, feedback e acessibilidade.
- Não trate um requisito de interface como implementado apenas porque há um endpoint: valide o fluxo completo com usuários quando aplicável.
- Considere heurísticas de usabilidade, respostas de erro compreensíveis e estados de carregamento/sucesso ao propor integrações com o cliente.
- Documente decisões de produto que afetem estudantes, privacidade ou organização de dados.

## Padrão visual do frontend

- Use Disciplinas e Frequência como referência de dimensões para as demais telas: cards de resumo, filtros, ordenação, botões de adicionar e campos de pesquisa devem manter o mesmo padrão de fontes, ícones, bordas e espaçamentos, com adaptação para telas pequenas.
