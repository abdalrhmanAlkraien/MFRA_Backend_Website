# Plan: Auth Module

**Module**: auth
**Generated**: 29/03/2026 at 14:00

---

## Implementation Approach

### Package Structure
```
com.mfra.website/
├── config/
│   ├── SecurityConfig.java
│   ├── RedisConfig.java
│   ├── CorsConfig.java
│   └── SwaggerConfig.java
├── common/
│   ├── entity/BaseEntity.java
│   ├── response/ApiResponse.java
│   ├── response/PagedResponse.java
│   ├── exception/GlobalExceptionHandler.java
│   ├── exception/ResourceNotFoundException.java
│   ├── exception/RateLimitExceededException.java
│   └── security/
│       ├── JwtUtil.java
│       ├── JwtAuthFilter.java
│       └── AuditorAwareImpl.java
└── module/
    └── auth/
        ├── entity/AdminUserEntity.java
        ├── repository/AdminUserRepository.java
        ├── service/AuthService.java
        ├── service/RateLimitService.java
        ├── controller/AuthController.java
        ├── dto/LoginRequest.java
        ├── dto/LoginResponse.java
        ├── dto/RefreshRequest.java
        ├── dto/TokenResponse.java
        └── dto/UserInfoResponse.java
```

### Layer Decisions
- **Password hashing**: BCrypt via Spring Security's PasswordEncoder
- **JWT library**: JJWT (jjwt-api, jjwt-impl, jjwt-jackson)
- **Token storage**: Access token in Authorization header, refresh token in Redis
- **Blacklist**: Redis key `auth:blacklist:{jti}` with TTL = remaining token life
- **Rate limiting**: Redis key `ratelimit:login:{ip}` with TTL = 15 minutes
- **Auth**: SecurityConfig with URL-based security + JwtAuthFilter
- **Seed user**: ApplicationRunner that creates default admin if no admin exists

### Key Service Methods
- `login(request)` → validate credentials → check rate limit → generate tokens → update last_login → log attempt
- `refresh(refreshToken)` → validate refresh → generate new access token
- `logout(accessToken)` → extract JTI → store in Redis blacklist with remaining TTL
- `checkRateLimit(ip)` → increment Redis counter → throw if exceeded

### Frontend Approach
- **State**: RTK Query for auth API calls, authSlice for token/user storage
- **Token storage**: localStorage for access + refresh tokens
- **Auto-refresh**: Interceptor that refreshes token when 401 received or before expiry
- **Guards**: AuthGuard checks for valid token, RoleGuard checks role
- **Forms**: react-hook-form + zod for login validation

---

## Cache Keys

| Key Pattern | TTL | Invalidated By |
|-------------|-----|----------------|
| auth:blacklist:{jti} | Remaining token TTL | Logout |
| auth:refresh:{userId} | 7 days / 30 days | Refresh, Logout |
| ratelimit:login:{ip} | 15 minutes | Auto-expire |

---

## Dependencies

- Depends on: Foundation (Task 1.1, 1.2) — project scaffold, DB, BaseEntity
- Required config: SecurityConfig, RedisConfig, CorsConfig
- Required env vars: JWT_SECRET, DB credentials, REDIS_HOST
