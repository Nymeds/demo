-- Flyway (perfil padrao, PostgreSQL). Idempotente e protegido por to_regclass: num banco novo (Flyway roda
-- antes do Hibernate criar as tabelas) vira no-op. Nao edite depois de aplicado: crie um novo V<n>__.
-- Complements V4. Names match the @Index declarations in the entities, so Hibernate finds them
-- already present. (The old refresh_tokens indexes were dropped together with the table in V6.)
DO $$
DECLARE
    specs text[][] := ARRAY[
        ['idx_dashboards_owner_id',                'dashboards',           'owner_id'],
        ['idx_discipline_schedules_discipline_id', 'discipline_schedules', 'discipline_id'],
        ['idx_calendar_events_dashboard_starts_at','calendar_events',      'dashboard_id, starts_at'],
        ['idx_grades_discipline_id',               'grades',               'discipline_id']
    ];
    i int;
BEGIN
    FOR i IN 1 .. array_length(specs, 1) LOOP
        IF to_regclass('public.' || specs[i][2]) IS NOT NULL THEN
            -- The column list is a fixed literal above (it may hold two columns), so it is not quoted.
            EXECUTE format('CREATE INDEX IF NOT EXISTS %I ON %I (%s)', specs[i][1], specs[i][2], specs[i][3]);
        END IF;
    END LOOP;
END $$;
