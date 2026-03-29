# /check-ready

> **Slash command for OpenClaw / Claude Code**
> Triggered when user types: `/check-ready`
> Validates the project is fully set up before any task is executed.
> Automatically called at the start of `/analyze-designs`, `/execute-task`,
> and `/continue-tasks` — but can also be run manually at any time.

---

## What This Command Does

Reads every required project file and validates it is present, non-empty,
and correctly filled. Reports a checklist with pass/fail/warn per item.

This command never modifies any file — it is read-only.

If called automatically before another command, it runs silently and only
interrupts if a **blocking condition** is found.
If called directly by the user, it always shows the full report.

---

## Checks — In Order

### Layer 1 — Core Files Exist

```
Check every file that must exist before anything works:

  ✅ / ❌  .claude/CLAUDE.md
  ✅ / ❌  .claude/project/stack.md
  ✅ / ❌  .claude/project/users.md
  ✅ / ❌  .claude/project/pages.md
  ✅ / ❌  .claude/configurations.md
  ✅ / ❌  .claude/tasks.md
  ✅ / ❌  .claude/systemTasks.md
  ✅ / ❌  .claude/instructions/pages.md
  ✅ / ❌  .claude/instructions/database.md
  ✅ / ❌  .claude/instructions/backend.md
  ✅ / ❌  .claude/instructions/frontend.md
  ✅ / ❌  .claude/commands/AI-AGENT-EXECUTION-GUIDE.md
```

Missing file → ❌ BLOCKING — stop and tell user which file is missing.

---

### Layer 2 — stack.md Is Filled

```
Read: .claude/project/stack.md

Check:
  ✅ / ❌  At least one platform listed (Backend required)
  ✅ / ❌  Backend framework specified (e.g. Spring Boot)
  ✅ / ❌  Database specified (PostgreSQL / MySQL)
  ✅ / ⚠️  Frontend framework specified (if Frontend tasks expected)
  ✅ / ⚠️  Mobile framework specified (if Mobile tasks expected)
  ✅ / ⚠️  Environment variables section present
  ✅ / ❌  "What is NOT in this project" section present
           (agent needs this to know which instruction files to skip)
```

Empty or mostly-placeholder stack.md → ❌ BLOCKING.
Missing optional fields → ⚠️ WARNING only.

---

### Layer 3 — users.md Is Filled

```
Read: .claude/project/users.md

Check:
  ✅ / ❌  At least one user type defined
  ✅ / ❌  PUBLIC user type defined (or explicitly stated not used)
  ✅ / ❌  Admin user type defined (if admin panel exists)
  ✅ / ❌  @PreAuthorize mapping section present
  ✅ / ❌  Frontend AuthGuard mapping section present
```

No user types defined → ❌ BLOCKING (agent cannot generate correct security code).

---

### Layer 4 — Designs Folder Readiness

```
Scan: designs/

Check:
  ✅ / ❌  At least one .png (or .jpg/.webp) image exists
  ✅ / ⚠️  Every .png has a matching .md file (same name)
  ✅ / ⚠️  Every .md file references a valid page description
           (not just the empty PAGE_TEMPLATE.md placeholder text)

For each image without a .md file:
  → ⚠️ WARNING: designs/[N].png has no matching [N].md
  → Cannot be analyzed until .md is added

For each .md file that still has placeholder text:
  → ⚠️ WARNING: designs/[N].md appears to be unfilled template
  → Check: "Business Description" section is not placeholder text
```

No images at all → ❌ BLOCKING if user is trying to run analyze-designs.
Images without .md files → ⚠️ WARNING (those pages will be skipped).

---

### Layer 5 — Analysis Status

```
Check whether design analysis has been run:

  For each image in designs/:
    Does designs/[N]-requirements.md exist?
      YES → ✅ Analyzed
      NO  → ⚠️ Not yet analyzed

  Summary:
    Analyzed:     [N] / [total] pages
    Not analyzed: [N] pages
```

If no pages analyzed and user is trying to run `/execute-task`:
→ ❌ BLOCKING — run `/analyze-designs` first.

If some pages not analyzed:
→ ⚠️ WARNING — those pages have no tasks yet.

---

### Layer 6 — systemTasks.md Status

```
Read: .claude/systemTasks.md

Check:
  ✅ / ⚠️  File is not empty
  ✅ / ⚠️  At least one task with status ⏳ PENDING exists
  ✅ / ⚠️  No tasks with status ⚠️ NEEDS CLARIFICATION
           (those must be resolved before execution)
```

Empty systemTasks.md → ❌ BLOCKING if user wants to execute tasks.
Tasks with NEEDS CLARIFICATION → ⚠️ WARNING with list of which tasks.

---

### Layer 7 — SpecKit Specs Status

```
Read: .claude/systemTasks.md → get list of all modules

For each module found in systemTasks.md:
  Does specs/[module]/spec.md exist?
    YES → ✅ Specced
    NO  → ⚠️ Not yet specced

Summary:
  Specced:     [N] / [total] modules
  Not specced: [N] modules → run /generate-spec [module]
```

Missing specs → ⚠️ WARNING (agent can still execute using requirements file directly,
but spec provides richer acceptance criteria).

---

### Layer 8 — Configurations Status

```
Read: .claude/configurations.md

Check:
  For each config marked as "Needed by" (detected during analysis):
    Is its status ✅ READY?
      YES → ✅
      NO  → ⚠️ WARNING: [Config Name] needed but not set up
              Will be set up during Phase 1 tasks — expected

  For Phase 1 tasks specifically (scaffold, auth, DB):
    Core configs must be ready:
      Database  → ⚠️ if MISSING and Phase 1 not done yet
      Security  → ⚠️ if MISSING and Phase 1 not done yet
```

All configs missing → ⚠️ WARNING only (expected at project start, Phase 1 sets them up).
Config missing when its dependent task is about to run → ❌ BLOCKING at task level
(handled by AI-AGENT-EXECUTION-GUIDE.md Step 3, not here).

---

### Layer 9 — Budget Status

```
Read: .claude/CLAUDE.md → budget field
Read: .claude/systemTasks.md → sum all completed task costs

Remaining = budget - spent

✅  Remaining > 20% of budget    → healthy
⚠️  Remaining between 10–20%     → low budget warning
❌  Remaining < 10% of budget    → near-exhausted, warn before running
❌  Remaining < $0.05            → stop, cannot run more tasks
```

---

## Output — Full Report (when called directly)

```
🔍 PROJECT READINESS CHECK
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Project: [name from CLAUDE.md]
Checked: DD/MM/YYYY at HH:MM

── CORE FILES ──────────────────────────────────────────
  ✅  .claude/CLAUDE.md
  ✅  .claude/project/stack.md
  ✅  .claude/project/users.md
  ✅  .claude/project/pages.md
  ✅  .claude/configurations.md
  ✅  .claude/tasks.md
  ❌  .claude/systemTasks.md    ← MISSING

── STACK ───────────────────────────────────────────────
  ✅  Backend: Spring Boot listed
  ✅  Frontend: React listed
  ⛔  Mobile: not in this project
  ✅  Database: PostgreSQL
  ✅  Environment variables defined
  ✅  Out-of-scope section present

── USERS ───────────────────────────────────────────────
  ✅  PUBLIC defined
  ✅  ADMIN defined
  ✅  EDITOR defined
  ✅  @PreAuthorize mapping present
  ✅  AuthGuard mapping present

── DESIGNS ─────────────────────────────────────────────
  ✅  14 images found
  ⚠️   designs/3.png — missing 3.md (will be skipped)
  ✅  13 pages have matching .md files

── ANALYSIS STATUS ─────────────────────────────────────
  ✅  12 / 13 pages analyzed (requirements exist)
  ⚠️   designs/9.md — not yet analyzed → run /analyze-page 9

── SYSTEM TASKS ────────────────────────────────────────
  ❌  systemTasks.md is empty
      Run /analyze-designs to generate tasks

── SPEC FILES ──────────────────────────────────────────
  ⚠️   No specs generated yet
      Run /generate-spec [module] after analysis

── CONFIGURATIONS ──────────────────────────────────────
  ⚠️   All configs ❌ MISSING — expected at project start
      Phase 1 tasks will set up: Database, Security, Async, Cache, Email

── BUDGET ──────────────────────────────────────────────
  ✅  Budget: $20.00 remaining (not started)

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

RESULT: ❌ 2 BLOCKING ISSUES — cannot proceed

Blocking issues:
  1. .claude/systemTasks.md is missing → run /analyze-designs
  2. 0 tasks in systemTasks.md → run /analyze-designs

Warnings (non-blocking):
  1. designs/3.png has no 3.md → that page will be skipped
  2. designs/9.md not yet analyzed → run /analyze-page 9
  3. No SpecKit specs yet → run /generate-spec after analysis

Recommended next action:
  /analyze-designs
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

---

## Output — Silent Mode (when called by another command)

When another command calls `/check-ready` automatically before proceeding,
it runs silently and only interrupts on blocking conditions:

```
[Silent — no output if all checks pass]

[If blocking condition found:]
❌ CANNOT PROCEED — pre-flight check failed
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Reason: [specific blocking issue]
Fix:    [exact command to run]
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

---

## Blocking vs Warning vs Info

| Level | Symbol | Meaning | Blocks Execution? |
|---|---|---|---|
| Blocking | ❌ | Must fix before anything can run | Yes |
| Warning | ⚠️ | Should fix but can continue | No |
| Pass | ✅ | All good | — |
| Not applicable | ⛔ | Not in this project (by design) | No |

---

## When Each Command Calls /check-ready

| Command | Calls check-ready? | Mode |
|---|---|---|
| `/analyze-designs` | Yes — at start | Silent |
| `/analyze-page [N]` | Yes — at start | Silent |
| `/generate-spec` | Yes — at start | Silent |
| `/execute-task` | Yes — at start | Silent |
| `/continue-tasks` | Yes — at start | Silent |
| `/check-ready` (direct) | N/A | Full report |
| `/review-progress` | No | — |
| `/fix-task` | No | — |
| `/add-feature` | No | — |

---

## Quick Reference

```
User types:      /check-ready
Agent reads:     All project files (read-only — never writes)
Agent checks:    9 layers from core files to budget
Agent reports:   Full checklist with ✅ / ⚠️ / ❌ per item
Agent suggests:  Exact command to run for each blocking issue
Modifies files:  Never — read-only command
Used by:         All execution commands at startup (silent mode)
```