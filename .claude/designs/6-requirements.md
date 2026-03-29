# Requirements: 6 — Case Studies List

**Design**: designs/6.png
**Spec**: designs/6.md
**Analyzed**: 29/03/2026 at 12:00
**URL**: /admin/case-studies
**Access**: ADMIN (full), EDITOR (no delete, no feature)
**Primary action**: View, filter, and manage all case studies

---

## Design System Notes
File: designs/DESIGN.md

Tokens used on this page:
  Headline: font-display "Case Study Inventory"
  Stat cards: surface_container_high — "TOTAL PROJECTS" + "LIVE REACH"
  Table: no borders, alternating surface_container/surface_container_low
  Status: Published primary_container, Draft on_surface_variant, Archived error
  Category/Industry chips: surface_container_highest bg
  Create button: primary gradient
  Pagination: surface_container_highest for active page

---

## Sections

| # | Section | Type | Data Source |
|---|---------|------|-------------|
| 1 | Breadcrumb | Static | "INVENTORY / ALL PROJECTS" |
| 2 | Page Title + Subtitle | Static | Hardcoded |
| 3 | Create Button + Stat Cards | Dynamic/Interactive | Total + Live Reach |
| 4 | Case Study Table | Dynamic | GET /api/admin/case-studies |
| 5 | Pagination | Interactive | URL param ?page= |

---

## Database Requirements

| Field | Shown In | Table | Column | Type | Required | Indexed |
|-------|----------|-------|--------|------|----------|---------|
| Title | Table | case_studies | title | VARCHAR(255) | Yes | No |
| Slug | URL | case_studies | slug | VARCHAR(255) | Yes | Yes (UNIQUE) |
| Content | Editor (page 8) | case_studies | content | TEXT | No | No |
| Status | Badge | case_studies | status | VARCHAR(50) | Yes | Yes |
| Client name | Subtitle | case_studies | client_name | VARCHAR(150) | No | No |
| Industry | Column | case_studies | industry | VARCHAR(100) | Yes | Yes |
| Country | Metadata | case_studies | country | VARCHAR(100) | No | No |
| City | Metadata | case_studies | city | VARCHAR(100) | No | No |
| Cover image | Thumbnail | case_studies | cover_image_url | VARCHAR(500) | No | No |
| Architecture diagram | Editor | case_studies | architecture_diagram_url | VARCHAR(500) | No | No |
| Is featured | Star icon | case_studies | is_featured | BOOLEAN | Yes | No |
| Display order | ORDER column | case_studies | display_order | INT | Yes | Yes |
| Display priority | Settings | case_studies | display_priority | INT | Yes | No |
| Published at | Status change | case_studies | published_at | TIMESTAMPTZ | No | Yes |
| Category name | Chip | case_study_categories | name | VARCHAR(100) | Yes | No |

---

## New Tables Required

```sql
-- V4__create_case_study_tables.sql

CREATE TABLE case_study_categories (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL,
    slug        VARCHAR(100) NOT NULL UNIQUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by  UUID,
    updated_by  UUID,
    deleted_at  TIMESTAMPTZ
);

CREATE UNIQUE INDEX idx_cs_categories_slug ON case_study_categories(slug) WHERE deleted_at IS NULL;
CREATE INDEX idx_cs_categories_active ON case_study_categories(deleted_at) WHERE deleted_at IS NULL;

CREATE TABLE case_study_tags (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL,
    slug        VARCHAR(100) NOT NULL UNIQUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at  TIMESTAMPTZ
);

CREATE UNIQUE INDEX idx_cs_tags_slug ON case_study_tags(slug) WHERE deleted_at IS NULL;
CREATE INDEX idx_cs_tags_active ON case_study_tags(deleted_at) WHERE deleted_at IS NULL;

CREATE TABLE case_studies (
    id                       UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    title                    VARCHAR(255) NOT NULL,
    slug                     VARCHAR(255) NOT NULL,
    content                  TEXT,
    content_type             VARCHAR(50)  NOT NULL DEFAULT 'CASE_STUDY',
    status                   VARCHAR(50)  NOT NULL DEFAULT 'DRAFT'
                             CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    client_name              VARCHAR(150),
    industry                 VARCHAR(100),
    country                  VARCHAR(100),
    city                     VARCHAR(100),
    cover_image_url          VARCHAR(500),
    architecture_diagram_url VARCHAR(500),
    is_featured              BOOLEAN      NOT NULL DEFAULT false,
    display_order            INT          NOT NULL DEFAULT 0,
    display_priority         INT          NOT NULL DEFAULT 1
                             CHECK (display_priority BETWEEN 1 AND 10),
    reading_time_min         INT,
    word_count               INT,
    published_at             TIMESTAMPTZ,
    category_id              UUID         REFERENCES case_study_categories(id),
    created_at               TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at               TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by               UUID,
    updated_by               UUID,
    deleted_at               TIMESTAMPTZ
);

CREATE UNIQUE INDEX idx_case_studies_slug ON case_studies(slug) WHERE deleted_at IS NULL;
CREATE INDEX idx_case_studies_status ON case_studies(status);
CREATE INDEX idx_case_studies_active ON case_studies(deleted_at) WHERE deleted_at IS NULL;
CREATE INDEX idx_case_studies_category ON case_studies(category_id);
CREATE INDEX idx_case_studies_industry ON case_studies(industry);
CREATE INDEX idx_case_studies_published_at ON case_studies(published_at DESC);
CREATE INDEX idx_case_studies_display_order ON case_studies(display_order) WHERE deleted_at IS NULL;
CREATE INDEX idx_case_studies_featured ON case_studies(is_featured) WHERE is_featured = true AND deleted_at IS NULL;

CREATE TABLE case_study_tag_map (
    case_study_id UUID NOT NULL REFERENCES case_studies(id) ON DELETE CASCADE,
    tag_id        UUID NOT NULL REFERENCES case_study_tags(id) ON DELETE CASCADE,
    PRIMARY KEY (case_study_id, tag_id)
);

CREATE INDEX idx_cs_tag_map_cs ON case_study_tag_map(case_study_id);
CREATE INDEX idx_cs_tag_map_tag ON case_study_tag_map(tag_id);

CREATE TABLE case_study_results (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    case_study_id   UUID         NOT NULL REFERENCES case_studies(id),
    label           VARCHAR(100) NOT NULL,
    value           VARCHAR(50)  NOT NULL,
    description     VARCHAR(200),
    color_variant   VARCHAR(20)  NOT NULL DEFAULT 'PRIMARY'
                    CHECK (color_variant IN ('PRIMARY', 'SECONDARY')),
    display_order   INT          NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at      TIMESTAMPTZ
);

CREATE INDEX idx_cs_results_cs ON case_study_results(case_study_id);
CREATE INDEX idx_cs_results_active ON case_study_results(deleted_at) WHERE deleted_at IS NULL;

CREATE TABLE case_study_comparisons (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    case_study_id    UUID         NOT NULL REFERENCES case_studies(id),
    metric           VARCHAR(150) NOT NULL,
    legacy_value     VARCHAR(100) NOT NULL,
    modernized_value VARCHAR(100) NOT NULL,
    display_order    INT          NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at       TIMESTAMPTZ
);

CREATE INDEX idx_cs_comparisons_cs ON case_study_comparisons(case_study_id);
CREATE INDEX idx_cs_comparisons_active ON case_study_comparisons(deleted_at) WHERE deleted_at IS NULL;
```

---

## API Endpoints

| Method | Path | Auth | Description | Cached | Rate Limited |
|--------|------|------|-------------|--------|--------------|
| GET | /api/admin/case-studies | ADMIN or EDITOR | Paginated list | No | No |
| GET | /api/admin/case-study-categories | ADMIN or EDITOR | Category options | No | No |
| GET | /api/admin/case-study-industries | ADMIN or EDITOR | Industry filter | No | No |
| PATCH | /api/admin/case-studies/:id/feature | ADMIN only | Toggle featured | No | No |
| PATCH | /api/admin/case-studies/:id/publish | ADMIN or EDITOR | Publish | No | No |
| PATCH | /api/admin/case-studies/:id/unpublish | ADMIN or EDITOR | Unpublish | No | No |
| DELETE | /api/admin/case-studies/:id | ADMIN only | Soft delete | No | No |
| POST | /api/admin/case-studies/bulk | ADMIN only | Bulk action | No | No |

---

## Business Rules

1. Only PUBLISHED case studies visible on public website
2. Only one featured at a time — backend enforces
3. EDITOR cannot delete or feature — hidden in UI, 403 in backend
4. Filtering + search server-side
5. URL reflects filter state for bookmarking
6. Insights icon only for PUBLISHED items
7. Soft delete only
8. Default page size: 15
9. Bulk actions ADMIN only

---

## Clarifications

None — design was clear.

---

## Out of Scope

- Importing from external sources
- Duplicating case studies
- Scheduled publishing
- CSV export
- Drag-and-drop reorder
