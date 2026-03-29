# /continue-tasks

> **Slash command for OpenClaw / Claude Code**
> Triggered when user types: `/continue-tasks`
> Keeps executing pending tasks one after another without stopping between them.

---

## What This Command Does

Runs `/execute-task` repeatedly until one of these happens:
- All tasks are completed
- A task fails and cannot be fixed automatically
- A blocking condition is met that requires user input
- Budget limit is reached

This is the "run until done" mode. The agent executes task after task,
documents each one, and only stops when it must.

---

## Execution Instructions

When this command is triggered, the agent must:

### 1. Read the Execution Guide First

```
Read: .claude/commands/AI-AGENT-EXECUTION-GUIDE.md
Read: .claude/commands/execute-task.md
Follow the workflow defined in AI-AGENT-EXECUTION-GUIDE.md for every task.
```

### 2. Read Stack to Know Available Platforms

```
Read: .claude/project/stack.md

Check what platforms are available:
  Backend listed?   → Backend is available
  React listed?     → Frontend is available
  Flutter listed?   → Mobile is available
```

### 3. Ask the User — One Time Only

Ask this question **once at the start** — not before every task:

```
⚙️  CONTINUE MODE — Run All Pending Tasks
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
I will execute all pending tasks without stopping.

Which platforms should I build for every task?

  [show only platforms listed in stack.md]

  1️⃣  Backend only
  2️⃣  Frontend only          ← only if React in stack.md
  3️⃣  Mobile only            ← only if Flutter in stack.md
  4️⃣  Backend + Frontend     ← only if both in stack.md
  5️⃣  Backend + Mobile       ← only if both in stack.md
  6️⃣  All platforms          ← only if all three in stack.md

Type a number to start.
I will apply this selection to every pending task automatically.
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

Wait for user answer. After that — execute without asking again.

---

### 4. Show a Run Plan Before Starting

After user selects platform, show what will be executed:

```
📋 RUN PLAN
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Platforms: [selected platforms]
Tasks to run: [N] pending tasks

  ⏳  Task 2.1 — Blog Module — Admin CRUD APIs
  ⏳  Task 2.2 — Blog Module — Public APIs
  ⏳  Task 3.1 — Case Studies — Admin CRUD APIs
  ⏳  Task 3.2 — Case Studies — Public APIs
  ⏳  Task 4.1 — Testimonials Module
  ... [all pending tasks]

Budget remaining: $X.XX
Estimated cost:   $X.XX per task × N tasks ≈ $X.XX total

Proceeding in 3 seconds...
(type "stop" to cancel before I start)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

---

### 5. Execute Tasks in Loop

For each pending task in `systemTasks.md` — in order:

```
For each task where Status: ⏳ PENDING:

  a. Check all dependencies are ✅ COMPLETED
     → If not: mark task as ⛔ BLOCKED, skip it, continue to next

  b. Execute the task following AI-AGENT-EXECUTION-GUIDE.md Steps 1–13
     → Do NOT ask the user for platform selection again (already decided)
     → Do NOT wait for "yes" before implementing — proceed automatically
     → DO still show task summary before implementing (Step 4 of guide)

  c. If task PASSES all quality gates:
     → Mark ✅ COMPLETED
     → Show brief completion summary
     → Continue to next task immediately

  d. If task FAILS (build or tests):
     → Attempt to fix automatically (max 2 fix attempts)
     → If fixed: continue
     → If still failing after 2 attempts: STOP and report to user

  e. After every task — show running totals:
     → Tasks completed so far
     → Total cost so far
     → Budget remaining
```

---

### 6. Progress Display Between Tasks

Show a brief separator between each completed task:

```
✅ Task 2.1 DONE — 38 min — $0.14
   Backend: 8/8 ✅ | Frontend: 6/6 ✅ | Mobile: — 
   ─────────────────────────────────────
▶  Starting Task 2.2 — Blog Module — Public APIs...
```

---

### 7. Auto-Fix Attempts

If a build or test fails during continue mode:

```
❌ Task 3.1 — Test failure detected
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Failed: [S3] Auth guard — redirect → 403 not returned
Attempt 1/2: Fixing...
  → Adding @PreAuthorize to endpoint
  → Re-running tests...
✅ Fixed. Continuing.
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

If second attempt also fails:

```
❌ Task 3.1 — Still failing after 2 fix attempts
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Failure:   [S3] Auth guard — 403 not returned
Root cause: Spring Security config not matching endpoint pattern
Status:    ⚠️ NEEDS REVIEW

I cannot fix this automatically.
Please review and then run /fix-task 3.1

Remaining tasks (N):
  ⏳ Task 3.2 — Case Studies Public APIs
  ⏳ Task 4.1 — Testimonials
  ...

Continue with remaining tasks? (yes/no)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

Wait for user response. If "yes" — skip the failed task and continue with the rest.

---

### 8. Budget Check Before Each Task

Before starting each task — check remaining budget:

```
Read: .claude/CLAUDE.md → app.budget (total budget)
Read: .claude/systemTasks.md → sum of all completed task costs

Remaining = budget - spent so far

If remaining < $0.05 (estimated minimum per task):
  Stop and warn:
  "⚠️ Budget nearly exhausted ($X.XX remaining).
   Stopping to avoid overage. Run /continue-tasks to resume
   after reviewing your budget in CLAUDE.md."
```

---

### 9. Blocked Task Handling

If a task's dependencies are not yet completed:

```
⛔ Task 4.2 — BLOCKED
   Reason: Task 4.1 (Testimonials Base) is still ⏳ PENDING
   Action: Skipping — will retry after 4.1 completes

[continues to next non-blocked task]
```

If the blocking task is also pending — execute it first, then return to the blocked task.

---

### 10. Final Summary When All Tasks Complete

```
🎉 ALL TASKS COMPLETED
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Total tasks:   [N] completed, [N] skipped/blocked
Total time:    [N] hours [N] minutes
Total cost:    $X.XX of $X.XX budget used

Platform breakdown:
  Backend:   [N] tasks — all tests passed ✅
  Frontend:  [N] tasks — all tests passed ✅
  Mobile:    [N] tasks — all tests passed ✅  (or — if not in stack)

Tasks with issues:
  ⚠️ Task 3.1 — needs manual review (stopped after 2 fix attempts)

Documentation:
  .claude/processed/ — [N] task files created
  doc/DATABASE_AUDIT.md — updated ✅
  .claude/configurations.md — updated ✅

Options:
  1️⃣  review-progress  → see full status of all tasks
  2️⃣  fix-task [id]    → fix any task that needs review
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

---

## Blocking Conditions — Stop and Ask User

```
Condition                                     Action
──────────────────────────────────────────────────────────────────
All tasks completed                           Show final summary
Task fails after 2 auto-fix attempts          Stop, report, ask to continue
Budget exhausted                              Stop, warn user
Spec is unclear (cannot infer intent)         Stop, ask for clarification
Missing config not in stack.md                Log warning, skip feature, continue
Circular dependency detected                  Stop, report to user
```

---

## Difference from /execute-task

| Behaviour | `/execute-task` | `/continue-tasks` |
|---|---|---|
| Asks platform per task | ✅ Yes — every task | ❌ No — asks once at start |
| Asks "ready to proceed?" | ✅ Yes — every task | ❌ No — proceeds automatically |
| Stops after one task | ✅ Yes | ❌ No — runs until done |
| Auto-fix on failure | ❌ No — shows error | ✅ Yes — 2 attempts |
| Shows run plan | ❌ No | ✅ Yes — before starting |
| Budget check | Per task in guide | ✅ Before every task |
| Blocked task handling | Manual | ✅ Auto-skips, retries later |

---

## Quick Reference

```
User types:        /continue-tasks
Agent reads:       AI-AGENT-EXECUTION-GUIDE.md + execute-task.md
Agent asks:        Platform selection — once only
Agent shows:       Run plan with all pending tasks
Agent executes:    All pending tasks in order, automatically
Agent stops at:    Failure (after 2 fix attempts) / budget / all done
Agent documents:   Every task — same as /execute-task
Agent updates:     systemTasks.md + configurations.md after each task
Agent presents:    Final summary when all done
```