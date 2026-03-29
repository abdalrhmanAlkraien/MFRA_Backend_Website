# Spec: Blog Module

**Module**: blog
**Generated**: 29/03/2026 at 14:00
**Source pages**: 3-requirements.md, 4-requirements.md, 5-requirements.md
**Platforms**: Backend ✅ | Frontend ✅ | Mobile ⛔

---

## Overview

The Blog module provides full CRUD for blog posts with categories, tags, and gallery images. It includes an admin interface with a rich text editor (TipTap), auto-save, featured post management, and per-article analytics (insights). Public endpoints serve published-only content with Redis caching. Page views are tracked for analytics.

---

## Database Schema

### Table: blog_categories
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

### Table: blog_tags
| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | UUID | PK, default gen_random_uuid() | |
| name | VARCHAR(100) | NOT NULL | |
| slug | VARCHAR(100) | NOT NULL, UNIQUE (partial) | Auto-generated |
| created_at | TIMESTAMPTZ | NOT NULL DEFAULT now() | |
| updated_at | TIMESTAMPTZ | NOT NULL DEFAULT now() | |
| deleted_at | TIMESTAMPTZ | | Soft delete |

### Table: blogs
| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | UUID | PK, default gen_random_uuid() | |
| title | VARCHAR(255) | NOT NULL | |
| slug | VARCHAR(255) | NOT NULL, UNIQUE (partial) | Auto-generated from title |
| content | TEXT | | Rich text (HTML from TipTap) |
| content_type | VARCHAR(50) | NOT NULL, DEFAULT 'BLOG', CHECK IN ('BLOG','CASE_STUDY','TUTORIAL','NEWS') | |
| status | VARCHAR(50) | NOT NULL, DEFAULT 'DRAFT', CHECK IN ('DRAFT','PUBLISHED','SCHEDULED','ARCHIVED') | |
| cover_image_url | VARCHAR(500) | | S3 URL |
| is_featured | BOOLEAN | NOT NULL, DEFAULT false | Only one at a time |
| display_order | INT | NOT NULL, DEFAULT 0 | |
| display_priority | INT | NOT NULL, DEFAULT 1, CHECK BETWEEN 1 AND 10 | 1=highest |
| reading_time_min | INT | | Auto-calculated from word count |
| word_count | INT | | Auto-calculated |
| published_at | TIMESTAMPTZ | | Set on publish |
| category_id | UUID | FK → blog_categories(id) | |
| author_id | UUID | FK → admin_users(id) | |
| created_at | TIMESTAMPTZ | NOT NULL DEFAULT now() | |
| updated_at | TIMESTAMPTZ | NOT NULL DEFAULT now() | |
| created_by | UUID | | |
| updated_by | UUID | | |
| deleted_at | TIMESTAMPTZ | | Soft delete |

### Table: blog_tag_map
| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| blog_id | UUID | FK → blogs(id), ON DELETE CASCADE | |
| tag_id | UUID | FK → blog_tags(id), ON DELETE CASCADE | |
| | | PRIMARY KEY (blog_id, tag_id) | Composite PK |

### Table: blog_gallery_images
| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | UUID | PK, default gen_random_uuid() | |
| blog_id | UUID | NOT NULL, FK → blogs(id) | |
| image_url | VARCHAR(500) | NOT NULL | S3 URL |
| display_order | INT | NOT NULL, DEFAULT 0 | |
| created_at | TIMESTAMPTZ | NOT NULL DEFAULT now() | |
| updated_at | TIMESTAMPTZ | NOT NULL DEFAULT now() | |
| deleted_at | TIMESTAMPTZ | | Soft delete |

### Table: page_views
| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | UUID | PK, default gen_random_uuid() | |
| resource_type | VARCHAR(50) | NOT NULL, CHECK IN ('BLOG','CASE_STUDY') | Shared with case-studies |
| resource_id | UUID | NOT NULL | |
| ip_address | VARCHAR(45) | NOT NULL | |
| city | VARCHAR(100) | | |
| country | VARCHAR(100) | | |
| country_code | VARCHAR(10) | | |
| continent | VARCHAR(50) | | |
| lat | NUMERIC(10,6) | | |
| lng | NUMERIC(10,6) | | |
| browser | VARCHAR(100) | | |
| os | VARCHAR(100) | | |
| browser_version | VARCHAR(50) | | |
| is_authenticated | BOOLEAN | NOT NULL, DEFAULT false | |
| session_id | VARCHAR(255) | | |
| read_time_sec | INT | | |
| bounced | BOOLEAN | DEFAULT false | |
| consultation_clicked | BOOLEAN | DEFAULT false | |
| created_at | TIMESTAMPTZ | NOT NULL DEFAULT now() | Append-only |

**Flyway files**: V3__create_blog_tables.sql, V6__create_page_views_table.sql

---

## API Endpoints

### Public Endpoints (no auth required)
| Method | Path | Description | Cached | Rate Limited |
|--------|------|-------------|--------|--------------|
| GET | /api/public/blogs | Paginated published blogs | Yes 10min | No |
| GET | /api/public/blogs/{slug} | Single published blog by slug | Yes 30min | No |
| GET | /api/public/blog-categories | All categories | Yes 1hr | No |

### Admin Endpoints (JWT required)
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | /api/admin/blogs | ADMIN+EDITOR | Paginated list with filters |
| GET | /api/admin/blogs/{id} | ADMIN+EDITOR | Single blog for editing |
| POST | /api/admin/blogs | ADMIN+EDITOR | Create new blog |
| PUT | /api/admin/blogs/{id} | ADMIN+EDITOR | Full update |
| PATCH | /api/admin/blogs/{id} | ADMIN+EDITOR | Auto-save partial update |
| PATCH | /api/admin/blogs/{id}/publish | ADMIN+EDITOR | Publish blog |
| PATCH | /api/admin/blogs/{id}/order | ADMIN | Update display order |
| DELETE | /api/admin/blogs/{id} | ADMIN | Soft delete |
| GET | /api/admin/blogs/insights | ADMIN+EDITOR | Insight summary cards |
| GET | /api/admin/blog-categories | ADMIN+EDITOR | Category list |
| GET | /api/admin/blog-tags | ADMIN+EDITOR | Tags for autocomplete |

### Insights Endpoints (JWT required)
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | /api/admin/blogs/{id}/insights/summary | ADMIN+EDITOR | 4 KPIs |
| GET | /api/admin/blogs/{id}/insights/geo | ADMIN+EDITOR | Geographic distribution |
| GET | /api/admin/blogs/{id}/insights/access-log | ADMIN+EDITOR | Recent page views |
| GET | /api/admin/blogs/{id}/insights/engagement | ADMIN+EDITOR | Engagement trend chart data |

---

## Acceptance Criteria

### Backend Criteria
- [ ] [AC-1] POST /api/admin/blogs creates a blog with status DRAFT
- [ ] [AC-2] Slug auto-generated from title — unique collision handling (-1, -2, etc.)
- [ ] [AC-3] Reading time auto-calculated: word_count / 200
- [ ] [AC-4] Word count auto-calculated on create/update
- [ ] [AC-5] Only one blog can be featured at a time — previous un-featured automatically
- [ ] [AC-6] PATCH /api/admin/blogs/{id} supports partial update (auto-save)
- [ ] [AC-7] PATCH /api/admin/blogs/{id}/publish sets status=PUBLISHED + publishedAt
- [ ] [AC-8] Title, content, and category required before publish — 400 if missing
- [ ] [AC-9] DELETE soft-deletes — sets deletedAt, never removes record
- [ ] [AC-10] Public API returns PUBLISHED only — drafts never visible
- [ ] [AC-11] Public blog list cached in Redis (10 min TTL)
- [ ] [AC-12] Single blog by slug cached in Redis (30 min TTL)
- [ ] [AC-13] Cache invalidated on create, update, delete, publish
- [ ] [AC-14] EDITOR cannot delete, reorder, feature, or change priority — 403
- [ ] [AC-15] Blog insights summary returns total_views, avg_read_time, bounce_rate, consultation_conversion
- [ ] [AC-16] Blog insights geo returns top 5 countries with view counts
- [ ] [AC-17] Blog insights access-log returns 8 most recent page_view entries
- [ ] [AC-18] Blog insights engagement returns daily/weekly view counts for chart
- [ ] [AC-19] Page views tracked on public blog slug endpoint

### Frontend Criteria
- [ ] [AC-20] Blog list page with filter tabs (All, Published, Draft, Scheduled, Archived)
- [ ] [AC-21] Filter tabs update URL params — browser back restores filter
- [ ] [AC-22] Pagination with URL-synced page state
- [ ] [AC-23] Blog editor with TipTap rich text (bold, italic, headings, lists, links, images)
- [ ] [AC-24] Auto-save: debounce 3 seconds, minimum 60-second interval
- [ ] [AC-25] Auto-save status bar with green dot + elapsed time
- [ ] [AC-26] Featured post toggle with conflict warning modal
- [ ] [AC-27] S3 presigned URL upload for hero + gallery images
- [ ] [AC-28] Maximum 10 gallery images per article
- [ ] [AC-29] EDITOR: delete icon hidden, ORDER read-only, featured hidden, priority hidden
- [ ] [AC-30] Blog insights page with 4 KPI cards, world map, access log, engagement chart
- [ ] [AC-31] Insights only accessible for PUBLISHED blogs — DRAFT redirects with toast
- [ ] [AC-32] Loading, error, and empty states on every API-connected component
- [ ] [AC-33] All interactive elements have data-testid attributes

### Security Criteria
- [ ] [AC-S1] Unauthenticated request to admin blog endpoints → 401
- [ ] [AC-S2] EDITOR accessing ADMIN-only blog endpoint → 403
- [ ] [AC-S3] Invalid or expired token → 401

---

## Business Rules

1. Default sort: last modified descending
2. Filter tabs update URL param — browser back restores filter
3. EDITOR: delete icon hidden, ORDER column read-only
4. Delete requires confirmation modal → soft delete
5. Category chips: show first 2 + "+N more" if > 2
6. Thumbnail placeholder if no cover image
7. Blog title click navigates to editor
8. Page size: 10 blogs per page (admin list)
9. Only one FEATURED blog at a time — warn and un-feature previous
10. Auto-save: debounce 3 seconds, min 60-second interval
11. Create mode: first auto-save fires POST, subsequent fires PATCH
12. Display Priority 1 = highest, 10 = lowest
13. Images upload to S3 directly via pre-signed URL
14. Hero image optional — can publish without it
15. Maximum 10 gallery images per article
16. Reading time auto-calculated: word_count / 200
17. Insights page only for PUBLISHED blogs
18. Total views abbreviated: >= 1000 → "k" format in frontend
19. Bounce rate: downward = positive (green), upward = negative (red)
20. Access log shows 8 most recent entries
21. Chart defaults to "Last 7 Days"

---

## Edge Cases

1. Slug collision → append -1, -2, etc. until unique
2. Empty blog list → return empty array, not 404
3. Unknown slug in public API → 404
4. Delete already-deleted blog → 404
5. Publish without title/content/category → 400 validation error
6. Feature blog when another is featured → un-feature previous automatically
7. Auto-save with no changes → skip (frontend debounce handles)
8. Upload exceeds 5 MB → 400 from presigned URL service
9. Access insights for DRAFT blog → 404 or redirect
10. Gallery at 10 images → disable upload button

---

## Out of Scope

- Version history
- Collaborative editing
- Scheduled publishing automation
- Post preview
- Custom slug editing
- SEO meta override
- Video upload
- Markdown raw mode toggle
- Bulk select/delete
- Drag-and-drop reorder
- CSV/PDF export from blog list
- Search within blog inventory
- Categories CRUD modal
- Exporting analytics as CSV/PDF
- Comparing articles
- Filtering access log
- Real-time WebSocket updates
