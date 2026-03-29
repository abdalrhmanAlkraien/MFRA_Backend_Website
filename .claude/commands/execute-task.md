# /execute-task

> **Slash command for OpenClaw / Claude Code**
> Triggered when user types: `/execute-task`
> This file is the entry point — it delegates all logic to AI-AGENT-EXECUTION-GUIDE.md

---

## What This Command Does

Executes the next pending task in `systemTasks.md` for one or more platforms
based on what is defined in `project/stack.md`.

---

## Execution Instructions

When this command is triggered, the agent must:

### 1. Read the Execution Guide First

```
Read: .claude/commands/AI-AGENT-EXECUTION-GUIDE.md
Follow the complete 13-step workflow defined there.
Do not skip steps. Do not reorder steps.
```

### 2. Read Stack to Know Available Platforms

```
Read: .claude/project/stack.md

Check what platforms are available:
  Backend listed?   → Backend is available
  React listed?     → Frontend is available
  Flutter listed?   → Mobile is available

Only offer platforms that exist in stack.md.
```

### 3. Find the Next Pending Task

```
Read: .claude/systemTasks.md

Find the first task where Status: ⏳ PENDING
Check all its dependencies are ✅ COMPLETED
```

### 4. Ask the User Which Platform to Build

Present only the platforms available in `stack.md`:

```
📋 NEXT TASK: [Task ID] — [Task Name]
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
What would you like to implement?

  [show only platforms in stack.md]

  1️⃣  Backend only
  2️⃣  Frontend only          ← only if React in stack.md
  3️⃣  Mobile only            ← only if Flutter in stack.md
  4️⃣  Backend + Frontend     ← only if both in stack.md
  5️⃣  Backend + Mobile       ← only if both in stack.md
  6️⃣  All platforms          ← only if all three in stack.md

Type a number to continue.
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

Wait for user answer before proceeding.

### 5. Follow the Full Execution Guide

After the user selects a platform, follow every step in
`AI-AGENT-EXECUTION-GUIDE.md` from Step 1 through Step 13.

---

## Platform — What Gets Built Per Selection

### Backend Only
```
Reads:   instructions/backend.md
         instructions/backend-testing.md
         instructions/database.md
         + conditional files (async, cache, email, etc.)

Builds:  Entity → Repository → Service → Controller → DTOs
         Flyway migration

Tests:   JUnit 5 + MockMvc + Testcontainers
         curl smoke tests + DATABASE_AUDIT.md

Build:   mvn clean verify → must pass
```

### Frontend Only
```
Reads:   instructions/frontend.md
         instructions/frontend-testing.md

Builds:  RTK Query API slice → Types → Pages → Components

Tests:   Playwright E2E scenarios
         Accessibility + Responsive checks

Build:   npm run build → must pass
```

### Mobile Only
```
Stack check: Flutter must be listed in project/stack.md

Reads:   (Flutter-specific patterns from stack.md)

Builds:  Repository → Model → Page → Widgets

Tests:   Flutter widget tests
         Integration tests (if applicable)

Build:   flutter analyze → must pass
```

### All Platforms
```
Executes Backend → Frontend → Mobile in sequence.
Each platform must build clean before moving to the next.
All tests from all platforms must pass.
```

---

## What the Agent Tracks Per Execution

Every `/execute-task` run produces:

```
Documentation:
  .claude/processed/Task X.Y.md              ← implementation record
  .claude/processed/Task X.Y - Test Results.md ← test results per platform

Updated files:
  .claude/systemTasks.md                     ← status updated exactly once
  .claude/configurations.md                  ← any new configs marked ✅ READY
  doc/DATABASE_AUDIT.md                      ← curl DB audit entries
  .claude/screenshots/                       ← Playwright screenshots
```

---

## Token and Time Tracking

Record in `.claude/processed/Task X.Y.md`:

```markdown
## Token Usage
| Phase          | Input  | Output | Cost  |
|----------------|--------|--------|-------|
| Implementation | X,XXX  | X,XXX  | $X.XX |
| Testing        | X,XXX  | X,XXX  | $X.XX |
| Total          | X,XXX  | X,XXX  | $X.XX |

## Execution Time
| Phase               | Duration |
|---------------------|----------|
| Reading files       | X min    |
| Implementation      | X min    |
| Build verification  | X min    |
| Test generation     | X min    |
| Test execution      | X min    |
| Documentation       | X min    |
| Total               | X min    |
```

---

## Blocking Conditions — Stop and Inform User

```
Condition                          Message to show
─────────────────────────────────────────────────────────────────────
No PENDING tasks in systemTasks.md → "All tasks are completed! 🎉"
Dependency task is PENDING         → "Task X.Y is blocked. [Task Z] must complete first."
Build fails after 2 attempts       → "Build is failing. Showing errors — please review."
Tests fail after 2 fix attempts    → "Tests still failing. Showing failures — please review."
Spec is unclear                    → "Spec needs clarification. [Quote + Options]"
Config missing in stack.md         → "Provider not in stack.md — skipping [feature]."
```

---

## Quick Reference

```
User types:        /execute-task
Agent reads:       AI-AGENT-EXECUTION-GUIDE.md  ← full workflow
                   project/stack.md              ← platform availability
                   systemTasks.md                ← next pending task
                   specs/<module>/spec.md        ← requirements
Agent asks:        Which platform?
Agent builds:      What user selected — only if in stack.md
Agent tests:       All platforms selected
Agent documents:   Implementation + test results
Agent updates:     systemTasks.md + configurations.md
Agent presents:    Final summary with next options
```