# Project Tasks

> This file is the master tracker for all pages and their development stages.

**Project**: MFRA Website
**Total pages**: 8 (analyzed) + 6 (pending spec)
**Created**: 29/03/2026
**Last updated**: 29/03/2026

---

## Summary Table

| # | Page Name | Design | Requirements | Backend | Frontend | Mobile |
|---|-----------|--------|--------------|---------|----------|--------|
| 1 | Admin Login | ✅ | ✅ | ⏳ Task 1.3 | ⏳ Task 1.5 | ⛔ |
| 2 | Admin Dashboard | ✅ | ✅ | ⏳ Task 2.1 | ⏳ Task 2.2 | ⛔ |
| 3 | Blog List | ✅ | ✅ | ⏳ Task 3.1 | ⏳ Task 3.3 | ⛔ |
| 4 | Blog Insights | ✅ | ✅ | ⏳ Task 3.5 | ⏳ Task 3.6 | ⛔ |
| 5 | Blog Editor | ✅ | ✅ | ⏳ Task 3.1 | ⏳ Task 3.4 | ⛔ |
| 6 | Case Study List | ✅ | ✅ | ⏳ Task 4.1 | ⏳ Task 4.3 | ⛔ |
| 7 | Case Study Insights | ✅ | ✅ | ⏳ Task 4.5 | ⏳ Task 4.6 | ⛔ |
| 8 | Case Study Editor | ✅ | ✅ | ⏳ Task 4.1 | ⏳ Task 4.4 | ⛔ |
| 9 | (Spec pending) | ✅ | ⚠️ | — | — | ⛔ |
| 10 | (Spec pending) | ✅ | ⚠️ | — | — | ⛔ |
| 11 | (Spec pending) | ✅ | ⚠️ | — | — | ⛔ |
| 12 | (Spec pending) | ✅ | ⚠️ | — | — | ⛔ |
| 13 | (Spec pending) | ✅ | ⚠️ | — | — | ⛔ |
| 14 | (Spec pending) | ✅ | ⚠️ | — | — | ⛔ |

---

## Page Details

---

## Page 1 — Admin Login

**Design**: designs/1.png
**Spec**: designs/1.md
**Requirements**: designs/1-requirements.md
**Analyzed on**: 29/03/2026 at 12:00
**Last update**: 29/03/2026 at 12:00

### Stage Status

| Stage | Status | Task ID | Notes |
|-------|--------|---------|-------|
| Design | ✅ Done | — | |
| Requirements | ✅ Done | — | designs/1-requirements.md |
| Backend | ⏳ Pending | 1.3 | Auth module — JWT login/logout/refresh |
| Frontend | ⏳ Pending | 1.5 | Login page + AuthGuard |
| Mobile | ⛔ Not in scope | — | |

### Sub-tasks

| ID | Platform | Description | Status | Depends On |
|----|----------|-------------|--------|------------|
| 1.3 | Backend | Auth — JWT login, logout, refresh, admin_users entity | ⏳ Pending | 1.2 |
| 1.5 | Frontend | Login page + AuthGuard + token management | ⏳ Pending | 1.3 |

---

## Page 2 — Admin Dashboard

**Design**: designs/2.png
**Spec**: designs/2.md
**Requirements**: designs/2-requirements.md
**Analyzed on**: 29/03/2026 at 12:00
**Last update**: 30/03/2026 at 18:00

### Stage Status

| Stage | Status | Task ID | Notes |
|-------|--------|---------|-------|
| Design | ✅ Done | — | |
| Requirements | ✅ Done | — | designs/2-requirements.md |
| Backend | ⏳ Pending | 2.1 | Dashboard APIs (summary, logs, perf, security) |
| Frontend | ⏳ Pending | 1.6 + 2.2 | Admin layout shell + dashboard page |
| Mobile | ⛔ Not in scope | — | |

### Sub-tasks

| ID | Platform | Description | Status | Depends On |
|----|----------|-------------|--------|------------|
| 1.6 | Frontend | Admin Layout Shell (sidebar + top bar) | ⏳ Pending | 1.5 |
| 2.1 | Backend | Dashboard summary, access logs, node perf, security APIs | ⏳ Pending | 1.3 |
| 2.2 | Frontend | Dashboard page — stat cards, logs, panels | ⏳ Pending | 1.6, 2.1 |

### Files Built from This Page

**Backend:**
(reset — will be filled again when task completes)

**Frontend:**
(reset — will be filled again when task completes)

---

## Page 3 — Blog List (Content Architecture)

**Design**: designs/3.png
**Spec**: designs/3.md
**Requirements**: designs/3-requirements.md
**Analyzed on**: 29/03/2026 at 12:00
**Last update**: 30/03/2026 at 18:00

### Stage Status

| Stage | Status | Task ID | Notes |
|-------|--------|---------|-------|
| Design | ✅ Done | — | |
| Requirements | ✅ Done | — | designs/3-requirements.md |
| Backend | ⏳ Pending | 3.1 | Blog entities + admin CRUD |
| Frontend | ⏳ Pending | 3.3 | Blog list page |
| Mobile | ⛔ Not in scope | — | |

### Sub-tasks

| ID | Platform | Description | Status | Depends On |
|----|----------|-------------|--------|------------|
| 3.1 | Backend | Blog entities, repos, services, admin controllers | ⏳ Pending | 1.3, 1.4 |
| 3.3 | Frontend | Blog list page — table, filters, pagination, insight cards | ⏳ Pending | 1.6, 3.1 |

### Files Built from This Page

**Backend:**
(reset — will be filled again when task completes)

**Frontend:**
(reset — will be filled again when task completes)

---

## Page 4 — Blog Article Insights

**Design**: designs/4.png
**Spec**: designs/4.md
**Requirements**: designs/4-requirements.md
**Analyzed on**: 29/03/2026 at 12:00
**Last update**: 29/03/2026 at 12:00

### Stage Status

| Stage | Status | Task ID | Notes |
|-------|--------|---------|-------|
| Design | ✅ Done | — | |
| Requirements | ✅ Done | — | designs/4-requirements.md |
| Backend | ⏳ Pending | 3.5 | Blog insights APIs + page views |
| Frontend | ⏳ Pending | 3.6 | Blog insights page — KPIs, map, chart |
| Mobile | ⛔ Not in scope | — | |

### Sub-tasks

| ID | Platform | Description | Status | Depends On |
|----|----------|-------------|--------|------------|
| 3.5 | Backend | Blog insights — summary, geo, access log, engagement APIs | ⏳ Pending | 3.1 |
| 3.6 | Frontend | Blog insights page — KPIs, map, access log, chart | ⏳ Pending | 3.3, 3.5 |

---

## Page 5 — Blog Post Editor

**Design**: designs/5.png
**Spec**: designs/5.md
**Requirements**: designs/5-requirements.md
**Analyzed on**: 29/03/2026 at 12:00
**Last update**: 29/03/2026 at 12:00

### Stage Status

| Stage | Status | Task ID | Notes |
|-------|--------|---------|-------|
| Design | ✅ Done | — | |
| Requirements | ✅ Done | — | designs/5-requirements.md |
| Backend | ⏳ Pending | 3.1 | Shared with page 3 (same blog CRUD) |
| Frontend | ⏳ Pending | 3.4 | Blog editor — TipTap, auto-save, media |
| Mobile | ⛔ Not in scope | — | |

### Sub-tasks

| ID | Platform | Description | Status | Depends On |
|----|----------|-------------|--------|------------|
| 3.1 | Backend | (shared with page 3) | ⏳ Pending | 1.3, 1.4 |
| 3.4 | Frontend | Blog editor — TipTap, auto-save, media, publish settings | ⏳ Pending | 3.3 |

---

## Page 6 — Case Study List

**Design**: designs/6.png
**Spec**: designs/6.md
**Requirements**: designs/6-requirements.md
**Analyzed on**: 29/03/2026 at 12:00
**Last update**: 29/03/2026 at 12:00

### Stage Status

| Stage | Status | Task ID | Notes |
|-------|--------|---------|-------|
| Design | ✅ Done | — | |
| Requirements | ✅ Done | — | designs/6-requirements.md |
| Backend | ⏳ Pending | 4.1 | Case study entities + admin CRUD |
| Frontend | ⏳ Pending | 4.3 | Case study list page |
| Mobile | ⛔ Not in scope | — | |

### Sub-tasks

| ID | Platform | Description | Status | Depends On |
|----|----------|-------------|--------|------------|
| 4.1 | Backend | Case study entities, repos, services, admin controllers | ⏳ Pending | 1.3, 1.4 |
| 4.3 | Frontend | Case study list — table, filters, bulk actions, featured | ⏳ Pending | 1.6, 4.1 |

---

## Page 7 — Case Study Insights

**Design**: designs/7.png
**Spec**: designs/7.md
**Requirements**: designs/7-requirements.md
**Analyzed on**: 29/03/2026 at 12:00
**Last update**: 29/03/2026 at 12:00

### Stage Status

| Stage | Status | Task ID | Notes |
|-------|--------|---------|-------|
| Design | ✅ Done | — | |
| Requirements | ✅ Done | — | designs/7-requirements.md |
| Backend | ⏳ Pending | 4.5 | CS insights + export + share |
| Frontend | ⏳ Pending | 4.6 | CS insights page — chart, geo, logs |
| Mobile | ⛔ Not in scope | — | |

### Sub-tasks

| ID | Platform | Description | Status | Depends On |
|----|----------|-------------|--------|------------|
| 4.5 | Backend | CS insights — summary, engagement, geo, logs, PDF, share | ⏳ Pending | 4.1, 3.5 |
| 4.6 | Frontend | CS insights page — bar chart, geo reach, access logs, export | ⏳ Pending | 4.3, 4.5 |

---

## Page 8 — Case Study Editor

**Design**: designs/8.png
**Spec**: designs/8.md
**Requirements**: designs/8-requirements.md
**Analyzed on**: 29/03/2026 at 12:00
**Last update**: 29/03/2026 at 12:00

### Stage Status

| Stage | Status | Task ID | Notes |
|-------|--------|---------|-------|
| Design | ✅ Done | — | |
| Requirements | ✅ Done | — | designs/8-requirements.md |
| Backend | ⏳ Pending | 4.1 | Shared with page 6 (same CS CRUD) |
| Frontend | ⏳ Pending | 4.4 | CS editor — TipTap, results, metadata, diagram |
| Mobile | ⛔ Not in scope | — | |

### Sub-tasks

| ID | Platform | Description | Status | Depends On |
|----|----------|-------------|--------|------------|
| 4.1 | Backend | (shared with page 6) | ⏳ Pending | 1.3, 1.4 |
| 4.4 | Frontend | CS editor — TipTap, results, metadata, diagram upload | ⏳ Pending | 4.3 |
