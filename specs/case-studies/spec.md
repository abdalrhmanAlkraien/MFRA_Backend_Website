# Spec: Case Studies Module

**Module**: case-studies
**Generated**: 29/03/2026 at 14:00
**Source pages**: 6-requirements.md, 7-requirements.md, 8-requirements.md
**Platforms**: Backend ✅ | Frontend ✅ | Mobile ⛔

---

## Overview

The Case Studies module provides full CRUD for case study posts with categories, tags, result metrics, before/after comparisons, and architecture diagrams. It includes an admin interface with rich text editor (TipTap), auto-save, featured management, bulk actions, and per-case-study analytics (insights). Insights include PDF export and shareable report links. Public endpoints serve published-only content with Redis caching.

---

## Database Schema

### Table: case_study_categories
| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | UUID | PK, default gen_random_uuid() | |
| name | VARCHAR(100) | NOT NULL | |
| slug | VARCHAR(100) | NOT NULL, UNIQUE (partial) | Auto-generated |
| created_at | TIMESTAMPTZ | NOT NULL DEFAULT now() | |
| updated_at | TIMESTAMPTZ | NOT NULL DEFAULT now() | |
| created_by | UUID | | |
| updated_by | UUID | | |
| deleted_at | TIMESTAMPTZ | | Soft delete |

### Table: case_study_tags
| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | UUID | PK, default gen_random_uuid() | |
| name | VARCHAR(100) | NOT NULL | |
| slug | VARCHAR(100) | NOT NULL, UNIQUE (partial) | Auto-generated |
| created_at | TIMESTAMPTZ | NOT NULL DEFAULT now() | |
| updated_at | TIMESTAMPTZ | NOT NULL DEFAULT now() | |
| deleted_at | TIMESTAMPTZ | | Soft delete |

### Table: case_studies
| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | UUID | PK, default gen_random_uuid() | |
| title | VARCHAR(255) | NOT NULL | |
| slug | VARCHAR(255) | NOT NULL, UNIQUE (partial) | Auto-generated from title |
| content | TEXT | | Rich text (HTML from TipTap) |
| content_type | VARCHAR(50) | NOT NULL, DEFAULT 'CASE_STUDY' | Read-only in editor |
| status | VARCHAR(50) | NOT NULL, DEFAULT 'DRAFT', CHECK IN ('DRAFT','PUBLISHED','ARCHIVED') | |
| client_name | VARCHAR(150) | | Supports anonymized text |
| industry | VARCHAR(100) | | Required before publish |
| country | VARCHAR(100) | | |
| city | VARCHAR(100) | | |
| cover_image_url | VARCHAR(500) | | S3 URL |
| architecture_diagram_url | VARCHAR(500) | | S3 URL, max 10MB |
| is_featured | BOOLEAN | NOT NULL, DEFAULT false | Only one at a time |
| display_order | INT | NOT NULL, DEFAULT 0 | |
| display_priority | INT | NOT NULL, DEFAULT 1, CHECK BETWEEN 1 AND 10 | 1=highest |
| reading_time_min | INT | | Auto-calculated |
| word_count | INT | | Auto-calculated |
| published_at | TIMESTAMPTZ | | Set on publish |
| category_id | UUID | FK → case_study_categories(id) | |
| created_at | TIMESTAMPTZ | NOT NULL DEFAULT now() | |
| updated_at | TIMESTAMPTZ | NOT NULL DEFAULT now() | |
| created_by | UUID | | |
| updated_by | UUID | | |
| deleted_at | TIMESTAMPTZ | | Soft delete |

### Table: case_study_tag_map
| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| case_study_id | UUID | FK → case_studies(id), ON DELETE CASCADE | |
| tag_id | UUID | FK → case_study_tags(id), ON DELETE CASCADE | |
| | | PRIMARY KEY (case_study_id, tag_id) | Composite PK |

### Table: case_study_results
| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | UUID | PK, default gen_random_uuid() | |
| case_study_id | UUID | NOT NULL, FK → case_studies(id) | |
| label | VARCHAR(100) | NOT NULL | e.g. "Cost Reduction" |
| value | VARCHAR(50) | NOT NULL | e.g. "47%" |
| description | VARCHAR(200) | | e.g. "Annual infrastructure costs" |
| color_variant | VARCHAR(20) | NOT NULL, DEFAULT 'PRIMARY', CHECK IN ('PRIMARY','SECONDARY') | |
| display_order | INT | NOT NULL, DEFAULT 0 | |
| created_at | TIMESTAMPTZ | NOT NULL DEFAULT now() | |
| updated_at | TIMESTAMPTZ | NOT NULL DEFAULT now() | |
| deleted_at | TIMESTAMPTZ | | Soft delete |

### Table: case_study_comparisons
| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | UUID | PK, default gen_random_uuid() | |
| case_study_id | UUID | NOT NULL, FK → case_studies(id) | |
| metric | VARCHAR(150) | NOT NULL | e.g. "Deployment Time" |
| legacy_value | VARCHAR(100) | NOT NULL | e.g. "2-3 weeks" |
| modernized_value | VARCHAR(100) | NOT NULL | e.g. "15 minutes" |
| display_order | INT | NOT NULL, DEFAULT 0 | |
| created_at | TIMESTAMPTZ | NOT NULL DEFAULT now() | |
| updated_at | TIMESTAMPTZ | NOT NULL DEFAULT now() | |
| deleted_at | TIMESTAMPTZ | | Soft delete |

### Table: insight_shares
| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | UUID | PK, default gen_random_uuid() | |
| resource_type | VARCHAR(50) | NOT NULL, CHECK IN ('BLOG','CASE_STUDY') | Shared with blog module |
| resource_id | UUID | NOT NULL | |
| share_token | VARCHAR(255) | NOT NULL, UNIQUE | Random secure token |
| expires_at | TIMESTAMPTZ | NOT NULL | 7 days from creation |
| created_by | UUID | FK → admin_users(id) | |
| created_at | TIMESTAMPTZ | NOT NULL DEFAULT now() | |

**Flyway files**: V4__create_case_study_tables.sql, V7__create_insight_shares_table.sql

---

## API Endpoints

### Public Endpoints (no auth required)
| Method | Path | Description | Cached | Rate Limited |
|--------|------|-------------|--------|--------------|
| GET | /api/public/case-studies | Paginated published case studies | Yes 10min | No |
| GET | /api/public/case-studies/{slug} | Single published case study (with results + comparisons) | Yes 30min | No |
| GET | /api/public/case-study-categories | All categories | Yes 1hr | No |

### Admin Endpoints (JWT required)
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | /api/admin/case-studies | ADMIN+EDITOR | Paginated list with filters + search |
| GET | /api/admin/case-studies/{id} | ADMIN+EDITOR | Single case study for editing |
| POST | /api/admin/case-studies | ADMIN+EDITOR | Create new |
| PUT | /api/admin/case-studies/{id} | ADMIN+EDITOR | Full update |
| PATCH | /api/admin/case-studies/{id} | ADMIN+EDITOR | Auto-save partial update |
| PATCH | /api/admin/case-studies/{id}/publish | ADMIN+EDITOR | Publish |
| PATCH | /api/admin/case-studies/{id}/unpublish | ADMIN+EDITOR | Unpublish |
| PATCH | /api/admin/case-studies/{id}/feature | ADMIN | Toggle featured |
| PATCH | /api/admin/case-studies/{id}/results | ADMIN+EDITOR | Save results + comparisons |
| DELETE | /api/admin/case-studies/{id} | ADMIN | Soft delete |
| POST | /api/admin/case-studies/bulk | ADMIN | Bulk action (publish/unpublish/archive/delete) |
| GET | /api/admin/case-study-categories | ADMIN+EDITOR | Category options |
| GET | /api/admin/case-study-industries | ADMIN+EDITOR | Distinct industry values |
| GET | /api/admin/tags?type=case-study | ADMIN+EDITOR | Tag autocomplete |
| POST | /api/admin/tags | ADMIN+EDITOR | Create new tag |

### Insights Endpoints (JWT required)
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | /api/admin/case-studies/{id}/insights/summary | ADMIN+EDITOR | 4 KPIs |
| GET | /api/admin/case-studies/{id}/insights/engagement | ADMIN+EDITOR | Bar chart data |
| GET | /api/admin/case-studies/{id}/insights/geo | ADMIN+EDITOR | Map + countries |
| GET | /api/admin/case-studies/{id}/insights/access-log | ADMIN+EDITOR | Access log table |
| POST | /api/admin/case-studies/{id}/insights/export-pdf | ADMIN+EDITOR | Generate PDF report |
| POST | /api/admin/case-studies/{id}/insights/share | ADMIN+EDITOR | Create shareable link |

---

## Acceptance Criteria

### Backend Criteria
- [ ] [AC-1] POST creates case study with status DRAFT
- [ ] [AC-2] Slug auto-generated from title — unique collision handling
- [ ] [AC-3] Reading time auto-calculated from word count (200 wpm)
- [ ] [AC-4] Only one case study can be featured at a time
- [ ] [AC-5] PATCH supports partial update for auto-save
- [ ] [AC-6] Title, content, category, industry, and all 3 metric cards required before publish
- [ ] [AC-7] Comparison table rows: all columns per row must be filled (table itself optional)
- [ ] [AC-8] DELETE soft-deletes — sets deletedAt
- [ ] [AC-9] Public API returns PUBLISHED only
- [ ] [AC-10] Public list cached in Redis (10 min), single cached (30 min)
- [ ] [AC-11] Cache invalidated on create, update, delete, publish
- [ ] [AC-12] EDITOR cannot delete, feature, set priority, or use bulk actions — 403
- [ ] [AC-13] PATCH /results saves metric_cards + comparison_rows atomically
- [ ] [AC-14] Bulk action processes multiple IDs in single request
- [ ] [AC-15] Architecture diagram upload max 10 MB (S3 folder: case-studies/diagrams/)
- [ ] [AC-16] Content type defaults to "CASE_STUDY" — read-only
- [ ] [AC-17] Tags shared across case studies
- [ ] [AC-18] Insights summary returns total_views, avg_read_time, conversion_rate, share_count
- [ ] [AC-19] Insights engagement returns daily bar chart data
- [ ] [AC-20] Insights geo returns top 5 countries with progress bars
- [ ] [AC-21] Insights access-log returns 10 entries with AUTHENTICATED/GUEST status
- [ ] [AC-22] Export PDF generated server-side on demand
- [ ] [AC-23] Share link generates unique token, expires after 7 days
- [ ] [AC-24] Performance labels: > 5% HIGH PERFORMING, 2-5% standard, < 2% NEEDS ATTENTION

### Frontend Criteria
- [ ] [AC-25] Case study list with search + multi-filter (status, industry, category)
- [ ] [AC-26] Bulk action bar visible for ADMIN only
- [ ] [AC-27] Featured star toggle with confirmation
- [ ] [AC-28] URL reflects filter state for bookmarking
- [ ] [AC-29] Case study editor with TipTap, result metric cards (3 fixed), comparison table
- [ ] [AC-30] Project metadata: industry, customer, country, city, tags
- [ ] [AC-31] Architecture diagram upload (max 10 MB)
- [ ] [AC-32] Auto-save: debounce 3 seconds, minimum 60-second interval, elapsed time display
- [ ] [AC-33] Insights page: bar chart, geo map, access log, export + share buttons
- [ ] [AC-34] Continent color coding: Americas=red, Europe=blue, Asia-Pacific=green, MENA=amber
- [ ] [AC-35] Loading, error, and empty states on every component
- [ ] [AC-36] All interactive elements have data-testid attributes
- [ ] [AC-37] EDITOR: delete icon hidden, featured hidden, bulk actions hidden, priority hidden

### Security Criteria
- [ ] [AC-S1] Unauthenticated request to admin endpoints → 401
- [ ] [AC-S2] EDITOR accessing ADMIN-only endpoint → 403
- [ ] [AC-S3] Invalid or expired token → 401

---

## Business Rules

1. Only PUBLISHED case studies visible on public website
2. Only one featured at a time — backend enforces
3. EDITOR cannot delete, feature, or use bulk actions
4. Filtering + search server-side
5. URL reflects filter state for bookmarking
6. Insights icon only for PUBLISHED items
7. Soft delete only
8. Default page size: 15
9. Bulk actions ADMIN only
10. Title, content, category, industry required before publish
11. All 3 metric cards must be filled before publish
12. Comparison table: all columns per row required (table optional)
13. Architecture diagram optional, max 10 MB
14. Content type defaults to "CASE_STUDY" — read-only in editor
15. Auto-save: debounce 3s, min 60s interval
16. Tags shared across case studies
17. Customer name supports anonymized text
18. Share link expires after 7 days
19. Export PDF generated server-side on demand
20. Performance labels: > 5% HIGH PERFORMING, 2-5% standard, < 2% NEEDS ATTENTION
21. Bar chart defaults to Daily
22. Geographic shows top 5 countries
23. Access log shows 10 entries

---

## Edge Cases

1. Slug collision → append -1, -2, etc.
2. Empty list → return empty array, not 404
3. Unknown slug in public API → 404
4. Delete already-deleted → 404
5. Publish without required fields → 400
6. Feature when another is featured → un-feature previous
7. Bulk action with mix of valid/invalid IDs → process valid, report invalid
8. Results PATCH with < 3 metric cards → 400
9. Comparison row with missing column → 400
10. Architecture diagram > 10 MB → 400
11. Share link accessed after expiry → 410 Gone
12. Export PDF for draft → 404
13. Insights for DRAFT case study → 404 or redirect

---

## Out of Scope

- Multiple architecture diagrams
- Video embeds
- Collaborative editing
- Scheduled publishing
- Version history
- Custom slug editing
- More than 3 metric cards
- CSV import
- Importing from external sources
- Duplicating case studies
- Drag-and-drop reorder
- CSV export from list
- Comparing case studies in insights
- Filtering access logs
- Custom date range for insights
- Share with password protection
