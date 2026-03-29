# Async Handling Instructions

> This file is reusable across all projects.
> The AI agent must read this file when it detects async patterns are needed.
> The agent must check configurations.md FIRST — async config may already be set up.
> The agent must recognize async requirements independently — never wait to be told.

---

## Step 0 — Check configurations.md Before Anything Else

Before writing any async code, the agent reads `.claude/configurations.md`
and checks the Async Configuration section:

```
Is Async status ✅ READY?    → Config exists — use it directly, do not re-setup
Is Async status ⚠️ PARTIAL?  → Complete the missing parts, then mark ✅ READY
Is Async status ❌ MISSING?  → Set up async config first, then mark ✅ READY
```

**Specifically check:**
- Is `@EnableAsync` already added?      → If yes, skip — never add it twice
- Is `emailTaskExecutor` already defined? → If yes, reuse it — never create a duplicate bean
- Is `@EnableScheduling` already added?   → If yes, skip — never add it twice

**After completing async setup — agent must update configurations.md:**
```
Async Configuration: ✅ READY
  @EnableAsync: config/AsyncConfig.java
  Thread pools: emailTaskExecutor (core=2, max=5), reportTaskExecutor (core=2, max=4)
  @EnableScheduling: config/SchedulingConfig.java
  Set up in task: [task ID]
```

---

## Agent Decision Rule — When to Read This File

The agent reads this file automatically when it detects ANY of these signals
in the spec, task, or requirements:

### 🔴 Always Async — Read This File

| Signal in Spec / Task | Why Async Is Required |
|---|---|
| "send email" / "send notification" | Email delivery must not block HTTP response |
| "process file" / "upload and process" | File parsing is slow — client must not wait |
| "generate report" / "export CSV/PDF" | Generation can take minutes |
| "call external API" / "webhook" | Third-party APIs are unpredictable in latency |
| "bulk operation" / "batch update" | Large datasets must not block the request thread |
| "migrate data" / "import data" | Migration jobs are long-running |
| "AI processing" / "run inference" | LLM calls can take 10–60 seconds |
| "scheduled job" / "run every X" | Recurring background work |
| "retry on failure" / "with retries" | Retry logic must not block the client |
| "status: pending → processing → done" | Multi-step status lifecycle = async |
| "poll for status" / "check progress" | Client polling = async backend processing |

### 🟡 Likely Async — Evaluate Before Deciding

| Signal | Evaluate This |
|---|---|
| "on submit, do X and Y" | If X and Y are independent → async Y |
| "notify the team" | Email/Slack notifications → always async |
| "after save, update stats" | Stats recalculation → async if slow |
| "send confirmation" | Confirmation email → always async |
| "log this event" | Audit logging → async to avoid blocking |

### 🟢 Sync Is Fine — No Async Needed

| Signal | Why Sync Works |
|---|---|
| "return list of blogs" | Simple DB read — fast |
| "create a record" | Single DB write — fast |
| "validate and save" | No external dependencies |
| "get by ID" | Indexed DB lookup — fast |
| "delete / soft delete" | Single DB write — fast |

---

## Decision Flowchart

```
Incoming request received
         ↓
Does it call an external service (email, SMS, AI, webhook)?
   YES → Async
   NO  ↓
Does it process a file or large dataset?
   YES → Async
   NO  ↓
Does it take more than 2 seconds in the worst case?
   YES → Async
   NO  ↓
Does the client need to wait for the result to continue?
   YES → Sync
   NO  → Async
```

---

## Backend Async Patterns — Spring Boot

---

### Pattern 1 — Fire and Forget (Email, Notifications, Logging)

Use when: client does not need to know the result.
Example: send confirmation email after consultation form submit.

```java
// service/ConsultationService.java
@Service
@RequiredArgsConstructor
@Slf4j
public class ConsultationService {

    private final ConsultationRepository repository;
    private final EmailService emailService;

    @Transactional
    public ConsultationResponse submit(ConsultationCreateRequest request) {
        // 1. Save to DB — synchronous — client needs the ID
        ConsultationEntity entity = mapper.toEntity(request);
        entity.setStatus(ConsultationStatus.NEW);
        entity = repository.save(entity);

        // 2. Send emails — asynchronous — client does not wait
        emailService.sendConfirmationAsync(entity);      // fire and forget
        emailService.sendAdminNotificationAsync(entity); // fire and forget

        log.info("Consultation submitted: id={}", entity.getId());

        // 3. Return immediately — emails are sent in background
        return mapper.toResponse(entity);
    }
}

// service/EmailService.java
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Async("emailTaskExecutor")    // runs in separate thread pool
    public void sendConfirmationAsync(ConsultationEntity consultation) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(consultation.getWorkEmail());
            message.setSubject("MFRA — We received your consultation request");
            message.setText(buildConfirmationBody(consultation));
            mailSender.send(message);
            log.info("Confirmation email sent to: {}", consultation.getWorkEmail());
        } catch (Exception e) {
            // Log failure — never throw from @Async void method
            log.error("Failed to send confirmation email for consultation: {}",
                consultation.getId(), e);
        }
    }

    @Async("emailTaskExecutor")
    public void sendAdminNotificationAsync(ConsultationEntity consultation) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo("admin@mfra.com");
            message.setSubject("New Consultation Request — " + consultation.getCompanyName());
            message.setText(buildAdminBody(consultation));
            mailSender.send(message);
            log.info("Admin notification sent for consultation: {}", consultation.getId());
        } catch (Exception e) {
            log.error("Failed to send admin notification for consultation: {}",
                consultation.getId(), e);
        }
    }

    private String buildConfirmationBody(ConsultationEntity c) {
        return String.format("""
            Dear %s,

            Thank you for reaching out to MFRA. We have received your consultation request.

            Our team will review your details and contact you within 24 hours.

            What happens next:
            1. Our engineers review your submission
            2. We schedule a 30-minute discovery call
            3. We propose a tailored plan

            Best regards,
            The MFRA Team
            """, c.getFullName());
    }
}
```

**Enable async in Spring Boot:**
```java
// config/AsyncConfig.java
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "emailTaskExecutor")
    public Executor emailTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("email-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    @Bean(name = "reportTaskExecutor")
    public Executor reportTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(20);
        executor.setThreadNamePrefix("report-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
```

---

### Pattern 2 — Job with Status Tracking (Reports, Exports, Imports)

Use when: operation takes more than 2 seconds and client needs to know when it finishes.
Example: export case studies to CSV, generate monthly report.

**Status lifecycle:**
```
PENDING → PROCESSING → COMPLETED
                     → FAILED
```

```sql
-- Flyway migration: V10__create_async_jobs.sql
CREATE TABLE async_jobs (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type        VARCHAR(100) NOT NULL,       -- EXPORT_CASE_STUDIES, GENERATE_REPORT
    status      VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payload     JSONB,                        -- input parameters
    result_url  VARCHAR(500),                 -- download URL when done
    error       TEXT,                         -- error message if failed
    progress    INT DEFAULT 0,                -- 0-100 percentage
    created_by  UUID,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at TIMESTAMPTZ,
    deleted_at  TIMESTAMPTZ
);

CREATE INDEX idx_async_jobs_status ON async_jobs(status);
CREATE INDEX idx_async_jobs_type   ON async_jobs(type);
```

```java
// Controller — returns immediately with job ID
@PostMapping("/exports/case-studies")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<ApiResponse<AsyncJobResponse>> exportCaseStudies(
    @Valid @RequestBody ExportRequest request
) {
    AsyncJobResponse job = exportService.startExport(request);
    return ResponseEntity.status(HttpStatus.ACCEPTED)      // 202 — not 200 or 201
        .body(ApiResponse.ok(job));
}

// Polling endpoint — client calls this every few seconds
@GetMapping("/exports/{jobId}/status")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<ApiResponse<AsyncJobResponse>> getJobStatus(
    @PathVariable UUID jobId
) {
    return ResponseEntity.ok(ApiResponse.ok(exportService.getJobStatus(jobId)));
}

// Download endpoint — only available when status is COMPLETED
@GetMapping("/exports/{jobId}/download")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<Resource> downloadExport(@PathVariable UUID jobId) {
    return exportService.getDownloadResource(jobId);
}
```

```java
// service/ExportService.java
@Service
@RequiredArgsConstructor
@Slf4j
public class ExportService {

    private final AsyncJobRepository jobRepository;
    private final CaseStudyRepository caseStudyRepository;
    private final S3Service s3Service;

    // Called by controller — returns immediately
    @Transactional
    public AsyncJobResponse startExport(ExportRequest request) {
        AsyncJobEntity job = new AsyncJobEntity();
        job.setType("EXPORT_CASE_STUDIES");
        job.setStatus(AsyncJobStatus.PENDING);
        job.setPayload(objectMapper.writeValueAsString(request));
        job.setProgress(0);
        job = jobRepository.save(job);

        // Trigger async processing — returns immediately
        processExportAsync(job.getId());

        log.info("Export job created: id={}", job.getId());
        return mapper.toResponse(job);
    }

    @Async("reportTaskExecutor")
    public void processExportAsync(UUID jobId) {
        AsyncJobEntity job = jobRepository.findById(jobId).orElseThrow();

        try {
            // Mark as processing
            job.setStatus(AsyncJobStatus.PROCESSING);
            job.setProgress(10);
            jobRepository.save(job);

            // Do the work
            List<CaseStudyEntity> studies = caseStudyRepository.findAllPublished();
            job.setProgress(50);
            jobRepository.save(job);

            // Generate CSV
            byte[] csv = generateCsv(studies);
            job.setProgress(80);
            jobRepository.save(job);

            // Upload to S3
            String url = s3Service.upload("exports/" + jobId + ".csv", csv);
            job.setProgress(100);
            jobRepository.save(job);

            // Mark as completed
            job.setStatus(AsyncJobStatus.COMPLETED);
            job.setResultUrl(url);
            job.setCompletedAt(Instant.now());
            jobRepository.save(job);

            log.info("Export job completed: id={}, url={}", jobId, url);

        } catch (Exception e) {
            // Mark as failed — never lose the error
            job.setStatus(AsyncJobStatus.FAILED);
            job.setError(e.getMessage());
            jobRepository.save(job);
            log.error("Export job failed: id={}", jobId, e);
        }
    }

    @Transactional(readOnly = true)
    public AsyncJobResponse getJobStatus(UUID jobId) {
        return jobRepository.findByIdAndDeletedAtIsNull(jobId)
            .map(mapper::toResponse)
            .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
    }
}
```

**Response for 202 ACCEPTED:**
```json
{
  "success": true,
  "data": {
    "jobId": "uuid-abc",
    "type": "EXPORT_CASE_STUDIES",
    "status": "PENDING",
    "progress": 0,
    "resultUrl": null,
    "createdAt": "2026-03-09T10:00:00Z"
  },
  "timestamp": "2026-03-09T10:00:00Z"
}
```

**Polling response (PROCESSING):**
```json
{
  "success": true,
  "data": {
    "jobId": "uuid-abc",
    "status": "PROCESSING",
    "progress": 50,
    "resultUrl": null
  }
}
```

**Polling response (COMPLETED):**
```json
{
  "success": true,
  "data": {
    "jobId": "uuid-abc",
    "status": "COMPLETED",
    "progress": 100,
    "resultUrl": "https://s3.../exports/uuid-abc.csv",
    "completedAt": "2026-03-09T10:01:23Z"
  }
}
```

---

### Pattern 3 — Scheduled Jobs (Cron Tasks)

Use when: work must run on a schedule, not triggered by user.
Example: clean up old sessions, send weekly digest, refresh stats cache.

```java
// scheduler/SiteStatsScheduler.java
@Component
@RequiredArgsConstructor
@Slf4j
public class SiteStatsScheduler {

    private final StatsService statsService;
    private final RedisTemplate<String, Object> redisTemplate;

    // Refresh stats cache every hour
    @Scheduled(fixedRate = 3600000)
    public void refreshStatsCache() {
        log.info("Refreshing stats cache...");
        try {
            redisTemplate.delete("stats:public");
            statsService.getPublicStats(); // repopulates cache
            log.info("Stats cache refreshed");
        } catch (Exception e) {
            log.error("Failed to refresh stats cache", e);
        }
    }

    // Clean up old unread contact messages — runs at 2AM every day
    @Scheduled(cron = "0 0 2 * * *")
    public void archiveOldMessages() {
        log.info("Archiving old contact messages...");
        try {
            int archived = statsService.archiveMessagesOlderThan(90); // 90 days
            log.info("Archived {} old messages", archived);
        } catch (Exception e) {
            log.error("Failed to archive old messages", e);
        }
    }
}
```

**Enable scheduling:**
```java
// config/SchedulingConfig.java
@Configuration
@EnableScheduling
public class SchedulingConfig {
    // Scheduling is enabled — cron and fixedRate annotations work
}
```

---

### Pattern 4 — Spring Events (Decoupled Side Effects)

Use when: one action triggers multiple independent side effects.
Example: consultation submitted → send email + create CRM record + send Slack.

```java
// event/ConsultationSubmittedEvent.java
public record ConsultationSubmittedEvent(ConsultationEntity consultation) {}

// service/ConsultationService.java
@Service
@RequiredArgsConstructor
public class ConsultationService {

    private final ApplicationEventPublisher eventPublisher;
    private final ConsultationRepository repository;

    @Transactional
    public ConsultationResponse submit(ConsultationCreateRequest request) {
        ConsultationEntity entity = mapper.toEntity(request);
        entity = repository.save(entity);

        // Publish event — all listeners run asynchronously and independently
        eventPublisher.publishEvent(new ConsultationSubmittedEvent(entity));

        return mapper.toResponse(entity);
    }
}

// listener/ConsultationEventListener.java
@Component
@RequiredArgsConstructor
@Slf4j
public class ConsultationEventListener {

    private final EmailService emailService;

    @Async("emailTaskExecutor")
    @EventListener
    public void onConsultationSubmitted(ConsultationSubmittedEvent event) {
        ConsultationEntity c = event.consultation();

        // Each handler is independent — failure in one does not affect others
        try {
            emailService.sendConfirmation(c.getWorkEmail(), c.getFullName());
        } catch (Exception e) {
            log.error("Failed to send confirmation for consultation: {}", c.getId(), e);
        }

        try {
            emailService.sendAdminNotification(c);
        } catch (Exception e) {
            log.error("Failed to send admin notification for consultation: {}", c.getId(), e);
        }
    }
}
```

---

## Frontend Async Patterns — React + RTK Query

---

### Pattern 1 — Optimistic Updates (Instant UI Feedback)

Use when: user action should feel instant even though API is being called.
Example: publishing a blog — toggle status in UI immediately.

```typescript
// features/blog/api.ts
publishBlog: builder.mutation<BlogResponse, string>({
  query: (id) => ({ url: `/admin/blogs/${id}/publish`, method: 'PATCH' }),
  async onQueryStarted(id, { dispatch, queryFulfilled, getState }) {
    // Optimistically update the cache before API responds
    const patchResult = dispatch(
      blogApi.util.updateQueryData('getBlogs', undefined, (draft) => {
        const blog = draft.data?.find((b) => b.id === id);
        if (blog) {
          blog.status = 'PUBLISHED';
          blog.publishedAt = new Date().toISOString();
        }
      })
    );

    try {
      await queryFulfilled; // Wait for real API response
      toast.success('Blog published successfully');
    } catch {
      patchResult.undo(); // Revert optimistic update on failure
      toast.error('Failed to publish blog');
    }
  },
}),
```

---

### Pattern 2 — Polling (Job Status Tracking)

Use when: user triggers a long-running job and needs to see progress.
Example: export job, AI processing, bulk import.

```typescript
// features/export/hooks.ts
export function useExportJobStatus(jobId: string | null) {
  return useGetJobStatusQuery(jobId!, {
    skip: !jobId,
    // Poll every 3 seconds while job is pending or processing
    pollingInterval: 3000,
    // Stop polling when job is complete or failed
    selectFromResult: ({ data, ...rest }) => ({
      ...rest,
      data,
      isComplete: data?.status === 'COMPLETED' || data?.status === 'FAILED',
    }),
  });
}

// features/export/components/ExportButton.tsx
export function ExportButton() {
  const [startExport, { isLoading: isStarting }] = useStartExportMutation();
  const [jobId, setJobId] = useState<string | null>(null);

  const { data: job, isComplete } = useExportJobStatus(jobId);

  // Stop polling when job completes
  useEffect(() => {
    if (isComplete && job?.status === 'COMPLETED' && job?.resultUrl) {
      toast.success('Export ready! Downloading...');
      window.open(job.resultUrl, '_blank');
      setJobId(null); // Stop polling
    }
    if (isComplete && job?.status === 'FAILED') {
      toast.error('Export failed. Please try again.');
      setJobId(null); // Stop polling
    }
  }, [isComplete, job]);

  const handleExport = async () => {
    try {
      const result = await startExport({}).unwrap();
      setJobId(result.jobId);
      toast.success('Export started — this may take a moment');
    } catch {
      toast.error('Failed to start export');
    }
  };

  return (
    <div>
      <button
        onClick={handleExport}
        disabled={isStarting || !!jobId}
        data-testid="export-btn"
      >
        {isStarting ? 'Starting...' : jobId ? 'Exporting...' : 'Export CSV'}
      </button>

      {jobId && job && (
        <div data-testid="export-progress">
          <div className="h-2 rounded-full bg-gray-200">
            <div
              className="h-2 rounded-full bg-orange-500 transition-all duration-500"
              style={{ width: `${job.progress}%` }}
            />
          </div>
          <p className="text-sm text-gray-600 mt-1">
            {job.status === 'PROCESSING' ? `${job.progress}% complete` : job.status}
          </p>
        </div>
      )}
    </div>
  );
}
```

---

### Pattern 3 — Background Mutation (Fire and Forget from Frontend)

Use when: frontend triggers an action that backend processes async and client does not need to wait for completion.
Example: submit consultation form — response is instant, emails sent in background.

```typescript
// features/consultation/api.ts
submitConsultation: builder.mutation<ConsultationResponse, ConsultationCreateRequest>({
  query: (body) => ({ url: '/public/consultation', method: 'POST', body }),
}),

// features/consultation/components/ConsultationForm.tsx
const [submit, { isLoading }] = useSubmitConsultationMutation();

const onSubmit = async (data: ConsultationFormData) => {
  try {
    // Backend responds immediately (201) — emails sent in background
    await submit(data).unwrap();

    // Show success — user does not wait for emails
    toast.success('Request submitted! We will contact you within 24 hours.');
    form.reset();

  } catch (error) {
    const apiError = error as ApiError;
    if (apiError.fields) {
      // Map backend field errors to form
      Object.entries(apiError.fields).forEach(([field, message]) => {
        form.setError(field as keyof ConsultationFormData, { message });
      });
    } else {
      toast.error(getErrorMessage(error));
    }
  }
};
```

---

### Pattern 4 — Async State Machine (Multi-Step Status UI)

Use when: a resource goes through multiple states over time and UI must reflect each.
Example: consultation request status, export job, case study review.

```typescript
// features/consultation/components/ConsultationStatusBadge.tsx
type ConsultationStatus = 'NEW' | 'REVIEWED' | 'CONTACTED' | 'CLOSED';

const STATUS_CONFIG: Record<ConsultationStatus, {
  label: string;
  color: string;
  icon: string;
}> = {
  NEW:       { label: 'New',       color: 'bg-blue-100 text-blue-800',   icon: '🆕' },
  REVIEWED:  { label: 'Reviewed',  color: 'bg-yellow-100 text-yellow-800', icon: '👀' },
  CONTACTED: { label: 'Contacted', color: 'bg-green-100 text-green-800',  icon: '📞' },
  CLOSED:    { label: 'Closed',    color: 'bg-gray-100 text-gray-800',    icon: '✅' },
};

export function ConsultationStatusBadge({ status }: { status: ConsultationStatus }) {
  const config = STATUS_CONFIG[status];
  return (
    <span
      className={`inline-flex items-center gap-1 rounded-full px-3 py-1 text-sm font-medium ${config.color}`}
      data-testid="consultation-status-badge"
    >
      {config.icon} {config.label}
    </span>
  );
}
```

---

## Testing Async — Backend

### Unit Test — @Async Method Called

```java
@Test
@DisplayName("submit() — consultation saved — async emails triggered")
void submit_validRequest_triggersAsyncEmails() {
    ConsultationCreateRequest request = factory.validConsultationRequest();

    when(repository.save(any())).thenAnswer(inv -> {
        ConsultationEntity e = inv.getArgument(0);
        e.setId(UUID.randomUUID());
        return e;
    });
    when(mapper.toEntity(any())).thenReturn(new ConsultationEntity());
    when(mapper.toResponse(any())).thenReturn(new ConsultationResponse());

    consultationService.submit(request);

    // Verify async methods were called — they run in background
    verify(emailService).sendConfirmationAsync(any(ConsultationEntity.class));
    verify(emailService).sendAdminNotificationAsync(any(ConsultationEntity.class));
}
```

### Integration Test — 202 Accepted for Async Job

```java
@Test
@DisplayName("POST /api/admin/exports/case-studies — 202 — returns jobId immediately")
void startExport_validRequest_returns202WithJobId() throws Exception {
    mockMvc.perform(adminPost("/api/admin/exports/case-studies", new ExportRequest()))
        .andExpect(status().isAccepted())               // 202 not 200 or 201
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.jobId").isNotEmpty())
        .andExpect(jsonPath("$.data.status").value("PENDING"))
        .andExpect(jsonPath("$.data.progress").value(0));
}

@Test
@DisplayName("GET /api/admin/exports/{jobId}/status — 200 — returns current job status")
void getJobStatus_existingJob_returnsCurrentStatus() throws Exception {
    // Start a job first
    String createResponse = mockMvc.perform(adminPost("/api/admin/exports/case-studies",
            new ExportRequest()))
        .andReturn().getResponse().getContentAsString();

    String jobId = objectMapper.readTree(createResponse).path("data").path("jobId").asText();

    // Poll status
    mockMvc.perform(adminGet("/api/admin/exports/" + jobId + "/status"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.jobId").value(jobId))
        .andExpect(jsonPath("$.data.status").isNotEmpty())
        .andExpect(jsonPath("$.data.progress").isNumber());
}

@Test
@DisplayName("Async job — status transitions PENDING → PROCESSING → COMPLETED")
void asyncJob_completesSuccessfully_statusTransitions() throws Exception {
    String createResponse = mockMvc.perform(adminPost("/api/admin/exports/case-studies",
            new ExportRequest()))
        .andReturn().getResponse().getContentAsString();

    String jobId = objectMapper.readTree(createResponse).path("data").path("jobId").asText();

    // Poll until completed — max 30 seconds
    String finalStatus = null;
    for (int i = 0; i < 30; i++) {
        Thread.sleep(1000);
        String statusResponse = mockMvc.perform(adminGet("/api/admin/exports/" + jobId + "/status"))
            .andReturn().getResponse().getContentAsString();
        finalStatus = objectMapper.readTree(statusResponse).path("data").path("status").asText();

        if ("COMPLETED".equals(finalStatus) || "FAILED".equals(finalStatus)) break;
    }

    assertThat(finalStatus).isEqualTo("COMPLETED");
}
```

---

## Testing Async — Frontend (Playwright)

### Test — Polling UI Updates Progress Bar

```javascript
// Scenario: Export job — progress bar updates as job progresses
let callCount = 0;

// Mock API to simulate job progressing
await page.route('**/api/admin/exports/case-studies/status**', async (route) => {
  callCount++;
  const responses = [
    { status: 'PENDING',    progress: 0   },
    { status: 'PROCESSING', progress: 30  },
    { status: 'PROCESSING', progress: 70  },
    { status: 'COMPLETED',  progress: 100, resultUrl: 'https://s3.../export.csv' },
  ];
  const response = responses[Math.min(callCount - 1, responses.length - 1)];

  await route.fulfill({
    status: 200,
    contentType: 'application/json',
    body: JSON.stringify({ success: true, data: { jobId: 'uuid-abc', ...response } }),
  });
});

// Trigger export
await playwright_click({ selector: '[data-testid="export-btn"]' });
await playwright_screenshot({ name: 'test-X.Y-async-1-started' });

// Verify progress bar appears
const progressBar = page.locator('[data-testid="export-progress"]');
await expect(progressBar).toBeVisible({ timeout: 5000 });
console.log('Progress bar shown:', '✅');

// Wait for completion
await page.waitForFunction(() => {
  const badge = document.querySelector('[data-testid="export-progress"]');
  return badge?.textContent?.includes('100') || badge === null;
}, { timeout: 15000 });

await playwright_screenshot({ name: 'test-X.Y-async-2-completed' });
console.log('Export flow completed:', '✅');
```

### Test — Fire and Forget (Form Submit Returns Instantly)

```javascript
// Scenario: Consultation form — response is fast even though emails are async
const startTime = Date.now();

await playwright_fill({ selector: '[name="fullName"]', value: 'Ahmed Al-Rashid' });
await playwright_fill({ selector: '[name="workEmail"]', value: 'ahmed@company.com' });
// ... fill other fields

await playwright_click({ selector: 'button[type="submit"]' });

// Success message should appear within 3 seconds (API is fast, emails are background)
await page.waitForSelector('[data-testid="success-message"]', { timeout: 3000 });
const responseTime = Date.now() - startTime;

console.log('Form submitted and success shown in:', responseTime + 'ms');
console.log('Fast response (< 3s):', responseTime < 3000 ? '✅' : '❌ Too slow');

await playwright_screenshot({ name: 'test-X.Y-async-fire-forget-success' });
```

---

## HTTP Status Codes for Async

| Scenario | Status Code | Meaning |
|---|---|---|
| Sync create — result ready now | `201 Created` | Resource created, data in response |
| Async job started — result not ready | `202 Accepted` | Request accepted, check status later |
| Sync read | `200 OK` | Data returned immediately |
| Job status check | `200 OK` | Current status returned |
| Job not found | `404 Not Found` | Job ID does not exist |

**Never return `200` for a long-running async job — always use `202 Accepted`.**

---

## application.yml — Async Config

```yaml
# Add to application.yml
spring:
  task:
    execution:
      pool:
        core-size: 2
        max-size: 10
        queue-capacity: 100
      thread-name-prefix: "async-"
    scheduling:
      pool:
        size: 5
      thread-name-prefix: "scheduled-"

app:
  async:
    email:
      core-pool-size: 2
      max-pool-size: 5
      queue-capacity: 50
    report:
      core-pool-size: 2
      max-pool-size: 4
      queue-capacity: 20
```

---

## ✅ Always Do This

1. Return `202 Accepted` for async job endpoints — never `200` or `201`
2. Always include `jobId` in the 202 response — client needs it to poll
3. Always provide a polling endpoint — client must be able to check status
4. Always store job status in DB — never only in memory
5. Always catch exceptions in `@Async` void methods — failures must be logged
6. Always update job status to `FAILED` on exception — never leave PROCESSING
7. Always use a named thread pool for `@Async` — never the default executor
8. Always set `progress` field on job — 0 to 100 — frontend needs it
9. Always include `resultUrl` in COMPLETED response — client needs download link
10. Use Spring Events for decoupled side effects — email, Slack, audit log
11. Use `@Scheduled` with cron for time-based jobs — never `Thread.sleep` loops
12. Poll every 3 seconds from frontend — not faster, not slower

---

## ❌ Never Do This

1. Block the HTTP thread waiting for email to send — always `@Async`
2. Return `200` for a job that isn't done yet — use `202 Accepted`
3. Store job status only in memory — always persist to DB
4. Leave job status as `PROCESSING` on failure — always set `FAILED`
5. Throw exceptions from `@Async` void methods — always catch and log
6. Use a single shared default thread pool for all async work — always name pools
7. Poll faster than 3 seconds from frontend — causes unnecessary server load
8. Poll forever — always stop polling on `COMPLETED` or `FAILED`
9. Skip the job status endpoint — client must always be able to check progress
10. Use `Thread.sleep` in scheduled jobs — use Spring `@Scheduled` cron instead
11. Swallow exceptions silently in async code — always log with full stack trace
12. Auto-act on async results — always surface result to user for action