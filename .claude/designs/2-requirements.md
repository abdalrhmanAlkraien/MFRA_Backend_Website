# Requirements: 2 — Admin Dashboard

**Design**: designs/2.png
**Spec**: designs/2.md
**Analyzed**: 29/03/2026 at 12:00
**URL**: /admin/dashboard
**Access**: ADMIN (full), EDITOR (restricted — masked usernames in logs)
**Primary action**: View real-time system overview after login

---

## Design System Notes
File: designs/DESIGN.md

Tokens used on this page:
  Background: surface (#0d1322)
  Sidebar: surface_container (#191f2f)
  Stat cards: surface_container_high (#242a3a)
  Active nav: surface_container_highest (#2f3445) + primary text
  Log panel: surface_container (#191f2f)
  Security panel border (RED state): error (#f87171)
  Progress bars: primary_container (#ff9900), tertiary_container (#38bdf8)
  Role badge: surface_container_highest bg, primary_container text (ADMIN) / tertiary (EDITOR)

---

## Sections

| # | Section | Type | Data Source |
|---|---------|------|-------------|
| 1 | Admin Layout Shell (sidebar + top bar) | Interactive/Static | Shared layout — reusable |
| 2 | Page Header + Refresh Button | Interactive | Static + refetch trigger |
| 3 | 3 Stat Cards | Dynamic | GET /api/admin/dashboard/summary |
| 4 | Recent Access Logs | Dynamic | GET /api/admin/dashboard/access-logs |
| 5 | Node Performance | Dynamic | GET /api/admin/dashboard/node-performance |
| 6 | Security Status | Dynamic | GET /api/admin/dashboard/security-status |
| 7 | Security Notice Footer | Static | Hardcoded text + dynamic timestamp |

---

## Database Requirements

| Field | Shown In | Table | Column | Type | Required | Indexed |
|-------|----------|-------|--------|------|----------|---------|
| Total users | Stat card 1 | admin_users | (count query) | — | — | — |
| Active admins | Stat card 2 | admin_users | (count where role=ADMIN, is_active=true) | — | — | — |
| Active editors | Stat card 3 | admin_users | (count where role=EDITOR, is_active=true) | — | — | — |
| User name | Access log | access_logs | user_name | VARCHAR(150) | No | No |
| Action | Access log | access_logs | action | VARCHAR(255) | Yes | No |
| IP address | Access log | access_logs | ip_address | VARCHAR(45) | Yes | Yes |
| Geo location | Access log | access_logs | geo_location | VARCHAR(200) | No | No |
| Event type | Access log | access_logs | event_type | VARCHAR(50) | Yes | Yes |
| CPU usage | Node perf | node_performance | cpu_usage | INT | Yes | No |
| Memory load | Node perf | node_performance | memory_load | INT | Yes | No |
| Storage | Node perf | node_performance | storage_usage | INT | Yes | No |
| Condition | Security | security_status | condition | VARCHAR(20) | Yes | No |

---

## New Tables Required

```sql
-- V5__create_dashboard_tables.sql

-- Access logs — records all admin and security events
CREATE TABLE access_logs (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID         REFERENCES admin_users(id),
    user_name      VARCHAR(150),
    action         VARCHAR(255) NOT NULL,
    location_path  VARCHAR(500),
    ip_address     VARCHAR(45)  NOT NULL,
    geo_location   VARCHAR(200),
    city           VARCHAR(100),
    country        VARCHAR(100),
    continent      VARCHAR(50),
    event_type     VARCHAR(50)  NOT NULL DEFAULT 'LOGIN'
                   CHECK (event_type IN ('LOGIN', 'CONTENT', 'SECURITY_ALERT')),
    browser        VARCHAR(100),
    os             VARCHAR(100),
    browser_version VARCHAR(50),
    resource_type  VARCHAR(50),
    resource_id    UUID,
    status         VARCHAR(50) DEFAULT 'AUTHENTICATED'
                   CHECK (status IN ('AUTHENTICATED', 'GUEST')),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_access_logs_event_type ON access_logs(event_type);
CREATE INDEX idx_access_logs_created_at ON access_logs(created_at DESC);
CREATE INDEX idx_access_logs_ip ON access_logs(ip_address);
CREATE INDEX idx_access_logs_user ON access_logs(user_id);
CREATE INDEX idx_access_logs_resource ON access_logs(resource_type, resource_id);

-- Node performance — single-row updated by monitoring
CREATE TABLE node_performance (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    cpu_usage      INT          NOT NULL DEFAULT 0,
    memory_load    INT          NOT NULL DEFAULT 0,
    storage_usage  INT          NOT NULL DEFAULT 0,
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Security status — single-row
CREATE TABLE security_status (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    condition        VARCHAR(20)  NOT NULL DEFAULT 'GREEN'
                     CHECK (condition IN ('GREEN', 'YELLOW', 'RED')),
    status_text      TEXT,
    last_audit_at    TIMESTAMPTZ,
    link_established BOOLEAN      NOT NULL DEFAULT true,
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Seed initial rows
INSERT INTO node_performance (cpu_usage, memory_load, storage_usage) VALUES (64, 82, 34);
INSERT INTO security_status (condition, status_text, last_audit_at, link_established)
    VALUES ('GREEN', 'All administrative systems are currently operating under CONDITION GREEN. No active breaches detected.', now(), true);
```

---

## API Endpoints

| Method | Path | Auth | Description | Cached | Rate Limited |
|--------|------|------|-------------|--------|--------------|
| GET | /api/admin/dashboard/summary | JWT (any role) | Stat card counts | No | No |
| GET | /api/admin/dashboard/access-logs | JWT (any role) | Recent access logs | No | No |
| GET | /api/admin/dashboard/node-performance | JWT (any role) | CPU/Memory/Storage | No | No |
| GET | /api/admin/dashboard/security-status | JWT (any role) | Security condition | No | No |

### GET /api/admin/dashboard/summary
```
Response:
  {
    total_users: number,
    active_admins: number,
    active_editors: number,
    users_trend: "+12%",
    admins_status: "System Peak",
    editors_status: "Stable"
  }
```

### GET /api/admin/dashboard/access-logs?limit=10
```
Response: array of {
  id, user_name, action, location_path,
  ip_address, geo_location, event_type, created_at
}
Note: EDITOR role sees masked user_name ("User ***")
```

---

## Business Rules

1. All 4 API calls happen in parallel on page load
2. "Refresh Data" re-calls all 4 endpoints simultaneously
3. EDITOR sees masked user names in access logs ("User ***")
4. If any single API fails, that panel shows error state — others still load
5. Node Performance colors: < 70% primary, 70-85% amber, > 85% red
6. Security panel border turns red when condition = RED
7. JWT must be valid — expired JWT → redirect to login
8. Global command search is non-functional placeholder in MVP
9. Notification bell has no dropdown in MVP
10. "View All Logs" link is non-functional in MVP

---

## Admin Layout Shell (shared component)

This page defines the AdminLayout component reused by all admin pages:
- Fixed left sidebar (240px desktop / 64px tablet / drawer mobile)
- Fixed top bar with role badge + search + icons
- Nav items: Dashboard, Blogs, Case Studies, Global Stats, Testimonials, Users, Settings
- Support + Logout at bottom of sidebar
- User profile panel at very bottom

---

## Clarifications

None — design was clear.

---

## Out of Scope

- Global command search functionality
- Notification system
- Real-time WebSocket updates
- "View All Logs" page
- Clicking individual log entries
- Date range filtering on logs
- Historical charts
- Dark/light mode toggle
