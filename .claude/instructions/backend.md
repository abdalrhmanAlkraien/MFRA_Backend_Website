# Backend Coding Instructions

> This file is reusable across all projects.
> The AI agent must read this file before writing any backend code.
> Project-specific details (port, package name, modules) are in `.claude/CLAUDE.md`.

---

## Stack

- **Framework**: Spring Boot 3.x
- **Language**: Java 21
- **Security**: Spring Security + JWT (stateless)
- **ORM**: Spring Data JPA + Hibernate
- **Database**: PostgreSQL
- **Cache**: Redis (Spring Data Redis)
- **Migrations**: Flyway
- **Build**: Maven
- **Validation**: Jakarta Bean Validation
- **Docs**: SpringDoc OpenAPI (Swagger UI at `/swagger-ui.html`)
- **Logging**: SLF4J + Logback
- **Mapping**: MapStruct

---

## Architecture Layers

```
HTTP Request
     ↓
Security Filter Chain
(JwtAuthFilter → SecurityContext)
     ↓
Controller Layer
(@RestController — DTOs only — no business logic)
     ↓
Service Layer
(@Service — @Transactional — all business logic lives here)
     ↓
Repository Layer
(JpaRepository — all queries scoped and filtered)
     ↓
Infrastructure
(PostgreSQL — Redis — Flyway — Spring Security)
```

---

## Package Structure Per Module

Every module follows this exact structure — no exceptions:

```
com.<company>.<project>/
├── config/
│   ├── SecurityConfig.java
│   ├── RedisConfig.java
│   ├── CorsConfig.java
│   └── SwaggerConfig.java
├── common/
│   ├── entity/
│   │   └── BaseEntity.java
│   ├── response/
│   │   ├── ApiResponse.java
│   │   └── PagedResponse.java
│   ├── exception/
│   │   ├── GlobalExceptionHandler.java
│   │   └── ResourceNotFoundException.java
│   └── security/
│       ├── JwtUtil.java
│       └── JwtAuthFilter.java
└── module/
    └── <module-name>/
        ├── entity/          ← JPA entities (extend BaseEntity)
        ├── repository/      ← Spring Data JPA interfaces
        ├── service/         ← Business logic (@Service, @Transactional)
        ├── controller/      ← REST controllers (DTOs only, never entities)
        ├── dto/             ← Request and Response DTOs
        └── mapper/          ← MapStruct mappers (entity ↔ DTO)
```

---

## BaseEntity — Extend on Every Entity

Every domain entity extends `BaseEntity` — no exceptions:

```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;        // UTC always — never LocalDateTime

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    @CreatedBy
    private UUID createdBy;

    @LastModifiedBy
    private UUID updatedBy;

    private Instant deletedAt;        // null = active, non-null = soft deleted
}
```

**Rules:**
- `id` is always UUID — never sequential integer
- `Instant` (UTC) for ALL timestamps — never `LocalDateTime`
- `deletedAt` is used for soft delete — never call `repository.delete()`
- `createdBy` / `updatedBy` resolved from `SecurityContext` via `AuditorAware`

---

## ApiResponse Wrapper — Mandatory on Every Response

Every controller response is wrapped in `ApiResponse<T>` — never return raw objects:

```java
@Data
@Builder
public class ApiResponse<T> {
    private boolean success;
    private T data;
    private Instant timestamp;
    private Object error;    // null on success

    public static <T> ApiResponse<T> ok(T data) {
        return ApiResponse.<T>builder()
            .success(true)
            .data(data)
            .timestamp(Instant.now())
            .build();
    }

    public static <T> ApiResponse<T> error(Object error) {
        return ApiResponse.<T>builder()
            .success(false)
            .data(null)
            .timestamp(Instant.now())
            .error(error)
            .build();
    }
}
```

**Success response — single object:**
```json
{
  "success": true,
  "data": { },
  "timestamp": "2026-03-09T10:00:00Z"
}
```

**Success response — paged list:**
```json
{
  "success": true,
  "data": [],
  "page": 0,
  "size": 20,
  "totalElements": 150,
  "totalPages": 8,
  "timestamp": "2026-03-09T10:00:00Z"
}
```

**Error response:**
```json
{
  "success": false,
  "error": {
    "code": "RESOURCE_NOT_FOUND",
    "message": "Blog post with slug 'aws-guide' was not found"
  },
  "timestamp": "2026-03-09T10:00:00Z"
}
```

**Validation error response (400):**
```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Request validation failed",
    "fields": {
      "email": "must be a valid email address",
      "fullName": "must not be blank"
    }
  },
  "timestamp": "2026-03-09T10:00:00Z"
}
```

---

## Controller Pattern

Controllers are thin — no business logic, no entity references:

```java
@RestController
@RequestMapping("/api/admin/blogs")
@RequiredArgsConstructor
public class BlogAdminController {

    private final BlogService blogService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BlogResponse>> create(
        @Valid @RequestBody BlogCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok(blogService.create(request)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<ApiResponse<Page<BlogResponse>>> list(
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String category,
        Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(blogService.list(status, category, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<ApiResponse<BlogResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(blogService.getById(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<ApiResponse<BlogResponse>> update(
        @PathVariable UUID id,
        @Valid @RequestBody BlogUpdateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(blogService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        blogService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PatchMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<ApiResponse<BlogResponse>> publish(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(blogService.publish(id)));
    }
}
```

**Controller rules:**
- `@Valid` on every `@RequestBody` — no exceptions
- `@PreAuthorize` on every endpoint — no unprotected endpoints
- Return `ApiResponse<T>` always — never raw objects or entities
- HTTP status must be correct: `201` for create, `200` for all others
- No business logic in controller — delegate everything to Service
- No entity imports in controller — DTOs only
- Public endpoints go in a separate controller: `BlogPublicController`

---

## Service Pattern

```java
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class BlogService {

    private final BlogRepository blogRepository;
    private final BlogCategoryRepository categoryRepository;
    private final BlogMapper mapper;
    private final RedisTemplate<String, Object> redisTemplate;

    public BlogResponse create(BlogCreateRequest request) {
        String slug = generateSlug(request.getTitle());

        if (blogRepository.existsBySlugAndDeletedAtIsNull(slug)) {
            throw new SlugAlreadyExistsException("blog.slug-exists");
        }

        BlogEntity entity = mapper.toEntity(request);
        entity.setSlug(slug);
        entity.setReadingTimeMins(calculateReadingTime(request.getContent()));
        entity.setStatus(BlogStatus.DRAFT);
        entity = blogRepository.save(entity);

        log.info("Blog created: id={}, slug={}", entity.getId(), slug);
        return mapper.toResponse(entity);
    }

    @Transactional(readOnly = true)
    public Page<BlogResponse> list(String status, String category, Pageable pageable) {
        return blogRepository.findAllFiltered(status, category, pageable)
            .map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public BlogResponse getById(UUID id) {
        return blogRepository.findByIdAndDeletedAtIsNull(id)
            .map(mapper::toResponse)
            .orElseThrow(() -> new ResourceNotFoundException("blog.not-found"));
    }

    public BlogResponse publish(UUID id) {
        BlogEntity entity = blogRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new ResourceNotFoundException("blog.not-found"));

        entity.setStatus(BlogStatus.PUBLISHED);
        entity.setPublishedAt(Instant.now());
        entity = blogRepository.save(entity);

        // Invalidate Redis cache
        invalidateBlogCache();

        log.info("Blog published: id={}, slug={}", entity.getId(), entity.getSlug());
        return mapper.toResponse(entity);
    }

    public void delete(UUID id) {
        BlogEntity entity = blogRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new ResourceNotFoundException("blog.not-found"));

        entity.setDeletedAt(Instant.now());    // soft delete — never hard delete
        blogRepository.save(entity);

        invalidateBlogCache();
        log.info("Blog soft-deleted: id={}", id);
    }

    private void invalidateBlogCache() {
        Set<String> keys = redisTemplate.keys("blogs:public:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    private String generateSlug(String title) {
        return title.toLowerCase()
            .replaceAll("[^a-z0-9\\s-]", "")
            .replaceAll("\\s+", "-")
            .replaceAll("-+", "-")
            .trim();
    }

    private int calculateReadingTime(String content) {
        int wordCount = content.split("\\s+").length;
        return (int) Math.ceil(wordCount / 200.0);    // 200 words per minute
    }
}
```

**Service rules:**
- `@Transactional` on class (covers writes)
- `@Transactional(readOnly = true)` on all read methods — improves performance
- SLF4J only — `log.info`, `log.warn`, `log.error` — never `System.out.println`
- Never return entity from service — always map to DTO first
- Soft delete only — `entity.setDeletedAt(Instant.now())` — never `repository.delete()`
- Slug generation and reading time calculation live in service — never in controller
- Invalidate Redis cache on every write operation

---

## Repository Pattern

```java
@Repository
public interface BlogRepository extends JpaRepository<BlogEntity, UUID> {

    // Always include DeletedAtIsNull — never return soft-deleted records
    Optional<BlogEntity> findByIdAndDeletedAtIsNull(UUID id);

    Optional<BlogEntity> findBySlugAndDeletedAtIsNull(String slug);

    boolean existsBySlugAndDeletedAtIsNull(String slug);

    Page<BlogEntity> findByStatusAndDeletedAtIsNull(String status, Pageable pageable);

    Page<BlogEntity> findByCategoryIdAndStatusAndDeletedAtIsNull(
        UUID categoryId, String status, Pageable pageable
    );

    List<BlogEntity> findTop3ByCategoryIdAndStatusAndDeletedAtIsNullOrderByPublishedAtDesc(
        UUID categoryId, String status
    );
}
```

**Repository rules:**
- Every method name includes `DeletedAtIsNull` — never return deleted records
- Never use `findAll()` without filters — always scope and filter
- Use `Page<T>` for all list operations — no unbounded lists
- Use `List<T>` only for small, bounded queries (e.g. top 3 related)
- No custom `@Query` unless Spring Data naming cannot express it

---

## DTO Pattern

```java
// Request DTO — only what client should send
@Data
public class BlogCreateRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    @NotBlank(message = "Summary is required")
    private String summary;

    @NotBlank(message = "Content is required")
    private String content;

    private String coverImageUrl;

    @NotNull(message = "Category is required")
    private UUID categoryId;

    private List<UUID> tagIds;
}

// Response DTO — controlled fields only
@Data
@Builder
public class BlogResponse {
    private UUID id;
    private String title;
    private String slug;
    private String summary;
    private String content;
    private String coverImageUrl;
    private String status;
    private int readingTimeMins;
    private Instant publishedAt;
    private Instant createdAt;
    private Instant updatedAt;
    private BlogCategoryResponse category;
    private List<BlogTagResponse> tags;
    // deletedAt NEVER in response
    // password NEVER in any response
    // internal IDs only when necessary
}
```

---

## Security Configuration

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/api/admin/**").authenticated()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }
}
```

**Security rules:**
- All `/api/public/**` → open — no auth required
- All `/api/admin/**` → JWT required
- JWT secret from environment variable `JWT_SECRET` — never hardcoded
- Token expiry configurable via `app.jwt.expiration` in `application.yml`
- Refresh token stored in Redis with TTL
- Blacklisted tokens stored in Redis until expiry
- JWT token never logged or returned in error responses

---

## Global Exception Handler

```java
@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> violations = ex.getBindingResult().getFieldErrors().stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                FieldError::getDefaultMessage
            ));
        return ApiResponse.error(Map.of(
            "code", "VALIDATION_ERROR",
            "message", "Request validation failed",
            "fields", violations
        ));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleNotFound(ResourceNotFoundException ex) {
        return ApiResponse.error(Map.of(
            "code", "RESOURCE_NOT_FOUND",
            "message", ex.getMessage()
        ));
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<Void> handleAccessDenied(AccessDeniedException ex) {
        return ApiResponse.error(Map.of(
            "code", "FORBIDDEN",
            "message", "You do not have permission to perform this action"
        ));
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleGeneral(Exception ex) {
        log.error("Unexpected error", ex);
        return ApiResponse.error(Map.of(
            "code", "INTERNAL_ERROR",
            "message", "An unexpected error occurred"
        ));
    }
}
```

---

## Redis Caching

```java
@Service
@RequiredArgsConstructor
public class BlogPublicService {

    private final BlogRepository blogRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final BlogMapper mapper;

    private static final String CACHE_PREFIX = "blogs:public:";
    private static final long CACHE_TTL_MINUTES = 30;

    public BlogResponse getBySlug(String slug) {
        String cacheKey = CACHE_PREFIX + slug;

        // Check cache first
        BlogResponse cached = (BlogResponse) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }

        // Fetch from DB — published only
        BlogResponse response = blogRepository
            .findBySlugAndStatusAndDeletedAtIsNull(slug, "PUBLISHED")
            .map(mapper::toResponse)
            .orElseThrow(() -> new ResourceNotFoundException("Blog not found: " + slug));

        // Store in cache
        redisTemplate.opsForValue().set(cacheKey, response, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        return response;
    }
}
```

**Cache rules:**
- Cache invalidated on every write — publish, update, delete
- Cache keys follow pattern: `{module}:{visibility}:{identifier}`
- TTL defined per data type — see CLAUDE.md for project-specific TTLs
- Redis is cache only — PostgreSQL is single source of truth
- Never cache admin responses — only public read-heavy endpoints

---

## Rate Limiting

Apply rate limiting on all public POST endpoints using Redis:

```java
@Component
@RequiredArgsConstructor
public class RateLimitService {

    private final RedisTemplate<String, String> redisTemplate;

    public void checkRateLimit(String ip, String endpoint, int maxRequests, int windowMinutes) {
        String key = "ratelimit:" + endpoint + ":" + ip;
        Long count = redisTemplate.opsForValue().increment(key);

        if (count == 1) {
            redisTemplate.expire(key, windowMinutes, TimeUnit.MINUTES);
        }

        if (count > maxRequests) {
            throw new RateLimitExceededException(
                "Too many requests. Try again in " + windowMinutes + " minutes."
            );
        }
    }
}
```

---

## Flyway Migration Rules

**Naming convention:**
```
V1__init_schema.sql
V2__create_blog_tables.sql
V3__create_case_study_tables.sql
V4__create_testimonials.sql
V5__create_stats_tools_settings.sql
V6__create_consultation_contact.sql
```

**Table convention — every table must have:**
```sql
CREATE TABLE blogs (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title             VARCHAR(255) NOT NULL,
    slug              VARCHAR(255) NOT NULL UNIQUE,
    content           TEXT NOT NULL,                 -- TEXT for long content, never VARCHAR
    status            VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    category_id       UUID REFERENCES blog_categories(id),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),   -- TIMESTAMPTZ always, never TIMESTAMP
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by        UUID,
    updated_by        UUID,
    deleted_at        TIMESTAMPTZ                          -- NULL = active, soft delete
);

-- Indexes mandatory on filtered columns
CREATE INDEX idx_blogs_slug       ON blogs(slug);
CREATE INDEX idx_blogs_status     ON blogs(status);
CREATE INDEX idx_blogs_category   ON blogs(category_id);
CREATE INDEX idx_blogs_active     ON blogs(deleted_at) WHERE deleted_at IS NULL;
```

**Migration rules:**
- One migration file per task or logical change
- Never modify an existing migration — always create a new versioned file
- All timestamps: `TIMESTAMPTZ` — never `TIMESTAMP`
- All monetary/decimal: `NUMERIC(19,4)` — never `FLOAT` or `DOUBLE PRECISION`
- Always add index on every column used in `WHERE`, `JOIN`, or `ORDER BY`
- `spring.jpa.hibernate.ddl-auto=validate` always — Flyway manages schema

---

## Naming Conventions

| Type | Convention | Example |
|---|---|---|
| Entity | `<Name>Entity` | `BlogEntity`, `CaseStudyEntity` |
| Repository | `<Name>Repository` | `BlogRepository` |
| Service | `<Name>Service` | `BlogService`, `BlogPublicService` |
| Admin Controller | `<Name>AdminController` | `BlogAdminController` |
| Public Controller | `<Name>PublicController` | `BlogPublicController` |
| Request DTO | `<Name>CreateRequest` / `<Name>UpdateRequest` | `BlogCreateRequest` |
| Response DTO | `<Name>Response` | `BlogResponse` |
| Mapper | `<Name>Mapper` | `BlogMapper` |
| Enum | `<Name>Status` / `<Name>Type` | `BlogStatus`, `ToolCategory` |

---

## application.yml Structure

```yaml
server:
  port: ${APP_PORT:8080}

spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME}
    username: ${DB_USER}
    password: ${DB_PASS}
  jpa:
    hibernate:
      ddl-auto: validate          # Flyway manages schema — never update or create
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        jdbc:
          time_zone: UTC
  flyway:
    enabled: true
    locations: classpath:db/migration
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      timeout: 2000ms

app:
  jwt:
    secret: ${JWT_SECRET}
    expiration-ms: ${JWT_EXPIRATION_MS:3600000}
    refresh-expiration-ms: ${JWT_REFRESH_EXPIRATION_MS:604800000}
  cors:
    allowed-origins: ${CORS_ORIGINS:http://localhost:3000}
  file:
    max-size-mb: 5
    allowed-types: image/jpeg,image/png,image/webp
```

---

## ✅ Always Do This

1. `@Valid` on every `@RequestBody` in controllers
2. `@PreAuthorize` on every controller method — no unprotected endpoints
3. Wrap every response in `ApiResponse<T>`
4. `@Transactional` on service class — `readOnly = true` on read methods
5. `Instant` (UTC) for all timestamps — never `LocalDateTime`
6. `BigDecimal` for all monetary and quantity fields — never `float` or `double`
7. Soft delete only — `entity.setDeletedAt(Instant.now())` — never `repository.delete()`
8. Include `deletedAtIsNull` in every repository query
9. Invalidate Redis cache on every write operation
10. Create a Flyway migration for every schema change
11. Return DTOs from service — never entities
12. SLF4J logger — `log.info / warn / error` — never `System.out.println`
13. Generate slug from title in service layer — unique and URL-safe
14. Calculate reading time from content word count in service layer
15. Rate limit all public POST endpoints via Redis

---

## ❌ Never Do This

1. Business logic in controller — service layer only
2. Return entity directly from controller or service — DTOs only
3. Use `findById()` without additional scope filters
4. Return deleted records — all queries must filter `deletedAt IS NULL`
5. Use `LocalDateTime` — always `Instant` (UTC)
6. Use `float` or `double` for money or quantities — always `BigDecimal`
7. Use `spring.jpa.hibernate.ddl-auto=update` or `create` — Flyway only
8. Hardcode JWT secret or any credentials — environment variables only
9. Log JWT token content anywhere
10. Return JWT token in error responses
11. Skip `@PreAuthorize` on any endpoint — even "harmless" ones
12. Modify existing Flyway migrations — create a new versioned file
13. Use `System.out.println` — SLF4J only
14. Cache admin responses — cache public read endpoints only
15. Hard delete records — soft delete only