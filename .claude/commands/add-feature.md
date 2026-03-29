# /add-feature

> **Slash command for OpenClaw / Claude Code**
> Triggered when user types: `/add-feature "description"` or `/add-feature`
> Adds a new feature, page, module, or change to an already-running project
> without repeating the full project setup.

---

## What This Command Does

Takes a user's description of a new feature or change, analyzes the impact
across all tracked files, generates new tasks, updates the relevant project
files, and queues the new work into `systemTasks.md` ready to execute.

Use this instead of manually editing specs or task files.

---

## Execution Instructions

### 1. Read Context First

```
Read: .claude/CLAUDE.md                   → project context and modules
Read: .claude/project/stack.md            → available platforms
Read: .claude/project/pages.md            → existing pages
Read: .claude/project/users.md            → existing user types
Read: .claude/configurations.md           → what configs are ready
Read: .claude/systemTasks.md              → existing tasks and phases
Read: specs/                              → existing modules and specs
```

### 2. Ask if No Description Was Given

If user typed `/add-feature` without a description:

```
➕ ADD FEATURE
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Describe the feature or change you want to add:

Examples:
  "Add a Partners page between About and Services"
  "Add a newsletter subscription to the blog page"
  "Add EDITOR role that can manage blogs but not settings"
  "Add export to CSV for consultation requests"
  "Add WhatsApp button to the contact page"
  "Add a budget field to the consultation form"

Type your feature description:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

### 3. Analyze the Feature

After receiving the description, analyze the impact:

```
🔍 ANALYZING: "[feature description]"
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Identifying what needs to change...

  New DB table needed?           [Yes / No]
  Existing table modified?       [Yes / No — which table]
  New API endpoint needed?       [Yes / No — which endpoints]
  Existing API modified?         [Yes / No — which endpoints]
  New page needed?               [Yes / No]
  Existing page modified?        [Yes / No — which page]
  New component needed?          [Yes / No]
  New user permission needed?    [Yes / No]
  New config needed?             [Yes / No — which config]
  New Flutter screen needed?     [Yes / No — if Flutter in stack]
  Affects existing tests?        [Yes / No — which tests]

Reading affected specs...
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

### 4. Present Impact Summary

Show exactly what will change before touching any file:

```
📋 IMPACT ANALYSIS — "[feature description]"
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

IMPACT ON PROJECT FILES:

  project/pages.md
    → Add: Partners page at /partners
    → Access: PUBLIC
    → Design: designs/public/partners.png (you need to add this)

  project/users.md
    → No changes needed

  project/stack.md
    → No changes needed

IMPACT ON SPECS:

  specs/partners/                          ← new module
    → spec.md     (will be generated)
    → plan.md     (will be generated)
    → tasks.md    (will be generated)

IMPACT ON BACKEND:

  New Flyway migration:
    → VX__create_partners_table.sql

  New files:
    → PartnerEntity.java
    → PartnerRepository.java
    → PartnerService.java
    → PartnerAdminController.java
    → PartnerPublicController.java

  New API endpoints:
    → GET  /api/public/partners
    → POST /api/admin/partners
    → PUT  /api/admin/partners/{id}
    → DELETE /api/admin/partners/{id}

IMPACT ON FRONTEND:

  New files:
    → features/partners/api.ts
    → features/partners/pages/PartnersPage.tsx
    → features/partners/components/PartnerCard.tsx

  Modified files:
    → app/router.tsx — add /partners route
    → components/layout/PublicLayout.tsx — add nav link

IMPACT ON MOBILE:  (Flutter in stack.md)

  New files:
    → lib/features/partners/data/partners_repository.dart
    → lib/features/partners/presentation/partners_page.dart

CONFIGURATIONS NEEDED:
  → No new configs required

NEW TASKS GENERATED:
  Phase [N+1] — Partners Module
    Task [N+1].1  Backend: Partners CRUD + Public API
    Task [N+1].2  Frontend: Partners page + admin panel
    Task [N+1].3  Mobile: Partners screen   ← if Flutter in stack

AFFECTED EXISTING TASKS:
  → Task 2.1 (Nav links) — needs regression test
  → Task 7.1 (Admin panel) — Partners section added

ESTIMATED COST: ~$0.35 (3 new tasks)

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Proceed with adding this feature? (yes/no/modify)
```

Wait for user response:
- **yes** → apply all changes
- **no** → cancel, no files changed
- **modify** → user explains what to adjust, then show updated impact

### 5. Ask for Missing Assets

Before generating specs, check if any required inputs are missing:

```
📌 BEFORE I GENERATE SPECS
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Some assets are needed before running SpecKit:

  [ ] Design file: designs/public/partners.png
      → Please add a screenshot or mockup of the Partners page

  [ ] No other assets needed

You can:
  A) Add the design file now and type "continue"
  B) Skip the design file and type "skip design"
     (SpecKit will generate spec without visual reference)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

### 6. Update Project Files

After user confirms — update these files:

#### Update `project/pages.md`
```markdown
### Partners
- URL: /partners
- ACCESS: PUBLIC
- DESIGN: designs/public/partners.png
- BACKEND APIS:
  - GET /api/public/partners
- NOTES: Logo grid of AWS and technology partners
```

#### Update `project/users.md`
Only if new permissions are needed:
```markdown
## USER_TYPE: ADMIN
- Permissions:
  + partners: CREATE, READ, UPDATE, DELETE   ← added
```

#### Update `project/stack.md`
Only if new provider or dependency is needed.

### 7. Generate SpecKit Spec

After updating project files, trigger SpecKit for the new module:

```
📝 GENERATING SPEC VIA SPECKIT
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Running:
  /speckit.specify  → describe Partners module
  /speckit.plan     → generate implementation plan
  /speckit.tasks    → generate task breakdown

Spec generated: specs/partners/spec.md ✅
Plan generated: specs/partners/plan.md ✅
Tasks generated: specs/partners/tasks.md ✅
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

### 8. Add New Tasks to systemTasks.md

Append the new tasks to `systemTasks.md`:

```markdown
## Phase 8 — Partners Module  ← new phase

### Task 8.1: Partners Backend — CRUD + Public API
- **Status**: ⏳ PENDING
- **Dependencies**: Task 1.1 ✅, Task 1.3 ✅
- **Spec**: specs/partners/spec.md
- **Platforms**: Backend

### Task 8.2: Partners Frontend — Page + Admin Panel
- **Status**: ⏳ PENDING
- **Dependencies**: Task 8.1 ⏳
- **Spec**: specs/partners/spec.md
- **Platforms**: Frontend

### Task 8.3: Partners Mobile — Screen
- **Status**: ⏳ PENDING
- **Dependencies**: Task 8.1 ⏳
- **Spec**: specs/partners/spec.md
- **Platforms**: Mobile
```

### 9. Flag Affected Existing Tests

If existing tasks need regression testing, add a note:

```
⚠️  REGRESSION NOTE
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
These existing tasks may be affected by this change:

  Task 2.1 — Nav links updated
    → Frontend regression test recommended
    → Run: /fix-task 2.1 to re-run tests

  Task 7.1 — Admin panel updated
    → Backend: no impact
    → Frontend: re-run Playwright nav tests
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

### 10. Present Final Summary

```
✅ FEATURE ADDED: "[feature description]"
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Files updated:
  ✅ project/pages.md          → Partners page added
  ✅ specs/partners/spec.md    → generated by SpecKit
  ✅ specs/partners/plan.md    → generated by SpecKit
  ✅ specs/partners/tasks.md   → generated by SpecKit
  ✅ systemTasks.md            → 3 new tasks added (8.1, 8.2, 8.3)

New tasks queued:
  ⏳ Task 8.1 — Partners Backend
  ⏳ Task 8.2 — Partners Frontend
  ⏳ Task 8.3 — Partners Mobile

Updated project task count:
  Before: 20 tasks (10 done, 10 pending)
  After:  23 tasks (10 done, 13 pending)

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Options:
  1️⃣  execute-task      → start Task 8.1 now
  2️⃣  continue-tasks    → run all pending tasks including new ones
  3️⃣  review-progress   → see full updated project status
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

---

## Common Feature Types — What Changes

| Feature Type | pages.md | users.md | New Spec | New Tasks |
|---|---|---|---|---|
| New page | ✅ Add page | Maybe | ✅ Yes | Backend + Frontend + Mobile |
| New field on form | ❌ | ❌ | Update existing | 1 task per platform |
| New user role/permission | ❌ | ✅ Add role | Update existing | Backend security task |
| New API endpoint | ❌ | Maybe | Update existing | Backend task |
| New admin section | ❌ | Maybe | ✅ Yes | Backend + Frontend |
| New provider (SMS, WA) | ❌ | ❌ | Update existing | Backend config task |
| Change navigation | ❌ | ❌ | Update existing | Frontend task |
| New mobile screen | ❌ | ❌ | ✅ Yes | Mobile only |

---

## Blocking Conditions

```
Condition                                 Action
──────────────────────────────────────────────────────────────
Provider not in stack.md                  Warn + ask to add to stack.md first
Feature conflicts with existing spec      Show conflict, ask for resolution
New page requires design not provided     Ask user to add design or skip
Feature requires new external service     Add to stack.md, update configurations.md
```

---

## Quick Reference

```
User types:        /add-feature "description"
Agent reads:       CLAUDE.md + stack.md + pages.md + users.md
                   + configurations.md + systemTasks.md + specs/
Agent analyzes:    Impact across all project files
Agent shows:       Full impact summary — waits for yes/no
Agent checks:      Missing assets (design images)
Agent updates:     project/pages.md + users.md (if needed)
Agent generates:   New spec via SpecKit
Agent adds:        New tasks to systemTasks.md
Agent flags:       Affected existing tasks needing regression
Agent presents:    Summary + next options
Agent modifies:    Only after user confirms — never silently
```