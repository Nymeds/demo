# Migrações (PostgreSQL) com Flyway

As migrações ficam em `backend/src/main/resources/db/migration` e são aplicadas **automaticamente** pelo Flyway a cada inicialização do perfil padrão (PostgreSQL), inclusive pelo `npm run init`. Não há mais scripts manuais para rodar com `psql`. Nos perfis `dev` e de teste (H2 em memória) o Flyway fica desligado (`spring.flyway.enabled=false`).

## Como funciona

1. O Flyway roda **antes** do Hibernate. Num banco sem a tabela `flyway_schema_history` e com outras tabelas, ele registra uma linha de base na versão 1 (`baseline-on-migrate=true`, `baseline-version=1`) e aplica V2 em diante.
2. Depois o Hibernate (`spring.jpa.hibernate.ddl-auto=${DDL_AUTO:update}`) cria tabelas e colunas novas.
3. Por fim rodam os `ApplicationRunner` (migração de avatares legados, `ActiveDashboardCorrectionRunner`, seed).

Como o Flyway roda antes do Hibernate, num **banco novo** as tabelas ainda não existem: todos os scripts são protegidos com `to_regclass(...)` e viram no-op. Isso é intencional: o Hibernate cria as tabelas já com as restrições e índices corretos.

| Arquivo | O que faz |
| --- | --- |
| `V2__enum_check_constraints.sql` | Recria `user_preferences_start_section_check` com todos os valores de `StartSection`. |
| `V3__frequency_discipline_cleanup.sql` | Remove `frequency.total_classes`; uma linha por disciplina (a sobrevivente recebe o MAIOR número de faltas); cria `uk_frequency_discipline_id`; `disciplines.professor_name` opcional. |
| `V4__indexes.sql` | `idx_activities_discipline_id`, `idx_disciplines_dashboard_id`, `idx_absence_records_discipline_id`, `idx_calendar_events_dashboard_id`, `idx_calendar_events_discipline_id`. |
| `V5__foreign_keys_set_null.sql` | Recria com `ON DELETE SET NULL` as FKs de `calendar_events.discipline_id` e `grades.activity_id`. |
| `V6__drop_refresh_tokens.sql` | Remove a tabela antiga `refresh_tokens` (sessão agora é o cookie HttpOnly em `browser_sessions`); sem isso, excluir conta dava erro 500. Todos entram de novo. |
| `V7__indexes.sql` | `idx_dashboards_owner_id`, `idx_discipline_schedules_discipline_id`, `idx_calendar_events_dashboard_starts_at`, `idx_grades_discipline_id`. |
| `afterMigrate__dashboard_single_active.sql` | Callback que roda **após todo migrate** (todo boot): cria o índice único parcial `ux_dashboards_one_active_per_owner` (um dashboard `ACTIVE` por usuário). |

### Índice parcial dos dashboards (por que callback e não `R__`)

O índice depende da tabela `dashboards` (criada pelo Hibernate, depois do Flyway) e de não haver duplicados (corrigidos pelo `ActiveDashboardCorrectionRunner`, também depois do Flyway). Um script repetível `R__` só roda de novo quando o conteúdo muda; num banco novo ele rodaria uma vez como no-op e nunca mais. O callback `afterMigrate` roda a cada boot e é idempotente:

- banco novo: no-op no primeiro boot, índice criado no **segundo boot**;
- duplicados ainda presentes: pula com `NOTICE` (nunca derruba o boot); o runner corrige e o índice é criado no próximo boot;
- índice já existe: nada a fazer.

## Regras para mudanças novas

- **Toda mudança em enum (`@Enumerated(EnumType.STRING)`) exige uma migração** `V<n>__...sql`: o Hibernate cria um CHECK com os valores do enum e o `update` nunca o altera.
- Nunca edite um `V<n>__` já aplicado (o checksum muda e o Flyway recusa subir). Crie o próximo número.
- Mantenha os scripts idempotentes e protegidos com `to_regclass` enquanto o Hibernate ainda criar as tabelas.
- Não use `BEGIN`/`COMMIT` nos scripts: o Flyway já envolve cada migração numa transação.
- Faça **backup** (`pg_dump`) antes de subir uma versão nova em banco com dados: o migrador de avatares legados apaga as linhas de `user_avatars` depois de converter as fotos.

## Plano (`ddl-auto=validate`)

1. Gerar um `V1__baseline.sql` com o esquema completo (por exemplo `pg_dump --schema-only` de um banco criado pela versão atual) e remover as guardas `to_regclass` das migrações seguintes que passarem a depender dele.
2. Bancos existentes continuam com a linha de base na versão 1 (não executam o V1).
3. Rodar com `DDL_AUTO=validate` em homologação; quando estável, tornar `validate` o padrão.

O teste `PostgresSmokeTest` (Testcontainers, `postgres:16-alpine`) sobe o contexto com o Flyway ligado e confere as tabelas; ele é pulado automaticamente quando não há Docker.
