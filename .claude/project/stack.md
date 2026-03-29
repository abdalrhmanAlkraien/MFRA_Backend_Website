# Project Stack

> This file defines the complete technology stack for this project.
> The AI agent reads this file to know which platforms, providers, and
> instruction files are in scope. Nothing is assumed — if it is not listed
> here, it is not built.

---

## Platforms

```
Backend:   ✅ Spring Boot           → instructions/backend.md
Frontend:  ✅ React JS              → instructions/frontend.md
Mobile:    ⛔ Not in this project   → skip all Flutter / mobile code
```

---

## Project Type

```
Type:      Website (Public + Admin Panel)
Public:    Client-facing website     → /
Admin:     Internal management panel → /admin
```

---

## Backend

```
Framework:     Spring Boot 3.x
Language:      Java 21
Port:          8080
Build:         Maven
API Style:     REST
Docs:          Swagger UI at /swagger-ui.html
Health:        /actuator/health
```

### Backend Dependencies

```
Core:
  spring-boot-starter-web
  spring-boot-starter-data-jpa
  spring-boot-starter-security
  spring-boot-starter-validation
  spring-boot-starter-actuator

Database:
  postgresql (driver)
  flyway-core

Cache:
  spring-boot-starter-data-redis    ← Redis selected

Auth:
  jjwt-api
  jjwt-impl
  jjwt-jackson

Utilities:
  lombok
  mapstruct
  springdoc-openapi-starter-webmvc-ui

Email:
  spring-boot-starter-mail
  spring-boot-starter-thymeleaf     ← HTML email templates

Testing:
  spring-boot-starter-test
  testcontainers (postgresql, redis)
```

---

## Frontend

```
Framework:     React 18 + TypeScript
Build tool:    Vite
Port (dev):    5173
```

### Frontend Dependencies

```
Core:
  react 18
  react-dom
  typescript
  vite

Routing:
  react-router-dom v6

State management:
  @reduxjs/toolkit
  react-redux

API calls:
  RTK Query (built into @reduxjs/toolkit)
  axios                               ← inside RTK Query only

Styling:
  tailwindcss
  postcss
  autoprefixer

Forms:
  react-hook-form
  @hookform/resolvers
  zod

Rich text editor:
  @tiptap/react                       ← blog and case study editors
  @tiptap/starter-kit

Notifications:
  react-hot-toast

Icons:
  lucide-react

HTTP Client:
  axios
```

---

## Database

```
Provider:    PostgreSQL 15
ORM:         Spring Data JPA + Hibernate
Migrations:  Flyway
Schema:      managed by Flyway only
             spring.jpa.hibernate.ddl-auto = validate
```

---

## Cache

```
Provider:    Redis 7                  ← ENABLED
Client:      Spring Data Redis (Lettuce)
Config file: config/RedisConfig.java
Usage:
  - Public blog list (10 min TTL)
  - Public case study list (10 min TTL)
  - Single blog by slug (30 min TTL)
  - Single case study by slug (30 min TTL)
  - Testimonials (1 hour TTL)
  - Site stats (24 hour TTL)
  - Site settings (1 hour TTL)
  - Tools list (1 hour TTL)
  - JWT blacklist (until token expiry)
  - Refresh tokens (7 days)
  - Rate limiting keys (per endpoint window)
```

---

## Email

```
Provider:    SMTP                     ← ENABLED
Client:      JavaMailSender
Templates:   Thymeleaf HTML templates
Config file: config/EmailConfig.java
Dev tool:    Mailhog (localhost:1025 SMTP, localhost:8025 UI)
Usage:
  - Consultation request confirmation (to client)
  - New consultation notification (to admin team)
  - New contact message notification (to admin team)
```

---

## Auth

```
Mechanism:   JWT (stateless)
Library:     JJWT
Storage:     JWT in Authorization header (Bearer)
             Refresh token in Redis
             Blacklist in Redis on logout
Roles:
  ROLE_ADMIN   → full access to all admin panel features
  ROLE_EDITOR  → blog and case study management only
```

---

## File Storage

```
Provider:    AWS S3                   ← ENABLED
SDK:         AWS SDK for Java v2
Usage:
  - Blog cover images
  - Case study architecture diagrams
  - Team member photos
  - Tool logos
Max size:    5 MB per file
Allowed:     image/jpeg, image/png, image/webp
```

---

## SMS

```
Provider:    ⛔ Not in this project   → skip sms.md entirely
```

---

## Push Notifications

```
Provider:    ⛔ Not in this project   → skip notification.md entirely
```

---

## WhatsApp

```
Provider:    WhatsApp Cloud API       ← ENABLED (contact button + messages)
Usage:
  - Floating WhatsApp button on public pages (wa.me link)
  - Static link only — no programmatic messages in MVP
Config file: N/A for static link (frontend only)
```

---

## WebSocket / Real-Time

```
Provider:    ⛔ Not in this project   → skip websocket.md entirely
```

---

## Async

```
@EnableAsync:         ✅ ENABLED
@EnableScheduling:    ✅ ENABLED
Config file:          config/AsyncConfig.java
Thread pools:
  emailTaskExecutor   → core=2, max=5, queue=50
  reportTaskExecutor  → core=2, max=4, queue=20
```

---

## Infrastructure

```
Deployment:       AWS (ECS + RDS + ElastiCache + S3 + CloudFront)
CI/CD:            GitHub Actions
Containers:       Docker + Docker Compose (local dev only)
```

### Docker Compose Services (local dev)

```yaml
services:
  postgres:    image: postgres:15    port: 5432
  redis:       image: redis:7        port: 6379
  mailhog:     image: mailhog        SMTP: 1025, UI: 8025
```

---

## Environment Variables

### Backend (.env)

```bash
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=mfra
DB_USER=mfra
DB_PASS=mfra

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# JWT
JWT_SECRET=your-256-bit-secret-here
JWT_EXPIRATION_MS=3600000
JWT_REFRESH_EXPIRATION_MS=604800000

# Email
MAIL_HOST=localhost
MAIL_PORT=1025
MAIL_USERNAME=
MAIL_PASSWORD=
MAIL_FROM=noreply@mfra.com
MAIL_FROM_NAME=MFRA Team
ADMIN_EMAIL=admin@mfra.com
MAIL_REPLY_TO=hello@mfra.com

# AWS S3
AWS_S3_BUCKET=mfra-uploads
AWS_ACCESS_KEY=your-access-key
AWS_SECRET_KEY=your-secret-key
AWS_REGION=me-south-1

# CORS
CORS_ORIGINS=http://localhost:5173

# App
APP_PORT=8080
```

### Frontend (.env)

```bash
VITE_API_BASE_URL=http://localhost:8080/api
VITE_APP_NAME=MFRA
```

---

## Instruction Files in Scope

Based on this stack, the AI agent reads these instruction files:

```
Always:
  ✅ instructions/backend.md
  ✅ instructions/frontend.md
  ✅ instructions/backend-testing.md
  ✅ instructions/frontend-testing.md
  ✅ instructions/database.md

Conditional (check before reading):
  ✅ instructions/async.md         ← async enabled
  ✅ instructions/cache.md         ← Redis enabled
  ✅ instructions/email.md         ← email enabled
  ✅ instructions/whatsapp.md      ← WhatsApp button enabled

Skip entirely:
  ⛔ instructions/sms.md           ← SMS not in this project
  ⛔ instructions/notification.md  ← Push not in this project
  ⛔ instructions/websocket.md     ← WebSocket not in this project
```

---

## What Is NOT in This Project

```
Mobile (Flutter):         ⛔ not in scope
SMS (Twilio / SNS):       ⛔ not in scope
Push Notifications (FCM): ⛔ not in scope
WebSocket / SSE:          ⛔ not in scope
GraphQL:                  ⛔ not in scope
Microservices:            ⛔ not in scope — monolith only
```