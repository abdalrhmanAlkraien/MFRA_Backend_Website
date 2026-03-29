# Requirements: 4 — Blog Article Insights

**Design**: designs/4.png
**Spec**: designs/4.md
**Analyzed**: 29/03/2026 at 12:00
**URL**: /admin/blogs/:blogId/insights
**Access**: ADMIN (full), EDITOR (full — IPs not masked on this page)
**Primary action**: View per-article analytics for a published blog post

---

## Design System Notes
File: designs/DESIGN.md

Tokens used on this page:
  Title: "Article Insights:" on_surface + article title primary_container
  KPI cards: surface_container_high bg
  Positive trend: green (#4ade80)
  Negative trend: error (#f87171)
  Map: surface_container bg, orange dots (primary_container)
  Access log: font-label for IPs, surface_container_highest for browser chips
  Chart: primary_container line with gradient fill
  System status bar: surface_container_lowest

---

## Sections

| # | Section | Type | Data Source |
|---|---------|------|-------------|
| 1 | Back link + Title | Dynamic | Blog title from API |
| 2 | 4 KPI Stat Cards | Dynamic | GET /api/admin/blogs/:id/insights/summary |
| 3 | Geographic Distribution (map + table) | Dynamic | GET /api/admin/blogs/:id/insights/geo |
| 4 | Access Log panel | Dynamic | GET /api/admin/blogs/:id/insights/access-log |
| 5 | Engagement Trend chart | Dynamic | GET /api/admin/blogs/:id/insights/engagement |
| 6 | System Status Footer | Static/Dynamic | Hardcoded + page load timestamp |

---

## Database Requirements

Uses the `access_logs` table defined in 2-requirements.md and `page_views` table:

```sql
-- V6__create_page_views_table.sql

CREATE TABLE page_views (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    resource_type    VARCHAR(50)  NOT NULL CHECK (resource_type IN ('BLOG', 'CASE_STUDY')),
    resource_id      UUID         NOT NULL,
    ip_address       VARCHAR(45)  NOT NULL,
    city             VARCHAR(100),
    country          VARCHAR(100),
    country_code     VARCHAR(10),
    continent        VARCHAR(50),
    lat              NUMERIC(10,6),
    lng              NUMERIC(10,6),
    browser          VARCHAR(100),
    os               VARCHAR(100),
    browser_version  VARCHAR(50),
    is_authenticated BOOLEAN      NOT NULL DEFAULT false,
    session_id       VARCHAR(255),
    read_time_sec    INT,
    bounced          BOOLEAN      DEFAULT false,
    consultation_clicked BOOLEAN  DEFAULT false,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_page_views_resource ON page_views(resource_type, resource_id);
CREATE INDEX idx_page_views_created ON page_views(created_at DESC);
CREATE INDEX idx_page_views_country ON page_views(country_code);
CREATE INDEX idx_page_views_ip ON page_views(ip_address);
```

---

## API Endpoints

| Method | Path | Auth | Description | Cached | Rate Limited |
|--------|------|------|-------------|--------|--------------|
| GET | /api/admin/blogs/:id/insights/summary | ADMIN or EDITOR | 4 KPIs | No | No |
| GET | /api/admin/blogs/:id/insights/geo | ADMIN or EDITOR | Map + country table | No | No |
| GET | /api/admin/blogs/:id/insights/access-log | ADMIN or EDITOR | Recent readers | No | No |
| GET | /api/admin/blogs/:id/insights/engagement | ADMIN or EDITOR | Chart data | No | No |

---

## Business Rules

1. Page only accessible if blog is PUBLISHED (or was published)
2. DRAFT blog → redirect to /admin/blogs with toast
3. Total views abbreviated: >= 1000 → "k" format in frontend
4. Avg read time displayed as mm:ss
5. Bounce rate: downward = positive (green), upward = negative (red)
6. Country table shows top 5
7. Access log shows 8 most recent entries
8. Map defaults to "World" view
9. Chart defaults to "Last 7 Days"
10. All 4 summary stats from single API call

---

## Clarifications

None — design was clear.

---

## Out of Scope

- Exporting analytics as CSV/PDF
- Comparing articles
- Filtering access log
- Real-time WebSocket updates
- Editing article from this page
- Heatmaps or scroll depth
