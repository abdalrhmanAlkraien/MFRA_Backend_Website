# Requirements: 7 — Case Study Insights

**Design**: designs/7.png
**Spec**: designs/7.md
**Analyzed**: 29/03/2026 at 12:00
**URL**: /admin/case-studies/:id/insights
**Access**: ADMIN (full), EDITOR (full including Export + Share)
**Primary action**: View per-case-study analytics with export and share capabilities

---

## Design System Notes
File: designs/DESIGN.md

Tokens used on this page:
  Breadcrumb: label font, last item primary_container
  KPI cards: surface_container_high
  Bar chart: on_surface_variant bars (~40% opacity), hover primary_container
  Geographic map: surface_container bg, orange dots
  Country progress bars: primary_container
  Access log table: alternating rows, status chips
  Export button: secondary style
  Share button: primary gradient

---

## Sections

| # | Section | Type | Data Source |
|---|---------|------|-------------|
| 1 | Breadcrumb (3-level) | Interactive | Static + dynamic title |
| 2 | Title + subtitle + action buttons | Dynamic/Interactive | API + Export/Share |
| 3 | 4 KPI Stat Cards | Dynamic | GET .../insights/summary |
| 4 | Engagement Trends (bar chart) | Dynamic | GET .../insights/engagement |
| 5 | Geographic Reach (map + progress bars) | Dynamic | GET .../insights/geo |
| 6 | Recent Access Logs table | Dynamic | GET .../insights/access-log |

---

## Database Requirements

Uses `page_views` table from 4-requirements.md. Additional table for share links:

```sql
-- V7__create_insight_shares_table.sql

CREATE TABLE insight_shares (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    resource_type  VARCHAR(50)  NOT NULL CHECK (resource_type IN ('BLOG', 'CASE_STUDY')),
    resource_id    UUID         NOT NULL,
    share_token    VARCHAR(255) NOT NULL UNIQUE,
    expires_at     TIMESTAMPTZ  NOT NULL,
    created_by     UUID         REFERENCES admin_users(id),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_insight_shares_token ON insight_shares(share_token);
CREATE INDEX idx_insight_shares_resource ON insight_shares(resource_type, resource_id);
CREATE INDEX idx_insight_shares_expires ON insight_shares(expires_at);
```

---

## API Endpoints

| Method | Path | Auth | Description | Cached | Rate Limited |
|--------|------|------|-------------|--------|--------------|
| GET | /api/admin/case-studies/:id/insights/summary | ADMIN or EDITOR | 4 KPIs | No | No |
| GET | /api/admin/case-studies/:id/insights/engagement | ADMIN or EDITOR | Bar chart data | No | No |
| GET | /api/admin/case-studies/:id/insights/geo | ADMIN or EDITOR | Map + countries | No | No |
| GET | /api/admin/case-studies/:id/insights/access-log | ADMIN or EDITOR | Access log table | No | No |
| POST | /api/admin/case-studies/:id/insights/export-pdf | ADMIN or EDITOR | Generate PDF | No | No |
| POST | /api/admin/case-studies/:id/insights/share | ADMIN or EDITOR | Create share link | No | No |

---

## Business Rules

1. Only accessible for PUBLISHED case studies
2. Total views shown as full integer — no abbreviation
3. Avg read time format: Xm Ys (not X:Y min like page 4)
4. Performance labels: > 5% = HIGH PERFORMING, 2-5% = standard, < 2% = NEEDS ATTENTION
5. Bar chart defaults to Daily
6. Geographic shows top 5 countries with progress bars
7. Access log shows 10 entries with AUTHENTICATED/GUEST status
8. Continent color coding: Americas=red, Europe=blue, Asia-Pacific=green, MENA=amber
9. Share link expires after 7 days
10. Export PDF generated server-side on demand

---

## Clarifications

None — design was clear.

---

## Out of Scope

- Comparing case studies
- Filtering access logs
- Custom date range
- Exporting access logs as CSV
- Share with password protection
