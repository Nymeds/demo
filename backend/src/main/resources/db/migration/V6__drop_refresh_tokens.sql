-- Flyway (perfil padrao, PostgreSQL). Idempotente e protegido por to_regclass: num banco novo (Flyway roda
-- antes do Hibernate criar as tabelas) vira no-op. Nao edite depois de aplicado: crie um novo V<n>__.
-- PRE-BOOT, REQUIRED. PostgreSQL only. Idempotent. (npm run init applies it automatically.)
-- The refresh-token mechanism was replaced by the HttpOnly browser session cookie
-- (table browser_sessions, created by Hibernate). refresh_tokens is no longer read or written, but its
-- foreign key to app_users has no ON DELETE CASCADE: while the table exists, deleting an account that
-- ever signed in fails (HTTP 500). Dropping it only discards old hashed tokens; every user signs in again.
DROP TABLE IF EXISTS refresh_tokens;
