-- ANY TIME (optional cleanup). PostgreSQL only. Idempotent.
-- The refresh-token mechanism was replaced by the HttpOnly browser session cookie
-- (table browser_sessions, created by Hibernate). refresh_tokens is no longer read or written;
-- dropping it only discards old hashed tokens. Every user signs in again after the upgrade.
DROP TABLE IF EXISTS refresh_tokens;
