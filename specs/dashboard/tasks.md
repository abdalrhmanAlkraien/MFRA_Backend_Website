# Tasks: Dashboard Module

**Module**: dashboard
**Generated**: 29/03/2026 at 14:00
**Total tasks**: 2

---

## Task 2.1 — Backend: Dashboard — Entities + APIs

**Platform**: Backend
**Complexity**: Medium
**Estimated cost**: $0.80
**Dependencies**: Task 1.3 (Security + JWT)

**Delivers**:
- Flyway migration: V5__create_dashboard_tables.sql (access_logs, node_performance, security_status)
- AccessLogEntity.java (no BaseEntity — append-only, no soft delete)
- NodePerformanceEntity.java
- SecurityStatusEntity.java
- AccessLogRepository.java
- NodePerformanceRepository.java
- SecurityStatusRepository.java
- DashboardService.java (summary, access logs with EDITOR masking)
- AccessLogService.java (log access events, used by AuthService)
- DashboardAdminController.java (4 endpoints)
- DTOs: DashboardSummaryResponse, AccessLogResponse, NodePerformanceResponse, SecurityStatusResponse
- Seed initial node_performance + security_status rows

**API endpoints covered**:
- GET /api/admin/dashboard/summary
- GET /api/admin/dashboard/access-logs
- GET /api/admin/dashboard/node-performance
- GET /api/admin/dashboard/security-status

**Acceptance criteria covered**:
- [AC-1] through [AC-10], [AC-S1], [AC-S2]

**Test scenarios**:
- Backend: 8 scenarios (summary counts, access logs pagination, EDITOR masking, node performance, security status, unauthorized access, seed data verification, login event logging)

---

## Task 2.2 — Frontend: Dashboard — Page + Components

**Platform**: Frontend
**Complexity**: Medium
**Estimated cost**: $0.80
**Dependencies**: Task 1.6 (Admin Layout Shell), Task 2.1 (Dashboard Backend)

**Delivers**:
- features/dashboard/dashboardApi.ts (4 RTK Query endpoints)
- features/dashboard/types.ts
- pages/admin/DashboardPage.tsx (page 2 design)
- components/dashboard/StatCard.tsx
- components/dashboard/AccessLogList.tsx
- components/dashboard/NodePerformancePanel.tsx
- components/dashboard/SecurityStatusPanel.tsx
- Skeleton loading components per panel
- Error state per panel (independent)
- Refresh Data button functionality

**Acceptance criteria covered**:
- [AC-11] through [AC-19]

**Test scenarios**:
- Frontend: 8 scenarios (render all panels, parallel loading, refresh data, EDITOR masking, error isolation, empty state, skeleton loading, data-testid attributes)
