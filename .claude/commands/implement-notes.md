# /implement-notes [N]

> **Slash command for OpenClaw / Claude Code**
> Triggered when user types: `/implement-notes 3` or `/implement-notes`
> Reads the human-written notes in `designs/[N]-notes.md`,
> implements every pending item, runs tests, and marks each note done.

---

## What This Command Does

After you manually test a page and write notes about what is wrong or missing,
this command reads those notes and implements everything.

It handles four types of work:
- **BUG** — fixes broken behavior in existing code
- **MISSING** — builds features that were never implemented
- **DATA** — adds seed data, migrations, or dropdown values
- **EDITOR** — implements or upgrades rich text editors and file uploads

Each note is worked on independently in order. After each note the agent
builds, tests, and marks the note ✅ Done before moving to the next.

---

## Execution Steps

### Step 1 — Read the Notes File

```
Read: designs/[N]-notes.md

If file does not exist:
  ⚠️  No notes file found for page [N].
  Create designs/[N]-notes.md using designs/PAGE_NOTES_TEMPLATE.md
  Fill in your notes, then run /implement-notes [N] again.

If file exists:
  Parse all notes with Status: ⏳ Pending
  Skip notes with Status: ✅ Done
  Report how many notes to implement
```

Also read:
```
Read: designs/[N].md                  → original page spec
Read: designs/[N]-requirements.md     → extracted requirements
Read: .claude/project/stack.md        → technology constraints
Read: .claude/project/users.md        → permission rules
Read: .claude/tasks.md                → existing tasks for this page
Read: designs/DESIGN.md               → design system (if exists)
```

---

### Step 2 — Show Notes Summary Before Starting

```
📋 NOTES FOR PAGE [N] — [Page Name]
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Notes to implement: [N]
  Note 1 — [BUG]     [title]              High
  Note 2 — [DATA]    [title]              High
  Note 3 — [EDITOR]  [title]              Medium

Notes already done: [N]
  (none / list titles)

Starting with Note 1 in 3 seconds...
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

---

### Step 3 — Implement Each Note

For each pending note — follow this process:

#### 3a — Diagnose

```
📋 NOTE [#] — [Category]: [Title]
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Problem:   [from notes file]
Expected:  [from notes file]
Scope:     Backend [✓/✗]  Frontend [✓/✗]  DB [✓/✗]

Root cause: [agent's diagnosis]
Files to change:
  → [file path] — [what will change]
  → [file path] — [what will change]
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Implementing...
```

#### 3b — Implement by Category

**BUG notes:**
```
1. Identify the exact broken line or missing logic
2. Read the surrounding code for context
3. Apply the minimum targeted fix
4. Verify the fix does not break adjacent behavior
```

**MISSING notes:**
```
1. Read designs/[N]-requirements.md for the feature's full spec
2. Read designs/DESIGN.md for visual patterns
3. Implement backend if needed (endpoint, service method)
4. Implement frontend (component, route, RTK Query hook)
5. Register route in router if new page
6. Add data-testid attributes to all new elements
```

**DATA notes:**
```
1. Check if the table exists in existing Flyway migrations
2. If table missing → create it in a new migration file
   VX__create_[table].sql
3. Add seed data in a separate migration:
   VX__seed_[table].sql
   Use ON CONFLICT DO NOTHING for idempotency
4. Verify GET endpoint returns the seed data
5. Verify frontend dropdown loads from the endpoint (not hardcoded)
```

**EDITOR notes:**
```
1. Check current package.json — is TipTap installed?
   If not → npm install @tiptap/react @tiptap/starter-kit [extensions]
2. Replace the current input/textarea with TipTap EditorContent
3. Build the toolbar with all required format buttons
4. Wire each button to the correct TipTap chain command
5. Implement file upload if image support is required:
   → file picker → validate → presigned URL → S3 → insert into editor
6. Verify content saves as HTML to the backend
7. Verify content loads back correctly in edit mode
```

**STYLE notes:**
```
1. Read designs/DESIGN.md — find the correct token
2. Replace the incorrect class/color/font with the correct one
3. Verify against the design image
```

**BEHAVIOR notes:**
```
1. Read designs/[N]-requirements.md → find the business rule
2. Identify where the logic lives (service / component / hook)
3. Fix the logic to match the expected behavior
4. Add or update the test that covers this behavior
```

---

#### 3c — Build After Each Note

After implementing a note — always verify before moving on:

**Backend change:**
```bash
mvn clean compile
# Must be: BUILD SUCCESS — 0 errors
```

**Frontend change:**
```bash
npm run build
# Must be: 0 TypeScript errors
```

**Both:**
```bash
mvn clean compile && npm run build
```

If build fails — fix the error before marking the note done or moving to the next note.

---

#### 3d — Smoke Test After Each Note

Start the dev server and verify the specific behavior from the note:

```
🔍 SMOKE TEST — Note [#]
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[For BUG]:
  Navigate to the page → confirm crash is gone → confirm correct behavior

[For MISSING]:
  Navigate to the new feature → confirm it renders → perform the action
  → confirm API call fires → confirm DB record created/updated

[For DATA]:
  Call GET /api/admin/[resource] → confirm seed data returned
  Navigate to form → open dropdown → confirm options appear

[For EDITOR]:
  Navigate to editor page → confirm editor renders
  Type text → apply Bold → confirm text bolds
  Click heading button → confirm heading applied
  Click image button → select file → confirm upload + insertion
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

---

#### 3e — Update Note Status

After smoke test passes — update the note in `designs/[N]-notes.md`:

```markdown
### Note [#]
**Status**: ✅ Done — implemented DD/MM/YYYY HH:MM

**What was done**:
[Brief description of what was implemented]

**Files changed**:
  - [file path] — [what changed]
  - [file path] — [what changed]
```

Then update the summary table at the bottom of the notes file:

```markdown
| [#] | [Category] | [Problem title] | ✅ Done | [files changed] |
```

---

### Step 4 — Update Tests

After all notes are implemented — check if existing tests need updating:

```
For each note that changed backend code:
  → Does the relevant test in <Module>ControllerTest.java cover the fix?
  → If not → add a test method for the specific behavior

For each note that changed frontend code:
  → Does tests/e2e/<module>/<page>.spec.ts cover the fix?
  → If not → add a Playwright scenario

For each DATA note:
  → Add a test that verifies the seed data endpoint returns the expected values

For each EDITOR note:
  → Add a Playwright scenario that:
      1. Navigates to the editor
      2. Types content
      3. Applies a format (bold, heading)
      4. Submits the form
      5. Verifies the saved content contains the formatted HTML
```

Run tests after updating:
```bash
mvn clean verify          # backend
npx playwright test       # frontend
```

---

### Step 5 — Update designs/[N]-requirements.md

If a note reveals that the original requirements were incomplete:

```
Open designs/[N]-requirements.md
Add the missing rule, endpoint, or field in the correct section
Add a Fix History entry at the bottom:

## Fix History
| Date | Note # | Section | What was missing | What was added |
|---|---|---|---|---|
| DD/MM/YYYY | Note 3 | Editor | TipTap not specified | Added TipTap + toolbar + S3 image upload |
```

---

### Step 6 — Present Final Report

```
✅ NOTES IMPLEMENTED — Page [N]: [Page Name]
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

NOTES SUMMARY:
  Note 1 — [BUG]    Crash on page load             ✅ Done
  Note 2 — [DATA]   Categories dropdown empty       ✅ Done
  Note 3 — [EDITOR] Markdown editor not working     ✅ Done

WHAT WAS BUILT:
  [Note 1 — Bug fix]
    Fixed: BlogListPage.tsx line 376 — added data?.data ?? [] guard
    Build: ✅ 0 TypeScript errors

  [Note 2 — Data]
    Created: V8__seed_blog_categories.sql (3 categories)
    Verified: GET /api/admin/blog-categories returns 3 items
    Verified: Category dropdown now shows options

  [Note 3 — Editor]
    Installed: @tiptap/react @tiptap/starter-kit @tiptap/extension-image
    Built:     BlogEditorPage.tsx with full TipTap toolbar
    Supports:  Bold, Italic, H1/H2/H3, Code, List, Quote, Image upload
    Upload:    blogs/content-images/ folder on S3

FILES CHANGED:
  src/features/blog/BlogListPage.tsx           (bug fix)
  src/resources/db/migration/V8__seed_blog_categories.sql  (new)
  src/features/blog/BlogEditorPage.tsx         (new)
  src/features/blog/components/EditorToolbar.tsx (new)
  src/features/blog/hooks/useImageUpload.ts    (new)
  package.json                                 (TipTap added)

TESTS:
  Backend:  mvn clean verify → [N]/[N] passed ✅
  Frontend: npx playwright test → [N]/[N] passed ✅

Notes file updated:
  designs/[N]-notes.md → all notes marked ✅ Done

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Options:
  1️⃣  /review-page [N]    → run the full 9-layer review now
  2️⃣  /implement-notes [N+1] → process notes for the next page
  3️⃣  /review-progress   → see full project status
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

---

## Quick Reference

```
User creates:    designs/[N]-notes.md      (copy PAGE_NOTES_TEMPLATE.md)
User fills:      One note per issue found during manual testing
User runs:       /implement-notes [N]

Agent reads:     designs/[N]-notes.md
                 designs/[N].md + [N]-requirements.md
                 stack.md + users.md + DESIGN.md

Agent implements:
  BUG     → targeted code fix
  MISSING → full feature implementation
  DATA    → Flyway seed migration + endpoint verification
  EDITOR  → TipTap setup + toolbar + S3 upload wiring
  STYLE   → design token correction
  BEHAVIOR → business logic fix

After each note:  build check → smoke test → mark ✅ Done in notes file
After all notes:  test update → requirements update → final report

Next step:       /review-page [N]   (full 9-layer verification)
```

---

## When to Use This vs Other Commands

| Situation | Command |
|---|---|
| You know the task ID that failed | `/fix-task [id]` |
| You want a full audit of a page | `/review-page [N]` |
| You manually tested and found specific issues | `/implement-notes [N]` |
| You want to add a whole new feature | `/add-feature "..."` |
| Something is missing but you are not sure what | `/review-page [N]` |
| You have a written list of specific things to fix | `/implement-notes [N]` |