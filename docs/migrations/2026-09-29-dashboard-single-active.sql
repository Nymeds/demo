-- POST-BOOT. PostgreSQL only (H2 does not support partial indexes).
-- Apply AFTER the app booted once with the new version (ActiveDashboardCorrectionRunner fixes
-- owners with more than one ACTIVE dashboard); otherwise creation fails on duplicates.
-- Idempotent, but NOTE: IF NOT EXISTS checks only the index NAME, not its definition. If an index
-- with this name exists with a different definition, drop it manually and re-run.
DO $$
BEGIN
    IF to_regclass('public.dashboards') IS NULL THEN
        RAISE NOTICE 'dashboards does not exist; boot the app first.';
        RETURN;
    END IF;
    CREATE UNIQUE INDEX IF NOT EXISTS ux_dashboards_one_active_per_owner
        ON dashboards (owner_id)
        WHERE status = 'ACTIVE';
END $$;
