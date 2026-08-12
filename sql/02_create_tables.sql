-- Run against the aulm_db database (e.g. `psql -U aulm_user -d aulm_db -f 02_create_tables.sql`).
-- Mirrors the JPA entities in com.vassarlabs.aulm.model with spring.jpa.hibernate.ddl-auto=validate.

CREATE TABLE IF NOT EXISTS app_users (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username       VARCHAR(255) NOT NULL,
    password_hash  VARCHAR(255) NOT NULL,
    full_name      VARCHAR(255),
    email          VARCHAR(255),
    enabled        BOOLEAN      NOT NULL DEFAULT TRUE,
    admin          BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT uk_app_users_username UNIQUE (username)
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
    CONSTRAINT ck_licenses_license_type CHECK (license_type IN ('TRIAL', 'STANDARD', 'PREMIUM', 'ADMIN')),
    CONSTRAINT ck_licenses_status CHECK (status IN ('ACTIVE', 'REVOKED')),
    CONSTRAINT fk_licenses_user FOREIGN KEY (user_id) REFERENCES app_users (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS user_permissions (
    user_id     BIGINT      NOT NULL,
    permission  VARCHAR(32) NOT NULL,
    CONSTRAINT pk_user_permissions PRIMARY KEY (user_id, permission),
    CONSTRAINT ck_user_permissions_permission CHECK (permission IN ('ACCESS', 'MODIFY', 'APPROVE')),
    CONSTRAINT fk_user_permissions_user FOREIGN KEY (user_id) REFERENCES app_users (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_licenses_user_id ON licenses (user_id);
CREATE INDEX IF NOT EXISTS idx_user_permissions_user_id ON user_permissions (user_id);
