-- Flyway callback: runs after EVERY migrate (each boot of the default profile), so it is idempotent.
-- Partial unique index: at most one ACTIVE dashboard per owner (JPA cannot declare partial indexes).
-- Flyway runs BEFORE Hibernate (ddl-auto) and before ActiveDashboardCorrectionRunner, so:
--   * fresh database: dashboards does not exist yet -> no-op; the index is created on the NEXT boot;
--   * duplicates still present: skipped with a NOTICE (never fails the boot); the runner fixes them
--     during this boot and the index is created on the next one.
-- NOTE: IF NOT EXISTS checks only the index NAME, not its definition.
DO $$
BEGIN
    IF to_regclass('public.dashboards') IS NULL THEN
        RAISE NOTICE 'dashboards does not exist yet; ux_dashboards_one_active_per_owner will be created on the next boot.';
        RETURN;
    END IF;
    IF to_regclass('public.ux_dashboards_one_active_per_owner') IS NOT NULL THEN
        RETURN;
    END IF;
    IF EXISTS (SELECT 1 FROM dashboards WHERE status = 'ACTIVE'
               GROUP BY owner_id HAVING count(*) > 1) THEN
        RAISE NOTICE 'Owners with more than one ACTIVE dashboard; index deferred to the next boot.';
        RETURN;
    END IF;
    CREATE UNIQUE INDEX IF NOT EXISTS ux_dashboards_one_active_per_owner
        ON dashboards (owner_id)
        WHERE status = 'ACTIVE';
END $$;
