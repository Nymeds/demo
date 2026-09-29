-- PRE-BOOT. PostgreSQL only. Idempotent; each index is created only if its table exists.
-- Names match the @Index declarations in the entities, so Hibernate finds them already present.
DO $$
DECLARE
    specs text[][] := ARRAY[
        ['idx_activities_discipline_id',        'activities',      'discipline_id'],
        ['idx_disciplines_dashboard_id',        'disciplines',     'dashboard_id'],
        ['idx_absence_records_discipline_id',   'absence_records', 'discipline_id'],
        ['idx_calendar_events_dashboard_id',    'calendar_events', 'dashboard_id'],
        ['idx_calendar_events_discipline_id',   'calendar_events', 'discipline_id'],
        ['idx_refresh_tokens_user_id',          'refresh_tokens',  'user_id']
    ];
    i int;
BEGIN
    FOR i IN 1 .. array_length(specs, 1) LOOP
        IF to_regclass('public.' || specs[i][2]) IS NOT NULL THEN
            EXECUTE format('CREATE INDEX IF NOT EXISTS %I ON %I (%I)', specs[i][1], specs[i][2], specs[i][3]);
        END IF;
    END LOOP;
END $$;
