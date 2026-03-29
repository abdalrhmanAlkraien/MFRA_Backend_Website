# Project Tasks

> This file is the master tracker for all pages and their development stages.
> The AI agent updates this file after analyzing each design and after
> completing each development task.
> Never edit task statuses manually — the agent manages this file.

**Project**: MFRA Website
**Total pages**: 0
**Created**: [date]
**Last updated**: [date]

---

## How This File Works

Each page goes through these stages in order:

```
Design         → You drop the image file into designs/
Requirements   → Agent reads image + .md file → generates requirements doc
Backend        → Agent implements API + DB + tests → fills "Files Built"
Frontend       → Agent implements UI + components + tests → fills "Files Built"
Mobile         → Agent implements screens + widgets + tests (if in stack.md)
```

The agent updates each stage status as work progresses.
The "Files Built from This Page" section is filled by the agent after each task
completes. This section is critical — it is the scope boundary used when the
design image is updated and the page needs to be re-analyzed.

---

## Status Legend

| Symbol | Meaning |
|--------|---------|
| ✅ Done | Completed and verified |
| ⏳ Pending | Not started yet |
| 🔄 In Progress | Currently being built |
| ❌ Failed | Attempted and failed — run /fix-task [id] |
| ⚠️ Needs Review | Stopped — requires your decision |
| ⛔ Not in scope | Not applicable for this project |
| 🔒 Blocked | Waiting for another task to complete first |

---

## Summary Table

| # | Page Name | Design | Requirements | Backend | Frontend | Mobile |
|---|-----------|--------|--------------|---------|----------|--------|
| — | No pages analyzed yet | — | — | — | — | — |

*This table is updated by the agent after each page is analyzed.*

---

## Page Details

*One section per page is added here by the agent after analysis.*

---

### How to Add a New Page

1. Drop your image into `designs/public/` or `designs/admin/`
    - Name it with a number: `1.png`, `2.png`, `3.png`

2. Create a .md file with the same number: `1.md`
    - Copy from `designs/PAGE_TEMPLATE.md`
    - Fill in: business description, access, permissions, related pages, notes

3. Tell the agent:
   ```
   Analyze designs/1.png
   ```
   The agent reads both files, extracts requirements, and generates tasks.

4. This file is updated automatically by the agent.

---

### Example Page Entry (filled by agent after analysis)

```markdown
## Page 1 — Blog List

**Design**:       designs/1.png
**Spec**:         designs/1.md
**Requirements**: designs/1-requirements.md
**Analyzed on**:  15/10/2025 at 10:00 PM
**Last update**:  23/03/2026 at 12:00 PM

### Stage Status

| Stage | Status | Task ID | Completed On | Notes |
|-------|--------|---------|--------------|-------|
| Design | ✅ Done | — | 15/10/2025 | |
| Requirements | ✅ Done | — | 15/10/2025 | designs/1-requirements.md |
| Backend | ✅ Done | 2.1 | 20/10/2025 | 8/8 tests passing |
| Frontend | 🔄 In Progress | 2.2 | — | Building now |
| Mobile | ⛔ Not in scope | — | — | Flutter not in stack.md |

### Sub-tasks

| ID | Platform | Description | Status | Depends On | Completed |
|----|----------|-------------|--------|------------|-----------|
| 2.1 | Backend | Blog entity + repo + service + controllers | ✅ Done | 1.3 | 20/10/2025 |
| 2.2 | Frontend | BlogPage + BlogCard + BlogFilter + api.ts | 🔄 In Progress | 2.1 | — |
| 2.3 | Mobile | — | ⛔ Not in scope | — | — |

### Files Built from This Page

> Filled by agent after each task completes.
> This is the scope boundary for /analyze-page 1 if the design is updated.

**Backend** (Task 2.1):
  src/main/java/com/<pkg>/blog/entity/BlogEntity.java
  src/main/java/com/<pkg>/blog/repository/BlogRepository.java
  src/main/java/com/<pkg>/blog/service/BlogService.java
  src/main/java/com/<pkg>/blog/controller/BlogAdminController.java
  src/main/java/com/<pkg>/blog/controller/BlogPublicController.java
  src/main/java/com/<pkg>/blog/dto/BlogCreateRequest.java
  src/main/java/com/<pkg>/blog/dto/BlogResponse.java

**Frontend** (Task 2.2):
  (in progress)

**Database migrations**:
  src/main/resources/db/migration/V3__create_blog_tables.sql

**Tests**:
  src/test/java/com/<pkg>/blog/BlogControllerTest.java
  tests/e2e/blog/blog-list.spec.ts