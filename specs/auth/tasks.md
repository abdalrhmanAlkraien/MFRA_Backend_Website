# Tasks: Auth Module

**Module**: auth
**Generated**: 29/03/2026 at 14:00
**Total tasks**: 2

---

## Task 1.3 — Backend: Auth — Security Config + JWT + Admin Users

**Platform**: Backend
**Complexity**: Complex
**Estimated cost**: $1.20
**Dependencies**: Task 1.2 (DB + Flyway + Core Entities)

**Delivers**:
- Flyway migration: V2__create_auth_tables.sql (admin_users table)
- AdminUserEntity.java (extends BaseEntity)
- AdminUserRepository.java (all queries with DeletedAtIsNull)
- AuthService.java (@Transactional, login/logout/refresh logic)
- RateLimitService.java (Redis-based rate limiting)
- AuthController.java (login, refresh, logout)
- SecurityConfig.java (URL security mapping, CORS, stateless session)
- JwtUtil.java (generate, validate, parse, extract claims)
- JwtAuthFilter.java (OncePerRequestFilter)
- AuditorAwareImpl.java (for @CreatedBy/@LastModifiedBy)
- RedisConfig.java (JWT blacklist + refresh token storage)
- CorsConfig.java
- SwaggerConfig.java
- DTOs: LoginRequest, LoginResponse, RefreshRequest, TokenResponse, UserInfoResponse
- Seed default admin user via ApplicationRunner

**API endpoints covered**:
- POST /api/auth/login
- POST /api/auth/refresh
- POST /api/auth/logout

**Acceptance criteria covered**:
- [AC-1] through [AC-17], [AC-S1], [AC-S2], [AC-S3], [AC-S4]

**Test scenarios**:
- Backend: 12 scenarios (login success, invalid creds, inactive user, rate limit block, logout blacklist, refresh success, refresh expired, expired token, wrong role, blacklisted token, case-insensitive email, soft delete)

---

## Task 1.5 — Frontend: Auth — Login Page + AuthGuard + Token Management

**Platform**: Frontend
**Complexity**: Medium
**Estimated cost**: $0.80
**Dependencies**: Task 1.3 (Security + JWT backend)

**Delivers**:
- Redux store + RTK Query base setup
- features/auth/authApi.ts (login, logout, refresh RTK Query endpoints)
- features/auth/authSlice.ts (token storage, user info, isAuthenticated)
- pages/admin/LoginPage.tsx (page 1 design)
- components/guards/AuthGuard.tsx
- components/guards/RoleGuard.tsx
- hooks/useAutoRefresh.ts (token auto-refresh)
- Tailwind config with DESIGN.md tokens
- React Router setup with public + admin routes

**Acceptance criteria covered**:
- [AC-18] through [AC-28]

**Test scenarios**:
- Frontend: 8 scenarios (render login, submit valid, submit invalid, error display, redirect on auth, AuthGuard block, RoleGuard block, token refresh)
