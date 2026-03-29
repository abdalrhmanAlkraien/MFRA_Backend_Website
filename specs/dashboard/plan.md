# Plan: Dashboard Module

**Module**: dashboard
**Generated**: 29/03/2026 at 14:00

---

## Implementation Approach

### Package Structure
```
com.mfra.website/
└── module/
    └── dashboard/
        ├── entity/
        │   ├── AccessLogEntity.java
        │   ├── NodePerformanceEntity.java
        │   └── SecurityStatusEntity.java
        ├── repository/
        │   ├── AccessLogRepository.java
        │   ├── NodePerformanceRepository.java
        │   └── SecurityStatusRepository.java
        ├── service/
        │   ├── DashboardService.java
        │   └── AccessLogService.java
        ├── controller/
        │   └── DashboardAdminController.java
        └── dto/
            ├── DashboardSummaryResponse.java
            ├── AccessLogResponse.java
            ├── NodePerformanceResponse.java
            └── SecurityStatusResponse.java
```

### Layer Decisions
- **Access logs**: Append-only table — no updated_at, no soft delete (audit trail)
- **Node performance**: Single-row table, updated periodically by monitoring
- **Security status**: Single-row table, updated by security checks
- **EDITOR masking**: Service layer masks user_name based on caller role from SecurityContext
- **Logging**: AuthService creates access_log entries on login/logout events
- **Auth**: All endpoints use @PreAuthorize("isAuthenticated()") — any admin role

### Key Service Methods
- `getSummary()` → count admin_users by role + active status
- `getAccessLogs(limit, role)` → fetch recent logs, mask usernames if EDITOR
- `getNodePerformance()` → fetch single row
- `getSecurityStatus()` → fetch single row
- `logAccess(userId, action, ip, eventType, ...)` → insert access_log entry

### Frontend Approach
- **State**: RTK Query for all 4 dashboard endpoints
- **Parallel loading**: All 4 queries triggered on mount with skip: false
- **Error isolation**: Each query independent — one failure doesn't affect others
- **Refresh**: Invalidate all 4 cache tags on "Refresh Data" click
- **Skeleton**: Custom skeleton components per panel type

---

## Cache Keys

No Redis caching for dashboard — all data is real-time admin-only.

---

## Dependencies

- Depends on: auth module (JWT, SecurityContext, AdminUserEntity)
- Required config: SecurityConfig (JWT filter)
