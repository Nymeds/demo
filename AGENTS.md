# Orientações para agentes

## Contexto

Este repositório é a API do **Sistema de Organização Acadêmica**, um projeto integrador orientado por práticas de IHC. O público principal são estudantes que precisam organizar compromissos e informações acadêmicas.

O código atual é uma base Spring Boot; não presuma que funcionalidades de domínio já estejam implementadas. Antes de alterar comportamento, confirme a necessidade no código, nas tarefas do projeto ou com o responsável.

## Stack e convenções

- Use Java 21 e Maven Wrapper (`backend/mvnw.cmd` no Windows). O backend fica em `backend/` e o frontend em `frontend/`; rode os comandos Maven dentro de `backend/`.
- Mantenha o código de produção em `backend/src/main/java/studdy/example/demo` e os testes espelhados em `backend/src/test/java/studdy/example/demo`.
- Prefira injeção por construtor; evite estado mutável e lógica de negócio em controllers.
- Valide entradas HTTP com Bean Validation e retorne respostas HTTP coerentes.
- Armazene configurações em `backend/src/main/resources/application.properties`; nunca versione segredos.
- Use PostgreSQL para a persistência de execução e JPA para o mapeamento de entidades.
- Mudanças de esquema que o `ddl-auto=update` não faz (enum/CHECK, índices parciais, FKs, limpeza de dados) viram uma nova migração Flyway `V<n>__` em `backend/src/main/resources/db/migration`, idempotente e protegida com `to_regclass`; nunca edite uma migração já aplicada (veja `docs/migrations/README.md`). O Flyway fica desligado nos perfis `dev` e de teste (H2).

## Fluxo de trabalho

1. Leia os arquivos relevantes e verifique alterações locais antes de editar.
2. Faça alterações pequenas, focadas e acompanhadas de testes quando o comportamento mudar.
3. Execute `cd backend; ./mvnw.cmd test` antes de concluir alterações de código.
4. Para iniciar localmente sem PostgreSQL, use o perfil `dev` (H2 em memória, dados perdidos ao reiniciar; usuário demo `desenvolvedor@dev.com`): `cd backend; ./mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"`. O perfil padrão exige PostgreSQL e as variáveis `DB_PASSWORD` e `JWT_SECRET` (veja o README). Para subir tudo (PostgreSQL no Docker, tabelas, seed de demonstração, backend e frontend), use `npm run init` ou a tarefa "init" do VS Code (seção "Início rápido" do README); o perfil `seed` ativa o mesmo seed do `dev` no PostgreSQL e só deve ser usado pelo init.

## Qualidade e IHC

- Escreva fluxos e mensagens pensando em clareza, feedback e acessibilidade.
- Não trate um requisito de interface como implementado apenas porque há um endpoint: valide o fluxo completo com usuários quando aplicável.
- Considere heurísticas de usabilidade, respostas de erro compreensíveis e estados de carregamento/sucesso ao propor integrações com o cliente.
- Documente decisões de produto que afetem estudantes, privacidade ou organização de dados.

## Padrão visual do frontend

- Use Disciplinas e Frequência como referência de dimensões para as demais telas: cards de resumo, filtros, ordenação, botões de adicionar e campos de pesquisa devem manter o mesmo padrão de fontes, ícones, bordas e espaçamentos, com adaptação para telas pequenas.

## Responsividade

- Breakpoints padrão com `max-width`: 1100 px (sidebar compacta), 760 px (celular) e 520 px (celular estreito). Evite criar breakpoints fora desse padrão.
- Largura mínima alvo: 360 px (conferir também 375 px), sem rolagem horizontal da página; tabelas e listas largas rolam só dentro de um contêiner próprio.
- No celular (≤ 760 px): alvos de toque com pelo menos 44 px, fontes com pelo menos 12 px (`.75rem`) e campos de formulário com pelo menos 16 px, para o navegador não aplicar zoom.
- Navegação móvel: barra inferior com 4 destinos fixos + "Mais" (folha com as demais seções, conta e Sair). A altura da barra fica em `--mobile-nav-height`; o conteúdo reserva esse espaço mais `env(safe-area-inset-bottom)`.
- Sem biblioteca de CSS ou de UI: use o CSS da feature e `<style scoped>`, como no restante do projeto.
