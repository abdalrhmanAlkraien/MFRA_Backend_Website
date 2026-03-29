# Project Configurations Tracker

> This file is the single source of truth for all configurations prepared in this project.
> The AI agent reads this file BEFORE writing any infrastructure or integration code.
> The AI agent updates this file AFTER completing any configuration setup.
> Never skip reading this file — it prevents duplicate setup and catches missing dependencies.

---

## How the Agent Uses This File

### Before writing any code — agent checks:
```
1. Read this file
2. Find the configuration needed for the current task
3. Is it ✅ READY?   → Use it directly, do not re-setup
4. Is it ❌ MISSING? → Set it up first, then mark ✅ READY
5. Is it ⚠️ PARTIAL? → Complete the missing parts, then mark ✅ READY
```

### After completing any configuration — agent must:
```
1. Update the relevant section in this file
2. Change status from ❌ MISSING or ⚠️ PARTIAL to ✅ READY
3. Fill in the details (provider, settings, notes)
4. Commit the change to this file with the task
```

---

## Configuration Status Legend

| Status | Meaning |
|---|---|
| ✅ READY | Fully configured, tested, and working |
| ⚠️ PARTIAL | Partially configured — details in the section |
| ❌ MISSING | Not configured — must be set up before use |
| ⛔ NOT USED | Not applicable for this project — intentionally skipped |

---

## Instruction Files Reference

The agent reads the following instruction files when a configuration is needed:

| Configuration | Instruction File | Trigger Signal |
|---|---|---|
| Database | `instructions/database.md` | Any entity, repository, migration |
| Cache | `instructions/cache.md` | Public read endpoints, stats, lists |
| Email | `instructions/email.md` | Consultation, contact, notifications |
| Async | `instructions/async.md` | Email, exports, long-running jobs |
| SMS | `instructions/sms.md` | OTP, phone verification, mobile alerts |
| Notifications | `instructions/notification.md` | Push notifications, browser/mobile alerts |
| WhatsApp | `instructions/whatsapp.md` | WhatsApp button, programmatic messages, MENA channel |
| WebSocket | `instructions/websocket.md` | Real-time updates, live progress, chat |
| Backend | `instructions/backend.md` | All backend code |
| Frontend | `instructions/frontend.md` | All frontend code |
| Backend Testing | `instructions/backend-testing.md` | All backend tests |
| Frontend Testing | `instructions/frontend-testing.md` | All frontend tests |

---

## 1. Database Configuration

**Status**: ❌ MISSING

**Instruction file**: `instructions/database.md`
**Required by**: Every module — must be set up in Phase 1
**Needed by**: Pages 1-8 (all analyzed pages) — Task 1.2

```
Provider:        [ ] PostgreSQL  [ ] MySQL  [ ] Other: ________
Version:         _______________
Migration tool:  [ ] Flyway  [ ] Liquibase
ORM:             [ ] Spring Data JPA + Hibernate
Connection pool: [ ] HikariCP (default)  [ ] Other: ________

Environment variables:
  DB_HOST:     ❌ not set
  DB_PORT:     ❌ not set
  DB_NAME:     ❌ not set
  DB_USER:     ❌ not set
  DB_PASS:     ❌ not set

application.yml key: spring.datasource
Config class:   N/A — auto-configured by Spring Boot
Migration path: src/main/resources/db/migration/

Setup completed in task: _______________
Notes: _______________
```

---

## 2. Cache Configuration

**Status**: ❌ MISSING

**Instruction file**: `instructions/cache.md`
**Required by**: Public read endpoints, stats, settings, blog/case study lists
**Needed by**: Pages 3, 6 (blog/CS public lists), JWT blacklist (Page 1) — Task 1.3, 3.2, 4.2

```
Provider:   [ ] Redis  [ ] Memcached  [ ] Not used (mark ⛔)

If Redis:
  Version:            _______________
  Config class:       config/RedisConfig.java
  CacheManager:       RedisCacheManager with per-cache TTL
  Template:           RedisTemplate<String, Object>

  Environment variables:
    REDIS_HOST:   ❌ not set
    REDIS_PORT:   ❌ not set

  Cache names configured:
    [ ] stats          TTL: 24h
    [ ] settings       TTL: 1h
    [ ] tools          TTL: 1h
    [ ] testimonials   TTL: 1h
    [ ] blogs          TTL: 10min
    [ ] blog           TTL: 30min
    [ ] case-studies   TTL: 10min
    [ ] case-study     TTL: 30min
    [ ] categories     TTL: 1h

If Memcached:
  Version:            _______________
  Config class:       config/MemcachedConfig.java

  Environment variables:
    MEMCACHED_HOST:   ❌ not set
    MEMCACHED_PORT:   ❌ not set

application.yml key:  spring.data.redis / memcached
Setup completed in task: _______________
Notes: _______________
```

---

## 3. Email Configuration

**Status**: ❌ MISSING

**Instruction file**: `instructions/email.md`
**Required by**: Consultation form, contact form, async job completion, notifications
**Needed by**: Pages 9-14 (pending specs — consultation + contact forms) — Task 5.1

```
Provider:   [ ] Gmail SMTP
            [ ] SendGrid
            [ ] AWS SES
            [ ] Mailgun
            [ ] Other: ________

Dependencies added to pom.xml:
  [ ] spring-boot-starter-mail
  [ ] spring-boot-starter-thymeleaf (if HTML templates)

Config class:       config/EmailConfig.java
Thread pool bean:   emailTaskExecutor (core=2, max=5, queue=50)
Templates location: src/main/resources/templates/email/

Environment variables:
  MAIL_HOST:      ❌ not set
  MAIL_PORT:      ❌ not set
  MAIL_USERNAME:  ❌ not set
  MAIL_PASSWORD:  ❌ not set
  MAIL_FROM:      ❌ not set
  MAIL_FROM_NAME: ❌ not set
  ADMIN_EMAIL:    ❌ not set
  MAIL_REPLY_TO:  ❌ not set

Local dev setup:
  [ ] Mailhog added to docker-compose.yml
  [ ] application-dev.yml configured to use localhost:1025
  [ ] application-test.yml configured to use localhost:1025

Email services created:
  [ ] EmailService.java            (base service)
  [ ] ConsultationEmailService.java
  [ ] ContactEmailService.java
  [ ] JobEmailService.java          (async job notifications)

Email templates created:
  [ ] consultation-confirmation.html
  [ ] admin-new-consultation.html
  [ ] contact-notification.html

application.yml key:  spring.mail + app.email
Setup completed in task: _______________
Notes: _______________
```

---

## 4. Async Configuration

**Status**: ❌ MISSING

**Instruction file**: `instructions/async.md`
**Required by**: Email sending, file exports, long-running jobs, scheduled tasks

```
@EnableAsync:        [ ] Added to config/AsyncConfig.java
@EnableScheduling:   [ ] Added to config/SchedulingConfig.java

Thread pools configured:
  [ ] emailTaskExecutor    core=2, max=5,  queue=50
  [ ] reportTaskExecutor   core=2, max=4,  queue=20

Scheduled jobs:
  [ ] Cache refresh        fixedRate: 1h
  [ ] Stats refresh        fixedRate: 24h
  [ ] Other: ________

Async job table (for status tracking):
  [ ] async_jobs table created (Flyway migration)
  [ ] AsyncJobEntity.java
  [ ] AsyncJobRepository.java
  [ ] AsyncJobStatus enum: PENDING, PROCESSING, COMPLETED, FAILED

application.yml key: spring.task.execution + spring.task.scheduling
Config class:       config/AsyncConfig.java
Setup completed in task: _______________
Notes: _______________
```

---

## 5. Security / JWT Configuration

**Status**: ❌ MISSING

**Instruction file**: `instructions/backend.md` (Security section)
**Required by**: All admin endpoints, authentication
**Needed by**: Pages 1-8 (all admin pages require JWT) — Task 1.3

```
Auth mechanism:   [ ] JWT (stateless)  [ ] Session-based  [ ] Other: ________

Dependencies added to pom.xml:
  [ ] spring-boot-starter-security
  [ ] jjwt-api
  [ ] jjwt-impl
  [ ] jjwt-jackson

Config classes:
  [ ] config/SecurityConfig.java
  [ ] common/security/JwtUtil.java
  [ ] common/security/JwtAuthFilter.java

Roles configured:
  [ ] ROLE_ADMIN
  [ ] ROLE_EDITOR
  [ ] Other: ________

Environment variables:
  JWT_SECRET:              ❌ not set
  JWT_EXPIRATION_MS:       ❌ not set  (default: 3600000 = 1h)
  JWT_REFRESH_EXPIRATION_MS: ❌ not set (default: 604800000 = 7d)

Redis used for:
  [ ] JWT blacklist (logout)
  [ ] Refresh tokens
  [ ] Rate limiting

Public endpoints (no auth):
  /api/public/**
  /api/auth/**
  /actuator/health
  /swagger-ui/**
  /v3/api-docs/**

application.yml key: app.jwt
Setup completed in task: _______________
Notes: _______________
```

---

## 6. File Storage Configuration

**Status**: ❌ MISSING

**Instruction file**: N/A — see CLAUDE.md for project-specific details
**Required by**: Image uploads (blog cover, case study architecture diagram, team photos)
**Needed by**: Pages 5, 8 (blog hero/gallery, CS architecture diagram) — Task 1.4

```
Provider:   [ ] AWS S3
            [ ] Local filesystem
            [ ] Azure Blob
            [ ] Other: ________

Dependencies added to pom.xml:
  [ ] aws-java-sdk-s3 (if S3)

Config class:   config/S3Config.java (if S3)
Service class:  service/FileStorageService.java

Environment variables (if S3):
  AWS_S3_BUCKET:     ❌ not set
  AWS_ACCESS_KEY:    ❌ not set
  AWS_SECRET_KEY:    ❌ not set
  AWS_REGION:        ❌ not set

Validation rules:
  Max file size: 5MB
  Allowed types: image/jpeg, image/png, image/webp

Admin upload endpoint:
  POST /api/admin/upload/image → returns public URL

application.yml key: app.file / aws.s3
Setup completed in task: _______________
Notes: _______________
```

---

## 7. API Documentation (Swagger)

**Status**: ❌ MISSING

**Instruction file**: N/A — standard setup
**Required by**: All modules (auto-generates from controller annotations)

```
Dependencies added to pom.xml:
  [ ] springdoc-openapi-starter-webmvc-ui

Swagger UI available at:   /swagger-ui.html
OpenAPI JSON available at: /v3/api-docs

Config class:   config/SwaggerConfig.java

API info:
  Title:       _______________
  Description: _______________
  Version:     _______________
  Contact:     _______________

application.yml key: springdoc
Setup completed in task: _______________
Notes: _______________
```

---

## 8. Rate Limiting Configuration

**Status**: ❌ MISSING

**Instruction file**: `instructions/backend.md` (Rate Limiting section)
**Required by**: Public POST endpoints (consultation, contact)
**Depends on**: Cache configuration (Redis) — must be ✅ READY first

```
Implementation:  Redis-based (RateLimitService.java)
Config class:    N/A — uses RedisTemplate from cache config

Rate limits configured:
  [ ] POST /api/public/consultation   3 req / 1 hour per IP
  [ ] POST /api/public/contact        5 req / 1 hour per IP
  [ ] POST /api/auth/login           10 req / 15 min per IP

Redis key pattern: ratelimit:{endpoint}:{ip}
Response on limit exceeded: 429 Too Many Requests

application.yml key: app.rate-limit
Setup completed in task: _______________
Notes: _______________
```

---

## 9. CORS Configuration

**Status**: ❌ MISSING

**Instruction file**: N/A — standard setup
**Required by**: All API endpoints (frontend must be able to call backend)

```
Config class:  config/CorsConfig.java

Allowed origins:
  Development: http://localhost:5173
  Production:  ${CORS_ORIGINS}

Allowed methods:  GET, POST, PUT, PATCH, DELETE, OPTIONS
Allowed headers:  Authorization, Content-Type, Accept
Allow credentials: true
Max age:          3600

Environment variables:
  CORS_ORIGINS: ❌ not set

application.yml key: app.cors.allowed-origins
Setup completed in task: _______________
Notes: _______________
```

---

## 10. Docker / Local Infrastructure

**Status**: ❌ MISSING

**Instruction file**: N/A
**Required by**: Local development and testing

```
docker-compose.yml services:
  [ ] PostgreSQL    port: 5432
  [ ] Redis         port: 6379  (if cache = Redis)
  [ ] Memcached     port: 11211 (if cache = Memcached)
  [ ] Mailhog       SMTP: 1025, UI: 8025  (if email configured)

docker-compose.yml location: project root
Start command: docker compose up -d

Setup completed in task: _______________
Notes: _______________
```


---

## 11. SMS Configuration

**Status**: ❌ MISSING

**Instruction file**: `instructions/sms.md`
**Required by**: OTP verification, mobile confirmations, 2FA
**Provider selected in**: `project/stack.md` — must be listed there before setup

```
Provider (from stack.md):
  [ ] Twilio    — global coverage
  [ ] AWS SNS   — good MENA delivery, already in stack if using AWS
  [ ] Not used (mark ⛔)

Dependencies added to pom.xml:
  [ ] twilio (if Twilio)
  [ ] aws-java-sdk-sns (if AWS SNS — may already exist via AWS SDK BOM)

Config class:      config/SmsConfig.java
Interface:         service/SmsService.java
Implementation:    service/TwilioSmsService.java or AwsSnsSmsService.java
Thread pool bean:  smsTaskExecutor (core=2, max=5, queue=50)

Environment variables (Twilio):
  TWILIO_ACCOUNT_SID:   ❌ not set
  TWILIO_AUTH_TOKEN:    ❌ not set
  TWILIO_FROM_NUMBER:   ❌ not set

Environment variables (AWS SNS):
  AWS_REGION:           ❌ not set
  SNS_SENDER_ID:        ❌ not set

Common:
  SMS_PROVIDER:   ❌ not set  (twilio | awssns)
  SMS_ENABLED:    ❌ not set  (false in dev/test, true in prod)

OTP support (if needed):
  [ ] OtpService.java created
  [ ] OTP stored in Redis with 5 min TTL
  [ ] Max 3 attempts enforced via Redis counter

application.yml key: app.sms
Setup completed in task: _______________
Notes: _______________
```

---

## 12. Notification Configuration

**Status**: ❌ MISSING

**Instruction file**: `instructions/notification.md`
**Required by**: Browser/mobile push alerts, real-time user notifications
**Provider selected in**: `project/stack.md` — must be listed there before setup

```
Push provider (from stack.md):
  [ ] Firebase FCM  — mobile (Android/iOS) + web
  [ ] Web Push VAPID — browser-only
  [ ] Not used (mark ⛔)

Dependencies added to pom.xml:
  [ ] firebase-admin SDK (if FCM)
  [ ] web-push library (if VAPID)

Config class:      config/PushNotificationConfig.java
Service:           service/PushNotificationService.java
Thread pool bean:  pushTaskExecutor (core=2, max=5, queue=100)

Device token storage:
  [ ] device_tokens table created (Flyway migration)
  [ ] DeviceTokenEntity.java
  [ ] DeviceTokenService.java
  [ ] DeviceTokenController.java (POST /api/push/register)

Environment variables (FCM):
  PUSH_PROVIDER:          ❌ not set  (fcm)
  PUSH_ENABLED:           ❌ not set  (false in dev/test)
  FCM_CREDENTIALS_FILE:   ❌ not set
  FCM_PROJECT_ID:         ❌ not set

Environment variables (VAPID):
  PUSH_PROVIDER:          ❌ not set  (vapid)
  PUSH_ENABLED:           ❌ not set
  VAPID_PUBLIC_KEY:       ❌ not set
  VAPID_PRIVATE_KEY:      ❌ not set
  VAPID_SUBJECT:          ❌ not set

Frontend environment:
  VITE_FIREBASE_API_KEY:              ❌ not set
  VITE_FIREBASE_PROJECT_ID:           ❌ not set
  VITE_FIREBASE_MESSAGING_SENDER_ID:  ❌ not set
  VITE_FIREBASE_APP_ID:               ❌ not set
  VITE_FIREBASE_VAPID_KEY:            ❌ not set

Frontend:
  [ ] usePushNotifications.ts hook
  [ ] PushPermissionBanner.tsx component
  [ ] Service worker: public/firebase-messaging-sw.js (if FCM)

application.yml key: app.push
Setup completed in task: _______________
Notes: _______________
```


---

## 13. WhatsApp Configuration

**Status**: ❌ MISSING

**Instruction file**: `instructions/whatsapp.md`
**Required by**: WhatsApp contact button, programmatic notifications to MENA clients
**Provider selected in**: `project/stack.md` — must be listed there before setup

```
Provider (from stack.md):
  [ ] WhatsApp Cloud API  — Meta Business direct integration
  [ ] Twilio WhatsApp     — use if Twilio already in stack (sms.md)
  [ ] Not used (mark ⛔)

Use case type:
  [ ] Static link only (wa.me button) — frontend only, NO backend needed
  [ ] Programmatic messages (send from server) — backend needed
  [ ] Receive incoming messages (webhook) — backend + webhook needed

If programmatic messages needed:
  Config class:     config/WhatsAppConfig.java
  Service class:    service/WhatsAppService.java
  Thread pool bean: whatsappTaskExecutor (core=2, max=5)

  Environment variables (Cloud API):
    WHATSAPP_PROVIDER:          ❌ not set  (cloud-api)
    WHATSAPP_ENABLED:           ❌ not set  (false in dev/test)
    WHATSAPP_TOKEN:             ❌ not set
    WHATSAPP_PHONE_NUMBER_ID:   ❌ not set

  Environment variables (Twilio WhatsApp):
    WHATSAPP_PROVIDER:          ❌ not set  (twilio)
    WHATSAPP_ENABLED:           ❌ not set
    app.whatsapp.twilio.from:   ❌ not set  (whatsapp:+14155238886)
    (reuses TWILIO_ACCOUNT_SID and TWILIO_AUTH_TOKEN from sms.md)

If webhook needed:
  [ ] WhatsAppWebhookController.java
  WHATSAPP_VERIFY_TOKEN:        ❌ not set

Frontend (static link):
  [ ] WhatsAppButton.tsx component
  [ ] Floating button on public pages
  [ ] Phone number loaded from site settings API

application.yml key: app.whatsapp
Setup completed in task: _______________
Notes: _______________
```

---

## 14. WebSocket Configuration

**Status**: ❌ MISSING

**Instruction file**: `instructions/websocket.md`
**Required by**: Real-time features, live job progress, live dashboards
**Depends on**: Async Configuration must be ✅ READY first

```
Type (from stack.md):
  [ ] Spring WebSocket + STOMP  — full duplex, chat, collaborative
  [ ] Server-Sent Events (SSE)  — one-way server push, simpler
  [ ] Not used (mark ⛔)

Decision check (agent must verify before implementing):
  Concurrent users < 50?     → Use polling instead (simpler)
  One-way server push only?  → Use SSE (simpler than WebSocket)
  Two-way communication?     → Use WebSocket + STOMP

If WebSocket + STOMP:
  Config class:     config/WebSocketConfig.java
  Security class:   config/WebSocketSecurityConfig.java
  Interceptor:      config/WebSocketAuthInterceptor.java
  Service class:    service/WebSocketMessageService.java
  STOMP endpoint:   /ws (with SockJS fallback)
  Topics:           /topic/consultations, /topic/notifications
  User queues:      /user/queue/job-progress, /user/queue/notification-count

If SSE:
  Controller:    controller/SseController.java
  Registry:      service/SseEmitterRegistry.java
  Endpoint:      GET /api/sse/subscribe (text/event-stream)

Frontend dependencies:
  [ ] @stomp/stompjs (if WebSocket + STOMP)
  [ ] sockjs-client (if WebSocket + STOMP)
  [ ] Native EventSource API (if SSE — no dependency needed)

  Files:
  [ ] lib/websocket.ts or hooks/useSse.ts
  [ ] hooks/useJobProgress.ts (if job progress needed)
  [ ] hooks/useLiveNotifications.ts (if live notification count needed)

application.yml key: spring.websocket (auto-configured)
Setup completed in task: _______________
Notes: _______________
```

---

## Configuration Dependency Map

Some configurations depend on others being ready first:

```
Database          ← Must be first — everything depends on it
    ↓
Security / JWT    ← Depends on Database (user lookup)
    ↓
Async             ← Must be ready before Email, SMS, Notifications
    ↓
Cache             ← Must be ready before Rate Limiting and OTP
    ↓
Email             ← Depends on Async (emailTaskExecutor)
    ↓
SMS               ← Depends on Async (smsTaskExecutor) + Cache (OTP storage)
    ↓
Notifications     ← Depends on Async (notificationTaskExecutor) + Database
    ↓
WhatsApp          ← Depends on Async (whatsappTaskExecutor)
    ↓
WebSocket / SSE   ← Depends on Async + Security (JWT validation in handshake)
    ↓
Rate Limiting     ← Depends on Cache (Redis)
    ↓
File Storage      ← Depends on Database (URL storage)
    ↓
API Docs          ← Depends on all controllers existing
```

**Recommended setup order:**
```
Phase 1: Database → Security → CORS → Docker
Phase 2: Async → Cache → Rate Limiting
Phase 3: Email → SMS → Notifications
Phase 4: WhatsApp → WebSocket / SSE
Phase 5: File Storage → API Docs
```

---

## Quick Status Dashboard

The agent reads this table at the start of every session to know
what is available and what is missing:

| # | Configuration | Status | Needed By | Set Up In Task |
|---|---|---|---|---|
| 1 | Database | ❌ MISSING | All pages | Task 1.2 |
| 2 | Cache (Redis) | ❌ MISSING | Pages 1,3,6 (JWT + public lists) | Task 1.3 |
| 3 | Email | ❌ MISSING | Pages 9-14 (pending specs) | Task 5.1 |
| 4 | Async | ❌ MISSING | Email, exports | Task 5.1 |
| 5 | Security / JWT | ❌ MISSING | All admin pages (1-8) | Task 1.3 |
| 6 | File Storage (S3) | ❌ MISSING | Pages 5, 8 (image uploads) | Task 1.4 |
| 7 | API Docs (Swagger) | ❌ MISSING | All endpoints | Task 1.3 |
| 8 | Rate Limiting | ❌ MISSING | Page 1 (login), future forms | Task 1.3 |
| 9 | CORS | ❌ MISSING | All API calls | Task 1.3 |
| 10 | Docker / Local | ❌ MISSING | Local dev | Task 1.1 |
| 11 | SMS | ⛔ NOT USED | — | — |
| 12 | Notifications (Push) | ⛔ NOT USED | — | — |
| 13 | WhatsApp | ❌ MISSING | Public pages (static link) | Future |
| 14 | WebSocket / SSE | ⛔ NOT USED | — | — |

**Last updated**: 29/03/2026 at 12:00
**Updated by**: /analyze-designs