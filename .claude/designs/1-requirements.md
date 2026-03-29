# Requirements: 1 — Admin Login Page

**Design**: designs/1.png
**Spec**: designs/1.md
**Analyzed**: 29/03/2026 at 12:00
**URL**: /admin/login
**Access**: PUBLIC (unauthenticated only)
**Primary action**: Authenticate admin/editor user with email + password

---

## Design System Notes
File: designs/DESIGN.md

Tokens used on this page:
  Background: surface (#0d1322)
  Card surface: surface_container (#191f2f) — login card
  Input bg: surface_container_lowest (#080e1d)
  Primary button: gradient primary → primary_container (45deg)
  Primary text: on_surface (#e8e8f0)
  Secondary text: on_surface_variant (#dbc2ad)
  Labels: font-label (Space Grotesk) — uppercase tracking-widest
  Logo text: font-display (Manrope) — primary_container (#ff9900)
  Body text: font-body (Inter)

---

## Sections

| # | Section | Type | Data Source |
|---|---------|------|-------------|
| 1 | Logo + Branding | Static | Hardcoded |
| 2 | Login Form | Interactive | User input → POST /api/auth/login |
| 3 | Security Notice | Static | Hardcoded text |
| 4 | Footer | Static | Hardcoded |

---

## Database Requirements

| Field | Shown In | Table | Column | Type | Required | Indexed |
|-------|----------|-------|--------|------|----------|---------|
| Email | Form input | admin_users | email | VARCHAR(150) | Yes | Yes (UNIQUE) |
| Password | Form input | admin_users | password | VARCHAR(255) | Yes | No |
| Role | Top bar badge (after login) | admin_users | role | VARCHAR(50) | Yes | Yes |
| Is Active | Login validation | admin_users | is_active | BOOLEAN | Yes | No |
| Last Login | Updated on success | admin_users | last_login | TIMESTAMPTZ | No | No |
| Full Name | Post-login display | admin_users | full_name | VARCHAR(150) | Yes | No |

---

## New Tables Required

```sql
-- V2__create_auth_tables.sql

CREATE TABLE admin_users (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name    VARCHAR(150) NOT NULL,
    email        VARCHAR(150) NOT NULL UNIQUE,
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

CREATE UNIQUE INDEX idx_admin_users_email ON admin_users(email) WHERE deleted_at IS NULL;
CREATE INDEX idx_admin_users_role ON admin_users(role);
CREATE INDEX idx_admin_users_active ON admin_users(deleted_at) WHERE deleted_at IS NULL;

-- Seed default admin (password set via env var in app, hashed by BCrypt)
-- Seed handled in application startup, not in migration
```

---

## API Endpoints

| Method | Path | Auth | Description | Cached | Rate Limited |
|--------|------|------|-------------|--------|--------------|
| POST | /api/auth/login | None | Authenticate user, return JWT | No | Yes — 10 req/15min/IP |
| POST | /api/auth/refresh | None (refresh token) | Refresh access token | No | No |
| POST | /api/auth/logout | JWT required | Blacklist current JWT | No | No |

### POST /api/auth/login
```
Request:
  { email: string, password: string, remember_device: boolean }

Response (success):
  {
    success: true,
    data: {
      access_token: string,
      refresh_token: string,
      token_type: "Bearer",
      expires_in: 3600,
      user: { id, full_name, email, role, avatar_url }
    }
  }

Response (failure):
  { success: false, error: { code: "INVALID_CREDENTIALS", message: "Invalid email or password" } }
  { success: false, error: { code: "ACCOUNT_LOCKED", message: "Access temporarily restricted" } }
```

---

## Business Rules

1. Max 5 failed login attempts per IP per 15 minutes — on 6th: block IP for 1 hour
2. All login attempts (success + failure) logged with IP, timestamp, email
3. JWT access token TTL: 1 hour
4. JWT refresh token TTL: 7 days (30 days if "Remember this device" checked)
5. Blacklisted JWTs stored in Redis on logout
6. Password never returned in any API response
7. If user is already authenticated (valid JWT), redirect to /admin/dashboard
8. BCrypt for password hashing — never plain text
9. Email field is case-insensitive for lookup

---

## Clarifications

None — design was clear.

---

## Out of Scope

- Social login (Google, GitHub)
- Two-factor authentication (2FA)
- Registration page (admins created by other admins only)
- CAPTCHA
- Password strength meter
- Forgot Password (link present but non-functional in MVP)
