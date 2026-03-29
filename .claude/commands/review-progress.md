# /review-progress

> **Slash command for OpenClaw / Claude Code**
> Triggered when user types: `/review-progress`
> Shows a full snapshot of the project's current state — no code changes made.

---

## What This Command Does

Reads all project tracking files and produces a comprehensive status report:
- How many tasks are done vs pending vs failed vs blocked
- Which platforms were built for each task
- Test results summary across all tasks
- Budget used vs remaining
- Configurations that are ready vs missing
- What to do next

**This command never modifies any file.**

---

## Execution Instructions

### 1. Read All Tracking Files

```
Read: .claude/systemTasks.md              → task statuses and costs
Read: .claude/CLAUDE.md                   → budget, project name, total tasks
Read: .claude/configurations.md           → config readiness
Read: .claude/project/stack.md            → available platforms
Read: .claude/processed/*.md              → implementation details per task
Read: doc/DATABASE_AUDIT.md              → curl audit status
```

### 2. Build the Status Report

Present the report in this exact structure:

---

```
📊 PROJECT PROGRESS REPORT
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Project:   [name from CLAUDE.md]
Generated: YYYY-MM-DD HH:MM
Platforms: Backend ✅ | Frontend ✅ | Mobile — (not in stack)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

── OVERALL PROGRESS ────────────────────────────────────────
  ██████████░░░░░░░░░░  10 / 20 tasks  (50%)

  ✅ Completed:      10
  ⏳ Pending:         8
  ❌ Failed:          1
  ⚠️  Needs Review:   1
  ⛔ Blocked:         0

── BY PHASE ────────────────────────────────────────────────
  Phase 1 — Foundation       ████████████  3/3  ✅ Done
  Phase 2 — Blog Module      ████████░░░░  2/3  ⏳ In progress
  Phase 3 — Case Studies     ░░░░░░░░░░░░  0/3  ⏳ Pending
  Phase 4 — Testimonials     ░░░░░░░░░░░░  0/2  ⏳ Pending
  Phase 5 — Consultation     ████░░░░░░░░  1/2  ⚠️ Has issues
  Phase 6 — Contact          ░░░░░░░░░░░░  0/2  ⛔ Blocked

── TASK DETAILS ────────────────────────────────────────────
  Phase 1 — Foundation
    ✅  1.1  Project scaffold + Auth         Backend ✅
    ✅  1.2  Database schema                 Backend ✅
    ✅  1.3  Security + JWT                  Backend ✅

  Phase 2 — Blog Module
    ✅  2.1  Blog Admin CRUD APIs            Backend ✅ | Frontend ✅
    ✅  2.2  Blog Public APIs                Backend ✅ | Frontend ✅
    ⏳  2.3  Blog Editor Component           Frontend ⏳

  Phase 3 — Case Studies
    ⏳  3.1  Case Study Admin APIs           Pending
    ⏳  3.2  Case Study Public APIs          Pending
    ⏳  3.3  Case Study Pages                Pending

  Phase 4 — Testimonials
    ⏳  4.1  Testimonials Module             Pending
    ⏳  4.2  Testimonials Admin UI           Pending

  Phase 5 — Consultation
    ✅  5.1  Consultation API                Backend ✅
    ⚠️   5.2  Consultation Form UI           Frontend ⚠️ Needs review
              → Reason: Playwright S4 loading state test failing

  Phase 6 — Contact
    ⛔  6.1  Contact Module                  Blocked
              → Waiting for: Task 5.2

── TESTS SUMMARY ───────────────────────────────────────────
  Backend:
    Tests run:    47 / 47  ✅
    Coverage avg: 84%
    curl audits:  24 operations — all ✅ MATCH

  Frontend:
    Tests run:    30 / 31  ⚠️  (1 failing in Task 5.2)
    Screenshots:  saved in .claude/screenshots/

  Mobile:
    Not in stack — skipped

── CONFIGURATIONS ──────────────────────────────────────────
  ✅ Database          (Task 1.1)
  ✅ Security / JWT    (Task 1.3)
  ✅ Async             (Task 2.1)
  ✅ Email             (Task 5.1)
  ✅ CORS              (Task 1.1)
  ✅ Docker / Local    (Task 1.1)
  ❌ Cache             — not set up yet
  ❌ Rate Limiting     — not set up yet
  ❌ File Storage      — not set up yet
  ❌ API Docs          — not set up yet
  ⛔ SMS               — not in stack.md
  ⛔ Push              — not in stack.md
  ⛔ WhatsApp          — not in stack.md
  ⛔ WebSocket         — not in stack.md

── BUDGET ──────────────────────────────────────────────────
  Total budget:    $20.00
  Spent so far:    $ 8.45
  Remaining:       $11.55
  Avg cost/task:   $ 0.85
  Est. to finish:  $[remaining tasks × avg] ≈ $ 8.50

  Per task breakdown:
    Task 1.1   $0.12   ✅
    Task 1.2   $0.09   ✅
    Task 1.3   $0.14   ✅
    Task 2.1   $0.18   ✅
    Task 2.2   $0.15   ✅
    Task 5.1   $0.11   ✅
    ...

── ISSUES ──────────────────────────────────────────────────
  ⚠️  Task 5.2 — Consultation Form UI
      Platform: Frontend
      Problem:  [S4] Loading state test — skeleton not shown
      Action:   Run /fix-task 5.2

  ⛔  Task 6.1 — Contact Module
      Blocked by: Task 5.2
      Action:     Fix Task 5.2 first, then Task 6.1 will unblock

── WHAT TO DO NEXT ─────────────────────────────────────────

  Recommended actions (in order):

  1️⃣  Fix the failing task first:
      /fix-task 5.2

  2️⃣  Then continue with remaining tasks:
      /continue-tasks

  3️⃣  Or execute the next task manually:
      /execute-task

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

---

### 3. Handle Special States

#### All tasks complete
```
🎉 PROJECT COMPLETE
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
All [N] tasks completed successfully.

Final stats:
  Duration: [N] days / [N] hours total
  Cost: $X.XX of $X.XX budget
  Tests: [N] backend + [N] frontend + [N] mobile — all passing
  Configurations: all ✅ READY (used ones)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

#### No tasks started yet
```
⏳ PROJECT NOT STARTED
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[N] tasks ready to execute.
All configurations: ❌ MISSING (expected — none set up yet)

To begin:
  /execute-task    → start the first task
  /continue-tasks  → run all tasks automatically
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

#### Budget warning
```
⚠️  BUDGET WARNING
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Remaining: $1.20 (low)
Remaining tasks: [N] × avg $0.85 ≈ $X.XX needed

Consider increasing budget in CLAUDE.md before continuing.
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

---

## Quick Reference

```
User types:        /review-progress
Agent reads:       systemTasks.md + CLAUDE.md + configurations.md
                   + stack.md + processed/*.md
Agent builds:      Full status report
Agent modifies:    Nothing — read-only command
Agent shows:       Progress bar, task list, test summary,
                   config status, budget, issues, next actions
```