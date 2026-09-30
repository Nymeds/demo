-- PRE-BOOT. PostgreSQL only. Idempotent; safe before the first boot of the new version.
-- Hibernate 7 creates CHECK constraints for @Enumerated(STRING) columns and ddl-auto=update
-- never changes them, so existing databases reject the new StartSection values.
-- Values must match backend/src/main/java/studdy/example/demo/settings/StartSection.java.
-- Other enum columns had no value changes in this work (activities.type is a new column and is
-- created by Hibernate together with its constraint). Any future enum change needs a script like this.
BEGIN;

DO $$
DECLARE
    c record;
BEGIN
    IF to_regclass('public.user_preferences') IS NULL THEN
        RAISE NOTICE 'user_preferences does not exist yet; Hibernate will create it with the right constraint.';
        RETURN;
    END IF;

    FOR c IN
        SELECT conname FROM pg_constraint
        WHERE conrelid = to_regclass('public.user_preferences')
          AND contype = 'c'
          AND pg_get_constraintdef(oid) ILIKE '%start_section%'
    LOOP
        EXECUTE format('ALTER TABLE user_preferences DROP CONSTRAINT %I', c.conname);
    END LOOP;

    ALTER TABLE user_preferences
        ADD CONSTRAINT user_preferences_start_section_check
        CHECK (start_section IN ('DASHBOARD','DISCIPLINES','ACTIVITIES','EXAMS','FREQUENCY','GRADES','SIMULATOR','CALENDAR'));
END $$;

COMMIT;
