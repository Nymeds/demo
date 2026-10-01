-- PRE-BOOT. PostgreSQL only. Idempotent; skips tables/columns that do not exist yet.
-- calendar_events.discipline_id and grades.activity_id are @OnDelete(SET_NULL) in the entities, but
-- ddl-auto=update never changes a foreign key that already exists. In older databases the key is
-- still NO ACTION, so deleting a discipline with calendar events (or an activity with grades)
-- fails with HTTP 500. This finds each key by its column (the generated name varies) and recreates
-- it with ON DELETE SET NULL, keeping the same name.
DO $$
DECLARE
    specs text[][] := ARRAY[
        ['calendar_events', 'discipline_id', 'disciplines'],
        ['grades',          'activity_id',   'activities']
    ];
    i int;
    fk record;
BEGIN
    FOR i IN 1 .. array_length(specs, 1) LOOP
        CONTINUE WHEN to_regclass('public.' || specs[i][1]) IS NULL;

        FOR fk IN
            SELECT c.conname, c.confdeltype
            FROM pg_constraint c
            JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attnum = ANY (c.conkey)
            WHERE c.contype = 'f'
              AND c.conrelid = ('public.' || specs[i][1])::regclass
              AND c.confrelid = ('public.' || specs[i][3])::regclass
              AND array_length(c.conkey, 1) = 1
              AND a.attname = specs[i][2]
        LOOP
            CONTINUE WHEN fk.confdeltype = 'n'; -- already ON DELETE SET NULL
            EXECUTE format('ALTER TABLE %I DROP CONSTRAINT %I', specs[i][1], fk.conname);
            EXECUTE format(
                'ALTER TABLE %I ADD CONSTRAINT %I FOREIGN KEY (%I) REFERENCES %I (id) ON DELETE SET NULL',
                specs[i][1], fk.conname, specs[i][2], specs[i][3]
            );
        END LOOP;
    END LOOP;
END $$;
