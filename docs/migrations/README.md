# Migrações manuais (PostgreSQL)

A aplicação usa `spring.jpa.hibernate.ddl-auto=update`. Isso cria tabelas e colunas novas (por exemplo `activities.type`, `browser_sessions`, `password_recoveries` e as colunas `terms_*`/`privacy_version` de `app_users`), mas não remove colunas antigas, não altera restrições nem índices existentes, não cria índices parciais e não corrige dados. Por isso, bancos já existentes precisam destes scripts. Os scripts são para PostgreSQL (o H2 do perfil `dev` e dos testes é descartável).

**Regra: toda mudança em enum (`@Enumerated(EnumType.STRING)`) exige um script.** O Hibernate cria uma restrição CHECK com os valores do enum e o `update` nunca a altera; sem script, bancos existentes rejeitam os valores novos.

## Scripts (nesta ordem)

Pré-boot (aplicar com a aplicação parada; são seguros mesmo se tabelas/colunas novas ainda não existirem):

| Script | O que faz |
| --- | --- |
| `2026-09-29-enum-check-constraints.sql` | Recria `user_preferences_start_section_check` com todos os valores de `StartSection` (inclui `EXAMS`, `FREQUENCY`, `CALENDAR`). |
| `2026-09-29-frequency-discipline-cleanup.sql` | Remove `frequency.total_classes`; mantém uma linha por disciplina (a sobrevivente, escolhida de forma arbitrária, recebe o MAIOR número de faltas); remove outras unicidades em `discipline_id`; cria `uk_frequency_discipline_id`; torna `disciplines.professor_name` opcional (vazio/só espaços vira `NULL`). |
| `2026-09-29-indexes.sql` | Cria os índices `idx_activities_discipline_id`, `idx_disciplines_dashboard_id`, `idx_absence_records_discipline_id`, `idx_calendar_events_dashboard_id`, `idx_calendar_events_discipline_id`, `idx_refresh_tokens_user_id`. |
| `2026-10-01-foreign-keys-set-null.sql` | Recria com `ON DELETE SET NULL` as chaves estrangeiras de `calendar_events.discipline_id` e `grades.activity_id` (acha o nome real em `pg_constraint`). Sem isso, num banco antigo, apagar uma disciplina com eventos no calendário dá erro 500. |
| `2026-10-01-drop-refresh-tokens.sql` | **Obrigatório.** Remove a tabela `refresh_tokens`, que deixou de ser usada quando a sessão passou a ser o cookie HttpOnly (`browser_sessions`). A chave estrangeira dela para `app_users` não tem `ON DELETE CASCADE`: enquanto a tabela existir, excluir uma conta que já fez login dá erro 500. Todos precisam entrar de novo. O `npm run init` aplica este script sozinho. |
| `2026-10-01-indexes.sql` | Cria `idx_dashboards_owner_id`, `idx_discipline_schedules_discipline_id`, `idx_calendar_events_dashboard_starts_at`, `idx_grades_discipline_id`, `idx_refresh_tokens_family`, `idx_refresh_tokens_expires_at` e o índice parcial `idx_refresh_tokens_revoked_at` (`WHERE revoked_at IS NOT NULL`). |

Pós-boot:

| Script | O que faz |
| --- | --- |
| `2026-09-29-dashboard-single-active.sql` | Cria o índice único parcial `ux_dashboards_one_active_per_owner` (um dashboard `ACTIVE` por usuário). Falha se ainda houver duplicados. `IF NOT EXISTS` verifica só o nome, não a definição. |

Todos são idempotentes.

## Como aplicar

1. **Backup** do banco (`pg_dump`). Obrigatório: no primeiro boot o migrador de avatares legados apaga as linhas de `user_avatars` depois de converter as fotos.
2. **Com a aplicação parada**, aplique os scripts pré-boot, na ordem acima.
3. **Suba a aplicação.** Na inicialização rodam a migração de avatares legados e a correção de dashboards ativos duplicados (`ActiveDashboardCorrectionRunner`), e o Hibernate cria tabelas/colunas novas.
4. **Aplique o script pós-boot** (`2026-09-29-dashboard-single-active.sql`).

```bash
for f in 2026-09-29-enum-check-constraints 2026-09-29-frequency-discipline-cleanup 2026-09-29-indexes \
         2026-10-01-foreign-keys-set-null 2026-10-01-drop-refresh-tokens 2026-10-01-indexes; do
  psql -h localhost -p 5432 -U "$DB_USERNAME" -d academic_organizer -f docs/migrations/$f.sql
done
# suba a aplicação, espere concluir a inicialização, então:
psql -h localhost -p 5432 -U "$DB_USERNAME" -d academic_organizer -f docs/migrations/2026-09-29-dashboard-single-active.sql
```

Ajuste host, porta e nome do banco ao seu ambiente (não use a URL JDBC no `psql`). Confira com `\d frequency`, `\d user_preferences` e `\di`.

## Trabalho futuro

Adotar Flyway (ou Liquibase) com `spring.jpa.hibernate.ddl-auto=validate`, substituindo estes scripts manuais por migrações versionadas.
