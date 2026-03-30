# /reset-tasks

> **Slash command for OpenClaw / Claude Code**
> Triggered when user types: `/reset-tasks` or `/reset-tasks 3 4 5`
> Resets task statuses back to ⏳ Pending without deleting any files.

---

## What This Command Does

Resets task statuses in `tasks.md` and `systemTasks.md` back to ⏳ Pending.
Never deletes files, code, or test files — only status fields are changed.

Two modes:

- `/reset-tasks` — resets ALL tasks across the entire project
- `/reset-tasks 3 4 5` — resets only the tasks linked to pages 3, 4, and 5

---

## Execution Steps

### Step 1 — Parse the Target

```
User typed /reset-tasks        → mode: ALL pages
User typed /reset-tasks 3 4 5  → mode: pages 3, 4, 5 only
```

---

### Step 2 — Read Current State

```
Read: .claude/tasks.md
Read: .claude/systemTasks.md
Read: .claude/tests/*.md       (test plan files if they exist)
```

Show what will be reset before touching anything:

```
🔄 RESET PREVIEW
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Mode: [ALL pages / Pages 3, 4, 5 only]

Tasks that will be reset to ⏳ Pending:
  Task 2.1 — Blog Backend APIs           ✅ → ⏳
  Task 2.2 — Blog Frontend              ✅ → ⏳
  Task 3.1 — Case Studies Backend       ❌ → ⏳
  Task 3.2 — Case Studies Frontend      ⏳ (already pending)
  ...

Pages that will be reset:
  Page 3 — Blog List
  Page 4 — Blog Insights
  Page 5 — Blog Editor

Files that will NOT change:
  ✅ Source code files — never touched
  ✅ Test code files (.java, .spec.ts, _test.dart) — never touched
  ✅ requirements.md files — never touched
  ✅ spec.md files — never touched

Proceed? (yes/no)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

Wait for "yes" before making any changes.

---

### Step 3 — Reset tasks.md

For each page in scope — update the entry in `tasks.md`:

**Stage Status table — reset all stages:**
```markdown
| Stage | Status | Task ID | Completed On | Notes |
|-------|--------|---------|--------------|-------|
| Design | ✅ Done | — | [keep original date] | |
| Requirements | ✅ Done | — | [keep original date] | |
| Backend | ⏳ Pending | [keep task ID] | — | |
| Frontend | ⏳ Pending | [keep task ID] | — | |
| Mobile | ⏳ Pending / ⛔ | [keep task ID] | — | |
```

Rules:
- Design stage → always keep ✅ Done (image still exists)
- Requirements stage → always keep ✅ Done (requirements file still exists)
- Backend / Frontend / Mobile → reset to ⏳ Pending
- Keep Task IDs — only clear Completed On date

**Sub-tasks table — reset status and completed date:**
```markdown
| ID | Platform | Description | Status | Depends On | Completed |
|----|----------|-------------|--------|------------|-----------|
| 2.1 | Backend | [...] | ⏳ Pending | — | — |
| 2.2 | Frontend | [...] | ⏳ Pending | 2.1 | — |
```

**Files Built from This Page — clear the file lists:**
```markdown
### Files Built from This Page

**Backend** (Task [X.Y]):
  (reset — will be filled again when task completes)

**Frontend** (Task [X.Z]):
  (reset — will be filled again when task completes)

**Database migrations**:
  (reset — will be filled again when task completes)

**Tests**:
  (reset — will be filled again when task completes)
```

**Summary table at top of tasks.md — reset status columns:**
```markdown
| 3 | Blog List | ✅ | ✅ | ⏳ | ⏳ | ⛔ |
| 4 | Blog Insights | ✅ | ✅ | ⏳ | ⏳ | ⛔ |
| 5 | Blog Editor | ✅ | ✅ | ⏳ | ⏳ | ⛔ |
```

---

### Step 4 — Reset systemTasks.md

For each task linked to pages in scope:

**Reset status field:**
```markdown
- **Status**: ⏳ Pending
```

**Remove completion fields:**
```markdown
- **Completed**: —          (was: DD/MM/YYYY HH:MM)
- **Fixed**: —              (was: DD/MM/YYYY HH:MM, if it existed)
- **Fix summary**: —        (clear if existed)
```

**Reset test results:**
```markdown
Testing:
- **Backend**: — (not run)
- **Frontend**: — (not run)
- **Mobile**: not in scope
- **Build**: —
- **curl DB Audit**: —
```

**Reset token usage:**
```markdown
Token Usage:
- **Total**: —
```

**Keep permanently:**
- Task ID and name
- Platform
- Dependencies
- Spec file reference
- Source page stamp — NEVER remove this
- Complexity and estimated cost
- Analyzed on date

---

### Step 5 — Reset Test Plan Files

For each `.claude/tests/Task X.Y - [Platform] Test Plan.md` file
linked to pages in scope:

Reset the Final Status line at the top:
```markdown
**Status**: ⏳ NOT EXECUTED
**Result**: —
```

Reset all scenario result rows:
```markdown
| TS-F-01 | Page load | ⏳ | — | — | — |
| TS-F-02 | Auth guard | ⏳ | — | — | — |
```

Reset the build output section:
```markdown
### TypeScript Build
```
(reset — will be filled when tests run again)
```
```

**Do not delete the test plan files** — only reset status fields inside them.
The scenario list and test steps are kept exactly as written.

---

### Step 6 — Update CLAUDE.md Current Status

Reset the progress counters:

```markdown
| Tasks completed | 0 |      ← was: [N]
| Cost spent | $0.00 |       ← reset to 0 (or keep actual spend for audit)
```

---

### Step 7 — Present Reset Summary

```
✅ RESET COMPLETE
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Mode: [ALL / Pages 3, 4, 5]

Tasks reset to ⏳ Pending:
  Task 2.1 — Blog Backend APIs          ✅ → ⏳
  Task 2.2 — Blog Frontend Page         ✅ → ⏳
  Task 3.1 — Case Studies Backend       ❌ → ⏳
  Task 4.1 — Testimonials Backend       ⏳ (was already pending)

Pages reset:
  Page 3 — Blog List
  Page 4 — Blog Insights
  Page 5 — Blog Editor

Test plan files reset:
  .claude/tests/Task 2.1 - Backend Test Plan.md   → ⏳ NOT EXECUTED
  .claude/tests/Task 2.2 - Frontend Test Plan.md  → ⏳ NOT EXECUTED

Files NOT changed:
  ✅ All source code files
  ✅ All test code files (.java / .spec.ts / _test.dart)
  ✅ All requirements.md files
  ✅ All spec.md files
  ✅ All notes.md files
  ✅ DATABASE_AUDIT.md

Total tasks reset: [N]
Total tasks still pending (unchanged): [N]

Next steps:
  /continue-tasks     → re-run all pending tasks from scratch
  /execute-task       → run one task at a time
  /review-page [N]    → review a specific page
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

---

## What Is Never Reset

These are permanently preserved regardless of the reset mode:

| What | Why |
|---|---|
| Source code files | Reset is for tracking only — code stays |
| Test code files (.java, .spec.ts, _test.dart) | Tests stay — just re-run them |
| `designs/[N]-requirements.md` | Requirements are still valid |
| `designs/[N].md` | Page spec is still valid |
| `designs/[N]-notes.md` | Human notes are still valid |
| `specs/<module>/spec.md` | SpecKit spec is still valid |
| `doc/DATABASE_AUDIT.md` | Historical audit record kept |
| Source page stamp in systemTasks.md | Permanent — never remove |
| Task IDs and descriptions | Never change task identity |
| Design and Requirements stage in tasks.md | Always ✅ — files exist |

---

## Quick Reference

```
User types:      /reset-tasks           → reset ALL tasks
                 /reset-tasks 3 4 5     → reset only pages 3, 4, 5

Agent reads:     .claude/tasks.md
                 .claude/systemTasks.md
                 .claude/tests/*.md

Agent resets:    Stage status in tasks.md → ⏳ Pending
                 Sub-task status → ⏳ Pending
                 Files Built section → cleared
                 Summary table columns → ⏳
                 Task status in systemTasks.md → ⏳ Pending
                 Completed/Fixed dates → —
                 Test results in systemTasks.md → —
                 Test plan file statuses → ⏳ NOT EXECUTED

Agent never:     Deletes any file
                 Changes source code
                 Removes test code
                 Modifies requirements
                 Removes task IDs or source page stamps

Next step:       /continue-tasks or /execute-task
```