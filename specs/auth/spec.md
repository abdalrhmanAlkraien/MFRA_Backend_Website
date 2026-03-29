# Spec: Auth Module

**Module**: auth
**Generated**: 29/03/2026 at 14:00
**Source pages**: 1-requirements.md
**Platforms**: Backend ✅ | Frontend ✅ | Mobile ⛔

---

## Overview

The Auth module handles administrator and editor authentication for the MFRA admin panel. It provides JWT-based stateless authentication with login, token refresh, and logout endpoints. Access tokens expire after 1 hour, refresh tokens after 7 days (or 30 days with "Remember this device"), and blacklisted tokens are stored in Redis until expiry.

---

## Database Schema

### Table: admin_users
| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | UUID | PK, default gen_random_uuid() | |
| full_name | VARCHAR(150) | NOT NULL | |
| email | VARCHAR(150) | NOT NULL, UNIQUE (partial where deleted_at IS NULL) | Case-insensitive lookup |
| password | VARCHAR(255) | NOT NULL | BCrypt hashed, never in response |
| role | VARCHAR(50) | NOT NULL, DEFAULT 'EDITOR', CHECK IN ('ADMIN', 'EDITOR') | |
| is_active | BOOLEAN | NOT NULL, DEFAULT true | |
| avatar_url | VARCHAR(500) | | Optional profile image |
| last_login | TIMESTAMPTZ | | Updated on each successful login |
| created_at | TIMESTAMPTZ | NOT NULL DEFAULT now() | |
| updated_at | TIMESTAMPTZ | NOT NULL DEFAULT now() | |
| created_by | UUID | | |
| updated_by | UUID | | |
| deleted_at | TIMESTAMPTZ | | Soft delete |

**Flyway file**: V2__create_auth_tables.sql

---

## API Endpoints

### Auth Endpoints (no JWT required)
| Method | Path | Description | Rate Limited |
|--------|------|-------------|--------------|
| POST | /api/auth/login | Authenticate with email + password | Yes — 10/15min/IP |
| POST | /api/auth/refresh | Refresh access token using refresh token | No |
| POST | /api/auth/logout | Blacklist current JWT in Redis | No (JWT required) |

---

## Acceptance Criteria

### Backend Criteria
- [ ] [AC-1] POST /api/auth/login returns access_token + refresh_token + user info on valid credentials
- [ ] [AC-2] Login response includes: access_token, refresh_token, token_type, expires_in, user { id, full_name, email, role, avatar_url }
- [ ] [AC-3] Invalid email or password returns 401 with code INVALID_CREDENTIALS
- [ ] [AC-4] Inactive user (is_active=false) cannot login — returns 401
- [ ] [AC-5] Max 5 failed login attempts per IP per 15 minutes — 6th returns ACCOUNT_LOCKED, block for 1 hour
- [ ] [AC-6] All login attempts (success + failure) logged with IP, timestamp, email
- [ ] [AC-7] last_login updated on successful login
- [ ] [AC-8] Email lookup is case-insensitive
- [ ] [AC-9] Password never returned in any API response
- [ ] [AC-10] JWT access token TTL: 1 hour
- [ ] [AC-11] JWT refresh token TTL: 7 days (30 days if remember_device=true)
- [ ] [AC-12] POST /api/auth/refresh returns new access_token given valid refresh_token
- [ ] [AC-13] POST /api/auth/logout blacklists JWT in Redis until expiry
- [ ] [AC-14] Expired or blacklisted JWT returns 401 on admin endpoints
- [ ] [AC-15] Soft delete only on admin_users — deleted_at set, record not removed
- [ ] [AC-16] Default admin user seeded on application startup (from env var)
- [ ] [AC-17] BCrypt used for password hashing

### Frontend Criteria
- [ ] [AC-18] Login page renders with email + password fields + "Remember this device" checkbox
- [ ] [AC-19] Form validation: email format, password required
- [ ] [AC-20] Loading state shown during login API call
- [ ] [AC-21] Error toast shown on failed login with message from API
- [ ] [AC-22] Successful login redirects to /admin/dashboard
- [ ] [AC-23] Already authenticated user visiting /admin/login redirects to /admin/dashboard
- [ ] [AC-24] AuthGuard wraps all /admin/** routes (except /admin/login)
- [ ] [AC-25] RoleGuard prevents EDITOR from accessing ADMIN-only pages
- [ ] [AC-26] Token auto-refresh before expiry
- [ ] [AC-27] Expired token with no valid refresh → redirect to /admin/login
- [ ] [AC-28] All interactive elements have data-testid attributes

### Security Criteria
- [ ] [AC-S1] Unauthenticated request to /api/admin/** → 401
- [ ] [AC-S2] EDITOR accessing ADMIN-only endpoint → 403
- [ ] [AC-S3] Invalid or expired token → 401
- [ ] [AC-S4] Blacklisted token (post-logout) → 401

---

## Business Rules

1. Max 5 failed login attempts per IP per 15 minutes — on 6th: block IP for 1 hour
2. All login attempts logged with IP, timestamp, email (success + failure)
3. JWT access token TTL: 1 hour; refresh token TTL: 7 days (30 days with remember_device)
4. Blacklisted JWTs stored in Redis on logout
5. Password never returned in any API response
6. If user already authenticated, redirect to /admin/dashboard
7. BCrypt for password hashing — never plain text
8. Email field is case-insensitive for lookup
9. Default admin user seeded on startup from environment variable

---

## Edge Cases

1. Login with non-existent email → 401 INVALID_CREDENTIALS (same message as wrong password)
2. Login with correct email, wrong password → 401 INVALID_CREDENTIALS
3. Login with inactive account → 401 (do not reveal account exists)
4. 6th failed attempt from same IP within 15 min → 423 ACCOUNT_LOCKED
5. Refresh with expired refresh token → 401
6. Refresh with blacklisted token → 401
7. Logout with already-blacklisted token → 200 (idempotent)
8. Concurrent login from multiple devices → all valid (stateless JWT)
9. Soft-deleted user attempts login → 401

---

## Out of Scope

- Social login (Google, GitHub)
- Two-factor authentication (2FA)
- Registration page (admins created by other admins only)
- CAPTCHA
- Password strength meter
- Forgot Password flow (link present but non-functional in MVP)
