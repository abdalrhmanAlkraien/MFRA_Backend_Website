# Task 2.1 — Dashboard Backend

**Status**: ✅ Completed
**Platform**: Backend
**Date**: 30/03/2026
**Actual cost**: $0.60

---

## What Was Built

### Migration
- **V3__create_dashboard_tables.sql** — 3 tables: access_logs, node_performance, security_status
  - Seeded initial node_performance record (CPU 64%, Memory 82%, Storage 34%)
  - Seeded initial security_status record (GREEN, no breaches)

### Entities
- **AccessLogEntity** — Logs user actions with IP, location, timestamps. FK to admin_users
- **NodePerformanceEntity** — CPU usage, memory load, storage percentages with recorded_at
- **SecurityStatusEntity** — Condition level (GREEN/YELLOW/RED), message, breach count, secure link flag
- **ConditionLevel** — Enum: GREEN, YELLOW, RED

### Repositories
- **AccessLogRepository** — Paginated list, ordered by created_at DESC
- **NodePerformanceRepository** — Latest record by recorded_at
- **SecurityStatusRepository** — Latest record by created_at

### Service
- **DashboardService** — 4 methods:
  - `getSummary()` — counts from admin_users (total, admins, editors)
  - `getAccessLogs(page, size)` — paginated access logs
  - `getNodePerformance()` — latest performance metrics
  - `getSecurityStatus()` — latest security condition

### Controller
- **DashboardAdminController** — 4 GET endpoints under `/api/admin/dashboard`
  - All endpoints require `ADMIN` or `EDITOR` role via `@PreAuthorize`

### DTOs
- DashboardSummaryResponse, AccessLogResponse, NodePerformanceResponse, SecurityStatusResponse

### AdminUserRepository Updated
- Added `countByDeletedAtIsNullAndIsActiveTrue()`
- Added `countByRoleAndDeletedAtIsNullAndIsActiveTrue(AdminRole role)`

---

## Endpoints

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | /api/admin/dashboard/summary | ADMIN, EDITOR | User counts |
| GET | /api/admin/dashboard/access-logs | ADMIN, EDITOR | Paginated logs |
| GET | /api/admin/dashboard/node-performance | ADMIN, EDITOR | Latest metrics |
| GET | /api/admin/dashboard/security-status | ADMIN, EDITOR | Security condition |

---

## Verification Results

```
Backend:
  mvn clean compile  — 0 errors
  Flyway V3          — applied successfully
  Health endpoint    — UP
  Summary endpoint   — 200 OK (totalUsers: 1, activeAdmins: 1)
  Access logs        — 200 OK (empty page)
  Node performance   — 200 OK (cpu: 64%, memory: 82%, storage: 34%)
  Security status    — 200 OK (GREEN, no breaches)
  No auth test       — 403 Forbidden
```

---

## Files Created/Modified

```
NEW:
  backend/src/main/resources/db/migration/V3__create_dashboard_tables.sql
  backend/src/main/java/com/mfra/website/module/dashboard/entity/AccessLogEntity.java
  backend/src/main/java/com/mfra/website/module/dashboard/entity/NodePerformanceEntity.java
  backend/src/main/java/com/mfra/website/module/dashboard/entity/SecurityStatusEntity.java
  backend/src/main/java/com/mfra/website/module/dashboard/entity/ConditionLevel.java
  backend/src/main/java/com/mfra/website/module/dashboard/repository/AccessLogRepository.java
  backend/src/main/java/com/mfra/website/module/dashboard/repository/NodePerformanceRepository.java
  backend/src/main/java/com/mfra/website/module/dashboard/repository/SecurityStatusRepository.java
  backend/src/main/java/com/mfra/website/module/dashboard/dto/DashboardSummaryResponse.java
  backend/src/main/java/com/mfra/website/module/dashboard/dto/AccessLogResponse.java
  backend/src/main/java/com/mfra/website/module/dashboard/dto/NodePerformanceResponse.java
  backend/src/main/java/com/mfra/website/module/dashboard/dto/SecurityStatusResponse.java
  backend/src/main/java/com/mfra/website/module/dashboard/service/DashboardService.java
  backend/src/main/java/com/mfra/website/module/dashboard/controller/DashboardAdminController.java

MODIFIED:
  backend/src/main/java/com/mfra/website/module/auth/repository/AdminUserRepository.java
```

---

## Next Task

Task 2.2: Dashboard Frontend
