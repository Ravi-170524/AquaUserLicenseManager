-- Run as a Postgres superuser (e.g. `psql -U postgres -f 01_create_role_and_db.sql`).
-- Creates the login role and database used by AULM. Adjust the password before running
-- in any shared environment, and set the matching AULM_DB_* env vars for the backend.

CREATE ROLE aulm_user WITH LOGIN PASSWORD 'change-this-password';

CREATE DATABASE aulm_db OWNER aulm_user;

GRANT ALL PRIVILEGES ON DATABASE aulm_db TO aulm_user;
