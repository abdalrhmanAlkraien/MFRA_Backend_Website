# /generate-spec [module]

> **Slash command for OpenClaw / Claude Code**
> Triggered when user types: `/generate-spec blog` or `/generate-spec`
> Bridges the gap between extracted requirements and SpecKit spec files.
> Reads `designs/[N]-requirements.md` for a module and generates the full
> SpecKit spec, plan, and tasks in `specs/<module>/`.

---

## What This Command Does

Takes a module name (e.g. `blog`, `case-studies`, `consultation`), finds
all requirements files that belong to that module, and produces complete
SpecKit-compatible spec files:

```
specs/<module>/spec.md     ← acceptance criteria derived from requirements
specs/<module>/plan.md     ← implementation decisions and approach
specs/<module>/tasks.md    ← task breakdown ready for OpenClaw execution
```

This command is the bridge between design analysis and code execution.
Run it after `/analyze-designs` or `/analyze-page` — before `/execute-task`.

---

## Execution Steps

### Step 1 — Identify the Target Module

If user typed `/generate-spec blog` → use module `blog`.

If user typed `/generate-spec` with no module:

```
📋 WHICH MODULE TO GENERATE SPEC FOR?
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Modules with requirements ready (no spec yet):

  blog          → 2 pages analyzed (4.md, 5.md)
  case-studies  → 2 pages analyzed (6.md, 7.md)
  consultation  → 1 page analyzed  (11.md)
  contact       → 1 page analyzed  (12.md)

Modules already specced:
  auth          → specs/auth/ ✅

Type a module name, or "all" to generate specs for every module:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

If user types "all" → run this command for every unspecced module in sequence.

---

### Step 2 — Read Context

```
Read: .claude/project/stack.md          → platforms and constraints
Read: .claude/project/users.md          → roles and permissions
Read: .claude/CLAUDE.md                 → module list, architecture rules
Read: .claude/instructions/backend.md   → coding standards for spec decisions
Read: .claude/instructions/database.md  → DB conventions for spec decisions
```

---

### Step 3 — Find All Requirements for This Module

```
Scan: designs/
  For each [N]-requirements.md:
    Read the MODULE field at the top
    If MODULE matches target → add to spec source list

Source list example for module "blog":
  designs/4-requirements.md  → Blog List page
  designs/5-requirements.md  → Blog Article page
  designs/13-requirements.md → Admin Blog List page
  designs/14-requirements.md → Admin Blog Editor page
```

If no requirements files found for this module:
```
⚠️  No requirements found for module: [module]
Run /analyze-page [N] first for the pages in this module.
```

---

### Step 4 — Check If Spec Already Exists

```
Does specs/[module]/ already exist?

  YES → Ask user:
    ⚠️  specs/[module]/ already exists.

    Options:
      A) Regenerate — overwrite existing spec
      B) Show existing spec — no changes
      C) Cancel

  NO → Create specs/[module]/ and proceed
```

---

### Step 5 — Generate spec.md

`specs/<module>/spec.md` — the acceptance criteria document.

```markdown
# Spec: [Module Name]

**Module**: [module]
**Generated**: DD/MM/YYYY at HH:MM
**Source pages**: [N]-requirements.md, [N]-requirements.md, ...
**Platforms**: Backend ✅ | Frontend ✅ | Mobile ⛔

---

## Overview

[2-3 sentence description of what this module does,
 derived from the Business Description fields in the source .md files]

---

## Database Schema

[All new tables from requirements, combined and deduplicated]

### Table: <table_name>
| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | UUID | PK, default gen_random_uuid() | |
| [field] | [type] | [NOT NULL / UNIQUE / FK] | |
| ... | | | |
| created_at | TIMESTAMPTZ | NOT NULL DEFAULT now() | |
| updated_at | TIMESTAMPTZ | NOT NULL DEFAULT now() | |
| deleted_at | TIMESTAMPTZ | | soft delete |

**Flyway file**: VX__create_[module]_tables.sql

---

## API Endpoints

### Public Endpoints (no auth required)
| Method | Path | Description | Cached | Rate Limited |
|--------|------|-------------|--------|--------------|
| GET | /api/public/[resource] | Paged list | Yes 10min | No |
| GET | /api/public/[resource]/{slug} | Single item | Yes 30min | No |

### Admin Endpoints (JWT required)
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | /api/admin/[resource] | ADMIN+EDITOR | Paged list with filters |
| POST | /api/admin/[resource] | ADMIN+EDITOR | Create |
| PUT | /api/admin/[resource]/{id} | ADMIN+EDITOR | Update |
| DELETE | /api/admin/[resource]/{id} | ADMIN | Delete (soft) |
| PATCH | /api/admin/[resource]/{id}/publish | ADMIN+EDITOR | Publish |

---

## Acceptance Criteria

[One criterion per requirement extracted from requirements files]
[Each criterion becomes at least one test scenario]

### Backend Criteria
- [ ] [AC-1] [criterion from requirements]
- [ ] [AC-2] [criterion]
- [ ] [AC-3] Draft items never returned by public API
- [ ] [AC-4] Soft delete — deleted_at set, record not removed
- [ ] [AC-5] Slug auto-generated from title, unique collision handling

### Frontend Criteria
- [ ] [AC-6] [UI criterion from design]
- [ ] [AC-7] Loading state shown during API call
- [ ] [AC-8] Empty state shown when no data
- [ ] [AC-9] Error state shown when API fails
- [ ] [AC-10] All admin routes behind AuthGuard

### Security Criteria (mandatory for every module)
- [ ] [AC-S1] Unauthenticated request to admin endpoint → 401
- [ ] [AC-S2] EDITOR accessing ADMIN-only endpoint → 403
- [ ] [AC-S3] Invalid or expired token → 401

---

## Business Rules

[All rules extracted from requirements files, combined]

1. [Rule from requirements]
2. [Rule from requirements]
3. Only PUBLISHED items returned by public API
4. Soft delete mandatory — no hard DELETE operations
5. [Module-specific rule]

---

## Edge Cases

[All edge cases from requirements + standard edge cases for this type of module]

1. Slug collision → append -1, -2, etc. until unique
2. Empty list → return empty array, not 404
3. Unknown slug in public API → 404
4. Delete already-deleted item → 404
5. [Module-specific edge case]

---

## Out of Scope

[Items marked as out of scope in any source .md file]

- [Item explicitly excluded]
- [Item deferred to future version]
```

---

### Step 6 — Generate plan.md

`specs/<module>/plan.md` — implementation decisions and approach.

```markdown
# Plan: [Module Name]

**Module**: [module]
**Generated**: DD/MM/YYYY at HH:MM

---

## Implementation Approach

### Package Structure
```
src/main/java/com/<pkg>/<module>/
entity/        ← JPA entities extending BaseEntity
repository/    ← Spring Data repositories
service/       ← Business logic + @Transactional
controller/    ← REST controllers (Admin + Public split)
dto/           ← Request/Response DTOs
mapper/        ← MapStruct mappers
```

### Layer Decisions
- **Slug generation**: In service layer, auto-generated — never user-entered
- **Reading time**: Calculated in service layer from word count (200 wpm)
- **Cache strategy**: [@Cacheable on public GET endpoints, invalidate on write]
- **Async**: [Email sends wrapped in @Async if email triggered by this module]
- **Auth**: All admin endpoints use @PreAuthorize per users.md rules

### Key Service Methods
- `create(request)` → validate → generate slug → save → invalidate cache
- `update(id, request)` → find + validate ownership → update → save → invalidate
- `delete(id)` → soft delete (setDeletedAt) → invalidate cache
- `publish(id)` → set status + publishedAt → invalidate public cache

### Frontend Approach
- **State**: RTK Query — createApi with tagTypes for cache invalidation
- **Forms**: react-hook-form + zod validation
- **Rich text**: [TipTap if editor is needed, plain textarea if not]
- **Admin route guard**: All admin pages wrapped in AuthGuard

---

## Cache Keys

| Key Pattern | TTL | Invalidated By |
|-------------|-----|----------------|
| [module]:public:list:{hash} | 10 min | create, update, delete, publish |
| [module]:public:{slug} | 30 min | update, delete, publish |

---

## Dependencies

- Depends on: auth module (JWT), upload module (if file fields present)
- Required config: Cache (Redis), [Email if email triggered]
```

---

### Step 7 — Generate tasks.md

`specs/<module>/tasks.md` — task breakdown for OpenClaw.

```markdown
# Tasks: [Module Name]

**Module**: [module]
**Generated**: DD/MM/YYYY at HH:MM
**Total tasks**: [N]

---

## Task [X.1] — Backend: [Module] — Entity + Repository + Service + Controllers

**Platform**: Backend
**Complexity**: [Simple / Medium / Complex]
**Estimated cost**: $[X.XX]
**Dependencies**: Task 1.1 (auth), Task 1.2 (DB setup)

**Delivers**:
- Flyway migration: VX__create_[module]_tables.sql
- [Module]Entity.java (extends BaseEntity)
- [Module]Repository.java (all queries with DeletedAtIsNull)
- [Module]Service.java (@Transactional, slug gen, cache invalidation)
- [Module]AdminController.java (CRUD + publish)
- [Module]PublicController.java (read-only, published-only)
- DTOs: [Module]CreateRequest, [Module]UpdateRequest, [Module]Response

**API endpoints covered**:
- [list all endpoints from spec.md]

**Acceptance criteria covered**:
- [AC-1], [AC-2], [AC-3], [AC-4], [AC-5], [AC-S1], [AC-S2], [AC-S3]

**Test scenarios**:
- Backend: [N] scenarios (security + validation + happy path + edge cases)

---

## Task [X.2] — Frontend: [Module] — Pages + Components + RTK Query

**Platform**: Frontend
**Complexity**: [Simple / Medium / Complex]
**Estimated cost**: $[X.XX]
**Dependencies**: Task [X.1]

**Delivers**:
- features/[module]/api.ts (RTK Query slice)
- features/[module]/types.ts
- features/[module]/pages/[Module]ListPage.tsx
- features/[module]/pages/[Module]DetailPage.tsx (if detail page exists)
- features/[module]/pages/Admin[Module]ListPage.tsx
- features/[module]/pages/Admin[Module]EditorPage.tsx (if editor exists)
- features/[module]/components/[Module]Card.tsx
- features/[module]/components/[Module]Filter.tsx (if filters exist)

**Acceptance criteria covered**:
- [AC-6], [AC-7], [AC-8], [AC-9], [AC-10]

**Test scenarios**:
- Frontend: [N] Playwright scenarios
```

---

### Step 8 — Cross-Check With systemTasks.md

After generating spec files, verify task IDs match:

```
Cross-check: specs/[module]/tasks.md tasks IDs
vs .claude/systemTasks.md task IDs for this module

Match? → All good
Mismatch? → Update systemTasks.md task descriptions to match spec
```

---

### Step 9 — Present Results

```
✅ SPEC GENERATED — [Module Name]
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Files created:
  ✅ specs/[module]/spec.md     → [N] acceptance criteria
  ✅ specs/[module]/plan.md     → implementation decisions
  ✅ specs/[module]/tasks.md    → [N] tasks defined

Database:
  Tables:     [N] tables
  Migration:  VX__create_[module]_tables.sql

API endpoints:
  Public:     [N] endpoints
  Admin:      [N] endpoints

Tasks:
  Backend:    Task [X.1] (Complexity: [level])
  Frontend:   Task [X.2] (Complexity: [level])
  Mobile:     ⛔ not in scope

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Options:
  1️⃣  /generate-spec [next-module]  → spec next module
  2️⃣  /execute-task                 → start building Task [X.1]
  3️⃣  /review-progress              → see full project status
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

---

## Quick Reference

```
User types:      /generate-spec [module]  or  /generate-spec all
Agent reads:     designs/[N]-requirements.md (all for this module)
                 stack.md + users.md + CLAUDE.md
                 instructions/backend.md + database.md
Agent writes:    specs/[module]/spec.md
                 specs/[module]/plan.md
                 specs/[module]/tasks.md
Agent checks:    systemTasks.md task IDs match spec task IDs
Agent presents:  Spec summary + next options
Prerequisite:    /analyze-page or /analyze-designs must run first
Next step:       /execute-task
```