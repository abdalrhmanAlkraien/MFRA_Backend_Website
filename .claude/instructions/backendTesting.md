# Backend Testing Instructions

> This file is reusable across all projects.
> The AI agent must read this file before writing any backend test.
> Project-specific details (package names, endpoints, credentials) are in `.claude/CLAUDE.md`.

---

## The Non-Negotiable Rule — Enforced by Hard Stop Gates

**Write the Java test files FIRST. Run `mvn verify` SECOND.**

The AI agent has TWO automatic gates that block execution if tests are missing:

**Gate 1 — Step 7b (before test runner):**
Checks that test files EXIST and have `@Test` methods.
If files are missing or have 0 methods → agent STOPS and returns to write them.

**Gate 2 — Step 9b (after test runner):**
Checks that ALL tests PASS (100%).
If any test fails → agent STOPS and fixes before marking task complete.

These gates cannot be bypassed. The agent cannot reach Step 8 or Step 10
without passing them.

An empty `src/test/` package means Gate 1 blocks execution.
A failing test means Gate 2 blocks completion.

**Before running `mvn clean verify`, these files must exist:**

```
src/test/java/com/<pkg>/<module>/
├── <Module>ServiceTest.java      ← unit tests — @Test methods required
└── <Module>ControllerTest.java   ← integration tests — @Test methods required
```

Verify before running:
```bash
ls src/test/java/com/<pkg>/<module>/
grep -c "@Test" src/test/java/com/<pkg>/<module>/<Module>ServiceTest.java
grep -c "@Test" src/test/java/com/<pkg>/<module>/<Module>ControllerTest.java
```

If either count is 0 → write the test methods before running mvn.

---

## Testing Stack

- **Unit Tests**: JUnit 5 + Mockito
- **Integration Tests**: `@SpringBootTest` + MockMvc + Testcontainers
- **Repository Tests**: `@DataJpaTest` + Testcontainers (PostgreSQL)
- **Coverage**: JaCoCo — minimum 80% per module
- **Build**: Maven (`mvn clean verify`)
- **Containers**: Testcontainers (PostgreSQL + Redis)

---

## Testing Workflow

```
1. Complete Task Implementation
         ↓
2. Read Current Task from systemTasks.md
   → identify module name and task ID
         ↓
3. Read Module Spec from SpecKit
   specs/<module>/spec.md        ← acceptance criteria
   specs/<module>/tasks.md       ← task breakdown and scope
         ↓
4. Write <Module>ServiceTest.java        ← unit tests — DO THIS NOW
   src/test/java/com/<pkg>/<module>/
         ↓
5. Write <Module>ControllerTest.java     ← integration tests — DO THIS NOW
   src/test/java/com/<pkg>/<module>/
         ↓
6. GATE 1 — Verify test files exist and have @Test methods
   ls src/test/java/com/<pkg>/<module>/
   → Must show: <Module>ServiceTest.java + <Module>ControllerTest.java
   grep -c "@Test" <Module>ServiceTest.java    → must be > 0
   grep -c "@Test" <Module>ControllerTest.java → must be > 0
   → If files missing or 0 methods → STOP → write them → re-verify
   → Do NOT continue to step 7 until both files have @Test methods
         ↓
7. Run All Tests
   mvn clean verify
         ↓
8. GATE 2 — Check all tests pass (100% required)
   mvn clean verify output must show: BUILD SUCCESS, 0 failures, 0 errors
   If any test fails → STOP → fix the failure → re-run → only continue at 100%

9. Check Coverage
   mvn jacoco:report → minimum 80%
         ↓
9. Run curl Smoke Tests against live server
         ↓
10. Cross-check every response against DB
    Write results to doc/DATABASE_AUDIT.md
         ↓
11. Write results into test plan file
    .claude/tests/Task X.Y - Backend Test Plan.md
    → Update each TC row with ✅ PASS or ❌ FAIL
    → Paste build output
    → Update Final Status
         ↓
12. All pass?
    YES → Mark ✅ COMPLETED in systemTasks.md
     NO → Fix → Re-run → Repeat until all pass
```

**The test package being empty = the task was not tested = the task is NOT complete.**
**Write the Java test files (steps 4–5) before running mvn verify (step 7).**
**Tasks cannot be marked complete without all tests passing — no exceptions.**

---

## SpecKit File Reference

SpecKit generates these files — always read them before writing tests:

| SpecKit File | Location | What to Extract |
|---|---|---|
| Module spec | `specs/<module>/spec.md` | Acceptance criteria → one test per criterion |
| Implementation plan | `specs/<module>/plan.md` | Technical decisions → what to verify in DB |
| Task breakdown | `specs/<module>/tasks.md` | Scope of current task → what is in/out |
| Task tracker | `.claude/systemTasks.md` | Current task ID and status |

**Example for blog module:**
```
specs/blog/spec.md      ← acceptance criteria → become test scenarios
specs/blog/plan.md      ← API endpoints defined → become curl tests
specs/blog/tasks.md     ← task scope → know what to test in this task
.claude/systemTasks.md  ← confirm current task and mark complete
```

---

## File Structure

```
specs/                                         ← SpecKit generates — read before writing tests
├── blog/
│   ├── spec.md                                ← acceptance criteria → test scenarios
│   ├── plan.md                                ← implementation decisions → DB verifications
│   └── tasks.md                               ← task scope → what to test per task
├── case-studies/
├── consultation/
└── ...

src/test/java/com/<company>/<project>/         ← Agent writes these
├── common/
│   ├── BaseIntegrationTest.java               ← Shared setup for all integration tests
│   └── TestDataFactory.java                   ← Creates test entities and requests
└── module/
    └── <module>/
        ├── <Module>ServiceTest.java           ← Unit tests (Mockito)
        └── <Module>ControllerTest.java        ← Integration tests (MockMvc)

doc/
└── DATABASE_AUDIT.md                          ← curl results cross-checked against DB
```

---

## Test Types — When to Write Each

| Test Type | Class | What It Tests | Tools |
|---|---|---|---|
| Unit | `<Module>ServiceTest` | Business logic in isolation | JUnit 5 + Mockito |
| Integration | `<Module>ControllerTest` | Full HTTP request → DB → response | SpringBootTest + MockMvc + Testcontainers |
| Repository | `<Module>RepositoryTest` | Custom queries and soft delete | DataJpaTest + Testcontainers |

---

## Base Integration Test Setup

Every integration test class extends this:

```java
// src/test/java/com/<company>/<project>/common/BaseIntegrationTest.java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@Transactional
public abstract class BaseIntegrationTest {

    // ─── Containers ──────────────────────────────────────────────
    @Container
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Container
    static GenericContainer<?> redis =
        new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    // ─── Injected Beans ──────────────────────────────────────────
    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    // ─── Auth Helpers ────────────────────────────────────────────
    @Autowired
    private JwtUtil jwtUtil;

    protected String adminToken() {
        return "Bearer " + jwtUtil.generateToken("admin-test-id", "admin@test.com", List.of("ROLE_ADMIN"));
    }

    protected String editorToken() {
        return "Bearer " + jwtUtil.generateToken("editor-test-id", "editor@test.com", List.of("ROLE_EDITOR"));
    }

    // ─── JSON Helpers ────────────────────────────────────────────
    protected String toJson(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }

    protected <T> T fromJson(String json, Class<T> clazz) throws Exception {
        return objectMapper.readValue(json, clazz);
    }

    // ─── Request Helpers ─────────────────────────────────────────
    protected MockHttpServletRequestBuilder adminGet(String url) {
        return MockMvcRequestBuilders.get(url)
            .header("Authorization", adminToken())
            .contentType(MediaType.APPLICATION_JSON);
    }

    protected MockHttpServletRequestBuilder adminPost(String url, Object body) throws Exception {
        return MockMvcRequestBuilders.post(url)
            .header("Authorization", adminToken())
            .contentType(MediaType.APPLICATION_JSON)
            .content(toJson(body));
    }

    protected MockHttpServletRequestBuilder adminPut(String url, Object body) throws Exception {
        return MockMvcRequestBuilders.put(url)
            .header("Authorization", adminToken())
            .contentType(MediaType.APPLICATION_JSON)
            .content(toJson(body));
    }

    protected MockHttpServletRequestBuilder adminPatch(String url) {
        return MockMvcRequestBuilders.patch(url)
            .header("Authorization", adminToken())
            .contentType(MediaType.APPLICATION_JSON);
    }

    protected MockHttpServletRequestBuilder adminDelete(String url) {
        return MockMvcRequestBuilders.delete(url)
            .header("Authorization", adminToken())
            .contentType(MediaType.APPLICATION_JSON);
    }

    protected MockHttpServletRequestBuilder publicGet(String url) {
        return MockMvcRequestBuilders.get(url)
            .contentType(MediaType.APPLICATION_JSON);
    }

    protected MockHttpServletRequestBuilder publicPost(String url, Object body) throws Exception {
        return MockMvcRequestBuilders.post(url)
            .contentType(MediaType.APPLICATION_JSON)
            .content(toJson(body));
    }
}
```

---

## Test Data Factory

Centralize test data creation — never create ad-hoc test data inline:

```java
// src/test/java/com/<company>/<project>/common/TestDataFactory.java
@Component
public class TestDataFactory {

    // ─── Blog ─────────────────────────────────────────────────────
    public BlogCreateRequest validBlogRequest() {
        return BlogCreateRequest.builder()
            .title("AWS Migration Best Practices")
            .summary("A comprehensive guide to migrating to AWS")
            .content("## Introduction\n\nMigrating to AWS requires careful planning...")
            .categoryId(UUID.randomUUID())
            .tagIds(List.of())
            .build();
    }

    public BlogCreateRequest blogRequestWithTitle(String title) {
        BlogCreateRequest req = validBlogRequest();
        req.setTitle(title);
        return req;
    }

    // ─── Case Study ───────────────────────────────────────────────
    public CaseStudyCreateRequest validCaseStudyRequest() {
        return CaseStudyCreateRequest.builder()
            .title("Zero Downtime Migration for Saudi Fintech")
            .industry("Financial Services")
            .companySize("51-200")
            .location("Saudi Arabia")
            .projectDuration("3 Months")
            .challenge("Legacy on-premise infrastructure causing scalability issues")
            .approach("Phased migration using AWS 6 R's framework")
            .solution("Full migration to AWS ECS + RDS + CloudWatch")
            .categoryId(UUID.randomUUID())
            .build();
    }

    // ─── Consultation ────────────────────────────────────────────
    public ConsultationCreateRequest validConsultationRequest() {
        return ConsultationCreateRequest.builder()
            .fullName("Ahmed Al-Rashid")
            .jobTitle("CTO")
            .companyName("TechCorp KSA")
            .workEmail("ahmed@techcorp.com")
            .phone("+966501234567")
            .country("Saudi Arabia")
            .companySize("51-200")
            .currentInfrastructure("On-Premise")
            .servicesInterested(List.of("Cloud Migration", "AWS Infrastructure"))
            .projectTimeline("1-3 months")
            .challengeDescription("We need to migrate our legacy Oracle DB to AWS RDS")
            .build();
    }

    // ─── Contact ─────────────────────────────────────────────────
    public ContactCreateRequest validContactRequest() {
        return ContactCreateRequest.builder()
            .fullName("Sara Al-Ahmad")
            .email("sara@company.com")
            .subject("Partnership Inquiry")
            .message("We are interested in learning more about your AWS migration services.")
            .build();
    }
}
```

---

## Unit Test Pattern — Service Layer

```java
// src/test/java/com/<company>/<project>/module/blog/BlogServiceTest.java
@ExtendWith(MockitoExtension.class)
class BlogServiceTest {

    @Mock private BlogRepository blogRepository;
    @Mock private BlogCategoryRepository categoryRepository;
    @Mock private BlogMapper mapper;
    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private SetOperations<String, Object> setOperations;

    @InjectMocks private BlogService blogService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.keys(anyString())).thenReturn(Set.of());
    }

    // ─── CREATE ───────────────────────────────────────────────────

    @Test
    @DisplayName("create() — happy path — returns BlogResponse with generated slug")
    void create_happyPath_returnsResponseWithSlug() {
        // Arrange
        BlogCreateRequest request = new BlogCreateRequest();
        request.setTitle("AWS Migration Best Practices");
        request.setContent("Word ".repeat(400)); // 400 words = 2 min read

        BlogEntity savedEntity = new BlogEntity();
        savedEntity.setId(UUID.randomUUID());
        savedEntity.setSlug("aws-migration-best-practices");
        savedEntity.setStatus(BlogStatus.DRAFT);
        savedEntity.setReadingTimeMins(2);

        BlogResponse expectedResponse = new BlogResponse();
        expectedResponse.setSlug("aws-migration-best-practices");

        when(blogRepository.existsBySlugAndDeletedAtIsNull(anyString())).thenReturn(false);
        when(mapper.toEntity(request)).thenReturn(new BlogEntity());
        when(blogRepository.save(any())).thenReturn(savedEntity);
        when(mapper.toResponse(savedEntity)).thenReturn(expectedResponse);

        // Act
        BlogResponse result = blogService.create(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getSlug()).isEqualTo("aws-migration-best-practices");
        verify(blogRepository).save(argThat(entity ->
            entity.getSlug().equals("aws-migration-best-practices") &&
            entity.getStatus() == BlogStatus.DRAFT &&
            entity.getReadingTimeMins() == 2
        ));
    }

    @Test
    @DisplayName("create() — duplicate slug — throws SlugAlreadyExistsException")
    void create_duplicateSlug_throwsException() {
        // Arrange
        BlogCreateRequest request = new BlogCreateRequest();
        request.setTitle("AWS Migration Best Practices");

        when(blogRepository.existsBySlugAndDeletedAtIsNull("aws-migration-best-practices"))
            .thenReturn(true);

        // Act + Assert
        assertThatThrownBy(() -> blogService.create(request))
            .isInstanceOf(SlugAlreadyExistsException.class);

        verify(blogRepository, never()).save(any());
    }

    // ─── SLUG GENERATION ─────────────────────────────────────────

    @Test
    @DisplayName("create() — special characters in title — slug is URL-safe")
    void create_specialCharactersInTitle_slugIsUrlSafe() {
        BlogCreateRequest request = new BlogCreateRequest();
        request.setTitle("AWS & Cloud: Best Practices (2025)!");
        request.setContent("content");

        when(blogRepository.existsBySlugAndDeletedAtIsNull(anyString())).thenReturn(false);
        when(mapper.toEntity(any())).thenReturn(new BlogEntity());
        when(blogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toResponse(any())).thenReturn(new BlogResponse());

        blogService.create(request);

        verify(blogRepository).save(argThat(entity ->
            entity.getSlug().matches("[a-z0-9-]+") &&
            !entity.getSlug().contains("&") &&
            !entity.getSlug().contains("(") &&
            !entity.getSlug().contains(")")
        ));
    }

    // ─── READING TIME ─────────────────────────────────────────────

    @ParameterizedTest
    @DisplayName("create() — reading time calculated correctly from word count")
    @CsvSource({
        "200, 1",   // 200 words = 1 min
        "400, 2",   // 400 words = 2 min
        "1000, 5",  // 1000 words = 5 min
        "50, 1",    // less than 200 words = 1 min minimum
    })
    void create_readingTimeCalculatedCorrectly(int wordCount, int expectedMinutes) {
        String content = "word ".repeat(wordCount);
        BlogCreateRequest request = new BlogCreateRequest();
        request.setTitle("Test Blog");
        request.setContent(content);

        when(blogRepository.existsBySlugAndDeletedAtIsNull(anyString())).thenReturn(false);
        when(mapper.toEntity(any())).thenReturn(new BlogEntity());
        when(blogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toResponse(any())).thenReturn(new BlogResponse());

        blogService.create(request);

        verify(blogRepository).save(argThat(entity ->
            entity.getReadingTimeMins() == expectedMinutes
        ));
    }

    // ─── DELETE ───────────────────────────────────────────────────

    @Test
    @DisplayName("delete() — existing blog — sets deletedAt (soft delete)")
    void delete_existingBlog_setsDeletedAt() {
        UUID id = UUID.randomUUID();
        BlogEntity entity = new BlogEntity();
        entity.setId(id);
        entity.setDeletedAt(null);

        when(blogRepository.findByIdAndDeletedAtIsNull(id))
            .thenReturn(Optional.of(entity));

        blogService.delete(id);

        verify(blogRepository).save(argThat(saved ->
            saved.getDeletedAt() != null   // soft delete applied
        ));
        verify(blogRepository, never()).delete(any()); // hard delete NEVER called
    }

    @Test
    @DisplayName("delete() — non-existent blog — throws ResourceNotFoundException")
    void delete_nonExistentBlog_throwsNotFoundException() {
        UUID id = UUID.randomUUID();
        when(blogRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> blogService.delete(id))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    // ─── PUBLISH ──────────────────────────────────────────────────

    @Test
    @DisplayName("publish() — draft blog — sets status PUBLISHED and publishedAt")
    void publish_draftBlog_setsStatusAndPublishedAt() {
        UUID id = UUID.randomUUID();
        BlogEntity entity = new BlogEntity();
        entity.setId(id);
        entity.setStatus(BlogStatus.DRAFT);
        entity.setPublishedAt(null);

        when(blogRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.of(entity));
        when(blogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toResponse(any())).thenReturn(new BlogResponse());

        blogService.publish(id);

        verify(blogRepository).save(argThat(saved ->
            saved.getStatus() == BlogStatus.PUBLISHED &&
            saved.getPublishedAt() != null
        ));
    }

    // ─── CACHE INVALIDATION ───────────────────────────────────────

    @Test
    @DisplayName("publish() — on success — invalidates Redis cache")
    void publish_onSuccess_invalidatesRedisCache() {
        UUID id = UUID.randomUUID();
        BlogEntity entity = new BlogEntity();
        entity.setId(id);
        entity.setStatus(BlogStatus.DRAFT);

        when(blogRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.of(entity));
        when(blogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toResponse(any())).thenReturn(new BlogResponse());
        when(redisTemplate.keys("blogs:public:*")).thenReturn(Set.of("blogs:public:list"));

        blogService.publish(id);

        verify(redisTemplate).keys("blogs:public:*");
        verify(redisTemplate).delete(anyCollection());
    }
}
```

---

## Integration Test Pattern — Controller Layer

```java
// src/test/java/com/<company>/<project>/module/blog/BlogControllerTest.java
class BlogControllerTest extends BaseIntegrationTest {

    @Autowired private BlogRepository blogRepository;
    @Autowired private BlogCategoryRepository categoryRepository;
    @Autowired private TestDataFactory factory;

    private UUID categoryId;

    @BeforeEach
    void setUp() {
        // Create prerequisite data
        BlogCategoryEntity category = new BlogCategoryEntity();
        category.setName("Migration");
        category.setSlug("migration");
        categoryId = categoryRepository.save(category).getId();
    }

    // ─── CREATE ───────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/admin/blogs — 201 — admin creates blog successfully")
    void createBlog_asAdmin_returns201() throws Exception {
        BlogCreateRequest request = factory.validBlogRequest();
        request.setCategoryId(categoryId);

        mockMvc.perform(adminPost("/api/admin/blogs", request))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.title").value(request.getTitle()))
            .andExpect(jsonPath("$.data.slug").isNotEmpty())
            .andExpect(jsonPath("$.data.status").value("DRAFT"))
            .andExpect(jsonPath("$.data.readingTimeMins").isNumber())
            .andExpect(jsonPath("$.data.id").isNotEmpty());

        // Verify persisted to DB
        Optional<BlogEntity> saved = blogRepository.findBySlugAndDeletedAtIsNull("aws-migration-best-practices");
        assertThat(saved).isPresent();
        assertThat(saved.get().getStatus()).isEqualTo(BlogStatus.DRAFT);
    }

    @Test
    @DisplayName("POST /api/admin/blogs — 401 — no token returns unauthorized")
    void createBlog_noToken_returns401() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/admin/blogs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(factory.validBlogRequest())))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/admin/blogs — 400 — blank title returns validation error")
    void createBlog_blankTitle_returns400() throws Exception {
        BlogCreateRequest request = factory.validBlogRequest();
        request.setTitle("");

        mockMvc.perform(adminPost("/api/admin/blogs", request))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.error.fields.title").isNotEmpty());
    }

    @Test
    @DisplayName("POST /api/admin/blogs — 400 — all required fields missing")
    void createBlog_emptyBody_returns400WithAllFieldErrors() throws Exception {
        mockMvc.perform(adminPost("/api/admin/blogs", new BlogCreateRequest()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.fields.title").isNotEmpty())
            .andExpect(jsonPath("$.error.fields.content").isNotEmpty())
            .andExpect(jsonPath("$.error.fields.summary").isNotEmpty());
    }

    @Test
    @DisplayName("POST /api/admin/blogs — 409 — duplicate slug returns conflict")
    void createBlog_duplicateTitle_returns409() throws Exception {
        BlogCreateRequest request = factory.validBlogRequest();
        request.setCategoryId(categoryId);

        // First create
        mockMvc.perform(adminPost("/api/admin/blogs", request))
            .andExpect(status().isCreated());

        // Second create with same title → same slug → conflict
        mockMvc.perform(adminPost("/api/admin/blogs", request))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error.code").value("SLUG_ALREADY_EXISTS"));
    }

    // ─── GET LIST ─────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/admin/blogs — 200 — returns paged list of all blogs")
    void listBlogs_asAdmin_returns200WithPagedResult() throws Exception {
        // Create 3 blogs
        for (int i = 1; i <= 3; i++) {
            BlogCreateRequest req = factory.blogRequestWithTitle("Blog Number " + i);
            req.setCategoryId(categoryId);
            mockMvc.perform(adminPost("/api/admin/blogs", req));
        }

        mockMvc.perform(adminGet("/api/admin/blogs?page=0&size=10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.totalElements").value(greaterThanOrEqualTo(3)))
            .andExpect(jsonPath("$.page").value(0));
    }

    @Test
    @DisplayName("GET /api/admin/blogs — 401 — no token returns unauthorized")
    void listBlogs_noToken_returns401() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/blogs"))
            .andExpect(status().isUnauthorized());
    }

    // ─── GET BY ID ────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/admin/blogs/{id} — 200 — returns blog details")
    void getBlogById_existingId_returns200() throws Exception {
        BlogCreateRequest request = factory.validBlogRequest();
        request.setCategoryId(categoryId);

        String createResponse = mockMvc.perform(adminPost("/api/admin/blogs", request))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        String id = objectMapper.readTree(createResponse).path("data").path("id").asText();

        mockMvc.perform(adminGet("/api/admin/blogs/" + id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(id))
            .andExpect(jsonPath("$.data.title").value(request.getTitle()));
    }

    @Test
    @DisplayName("GET /api/admin/blogs/{id} — 404 — unknown id returns not found")
    void getBlogById_unknownId_returns404() throws Exception {
        mockMvc.perform(adminGet("/api/admin/blogs/" + UUID.randomUUID()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }

    // ─── UPDATE ───────────────────────────────────────────────────

    @Test
    @DisplayName("PUT /api/admin/blogs/{id} — 200 — updates blog fields")
    void updateBlog_validRequest_returns200() throws Exception {
        // Create blog first
        BlogCreateRequest createReq = factory.validBlogRequest();
        createReq.setCategoryId(categoryId);

        String createResponse = mockMvc.perform(adminPost("/api/admin/blogs", createReq))
            .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(createResponse).path("data").path("id").asText();

        // Update
        BlogUpdateRequest updateReq = new BlogUpdateRequest();
        updateReq.setTitle("Updated AWS Migration Guide");
        updateReq.setSummary("Updated summary");

        mockMvc.perform(adminPut("/api/admin/blogs/" + id, updateReq))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.title").value("Updated AWS Migration Guide"));
    }

    // ─── PUBLISH ──────────────────────────────────────────────────

    @Test
    @DisplayName("PATCH /api/admin/blogs/{id}/publish — 200 — status changes to PUBLISHED")
    void publishBlog_draftBlog_returnsPublished() throws Exception {
        BlogCreateRequest request = factory.validBlogRequest();
        request.setCategoryId(categoryId);

        String createResponse = mockMvc.perform(adminPost("/api/admin/blogs", request))
            .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(createResponse).path("data").path("id").asText();

        mockMvc.perform(adminPatch("/api/admin/blogs/" + id + "/publish"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("PUBLISHED"))
            .andExpect(jsonPath("$.data.publishedAt").isNotEmpty());

        // Verify DB state
        BlogEntity entity = blogRepository.findById(UUID.fromString(id)).orElseThrow();
        assertThat(entity.getStatus()).isEqualTo(BlogStatus.PUBLISHED);
        assertThat(entity.getPublishedAt()).isNotNull();
    }

    // ─── DELETE ───────────────────────────────────────────────────

    @Test
    @DisplayName("DELETE /api/admin/blogs/{id} — 200 — blog soft deleted")
    void deleteBlog_existingBlog_softDeleteApplied() throws Exception {
        BlogCreateRequest request = factory.validBlogRequest();
        request.setCategoryId(categoryId);

        String createResponse = mockMvc.perform(adminPost("/api/admin/blogs", request))
            .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(createResponse).path("data").path("id").asText();

        mockMvc.perform(adminDelete("/api/admin/blogs/" + id))
            .andExpect(status().isOk());

        // Verify soft delete — deletedAt is set, record still in DB
        BlogEntity entity = blogRepository.findById(UUID.fromString(id)).orElseThrow();
        assertThat(entity.getDeletedAt()).isNotNull();       // soft delete applied

        // Verify not returned in list anymore
        mockMvc.perform(adminGet("/api/admin/blogs"))
            .andExpect(jsonPath("$.data[?(@.id == '" + id + "')]").doesNotExist());
    }

    @Test
    @DisplayName("DELETE /api/admin/blogs/{id} — 404 — already deleted blog returns not found")
    void deleteBlog_alreadyDeleted_returns404() throws Exception {
        BlogCreateRequest request = factory.validBlogRequest();
        request.setCategoryId(categoryId);

        String createResponse = mockMvc.perform(adminPost("/api/admin/blogs", request))
            .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(createResponse).path("data").path("id").asText();

        // Delete once
        mockMvc.perform(adminDelete("/api/admin/blogs/" + id));

        // Delete again → 404
        mockMvc.perform(adminDelete("/api/admin/blogs/" + id))
            .andExpect(status().isNotFound());
    }

    // ─── PUBLIC API ───────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/public/blogs — 200 — returns only published blogs")
    void listPublicBlogs_returnsOnlyPublished() throws Exception {
        // Create published blog
        BlogCreateRequest pubReq = factory.blogRequestWithTitle("Published Blog");
        pubReq.setCategoryId(categoryId);
        String pubResponse = mockMvc.perform(adminPost("/api/admin/blogs", pubReq))
            .andReturn().getResponse().getContentAsString();
        String pubId = objectMapper.readTree(pubResponse).path("data").path("id").asText();
        mockMvc.perform(adminPatch("/api/admin/blogs/" + pubId + "/publish"));

        // Create draft blog
        BlogCreateRequest draftReq = factory.blogRequestWithTitle("Draft Blog");
        draftReq.setCategoryId(categoryId);
        mockMvc.perform(adminPost("/api/admin/blogs", draftReq));

        // Public API — only published returned
        mockMvc.perform(publicGet("/api/public/blogs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[*].title", hasItem("Published Blog")))
            .andExpect(jsonPath("$.data[*].title", not(hasItem("Draft Blog"))))
            .andExpect(jsonPath("$.data[*].status", everyItem(is("PUBLISHED"))));
    }

    @Test
    @DisplayName("GET /api/public/blogs/{slug} — 200 — returns blog by slug")
    void getBlogBySlug_publishedBlog_returns200() throws Exception {
        BlogCreateRequest request = factory.blogRequestWithTitle("AWS Best Practices");
        request.setCategoryId(categoryId);

        String createResponse = mockMvc.perform(adminPost("/api/admin/blogs", request))
            .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(createResponse).path("data").path("id").asText();
        mockMvc.perform(adminPatch("/api/admin/blogs/" + id + "/publish"));

        mockMvc.perform(publicGet("/api/public/blogs/aws-best-practices"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.slug").value("aws-best-practices"))
            .andExpect(jsonPath("$.data.status").value("PUBLISHED"));
    }

    @Test
    @DisplayName("GET /api/public/blogs/{slug} — 404 — draft blog not accessible publicly")
    void getBlogBySlug_draftBlog_returns404() throws Exception {
        BlogCreateRequest request = factory.blogRequestWithTitle("Draft Hidden Blog");
        request.setCategoryId(categoryId);
        mockMvc.perform(adminPost("/api/admin/blogs", request));

        // Draft blog should NOT be accessible via public API
        mockMvc.perform(publicGet("/api/public/blogs/draft-hidden-blog"))
            .andExpect(status().isNotFound());
    }

    // ─── RATE LIMITING ────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/public/consultation — rate limit — 4th request in window returns 429")
    void submitConsultation_exceedsRateLimit_returns429() throws Exception {
        ConsultationCreateRequest request = factory.validConsultationRequest();

        // 3 allowed per hour
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(publicPost("/api/public/consultation", request))
                .andExpect(status().isCreated());
        }

        // 4th exceeds limit
        mockMvc.perform(publicPost("/api/public/consultation", request))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.error.code").value("RATE_LIMIT_EXCEEDED"));
    }
}
```

---

## Mandatory Test Scenarios Per Module

Every module must have ALL of these scenarios covered:

### CRUD Tests

```
CREATE:
  ✅ happy path — valid request → 201 → verify DB record created
  ✅ no token → 401
  ✅ wrong role → 403
  ✅ blank required field → 400 with field error
  ✅ all fields missing → 400 with all field errors
  ✅ duplicate (slug/email/code) → 409

GET LIST:
  ✅ happy path — returns paged result
  ✅ no token (admin endpoint) → 401
  ✅ filter by status/category — returns correct subset
  ✅ deleted records not returned

GET BY ID / SLUG:
  ✅ existing id → 200 with correct data
  ✅ unknown id → 404
  ✅ soft-deleted id → 404

UPDATE:
  ✅ valid update → 200 → verify DB updated
  ✅ no token → 401
  ✅ unknown id → 404
  ✅ invalid field → 400

DELETE:
  ✅ existing → 200 → verify deletedAt set (soft delete)
  ✅ verify deleted record not in list
  ✅ delete again → 404 (already deleted)
  ✅ no token → 401

PUBLISH / STATUS CHANGE:
  ✅ draft → published → verify status and publishedAt set
  ✅ public API returns only published records
  ✅ draft not accessible via public API → 404
```

### Security Tests — Required on Every Protected Endpoint

```java
@Test
@DisplayName("Security — no token — 401")
void endpoint_noToken_returns401() throws Exception {
    mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/blogs")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());
}

@Test
@DisplayName("Security — invalid token — 401")
void endpoint_invalidToken_returns401() throws Exception {
    mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/blogs")
            .header("Authorization", "Bearer invalid.token.here")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());
}

@Test
@DisplayName("Security — expired token — 401")
void endpoint_expiredToken_returns401() throws Exception {
    String expiredToken = "Bearer " + generateExpiredToken();
    mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/blogs")
            .header("Authorization", expiredToken))
        .andExpect(status().isUnauthorized());
}

@Test
@DisplayName("Security — public endpoint — no auth required — 200")
void publicEndpoint_noToken_returns200() throws Exception {
    mockMvc.perform(publicGet("/api/public/blogs"))
        .andExpect(status().isOk());
}
```

### ApiResponse Structure Tests — Required on Every Endpoint

```java
// Every response must match the standard ApiResponse<T> wrapper
@Test
@DisplayName("Response structure — success response matches ApiResponse<T> format")
void response_successFormat_matchesApiResponseStructure() throws Exception {
    mockMvc.perform(publicGet("/api/public/blogs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data").exists())
        .andExpect(jsonPath("$.timestamp").isNotEmpty())
        .andExpect(jsonPath("$.error").doesNotExist());
}

@Test
@DisplayName("Response structure — error response matches ApiResponse<T> format")
void response_errorFormat_matchesApiResponseStructure() throws Exception {
    mockMvc.perform(adminGet("/api/admin/blogs/" + UUID.randomUUID()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.data").doesNotExist())
        .andExpect(jsonPath("$.error.code").isNotEmpty())
        .andExpect(jsonPath("$.error.message").isNotEmpty())
        .andExpect(jsonPath("$.timestamp").isNotEmpty());
}
```

### Soft Delete Tests — Required on Every Entity

```java
@Test
@DisplayName("Soft delete — deleted record still exists in DB with deletedAt set")
void softDelete_deletedAtSetRecordStillInDb() throws Exception {
    // Create
    String createResponse = mockMvc.perform(adminPost("/api/admin/blogs", factory.validBlogRequest()))
        .andReturn().getResponse().getContentAsString();
    String id = objectMapper.readTree(createResponse).path("data").path("id").asText();

    // Delete
    mockMvc.perform(adminDelete("/api/admin/blogs/" + id)).andExpect(status().isOk());

    // DB: record still exists
    BlogEntity entity = blogRepository.findById(UUID.fromString(id)).orElseThrow();
    assertThat(entity.getDeletedAt()).isNotNull();     // ✅ soft deleted

    // API: not visible anymore
    mockMvc.perform(adminGet("/api/admin/blogs/" + id))
        .andExpect(status().isNotFound());             // ✅ hidden from API
}
```

---

## curl Smoke Tests — Run Against Live Server

After all JUnit tests pass, run curl smoke tests against live `localhost:8080`:

```bash
#!/bin/bash
# scripts/smoke-test.sh
# Run: bash scripts/smoke-test.sh

BASE_URL="http://localhost:8080/api"
PASS=0
FAIL=0

check() {
  local name=$1
  local expected=$2
  local actual=$3

  if [ "$actual" = "$expected" ]; then
    echo "  ✅ $name"
    ((PASS++))
  else
    echo "  ❌ $name — expected: $expected, got: $actual"
    ((FAIL++))
  fi
}

echo "🔐 AUTH"
TOKEN=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@mfra.com","password":"admin123"}' | jq -r '.data.token')

check "Login returns token" "true" "$([ -n "$TOKEN" ] && echo true || echo false)"

echo ""
echo "📝 BLOGS"

# Create
CREATE=$(curl -s -X POST "$BASE_URL/admin/blogs" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Smoke Test Blog",
    "summary": "Smoke test summary",
    "content": "Smoke test content for our AWS guide",
    "categoryId": "'"$(curl -s "$BASE_URL/public/blog-categories" | jq -r '.data[0].id')"'"
  }')

BLOG_ID=$(echo $CREATE | jq -r '.data.id')
BLOG_SLUG=$(echo $CREATE | jq -r '.data.slug')

check "Create blog — success true" "true" "$(echo $CREATE | jq -r '.success')"
check "Create blog — status DRAFT" "DRAFT" "$(echo $CREATE | jq -r '.data.status')"
check "Create blog — slug generated" "smoke-test-blog" "$BLOG_SLUG"

# Get by ID
GET=$(curl -s "$BASE_URL/admin/blogs/$BLOG_ID" \
  -H "Authorization: Bearer $TOKEN")

check "Get blog by ID — success" "true" "$(echo $GET | jq -r '.success')"
check "Get blog by ID — title matches" "Smoke Test Blog" "$(echo $GET | jq -r '.data.title')"

# Publish
PUBLISH=$(curl -s -X PATCH "$BASE_URL/admin/blogs/$BLOG_ID/publish" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json")

check "Publish blog — status PUBLISHED" "PUBLISHED" "$(echo $PUBLISH | jq -r '.data.status')"
check "Publish blog — publishedAt set" "true" "$([ "$(echo $PUBLISH | jq -r '.data.publishedAt')" != "null" ] && echo true || echo false)"

# Public API — published blog visible
PUBLIC=$(curl -s "$BASE_URL/public/blogs/$BLOG_SLUG")
check "Public blog by slug — visible after publish" "true" "$(echo $PUBLIC | jq -r '.success')"

# Delete
DELETE=$(curl -s -X DELETE "$BASE_URL/admin/blogs/$BLOG_ID" \
  -H "Authorization: Bearer $TOKEN")

check "Delete blog — success" "true" "$(echo $DELETE | jq -r '.success')"

# Verify not accessible after delete
AFTER_DELETE=$(curl -s -o /dev/null -w "%{http_code}" \
  "$BASE_URL/admin/blogs/$BLOG_ID" \
  -H "Authorization: Bearer $TOKEN")

check "After delete — 404 returned" "404" "$AFTER_DELETE"

echo ""
echo "🔒 SECURITY"

# No token
NO_TOKEN=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/admin/blogs")
check "Admin endpoint no token — 401" "401" "$NO_TOKEN"

# Public endpoint no token
PUBLIC_NO_TOKEN=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/public/blogs")
check "Public endpoint no token — 200" "200" "$PUBLIC_NO_TOKEN"

echo ""
echo "────────────────────────────────"
echo "Results: ✅ $PASS passed  ❌ $FAIL failed"
echo "────────────────────────────────"

[ $FAIL -eq 0 ] && exit 0 || exit 1
```

---

## DATABASE_AUDIT.md — Required After Every curl Run

After running curl smoke tests, cross-check every write operation against the actual DB row and record results:

```markdown
# DATABASE_AUDIT.md

## Task X.Y — [Module Name]
**Date**: [Timestamp]
**Table**: blogs

---

### Operation: CREATE

**curl Request**:
```bash
POST /api/admin/blogs
Body: { "title": "Smoke Test Blog", ... }
```

**API Response**:
```json
{ "success": true, "data": { "id": "uuid-abc", "slug": "smoke-test-blog", "status": "DRAFT" } }
```

**DB Query**:
```sql
SELECT id, title, slug, status, deleted_at, created_at
FROM blogs
WHERE id = 'uuid-abc';
```

**DB Result**:
```
id        | uuid-abc
title     | Smoke Test Blog
slug      | smoke-test-blog
status    | DRAFT
deleted_at| NULL
created_at| 2026-03-09 10:00:00+00
```

**Match**: ✅ MATCH — API response matches DB row

---

### Operation: DELETE (Soft Delete)

**curl Request**:
```bash
DELETE /api/admin/blogs/uuid-abc
```

**API Response**:
```json
{ "success": true, "data": null }
```

**DB Query**:
```sql
SELECT id, deleted_at FROM blogs WHERE id = 'uuid-abc';
```

**DB Result**:
```
id        | uuid-abc
deleted_at| 2026-03-09 10:05:00+00   ← SET (soft delete confirmed)
```

**Match**: ✅ MATCH — record soft deleted, row still exists in DB

---

## Summary
- Total Operations: 4
- ✅ MATCH: 4
- ❌ MISMATCH: 0
- Status: ALL MATCH — task can be marked complete
```

**Rules for DATABASE_AUDIT.md:**
- One entry per curl operation — CREATE, GET, UPDATE, DELETE
- Every entry must show: curl request + API response + DB query + DB result + match status
- All entries must be `✅ MATCH` — any `❌ MISMATCH` means task is NOT complete
- Never skip this step — it is a hard requirement before marking complete

---

## application-test.yml

```yaml
# src/test/resources/application-test.yml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        jdbc:
          time_zone: UTC
  flyway:
    enabled: true
    locations: classpath:db/migration

app:
  jwt:
    secret: test-secret-key-minimum-256-bits-long-for-hs256
    expiration-ms: 3600000
    refresh-expiration-ms: 604800000

logging:
  level:
    org.springframework.security: WARN
    org.hibernate.SQL: WARN
```

---

## Maven Commands

```bash
# Compile — must be 0 errors before running tests
mvn clean compile

# Run all tests
mvn clean test

# Run specific test class
mvn test -Dtest=BlogControllerTest

# Run specific test method
mvn test -Dtest=BlogControllerTest#createBlog_asAdmin_returns201

# Full verify — build + test + coverage report
mvn clean verify

# Coverage report — open target/site/jacoco/index.html
mvn jacoco:report

# Run tests with output (no quiet mode)
mvn test -Dsurefire.failIfNoSpecifiedTests=false
```

---

## Quality Gates — Cannot Mark Task Complete Unless

**Build:**
- ✅ `mvn clean compile` → 0 errors
- ✅ `mvn clean verify` → all tests pass, 0 failures

**JUnit Coverage:**
- ✅ Coverage ≥ 80% on all changed classes
- ✅ Every CRUD operation has a test
- ✅ Every security scenario tested (no token, invalid token)
- ✅ Every validation error tested (missing fields, invalid formats)
- ✅ Soft delete verified — `deletedAt` set, record still in DB
- ✅ Public API returns only published/active records
- ✅ ApiResponse structure verified on success and error responses

**curl Smoke Tests:**
- ✅ `bash scripts/smoke-test.sh` → 0 failures
- ✅ All operations run against live `localhost:8080`
- ✅ CREATE → GET → UPDATE → DELETE all verified

**DATABASE_AUDIT.md:**
- ✅ One entry per curl operation
- ✅ All entries are `✅ MATCH`
- ✅ No `❌ MISMATCH` entries
- ✅ File updated in `doc/DATABASE_AUDIT.md`

---

## ✅ Always Do This

1. Extend `BaseIntegrationTest` in every integration test class
2. Use `TestDataFactory` for all test data — never create inline ad-hoc objects
3. Verify DB state after every write operation — not just API response
4. Test soft delete specifically — `deletedAt` set, record still in DB
5. Test security on every endpoint — no token + invalid token
6. Test ApiResponse structure on success and error — always
7. Test that public API returns only published/active records
8. Run curl smoke tests against live server after JUnit passes
9. Cross-check every curl response against DB row
10. Write all results to `doc/DATABASE_AUDIT.md`
11. Use `@DisplayName` on every test — human-readable test names
12. Use `@ParameterizedTest` for boundary values — word count, slug generation

---

## ❌ Never Do This

1. Mark task complete if any test fails — 100% pass rate required
2. Skip security tests — no token and invalid token are mandatory
3. Skip soft delete verification — always check `deletedAt` in DB
4. Call `repository.delete()` anywhere — soft delete only
5. Skip DATABASE_AUDIT.md — every write must be cross-checked
6. Use random test data that makes assertions fragile — use TestDataFactory
7. Skip curl smoke tests — JUnit alone is not enough
8. Skip coverage check — minimum 80% is a hard requirement
9. Hardcode JWT secrets in tests — use `application-test.yml`
10. Use `@SpringBootTest` without `@Testcontainers` — always use real DB in integration tests
11. Test only happy path — error cases and edge cases are mandatory
12. Leave test methods without `@DisplayName` — always describe what is being tested