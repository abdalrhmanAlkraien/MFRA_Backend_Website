# Project Users

> This file defines all user types, their roles, permissions, and access rules.
> The AI agent reads this file when generating specs, setting up security,
> and writing tests. Every @PreAuthorize rule and AuthGuard decision is
> driven by what is defined here.

---

## User Types Overview

```
PUBLIC     → Any visitor — no login required
ADMIN      → MFRA internal team — full access to everything
EDITOR     → Content manager — blog and case studies only
```

---

## USER_TYPE: PUBLIC

```
Description:   Any visitor to the website — no authentication required
Auth:          None — no token needed
Session:       Stateless — no session stored
Rate limited:  Yes — POST endpoints limited by IP
```

### Public Permissions

| Resource | Actions | Notes |
|---|---|---|
| `blogs` | READ | Published only — drafts never visible |
| `blog_categories` | READ | All categories |
| `blog_tags` | READ | All tags |
| `case_studies` | READ | Published only — drafts never visible |
| `case_study_categories` | READ | All categories |
| `testimonials` | READ | Active only |
| `stats` | READ | Site-wide numbers bar |
| `tools` | READ | Active only |
| `settings` | READ | Public fields only (contact info, social links) |
| `consultation_requests` | CREATE | Submit free consultation form |
| `contact_messages` | CREATE | Submit contact form |

### Public Rate Limits

```
POST /api/public/consultation   → max 3 requests per IP per hour
POST /api/public/contact        → max 5 requests per IP per hour
```

### Public Access Rules

```
✅ Can read all published content
✅ Can submit consultation form
✅ Can submit contact form
❌ Cannot read draft or unpublished content
❌ Cannot access any /api/admin/** endpoint
❌ Cannot read consultation submissions from other users
❌ Cannot read contact messages
```

---

## USER_TYPE: ADMIN

```
Description:   MFRA internal team member — full control of everything
Auth:          JWT required — Bearer token in Authorization header
Role:          ROLE_ADMIN
Stored in:     admin_users table
```

### Admin Permissions

| Resource | Actions | Notes |
|---|---|---|
| `blogs` | CREATE, READ, UPDATE, DELETE, PUBLISH, UNPUBLISH | All blogs including drafts |
| `blog_categories` | CREATE, READ, UPDATE, DELETE | Full control |
| `blog_tags` | CREATE, READ, UPDATE, DELETE | Full control |
| `case_studies` | CREATE, READ, UPDATE, DELETE, PUBLISH, UNPUBLISH, FEATURE | All case studies |
| `case_study_categories` | CREATE, READ, UPDATE, DELETE | Full control |
| `testimonials` | CREATE, READ, UPDATE, DELETE, REORDER, TOGGLE | Full control |
| `stats` | READ, UPDATE | Update all numbers |
| `tools` | CREATE, READ, UPDATE, DELETE, REORDER, TOGGLE | Full control |
| `settings` | READ, UPDATE | All settings including private |
| `consultation_requests` | READ, UPDATE_STATUS, ADD_NOTES, DELETE | Full management |
| `contact_messages` | READ, DELETE | Full management |
| `file_uploads` | CREATE, DELETE | Upload and remove images |
| `admin_users` | CREATE, READ, UPDATE, DELETE | Manage other users |
| `dashboard` | READ | Summary counts and stats |

### Admin Access Rules

```
✅ Full access to all /api/admin/** endpoints
✅ Can read all content including drafts and unpublished
✅ Can create and manage other admin users
✅ Can manage EDITOR accounts
✅ Can change all site settings
✅ Can upload and delete files
✅ Can delete consultation requests and contact messages
✅ Can access dashboard summary
❌ No restrictions within the admin panel
```

---

## USER_TYPE: EDITOR

```
Description:   Content manager — manages blogs and case studies only
Auth:          JWT required — Bearer token in Authorization header
Role:          ROLE_EDITOR
Stored in:     admin_users table
```

### Editor Permissions

| Resource | Actions | Notes |
|---|---|---|
| `blogs` | CREATE, READ, UPDATE, PUBLISH, UNPUBLISH | Cannot delete |
| `blog_categories` | READ | View only |
| `blog_tags` | READ | View only |
| `case_studies` | CREATE, READ, UPDATE, PUBLISH, UNPUBLISH | Cannot delete |
| `case_study_categories` | READ | View only |
| `testimonials` | READ | View only — cannot manage |
| `stats` | READ | View only — cannot update |
| `tools` | READ | View only — cannot manage |
| `settings` | READ | View only — cannot update |
| `consultation_requests` | READ | View only — cannot update status or delete |
| `contact_messages` | READ | View only — cannot delete |
| `file_uploads` | CREATE | Can upload images — cannot delete |
| `admin_users` | ❌ NO ACCESS | Cannot view or manage users |
| `dashboard` | READ | Summary counts only |

### Editor Access Rules

```
✅ Can create and publish blogs and case studies
✅ Can upload images for content
✅ Can read consultation requests and contact messages (view only)
✅ Can access dashboard
❌ Cannot delete any content
❌ Cannot manage testimonials, tools, or settings
❌ Cannot update consultation request status
❌ Cannot manage admin users
❌ Cannot change site settings
❌ Cannot delete uploaded files
```

---

## Permission Comparison Table

| Resource | PUBLIC | EDITOR | ADMIN |
|---|---|---|---|
| Blogs (published) | READ | READ + WRITE | FULL |
| Blogs (drafts) | ❌ | READ + WRITE | FULL |
| Blog categories | READ | READ | FULL |
| Case studies (published) | READ | READ + WRITE | FULL |
| Case studies (drafts) | ❌ | READ + WRITE | FULL |
| Case study categories | READ | READ | FULL |
| Testimonials | READ (active) | READ | FULL |
| Stats | READ | READ | READ + UPDATE |
| Tools | READ (active) | READ | FULL |
| Settings | READ (public) | READ | FULL |
| Consultation requests | CREATE | READ | FULL |
| Contact messages | CREATE | READ | FULL |
| File uploads | ❌ | CREATE | FULL |
| Admin users | ❌ | ❌ | FULL |
| Dashboard | ❌ | READ | READ |

---

## Backend Security Rules

### @PreAuthorize Mapping

```java
// Public endpoints — no annotation needed (open by default)
// All under /api/public/**

// Admin-only endpoints
@PreAuthorize("hasRole('ADMIN')")
// Use for: delete, settings update, stats update, user management, file delete

// Admin or Editor
@PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
// Use for: create blog, update blog, publish blog, create case study,
//          upload image, view consultation requests, view dashboard

// Authenticated only (any role)
@PreAuthorize("isAuthenticated()")
// Use for: dashboard summary count (any admin panel user)
```

### URL Security Mapping

```
/api/public/**          → permitAll()       — no token required
/api/auth/**            → permitAll()       — login endpoint
/api/admin/**           → authenticated()   — JWT required
/actuator/health        → permitAll()
/swagger-ui/**          → permitAll()
/v3/api-docs/**         → permitAll()
```

---

## Frontend Auth Guard Mapping

```typescript
// Public pages — no guard
/                        → PublicLayout (no guard)
/about                   → PublicLayout (no guard)
/services/**             → PublicLayout (no guard)
/case-studies/**         → PublicLayout (no guard)
/blog/**                 → PublicLayout (no guard)
/client-tools            → PublicLayout (no guard)
/free-consultation       → PublicLayout (no guard)
/contact                 → PublicLayout (no guard)

// Admin pages — AuthGuard required
/admin/login             → No guard (login page itself)
/admin/**                → AuthGuard (JWT must be valid)
```

### AuthGuard Role Checks

```typescript
// Any authenticated admin user
/admin/dashboard         → isAuthenticated
/admin/blogs/**          → isAuthenticated (ADMIN or EDITOR)
/admin/case-studies/**   → isAuthenticated (ADMIN or EDITOR)
/admin/consultations     → isAuthenticated (ADMIN or EDITOR)
/admin/messages          → isAuthenticated (ADMIN or EDITOR)

// Admin only — redirect EDITOR to dashboard
/admin/testimonials      → ROLE_ADMIN only
/admin/stats             → ROLE_ADMIN only
/admin/tools             → ROLE_ADMIN only
/admin/settings          → ROLE_ADMIN only
/admin/users             → ROLE_ADMIN only
```

---

## Test Scenarios Required Per User Type

Every protected endpoint must have these test scenarios:

```
PUBLIC user accessing admin endpoint:
  → 401 Unauthorized (no token)

EDITOR accessing ADMIN-only endpoint:
  → 403 Forbidden (wrong role)

ADMIN accessing any admin endpoint:
  → 200 / 201 (full access)

PUBLIC accessing public endpoint:
  → 200 (no token needed)

EDITOR accessing shared content endpoint:
  → 200 (can access)

Invalid or expired token:
  → 401 Unauthorized
```

---

## Admin User Entity

```
Table:         admin_users
Fields:
  id           UUID (primary key)
  full_name    VARCHAR(150)
  email        VARCHAR(150) UNIQUE
  password     VARCHAR(255) — BCrypt hashed, never plain text
  role         VARCHAR(50) — ADMIN or EDITOR
  is_active    BOOLEAN DEFAULT true
  last_login   TIMESTAMPTZ
  created_at   TIMESTAMPTZ
  updated_at   TIMESTAMPTZ
  deleted_at   TIMESTAMPTZ — soft delete

Rules:
  - Password never returned in any API response
  - Email must be unique across all admin users
  - Soft delete only — never hard delete
  - last_login updated on every successful login
```

---

## Seed Data

```
Default admin user (created on first setup):
  email:    admin@mfra.com
  password: set via environment variable ADMIN_DEFAULT_PASSWORD
  role:     ADMIN
  active:   true

Note: Default password must be changed on first login.
      Never commit credentials to version control.
```