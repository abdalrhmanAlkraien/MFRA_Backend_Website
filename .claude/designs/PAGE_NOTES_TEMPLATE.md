# Page Notes Template

> Copy this file as `designs/[N]-notes.md` for each page you are reviewing.
> Fill in the notes after manually testing the page.
> Then run: `/implement-notes [N]`
> The agent reads this file, implements everything listed, and marks each
> item as done when complete.

---

# Page Notes: [N] — [Page Name]

**Page**: designs/[N].png
**Tested by**: [your name or "manual review"]
**Tested on**: DD/MM/YYYY
**Status**: ⏳ Pending implementation

---

## How to Write Notes

Each note is one specific thing that is wrong, missing, or needs changing.
Write it clearly so the agent knows exactly what to build.

**Good note:**
```
The "Create Post" button navigates to /admin/blogs/new but the page shows
a blank white screen. The create form is not implemented. Need: title field,
content editor (TipTap), category dropdown loaded from API, publish button.
```

**Bad note (too vague):**
```
Create doesn't work
```

Use these categories to group your notes:

- `[BUG]` — something that was implemented but is broken
- `[MISSING]` — a feature or UI element that was never built
- `[DATA]` — seed data, dropdown values, or database content needed
- `[EDITOR]` — rich text editor, upload, or input behavior
- `[STYLE]` — visual issue that does not match the design
- `[BEHAVIOR]` — business logic or user flow that works differently than expected

---

## Notes

### Note 1
**Category**: [BUG / MISSING / DATA / EDITOR / STYLE / BEHAVIOR]
**Priority**: [High / Medium / Low]
**Status**: ⏳ Pending

**Problem**:
[Describe exactly what is wrong or missing. Be specific.]

**Expected behavior**:
[Describe exactly what should happen instead.]

**Scope**:
- [ ] Backend change needed
- [ ] Frontend change needed
- [ ] Database / seed data needed
- [ ] Test update needed

---

### Note 2
**Category**:
**Priority**:
**Status**: ⏳ Pending

**Problem**:

**Expected behavior**:

**Scope**:
- [ ] Backend change needed
- [ ] Frontend change needed
- [ ] Database / seed data needed
- [ ] Test update needed

---

### Note 3
[Add as many notes as needed — one per issue]

---

## After Implementation

The agent fills this in automatically after implementing each note:

| # | Category | Problem | Status | Files Changed |
|---|---|---|---|---|
| 1 | | | ⏳ | — |
| 2 | | | ⏳ | — |
| 3 | | | ⏳ | — |