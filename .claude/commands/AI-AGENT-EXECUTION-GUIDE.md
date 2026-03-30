# AI Agent Execution Guide

**Document Purpose:** Complete instructions for AI agents (OpenClaw, Claude Code) to execute tasks in any project using this template system.

**Read Order:** This file is read FIRST before any other file in every session.
**Stack Reference:** All technology decisions are driven by `project/stack.md` — never assume.
**Spec Reference:** All requirements come from `specs/<module>/spec.md` — never assume.

---

## 📋 Overview

This project uses a **structured task execution system** with:
- SpecKit spec-driven development — spec must exist before implementation
- Stack-driven execution — only build what is defined in `project/stack.md`
- Automated testing — backend (JUnit + MockMvc), frontend (Playwright), mobile (Flutter test)
- Configuration tracking — `configurations.md` updated after every config setup
- Requirements feedback loop — failed tests can update requirements, not just code
- Atomic tracking — `systemTasks.md` and `tasks.md` always updated together
- Quality gates at every step — cannot mark complete without passing all gates

**Supported platforms** (only if present in `project/stack.md`):
- ✅ Backend — Spring Boot (always)
- ✅ Frontend — React + TypeScript (if listed in stack.md)
- ✅ Mobile — Flutter (if listed in stack.md)

**Command reference:**
```
/check-ready       → validate project setup before any execution
/analyze-designs   → full BA analysis of all design images
/analyze-page [N]  → single page analysis, merge into existing plan
/generate-spec [m] → requirements → SpecKit spec bridge
/execute-task      → execute the next pending task
/continue-tasks    → run all tasks without stopping
/fix-task [id]     → fix a known failing task (code or requirements)
/review-page [N]   → audit page N end-to-end — find and fix all gaps
/review-progress   → full project status snapshot
/add-feature "..." → add a new feature mid-project
```

**Your role as AI agent:** Execute tasks exactly as defined in SpecKit specs, following all instruction files, generating tests for every applicable platform, documenting everything, and never skipping a step.

---

## 🎯 Quick Start

### Trigger: Analyze Designs + Generate Phases

When user says **"analyze designs"**, **"analyze all designs"**, or
**"generate project phases"**:

```
→ Run /analyze-designs command
   Full workflow defined in: commands/analyze-designs.md
```

When user says **"analyze page [N]"** or **"analyze 3"**:
```
→ Run /analyze-page [N] command
   Full workflow defined in: commands/analyze-page.md
```

When user says **"generate spec for [module]"**:
```
→ Run /generate-spec [module] command
   Full workflow defined in: commands/generate-spec.md
```

---

### Trigger: Execute Task

When user says **"execute task"**, **"continue"**, or **"start next task"**, the agent follows this exact sequence:

### Step 0 — Pre-Flight Check

Before asking anything, run `/check-ready` in silent mode:

```
Read: commands/check-ready.md → run all 9 validation layers

If any BLOCKING condition found:
  → Report to user: "Cannot proceed — [reason]. Fix: [command]"
  → Stop here

If all checks pass (or warnings only):
  → Continue to Step 0a silently
```

### Step 0a — Ask the User

Before doing anything, ask:

```
📋 TASK READY TO EXECUTE
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Next task: [Task ID] — [Task Name]
Module: [module name]
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
What would you like to implement for this task?

  1️⃣  Backend only
  2️⃣  Frontend only
  3️⃣  Mobile only
  4️⃣  Backend + Frontend
  5️⃣  Backend + Mobile
  6️⃣  All (Backend + Frontend + Mobile)

Note: Only platforms listed in project/stack.md are available.
Type a number or say "all" to proceed.
```

**Wait for the user's answer before proceeding.**
If user says "all" — check `project/stack.md` and only build what is listed there.

---

### Step 1 — Read Context Files

```
Read in this exact order:
  1. .claude/CLAUDE.md                    → Project context, modules, budget
  2. .claude/configurations.md            → What configs are ready / missing
  3. .claude/systemTasks.md               → Find current PENDING task
  4. project/stack.md                     → What platforms and providers are in scope
  5. designs/DESIGN.md                    → Project design system (if file exists)
  6. specs/<module>/spec.md               → Acceptance criteria
  7. specs/<module>/plan.md               → Implementation decisions
  8. specs/<module>/tasks.md              → Task scope and boundaries
  9. designs/[N]-requirements.md          → Source requirements for this module
     (find [N] from the source page listed in systemTasks.md for this task)
```

**`designs/DESIGN.md`** — if this file exists, it defines the color tokens,
typography system, component patterns, and spacing rules for this project.
Read it before writing any frontend code. It overrides all generic Tailwind
color defaults for this project.

Reading `designs/[N]-requirements.md` is mandatory. It is the source of truth
for what the page should do. If spec.md contradicts requirements, raise a
clarification before implementing — never silently pick one over the other.

**Extract from spec.md:**
- Acceptance criteria → each becomes a test scenario
- API endpoints → each becomes a curl smoke test
- UI components → each becomes a Playwright scenario
- Edge cases → each becomes an error test

---

### Step 2 — Verify Dependencies

```
Check systemTasks.md for this task's dependencies.

All dependencies ✅ COMPLETED? → Proceed to Step 3
Any dependency ⏳ PENDING?     → Stop and inform user:
  "Task [X.Y] is blocked. [Dependency task] must be completed first.
   Would you like me to execute [Dependency task] now?"
Any dependency ❌ FAILED?      → Stop and inform user:
  "Task [X.Y] is blocked. [Dependency task] failed and must be fixed first."
```

---

### Step 3 — Read Instruction Files

Read the relevant instruction files based on what the task involves:

```
Always read:
  instructions/backend.md               (if implementing backend)
  instructions/frontend.md              (if implementing frontend)
  instructions/database.md              (if task has schema changes)
  instructions/backend-testing.md       (if implementing backend)
  instructions/frontend-testing.md      (if implementing frontend)
  designs/DESIGN.md                     (if implementing frontend AND file exists)

Read when triggered by spec signals:
  instructions/async.md                 (email, jobs, exports, scheduled)
  instructions/cache.md                 (public lists, stats — check stack.md)
  instructions/email.md                 (confirmation, notification emails)
  instructions/sms.md                   (OTP, mobile alerts — check stack.md)
  instructions/notification.md          (push notifications — check stack.md)
  instructions/whatsapp.md              (WA button or messages — check stack.md)
  instructions/websocket.md             (real-time features — check stack.md)
```

**After reading relevant instruction files — check configurations.md:**
```
For each instruction file read:
  Is the required config ✅ READY in configurations.md?
    YES → proceed
    NO  → set up the config first, then mark ✅ READY in configurations.md
```

---

### Step 4 — Present Task Summary

Before writing a single line of code, present this summary and wait for confirmation:

```
📋 TASK [X.Y]: [Task Name]
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Objective:    [one sentence from spec]
Module:       [module name]
Spec:         specs/<module>/spec.md
Platforms:    [Backend ✅] [Frontend ✅/—] [Mobile ✅/—]

BACKEND files to create:
  src/main/java/com/<pkg>/<module>/entity/<Module>Entity.java
  src/main/java/com/<pkg>/<module>/repository/<Module>Repository.java
  src/main/java/com/<pkg>/<module>/service/<Module>Service.java
  src/main/java/com/<pkg>/<module>/controller/<Module>AdminController.java
  src/main/java/com/<pkg>/<module>/controller/<Module>PublicController.java
  src/main/java/com/<pkg>/<module>/dto/<Module>CreateRequest.java
  src/main/java/com/<pkg>/<module>/dto/<Module>Response.java
  src/main/resources/db/migration/VX__create_<module>_tables.sql

FRONTEND files to create:  (if applicable)
  src/features/<module>/api.ts
  src/features/<module>/types.ts
  src/features/<module>/pages/<Module>ListPage.tsx
  src/features/<module>/components/<Module>Card.tsx

MOBILE files to create:  (if applicable)
  lib/features/<module>/data/<module>_repository.dart
  lib/features/<module>/presentation/<module>_page.dart
  lib/features/<module>/presentation/widgets/<module>_card.dart

Acceptance criteria: [X] items from spec
Estimated tests: [N] backend + [N] frontend + [N] mobile
Config checks: [list any configs needed]
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Ready to proceed? (yes/no)
```

**Wait for "yes" before continuing. If user says "no" or asks questions — answer them first.**

---

### Step 5 — Implementation

Show progress as each file is created:

```
⚙️  IMPLEMENTING [Task X.Y] — [Task Name]
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

BACKEND [1/4]: Database schema...
  ✓ VX__create_blog_tables.sql

BACKEND [2/4]: Entity + Repository...
  ✓ BlogEntity.java
  ✓ BlogRepository.java

BACKEND [3/4]: Service + Controllers...
  ✓ BlogService.java
  ✓ BlogAdminController.java
  ✓ BlogPublicController.java

BACKEND [4/4]: DTOs + Mappers...
  ✓ BlogCreateRequest.java
  ✓ BlogUpdateRequest.java
  ✓ BlogResponse.java

FRONTEND [1/3]: API + Types...
  ✓ features/blog/api.ts
  ✓ features/blog/types.ts

FRONTEND [2/3]: Pages...
  ✓ features/blog/pages/BlogListPage.tsx
  ✓ features/blog/pages/BlogDetailPage.tsx

FRONTEND [3/3]: Components...
  ✓ features/blog/components/BlogCard.tsx
  ✓ features/blog/components/BlogFilter.tsx

MOBILE [1/2]: Data layer...
  ✓ lib/features/blog/data/blog_repository.dart
  ✓ lib/features/blog/data/models/blog_model.dart

MOBILE [2/2]: Presentation layer...
  ✓ lib/features/blog/presentation/blog_list_page.dart
  ✓ lib/features/blog/presentation/widgets/blog_card_widget.dart
```

**If a requirement from the spec is unclear during implementation:**
```
❓ CLARIFICATION NEEDED
━━━━━━━━━━━━━━━━━━━━━━━━
The spec says: "[quote from spec]"
I need to clarify: [specific question]

Options:
  A) [interpretation A]
  B) [interpretation B]
  C) Let me know the correct approach

Pausing implementation until clarified.
```

**If spec.md contradicts designs/[N]-requirements.md during implementation:**
```
⚠️  SPEC / REQUIREMENTS CONFLICT
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
spec.md says:        "[quote]"
requirements say:    "[quote from designs/[N]-requirements.md]"
Design image shows:  [what is visually in the design]

These conflict. No code written yet — pausing until resolved.

Options:
  A) Follow the design image (requirements + spec will be updated to match)
  B) Follow the spec as written (requirements will be updated to match)
  C) [Third interpretation if applicable]
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
After user decides — update the losing file before writing any code.
```

---

### Step 6 — Build Verification

**Run builds before any tests. All platforms must compile clean:**

```bash
# Backend
mvn clean compile
# Must show: BUILD SUCCESS — 0 errors

# Frontend (if applicable)
npm run build
# Must show: 0 TypeScript errors, 0 warnings

# Mobile (if applicable)
flutter analyze
# Must show: No issues found
```

**If any build fails:**
```
❌ BUILD FAILED
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Platform: [Backend / Frontend / Mobile]
Error: [exact error message]
File: [file with error]
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Fixing now...
```

Fix all errors before proceeding to tests. Never skip.

---

### Step 7 — Write Test Code Files

**This step happens BEFORE any test runner is executed.**
Write the actual test code — the `.java`, `.spec.ts`, and `_test.dart` files —
based on the scenarios from the spec. Then in Step 8 generate the plan file.
Then in Step 9 run the tests.

This is the step that was missing. Planning without code produces nothing.
The test package is empty because this step was being skipped.

---

#### Write Backend Test Classes

Two Java files per module — always:

**File 1:** `src/test/java/com/<pkg>/<module>/<Module>ServiceTest.java`
→ Unit tests using Mockito — tests business logic in isolation
→ Read `instructions/backend-testing.md` → Unit Test Pattern section

**File 2:** `src/test/java/com/<pkg>/<module>/<Module>ControllerTest.java`
→ Integration tests using MockMvc + Testcontainers
→ Read `instructions/backend-testing.md` → Integration Test Pattern section

Write these files now — before running `mvn verify`. The test package must
not be empty when the build runs.

**Mandatory test methods per controller file:**

```java
// Every ControllerTest must contain ALL of these method groups:

// ── GROUP 1: CREATE ──────────────────────────────────────
@Test void create_asAdmin_returns201()           // happy path + DB verify
@Test void create_noToken_returns401()           // security
@Test void create_editorRole_returns403()        // role check (if admin-only)
@Test void create_blankRequiredField_returns400() // validation
@Test void create_allFieldsMissing_returns400()   // validation all fields
@Test void create_duplicateSlug_returns409()      // conflict (if slug exists)

// ── GROUP 2: GET LIST ─────────────────────────────────────
@Test void list_asAdmin_returns200WithPagedResult()
@Test void list_noToken_returns401()
@Test void list_filterByStatus_returnsCorrectSubset() // if filterable

// ── GROUP 3: GET BY ID ───────────────────────────────────
@Test void getById_existingId_returns200()
@Test void getById_unknownId_returns404()
@Test void getById_softDeletedId_returns404()

// ── GROUP 4: UPDATE ──────────────────────────────────────
@Test void update_validRequest_returns200()
@Test void update_noToken_returns401()
@Test void update_unknownId_returns404()

// ── GROUP 5: DELETE ──────────────────────────────────────
@Test void delete_existingRecord_softDeleteApplied()  // deletedAt set, row still in DB
@Test void delete_notInListAfterDelete()              // deleted record hidden from API
@Test void delete_alreadyDeleted_returns404()
@Test void delete_noToken_returns401()

// ── GROUP 6: PUBLISH / STATUS CHANGE (if applicable) ────
@Test void publish_draftRecord_returnsPublished()
@Test void publish_setsPublishedAt()

// ── GROUP 7: PUBLIC API ──────────────────────────────────
@Test void publicList_returnsOnlyPublishedRecords()
@Test void publicGetBySlug_publishedRecord_returns200()
@Test void publicGetBySlug_draftRecord_returns404()  // draft hidden from public

// ── GROUP 8: API RESPONSE STRUCTURE ─────────────────────
@Test void response_successFormat_matchesApiResponseStructure()
@Test void response_errorFormat_matchesApiResponseStructure()

// ── GROUP 9: MODULE-SPECIFIC ─────────────────────────────
// One test per acceptance criterion in specs/<module>/spec.md
// that is not covered by the groups above
```

Also write **ServiceTest** with these mandatory groups:

```java
// Every ServiceTest must contain:
@Test void create_happyPath_returnsResponseWithGeneratedSlug()
@Test void create_duplicateSlug_throwsException()
@Test void create_specialCharactersInTitle_slugIsUrlSafe()
@ParameterizedTest void create_readingTimeCalculatedCorrectly()  // if content module
@Test void delete_existingRecord_setsDeletedAt()
@Test void delete_nonExistent_throwsNotFoundException()
@Test void publish_draftRecord_setsStatusAndPublishedAt()
@Test void publish_onSuccess_invalidatesRedisCache()  // if caching enabled
// + one test per business rule from spec.md
```

Show progress as files are written:
```
✍️  WRITING BACKEND TEST CODE — Task X.Y
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✓ BlogServiceTest.java           (12 unit tests)
✓ BlogControllerTest.java        (21 integration tests)
✓ TestDataFactory.java           (updated with blog test data)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Total backend test methods: 33
```

---

#### Write Frontend Test Files (Playwright)

One `.spec.ts` file per page that was implemented in this task:

**File:** `tests/e2e/<module>/<page-name>.spec.ts`

Read `instructions/frontend-testing.md` for exact patterns.

**Every `.spec.ts` file must contain:**

```typescript
// ── IMPORTS ───────────────────────────────────────────────
import { test, expect } from '@playwright/test';

// ── CONSTANTS ─────────────────────────────────────────────
const BASE_URL = 'http://localhost:5173';
const API_URL = 'http://localhost:8080/api';

// ── TEST SUITE ────────────────────────────────────────────
test.describe('[PageName] — [Module]', () => {

   test.beforeEach(async ({ page }) => {
      // Clear state before each test
      await page.addInitScript(() => {
         localStorage.clear();
         sessionStorage.clear();
      });
   });

   // ── TC-F-01: Page load (always required) ─────────────
   test('TC-F-01: page loads and renders main content', async ({ page }) => {
      await page.goto(`${BASE_URL}/[route]`);
      await page.waitForLoadState('networkidle');
      await expect(page.locator('[data-testid="..."]')).toBeVisible();
      await page.screenshot({ path: 'screenshots/test-X.Y-F01-load.png' });
   });

   // ── TC-F-02: Auth guard (required for all admin pages) ─
   test('TC-F-02: unauthenticated user redirected to login', async ({ page }) => {
      await page.goto(`${BASE_URL}/admin/[route]`);
      await page.waitForURL('**/admin/login');
      await expect(page).toHaveURL(/admin\/login/);
      await page.screenshot({ path: 'screenshots/test-X.Y-F02-auth.png' });
   });

   // ── TC-F-03: Form submission (required if page has form) ─
   test('TC-F-03: valid form submission succeeds', async ({ page }) => {
      // ... login first if admin page ...
      await page.goto(`${BASE_URL}/[form-route]`);
      await page.fill('[name="fieldName"]', 'valid value');
      await page.click('[data-testid="submit-btn"]');
      await expect(page.locator('[data-testid="success-message"]')).toBeVisible();
      await page.screenshot({ path: 'screenshots/test-X.Y-F03-submit.png' });
   });

   // ── TC-F-04: Form validation (required if page has form) ─
   test('TC-F-04: empty form shows inline validation errors', async ({ page }) => {
      await page.goto(`${BASE_URL}/[form-route]`);
      await page.click('[data-testid="submit-btn"]');
      const errorCount = await page.locator('[role="alert"]').count();
      expect(errorCount).toBeGreaterThan(0);
      await page.screenshot({ path: 'screenshots/test-X.Y-F04-validation.png' });
   });

   // ── TC-F-05: Loading state (required for all data pages) ─
   test('TC-F-05: loading skeleton shown during fetch', async ({ page }) => {
      await page.route('**/api/**', async route => {
         await new Promise(r => setTimeout(r, 800));
         await route.continue();
      });
      await page.goto(`${BASE_URL}/[route]`);
      const skeleton = page.locator('[data-testid="skeleton"], .animate-pulse');
      await expect(skeleton.first()).toBeVisible();
      await page.screenshot({ path: 'screenshots/test-X.Y-F05-loading.png' });
   });

   // ── TC-F-06: Empty state ─────────────────────────────
   test('TC-F-06: empty state shown when no data returned', async ({ page }) => {
      await page.route('**/api/[endpoint]**', async route => {
         await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({ success: true, data: [], totalElements: 0 }),
         });
      });
      await page.goto(`${BASE_URL}/[route]`);
      await page.waitForLoadState('networkidle');
      await expect(page.locator('[data-testid="empty-state"]')).toBeVisible();
      await page.screenshot({ path: 'screenshots/test-X.Y-F06-empty.png' });
   });

   // ── TC-F-07: Error state ─────────────────────────────
   test('TC-F-07: error state shown when API returns 500', async ({ page }) => {
      await page.route('**/api/[endpoint]**', async route => {
         await route.fulfill({ status: 500, body: '{"success":false}' });
      });
      await page.goto(`${BASE_URL}/[route]`);
      await page.waitForLoadState('networkidle');
      await expect(page.locator('[data-testid="error-state"]')).toBeVisible();
      await page.screenshot({ path: 'screenshots/test-X.Y-F07-error.png' });
   });

   // ── TC-F-08: Responsive ──────────────────────────────
   test('TC-F-08: layout correct on tablet and mobile', async ({ page }) => {
      await page.goto(`${BASE_URL}/[route]`);
      await page.waitForLoadState('networkidle');

      await page.setViewportSize({ width: 768, height: 1024 });
      await page.waitForTimeout(300);
      const tabletScroll = await page.evaluate(() => document.body.scrollWidth > window.innerWidth);
      expect(tabletScroll).toBe(false);
      await page.screenshot({ path: 'screenshots/test-X.Y-F08-tablet.png' });

      await page.setViewportSize({ width: 375, height: 812 });
      await page.waitForTimeout(300);
      const mobileScroll = await page.evaluate(() => document.body.scrollWidth > window.innerWidth);
      expect(mobileScroll).toBe(false);
      await page.screenshot({ path: 'screenshots/test-X.Y-F08-mobile.png' });
   });

   // ── TC-F-NN: [Module-specific tests] ─────────────────
   // Add one test per acceptance criterion from specs/<module>/spec.md
   // that is not covered by TC-F-01 through TC-F-08

});
```

Show progress as files are written:
```
✍️  WRITING FRONTEND TEST CODE — Task X.Y
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✓ tests/e2e/blog/blog-list.spec.ts    (8 test cases)
✓ tests/e2e/blog/blog-editor.spec.ts  (10 test cases)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Total frontend test cases: 18
```

---

#### Write Mobile Test Files (Flutter — only if Flutter in stack.md)

**File:** `test/features/<module>/<module>_test.dart`

```dart
// test/features/blog/blog_list_test.dart

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mockito/mockito.dart';
import 'package:provider/provider.dart';

import 'package:app/features/blog/presentation/blog_list_page.dart';
import 'package:app/features/blog/data/blog_repository.dart';
import '../../../mocks/mock_blog_repository.dart';

void main() {
  late MockBlogRepository mockRepo;

  setUp(() {
    mockRepo = MockBlogRepository();
  });

  // Helper to build widget under test
  Widget buildWidget() {
    return MaterialApp(
      home: Provider<BlogRepository>.value(
        value: mockRepo,
        child: const BlogListPage(),
      ),
    );
  }

  // ── TC-M-01: Widget renders ───────────────────────────
  testWidgets('TC-M-01: BlogListPage renders without exception', (tester) async {
    when(mockRepo.getBlogs()).thenAnswer((_) async => []);
    await tester.pumpWidget(buildWidget());
    await tester.pump();
    expect(find.byType(BlogListPage), findsOneWidget);
  });

  // ── TC-M-02: Loading state ────────────────────────────
  testWidgets('TC-M-02: shows loading indicator during fetch', (tester) async {
    when(mockRepo.getBlogs()).thenAnswer((_) async {
      await Future.delayed(const Duration(seconds: 1));
      return [];
    });
    await tester.pumpWidget(buildWidget());
    await tester.pump(); // first frame
    expect(find.byType(CircularProgressIndicator), findsOneWidget);
  });

  // ── TC-M-03: Empty state ──────────────────────────────
  testWidgets('TC-M-03: shows empty state when list is empty', (tester) async {
    when(mockRepo.getBlogs()).thenAnswer((_) async => []);
    await tester.pumpWidget(buildWidget());
    await tester.pumpAndSettle();
    expect(find.byKey(const Key('empty-state')), findsOneWidget);
  });

  // ── TC-M-04: Error state ──────────────────────────────
  testWidgets('TC-M-04: shows error state when API throws', (tester) async {
    when(mockRepo.getBlogs()).thenThrow(Exception('Network error'));
    await tester.pumpWidget(buildWidget());
    await tester.pumpAndSettle();
    expect(find.byKey(const Key('error-state')), findsOneWidget);
  });

  // ── TC-M-05: Data renders ─────────────────────────────
  testWidgets('TC-M-05: renders list items when data loaded', (tester) async {
    when(mockRepo.getBlogs()).thenAnswer((_) async => [
      BlogModel(id: '1', title: 'AWS Guide', slug: 'aws-guide'),
      BlogModel(id: '2', title: 'Cloud Tips', slug: 'cloud-tips'),
    ]);
    await tester.pumpWidget(buildWidget());
    await tester.pumpAndSettle();
    expect(find.text('AWS Guide'), findsOneWidget);
    expect(find.text('Cloud Tips'), findsOneWidget);
  });

  // ── TC-M-NN: Module-specific ──────────────────────────
  // Add tests per acceptance criteria from specs/<module>/spec.md
}
```

Show progress as files are written:
```
✍️  WRITING MOBILE TEST CODE — Task X.Y
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✓ test/features/blog/blog_list_test.dart    (5 test cases)
✓ test/features/blog/blog_card_test.dart    (3 test cases)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Total mobile test cases: 8
```

---

#### Verify Test Code Files Exist Before Continuing

After writing test code — verify before running any test runner:

```
✅ TEST CODE FILES WRITTEN — Task X.Y
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Backend:
  src/test/java/com/<pkg>/blog/BlogServiceTest.java       ✅ (12 methods)
  src/test/java/com/<pkg>/blog/BlogControllerTest.java    ✅ (21 methods)

Frontend:
  tests/e2e/blog/blog-list.spec.ts                        ✅ (8 cases)

Mobile:
  test/features/blog/blog_list_test.dart                  ✅ (5 cases)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Test source directories are NOT empty. Proceeding to Step 8.
```

**If any test directory is still empty after this step — STOP. The test code
was not written. Do not proceed to Step 8 until all test files exist.**

### Step 7b — HARD STOP: Verify Test Files Exist

**This gate cannot be skipped. It runs automatically after Step 7.**
**If any check fails — the agent stops completely and reports to the user.**

Run these checks now and report the result:

#### Backend verification

```bash
# Count test methods in the service test file
SERVICE_TEST="src/test/java/com/<pkg>/<module>/<Module>ServiceTest.java"
CONTROLLER_TEST="src/test/java/com/<pkg>/<module>/<Module>ControllerTest.java"

# Check files exist
[ -f "$SERVICE_TEST" ]    && echo "✅ ServiceTest exists"    || echo "❌ ServiceTest MISSING"
[ -f "$CONTROLLER_TEST" ] && echo "✅ ControllerTest exists" || echo "❌ ControllerTest MISSING"

# Count @Test methods
grep -c "@Test" "$SERVICE_TEST"    2>/dev/null && echo " test methods in ServiceTest"    || echo "0 test methods"
grep -c "@Test" "$CONTROLLER_TEST" 2>/dev/null && echo " test methods in ControllerTest" || echo "0 test methods"
```

**Backend gate passes when:**
- `<Module>ServiceTest.java` exists AND has ≥ 1 `@Test` method
- `<Module>ControllerTest.java` exists AND has ≥ 1 `@Test` method

**If backend gate FAILS:**
```
🚫 HARD STOP — BACKEND TESTS NOT WRITTEN
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
❌ <Module>ServiceTest.java     → MISSING or has 0 @Test methods
❌ <Module>ControllerTest.java  → MISSING or has 0 @Test methods

The agent cannot proceed to Step 8 until test files exist.
Returning to Step 7 to write the missing test files now.
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```
→ Go back to Step 7. Write the missing files. Re-run this gate.

#### Frontend verification

```bash
SPEC_FILE="tests/e2e/<module>/<page>.spec.ts"
[ -f "$SPEC_FILE" ] && echo "✅ Playwright spec exists" || echo "❌ Playwright spec MISSING"
grep -c "test(" "$SPEC_FILE" 2>/dev/null && echo " test cases" || echo "0 test cases"
```

**Frontend gate passes when:**
- `tests/e2e/<module>/<page>.spec.ts` exists AND has ≥ 1 `test(` block

**If frontend gate FAILS:**
```
🚫 HARD STOP — FRONTEND TESTS NOT WRITTEN
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
❌ tests/e2e/<module>/<page>.spec.ts → MISSING or has 0 test() blocks

The agent cannot proceed to Step 8 until the Playwright spec exists.
Returning to Step 7 to write the missing spec file now.
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```
→ Go back to Step 7. Write the missing file. Re-run this gate.

#### Mobile verification (only if Flutter in stack.md)

```bash
DART_TEST="test/features/<module>/<page>_test.dart"
[ -f "$DART_TEST" ] && echo "✅ Dart test exists" || echo "❌ Dart test MISSING"
```

#### Gate passes — show confirmation before continuing

```
✅ STEP 7b GATE PASSED — Test files verified
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Backend:
  ✅ <Module>ServiceTest.java     ([N] @Test methods)
  ✅ <Module>ControllerTest.java  ([N] @Test methods)

Frontend:
  ✅ tests/e2e/<module>/<page>.spec.ts  ([N] test() blocks)

Mobile:
  ✅ test/features/<module>/<page>_test.dart ([N] tests)
  OR ⛔ Not in scope

Proceeding to Step 8.
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

**Only after all gates pass does execution continue to Step 8.**

---

### Step 8 — Generate Test Plan Files

**This step happens BEFORE any test is executed.**
Generate one test plan file per platform. The file is written FIRST — then
tests are executed and results are written BACK into the same file.

This is mandatory. Even if implementation failed, even if the app won't start,
even if tests all fail — the test plan file must exist with results documented.
A task with no test plan files cannot be marked complete.

#### Create Test Plan Files

Create one file per platform that is in scope for this task:

**Backend test plan:**
```
.claude/tests/Task X.Y - Backend Test Plan.md
```

**Frontend test plan:**
```
.claude/tests/Task X.Y - Frontend Test Plan.md
```

**Mobile test plan (only if Flutter in stack.md):**
```
.claude/tests/Task X.Y - Mobile Test Plan.md
```

---

#### Backend Test Plan File Format

```markdown
# Backend Test Plan — Task X.Y — [Task Name]

**Module**: [module name]
**Generated**: DD/MM/YYYY HH:MM
**Status**: ⏳ NOT EXECUTED
**Result**: —

---

## Environment

- Java 21 + Spring Boot 3.x
- PostgreSQL (Testcontainers)
- Redis (Testcontainers)
- Run command: `mvn clean verify`

---

## Test Cases

Derived from: specs/<module>/spec.md acceptance criteria

### TC-B-01: [Name — Happy Path]
**Type**: Integration (MockMvc)
**Endpoint**: POST /api/admin/[module]
**Description**: Valid request from ADMIN creates record successfully
**Expected**: 201 + DB record created with correct fields
**Result**: ⏳ | **Notes**: —

### TC-B-02: [Name — No Token]
**Type**: Integration (Security)
**Endpoint**: POST /api/admin/[module]
**Description**: Request without Authorization header is rejected
**Expected**: 401 Unauthorized
**Result**: ⏳ | **Notes**: —

### TC-B-03: [Name — Wrong Role]
**Type**: Integration (Security)
**Endpoint**: POST /api/admin/[module]
**Description**: EDITOR role calling ADMIN-only endpoint is rejected
**Expected**: 403 Forbidden
**Result**: ⏳ | **Notes**: —

### TC-B-04: [Name — Missing Required Field]
**Type**: Integration (Validation)
**Endpoint**: POST /api/admin/[module]
**Description**: Request with blank required field returns field error
**Expected**: 400 + field error in response body
**Result**: ⏳ | **Notes**: —

### TC-B-05: [Name — Not Found]
**Type**: Integration
**Endpoint**: GET /api/admin/[module]/:id
**Description**: Unknown UUID returns not found
**Expected**: 404 + RESOURCE_NOT_FOUND error code
**Result**: ⏳ | **Notes**: —

### TC-B-06: [Name — Soft Delete]
**Type**: Integration + DB Verification
**Endpoint**: DELETE /api/admin/[module]/:id
**Description**: Delete sets deletedAt, does not remove record from DB
**Expected**: 200 + deletedAt non-null in DB + 404 on subsequent GET
**Result**: ⏳ | **Notes**: —

### TC-B-07: [Name — Public API Restriction]
**Type**: Integration
**Endpoint**: GET /api/public/[module]
**Description**: Public API returns only PUBLISHED records, not drafts
**Expected**: 200 + only PUBLISHED status items in array
**Result**: ⏳ | **Notes**: —

[Add more from spec acceptance criteria]

---

## curl Smoke Tests

Run against live server (localhost:8080) after JUnit passes:

| Operation | Endpoint | Expected Status | DB Check |
|---|---|---|---|
| Create | POST /api/admin/[module] | 201 | Record exists with correct fields |
| Read | GET /api/admin/[module]/:id | 200 | Fields match API response |
| Publish | PATCH /api/admin/[module]/:id/publish | 200 | status=PUBLISHED, publishedAt set |
| Delete | DELETE /api/admin/[module]/:id | 200 | deletedAt set, record still in DB |
| After delete | GET /api/admin/[module]/:id | 404 | — |

Results written to: doc/DATABASE_AUDIT.md

---

## Execution Results

**Executed**: —
**Duration**: —
**Command output**: —

### JUnit Results

| TC | Test Name | Status | Error |
|---|---|---|---|
| TC-B-01 | Create — happy path | ⏳ | — |
| TC-B-02 | Create — no token | ⏳ | — |
| TC-B-03 | Create — wrong role | ⏳ | — |
| TC-B-04 | Create — missing field | ⏳ | — |
| TC-B-05 | Get — not found | ⏳ | — |
| TC-B-06 | Delete — soft delete | ⏳ | — |
| TC-B-07 | Public API restriction | ⏳ | — |

**Total**: 0 / 7 | **Coverage**: —%

### curl Smoke Test Results

| Operation | Status | DB Match |
|---|---|---|
| Create | ⏳ | ⏳ |
| Read | ⏳ | ⏳ |
| Publish | ⏳ | ⏳ |
| Delete | ⏳ | ⏳ |

### Build Result

```
mvn clean verify
[result will be pasted here]
```

---

## Final Status

**Status**: ⏳ NOT EXECUTED
**All Tests Pass**: —
**Coverage ≥ 80%**: —
**DB Audit Complete**: —
```

---

#### Frontend Test Plan File Format

```markdown
# Frontend Test Plan — Task X.Y — [Task Name]

**Module**: [module name]
**Generated**: DD/MM/YYYY HH:MM
**Status**: ⏳ NOT EXECUTED
**Result**: —

---

## Environment

- React 18 + TypeScript + Vite
- Playwright (Chromium)
- Dev server: npm run dev → http://localhost:5173
- Backend API: http://localhost:8080
- Test admin: admin@mfra.com / admin123

---

## Prerequisites Checklist

- [ ] Backend running on localhost:8080
- [ ] Frontend running on localhost:5173 (`npm run dev`)
- [ ] Admin user exists in DB
- [ ] Browser state clean (localStorage cleared)

---

## Test Scenarios

Derived from: specs/<module>/spec.md + designs/[N]-requirements.md

### TS-F-01: Page Load — Happy Path
**Purpose**: Verify page renders correctly with real API data
**URL**: http://localhost:5173/[path]
**Viewport**: 1280x720
**Steps**:
1. Navigate to URL
2. Wait for networkidle
3. Verify page title visible
4. Verify main content area renders
5. Verify no console errors
6. Screenshot

**Expected**:
- [ ] Page title shown
- [ ] Content loaded (not skeleton)
- [ ] No console errors
- [ ] No 4xx/5xx network calls

**Result**: ⏳ | **Screenshot**: — | **Notes**: —

---

### TS-F-02: Auth Guard — Unauthenticated Redirect
**Purpose**: Protected route redirects to login when not authenticated
**Steps**:
1. Clear localStorage
2. Navigate to protected admin route
3. Verify redirect to /admin/login

**Expected**:
- [ ] Redirected to /admin/login
- [ ] Original URL not accessible

**Result**: ⏳ | **Screenshot**: — | **Notes**: —

---

### TS-F-03: Form Submission — Valid Data
**Purpose**: Form submits successfully with valid data
**Steps**:
1. Navigate to form page
2. Fill all required fields with valid data
3. Submit the form
4. Verify success state

**Expected**:
- [ ] Form submits without validation errors
- [ ] Correct API endpoint called
- [ ] Success state shown (message or redirect)
- [ ] No console errors

**Result**: ⏳ | **Screenshot**: — | **Notes**: —

---

### TS-F-04: Form Validation — Empty Required Fields
**Purpose**: Form shows inline errors for empty required fields
**Steps**:
1. Navigate to form page
2. Click submit without filling any fields
3. Verify inline errors appear

**Expected**:
- [ ] Inline errors shown below each required field
- [ ] Errors have role="alert"
- [ ] No API call made

**Result**: ⏳ | **Screenshot**: — | **Notes**: —

---

### TS-F-05: Loading State
**Purpose**: Skeleton or spinner shown during API data fetch
**Steps**:
1. Intercept API to add 800ms delay
2. Navigate to page
3. Screenshot during loading
4. Wait for content
5. Verify skeleton removed

**Expected**:
- [ ] Loading skeleton or spinner visible during fetch
- [ ] Skeleton removed after data loads
- [ ] Content replaces skeleton

**Result**: ⏳ | **Screenshot**: — | **Notes**: —

---

### TS-F-06: Empty State
**Purpose**: Empty state shown when API returns no data
**Steps**:
1. Mock API to return empty array
2. Navigate to list page
3. Verify empty state UI

**Expected**:
- [ ] Empty state message shown (not blank page)
- [ ] data-testid="empty-state" visible

**Result**: ⏳ | **Screenshot**: — | **Notes**: —

---

### TS-F-07: Error State
**Purpose**: Error state shown when API returns 500
**Steps**:
1. Mock API to return 500
2. Navigate to page
3. Verify error state UI

**Expected**:
- [ ] Error state shown
- [ ] No raw error message shown to user
- [ ] Retry button or message present

**Result**: ⏳ | **Screenshot**: — | **Notes**: —

---

### TS-F-08: Responsive — Tablet and Mobile
**Purpose**: Layout works on all viewports without horizontal scroll
**Steps**:
1. Navigate to page
2. Set viewport to 768x1024 (tablet)
3. Verify layout, screenshot
4. Set viewport to 375x812 (mobile)
5. Verify layout, screenshot

**Expected**:
- [ ] Tablet: no horizontal scroll, content readable
- [ ] Mobile: no horizontal scroll, content readable
- [ ] Mobile: sidebar hidden or collapsed

**Result**: ⏳ | **Screenshots**: — | **Notes**: —

---

[Add page-specific scenarios from requirements]

---

## Network Inspection

| Call | Endpoint | Auth Header | Expected Status |
|---|---|---|---|
| Page load | GET /api/... | — | 200 |
| [Form submit] | POST /api/... | Bearer ✅ | 201 |

---

## Execution Results

**Playwright started**: —
**Playwright completed**: —
**Total duration**: —

### Scenario Results

| TS | Scenario | Status | Screenshot | Console Errors | Notes |
|---|---|---|---|---|---|
| TS-F-01 | Page load | ⏳ | — | — | — |
| TS-F-02 | Auth guard | ⏳ | — | — | — |
| TS-F-03 | Form submit | ⏳ | — | — | — |
| TS-F-04 | Form validation | ⏳ | — | — | — |
| TS-F-05 | Loading state | ⏳ | — | — | — |
| TS-F-06 | Empty state | ⏳ | — | — | — |
| TS-F-07 | Error state | ⏳ | — | — | — |
| TS-F-08 | Responsive | ⏳ | — | — | — |

**Total**: 0 / 8

### TypeScript Build

```
npm run build
[result pasted here]
```

### Accessibility Results

| Check | Status |
|---|---|
| All inputs have labels | ⏳ |
| Errors have role="alert" | ⏳ |
| Icon buttons have aria-label | ⏳ |

---

## Final Status

**Status**: ⏳ NOT EXECUTED
**All Scenarios Pass**: —
**Build Clean**: —
**No Console Errors**: —
**Responsive OK**: —
```

---

#### Mobile Test Plan File Format (only if Flutter in stack.md)

```markdown
# Mobile Test Plan — Task X.Y — [Task Name]

**Module**: [module name]
**Generated**: DD/MM/YYYY HH:MM
**Status**: ⏳ NOT EXECUTED
**Result**: —

---

## Environment

- Flutter 3.x
- Run command: `flutter test test/features/<module>/`
- Analyze: `flutter analyze`

---

## Test Cases

### TC-M-01: Widget renders correctly
**Type**: Widget test
**Widget**: [Module]Page
**Expected**: Renders without exception
**Result**: ⏳ | **Notes**: —

### TC-M-02: Loading state shown
**Widget**: [Module]Page
**Expected**: CircularProgressIndicator or skeleton visible during fetch
**Result**: ⏳ | **Notes**: —

### TC-M-03: Empty state shown
**Widget**: [Module]Page
**Expected**: Empty state widget visible when list is empty
**Result**: ⏳ | **Notes**: —

### TC-M-04: Error state shown
**Widget**: [Module]Page
**Expected**: Error widget visible when API throws
**Result**: ⏳ | **Notes**: —

---

## Execution Results

| TC | Test Name | Status | Error |
|---|---|---|---|
| TC-M-01 | Widget renders | ⏳ | — |
| TC-M-02 | Loading state | ⏳ | — |
| TC-M-03 | Empty state | ⏳ | — |
| TC-M-04 | Error state | ⏳ | — |

**Total**: 0 / 4
**flutter analyze**: ⏳

---

## Final Status

**Status**: ⏳ NOT EXECUTED
```

---

### Step 9 — Execute Tests and Write Results

After generating all test plan files, execute tests and write results back
into those SAME files.

**Never run tests without first generating the test plan file.**
**Never skip writing results into the file — even if all tests fail.**

#### Execute Backend Tests

```bash
mvn clean verify
```

Show progress as tests run:
```
🧪 BACKEND — Task X.Y
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
TC-B-01  Create — happy path → 201        ✅
TC-B-02  Create — no token → 401          ✅
TC-B-03  Create — wrong role → 403        ✅
TC-B-04  Create — blank title → 400       ✅
TC-B-05  Get — not found → 404            ✅
TC-B-06  Delete — soft delete             ✅
TC-B-07  Public API — draft hidden        ✅
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
7 / 7 passed ✅ | Coverage: 84% ✅
```

After JUnit passes — run curl smoke tests:
```bash
bash scripts/smoke-test.sh
```

Write all results into `.claude/tests/Task X.Y - Backend Test Plan.md`:
- Fill in each TC row with ✅ PASS or ❌ FAIL + error message
- Paste build output into Build Result section
- Update Final Status at top

#### Execute Frontend Tests — Playwright

```bash
npm run build        ← must be clean first
npm run dev          ← start dev server
npx playwright test  ← run all scenarios
```

Show progress as scenarios run:
```
🧪 FRONTEND — Task X.Y (Playwright)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
TS-F-01  Page load — happy path            ✅  screenshot: test-X.Y-F01.png
TS-F-02  Auth guard — redirect             ✅  screenshot: test-X.Y-F02.png
TS-F-03  Form submit — valid data          ✅  screenshot: test-X.Y-F03.png
TS-F-04  Form validation — empty fields    ✅  screenshot: test-X.Y-F04.png
TS-F-05  Loading state                     ✅  screenshot: test-X.Y-F05.png
TS-F-06  Empty state                       ✅  screenshot: test-X.Y-F06.png
TS-F-07  Error state                       ✅  screenshot: test-X.Y-F07.png
TS-F-08  Responsive (tablet + mobile)      ✅  screenshots: test-X.Y-F08-*.png
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
8 / 8 passed ✅ | Build: 0 errors ✅
```

Write all results into `.claude/tests/Task X.Y - Frontend Test Plan.md`:
- Fill in each TS row with ✅ PASS or ❌ FAIL + screenshot name + error
- Paste build output into TypeScript Build section
- Fill Accessibility Results
- Update Final Status at top

#### Execute Mobile Tests (if Flutter in stack.md)

```bash
flutter analyze
flutter test test/features/<module>/
```

Write results into `.claude/tests/Task X.Y - Mobile Test Plan.md`.

---

### Step 9b — HARD STOP: Verify Test Results Before Continuing

**This gate runs automatically after Step 9.**
**If any test failed — the agent cannot proceed to Step 10 or mark the task complete.**

```
Backend result:   [N] / [N] passed
Frontend result:  [N] / [N] passed
Mobile result:    [N] / [N] passed / ⛔ not in scope
```

If result is NOT 100%:

```
🚫 HARD STOP — TESTS ARE FAILING
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
❌ Backend:  [X] tests failing
   Failed:   [test method name] — [error message]

❌ Frontend: [X] scenarios failing
   Failed:   [scenario name] — [error]

The task CANNOT be marked complete until all tests pass.
Fixing failures now before proceeding.
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

→ Fix the failing tests → re-run → only continue when 100% pass.

If result IS 100%:
```
✅ STEP 9b GATE PASSED — All tests passing
   Backend:  [N]/[N] ✅  Frontend: [N]/[N] ✅
   Proceeding to Step 10.
```

### Step 10 — Handle Test Failures

**If ANY test fails on ANY platform:**

```
❌ TEST FAILURE
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Platform:  Frontend (Playwright)
Scenario:  TS-F-02 — Auth guard redirect
Expected:  Redirect to /admin/login
Actual:    Stayed at /admin/dashboard (no redirect)
Root cause: AuthGuard missing from router config
File:       src/router/AdminRouter.tsx
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

After fix:
1. Re-run build — must be clean
2. Re-run only the failed scenario first to confirm fix
3. Re-run full suite to confirm no regression
4. Update the test plan file result row: ❌ → ✅
5. Add fix note to the result row

**If the page/server won't start at all:**

```
❌ PAGE CANNOT BE TESTED
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Platform:  Frontend
Reason:    Dev server crashes on start — TypeScript errors
Error:     [paste error]
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

Write this into the test plan file Final Status:
```
**Status**: ❌ COULD NOT EXECUTE — BUILD FAILED
**Reason**: TypeScript error in [file] prevents server from starting
**All planned test cases**: BLOCKED (0/8 executed)
```

Then fix the build error and re-run all scenarios.

**Never mark a task complete with failing tests or blocked test files.**

---

### Step 11 — Update configurations.md

After task implementation, update any configs that were set up:

```
For each config set up during this task:
  Open .claude/configurations.md
  Change ❌ MISSING → ✅ READY
  Fill: config class, env vars, thread pool (if async), task ID
```

```markdown
## 3. Email Configuration
**Status**: ✅ READY
  Config class:   config/EmailConfig.java  ✅
  Thread pool:    emailTaskExecutor        ✅
  MAIL_HOST:      ✅ set
  Set up in:      Task 2.1
```

### Step 12 — Update systemTasks.md AND tasks.md — Atomically

**These two files must always be updated in the same step.**
Never update one without the other. A drift between them is a tracking failure.

**systemTasks.md** — update exactly once:
```markdown
### Task X.Y: [Task Name]
- **Status**: ✅ COMPLETED
- **Completed**: DD/MM/YYYY HH:MM
- **Platforms**: Backend ✅ | Frontend ✅ | Mobile —

Testing:
- **Backend**: 8/8 passed ✅ | Coverage: 84% ✅
- **Frontend**: 6/6 passed ✅
- **Mobile**: not in scope
- **Build**: All clean ✅
- **curl DB Audit**: All ✅ MATCH

Token Usage:
- **Implementation**: 8,400 / 5,200 tokens → $0.10
- **Testing**: 3,100 / 2,800 tokens → $0.05
- **Total**: $0.15

Docs:
- `.claude/processed/Task X.Y.md` ✅
- `.claude/processed/Task X.Y - Test Results.md` ✅
- `doc/DATABASE_AUDIT.md` updated ✅
- `.claude/configurations.md` updated ✅ (if config was set up)
- `designs/[N]-requirements.md` updated ✅ (if requirements were corrected)
```

**tasks.md** — three things to update in the same step:

1. Stage status row for this page:
```markdown
| Backend  | ✅ Done | .claude/Phases/PhaseX/Task X.Y.md | Completed DD/MM/YYYY |
| Frontend | ✅ Done | .claude/Phases/PhaseX/Task X.Z.md | Completed DD/MM/YYYY |
```

2. Sub-tasks table — mark as Done with date:
```markdown
| X.Y | Backend | [...] | ✅ Done | — | DD/MM/YYYY |
| X.Z | Frontend | [...] | ✅ Done | X.Y | DD/MM/YYYY |
```

3. **"Files Built from This Page" section — fill it completely:**
```markdown
### Files Built from This Page

**Backend** (Task [X.Y]):
  src/main/java/com/<pkg>/<module>/entity/<Module>Entity.java
  src/main/java/com/<pkg>/<module>/repository/<Module>Repository.java
  src/main/java/com/<pkg>/<module>/service/<Module>Service.java
  src/main/java/com/<pkg>/<module>/controller/<Module>AdminController.java
  src/main/java/com/<pkg>/<module>/controller/<Module>PublicController.java
  src/main/java/com/<pkg>/<module>/dto/<Module>CreateRequest.java
  src/main/java/com/<pkg>/<module>/dto/<Module>Response.java

**Frontend** (Task [X.Z]):
  src/features/<module>/api.ts
  src/features/<module>/types.ts
  src/features/<module>/pages/<Module>Page.tsx
  src/features/<module>/components/<Module>Card.tsx

**Database migrations**:
  src/main/resources/db/migration/VX__create_<module>_tables.sql

**Tests**:
  src/test/java/com/<pkg>/<module>/<Module>ControllerTest.java
  tests/e2e/<module>/<module>.spec.ts
```

This section is the scope map used by `/analyze-page [N]` when the design
is updated. Without it, the agent cannot safely re-analyze without guessing.

Also update the Summary Table at the top of tasks.md — set the correct ✅
for the Backend and/or Frontend column for this page row.

**Never finish Step 12 with only one of these two files updated.**
**Never leave "Files Built from This Page" empty after a task completes.**

---

### Step 13 — Present Final Results

```
✅ TASK X.Y COMPLETED
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Task:     X.Y — [Task Name]
Duration: 45 min
Cost:     $0.15

📁 FILES CREATED:
  Backend:  6 files (entity, repo, service, 2 controllers, migration)
  Frontend: 4 files (api, types, 2 pages, 2 components)
  Mobile:   — (not in scope)

🧪 TESTS:
  Backend:  8/8 passed ✅ | Coverage: 84% ✅
  Frontend: 6/6 passed ✅
  Mobile:   — (not in scope)

📊 DB AUDIT:
  5 curl operations verified against DB ✅ All MATCH

⚙️  CONFIGS:
  Email Configuration → marked ✅ READY

📝 DOCS:
  .claude/processed/Task X.Y.md
  .claude/processed/Task X.Y - Test Results.md

Next task: X.Z — [Next Task Name]
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Options:
  1️⃣  continue     → start next task immediately
  2️⃣  review       → show implementation details
  3️⃣  review-tests → show full test results
  4️⃣  pause        → stop here
```

---

## ❌ Quality Gates — Cannot Mark COMPLETED Unless

### Backend Gates
- [ ] `<Module>ServiceTest.java` EXISTS — HARD STOP if missing (Step 7b)
- [ ] `<Module>ControllerTest.java` EXISTS — HARD STOP if missing (Step 7b)
- [ ] Both files have `@Test` methods — HARD STOP if 0 methods (Step 7b)
- [ ] `mvn clean compile` → BUILD SUCCESS, 0 errors
- [ ] `mvn clean verify` → BUILD SUCCESS, 0 failures — HARD STOP if any fail (Step 9b)
- [ ] Coverage ≥ 80% on changed classes
- [ ] All admin endpoints have `@PreAuthorize`
- [ ] All entities extend `BaseEntity`
- [ ] DTOs used in controllers — no entity exposure
- [ ] Flyway migration created for schema changes
- [ ] Redis cache invalidated on every write
- [ ] Soft delete verified — `deletedAt` set, never hard delete
- [ ] Public API returns only published/active records
- [ ] `doc/DATABASE_AUDIT.md` updated — all curl entries `✅ MATCH`
- [ ] `.claude/tests/Task X.Y - Backend Test Plan.md` filled with results

### Frontend Gates (only if Frontend in scope)
- [ ] `tests/e2e/<module>/<page>.spec.ts` EXISTS — HARD STOP if missing (Step 7b)
- [ ] File has `test(` blocks — HARD STOP if 0 tests (Step 7b)
- [ ] `npm run build` → 0 TypeScript errors
- [ ] All Playwright scenarios pass (100%)
- [ ] All admin routes wrapped in `AuthGuard`
- [ ] No hardcoded API URLs — env vars only
- [ ] Loading, error, and empty states all handled
- [ ] All `data-testid` attributes present
- [ ] Responsive on Desktop, Tablet, Mobile ✅
- [ ] Accessibility — labels, role="alert", aria-label ✅
- [ ] `.claude/tests/Task X.Y - Frontend Test Plan.md` filled with results
- [ ] Screenshots saved for every scenario

### Mobile Gates (only if Flutter in stack.md)
- [ ] `test/features/<module>/<page>_test.dart` EXISTS with test cases
- [ ] `test/` is NOT empty — verified with `ls`
- [ ] `flutter analyze` → 0 issues
- [ ] All widget tests pass
- [ ] Loading, error, and empty states tested
- [ ] No hardcoded API URLs — constants only
- [ ] `.claude/tests/Task X.Y - Mobile Test Plan.md` filled with results

### Documentation Gates
- [ ] `.claude/processed/Task X.Y.md` created with token usage and timing
- [ ] `.claude/processed/Task X.Y - Test Results.md` created with all results
- [ ] `systemTasks.md` updated exactly once
- [ ] `tasks.md` updated in the same step as systemTasks.md — never separate
- [ ] `doc/DATABASE_AUDIT.md` updated
- [ ] `configurations.md` updated if any config was set up this task
- [ ] `designs/[N]-requirements.md` updated if any requirements were corrected during implementation

---

## 🚨 Critical Rules

| # | Rule | Wrong | Right |
|---|---|---|---|
| 1 | Ask user first | Start implementing immediately | Ask backend / frontend / mobile / all |
| 2 | Read spec before coding | Assume requirements | `cat specs/<module>/spec.md` |
| 3 | Check stack.md | Build Flutter without checking | Read stack.md — only build what's listed |
| 16 | Read DESIGN.md | Use generic Tailwind colors | Read designs/DESIGN.md before any frontend code |
| 4 | Check configurations.md | Set up Redis config again | Check if already ✅ READY |
| 5 | Tests must all pass | Mark complete at 6/8 | Fix until 8/8 |
| 6 | Build clean first | Test with compile errors | Build must be clean before tests |
| 7 | Soft delete only | `repository.delete(entity)` | `entity.setDeletedAt(Instant.now())` |
| 8 | DTOs in controllers | Return entity from controller | Map to DTO in service |
| 9 | DB audit after curl | Skip DB verification | Cross-check every write against DB |
| 10 | Update systemTasks once | Update in a loop | Update exactly once after all gates pass |
| 11 | Update configurations.md | Forget to mark config ready | Update after every config setup |
| 12 | No skipping steps | Jump to implementation | Follow all steps in order |
| 17 | Write test code before running | Run mvn verify on empty test package | Write ServiceTest.java + ControllerTest.java first |
| 18 | Write .spec.ts before Playwright | Run Playwright with no spec file | Write tests/e2e/<module>/<page>.spec.ts first |
| 19 | Write _test.dart before flutter test | Run flutter test on empty test dir | Write test/features/<module>/<page>_test.dart first |
| 20 | Test plan file before execution | Run tests without plan file | Generate .claude/tests/Task X.Y - [Platform] Test Plan.md first |
| 21 | Write results back to plan file | Leave plan file as ⏳ NOT EXECUTED | Fill every scenario row with ✅ or ❌ after execution |
| 22 | Document failure when page broken | Skip tests if build fails | Generate plan file, document failure reason, fix, re-run |
| 23 | Step 7b gate blocks Step 8 | Skip test writing silently | If gate fails — return to Step 7 and write the files |
| 24 | Step 9b gate blocks Step 10 | Mark complete with failing tests | Fix all failures before proceeding — 100% pass required |
| 25 | Zero tolerance for empty test dirs | Proceed with 0 test methods | 0 @Test methods = gate fail = return to Step 7 |
| 18 | Write results back to plan file | Leave plan file as ⏳ NOT EXECUTED | Fill every scenario row with ✅ or ❌ after execution |
| 19 | Test even when page is broken | Skip tests if build fails | Generate plan file, document failure, fix, re-run |
| 13 | tasks.md + systemTasks.md atomic | Update systemTasks only | Always update both in Step 12 |
| 14 | Read requirements before code | Skip requirements file | Read designs/[N]-requirements.md in Step 1 |
| 15 | Spec/requirements conflict → ask | Pick one silently | Surface conflict before writing any code |

---

## 🔧 Handling Failures

### Build Fails
```
1. Show exact error message and file
2. Fix the root cause
3. Run mvn clean compile / npm run build / flutter analyze
4. Only proceed to tests after clean build
```

### Test Fails
```
1. Show which scenario failed and why
2. Identify root cause
3. Fix the code
4. Re-run the failed scenario
5. Re-run full suite
6. All must pass — never skip a failing test
```

### Missing Dependency Task
```
1. Inform user: "Task X.Y is blocked — [Task Z.W] must complete first"
2. Offer to execute the dependency task
3. Wait for user decision
```

### Config Missing in configurations.md
```
1. Read the relevant instruction file (e.g., instructions/email.md)
2. Follow Step 0 in that file to set up the config
3. Mark config as ✅ READY in configurations.md
4. Continue with task implementation
```

### Spec Unclear
```
1. Quote the unclear part of the spec exactly
2. Present two or three interpretations
3. Ask user to clarify
4. Wait for answer before continuing
```

### Spec Contradicts Requirements
```
1. Quote both the spec text and the requirements text
2. Show what the design image actually shows
3. Ask user which takes precedence
4. After decision: update the losing file to match
5. Never implement without resolving the conflict
```

### Requirements Were Wrong
```
1. Identify the wrong section in designs/[N]-requirements.md
2. Show: what requirements say vs what implementation revealed
3. Correct designs/[N]-requirements.md — add fix log entry
4. Update affected AC in specs/[module]/spec.md
5. Then fix the code to match corrected requirements
6. Note in processed/Task X.Y.md under "Fix Applied"
```

---

## 🎯 Success = All of These True

1. ✅ Pre-flight check passed (/check-ready)
2. ✅ User confirmed the implementation scope (backend / frontend / mobile)
3. ✅ All dependencies are COMPLETED
4. ✅ designs/[N]-requirements.md read — no conflict with spec.md
5. ✅ Task summary presented and user confirmed
6. ✅ All builds clean — backend + frontend + mobile
   6a. ✅ Step 7b gate passed — test files exist with actual test methods
   Backend:  ServiceTest.java ≥1 @Test | ControllerTest.java ≥1 @Test
   Frontend: tests/e2e/<module>/<page>.spec.ts ≥1 test() block
7. ✅ Test code files written BEFORE test runners executed:
   Backend:  ServiceTest.java + ControllerTest.java exist with methods
   Frontend: tests/e2e/<module>/<page>.spec.ts exists with test cases
   Mobile:   test/features/<module>/<page>_test.dart exists (if in scope)
   7a. ✅ Test plan files generated BEFORE tests ran — one file per platform
8. ✅ All tests pass — backend + frontend + mobile
9. ✅ All test plan files filled with results (no ⏳ NOT EXECUTED rows remaining)
10. ✅ curl smoke tests run, all DB entries `✅ MATCH`
11. ✅ `configurations.md` updated for any configs set up
12. ✅ `.claude/processed/Task X.Y.md` implementation record created
13. ✅ `systemTasks.md` AND `tasks.md` updated atomically (same step)
14. ✅ `designs/[N]-requirements.md` updated if any requirement was corrected
15. ✅ Final results presented with options

**Only then:** Status = ✅ COMPLETED

---

**Remember: Quality over speed. Ask before building. Check stack.md before assuming. Read specs before coding. Test before marking complete. Document everything.**