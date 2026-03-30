-- V2__create_auth_tables.sql
-- Module: auth
-- Extracted from: designs/1-requirements.md

CREATE TABLE admin_users (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name    VARCHAR(150) NOT NULL,
    email        VARCHAR(150) NOT NULL,
    password     VARCHAR(255) NOT NULL,
    role         VARCHAR(50)  NOT NULL DEFAULT 'EDITOR'
                 CHECK (role IN ('ADMIN', 'EDITOR')),
    is_active    BOOLEAN      NOT NULL DEFAULT true,
    avatar_url   VARCHAR(500),
    last_login   TIMESTAMPTZ,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by   UUID,
    updated_by   UUID,
    deleted_at   TIMESTAMPTZ
);

-- Partial unique index on email for active users only
CREATE UNIQUE INDEX idx_admin_users_email ON admin_users(LOWER(email)) WHERE deleted_at IS NULL;
CREATE INDEX idx_admin_users_role ON admin_users(role);
CREATE INDEX idx_admin_users_active ON admin_users(deleted_at) WHERE deleted_at IS NULL;
