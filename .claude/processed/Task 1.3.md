# Task 1.3 + 1.5 — Security Config + JWT Auth + Login Frontend

**Status**: ✅ Completed
**Platform**: Backend + Frontend (merged 1.3 + 1.5)
**Date**: 30/03/2026
**Actual cost**: $1.20

---

## What Was Built

### Backend — Auth Module

**Entity & Repository:**
- `AdminRole.java` — Enum (ADMIN, EDITOR)
- `AdminUserEntity.java` — JPA entity mapped to admin_users table
- `AdminUserRepository.java` — findByEmailIgnoreCaseAndDeletedAtIsNull

**Security:**
- `JwtUtil.java` — Token generation (access + refresh), parsing, validation, blacklist check
- `JwtAuthFilter.java` — OncePerRequestFilter, extracts Bearer token, sets SecurityContext
- `CustomUserDetailsService.java` — Loads user from DB for Spring Security
- `SecurityConfig.java` — Updated: JWT filter, CORS, @EnableMethodSecurity, AuthenticationManager

**Service & Controller:**
- `AuthService.java` — login, refresh, logout logic with Redis blacklist
- `AuthController.java` — POST /api/auth/login, /refresh, /logout
- `RateLimitService.java` — Redis-based, 5 attempts per IP per 15min, 1h block

**DTOs:**
- `LoginRequest.java` — email, password, rememberDevice
- `LoginResponse.java` — accessToken, refreshToken, tokenType, expiresIn, user
- `RefreshRequest.java` — refreshToken
- `AdminUserResponse.java` — id, fullName, email, role, avatarUrl

**Configuration:**
- `RedisConfig.java` — StringRedisTemplate bean
- `CorsConfig.java` — Dynamic origins from app.cors.allowed-origins
- `AdminUserSeeder.java` — Seeds default admin on startup from env vars

**Migration:**
- `V2__create_auth_tables.sql` — admin_users table with partial unique index on email

### Frontend — Login + Auth Guards

- `features/auth/types.ts` — LoginRequest, LoginResponse, AuthUser, RefreshRequest
- `features/auth/authApi.ts` — RTK Query endpoints (login, refresh, logout)
- `features/auth/authSlice.ts` — Redux slice with setCredentials, updateToken, logout
- `features/auth/pages/LoginPage.tsx` — Dark theme login page matching design
- `components/layout/AuthGuard.tsx` — Redirects unauthenticated users to /admin/login
- `components/layout/RoleGuard.tsx` — Restricts by role (ADMIN vs EDITOR)
- `app/hooks.ts` — Typed useAppDispatch, useAppSelector
- `app/store.ts` — Updated with auth reducer + authApi middleware
- `App.tsx` — Updated with /admin/login route and AuthGuard-protected /admin/dashboard

---

## Infrastructure Changes

- **Redis port**: Changed from 6379 to 6380 in docker-compose.yml to avoid conflict with existing Redis container
- **application.yml**: Updated REDIS_PORT default to 6380, added app.admin.* env vars for seeder

---

## Verification Results

```
Backend:
  mvn clean compile    — BUILD SUCCESS
  App startup          — 2.263 seconds
  Flyway               — V1 + V2 validated, schema up to date
  Admin seeder         — Default admin user seeded (admin@mfra.com)
  Redis                — Connected on port 6380
  Health               — {"status":"UP"}

API Tests:
  POST /api/auth/login (valid)     — 200 OK, tokens + user returned
  POST /api/auth/login (invalid)   — 401, INVALID_CREDENTIALS
  POST /api/auth/refresh           — 200 OK, new access token
  POST /api/auth/logout            — 200 OK
  GET /api/admin/* (no token)      — 403 Forbidden

Frontend:
  tsc --noEmit         — 0 errors
  npm run build        — 380.29 kB JS, 9.86 kB CSS
```

---

## Files Created/Modified

### New Files (17)
```
backend/src/main/java/com/mfra/website/module/auth/entity/AdminRole.java
backend/src/main/java/com/mfra/website/module/auth/entity/AdminUserEntity.java
backend/src/main/java/com/mfra/website/module/auth/repository/AdminUserRepository.java
backend/src/main/java/com/mfra/website/module/auth/dto/LoginRequest.java
backend/src/main/java/com/mfra/website/module/auth/dto/LoginResponse.java
backend/src/main/java/com/mfra/website/module/auth/dto/RefreshRequest.java
backend/src/main/java/com/mfra/website/module/auth/dto/AdminUserResponse.java
backend/src/main/java/com/mfra/website/module/auth/security/JwtUtil.java
backend/src/main/java/com/mfra/website/module/auth/security/JwtAuthFilter.java
backend/src/main/java/com/mfra/website/module/auth/security/CustomUserDetailsService.java
backend/src/main/java/com/mfra/website/module/auth/service/AuthService.java
backend/src/main/java/com/mfra/website/module/auth/service/RateLimitService.java
backend/src/main/java/com/mfra/website/module/auth/controller/AuthController.java
backend/src/main/java/com/mfra/website/module/auth/config/AdminUserSeeder.java
backend/src/main/java/com/mfra/website/config/RedisConfig.java
backend/src/main/java/com/mfra/website/config/CorsConfig.java
backend/src/main/resources/db/migration/V2__create_auth_tables.sql
frontend/src/features/auth/types.ts
frontend/src/features/auth/authApi.ts
frontend/src/features/auth/authSlice.ts
frontend/src/features/auth/pages/LoginPage.tsx
frontend/src/components/layout/AuthGuard.tsx
frontend/src/components/layout/RoleGuard.tsx
frontend/src/app/hooks.ts
```

### Modified Files (5)
```
backend/src/main/java/com/mfra/website/config/SecurityConfig.java
backend/src/main/java/com/mfra/website/common/exception/GlobalExceptionHandler.java
backend/src/main/resources/application.yml
frontend/src/app/store.ts
frontend/src/App.tsx
docker-compose.yml (Redis port 6379 → 6380)
```

---

## Next Task

Task 1.4: File Upload Service (S3) (Backend)
