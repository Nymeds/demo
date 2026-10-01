-- PRE-BOOT. PostgreSQL only. Idempotent; each index is created only if its table exists.
-- Complements 2026-09-29-indexes.sql. Names match the @Index declarations in the entities, so
-- Hibernate finds them already present. The partial index on refresh_tokens.revoked_at only exists
-- here (JPA cannot declare partial indexes).
DO $$
DECLARE
    specs text[][] := ARRAY[
        ['idx_dashboards_owner_id',                'dashboards',           'owner_id'],
        ['idx_discipline_schedules_discipline_id', 'discipline_schedules', 'discipline_id'],
        ['idx_calendar_events_dashboard_starts_at','calendar_events',      'dashboard_id, starts_at'],
        ['idx_grades_discipline_id',               'grades',               'discipline_id'],
        ['idx_refresh_tokens_family',              'refresh_tokens',       'family_id'],
        ['idx_refresh_tokens_expires_at',          'refresh_tokens',       'expires_at']
    ];
    i int;
BEGIN
    FOR i IN 1 .. array_length(specs, 1) LOOP
        IF to_regclass('public.' || specs[i][2]) IS NOT NULL THEN
            -- The column list is a fixed literal above (it may hold two columns), so it is not quoted.
            EXECUTE format('CREATE INDEX IF NOT EXISTS %I ON %I (%s)', specs[i][1], specs[i][2], specs[i][3]);
        END IF;
    END LOOP;

    IF to_regclass('public.refresh_tokens') IS NOT NULL THEN
        CREATE INDEX IF NOT EXISTS idx_refresh_tokens_revoked_at
            ON refresh_tokens (revoked_at) WHERE revoked_at IS NOT NULL;
    END IF;
END $$;
