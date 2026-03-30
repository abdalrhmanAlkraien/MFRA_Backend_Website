# /review-page [N]

> **Slash command for OpenClaw / Claude Code**
> Triggered when user types: `/review-page 1` or `/review-page 2`
> Reviews a specific design page end-to-end — finds all tasks linked to it,
> checks implementation completeness against the spec and requirements,
> identifies every gap, and fixes what is missing.

---

## What This Command Does

Takes a page number, traces it through the entire project:
requirements → spec → tasks → implementation files → test files → test results.

At every layer it checks: was this fully done? If not — it fixes it.

Unlike `/fix-task` which targets a known failure, this command asks:
"Is page N actually complete — and how do I know?"

---

## Execution Steps

---

### Step 1 — Identify Everything Linked to Page N

```
Read: .claude/tasks.md
  → Find the entry for Page [N]
  → Extract:
    - Page name
    - Stage statuses (Design / Requirements / Backend / Frontend / Mobile)
    - Sub-tasks with IDs (e.g. Task 3.1, Task 3.2)
    - "Files Built from This Page" section (the exact file list)

Read: designs/[N].md                    → original business spec
Read: designs/[N]-requirements.md       → extracted requirements
Read: designs/DESIGN.md                 → design system (if it exists)
Read: .claude/systemTasks.md            → task details + statuses
```

If `tasks.md` has no entry for Page [N]:
```
⚠️  PAGE [N] NOT ANALYZED YET
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
designs/[N]-requirements.md does not exist.
This page has not been analyzed.

Run /analyze-page [N] first to generate requirements and tasks.
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

---

### Step 2 — Show Page Overview (Before Starting)

```
📄 REVIEWING PAGE [N] — [Page Name]
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Design:       designs/[N].png
Spec:         designs/[N].md
Requirements: designs/[N]-requirements.md
Analyzed:     DD/MM/YYYY

Tasks linked to this page:
  Task [X.1] — Backend     → [status]
  Task [X.2] — Frontend    → [status]
  Task [X.3] — Mobile      → [status] / ⛔ not in scope

Stage status:
  Design          ✅ Done
  Requirements    ✅ Done
  Backend         [status]
  Frontend        [status]
  Mobile          ⛔ / [status]

Files built from this page:
  [list from tasks.md "Files Built" section]
  (empty if tasks not yet complete)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Starting review across [N] layers...
```

---

### Step 3 — Layer 1: Requirements Review

Check that `designs/[N]-requirements.md` is complete and accurate.

```
Read: designs/[N].md          → what the business spec says
Read: designs/[N]-requirements.md → what was extracted

Check:
  ✅ / ❌  All UI sections identified (from design image)
  ✅ / ❌  All form fields mapped to DB columns
  ✅ / ❌  All API endpoints defined
  ✅ / ❌  All business rules captured
  ✅ / ❌  All edge cases from [N].md documented
  ✅ / ❌  Access/permission rules match [N].md
  ✅ / ❌  Out-of-scope items listed
  ✅ / ❌  Fix History section exists (if requirements were ever corrected)
```

If gaps found in requirements:
```
⚠️  REQUIREMENTS GAP — designs/[N]-requirements.md
  Missing: [specific field/rule/endpoint not in requirements]
  Source:  designs/[N].md says "[quote]"

  → Updating requirements now...
```

Update `designs/[N]-requirements.md` with the missing content.
Add a Fix History entry at the bottom.

---

### Step 4 — Layer 2: Spec Review

Find the SpecKit spec for the module this page belongs to.

```
Read: specs/<module>/spec.md
Read: specs/<module>/plan.md
Read: specs/<module>/tasks.md

Check:
  ✅ / ❌  Spec exists for this module
  ✅ / ❌  Acceptance criteria cover all requirements from [N]-requirements.md
  ✅ / ❌  API endpoints in spec match endpoints in requirements
  ✅ / ❌  Business rules in spec match requirements
  ✅ / ❌  Edge cases in spec cover edge cases in requirements
  ✅ / ❌  No contradictions between spec.md and requirements
```

If spec is missing:
```
⚠️  SPEC MISSING — specs/<module>/spec.md does not exist
  → Run /generate-spec <module> to create it
  → OR generating spec now from requirements...
```

If spec exists but has gaps:
```
⚠️  SPEC GAP — specs/<module>/spec.md
  Missing AC: [criterion from requirements not covered in spec]
  → Adding to spec.md now...
```

---

### Step 5 — Layer 3: Backend Implementation Review

For each backend task linked to this page (from tasks.md):

```
Read: .claude/processed/Task X.Y.md     → what was implemented
Read: designs/[N]-requirements.md       → what was required
Read: specs/<module>/spec.md            → acceptance criteria

Check every requirement → verify the implementation:

  Database:
  ✅ / ❌  Migration file exists: VX__create_<module>_tables.sql
  ✅ / ❌  All required columns exist in the migration
  ✅ / ❌  All indexes defined (status, slug, is_active, etc.)
  ✅ / ❌  Soft delete column: deleted_at TIMESTAMPTZ
  ✅ / ❌  Audit columns: created_at, updated_at, created_by, updated_by

  Entity:
  ✅ / ❌  Entity class exists and extends BaseEntity
  ✅ / ❌  All DB columns mapped as fields
  ✅ / ❌  All relationships defined

  Repository:
  ✅ / ❌  Repository exists
  ✅ / ❌  All custom queries use DeletedAtIsNull condition
  ✅ / ❌  Queries match what the service needs

  Service:
  ✅ / ❌  Service exists
  ✅ / ❌  All CRUD methods implemented
  ✅ / ❌  Slug generation present (if text content)
  ✅ / ❌  Cache invalidation on every write
  ✅ / ❌  @Transactional on write methods
  ✅ / ❌  Async email/notification triggered (if required by requirements)

  Controllers:
  ✅ / ❌  Admin controller exists with all required endpoints
  ✅ / ❌  Public controller exists (if page has public API)
  ✅ / ❌  Every admin endpoint has @PreAuthorize
  ✅ / ❌  DTOs used in all controllers (no entity exposure)
  ✅ / ❌  All endpoints match the requirements API table exactly

  Business Rules:
  For each business rule in [N]-requirements.md:
  ✅ / ❌  Rule [N]: "[rule text]" → implemented in [file]
```

For each ❌ found:
```
❌ BACKEND GAP — Task [X.Y]
   Missing: [exactly what is missing]
   Required by: designs/[N]-requirements.md → [section]
   Spec criterion: [AC-N]

   → Implementing now...
   [writes the missing code]
```

After fixing — run build verification:
```bash
mvn clean compile
```
Must be BUILD SUCCESS before continuing.

---

### Step 6 — Layer 4: Backend Test Review

Check that test files exist AND cover the page's requirements.

```
Check test file existence:
  ✅ / ❌  src/test/.../[Module]ServiceTest.java EXISTS
  ✅ / ❌  src/test/.../[Module]ControllerTest.java EXISTS

If EXISTS — check coverage:

  For each requirement in [N]-requirements.md:
    Is there a test method covering this requirement?

  Mandatory test groups:
  ✅ / ❌  Security: no token → 401 test
  ✅ / ❌  Security: wrong role → 403 test
  ✅ / ❌  Validation: missing required fields → 400 test
  ✅ / ❌  Happy path: create → 201 + DB verify
  ✅ / ❌  Happy path: get by ID → 200
  ✅ / ❌  Happy path: list → 200 paged
  ✅ / ❌  Happy path: update → 200
  ✅ / ❌  Soft delete: deletedAt set in DB
  ✅ / ❌  Soft delete: record hidden from API after delete
  ✅ / ❌  Public API: only published records returned
  ✅ / ❌  Business rules: one test per AC in spec.md
```

If test files are missing:
```
❌ BACKEND TESTS MISSING
   ServiceTest.java:    ❌ does not exist
   ControllerTest.java: ❌ does not exist

   → Writing test files now...
   [writes complete test classes]
```

If test files exist but have gaps:
```
❌ BACKEND TEST GAP
   Missing test: no token → 401 on POST /api/admin/[module]
   → Adding test method now...
```

After fixing tests — run them:
```bash
mvn clean verify
```

Show results:
```
🧪 BACKEND TESTS — Page [N]
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[TC-B-01] Create — happy path → 201          ✅
[TC-B-02] Create — no token → 401            ✅
[TC-B-03] Create — wrong role → 403          ✅
[TC-B-04] Create — missing field → 400       ✅
[TC-B-05] Get — not found → 404              ✅
[TC-B-06] Delete — soft delete verified      ✅
[TC-B-07] Public API — draft hidden          ✅
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[N] / [N] passed ✅ | Coverage: [N]%
```

Write results into `.claude/tests/Task X.Y - Backend Test Plan.md`.

---

### Step 7 — Layer 5: Frontend Implementation Review

For each frontend task linked to this page:

```
Read: .claude/processed/Task X.Y.md     → what was implemented
Read: designs/[N]-requirements.md       → what was required
Read: designs/DESIGN.md                 → design system tokens

Check:

  Files:
  ✅ / ❌  Page component exists at expected path
  ✅ / ❌  RTK Query api.ts slice exists for this module
  ✅ / ❌  types.ts exists with correct TypeScript types

  Design System:
  ✅ / ❌  tailwind.config.ts has custom color tokens (if DESIGN.md exists)
  ✅ / ❌  No generic gray-*/white/indigo-* colors (if DESIGN.md exists)
  ✅ / ❌  Correct font families used per DESIGN.md

  All UI Sections (from requirements):
  For each section in [N]-requirements.md → Sections table:
  ✅ / ❌  Section [N]: "[section name]" → component exists

  All Form Fields:
  For each field in the requirements → verify in component:
  ✅ / ❌  Field "[name]" → input rendered + validated + data-testid present

  States:
  ✅ / ❌  Loading state: skeleton or spinner shown during API call
  ✅ / ❌  Empty state: empty-state component shown when no data
  ✅ / ❌  Error state: error component shown when API fails
  ✅ / ❌  Success state: shown after form submit (if applicable)

  Auth:
  ✅ / ❌  Admin pages wrapped in AuthGuard
  ✅ / ❌  Public pages accessible without auth

  Routing:
  ✅ / ❌  Route registered in router config
  ✅ / ❌  Correct URL matches requirements

  Business Rules:
  For each rule in [N]-requirements.md → verify in frontend code:
  ✅ / ❌  Rule [N]: "[rule text]" → handled in component
```

For each ❌ found:
```
❌ FRONTEND GAP — Task [X.Z]
   Missing: [exactly what]
   Required by: designs/[N]-requirements.md → [section/rule]

   → Implementing now...
   [writes the missing component/logic/state]
```

After fixing — run build check:
```bash
npm run build
```
Must be 0 TypeScript errors before continuing.

---

### Step 8 — Layer 6: Frontend Test Review

```
Check test file existence:
  ✅ / ❌  tests/e2e/<module>/<page>.spec.ts EXISTS

If EXISTS — check scenario coverage:
  ✅ / ❌  TC-F-01: page load (happy path)
  ✅ / ❌  TC-F-02: auth guard redirect (if admin page)
  ✅ / ❌  TC-F-03: form submit valid (if form exists)
  ✅ / ❌  TC-F-04: form validation empty fields (if form exists)
  ✅ / ❌  TC-F-05: loading state
  ✅ / ❌  TC-F-06: empty state
  ✅ / ❌  TC-F-07: error state
  ✅ / ❌  TC-F-08: responsive (tablet + mobile)
  ✅ / ❌  Module-specific: one test per requirement not covered above

Check test results:
  ✅ / ❌  .claude/tests/Task X.Y - Frontend Test Plan.md exists
  ✅ / ❌  Plan file has no ⏳ NOT EXECUTED rows (all executed)
  ✅ / ❌  All scenario rows show ✅ PASS
  ✅ / ❌  Screenshots exist in .claude/screenshots/
```

If tests missing:
```
❌ FRONTEND TESTS MISSING
   tests/e2e/<module>/<page>.spec.ts: ❌ does not exist

   → Writing Playwright spec file now...
   [writes complete spec file]
```

After writing spec file — execute tests:
```bash
npx playwright test tests/e2e/<module>/<page>.spec.ts
```

Show results per scenario:
```
🧪 FRONTEND TESTS — Page [N] (Playwright)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[TC-F-01] Page load — content renders        ✅  screenshot saved
[TC-F-02] Auth guard — redirects to login    ✅  screenshot saved
[TC-F-03] Form submit — success state        ✅  screenshot saved
[TC-F-04] Form validation — errors shown     ✅  screenshot saved
[TC-F-05] Loading state — skeleton visible   ✅  screenshot saved
[TC-F-06] Empty state — message shown        ✅  screenshot saved
[TC-F-07] Error state — error UI shown       ✅  screenshot saved
[TC-F-08] Responsive — no horizontal scroll  ✅  screenshots saved
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[N] / [N] passed ✅
```

Write results into `.claude/tests/Task X.Y - Frontend Test Plan.md`.

---

### Step 9 — Layer 7: Mobile Review (if Flutter in stack.md)

Same pattern as Layers 5 and 6 but for Flutter:

```
Check:
  ✅ / ❌  lib/features/<module>/presentation/<module>_page.dart EXISTS
  ✅ / ❌  lib/features/<module>/data/<module>_repository.dart EXISTS
  ✅ / ❌  test/features/<module>/<module>_test.dart EXISTS

If any missing → implement and write tests
Run: flutter analyze → 0 issues
Run: flutter test test/features/<module>/
```

---

### Step 10 — Layer 8: curl DB Audit Review

```
Read: doc/DATABASE_AUDIT.md

For each write operation defined in [N]-requirements.md API table:
  Is there an entry in DATABASE_AUDIT.md?
  ✅ / ❌  CREATE operation verified
  ✅ / ❌  UPDATE operation verified
  ✅ / ❌  DELETE (soft) verified
  ✅ / ❌  Status change (publish) verified
  ✅ / ❌  All entries show ✅ MATCH (not ❌ MISMATCH)
```

If entries are missing:
```
❌ DB AUDIT MISSING — doc/DATABASE_AUDIT.md
   No entry for: POST /api/admin/[module]
   → Running curl smoke test now...
   [runs curl, checks DB, writes entry to DATABASE_AUDIT.md]
```

---

### Step 11 — Layer 9: Full Cycle Behavioral Verification

**This is the final gate. Nothing passes without this.**

Layers 1–8 verified that the right code exists and unit/integration tests pass.
This layer verifies that the whole system actually WORKS together — UI renders,
user actions trigger the right API calls, data saves to the database correctly,
and the page responds as a real user would experience it.

**This layer only runs after Layers 1–8 are all ✅.**
If any earlier layer is still ❌, fix it first.

---

#### Phase A — Start the Full Stack

```bash
# Terminal 1 — Start backend
mvn spring-boot:run
# Confirm: "Started Application in X seconds"
# Confirm: listening on port 8080

# Terminal 2 — Start frontend
npm run dev
# Confirm: "Local: http://localhost:5173"
# Confirm: no compile errors in output
```

If either fails to start:
```
❌ FULL CYCLE BLOCKED — [backend/frontend] cannot start
   Error: [paste exact startup error]
   → Fix startup error first → re-attempt Layer 9
```

If mobile is in scope:
```bash
# Terminal 3 — Run mobile on emulator
flutter run
# Confirm: app launches on emulator without crash
```

---

#### Phase B — Frontend → Backend → Database Full Cycle

Run Playwright in headful mode to simulate real user behavior.
Every action is verified at THREE levels simultaneously:
1. **UI behavior** — what the user sees in the browser
2. **API call** — the correct endpoint was called with correct data
3. **Database state** — the correct row exists with correct values

**Playwright intercepts network calls and verifies DB state via the admin API.**

---

##### Cycle 1: CREATE flow (if page has create action)

```javascript
// ── STEP 1: User fills and submits the form ──
await page.goto('http://localhost:5173/[page-url]');
await page.waitForLoadState('networkidle');

// Fill every required field from designs/[N]-requirements.md
await page.fill('[name="fieldName"]', 'test value');
// ... all fields ...

// Capture network call on submit
let capturedRequest = null;
let capturedResponse = null;
page.on('request', req => {
  if (req.url().includes('/api/') && req.method() === 'POST') {
    capturedRequest = { url: req.url(), body: req.postData() };
  }
});
page.on('response', async res => {
  if (res.url().includes('/api/') && res.request().method() === 'POST') {
    capturedResponse = { status: res.status(), body: await res.json() };
  }
});

await page.click('[data-testid="submit-btn"]');
await page.waitForLoadState('networkidle');

// ── STEP 2: Verify UI shows correct response ──
console.log('UI CHECK:');
const successVisible = await page.locator('[data-testid="success-message"]').isVisible();
console.log('  Success state shown:', successVisible ? '✅' : '❌');

// ── STEP 3: Verify API call was made correctly ──
console.log('API CHECK:');
console.log('  Endpoint called:', capturedRequest?.url?.includes('/api/[module]') ? '✅' : '❌');
console.log('  Method: POST:', capturedRequest ? '✅' : '❌');
console.log('  Response status 201:', capturedResponse?.status === 201 ? '✅' : '❌');
const newRecordId = capturedResponse?.body?.data?.id;
console.log('  Record ID returned:', newRecordId ? '✅ ' + newRecordId : '❌');

// ── STEP 4: Verify Database — query via admin API ──
console.log('DATABASE CHECK:');
const dbCheck = await page.evaluate(async (id) => {
  const res = await fetch(`http://localhost:8080/api/admin/[module]/${id}`, {
    headers: { Authorization: 'Bearer ' + localStorage.getItem('token') }
  });
  return await res.json();
}, newRecordId);

console.log('  Record exists in DB:', dbCheck?.data?.id === newRecordId ? '✅' : '❌');
console.log('  Field values match submitted data:', '✅ / ❌ (check each field)');
console.log('  Status is correct (DRAFT/ACTIVE):', dbCheck?.data?.status === 'DRAFT' ? '✅' : '❌');
console.log('  created_at is set:', dbCheck?.data?.createdAt ? '✅' : '❌');
console.log('  deleted_at is null (not soft deleted):', dbCheck?.data?.deletedAt === null ? '✅' : '❌');

await page.screenshot({ path: 'screenshots/fullcycle-[N]-create.png' });
```

---

##### Cycle 2: READ / LIST flow

```javascript
// Navigate to list page
await page.goto('http://localhost:5173/[list-url]');
await page.waitForLoadState('networkidle');

// ── STEP 1: UI shows records ──
const cards = await page.locator('[data-testid="[module]-card"]').count();
console.log('LIST CHECK:');
console.log('  Records rendered:', cards > 0 ? `✅ ${cards} items` : '❌ None');

// ── STEP 2: API call verified ──
// (network listener added before navigation)
console.log('  GET /api/[module] called:', '✅ / ❌');
console.log('  Response 200:', '✅ / ❌');

// ── STEP 3: UI data matches API response ──
const firstTitle = await page.locator('[data-testid="[module]-card"]:first-child [data-testid="title"]').textContent();
console.log('  First item title matches API:', firstTitle ? '✅ ' + firstTitle : '❌');

await page.screenshot({ path: 'screenshots/fullcycle-[N]-list.png' });
```

---

##### Cycle 3: UPDATE flow (if page has edit action)

```javascript
// Navigate to edit page for the record created in Cycle 1
await page.goto(`http://localhost:5173/[edit-url]/${newRecordId}`);
await page.waitForLoadState('networkidle');

// ── STEP 1: Form pre-populated with existing data ──
const existingValue = await page.inputValue('[name="fieldName"]');
console.log('UPDATE CHECK:');
console.log('  Form pre-populated:', existingValue.length > 0 ? '✅' : '❌');

// Update a field
await page.fill('[name="fieldName"]', 'updated value');
await page.click('[data-testid="save-btn"]');
await page.waitForLoadState('networkidle');

// ── STEP 2: API call ──
console.log('  PUT/PATCH /api/[module]/:id called:', '✅ / ❌');
console.log('  Response 200:', '✅ / ❌');

// ── STEP 3: Database reflects update ──
const updatedRecord = await page.evaluate(async (id) => {
  const res = await fetch(`http://localhost:8080/api/admin/[module]/${id}`, {
    headers: { Authorization: 'Bearer ' + localStorage.getItem('token') }
  });
  return await res.json();
}, newRecordId);

console.log('  DB field updated:', updatedRecord?.data?.fieldName === 'updated value' ? '✅' : '❌');
console.log('  updated_at changed:', updatedRecord?.data?.updatedAt ? '✅' : '❌');

await page.screenshot({ path: 'screenshots/fullcycle-[N]-update.png' });
```

---

##### Cycle 4: STATUS CHANGE flow (publish / approve / activate — if applicable)

```javascript
// Trigger status change action
await page.click('[data-testid="publish-btn"]');
await page.waitForLoadState('networkidle');

// ── STEP 1: UI shows new status ──
const statusBadge = await page.locator('[data-testid="status-badge"]').textContent();
console.log('STATUS CHANGE CHECK:');
console.log('  UI shows PUBLISHED:', statusBadge?.includes('PUBLISHED') ? '✅' : '❌ Got: ' + statusBadge);

// ── STEP 2: API call ──
console.log('  PATCH /api/[module]/:id/publish called:', '✅ / ❌');

// ── STEP 3: Database ──
const publishedRecord = await page.evaluate(async (id) => {
  const res = await fetch(`http://localhost:8080/api/admin/[module]/${id}`, {
    headers: { Authorization: 'Bearer ' + localStorage.getItem('token') }
  });
  return await res.json();
}, newRecordId);

console.log('  DB status = PUBLISHED:', publishedRecord?.data?.status === 'PUBLISHED' ? '✅' : '❌');
console.log('  DB publishedAt set:', publishedRecord?.data?.publishedAt ? '✅' : '❌');

// ── STEP 4: Verify visible on public API now ──
const publicCheck = await page.evaluate(async (slug) => {
  const res = await fetch(`http://localhost:8080/api/public/[module]/${slug}`);
  return res.status;
}, publishedRecord?.data?.slug);

console.log('  Public API accessible after publish:', publicCheck === 200 ? '✅' : '❌ Status: ' + publicCheck);

await page.screenshot({ path: 'screenshots/fullcycle-[N]-publish.png' });
```

---

##### Cycle 5: DELETE flow (soft delete verification)

```javascript
// Trigger delete
await page.click('[data-testid="delete-btn"]');
// Confirm modal
await page.click('[data-testid="confirm-delete-btn"]');
await page.waitForLoadState('networkidle');

// ── STEP 1: UI removes the record ──
const stillVisible = await page.locator(`[data-testid="[module]-row-${newRecordId}"]`).isVisible().catch(() => false);
console.log('DELETE CHECK:');
console.log('  Record removed from UI:', !stillVisible ? '✅' : '❌ Still visible');

// ── STEP 2: API call ──
console.log('  DELETE /api/[module]/:id called:', '✅ / ❌');
console.log('  Response 200:', '✅ / ❌');

// ── STEP 3: Database — soft delete only ──
const deletedRecord = await page.evaluate(async (id) => {
  // Direct DB check via admin — soft deleted records may return 404 via API
  // Use raw DB query via a debug endpoint, or check that API returns 404
  const res = await fetch(`http://localhost:8080/api/admin/[module]/${id}`, {
    headers: { Authorization: 'Bearer ' + localStorage.getItem('token') }
  });
  return { status: res.status };
}, newRecordId);

console.log('  API returns 404 after delete:', deletedRecord?.status === 404 ? '✅' : '❌');
console.log('  DB row still exists (soft delete):', '✅ confirmed via DATABASE_AUDIT.md');
console.log('  DB deleted_at is NOT null:', '✅ confirmed via DATABASE_AUDIT.md');

// ── STEP 4: Public API also returns 404 after delete ──
const publicAfterDelete = await page.evaluate(async () => {
  const res = await fetch('http://localhost:8080/api/public/[module]/[slug]');
  return res.status;
});
console.log('  Public API 404 after delete:', publicAfterDelete === 404 ? '✅' : '❌');

await page.screenshot({ path: 'screenshots/fullcycle-[N]-delete.png' });
```

---

##### Cycle 6: AUTH PROTECTION cycle

```javascript
// Clear auth — simulate logged out
await page.evaluate(() => localStorage.clear());
await page.goto('http://localhost:5173/admin/[page-url]');
await page.waitForLoadState('networkidle');

console.log('AUTH CYCLE CHECK:');
// ── UI redirects ──
const url = page.url();
console.log('  Logged-out user redirected to login:', url.includes('/admin/login') ? '✅' : '❌ Stayed at: ' + url);

// ── API also rejects — direct call without token ──
const directApiCall = await page.evaluate(async () => {
  const res = await fetch('http://localhost:8080/api/admin/[module]');
  return res.status;
});
console.log('  API returns 401 without token:', directApiCall === 401 ? '✅' : '❌ Got: ' + directApiCall);

// ── Login and confirm access restored ──
await page.fill('[name="email"]', 'admin@mfra.com');
await page.fill('[name="password"]', 'admin123');
await page.click('button[type="submit"]');
await page.waitForURL('**/admin/dashboard');
console.log('  Login works and redirects to dashboard:', page.url().includes('/admin/dashboard') ? '✅' : '❌');

await page.screenshot({ path: 'screenshots/fullcycle-[N]-auth.png' });
```

---

#### Phase C — Mobile Full Cycle (if Flutter in stack.md)

```bash
# Run integration test on emulator — tests the full flow
flutter test integration_test/<module>_integration_test.dart --device-id <emulator-id>
```

Integration test covers:
- App launches without crash ✅
- Screen renders with correct UI elements ✅
- API call fires on load ✅
- Data renders correctly in list/detail ✅
- Form submits and updates DB ✅
- Error state shows on network failure ✅

---

#### Phase D — Full Cycle Results Summary

Present results clearly per cycle:

```
🔄 FULL CYCLE VERIFICATION — Page [N]: [Page Name]
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Cycle 1 — CREATE
  UI:       Success state shown after submit            ✅
  API:      POST /api/admin/[module] → 201              ✅
  Database: Row created with correct fields             ✅
            created_at set                              ✅
            deleted_at = null                           ✅
            status = DRAFT                              ✅

Cycle 2 — READ / LIST
  UI:       Records render in list                      ✅
  API:      GET /api/admin/[module] → 200               ✅
  Database: Data matches what API returned              ✅

Cycle 3 — UPDATE
  UI:       Form pre-populated with existing data       ✅
            Success state after save                    ✅
  API:      PUT /api/admin/[module]/:id → 200           ✅
  Database: Field updated in DB                         ✅
            updated_at changed                          ✅

Cycle 4 — STATUS CHANGE (Publish)
  UI:       Status badge shows PUBLISHED                ✅
  API:      PATCH /api/admin/[module]/:id/publish → 200 ✅
  Database: status = PUBLISHED                          ✅
            publishedAt set                             ✅
  Public:   GET /api/public/[module]/:slug → 200        ✅

Cycle 5 — DELETE (Soft)
  UI:       Record removed from list                    ✅
  API:      DELETE /api/admin/[module]/:id → 200        ✅
            GET after delete → 404                      ✅
  Database: Row still exists (soft delete)              ✅
            deleted_at is NOT null                      ✅
  Public:   GET /api/public/[module]/:slug → 404        ✅

Cycle 6 — AUTH PROTECTION
  UI:       Logged-out redirect to login                ✅
  API:      Direct call without token → 401             ✅
  Login:    Login → redirect to dashboard               ✅

Mobile:     ⛔ Not in scope

Screenshots:
  fullcycle-[N]-create.png   ✅
  fullcycle-[N]-list.png     ✅
  fullcycle-[N]-update.png   ✅
  fullcycle-[N]-publish.png  ✅
  fullcycle-[N]-delete.png   ✅
  fullcycle-[N]-auth.png     ✅

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
FULL CYCLE: ✅ ALL CYCLES PASSED
```

If any cycle fails:
```
❌ FULL CYCLE FAILURE — Cycle 3 (UPDATE)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Problem:    Database field NOT updated after PATCH call
UI showed:  Success message ✅
API:        PATCH → 200 ✅
Database:   field = old value ❌ (expected: "updated value")

Root cause: Mapper.toEntity() not applying partial update correctly
           UpdateRequest fields not mapped in BlogMapper.java

Fix:        [fix mapper] → re-run Cycle 3 → confirm ✅
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

Fix and re-run only the failed cycle — not all 6.

---

### Step 12 — Update All Tracking Files

After all gaps are found and fixed:

**Update `.claude/tasks.md` — Page [N] entry:**
- Set stage statuses to ✅ for everything now complete
- Fill in the "Files Built from This Page" section if it was empty

**Update `.claude/systemTasks.md`:**
- Mark any tasks that are now complete as ✅ COMPLETED
- Add review note: "Reviewed and completed via /review-page [N] on DD/MM/YYYY"

---

### Step 13 — Present Full Review Report

```
✅ PAGE [N] REVIEW COMPLETE — [Page Name]
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

LAYER RESULTS:
  Layer 1 — Requirements     ✅ Complete  (0 gaps / [N] gaps fixed)
  Layer 2 — Spec             ✅ Complete  (0 gaps / [N] gaps fixed)
  Layer 3 — Backend impl     ✅ Complete  (0 gaps / [N] gaps fixed)
  Layer 4 — Backend tests    ✅ Complete  ([N]/[N] passed | [N]% coverage)
  Layer 5 — Frontend impl    ✅ Complete  (0 gaps / [N] gaps fixed)
  Layer 6 — Frontend tests   ✅ Complete  ([N]/[N] Playwright scenarios)
  Layer 7 — Mobile           ⛔ Not in scope (Flutter not in stack.md)
  Layer 8 — DB Audit         ✅ Complete  (all curl entries ✅ MATCH)
  Layer 9 — Full Cycle       ✅ All 6 cycles passed

FULL CYCLE RESULTS:
  Cycle 1 CREATE:        UI ✅ | API 201 ✅ | DB row created ✅
  Cycle 2 READ/LIST:     UI ✅ | API 200 ✅ | DB data matches ✅
  Cycle 3 UPDATE:        UI ✅ | API 200 ✅ | DB field updated ✅
  Cycle 4 STATUS CHANGE: UI ✅ | API 200 ✅ | DB status updated ✅
                         Public API accessible after publish ✅
  Cycle 5 DELETE:        UI ✅ | API 200 ✅ | DB soft delete ✅
                         deleted_at set ✅ | row still exists ✅
                         Public API 404 after delete ✅
  Cycle 6 AUTH:          Logged-out redirect ✅ | Direct API 401 ✅
                         Login → dashboard ✅

GAPS FOUND AND FIXED DURING REVIEW:
  [list each gap: layer → what was missing → what was done]
  OR: "None — all layers were already complete"

TEST COUNTS:
  Backend:   [N] / [N] passed ✅ | Coverage: [N]%
  Frontend:  [N] / [N] Playwright scenarios ✅
  Mobile:    ⛔ not in scope

SCREENSHOTS SAVED:
  .claude/screenshots/fullcycle-[N]-create.png
  .claude/screenshots/fullcycle-[N]-list.png
  .claude/screenshots/fullcycle-[N]-update.png
  .claude/screenshots/fullcycle-[N]-publish.png
  .claude/screenshots/fullcycle-[N]-delete.png
  .claude/screenshots/fullcycle-[N]-auth.png

FILES MODIFIED DURING REVIEW:
  [list every file changed or created — or "None"]

TASKS AFTER REVIEW:
  Task [X.1] — Backend   → ✅ COMPLETED
  Task [X.2] — Frontend  → ✅ COMPLETED

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Page [N] passed all 9 layers including full cycle verification.
Every user action verified at UI + API + Database level.

Options:
  1️⃣  /review-page [N+1]  → review the next page
  2️⃣  /review-progress    → see full project status
  3️⃣  /continue-tasks     → run remaining unstarted tasks
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

---

## Partial Completion Handling

If some layers are complete and others are not — the command picks up
from the first incomplete layer:

```
📄 REVIEWING PAGE [N] — [Page Name]
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Layer 1 Requirements  ✅ Already complete — skipping
Layer 2 Spec          ✅ Already complete — skipping
Layer 3 Backend impl  ✅ Already complete — skipping
Layer 4 Backend tests ❌ Tests missing — starting here
...
Layer 9 Full Cycle   ⏳ Waiting for layers 1–8 to complete
...
```

It never re-implements what is already correct — only fills the gaps.

---

## When a Page Has Multiple Tasks

Some pages generate multiple tasks (e.g. page 2 Dashboard generates tasks for
backend summary APIs, access log API, node performance API).

The review covers ALL tasks linked to the page, not just the first one:

```
Page 2 — Dashboard
  Task 2.1 — Backend: Dashboard summary API    → Layer 3 + 4
  Task 2.2 — Backend: Access logs API          → Layer 3 + 4
  Task 2.3 — Frontend: Dashboard page + cards  → Layer 5 + 6
```

Each task is reviewed in sequence. The final report shows all tasks.

---

## Key Differences From Other Commands

| Command | Purpose | Modifies Code? |
|---|---|---|
| `/review-progress` | Status snapshot of all tasks | Never |
| `/fix-task [id]` | Fix one known failing task | Yes — one task |
| `/review-page [N]` | Full audit + full cycle behavioral verification | Yes — everything for this page |
| `/continue-tasks` | Run next pending tasks | Yes — implements new tasks |

---

## Quick Reference

```
User types:      /review-page [N]
Agent reads:     tasks.md → finds tasks for page N
                 designs/[N].md + [N]-requirements.md → what was required
                 specs/<module>/spec.md → acceptance criteria
                 processed/Task X.Y.md → what was implemented
                 test files → what was tested
Agent checks:    9 layers: requirements → spec → backend → backend tests
                           → frontend → frontend tests → mobile → DB audit
                           → full cycle (UI + API + database behavioral verification)
Agent fixes:     Everything missing or incomplete at each layer
Agent runs:      mvn clean verify + npx playwright test after each fix
Agent updates:   tasks.md + systemTasks.md + plan files
Agent presents:  Full gap report + what was fixed + final test results
Scope:           Only files linked to page N — never touches other pages
Final gate:      Full cycle must pass before page is marked complete
                 Every user action verified at UI + API + Database level
```