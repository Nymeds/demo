-- Flyway (perfil padrao, PostgreSQL). Idempotente e protegido por to_regclass: num banco novo (Flyway roda
-- antes do Hibernate criar as tabelas) vira no-op. Nao edite depois de aplicado: crie um novo V<n>__.
-- PRE-BOOT. PostgreSQL only. Idempotent; safe before the first boot (guards on table/column existence).

-- Remove legacy column.
DO $$
BEGIN
    IF to_regclass('public.frequency') IS NOT NULL THEN
        ALTER TABLE frequency DROP COLUMN IF EXISTS total_classes;
    END IF;
END $$;

-- Frequency becomes 1:1 with discipline.
DO $$
DECLARE
    disc_attnum smallint;
    c record;
BEGIN
    IF to_regclass('public.frequency') IS NULL THEN
        RETURN;
    END IF;

    -- Keep one row per discipline. The survivor gets MAX(absences) (the highest recorded count
    -- avoids silently forgiving absences). Which row survives is ARBITRARY (ctid order; the table
    -- has no timestamp column) and irrelevant, since the survivor receives the maximum.
    WITH ranked AS (
        SELECT id, discipline_id, absences,
               ROW_NUMBER() OVER (PARTITION BY discipline_id ORDER BY ctid DESC) AS rn,
               MAX(absences) OVER (PARTITION BY discipline_id) AS max_absences
        FROM frequency
    ), upd AS (
        UPDATE frequency f SET absences = r.max_absences
        FROM ranked r WHERE f.id = r.id AND r.rn = 1 AND f.absences <> r.max_absences
        RETURNING f.id
    )
    DELETE FROM frequency f USING ranked r WHERE f.id = r.id AND r.rn > 1;

    SELECT attnum INTO disc_attnum FROM pg_attribute
    WHERE attrelid = 'public.frequency'::regclass AND attname = 'discipline_id' AND NOT attisdropped;

    -- Drop OTHER unique constraints on (discipline_id) (e.g. Hibernate-generated UK...).
    FOR c IN
        SELECT conname FROM pg_constraint
        WHERE conrelid = 'public.frequency'::regclass AND contype = 'u'
          AND conkey = ARRAY[disc_attnum] AND conname <> 'uk_frequency_discipline_id'
    LOOP
        EXECUTE format('ALTER TABLE frequency DROP CONSTRAINT %I', c.conname);
    END LOOP;

    -- Drop OTHER stand-alone unique indexes on (discipline_id) not backing a constraint.
    FOR c IN
        SELECT ic.relname AS idxname
        FROM pg_index i JOIN pg_class ic ON ic.oid = i.indexrelid
        WHERE i.indrelid = 'public.frequency'::regclass AND i.indisunique AND NOT i.indisprimary
          AND i.indnatts = 1 AND i.indkey[0] = disc_attnum AND i.indpred IS NULL
          AND ic.relname <> 'uk_frequency_discipline_id'
          AND NOT EXISTS (SELECT 1 FROM pg_constraint k WHERE k.conindid = i.indexrelid)
    LOOP
        EXECUTE format('DROP INDEX %I', c.idxname);
    END LOOP;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint
                   WHERE conrelid = 'public.frequency'::regclass AND conname = 'uk_frequency_discipline_id') THEN
        ALTER TABLE frequency ADD CONSTRAINT uk_frequency_discipline_id UNIQUE (discipline_id);
    END IF;
END $$;

-- Optional professor stored as NULL (blank/whitespace-only becomes NULL).
DO $$
BEGIN
    IF to_regclass('public.disciplines') IS NOT NULL THEN
        ALTER TABLE disciplines ALTER COLUMN professor_name DROP NOT NULL;
        UPDATE disciplines SET professor_name = NULL WHERE professor_name ~ '^\s*$';
    END IF;
END $$;

