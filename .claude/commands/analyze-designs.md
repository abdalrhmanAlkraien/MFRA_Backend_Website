# /analyze-designs

> **Slash command for OpenClaw / Claude Code**
> Triggered when user types: `/analyze-designs`
> Analyzes ALL design images in `designs/` and generates the full project
> phase plan, requirements, tasks, and config pre-population.

---

## What This Command Does

Runs the full BA & PM workflow across every design file in `designs/`.
For each image + .md pair it extracts requirements, defines the DB schema,
maps API endpoints, and generates tasks per platform.

After all pages are analyzed it groups tasks into phases, writes them to
`systemTasks.md`, updates `CLAUDE.md`, pre-populates `configurations.md`
with detected needs, and presents a full project plan.

**Run this once before any `/execute-task` or `/continue-tasks`.**

---

## Pre-Conditions — Check Before Starting

```
Read: .claude/commands/check-ready.md
Run the pre-flight check first. If any blocking condition exists, stop
and report to user before analyzing anything.

Blocking conditions:
  → project/stack.md is empty or missing
  → project/users.md is empty or missing
  → designs/ folder has no .png files
  → Any .png file has no matching .md file
```

If all checks pass — proceed.

---

## Execution Steps

### Step 1 — Read All Context Files

```
Read: .claude/CLAUDE.md                → project name, budget, module list
Read: .claude/project/stack.md         → platforms + providers
Read: .claude/project/users.md         → access rules + permissions
Read: .claude/project/pages.md         → page reference list (if it exists)
Read: .claude/instructions/pages.md    → BA & PM workflow
Read: .claude/instructions/database.md → DB conventions
```

---

### Step 2 — Scan the Designs Folder

```
Scan: designs/

For each file found:
  Is it an image? (.png / .jpg / .jpeg / .webp)
    → Add to analysis queue
  Does it have a matching .md file? (same name, .md extension)
    → Pair confirmed
    → No .md found → report missing, ask user: skip or describe in text

Build the queue:
  Queue: [ {image: 1.png, spec: 1.md}, {image: 2.png, spec: 2.md}, ... ]
```

Report the queue before starting:

```
📋 DESIGNS FOUND — [N] pages to analyze
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  ✅  1.png + 1.md    → ready
  ✅  2.png + 2.md    → ready
  ⚠️   3.png          → missing 3.md — will skip unless you add it
  ✅  4.png + 4.md    → ready
  ...

Platforms to generate tasks for (from stack.md):
  Backend:   ✅ Spring Boot
  Frontend:  ✅ React
  Mobile:    ⛔ not in stack

Starting analysis in 3 seconds...
(type "stop" to cancel)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

---

### Step 3 — Analyze Each Page

For each confirmed pair in the queue, follow every step in
`instructions/pages.md` Steps 1–10:

```
For each {image, spec} pair:

  a. Read [N].md (context — business, access, permissions, notes)
  b. Open [N].png (visual analysis)
  c. Extract all UI sections, elements, and states
  d. Map every visual element to a DB column
  e. Write Flyway SQL for every new table
  f. Define API endpoints for every user action
  g. Extract business rules (validation, visibility, triggers)
  h. Ask clarifying questions if spec is ambiguous
     → Wait for answer before continuing this page
  i. Write requirements document → designs/[N]-requirements.md
  j. Generate tasks per platform (check stack.md first)

Show progress between pages:
  ✅ Page 1 analyzed — 3 tables, 8 endpoints, 4 tasks generated
  ✅ Page 2 analyzed — 1 table, 4 endpoints, 2 tasks generated
  🔄 Page 3 analyzing...
```

---

### Step 4 — Detect Required Configurations

After all pages are analyzed, scan all generated requirements for signals:

```
Scan every designs/[N]-requirements.md for:

Signal found                    → Mark in configurations.md
────────────────────────────────────────────────────────
Email send / confirmation       → Email Configuration: needed by [page list]
Redis / cache / rate limit      → Cache Configuration: needed by [page list]
File upload / S3 / image        → File Storage Configuration: needed by [page list]
@Async / job / export           → Async Configuration: needed by [page list]
OTP / SMS / verify phone        → SMS Configuration: needed by [page list]
Push notification / FCM         → Notification Configuration: needed by [page list]
WhatsApp / wa.me                → WhatsApp Configuration: needed by [page list]
WebSocket / real-time           → WebSocket Configuration: needed by [page list]
```

Update `configurations.md` — add "Needed by" line to each detected section:

```markdown
## 3. Email Configuration
**Status**: ❌ MISSING
**Needed by**: Page 7 (Free Consultation), Page 8 (Contact)
```

This gives you a complete config checklist before a single line of code is written.

---

### Step 5 — Group Tasks Into Phases

Group all generated tasks into phases using the PM rules from `instructions/pages.md`:

```
Phase grouping rules:
  Phase 1 — Always Foundation
    → Project scaffold, Docker Compose, DB setup
    → Security config, JWT auth, CORS
    → BaseEntity, ApiResponse wrapper, GlobalExceptionHandler

  Phase 2 to N — One Phase Per Module
    → Backend task before Frontend task in same module
    → Modules ordered by dependency:
        - Auth module before any module needing JWT
        - Upload module before any module needing file fields
        - Settings module before any module reading settings
        - Core data modules (blog, case-studies) before dependent modules

  Final Phase — Integration + Polish
    → Navigation wiring (all routes in router)
    → Regression tests
    → SEO, performance, accessibility review
```

---

### Step 6 — Write systemTasks.md

Write all generated tasks to `.claude/systemTasks.md`:

```markdown
# System Tasks

**Project**: [name from CLAUDE.md]
**Total tasks**: [N]
**Generated**: DD/MM/YYYY at HH:MM
**Last updated**: DD/MM/YYYY at HH:MM

---

## Phase 1 — Foundation

### Task 1.1: Project Scaffold + Auth
- **Status**: ⏳ Pending
- **Platform**: Backend
- **Dependencies**: none
- **Spec**: specs/auth/spec.md
- **Complexity**: Medium
- **Est. cost**: $0.60

### Task 1.2: Docker Compose + Local Dev Setup
...

## Phase 2 — [Module Name]

### Task 2.1: [Module] — Backend API
...
```

---

### Step 7 — Update tasks.md

Update `.claude/tasks.md` with one entry per analyzed page:

```markdown
## Page [N] — [Page Name]

**Design**:       designs/[N].png
**Spec**:         designs/[N].md
**Requirements**: designs/[N]-requirements.md
**Created**:      DD/MM/YYYY at HH:MM
**Last update**:  DD/MM/YYYY at HH:MM

### Stage Status

| Stage | Status | File | Notes |
|-------|--------|------|-------|
| Design | ✅ Done | designs/[N].png | |
| Requirements | ✅ Done | designs/[N]-requirements.md | |
| Backend | ⏳ Pending | — | Task [X.Y] |
| Frontend | ⏳ Pending | — | Task [X.Z] |
| Mobile | ⛔ Not in scope | — | |

### Sub-tasks

| ID | Platform | Description | Status | Depends On |
|----|----------|-------------|--------|------------|
| X.Y | Backend | [entity + service + controller] | ⏳ Pending | — |
| X.Z | Frontend | [pages + components] | ⏳ Pending | X.Y |
```

---

### Step 8 — Update CLAUDE.md

Write phases to the `Project Phases` section of `CLAUDE.md`:

```markdown
## Project Phases

| Phase | Scope | Platforms | Tasks | Priority |
|---|---|---|---|---|
| Phase 1 | Foundation — scaffold, auth, DB | Backend | 3 | Must |
| Phase 2 | [Module] | Backend + Frontend | 4 | Must |
| Phase 3 | [Module] | Backend + Frontend | 3 | Must |
| ...

**Total tasks**: [N]
**Total estimated cost**: $[N] of $[budget] budget
```

Update `Current Status` section:

```markdown
**Project Phase**: ⏳ Ready to execute

| Metric | Value |
|---|---|
| Designs added | [N] |
| Designs analyzed | [N] / [N] |
| Tasks generated | [N] |
| Tasks completed | 0 |
| Cost spent | $0.00 / $[budget] |
```

---

### Step 9 — Detect and Flag Config Setup Order

Based on the dependency chain in `instructions/database.md` and detected configs,
present the recommended setup order:

```
⚙️  CONFIGURATION SETUP ORDER
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
These configs are needed by this project (detected from requirements):

  Phase 1:  Database          → needed by all modules
  Phase 1:  Security / JWT    → needed by all admin modules
  Phase 1:  Async             → needed by Email (Task X.Y)
  Phase 2:  Cache (Redis)     → needed by public lists (Task X.Y)
  Phase 2:  Email             → needed by consultation form (Task X.Y)
  Phase 3:  File Storage (S3) → needed by blog images (Task X.Y)

These configs are NOT needed (not in stack.md or not detected):
  ⛔ SMS            → not detected
  ⛔ Push (FCM)     → not detected
  ⛔ WebSocket      → not detected
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

---

### Step 10 — Present Full Analysis Summary

```
✅ DESIGN ANALYSIS COMPLETE
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Pages analyzed:    [N]
Pages skipped:     [N] (missing .md files)
Requirements docs: [N] files → designs/[N]-requirements.md

Database:
  New tables:      [N] tables defined
  Migrations:      [N] Flyway files to create

Tasks generated:
  Backend:         [N] tasks
  Frontend:        [N] tasks
  Mobile:          [N] tasks  (or ⛔ not in scope)
  Total:           [N] tasks across [N] phases

Configs needed:    [N] → see configurations.md
Open questions:    [N] clarifications pending (see below)

Files updated:
  ✅ designs/[N]-requirements.md  (one per page)
  ✅ .claude/systemTasks.md
  ✅ .claude/tasks.md
  ✅ .claude/configurations.md   (needed-by pre-populated)
  ✅ .claude/CLAUDE.md           (phases + status)

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

[If any open questions remain:]
❓ OPEN QUESTIONS — answer before executing tasks:
  1. Page 3: "Does the filter bar update the URL?" (A/B/C)
  2. Page 7: "Is preferred meeting time a date picker or free text?" (A/B)

[If all clear:]
Project is ready to execute.

Options:
  1️⃣  /generate-spec    → generate SpecKit specs for all modules
  2️⃣  /execute-task     → start the first task
  3️⃣  /continue-tasks   → run all tasks automatically
  4️⃣  /review-progress  → see full project plan
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

---

## Clarification Handling

If a clarifying question is raised during page analysis:

```
❓ CLARIFICATION NEEDED — Page [N]: [Page Name]
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[Quote the exact ambiguity]

Options:
  A) [interpretation + implication]
  B) [interpretation + implication]

Type A or B to continue analysis.
(type "skip" to flag and continue — will require resolution before task executes)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

Skipped clarifications are collected and shown at the end of Step 10.
Any task depending on an unresolved question is flagged `⚠️ NEEDS CLARIFICATION`
in `systemTasks.md` and cannot be executed until resolved.

---

## Quick Reference

```
User types:      /analyze-designs
Agent reads:     instructions/pages.md + database.md + stack.md + users.md
Agent scans:     designs/ → all .png + .md pairs
Agent runs:      Full BA workflow per page
Agent writes:    designs/[N]-requirements.md (per page)
                 .claude/systemTasks.md (all tasks)
                 .claude/tasks.md (stage tracking)
                 .claude/configurations.md (needed-by)
                 .claude/CLAUDE.md (phases + status)
Agent presents:  Summary + open questions + next options
Next step:       /generate-spec or /execute-task
```