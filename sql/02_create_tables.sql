-- Run against the aulm_db database (e.g. `psql -U aulm_user -d aulm_db -f 02_create_tables.sql`).
-- Mirrors the JPA entities in com.vassarlabs.aulm.model with spring.jpa.hibernate.ddl-auto=validate.

CREATE TABLE IF NOT EXISTS projects (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    uuid        UUID         NOT NULL DEFAULT gen_random_uuid(),
    name        VARCHAR(255) NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT uk_projects_name UNIQUE (name),
    CONSTRAINT uk_projects_uuid UNIQUE (uuid)
);

CREATE TABLE IF NOT EXISTS app_users (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    uuid           UUID         NOT NULL DEFAULT gen_random_uuid(),
    username       VARCHAR(255) NOT NULL,
    password_hash  VARCHAR(255) NOT NULL,
    full_name      VARCHAR(255),
    email          VARCHAR(255) NOT NULL,
    project_name   VARCHAR(255) NOT NULL,
    enabled        BOOLEAN      NOT NULL DEFAULT TRUE,
    admin          BOOLEAN      NOT NULL DEFAULT FALSE,
    super_admin    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT uk_app_users_username_project UNIQUE (username, project_name),
    CONSTRAINT uk_app_users_uuid UNIQUE (uuid)
);

CREATE TABLE IF NOT EXISTS licenses (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    license_key   VARCHAR(255) NOT NULL,
    license_type  VARCHAR(32)  NOT NULL,
    status        VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE',
    issued_date   DATE         NOT NULL,
    expiry_date   DATE,
    user_id       BIGINT       NOT NULL,
    CONSTRAINT uk_licenses_license_key UNIQUE (license_key),
    CONSTRAINT uk_licenses_user_id UNIQUE (user_id),
    CONSTRAINT ck_licenses_license_type CHECK (license_type IN ('trial', 'standard', 'premium', 'admin', 'custom')),
    CONSTRAINT ck_licenses_status CHECK (status IN ('ACTIVE', 'REVOKED')),
    CONSTRAINT fk_licenses_user FOREIGN KEY (user_id) REFERENCES app_users (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS user_permissions (
    user_id     BIGINT      NOT NULL,
    permission  VARCHAR(32) NOT NULL,
    CONSTRAINT pk_user_permissions PRIMARY KEY (user_id, permission),
    CONSTRAINT ck_user_permissions_permission CHECK (permission IN ('access', 'modify', 'approve')),
    CONSTRAINT fk_user_permissions_user FOREIGN KEY (user_id) REFERENCES app_users (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS access_requests (
    id                     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id                BIGINT       NOT NULL,
    assigned_admin_id      BIGINT,
    request_type           VARCHAR(32)  NOT NULL,
    status                 VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
    requested_license_type VARCHAR(32),
    requested_start_date   DATE,
    requested_expiry_date  DATE,
    note                   VARCHAR(1000),
    resolution_note        VARCHAR(1000),
    created_at             TIMESTAMP    NOT NULL DEFAULT now(),
    resolved_at            TIMESTAMP,
    CONSTRAINT ck_access_requests_type CHECK (request_type IN ('REGISTRATION', 'RENEWAL', 'PERMISSION_CHANGE')),
    CONSTRAINT ck_access_requests_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    CONSTRAINT ck_access_requests_license_type CHECK (requested_license_type IS NULL OR requested_license_type IN ('trial', 'standard', 'premium', 'admin', 'custom')),
    CONSTRAINT fk_access_requests_user FOREIGN KEY (user_id) REFERENCES app_users (id) ON DELETE CASCADE,
    CONSTRAINT fk_access_requests_assigned_admin FOREIGN KEY (assigned_admin_id) REFERENCES app_users (id) ON DELETE SET NULL
);

ALTER TABLE access_requests ADD COLUMN IF NOT EXISTS requested_start_date DATE;
ALTER TABLE access_requests ADD COLUMN IF NOT EXISTS requested_expiry_date DATE;

CREATE TABLE IF NOT EXISTS access_request_permissions (
    access_request_id  BIGINT      NOT NULL,
    permission          VARCHAR(32) NOT NULL,
    CONSTRAINT pk_access_request_permissions PRIMARY KEY (access_request_id, permission),
    CONSTRAINT ck_access_request_permissions_permission CHECK (permission IN ('access', 'modify', 'approve')),
    CONSTRAINT fk_access_request_permissions_request FOREIGN KEY (access_request_id) REFERENCES access_requests (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    token       VARCHAR(255) NOT NULL,
    user_id     BIGINT       NOT NULL,
    expires_at  TIMESTAMP    NOT NULL,
    used        BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT uk_password_reset_tokens_token UNIQUE (token),
    CONSTRAINT fk_password_reset_tokens_user FOREIGN KEY (user_id) REFERENCES app_users (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_licenses_user_id ON licenses (user_id);
CREATE INDEX IF NOT EXISTS idx_user_permissions_user_id ON user_permissions (user_id);
CREATE INDEX IF NOT EXISTS idx_access_requests_user_id ON access_requests (user_id);
CREATE INDEX IF NOT EXISTS idx_access_requests_status ON access_requests (status);
CREATE INDEX IF NOT EXISTS idx_password_reset_tokens_user_id ON password_reset_tokens (user_id);

INSERT INTO projects (name) VALUES ('AULM') ON CONFLICT (name) DO NOTHING;
