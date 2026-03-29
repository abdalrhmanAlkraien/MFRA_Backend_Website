# /fix-task [task-id]

> **Slash command for OpenClaw / Claude Code**
> Triggered when user types: `/fix-task 3.1` or `/fix-task`
> Fixes a specific failed or blocked task without re-executing the whole pipeline.

---

## What This Command Does

Targets a single task that is in one of these states:
- ❌ FAILED — build or tests failed
- ⚠️ NEEDS REVIEW — stopped after 2 auto-fix attempts in continue mode
- ⛔ BLOCKED — dependency issue or spec unclear
- 📋 NEEDS CLARIFICATION — requirements were ambiguous at analysis time

It diagnoses the root cause — code issue **or** requirements issue — fixes it,
updates the right files, re-runs only the affected tests, and marks the task
complete if all gates pass.

**Key principle:** If the root cause is in the code → fix the code.
If the root cause is in the requirements → fix the requirements first,
then fix the code. Never fix code based on wrong requirements.

---

## Execution Instructions

### 1. Read Context

```
Read: .claude/commands/AI-AGENT-EXECUTION-GUIDE.md
Read: .claude/project/stack.md
Read: .claude/systemTasks.md                          → find the target task
Read: .claude/processed/Task X.Y.md                   → what was implemented
Read: .claude/processed/Task X.Y - Test Results.md    → what failed and why
Read: specs/<module>/spec.md                          → spec acceptance criteria
Read: specs/<module>/plan.md                          → implementation decisions
Read: designs/[N]-requirements.md                     → original extracted requirements
     (find [N] by matching the module to the source page in systemTasks.md)
```

Reading `designs/[N]-requirements.md` is mandatory — it is the source of truth
for what the page should do. Never fix code without checking whether the
requirements themselves are correct.

---

### 2. Identify the Task

If user typed `/fix-task 3.1` — use Task 3.1.

If user typed `/fix-task` with no ID:

```
🔍 WHICH TASK TO FIX?
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Tasks needing attention:

  ❌  Task 3.1 — Case Studies Admin APIs
      Reason: [S3] Auth guard test failed

  ⚠️  Task 5.2 — Consultation Module
      Reason: Stopped after 2 auto-fix attempts

  ⛔  Task 6.1 — Contact Module
      Reason: Dependency Task 5.2 not completed

  📋  Task 4.2 — Blog Editor
      Reason: Requirements ambiguity flagged at analysis

Which task would you like to fix? (type task ID)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

---

### 3. Diagnose the Root Cause

Read all context files and identify the failure category precisely:

```
📋 DIAGNOSING Task [X.Y] — [Task Name]
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Failure type (select one):
  [ ] Code bug             → implementation error (wrong logic, missing annotation)
  [ ] Build failure        → compile error, missing import, wrong type
  [ ] Test failure         → test scenario not passing for code reasons
  [ ] DB mismatch          → curl response ≠ DB state
  [ ] Blocked              → dependency task not completed
  [ ] Config missing       → required config not set up yet
  [ ] Requirements wrong   → extracted requirements were incorrect or incomplete
  [ ] Spec contradiction   → spec.md contradicts requirements or design
  [ ] Requirements unclear → ambiguous rule — multiple valid interpretations

Root cause origin:
  [ ] Code layer  → fix the code, requirements are correct
  [ ] Requirements layer → fix requirements first, then fix code

Platform: [Backend / Frontend / Mobile]
Scenario: [which test failed]
Error:    [exact error message]
Diagnosis: [agent's assessment of root cause and origin]
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

**The root cause origin decision is critical.**

Ask: "Is the code wrong given correct requirements, or are the requirements
themselves wrong?"

```
Code is wrong     → Fix code → re-run tests
Requirements wrong → Fix requirements → update spec → fix code → re-run tests
```

---

### 4. Present Fix Plan

Show exactly what will change before touching any file:

```
🔧 FIX PLAN — Task [X.Y]
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Root cause origin: [Code layer / Requirements layer]
Problem:   [description]
Root cause: [specific diagnosis]
Platform:  [Backend / Frontend / Mobile]

[If requirements layer:]
Requirements change:
  → designs/[N]-requirements.md — [what will be corrected]
  → specs/[module]/spec.md     — [which AC will be updated]

Code change:
  → [file to modify] — [what will change]

[If code layer:]
Code change:
  → [file to modify] — [what will change]

Tests to re-run:
  → [failed scenario] only → then full suite for regression

Estimated fix time: [N] minutes
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Proceed with fix? (yes/no)
```

Wait for "yes" before making any changes.

---

### 5. Fix Requirements (if root cause is in requirements layer)

**Only run this step if root cause origin = Requirements layer.**

#### 5a — Update designs/[N]-requirements.md

Open the requirements file that is the source for this module and make
the correction:

```
📋 UPDATING REQUIREMENTS — designs/[N]-requirements.md
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Section:  [Business Rules / API Endpoints / Database Requirements]
Before:   [what the requirements said]
After:    [what they should say]
Reason:   [why this was wrong — test failure / design contradiction / user input]
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

Add a fix log entry at the bottom of the requirements file:

```markdown
---

## Fix History

| Date | Fixed By | Section Changed | Problem | Correction |
|---|---|---|---|---|
| DD/MM/YYYY | /fix-task | Business Rules | [what was wrong] | [what was corrected] |
```

#### 5b — Update specs/[module]/spec.md

If the requirements change affects an acceptance criterion, update `spec.md`:

```
Update the affected AC in specs/[module]/spec.md:
  Before: [ ] [AC-N] [old criterion]
  After:  [ ] [AC-N] [corrected criterion]

Add a note:
  > Updated DD/MM/YYYY — corrected via /fix-task [X.Y]
  > Reason: [brief explanation]
```

#### 5c — Report the Requirements Change to User

```
📋 REQUIREMENTS UPDATED
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
File:    designs/[N]-requirements.md
Section: [section name]
Change:  [what was corrected]

This also updated:
  specs/[module]/spec.md → [AC-N] corrected

Proceeding to fix the code based on corrected requirements...
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

---

### 6. Apply the Code Fix

Show each change as it is made:

```
🔧 APPLYING CODE FIX — Task [X.Y]
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✓ [FileName].java — [what was changed]
✓ Verifying no related issues in same file...
  → All related code checked ✅
```

---

### 7. Build Verification

Run the build for the affected platform only:

```bash
# Backend fix
mvn clean compile
# Must be: BUILD SUCCESS — 0 errors

# Frontend fix
npm run build
# Must be: 0 TypeScript errors

# Mobile fix
flutter analyze
# Must be: No issues found
```

If build fails after fix:
```
❌ Build still failing after fix attempt.
Error: [new error message]

Options:
  1️⃣  Show full error details
  2️⃣  Try a different fix approach
  3️⃣  Leave as ⚠️ NEEDS REVIEW — requires manual intervention
```

---

### 8. Re-run Tests

**First:** Run only the failed scenario to confirm the fix:

```
🧪 RE-TESTING Failed Scenario
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[S3] Auth guard — 403 returned ✅ FIXED
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Fix confirmed. Running full test suite...
```

**Then:** Run the full suite for regression:

```
🧪 FULL SUITE — Task [X.Y]
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Backend:
[1/8] Create — happy path ✅
[2/8] No token → 401 ✅
[3/8] Auth guard → 403 ✅  ← was failing, now fixed
[4/8] Get by ID ✅
[5/8] Not found → 404 ✅
[6/8] Update ✅
[7/8] Soft delete ✅
[8/8] Public API ✅
Backend: 8/8 ✅

Frontend: 6/6 ✅  (if in scope)
Mobile:   5/5 ✅  (if in scope)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
All tests passing ✅
```

---

### 9. Handle Fix for Each Failure Type

#### Code Bug / Build Failure
```
Read the exact error (compiler error, MockMvc failure, Playwright output)
Identify the specific cause: missing annotation, wrong type, wrong selector
Fix the specific line(s) — minimum targeted change
Run build → run failed scenario → run full suite
```

#### Test Failure — Backend
```
Read MockMvc failure message carefully
Identify: wrong status code / missing field / wrong data / security gap
Fix service / controller / entity method
Run the failed scenario first, then full suite
Update DATABASE_AUDIT.md if any curl tests were affected
```

#### Test Failure — Frontend
```
Read Playwright failure output
Identify: selector not found / wrong URL / state not updating / missing testid
Fix the specific component / page / hook
Re-run the failed Playwright scenario
Check screenshots in .claude/screenshots/ for visual clues
```

#### Test Failure — Mobile (Flutter)
```
Read flutter test failure output
Identify: widget not rendered / null / navigation / state issue
Fix the specific widget / page / repository
Run: flutter test test/features/<module>/
```

#### DB Mismatch
```
Read DATABASE_AUDIT.md entry marked ❌ MISMATCH
Compare API response fields vs actual DB row
Identify: mapper issue / missing column / wrong field name
Fix mapper / DTO / entity
Re-run curl smoke test for that specific operation
Update DATABASE_AUDIT.md entry to ✅ MATCH
```

#### Blocked — Dependency
```
Identify which dependency task is PENDING or FAILED
Option A: Run /execute-task [dependency-id] first
Option B: If dependency was actually done, correct its status in systemTasks.md
After dependency completes → re-run /fix-task [blocked-task-id]
```

#### Requirements Wrong or Unclear
```
Run Step 5 (Fix Requirements) above before touching any code
Only fix code after requirements are corrected
If correction requires a new Flyway migration → create it
If correction removes a field → add migration to drop or rename column
```

#### Config Missing
```
Read instructions/[config].md → Step 0 for setup
Set up the config
Mark ✅ READY in configurations.md
Re-run the task
```

---

### 10. Update Documentation

After successful fix, update all relevant files:

**Update** `.claude/processed/Task X.Y.md`:
```markdown
## Fix Applied — DD/MM/YYYY HH:MM

**Fixed by**: /fix-task command
**Root cause origin**: Code layer / Requirements layer
**Problem**: [description]
**Root cause**: [specific cause]
**Requirements changed**: Yes / No
  → If yes: designs/[N]-requirements.md — [what was corrected]
  → If yes: specs/[module]/spec.md — [AC updated]
**Code fix**: [what code change was made]
**Additional tokens**: [input] / [output] — $[cost]
```

**Update** `.claude/processed/Task X.Y - Test Results.md` — append fix history:
```markdown
## Fix History

| # | Date | Root Cause | Origin | Fix | Result |
|---|---|---|---|---|---|
| 1 | DD/MM/YYYY | [cause] | Code / Requirements | [fix] | ✅ Fixed |
```

**Update** `.claude/configurations.md` — if missing config was the root cause.

---

### 11. Update systemTasks.md AND tasks.md — Atomically

Update both files in the same step. Never update one without the other.

**systemTasks.md:**
```markdown
### Task X.Y: [Task Name]
- **Status**: ✅ COMPLETED
- **Fixed**: DD/MM/YYYY HH:MM
- **Root cause**: [code / requirements]
- **Fix summary**: [one line]
- **Tests after fix**: [N/N] ✅
- **Requirements updated**: Yes (designs/[N]-requirements.md) / No
```

**tasks.md — update the stage for this page immediately after:**
```markdown
| Backend | ✅ Done | .claude/Phases/PhaseX/Task X.Y.md | Fixed DD/MM/YYYY |
```

Summary table at top of tasks.md — update the Backend / Frontend column for this page.

---

### 12. Present Results

```
✅ Task [X.Y] FIXED AND COMPLETED
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Task:     X.Y — [Task Name]
Fix time: [N] minutes
Cost:     $[X.XX] (fix attempt)

Root cause: [code / requirements]
Fix applied:
  [If requirements changed:]
  → designs/[N]-requirements.md — corrected [section]
  → specs/[module]/spec.md — AC-[N] updated
  → [FileName].java — [code change]

  [If code only:]
  → [FileName].java — [code change]

Tests after fix:
  Backend:   [N/N] ✅
  Frontend:  [N/N] ✅
  Mobile:    — (not in scope)

Files updated:
  ✅ .claude/processed/Task X.Y.md
  ✅ .claude/processed/Task X.Y - Test Results.md
  ✅ .claude/systemTasks.md
  ✅ .claude/tasks.md
  [✅ designs/[N]-requirements.md  ← if requirements were changed]
  [✅ specs/[module]/spec.md       ← if AC was updated]

Options:
  1️⃣  /continue-tasks  → resume running remaining tasks
  2️⃣  /review-progress → see full project status
  3️⃣  /execute-task    → run next task manually
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

---

## Cannot Fix Automatically — Report to User

```
⚠️ CANNOT FIX AUTOMATICALLY — Task [X.Y]
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Reason: [one of the below]

  - Requirements contradiction: requirements say X, design shows Y
  - Architecture decision needed: two valid approaches
  - External dependency: requires API key / third-party credential
  - Cascading failure: fixing this breaks N other tests
  - Data migration needed: requires user decision on data handling
  - Design missing: no image to re-analyze the ambiguous section

What I need from you:
  [specific question or decision — quoted from requirements/spec]

Task status: ⚠️ NEEDS REVIEW
Files unchanged until you respond.
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

---

## Quick Reference

```
User types:        /fix-task [id]  or  /fix-task
Agent reads:       AI-AGENT-EXECUTION-GUIDE.md
                   systemTasks.md → find the task
                   processed/Task X.Y - Test Results.md → failure details
                   specs/<module>/spec.md → acceptance criteria
                   designs/[N]-requirements.md → source of truth requirements
Agent diagnoses:   Failure type + ROOT CAUSE ORIGIN (code vs requirements)
Agent shows:       Fix plan (includes requirements changes if needed) — waits for yes
Agent fixes:       Requirements first (if needed) → then code
Agent tests:       Failed scenario first → full suite for regression
Agent updates:     requirements.md + spec.md (if changed)
                   processed docs (implementation record + test results)
                   systemTasks.md + tasks.md (ATOMICALLY — same step)
                   configurations.md (if config was the issue)
Agent presents:    Full summary of what changed + next options
```