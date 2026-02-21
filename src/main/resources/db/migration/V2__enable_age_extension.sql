-- Enable the Apache AGE extension for graph queries.
-- Requires AGE to be installed on the PostgreSQL server.
-- In environments without AGE, this migration is skipped via the
-- age.enabled application property (see AgeFlywayCallback).
--
-- To install AGE on PostgreSQL 16:
--   apt install postgresql-16-age  (Debian/Ubuntu)
--   CREATE EXTENSION IF NOT EXISTS age;
--   LOAD 'age';
--   SET search_path = ag_catalog, "$user", public;

-- This migration intentionally left as a placeholder.
-- The actual extension creation is handled conditionally at startup
-- to avoid failures in environments where AGE is not installed.
SELECT 1;
