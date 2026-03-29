# Requirements: 3 — Admin Blog List

**Design**: designs/3.png
**Spec**: designs/3.md
**Analyzed**: 29/03/2026 at 12:00
**URL**: /admin/blogs
**Access**: ADMIN (full), EDITOR (no delete, no reorder)
**Primary action**: View, filter, and manage all blog posts

---

## Design System Notes
File: designs/DESIGN.md

Tokens used on this page:
  Two-tone headline: "Content" on_surface + "Architecture" primary_container
  Filter tabs: surface_container_highest (active), transparent (inactive)
  Status badges: PUBLISHED primary_container, DRAFT on_surface_variant, SCHEDULED tertiary_container
  Category chips: surface_container_highest bg, on_surface text
  Table rows: no borders — alternating surface_container/surface_container_low
  Impact metric: font-display large number
  Insight cards: surface_container bg

---

## Sections

| # | Section | Type | Data Source |
|---|---------|------|-------------|
| 1 | Breadcrumb | Static | Hardcoded "ADMIN > BLOG INVENTORY" |
| 2 | Two-tone headline + subtitle | Static | Hardcoded |
| 3 | Impact metric card | Dynamic | GET /api/admin/blogs (total_published from meta) |
| 4 | Filter tabs | Interactive | Updates URL param ?status= |
| 5 | Create Blog + Categories + Sort buttons | Interactive | Navigation + sort |
| 6 | Blog table | Dynamic | GET /api/admin/blogs |
| 7 | Pagination | Interactive | URL param ?page= |
| 8 | 3 Insight cards | Dynamic | GET /api/admin/blogs/insights |

---

## Database Requirements

| Field | Shown In | Table | Column | Type | Required | Indexed |
|-------|----------|-------|--------|------|----------|---------|
| Title | Table row | blogs | title | VARCHAR(255) | Yes | No |
| Slug | URL/SEO | blogs | slug | VARCHAR(255) | Yes | Yes (UNIQUE) |
| Content | Editor (page 5) | blogs | content | TEXT | No | No |
| Content type | Dropdown | blogs | content_type | VARCHAR(50) | Yes | No |
| Status | Status badge | blogs | status | VARCHAR(50) | Yes | Yes |
| Cover image | Thumbnail | blogs | cover_image_url | VARCHAR(500) | No | No |
| Is featured | Featured toggle | blogs | is_featured | BOOLEAN | Yes | No |
| Display order | ORDER column | blogs | display_order | INT | Yes | Yes |
| Display priority | Settings panel | blogs | display_priority | INT | Yes | No |
| Reading time | Computed | blogs | reading_time_min | INT | No | No |
| Word count | Computed | blogs | word_count | INT | No | No |
| Published at | Status change | blogs | published_at | TIMESTAMPTZ | No | Yes |
| Category name | Category chips | blog_categories | name | VARCHAR(100) | Yes | No |
| Category slug | URL filter | blog_categories | slug | VARCHAR(100) | Yes | Yes |
| Tag name | Post metadata | blog_tags | name | VARCHAR(100) | Yes | No |
| Created by name | Table metadata | admin_users | full_name | — | — | — |

---

## New Tables Required

```sql
-- V3__create_blog_tables.sql

CREATE TABLE blog_categories (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL,
    slug        VARCHAR(100) NOT NULL UNIQUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by  UUID,
    updated_by  UUID,
    deleted_at  TIMESTAMPTZ
);

CREATE UNIQUE INDEX idx_blog_categories_slug ON blog_categories(slug) WHERE deleted_at IS NULL;
CREATE INDEX idx_blog_categories_active ON blog_categories(deleted_at) WHERE deleted_at IS NULL;

CREATE TABLE blog_tags (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL,
    slug        VARCHAR(100) NOT NULL UNIQUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at  TIMESTAMPTZ
);

CREATE UNIQUE INDEX idx_blog_tags_slug ON blog_tags(slug) WHERE deleted_at IS NULL;
CREATE INDEX idx_blog_tags_active ON blog_tags(deleted_at) WHERE deleted_at IS NULL;

CREATE TABLE blogs (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    title            VARCHAR(255) NOT NULL,
    slug             VARCHAR(255) NOT NULL,
    content          TEXT,
    content_type     VARCHAR(50)  NOT NULL DEFAULT 'BLOG'
                     CHECK (content_type IN ('BLOG', 'CASE_STUDY', 'TUTORIAL', 'NEWS')),
    status           VARCHAR(50)  NOT NULL DEFAULT 'DRAFT'
                     CHECK (status IN ('DRAFT', 'PUBLISHED', 'SCHEDULED', 'ARCHIVED')),
    cover_image_url  VARCHAR(500),
    is_featured      BOOLEAN      NOT NULL DEFAULT false,
    display_order    INT          NOT NULL DEFAULT 0,
    display_priority INT          NOT NULL DEFAULT 1
                     CHECK (display_priority BETWEEN 1 AND 10),
    reading_time_min INT,
    word_count       INT,
    published_at     TIMESTAMPTZ,
    category_id      UUID         REFERENCES blog_categories(id),
    author_id        UUID         REFERENCES admin_users(id),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by       UUID,
    updated_by       UUID,
    deleted_at       TIMESTAMPTZ
);

CREATE UNIQUE INDEX idx_blogs_slug ON blogs(slug) WHERE deleted_at IS NULL;
CREATE INDEX idx_blogs_status ON blogs(status);
CREATE INDEX idx_blogs_active ON blogs(deleted_at) WHERE deleted_at IS NULL;
CREATE INDEX idx_blogs_category ON blogs(category_id);
CREATE INDEX idx_blogs_author ON blogs(author_id);
CREATE INDEX idx_blogs_published_at ON blogs(published_at DESC);
CREATE INDEX idx_blogs_display_order ON blogs(display_order) WHERE deleted_at IS NULL;
CREATE INDEX idx_blogs_featured ON blogs(is_featured) WHERE is_featured = true AND deleted_at IS NULL;

CREATE TABLE blog_tag_map (
    blog_id UUID NOT NULL REFERENCES blogs(id) ON DELETE CASCADE,
    tag_id  UUID NOT NULL REFERENCES blog_tags(id) ON DELETE CASCADE,
    PRIMARY KEY (blog_id, tag_id)
);

CREATE INDEX idx_blog_tag_map_blog ON blog_tag_map(blog_id);
CREATE INDEX idx_blog_tag_map_tag ON blog_tag_map(tag_id);

CREATE TABLE blog_gallery_images (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    blog_id       UUID         NOT NULL REFERENCES blogs(id),
    image_url     VARCHAR(500) NOT NULL,
    display_order INT          NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at    TIMESTAMPTZ
);

CREATE INDEX idx_blog_gallery_blog ON blog_gallery_images(blog_id);
CREATE INDEX idx_blog_gallery_active ON blog_gallery_images(deleted_at) WHERE deleted_at IS NULL;
```

---

## API Endpoints

| Method | Path | Auth | Description | Cached | Rate Limited |
|--------|------|------|-------------|--------|--------------|
| GET | /api/admin/blogs | ADMIN or EDITOR | Paginated blog list | No | No |
| GET | /api/admin/blogs/insights | ADMIN or EDITOR | Insight cards data | No | No |
| GET | /api/admin/blog-categories | ADMIN or EDITOR | Category list | No | No |
| GET | /api/admin/blog-tags | ADMIN or EDITOR | Tags for autocomplete | No | No |
| PATCH | /api/admin/blogs/{id}/order | ADMIN only | Update display order | No | No |
| DELETE | /api/admin/blogs/{id} | ADMIN only | Soft delete blog | No | No |

### GET /api/admin/blogs
```
Query params: status, page, size, sort, order
Response: {
  data: [{ id, title, slug, cover_image_url, status, display_order,
           categories: [{id, name}], created_at, updated_at,
           created_by_name, updated_by_name }],
  meta: { total, page, size, total_pages, total_published }
}
```

---

## Business Rules

1. Default sort: last modified descending
2. Filter tabs update URL param — browser back restores filter
3. EDITOR: delete icon hidden, ORDER column read-only
4. Delete requires confirmation modal → soft delete
5. Category chips: show first 2 + "+N more" if > 2
6. Relative timestamps recalculated on load
7. Thumbnail placeholder if no cover image
8. Blog title click navigates to editor (page 5)
9. Page size: 10 blogs per page

---

## Clarifications

None — design was clear.

---

## Out of Scope

- Bulk select/delete
- Drag-and-drop reorder
- CSV/PDF export
- Search within blog inventory
- Categories CRUD modal (future)
- Scheduled publishing automation
