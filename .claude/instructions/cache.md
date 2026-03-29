# Cache Handling Instructions

> This file is reusable across all projects.
> The AI agent must read this file when it detects caching is needed.
> The agent must check stack.md AND configurations.md FIRST before writing any cache code.

---

## Step 0 — Two Checks Before Anything Else

### Check 1 — Does this project use cache? (stack.md)

Before writing any cache code, the agent reads `project/stack.md` and checks:

```
Is Redis listed under Infrastructure or Backend?     → Use Redis patterns
Is Memcached listed under Infrastructure or Backend? → Use Memcached patterns
Is neither listed?                                   → DO NOT add any caching
                                                       Ignore this file entirely
```

**If cache is not in stack.md — no cache code is written. No exceptions.**

---

### Check 2 — Is cache already configured? (configurations.md)

After confirming cache is in stack.md, the agent reads `.claude/configurations.md`
and checks the Cache Configuration section:

```
Is Cache status ✅ READY?    → Config exists — use it directly, do not re-setup
Is Cache status ⚠️ PARTIAL?  → Complete the missing parts, then mark ✅ READY
Is Cache status ❌ MISSING?  → Set up cache config first, then mark ✅ READY
```

**After completing cache setup — agent must update configurations.md:**
```
Cache Configuration: ✅ READY
  Provider: Redis / Memcached
  Config class: config/RedisConfig.java
  Cache names: stats (24h), settings (1h), tools (1h), blogs (10min), ...
  Environment variables: REDIS_HOST ✅, REDIS_PORT ✅
  Set up in task: [task ID]
```

---

## Agent Decision Rule — When to Apply Caching

After confirming cache is in stack.md, the agent detects caching need
automatically from signals in the spec or task:

### 🔴 Always Cache — Apply Caching

| Signal in Spec / Task | Why Cache Is Required |
|---|---|
| "public endpoint" + "read heavy" | Many users reading same data — cache it |
| "stats" / "counters" / "numbers bar" | Aggregation queries are expensive — cache result |
| "site settings" / "contact info" / "social links" | Rarely changes — cache aggressively |
| "tools list" / "tech stack display" | Static display data — cache for hours |
| "testimonials" / "featured items" | Changes infrequently — cache for hours |
| "home page data" | Multiple sources merged — cache the merge result |
| "blog list" / "case study list" (public) | Read by many visitors — cache per filter set |
| "single blog by slug" / "single case study" | Repeated reads of same resource — cache |
| "categories" / "tags" / "filters" | Almost never changes — cache aggressively |
| "dashboard summary" / "KPI" | Heavy aggregation — cache with short TTL |

### 🟡 Cache Conditionally — Evaluate First

| Signal | Evaluate This |
|---|---|
| "admin list" | Usually NO — admins see live data |
| "search results" | YES if same query repeated often, NO if unique |
| "user-specific data" | YES with user-scoped key, be careful with size |
| "paginated list" | YES — cache per page + filters combination |
| "filtered results" | YES — cache key must include all filter params |

### 🟢 Never Cache — Skip Caching

| Signal | Why No Cache |
|---|---|
| "create" / "submit" / "POST" | Write operations — never cache |
| "update" / "edit" / "PUT/PATCH" | Write operations — never cache |
| "delete" / "remove" | Write operations — never cache |
| "admin panel data" | Admins need live data — stale data is dangerous |
| "consultation requests" (admin view) | Must show latest — no cache |
| "contact messages" (admin view) | Must show latest — no cache |
| "auth" / "login" / "token" | Security-critical — never cache responses |
| "real-time" / "live" / "streaming" | By definition cannot be cached |

---

## Decision Flowchart

```
stack.md has cache defined?
   NO  → Stop. Do not write any cache code.
   YES ↓

Is this a write operation (POST/PUT/PATCH/DELETE)?
   YES → No cache. Invalidate related cache keys instead.
   NO  ↓

Is this admin-only data?
   YES → No cache. Admins need live data.
   NO  ↓

Is the same data requested by multiple users?
   YES → Cache it.
   NO  ↓

Is the query expensive (aggregation, joins, external call)?
   YES → Cache it.
   NO  → Cache optional — use judgment on request volume.
```

---

## Cache Provider Setup

### Redis Setup (Spring Boot)

Used when `stack.md` lists Redis:

```java
// config/RedisConfig.java
@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // JSON serialization — human-readable in Redis
        Jackson2JsonRedisSerializer<Object> serializer =
            new Jackson2JsonRedisSerializer<>(Object.class);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))           // default TTL
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair
                    .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair
                    .fromSerializer(new GenericJackson2JsonRedisSerializer()))
            .disableCachingNullValues();

        // Per-cache TTL configuration
        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
        cacheConfigs.put("stats",        defaultConfig.entryTtl(Duration.ofHours(24)));
        cacheConfigs.put("settings",     defaultConfig.entryTtl(Duration.ofHours(1)));
        cacheConfigs.put("tools",        defaultConfig.entryTtl(Duration.ofHours(1)));
        cacheConfigs.put("testimonials", defaultConfig.entryTtl(Duration.ofHours(1)));
        cacheConfigs.put("blogs",        defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigs.put("blog",         defaultConfig.entryTtl(Duration.ofMinutes(30)));
        cacheConfigs.put("case-studies", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigs.put("case-study",   defaultConfig.entryTtl(Duration.ofMinutes(30)));
        cacheConfigs.put("categories",   defaultConfig.entryTtl(Duration.ofHours(1)));

        return RedisCacheManager.builder(factory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigs)
            .build();
    }
}
```

```yaml
# application.yml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      timeout: 2000ms
      lettuce:
        pool:
          max-active: 10
          max-idle: 5
          min-idle: 2
  cache:
    type: redis
```

---

### Memcached Setup (Spring Boot)

Used when `stack.md` lists Memcached:

```java
// config/MemcachedConfig.java
@Configuration
@EnableCaching
public class MemcachedConfig {

    @Value("${memcached.host:localhost}")
    private String host;

    @Value("${memcached.port:11211}")
    private int port;

    @Bean
    public MemcachedClient memcachedClient() throws IOException {
        return new MemcachedClient(new InetSocketAddress(host, port));
    }

    @Bean
    public CacheManager cacheManager(MemcachedClient client) {
        MemcachedCacheManager manager = new MemcachedCacheManager(client);
        manager.setDefaultExpiration(600);    // 10 minutes default TTL in seconds

        Map<String, Integer> expirations = new HashMap<>();
        expirations.put("stats",        86400);  // 24 hours
        expirations.put("settings",     3600);   // 1 hour
        expirations.put("tools",        3600);   // 1 hour
        expirations.put("testimonials", 3600);   // 1 hour
        expirations.put("blogs",        600);    // 10 minutes
        expirations.put("blog",         1800);   // 30 minutes
        expirations.put("case-studies", 600);    // 10 minutes
        expirations.put("case-study",   1800);   // 30 minutes
        expirations.put("categories",   3600);   // 1 hour
        manager.setExpirations(expirations);

        return manager;
    }
}
```

```yaml
# application.yml
memcached:
  host: ${MEMCACHED_HOST:localhost}
  port: ${MEMCACHED_PORT:11211}

spring:
  cache:
    type: none    # Memcached managed manually — not via Spring auto-config
```

> **Key difference**: Redis supports complex data types, pub/sub, and persistence.
> Memcached is simpler, faster for pure key-value caching, no persistence.
> Use whichever is defined in `stack.md` — never both.

---

## Cache Key Convention

**Pattern: `{module}:{visibility}:{identifier}`**

| Data | Cache Key Pattern | Example |
|---|---|---|
| Public blog list | `blogs:public:list:{params-hash}` | `blogs:public:list:a3f9c2` |
| Single blog | `blogs:public:{slug}` | `blogs:public:aws-guide` |
| Featured blogs | `blogs:public:featured` | `blogs:public:featured` |
| Public case study list | `casestudies:public:list:{params-hash}` | `casestudies:public:list:b7d1e4` |
| Single case study | `casestudies:public:{slug}` | `casestudies:public:fintech-migration` |
| Featured case studies | `casestudies:public:featured` | `casestudies:public:featured` |
| Testimonials | `testimonials:public` | `testimonials:public` |
| Tools by category | `tools:public:{category}` | `tools:public:AWS_SERVICE` |
| All tools | `tools:public:all` | `tools:public:all` |
| Site stats | `stats:public` | `stats:public` |
| Site settings | `settings:public` | `settings:public` |
| Blog categories | `categories:blog:public` | `categories:blog:public` |
| Case study categories | `categories:casestudy:public` | `categories:casestudy:public` |

**Rules for cache keys:**
- Always lowercase
- Always use `:` as separator — never `/` or `.`
- Always include visibility scope (`public` / `admin`)
- For filtered lists — hash the filter params: `{module}:public:list:{hash(params)}`
- Never include sensitive data (email, token) in cache key

---

## TTL Reference Table

| Cache Name | TTL | Reason |
|---|---|---|
| `stats` | 24 hours | Updated manually by admin — very stable |
| `settings` | 1 hour | Contact info rarely changes |
| `tools` | 1 hour | Tech stack rarely changes |
| `testimonials` | 1 hour | Added/removed infrequently |
| `categories` | 1 hour | Category list almost never changes |
| `blog` (single) | 30 minutes | Content of published blog is stable |
| `case-study` (single) | 30 minutes | Content of published case study is stable |
| `blogs` (list) | 10 minutes | New blogs published occasionally |
| `case-studies` (list) | 10 minutes | New case studies published occasionally |
| Rate limit keys | Per config | Defined in async.md / rate limit config |
| JWT blacklist | Until token expiry | Security — must expire with token |

---

## Cache Implementation Patterns

### Pattern 1 — Spring @Cacheable (Simple Read)

Use when: single result, stable key, no complex invalidation needed.

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class StatsPublicService {

    private final SiteStatsRepository statsRepository;

    // Cache result for 24 hours — key is fixed (only one row of stats)
    @Cacheable(value = "stats", key = "'public'")
    @Transactional(readOnly = true)
    public SiteStatsResponse getPublicStats() {
        log.debug("Cache miss — loading stats from DB");
        return statsRepository.findFirst()
            .map(mapper::toResponse)
            .orElse(new SiteStatsResponse());
    }
}

@Service
@RequiredArgsConstructor
public class SettingsPublicService {

    @Cacheable(value = "settings", key = "'public'")
    @Transactional(readOnly = true)
    public Map<String, String> getPublicSettings() {
        return settingsRepository.findPublicSettings()
            .stream()
            .collect(Collectors.toMap(
                SiteSettingEntity::getKey,
                SiteSettingEntity::getValue
            ));
    }
}
```

---

### Pattern 2 — Manual Redis Cache (Complex Keys / Filtered Lists)

Use when: cache key depends on dynamic query params (filters, pagination).

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class BlogPublicService {

    private final BlogRepository blogRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final BlogMapper mapper;

    private static final String CACHE_PREFIX = "blogs:public:";
    private static final long LIST_TTL_MINUTES = 10;
    private static final long SINGLE_TTL_MINUTES = 30;

    @Transactional(readOnly = true)
    public PagedResponse<BlogResponse> getPublicBlogs(BlogFilters filters, Pageable pageable) {
        String cacheKey = CACHE_PREFIX + "list:" + buildParamsHash(filters, pageable);

        // Check cache first
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.debug("Cache hit: {}", cacheKey);
            return (PagedResponse<BlogResponse>) cached;
        }

        // Cache miss — fetch from DB
        log.debug("Cache miss: {}", cacheKey);
        Page<BlogEntity> page = blogRepository.findPublished(filters, pageable);
        PagedResponse<BlogResponse> response = buildPagedResponse(page);

        // Store in cache
        redisTemplate.opsForValue().set(cacheKey, response, LIST_TTL_MINUTES, TimeUnit.MINUTES);
        return response;
    }

    @Transactional(readOnly = true)
    public BlogResponse getBySlug(String slug) {
        String cacheKey = CACHE_PREFIX + slug;

        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.debug("Cache hit: {}", cacheKey);
            return (BlogResponse) cached;
        }

        log.debug("Cache miss: {}", cacheKey);
        BlogResponse response = blogRepository
            .findBySlugAndStatusAndDeletedAtIsNull(slug, BlogStatus.PUBLISHED)
            .map(mapper::toResponse)
            .orElseThrow(() -> new ResourceNotFoundException("Blog not found: " + slug));

        redisTemplate.opsForValue().set(cacheKey, response, SINGLE_TTL_MINUTES, TimeUnit.MINUTES);
        return response;
    }

    // Build stable hash from filter params — same params = same cache key
    private String buildParamsHash(BlogFilters filters, Pageable pageable) {
        String params = String.format(
            "cat=%s&tag=%s&page=%d&size=%d&sort=%s",
            filters.getCategory() != null ? filters.getCategory() : "",
            filters.getTag() != null ? filters.getTag() : "",
            pageable.getPageNumber(),
            pageable.getPageSize(),
            pageable.getSort().toString()
        );
        return Integer.toHexString(params.hashCode());
    }
}
```

---

### Pattern 3 — Cache Invalidation on Write

**Every write operation must invalidate related cache keys.**
This is the most critical rule — stale cache = wrong data shown to users.

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class BlogAdminService {

    private final BlogRepository blogRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public BlogResponse publish(UUID id) {
        BlogEntity entity = blogRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new ResourceNotFoundException("Blog not found"));

        entity.setStatus(BlogStatus.PUBLISHED);
        entity.setPublishedAt(Instant.now());
        entity = blogRepository.save(entity);

        // Invalidate ALL blog public cache — new blog is now visible
        invalidateBlogCache(entity.getSlug());

        log.info("Blog published and cache invalidated: id={}, slug={}", id, entity.getSlug());
        return mapper.toResponse(entity);
    }

    public BlogResponse update(UUID id, BlogUpdateRequest request) {
        BlogEntity entity = blogRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new ResourceNotFoundException("Blog not found"));

        mapper.updateEntity(entity, request);
        entity = blogRepository.save(entity);

        // Invalidate specific blog + all lists
        invalidateBlogCache(entity.getSlug());

        return mapper.toResponse(entity);
    }

    public void delete(UUID id) {
        BlogEntity entity = blogRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new ResourceNotFoundException("Blog not found"));

        entity.setDeletedAt(Instant.now());
        blogRepository.save(entity);

        // Invalidate — deleted blog must not appear in public lists
        invalidateBlogCache(entity.getSlug());
    }

    private void invalidateBlogCache(String slug) {
        // Invalidate specific blog by slug
        redisTemplate.delete("blogs:public:" + slug);

        // Invalidate all list pages — any filter combination
        Set<String> listKeys = redisTemplate.keys("blogs:public:list:*");
        if (listKeys != null && !listKeys.isEmpty()) {
            redisTemplate.delete(listKeys);
            log.debug("Invalidated {} blog list cache keys", listKeys.size());
        }

        // Invalidate featured
        redisTemplate.delete("blogs:public:featured");
    }
}
```

**Invalidation map — what to invalidate on each write:**

| Write Operation | Invalidate These Keys |
|---|---|
| Blog published / updated / deleted | `blogs:public:{slug}`, `blogs:public:list:*`, `blogs:public:featured` |
| Case study published / updated / deleted | `casestudies:public:{slug}`, `casestudies:public:list:*`, `casestudies:public:featured` |
| Testimonial added / updated / deleted | `testimonials:public` |
| Stats updated | `stats:public` |
| Tool added / updated / deleted | `tools:public:*` |
| Settings updated | `settings:public` |
| Blog category added / updated / deleted | `categories:blog:public`, `blogs:public:list:*` |
| Case study category changed | `categories:casestudy:public`, `casestudies:public:list:*` |

---

### Pattern 4 — @CacheEvict (Spring Annotation Invalidation)

Use when: Spring `@Cacheable` is used and invalidation is straightforward.

```java
@Service
@RequiredArgsConstructor
public class StatsAdminService {

    private final SiteStatsRepository statsRepository;

    // Invalidate stats cache when admin updates stats
    @CacheEvict(value = "stats", key = "'public'")
    @Transactional
    public SiteStatsResponse updateStats(StatsUpdateRequest request) {
        SiteStatsEntity entity = statsRepository.findFirst()
            .orElse(new SiteStatsEntity());

        entity.setProjectsDelivered(request.getProjectsDelivered());
        entity.setClientsServed(request.getClientsServed());
        entity.setAvgCostReduction(request.getAvgCostReduction());
        entity.setUptimeRate(request.getUptimeRate());

        return mapper.toResponse(statsRepository.save(entity));
        // @CacheEvict removes "stats::'public'" key automatically
    }
}

@Service
public class SettingsAdminService {

    @CacheEvict(value = "settings", key = "'public'")
    @Transactional
    public void updateSettings(Map<String, String> settings) {
        settings.forEach((key, value) -> {
            SiteSettingEntity entity = settingsRepository.findByKey(key)
                .orElse(new SiteSettingEntity(key));
            entity.setValue(value);
            settingsRepository.save(entity);
        });
    }
}
```

---

### Pattern 5 — Cache Warmup (Pre-load on Startup)

Use when: critical public data must be available instantly on first request.

```java
// cache/CacheWarmupService.java
@Component
@RequiredArgsConstructor
@Slf4j
public class CacheWarmupService {

    private final BlogPublicService blogPublicService;
    private final StatsPublicService statsPublicService;
    private final SettingsPublicService settingsPublicService;
    private final TestimonialsPublicService testimonialsPublicService;

    // Run after application starts — pre-load critical caches
    @EventListener(ApplicationReadyEvent.class)
    public void warmUpCaches() {
        log.info("Starting cache warmup...");

        try {
            statsPublicService.getPublicStats();
            log.debug("Stats cache warmed up");
        } catch (Exception e) {
            log.warn("Failed to warm up stats cache", e);
        }

        try {
            settingsPublicService.getPublicSettings();
            log.debug("Settings cache warmed up");
        } catch (Exception e) {
            log.warn("Failed to warm up settings cache", e);
        }

        try {
            testimonialsPublicService.getActiveTestimonials();
            log.debug("Testimonials cache warmed up");
        } catch (Exception e) {
            log.warn("Failed to warm up testimonials cache", e);
        }

        log.info("Cache warmup completed");
    }
}
```

---

## Cache Implementation — Memcached Variant

When `stack.md` specifies Memcached instead of Redis:

```java
// The service logic is identical — only the cache client differs
@Service
@RequiredArgsConstructor
@Slf4j
public class BlogPublicService {

    private final BlogRepository blogRepository;
    private final MemcachedClient memcachedClient;
    private final BlogMapper mapper;

    private static final int LIST_TTL_SECONDS  = 600;   // 10 minutes
    private static final int SINGLE_TTL_SECONDS = 1800;  // 30 minutes

    @Transactional(readOnly = true)
    public BlogResponse getBySlug(String slug) {
        String cacheKey = "blogs_public_" + slug;  // Memcached: use _ not : in keys

        try {
            Object cached = memcachedClient.get(cacheKey);
            if (cached != null) {
                log.debug("Cache hit: {}", cacheKey);
                return (BlogResponse) cached;
            }
        } catch (Exception e) {
            log.warn("Memcached get failed for key: {} — falling back to DB", cacheKey, e);
        }

        BlogResponse response = blogRepository
            .findBySlugAndStatusAndDeletedAtIsNull(slug, BlogStatus.PUBLISHED)
            .map(mapper::toResponse)
            .orElseThrow(() -> new ResourceNotFoundException("Blog not found: " + slug));

        try {
            memcachedClient.set(cacheKey, SINGLE_TTL_SECONDS, response);
        } catch (Exception e) {
            log.warn("Memcached set failed for key: {}", cacheKey, e);
            // Non-fatal — continue without cache
        }

        return response;
    }

    // Memcached does not support key pattern matching (no keys("blogs:*"))
    // Must track keys to invalidate or use TTL-only invalidation strategy
    public void invalidateBlogCache(String slug) {
        try {
            memcachedClient.delete("blogs_public_" + slug);
            memcachedClient.delete("blogs_public_featured");
            // Note: Cannot invalidate list keys by pattern in Memcached
            // List caches expire naturally via TTL — 10 minutes max staleness
            log.debug("Blog cache invalidated for slug: {}", slug);
        } catch (Exception e) {
            log.warn("Memcached delete failed for slug: {}", slug, e);
        }
    }
}
```

**Memcached key rules (different from Redis):**
- No colons `:` — use underscores `_`
- Max key length: 250 characters
- No pattern-based deletion — TTL is the only bulk invalidation strategy
- Always wrap in try-catch — Memcached failure must never break the request

---

## Frontend Cache — RTK Query

RTK Query has built-in cache on the frontend. Configure per-endpoint:

```typescript
// features/blog/api.ts
export const blogApi = createApi({
  reducerPath: 'blogApi',
  baseQuery: axiosBaseQuery(),
  tagTypes: ['Blog', 'BlogList', 'BlogCategory'],
  keepUnusedDataFor: 300,    // global default: 5 minutes

  endpoints: (builder) => ({

    // Public blogs list — cache 10 minutes
    getPublicBlogs: builder.query<PagedResponse<BlogResponse>, BlogFilters>({
      query: (filters) => ({ url: '/public/blogs', params: filters }),
      keepUnusedDataFor: 600,       // 10 minutes — matches backend TTL
      providesTags: ['BlogList'],
    }),

    // Single blog — cache 30 minutes
    getPublicBlogBySlug: builder.query<BlogResponse, string>({
      query: (slug) => ({ url: `/public/blogs/${slug}` }),
      keepUnusedDataFor: 1800,      // 30 minutes — matches backend TTL
      providesTags: (result, error, slug) => [{ type: 'Blog', id: slug }],
    }),

    // Stats — cache 24 hours (almost never changes)
    getPublicStats: builder.query<SiteStatsResponse, void>({
      query: () => ({ url: '/public/stats' }),
      keepUnusedDataFor: 86400,     // 24 hours
      providesTags: ['Stats'],
    }),

    // Admin blogs — short cache — admins need live data
    getAdminBlogs: builder.query<PagedResponse<BlogResponse>, BlogFilters>({
      query: (filters) => ({ url: '/admin/blogs', params: filters }),
      keepUnusedDataFor: 30,        // 30 seconds only
      providesTags: ['BlogList'],
    }),

    // Write operations — invalidate tags
    publishBlog: builder.mutation<BlogResponse, string>({
      query: (id) => ({ url: `/admin/blogs/${id}/publish`, method: 'PATCH' }),
      invalidatesTags: ['BlogList', 'Blog'],  // Clears both list and single caches
    }),

    createBlog: builder.mutation<BlogResponse, BlogCreateRequest>({
      query: (body) => ({ url: '/admin/blogs', method: 'POST', body }),
      invalidatesTags: ['BlogList'],
    }),

    updateBlog: builder.mutation<BlogResponse, { id: string; body: BlogUpdateRequest }>({
      query: ({ id, body }) => ({ url: `/admin/blogs/${id}`, method: 'PUT', body }),
      invalidatesTags: (result, error, { id }) => [
        'BlogList',
        { type: 'Blog', id: result?.slug },
      ],
    }),

    deleteBlog: builder.mutation<void, string>({
      query: (id) => ({ url: `/admin/blogs/${id}`, method: 'DELETE' }),
      invalidatesTags: ['BlogList', 'Blog'],
    }),
  }),
});
```

**Frontend TTL must match or be less than backend TTL:**
```
Backend TTL  ≥  Frontend TTL
30 min (blog)   ≥  30 min (RTK Query)  ✅
10 min (list)   ≥  10 min (RTK Query)  ✅
24 hr (stats)   ≥  24 hr  (RTK Query)  ✅
```

---

## Testing Cache — Backend

### Test — Cache Hit Returns Cached Data

```java
@Test
@DisplayName("getPublicStats() — second call — returns cached result without DB hit")
void getPublicStats_secondCall_returnsCachedResult() {
    // First call — populates cache
    statsPublicService.getPublicStats();

    // Second call — should hit cache, not DB
    statsPublicService.getPublicStats();

    // Repository called only once — second call from cache
    verify(statsRepository, times(1)).findFirst();
}
```

### Test — Cache Invalidated on Write

```java
@Test
@DisplayName("updateStats() — after update — cache evicted and next read hits DB")
void updateStats_afterUpdate_cacheEvicted() {
    // Populate cache
    statsPublicService.getPublicStats();
    verify(statsRepository, times(1)).findFirst();

    // Update — should evict cache
    statsAdminService.updateStats(new StatsUpdateRequest());

    // Next read — must hit DB again (cache was evicted)
    statsPublicService.getPublicStats();
    verify(statsRepository, times(2)).findFirst();    // called twice — cache was cleared
}
```

### Test — Publish Blog Invalidates Public Cache

```java
@Test
@DisplayName("publishBlog() — invalidates blog public cache")
void publishBlog_invalidatesCache() throws Exception {
    // Create and cache a blog
    BlogCreateRequest request = factory.validBlogRequest();
    request.setCategoryId(categoryId);

    String createResponse = mockMvc.perform(adminPost("/api/admin/blogs", request))
        .andReturn().getResponse().getContentAsString();
    String id = objectMapper.readTree(createResponse).path("data").path("id").asText();

    // Warm up public list cache
    mockMvc.perform(publicGet("/api/public/blogs"));

    // Publish — should invalidate public cache
    mockMvc.perform(adminPatch("/api/admin/blogs/" + id + "/publish"))
        .andExpect(status().isOk());

    // Public list should now include the published blog (fresh from DB)
    mockMvc.perform(publicGet("/api/public/blogs"))
        .andExpect(jsonPath("$.data[*].title", hasItem(request.getTitle())));
}
```

### Test — Cache Miss Falls Back to DB

```java
@Test
@DisplayName("getBySlug() — cold cache — falls back to DB and caches result")
void getBySlug_coldCache_fetchesFromDbAndCaches() {
    // Clear cache to ensure cold start
    redisTemplate.delete("blogs:public:aws-guide");

    // First call — DB hit
    BlogResponse first = blogPublicService.getBySlug("aws-guide");

    // Verify cached now
    Object cached = redisTemplate.opsForValue().get("blogs:public:aws-guide");
    assertThat(cached).isNotNull();

    // Second call — cache hit
    BlogResponse second = blogPublicService.getBySlug("aws-guide");

    assertThat(first.getSlug()).isEqualTo(second.getSlug());
    verify(blogRepository, times(1))    // DB called only once
        .findBySlugAndStatusAndDeletedAtIsNull(anyString(), any());
}
```

---

## Testing Cache — Frontend (Playwright)

### Test — Cached Data Loads Without Network Call

```javascript
// Scenario: Blog list loaded twice — second load from RTK Query cache (no API call)
const apiCalls = [];
page.on('request', req => {
  if (req.url().includes('/api/public/blogs')) {
    apiCalls.push(req.url());
  }
});

// First load — triggers API call
await playwright_navigate({ url: 'http://localhost:5173/blog' });
await page.waitForLoadState('networkidle');
const firstLoadCalls = apiCalls.length;
console.log('First load API calls:', firstLoadCalls);

// Navigate away
await playwright_navigate({ url: 'http://localhost:5173' });
await page.waitForLoadState('networkidle');

// Navigate back — RTK Query should serve from cache
await playwright_navigate({ url: 'http://localhost:5173/blog' });
await page.waitForLoadState('networkidle');
const secondLoadCalls = apiCalls.length - firstLoadCalls;

console.log('Second load API calls:', secondLoadCalls === 0 ? '✅ 0 (from cache)' : `⚠️ ${secondLoadCalls}`);
await playwright_screenshot({ name: 'test-X.Y-cache-second-load' });
```

### Test — Cache Invalidated After Mutation

```javascript
// Scenario: After publishing a blog, public list reflects the change (cache cleared)
const apiCallsAfterPublish = [];

// Login as admin and publish a blog
await loginAsAdmin(page);
await playwright_navigate({ url: 'http://localhost:5173/admin/blogs' });

// Publish a draft blog
await playwright_click({ selector: '[data-testid="blog-publish-btn"]:first-child' });
await page.waitForSelector('[data-testid="success-toast"]', { timeout: 5000 });
console.log('Blog published:', '✅');

// Monitor subsequent public API calls
page.on('request', req => {
  if (req.url().includes('/api/public/blogs')) {
    apiCallsAfterPublish.push(req.url());
  }
});

// Public list should refetch (RTK Query tag invalidated)
await playwright_navigate({ url: 'http://localhost:5173/blog' });
await page.waitForLoadState('networkidle');

console.log('Public list refetched after publish:',
  apiCallsAfterPublish.length > 0 ? '✅' : '❌ Still serving stale cache');

await playwright_screenshot({ name: 'test-X.Y-cache-after-publish' });
```

---

## Cache Failure Handling

Cache must NEVER break the application — always fall back to DB:

```java
@Transactional(readOnly = true)
public BlogResponse getBySlug(String slug) {
    String cacheKey = "blogs:public:" + slug;

    // Try cache — if it fails, fall back silently
    try {
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return (BlogResponse) cached;
        }
    } catch (Exception e) {
        // Cache failure is non-fatal — log and continue to DB
        log.warn("Cache read failed for key: {} — falling back to DB", cacheKey, e);
    }

    // Always falls back to DB
    BlogResponse response = blogRepository
        .findBySlugAndStatusAndDeletedAtIsNull(slug, BlogStatus.PUBLISHED)
        .map(mapper::toResponse)
        .orElseThrow(() -> new ResourceNotFoundException("Blog not found: " + slug));

    // Try to cache — if it fails, return result anyway
    try {
        redisTemplate.opsForValue().set(cacheKey, response, 30, TimeUnit.MINUTES);
    } catch (Exception e) {
        log.warn("Cache write failed for key: {}", cacheKey, e);
    }

    return response;
}
```

---

## ✅ Always Do This

1. Check `stack.md` first — only add cache if Redis or Memcached is defined
2. Always wrap cache reads in try-catch — cache failure must not break the request
3. Always wrap cache writes in try-catch — same reason
4. Always invalidate related cache keys on every write operation
5. Always use the key convention: `{module}:{visibility}:{identifier}`
6. Always set TTL on every cache entry — never cache without expiry
7. Match frontend RTK Query TTL to backend TTL — never longer
8. Cache public endpoints — never cache admin endpoints
9. Log cache hits and misses at DEBUG level — never at INFO (too noisy)
10. Log cache invalidation at DEBUG level with key count
11. Warm up critical caches on application startup
12. Always test cache invalidation — not just cache hit

---

## ❌ Never Do This

1. Add cache code if stack.md does not list Redis or Memcached
2. Cache admin endpoints — admins always need live data
3. Cache write operations (POST/PUT/PATCH/DELETE)
4. Cache without TTL — entries must always expire
5. Include sensitive data (email, token, password) in cache key or value
6. Let cache failure throw an exception to the client — always fallback to DB
7. Use Memcached key syntax (underscores) with Redis or vice versa
8. Invalidate only the specific slug — always also invalidate list keys
9. Set frontend RTK Query TTL longer than backend TTL — stale data risk
10. Skip cache invalidation on delete — deleted items must not appear
11. Use `keys("*")` in production for Redis — blocks the server
12. Cache null values — `disableCachingNullValues()` must always be set