# Spec: Dashboard Module

**Module**: dashboard
**Generated**: 29/03/2026 at 14:00
**Source pages**: 2-requirements.md
**Platforms**: Backend ✅ | Frontend ✅ | Mobile ⛔

---

## Overview

The Dashboard module provides a real-time system overview for the admin panel. It displays user counts via stat cards, recent access logs, node performance metrics (CPU/Memory/Storage), and security status. EDITOR users see masked usernames in access logs. All four panels load in parallel and can be refreshed simultaneously.

---

## Database Schema

### Table: access_logs
| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | UUID | PK, default gen_random_uuid() | |
| user_id | UUID | FK → admin_users(id) | Nullable for guest/system events |
| user_name | VARCHAR(150) | | Denormalized for display |
| action | VARCHAR(255) | NOT NULL | e.g. "Created Blog Post" |
| location_path | VARCHAR(500) | | URL path |
| ip_address | VARCHAR(45) | NOT NULL | IPv4/IPv6 |
| geo_location | VARCHAR(200) | | e.g. "Dubai, UAE" |
| city | VARCHAR(100) | | |
| country | VARCHAR(100) | | |
| continent | VARCHAR(50) | | |
| event_type | VARCHAR(50) | NOT NULL, DEFAULT 'LOGIN', CHECK IN ('LOGIN', 'CONTENT', 'SECURITY_ALERT') | |
| browser | VARCHAR(100) | | |
| os | VARCHAR(100) | | |
| browser_version | VARCHAR(50) | | |
| resource_type | VARCHAR(50) | | e.g. 'BLOG', 'CASE_STUDY' |
| resource_id | UUID | | ID of affected resource |
| status | VARCHAR(50) | DEFAULT 'AUTHENTICATED', CHECK IN ('AUTHENTICATED', 'GUEST') | |
| created_at | TIMESTAMPTZ | NOT NULL DEFAULT now() | No updated_at — append-only |

### Table: node_performance
| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | UUID | PK, default gen_random_uuid() | |
| cpu_usage | INT | NOT NULL, DEFAULT 0 | Percentage 0-100 |
| memory_load | INT | NOT NULL, DEFAULT 0 | Percentage 0-100 |
| storage_usage | INT | NOT NULL, DEFAULT 0 | Percentage 0-100 |
| updated_at | TIMESTAMPTZ | NOT NULL DEFAULT now() | Single row, updated periodically |

### Table: security_status
| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | UUID | PK, default gen_random_uuid() | |
| condition | VARCHAR(20) | NOT NULL, DEFAULT 'GREEN', CHECK IN ('GREEN', 'YELLOW', 'RED') | |
| status_text | TEXT | | Human-readable status message |
| last_audit_at | TIMESTAMPTZ | | |
| link_established | BOOLEAN | NOT NULL, DEFAULT true | |
| updated_at | TIMESTAMPTZ | NOT NULL DEFAULT now() | Single row |

**Flyway file**: V5__create_dashboard_tables.sql

---

## API Endpoints

### Admin Endpoints (JWT required — any role)
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | /api/admin/dashboard/summary | Any authenticated | User counts (total, admins, editors) |
| GET | /api/admin/dashboard/access-logs | Any authenticated | Recent access log entries |
| GET | /api/admin/dashboard/node-performance | Any authenticated | CPU/Memory/Storage metrics |
| GET | /api/admin/dashboard/security-status | Any authenticated | Security condition + status text |

---

## Acceptance Criteria

### Backend Criteria
- [ ] [AC-1] GET /summary returns total_users, active_admins, active_editors counts
- [ ] [AC-2] GET /access-logs returns recent log entries ordered by created_at DESC
- [ ] [AC-3] GET /access-logs supports ?limit= query param (default 10)
- [ ] [AC-4] EDITOR role receives masked user_name ("User ***") in access logs
- [ ] [AC-5] ADMIN role receives full user_name in access logs
- [ ] [AC-6] GET /node-performance returns single-row CPU/Memory/Storage values
- [ ] [AC-7] GET /security-status returns condition + status_text + last_audit_at
- [ ] [AC-8] All 4 endpoints require valid JWT — no specific role check
- [ ] [AC-9] Login events automatically create access_log entries
- [ ] [AC-10] Seed data: initial node_performance and security_status rows in migration

### Frontend Criteria
- [ ] [AC-11] All 4 API calls fire in parallel on page load
- [ ] [AC-12] "Refresh Data" button re-calls all 4 endpoints simultaneously
- [ ] [AC-13] Each panel shows independent loading skeleton state
- [ ] [AC-14] If one panel API fails, error state shown only for that panel — others still load
- [ ] [AC-15] Node Performance colors: < 70% primary, 70-85% amber, > 85% red
- [ ] [AC-16] Security panel border turns red when condition = RED
- [ ] [AC-17] EDITOR sees masked usernames in access logs
- [ ] [AC-18] All interactive elements have data-testid attributes
- [ ] [AC-19] Empty state shown when access logs are empty

### Security Criteria
- [ ] [AC-S1] Unauthenticated request to dashboard endpoints → 401
- [ ] [AC-S2] Invalid or expired token → 401

---

## Business Rules

1. All 4 dashboard API calls happen in parallel on page load
2. "Refresh Data" re-calls all 4 endpoints simultaneously
3. EDITOR sees masked user names in access logs ("User ***")
4. If any single API fails, that panel shows error state — others still load
5. Node Performance colors: < 70% primary, 70-85% amber, > 85% red
6. Security panel border turns red when condition = RED
7. JWT must be valid — expired JWT → redirect to login
8. Global command search is non-functional placeholder in MVP
9. Notification bell has no dropdown in MVP

---

## Edge Cases

1. No access logs exist → empty state with message
2. Node performance row missing → show "No data" state
3. Security status row missing → show default GREEN state
4. Token expires while viewing dashboard → redirect to login
5. All 4 APIs fail simultaneously → all panels show error state

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
