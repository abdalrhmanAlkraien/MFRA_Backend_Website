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
/fix-task [id]     → fix a failed task (code or requirements)
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

### Step 7 — Generate Test Scenarios

After successful build, generate test scenarios for each applicable platform.
Read `specs/<module>/spec.md` acceptance criteria — **each criterion becomes at least one test.**

**Save test file to:** `.claude/Tests/<module>/Task X.Y.md`

---

#### Backend Test Generation

For every controller task, always generate these mandatory scenarios:

```
REQUIRED test scenarios per controller:
  Security:
    [ ] No token → 401
    [ ] Invalid token → 401
    [ ] Wrong role → 403
    [ ] Admin endpoint no auth → 401
    [ ] Public endpoint no auth → 200

  Validation:
    [ ] All required fields missing → 400 with field errors
    [ ] Individual required field missing → 400 with that field error
    [ ] Invalid format (email, uuid, date) → 400

  Happy path:
    [ ] Create → 201 + verify DB record created
    [ ] Get by ID → 200 + correct data returned
    [ ] Get list → 200 + paged result
    [ ] Update → 200 + verify DB updated
    [ ] Delete → 200 + verify deletedAt set (soft delete)

  Business rules:
    [ ] One scenario per acceptance criterion in spec.md

  Edge cases:
    [ ] Unknown ID → 404
    [ ] Duplicate slug/email/code → 409
    [ ] Deleted record not in list
    [ ] Published vs draft visibility rules

  Public vs Admin:
    [ ] Draft not accessible via public API → 404
    [ ] Published accessible via public API → 200
```

**Test file format for backend:**
```markdown
# Backend Tests: Task X.Y — [Task Name]

**Spec**: specs/<module>/spec.md
**Status**: ⏳ Not Executed

## Test Scenarios

### [S1] Create — happy path → 201
### [S2] Create — no token → 401
### [S3] Create — blank title → 400
...
```

---

#### Frontend Test Generation

For every frontend task, read `instructions/frontend-testing.md` and generate:

```
REQUIRED test scenarios per frontend task:
  Scenario 1 — Happy Path:
    [ ] Page loads without errors
    [ ] Data renders correctly
    [ ] No console errors

  Scenario 2 — Form Submission (if form):
    [ ] Valid submit → success message
    [ ] Empty submit → inline validation errors
    [ ] Backend field errors → displayed inline

  Scenario 3 — Auth Guard (if admin page):
    [ ] Unauthenticated → redirect to login
    [ ] Authenticated → page accessible

  Scenario 4 — Loading State:
    [ ] Skeleton/spinner shown during API call
    [ ] Skeleton removed after load

  Scenario 5 — Empty State:
    [ ] Empty state UI shown when no data

  Scenario 6 — Error State:
    [ ] Error UI shown when API returns 500

  Scenario 7 — Filter/Search (if applicable):
    [ ] Filter changes displayed content
    [ ] URL updates with filter params

  Accessibility:
    [ ] All inputs have labels
    [ ] Error messages have role="alert"
    [ ] Icon buttons have aria-label

  Responsive:
    [ ] Desktop (1280x720)
    [ ] Tablet (768x1024)
    [ ] Mobile (375x812)
```

**Test file format for frontend:**
```markdown
# Frontend Tests: Task X.Y — [Task Name]

**Spec**: specs/<module>/spec.md
**Status**: ⏳ Not Executed

## Scenarios

### [S1] Happy Path — page loads and data renders
### [S2] Form validation — inline errors shown
### [S3] Auth guard — redirects unauthenticated
...
```

---

#### Mobile Test Generation (Flutter)

Only generated if Flutter is listed in `project/stack.md`.

```
REQUIRED test scenarios per Flutter task:
  Widget Tests:
    [ ] Widget renders without errors
    [ ] Loading state shown (CircularProgressIndicator)
    [ ] Data renders correctly when loaded
    [ ] Empty state widget shown when no data
    [ ] Error state widget shown on API failure

  Integration Tests (if applicable):
    [ ] Form validation messages appear
    [ ] Navigation works on tap
    [ ] Pull-to-refresh reloads data

  Golden Tests (if UI-heavy):
    [ ] Widget matches golden file on first run
```

**Test file format for mobile:**
```markdown
# Mobile Tests: Task X.Y — [Task Name]

**Spec**: specs/<module>/spec.md
**Status**: ⏳ Not Executed

## Widget Tests

### [S1] BlogCard — renders title and summary
### [S2] BlogListPage — shows loading indicator
### [S3] BlogListPage — shows empty state
...
```

---

### Step 8 — Execute Tests

Execute tests for each applicable platform and show results as they run:

#### Execute Backend Tests

```bash
mvn clean verify
```

Show live progress:
```
🧪 BACKEND TESTS — Task X.Y
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[1/8] Create blog — happy path → 201 ✅
[2/8] Create blog — no token → 401 ✅ 🔒
[3/8] Create blog — blank title → 400 ✅
[4/8] Get by ID — exists → 200 ✅
[5/8] Get by ID — unknown ID → 404 ✅
[6/8] Publish → status PUBLISHED ✅
[7/8] Delete → deletedAt set in DB ✅
[8/8] Public API — draft not visible → 404 ✅
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Backend: 8/8 passed ✅ | Coverage: 84% ✅
```

After JUnit passes — run curl smoke tests:
```bash
# Backend must be running: mvn spring-boot:run
# Run smoke tests against localhost:8080
bash scripts/smoke-test.sh
```

Show curl results:
```
🔍 CURL SMOKE TESTS — Task X.Y
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
POST /api/admin/blogs    → 201 ✅ | DB: ✅ MATCH
GET  /api/admin/blogs    → 200 ✅ | DB: ✅ MATCH
PATCH .../publish        → 200 ✅ | DB: ✅ MATCH
DELETE /api/admin/blogs  → 200 ✅ | DB: ✅ MATCH (deletedAt set)
GET  /api/public/blogs   → 200 ✅ | published only ✅
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
All curl operations verified against DB ✅
Written to: doc/DATABASE_AUDIT.md
```

#### Execute Frontend Tests

```bash
# Frontend must be running: npm run dev
# Backend must be running
npx playwright test --headed
```

Show live progress:
```
🧪 FRONTEND TESTS — Task X.Y
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[S1] Happy path — blog list loads ✅
     ↳ 6 blog cards rendered ✅
     ↳ Filter bar visible ✅
     ↳ No console errors ✅

[S2] Form validation — inline errors ✅
     ↳ Empty title shows error ✅
     ↳ Empty content shows error ✅
     ↳ role="alert" on errors ✅

[S3] Auth guard — redirect ✅
     ↳ /admin/blogs → redirects to /admin/login ✅

[S4] Loading state ✅
     ↳ Skeleton shown during fetch ✅
     ↳ Skeleton removed after load ✅

[S5] Empty state ✅
     ↳ "No articles found" shown ✅

[S6] Responsive ✅
     ↳ Desktop 1280x720 ✅
     ↳ Tablet 768x1024 ✅
     ↳ Mobile 375x812 ✅
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Frontend: 6/6 passed ✅ | Screenshots: saved ✅
```

#### Execute Mobile Tests (Flutter)

Only if Flutter is in `project/stack.md`.

```bash
flutter test test/features/blog/
```

Show live progress:
```
🧪 MOBILE TESTS — Task X.Y
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[S1] BlogCard renders title and summary ✅
[S2] BlogListPage shows loading indicator ✅
[S3] BlogListPage shows empty state ✅
[S4] BlogListPage shows error state ✅
[S5] BlogCard tap navigates to detail ✅
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Mobile: 5/5 passed ✅
```

---

### Step 9 — Handle Test Failures

**If ANY test fails on ANY platform — fix before proceeding:**

```
❌ TEST FAILURE
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Platform:  Backend / Frontend / Mobile
Scenario:  [S3] Auth guard — redirect
Expected:  Redirect to /admin/login
Actual:    Stayed at /admin/blogs (no redirect)
Root cause: AuthGuard component missing from router config
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Fixing now...
```

After fix:
1. Re-run build — must be clean
2. Re-run only the failed scenario first
3. Then run the full suite again
4. All must pass before marking complete

**Never mark a task complete with failing tests.**

---

### Step 10 — Update configurations.md

After task implementation, check if any new configuration was set up:

```
For each config that was set up during this task:
  Open .claude/configurations.md
  Find the relevant section
  Change status from ❌ MISSING to ✅ READY
  Fill in:
    - Config class created
    - Environment variables set
    - Thread pool name (if async)
    - Set up in task: [X.Y]
```

Example update:
```markdown
## 3. Email Configuration

**Status**: ✅ READY

  Config class:      config/EmailConfig.java  ✅
  Thread pool:       emailTaskExecutor        ✅
  MAIL_HOST:         ✅ set
  MAIL_USERNAME:     ✅ set
  Mailhog (dev):     ✅ docker-compose.yml
  Set up in task:    2.1
```

---

### Step 11 — Create Documentation

Create two documentation files per task:

#### File 1 — Implementation Record

**Path:** `.claude/processed/Task X.Y.md`

```markdown
# Task X.Y — [Task Name]

**Status**: ✅ COMPLETED
**Date**: YYYY-MM-DD HH:MM
**Module**: [module name]
**Platforms**: Backend ✅ | Frontend ✅ | Mobile —

---

## Objective

[One sentence from spec]

---

## Files Created

### Backend
| File | Purpose |
|---|---|
| BlogEntity.java | JPA entity with soft delete |
| BlogService.java | Business logic, slug generation |
| BlogAdminController.java | Admin CRUD endpoints |
| BlogPublicController.java | Public read endpoints |
| VX__create_blog_tables.sql | Flyway migration |

### Frontend
| File | Purpose |
|---|---|
| features/blog/api.ts | RTK Query API slice |
| features/blog/pages/BlogListPage.tsx | List page with filter |

### Mobile
(not in scope for this task)

---

## Key Decisions

- Slug auto-generated from title in service layer
- Reading time calculated from word count (200 wpm)
- Draft blogs hidden from public API
- Soft delete applied — deletedAt set, never hard delete

---

## Configs Set Up This Task

- Email Configuration → marked ✅ READY in configurations.md

---

## Acceptance Criteria Coverage

| Criterion | Status |
|---|---|
| Admin can create blog with title, summary, content | ✅ |
| Slug auto-generated from title | ✅ |
| Draft not visible via public API | ✅ |
| Publish changes status to PUBLISHED | ✅ |

---

## Token Usage

| Phase | Input Tokens | Output Tokens | Cost |
|---|---|---|---|
| Implementation | 8,400 | 5,200 | $0.10 |
| Testing | 3,100 | 2,800 | $0.05 |
| **Total** | **11,500** | **8,000** | **$0.15** |

---

## Execution Time

| Phase | Duration |
|---|---|
| Reading files | 2 min |
| Implementation | 22 min |
| Build verification | 3 min |
| Test generation | 5 min |
| Test execution | 8 min |
| Documentation | 5 min |
| **Total** | **45 min** |
```

#### File 2 — Test Results

**Path:** `.claude/processed/Task X.Y - Test Results.md`

```markdown
# Test Results — Task X.Y — [Task Name]

**Date**: YYYY-MM-DD HH:MM
**Overall**: ✅ ALL PASSED

---

## Backend Results

**Total**: 8/8 passed ✅
**Coverage**: 84% ✅
**Build**: mvn clean verify → BUILD SUCCESS

| # | Scenario | Result | Notes |
|---|---|---|---|
| S1 | Create blog — happy path → 201 | ✅ | DB record verified |
| S2 | Create — no token → 401 | ✅ | Security confirmed |
| S3 | Create — blank title → 400 | ✅ | Field error returned |
| S4 | Get by ID — exists → 200 | ✅ | — |
| S5 | Get by ID — unknown → 404 | ✅ | — |
| S6 | Publish → PUBLISHED status | ✅ | publishedAt set |
| S7 | Delete → soft delete | ✅ | deletedAt set in DB |
| S8 | Public API — draft hidden | ✅ | — |

### curl Smoke Test Results
| Operation | Status | DB Audit |
|---|---|---|
| POST /api/admin/blogs | 201 ✅ | ✅ MATCH |
| GET /api/admin/blogs | 200 ✅ | ✅ MATCH |
| PATCH .../publish | 200 ✅ | ✅ MATCH |
| DELETE .../blogs/id | 200 ✅ | ✅ MATCH |
| GET /api/public/blogs | 200 ✅ | ✅ MATCH |

---

## Frontend Results

**Total**: 6/6 passed ✅
**Build**: npm run build → 0 errors ✅

| # | Scenario | Result | Screenshot |
|---|---|---|---|
| S1 | Blog list loads | ✅ | test-2.1-s1-list.png |
| S2 | Form validation | ✅ | test-2.1-s2-validation.png |
| S3 | Auth guard redirect | ✅ | test-2.1-s3-redirect.png |
| S4 | Loading state | ✅ | test-2.1-s4-loading.png |
| S5 | Empty state | ✅ | test-2.1-s5-empty.png |
| S6 | Responsive | ✅ | test-2.1-s6-responsive.png |

### Accessibility
- All inputs labelled: ✅
- Error role="alert": ✅
- Icon buttons aria-label: ✅

### Responsive
- Desktop 1280x720: ✅
- Tablet 768x1024: ✅
- Mobile 375x812: ✅

---

## Mobile Results

**Status**: Not in scope — Flutter not in stack.md for this task

---

## Issues Found & Fixed

| Issue | Severity | Fix | Time |
|---|---|---|---|
| publishedAt not set on publish | Medium | Added Instant.now() in service | 5 min |
```

---

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
- [ ] `mvn clean compile` → BUILD SUCCESS, 0 errors
- [ ] `mvn clean verify` → BUILD SUCCESS, 0 failures
- [ ] Coverage ≥ 80% on changed classes
- [ ] All admin endpoints have `@PreAuthorize`
- [ ] All entities extend `BaseEntity`
- [ ] DTOs used in controllers — no entity exposure
- [ ] Flyway migration created for schema changes
- [ ] Redis cache invalidated on every write
- [ ] Soft delete verified — `deletedAt` set, never hard delete
- [ ] Public API returns only published/active records
- [ ] `doc/DATABASE_AUDIT.md` updated — all curl entries `✅ MATCH`

### Frontend Gates (only if Frontend in scope)
- [ ] `npm run build` → 0 TypeScript errors
- [ ] All scenarios pass (100%)
- [ ] All admin routes wrapped in `AuthGuard`
- [ ] No hardcoded API URLs — env vars only
- [ ] Loading, error, and empty states all handled
- [ ] All `data-testid` attributes present
- [ ] Responsive on Desktop, Tablet, Mobile ✅
- [ ] Accessibility — labels, role="alert", aria-label ✅

### Mobile Gates (only if Flutter in stack.md)
- [ ] `flutter analyze` → 0 issues
- [ ] All widget tests pass
- [ ] Loading and error states handled
- [ ] No hardcoded API URLs — constants only

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
| 12 | No skipping steps | Jump to implementation | Follow all 13 steps in order |
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
7. ✅ All tests pass — backend + frontend + mobile
8. ✅ curl smoke tests run, all DB entries `✅ MATCH`
9. ✅ `configurations.md` updated for any configs set up
10. ✅ Both documentation files created
11. ✅ `systemTasks.md` AND `tasks.md` updated atomically (same step)
12. ✅ `designs/[N]-requirements.md` updated if any requirement was corrected
13. ✅ Final results presented with options

**Only then:** Status = ✅ COMPLETED

---

**Remember: Quality over speed. Ask before building. Check stack.md before assuming. Read specs before coding. Test before marking complete. Document everything.**