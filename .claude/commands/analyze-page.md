# /analyze-page [N]

> **Slash command for OpenClaw / Claude Code**
> Triggered when user types: `/analyze-page 3` or `/analyze-page`
> Analyzes a SINGLE design page and merges its tasks into the existing project plan.
> Use this when adding a new page mid-project or re-analyzing an updated design.

---

## What This Command Does

Targets one `designs/[N].png + designs/[N].md` pair.
Runs the full BA workflow on that one page, writes the requirements document,
generates tasks, and merges everything into the existing `systemTasks.md`,
`tasks.md`, and `CLAUDE.md` without touching other pages.

Use this instead of `/analyze-designs` when:
- You add a new page mid-project
- A design is updated and requirements need to be re-extracted
- One page was skipped during the full analysis (missing .md at the time)
- You want to analyze pages one at a time during early planning

---

## Execution Steps

### Step 1 — Identify the Target Page

If user typed `/analyze-page 3` → use `designs/3.png` + `designs/3.md`.

If user typed `/analyze-page` with no number:

```
📋 WHICH PAGE TO ANALYZE?
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Pages available in designs/:

  Not yet analyzed:
    3   designs/3.png + 3.md    → ready
    5   designs/5.png           → missing 5.md

  Already analyzed (re-analyze?):
    1   designs/1.png           → 1-requirements.md exists
    2   designs/2.png           → 2-requirements.md exists

Type the page number:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

### Step 2 — Check for Existing Analysis

```
Does designs/[N]-requirements.md already exist?

  YES → Ask user:
    ⚠️  Page [N] was already analyzed on [date from requirements file].

    Options:
      A) Re-analyze — design was updated, requirements need refreshing
      B) Show existing requirements — no changes
      C) Cancel

    If A chosen → run re-analysis flow (Step 9 below)
    Then produce diff report before touching ANY implementation file

  NO → This is a fresh page → proceed with full analysis
```

**Isolation guarantee:** This command only ever touches files that belong to
page [N]. It never reads or modifies requirements, tasks, or implementation
files from any other page number.

### Step 3 — Read Context

```
Read: .claude/project/stack.md        → platforms + providers
Read: .claude/project/users.md        → access rules
Read: .claude/instructions/pages.md   → BA & PM workflow
Read: .claude/instructions/database.md → DB conventions
Read: .claude/systemTasks.md          → existing tasks (to avoid conflicts)
Read: designs/[N].md                  → page spec (business, access, permissions)
```

### Step 4 — Run Full BA Analysis on This Page

Follow `instructions/pages.md` Steps 1–10 exactly:

```
Step 1  — Read [N].md (context)
Step 2  — Open [N].png (full visual read)
Step 3  — Extract all UI elements section by section
Step 4  — Map every visual element to DB column
Step 5  — Write Flyway migration SQL
Step 6  — Define API endpoints per user action
Step 7  — Extract business rules
Step 8  — Ask clarifying questions if needed
Step 9  — Write designs/[N]-requirements.md
Step 10 — Generate tasks per platform (check stack.md first)
```

Show progress:
```
🔍 ANALYZING Page [N] — [Page Name from .md]
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Reading spec...         ✅
Opening image...        ✅
Extracting elements...  ✅
Mapping to DB...        ✅
Defining endpoints...   ✅
Extracting rules...     ✅
Writing requirements... ✅
Generating tasks...     ✅
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

### Step 5 — Detect Config Signals

Scan the generated `[N]-requirements.md` for config signals:

```
For each signal found:
  Open .claude/configurations.md
  Find the matching config section
  Add this page to the "Needed by" line if not already there:
    Needed by: [existing pages], Page [N] ([page name])
```

### Step 6 — Determine Phase Placement

```
Read: .claude/systemTasks.md → existing phases and last task ID

Determine where this page's tasks fit:
  New module not yet in any phase?
    → Create new phase at the end: Phase [N+1]

  Module already exists in systemTasks.md?
    → Add sub-tasks to existing phase

  Tasks depend on a phase not yet complete?
    → Flag as blocked with dependency noted

Assign task IDs sequentially from the last existing ID.
```

### Step 7 — Merge Into systemTasks.md

**Append** new tasks — never overwrite existing tasks.

Every task generated from this page carries a permanent **Source page** stamp.
This stamp is how the agent knows which tasks belong to which image — forever.

```markdown
## Phase [X] — [Module Name]   ← new phase if needed

### Task [X.Y]: [Module] — [Backend / Frontend]
- **Status**: ⏳ Pending
- **Platform**: Backend / Frontend / Mobile
- **Dependencies**: [existing task IDs]
- **Spec**: specs/<module>/spec.md
- **Complexity**: Simple / Medium / Complex
- **Est. cost**: $X.XX
- **Source page**: designs/[N].png   ← PERMANENT — never change this stamp
- **Analyzed on**: DD/MM/YYYY HH:MM  ← when requirements were extracted
```

### Step 8 — Update tasks.md

Add a new entry for this page (or update if re-analyzing).

```markdown
## Page [N] — [Page Name]

**Design**:       designs/[N].png
**Spec**:         designs/[N].md
**Requirements**: designs/[N]-requirements.md
**Analyzed on**:  DD/MM/YYYY at HH:MM   ← set once, never change unless re-analyzed
**Last update**:  DD/MM/YYYY at HH:MM   ← updated after every task completion

### Stage Status

| Stage | Status | Task ID | Completed On | Notes |
|-------|--------|---------|--------------|-------|
| Design | ✅ Done | — | DD/MM/YYYY | |
| Requirements | ✅ Done | — | DD/MM/YYYY | designs/[N]-requirements.md |
| Backend | ⏳ Pending | [X.Y] | — | |
| Frontend | ⏳ Pending | [X.Z] | — | |
| Mobile | ⛔ Not in scope | — | — | Flutter not in stack.md |

### Sub-tasks

| ID | Platform | Description | Status | Depends On | Completed |
|----|----------|-------------|--------|------------|-----------|
| X.Y | Backend | [...] | ⏳ Pending | — | — |
| X.Z | Frontend | [...] | ⏳ Pending | X.Y | — |

### Files Built from This Page

> This section is filled by the agent after each task completes.
> It is the exact scope the agent uses when re-analysis requires code updates.
> The agent reads this list and ONLY touches these files — nothing else.

**Backend** (Task [X.Y]):
```
(empty until Task X.Y completes)
```

**Frontend** (Task [X.Z]):
```
(empty until Task X.Z completes)
```

**Database migrations**:
```
(empty until Task X.Y completes)
```
```

Also update the Summary Table at the top of `tasks.md`:

```
| [N] | [Page Name] | ✅ | ✅ | ⏳ | ⏳ | ⛔ |
```

### Step 9 — Update CLAUDE.md

Update the task count in Current Status:

```markdown
| Tasks generated | [old count + new tasks] |
```

If a new phase was added, append it to the Phases table.

### Step 10 — Present Results

```
✅ PAGE [N] ANALYZED — [Page Name]
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Requirements: designs/[N]-requirements.md ✅

Database:
  New tables:  [N] (VX__create_<module>_tables.sql)
  Modified:    [N] existing tables

Tasks generated:
  Backend:   [N] tasks → added to Phase [X]
  Frontend:  [N] tasks → added to Phase [X]
  Mobile:    ⛔ not in scope

Configs flagged:
  + Email → Page [N] added to "Needed by"
  (no new configs detected)

Open questions:
  [list if any — or "None — design was clear"]

Files updated:
  ✅ designs/[N]-requirements.md
  ✅ .claude/systemTasks.md  (+[N] tasks)
  ✅ .claude/tasks.md        (Page [N] added)
  ✅ .claude/configurations.md (if config detected)
  ✅ .claude/CLAUDE.md       (task count updated)

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Options:
  1️⃣  /analyze-page [N+1]  → analyze the next page
  2️⃣  /generate-spec       → generate SpecKit spec for this module
  3️⃣  /execute-task        → start next pending task
  4️⃣  /review-progress     → see full project status
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

---

## Re-Analysis Behavior

When re-analyzing a page that was already analyzed (user chose option A):

**The agent scope is strictly limited to files that belong to page [N].**
It identifies this scope from the "Files Built from This Page" section
in tasks.md for page [N]. It will NOT read or modify any file outside that list.

```
1. Read tasks.md → Page [N] → "Files Built from This Page"
   → This is the exact file scope — nothing outside it will be touched

2. Read existing designs/[N]-requirements.md → note current state

3. Run fresh analysis on the updated design image

4. Build the diff: old requirements vs new requirements

5. Present diff report to user — wait for "yes" before changing anything

6. If approved:
   a. Overwrite designs/[N]-requirements.md — add entry to Fix History
   b. For each changed item — only touch scoped files:
      → New table      → new Flyway migration (new file, does not modify existing)
      → Changed column → new Flyway ALTER migration (new file)
      → New endpoint   → new method in scoped controller/service only
      → Changed rule   → update scoped service method only
   c. Mark affected tasks ⚠️ NEEDS REVIEW in systemTasks.md
   d. Update specs/[module]/spec.md — only the affected acceptance criteria
   e. Update tasks.md → Page [N] → Last update date
   f. NEVER touch any file outside the scoped list
```

**Diff report format:**
```
🔄 RE-ANALYSIS DIFF — Page [N]: [Page Name]
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Original analysis:  DD/MM/YYYY
Re-analyzed:        DD/MM/YYYY
Scope — only these files may be updated:
  [exact list from "Files Built from This Page" in tasks.md]

Changes detected:

  Database:
    + NEW column:   table.column_name TYPE
      → New migration: VX__add_column_to_table.sql
    ~ CHANGED rule: [what changed]
    = NO CHANGE:   [tables with no changes]

  API:
    + NEW endpoint: METHOD /path → new method in [ScopedController.java]
    ~ CHANGED:      METHOD /path → [field added/removed] → [ScopedService.java]
    = NO CHANGE:    [endpoints unchanged]

  Business rules:
    + NEW:  [rule description] → [which scoped file is affected]
    ~ CHANGED: [old rule] → [new rule] → [which scoped file]

  Spec update needed:
    specs/[module]/spec.md → AC-[N] will be updated

  Files confirmed NOT touched (other pages stay untouched):
    ✅ designs/[other-N]-requirements.md — not touched
    ✅ All Task [other-X.Y] files — not touched
    ✅ All other pages in tasks.md — not touched

Affected tasks (this page only):
  ⚠️ Task [X.Y] — [name] → [reason]

Proceed with update? (yes/no)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```


---

## Quick Reference

```
User types:      /analyze-page [N]
Agent reads:     instructions/pages.md + database.md + stack.md + users.md
                 designs/[N].md + designs/[N].png
Agent runs:      Full BA workflow on one page
Agent writes:    designs/[N]-requirements.md
Agent merges:    New tasks into systemTasks.md (append — never overwrite)
Agent updates:   tasks.md + configurations.md + CLAUDE.md
Agent presents:  Results + diffs (if re-analysis) + next options
Difference from /analyze-designs: targets one page, merges — does not reset
```