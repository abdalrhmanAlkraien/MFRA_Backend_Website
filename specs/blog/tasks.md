# Tasks: Blog Module

**Module**: blog
**Generated**: 29/03/2026 at 14:00
**Total tasks**: 6

---

## Task 3.1 — Backend: Blog — Entities + Admin CRUD

**Platform**: Backend
**Complexity**: Complex
**Estimated cost**: $1.20
**Dependencies**: Task 1.3 (Security + JWT), Task 1.4 (S3 Upload)

**Delivers**:
- Flyway migration: V3__create_blog_tables.sql (blogs, blog_categories, blog_tags, blog_tag_map, blog_gallery_images)
- BlogEntity, BlogCategoryEntity, BlogTagEntity, BlogGalleryImageEntity
- BlogRepository, BlogCategoryRepository, BlogTagRepository, BlogGalleryImageRepository
- BlogService (CRUD, publish, feature, auto-save, slug generation, reading time)
- BlogAdminController
- BlogCategoryAdminController
- BlogTagAdminController
- All DTOs (request + response) + MapStruct mapper

**API endpoints covered**:
- GET /api/admin/blogs (paginated + filtered)
- GET /api/admin/blogs/{id}
- POST /api/admin/blogs
- PUT /api/admin/blogs/{id}
- PATCH /api/admin/blogs/{id}
- PATCH /api/admin/blogs/{id}/publish
- PATCH /api/admin/blogs/{id}/order
- DELETE /api/admin/blogs/{id}
- GET /api/admin/blogs/insights (summary cards)
- GET /api/admin/blog-categories
- GET /api/admin/blog-tags

**Acceptance criteria covered**:
- [AC-1] through [AC-9], [AC-14], [AC-S1], [AC-S2], [AC-S3]

**Test scenarios**:
- Backend: 18 scenarios (CRUD happy path, slug generation + collision, reading time calculation, featured toggle + un-feature, auto-save partial update, publish validation, soft delete, EDITOR restrictions, pagination + filtering, unauthorized access)

---

## Task 3.2 — Backend: Blog — Public Read Endpoints + Caching

**Platform**: Backend
**Complexity**: Simple
**Estimated cost**: $0.40
**Dependencies**: Task 3.1

**Delivers**:
- BlogPublicController
- BlogPublicService (published-only queries + Redis caching)
- Public DTOs (no draft fields, no admin fields)
- Redis caching: blog list 10min, single blog 30min
- Cache invalidation on write operations

**API endpoints covered**:
- GET /api/public/blogs (paginated, published only)
- GET /api/public/blogs/{slug}
- GET /api/public/blog-categories

**Acceptance criteria covered**:
- [AC-10], [AC-11], [AC-12], [AC-13]

**Test scenarios**:
- Backend: 6 scenarios (public list published only, public slug lookup, draft not visible, cache hit, cache invalidation, unknown slug 404)

---

## Task 3.3 — Frontend: Blog — List Page

**Platform**: Frontend
**Complexity**: Complex
**Estimated cost**: $1.00
**Dependencies**: Task 1.6 (Admin Layout Shell), Task 3.1 (Blog Backend)

**Delivers**:
- features/blog/blogApi.ts (RTK Query endpoints)
- features/blog/types.ts
- pages/admin/blogs/BlogListPage.tsx (page 3 design)
- components/blog/BlogTable.tsx
- components/blog/BlogFilterTabs.tsx
- components/blog/BlogInsightCards.tsx
- components/shared/Pagination.tsx
- components/shared/StatusBadge.tsx
- components/shared/CategoryChip.tsx
- URL-synced filter state
- Delete confirmation modal
- Empty states

**Acceptance criteria covered**:
- [AC-20], [AC-21], [AC-22], [AC-29], [AC-32], [AC-33]

**Test scenarios**:
- Frontend: 10 scenarios (render page, filter tabs, pagination, EDITOR restrictions, delete modal, empty state, loading state, error state, category chips, navigation to editor)

---

## Task 3.4 — Frontend: Blog — Editor Page

**Platform**: Frontend
**Complexity**: Complex
**Estimated cost**: $1.20
**Dependencies**: Task 3.3 (Blog List Page)

**Delivers**:
- pages/admin/blogs/BlogEditorPage.tsx (page 5 design)
- components/blog/editor/RichTextEditor.tsx (TipTap setup)
- components/blog/editor/RichTextToolbar.tsx
- components/blog/editor/PublishingSettings.tsx (right panel)
- components/blog/editor/PostMetadata.tsx (tags + author)
- components/blog/editor/MediaAssets.tsx (hero + gallery)
- components/shared/S3UploadCard.tsx (presigned URL upload flow)
- hooks/useAutoSave.ts
- components/blog/editor/SearchPreview.tsx
- Featured post conflict modal

**Acceptance criteria covered**:
- [AC-23], [AC-24], [AC-25], [AC-26], [AC-27], [AC-28], [AC-29], [AC-32], [AC-33]

**Test scenarios**:
- Frontend: 12 scenarios (render editor, create mode, edit mode, auto-save trigger, TipTap toolbar, publish validation, featured conflict, S3 upload, gallery limit, EDITOR restrictions, discard confirmation, loading/error states)

---

## Task 3.5 — Backend: Blog Insights — Analytics APIs

**Platform**: Backend
**Complexity**: Medium
**Estimated cost**: $0.80
**Dependencies**: Task 3.1 (Blog Backend)

**Delivers**:
- Flyway migration: V6__create_page_views_table.sql
- PageViewEntity + PageViewRepository
- BlogInsightsService (aggregation queries)
- PageViewTrackingService (record views from public endpoints)
- BlogInsightsController (4 endpoints)
- Insights DTOs

**API endpoints covered**:
- GET /api/admin/blogs/{id}/insights/summary
- GET /api/admin/blogs/{id}/insights/geo
- GET /api/admin/blogs/{id}/insights/access-log
- GET /api/admin/blogs/{id}/insights/engagement

**Acceptance criteria covered**:
- [AC-15], [AC-16], [AC-17], [AC-18], [AC-19]

**Test scenarios**:
- Backend: 8 scenarios (summary aggregation, geo top 5, access log recent 8, engagement daily/weekly, page view tracking, draft blog → 404, empty views → zero counts, time period filtering)

---

## Task 3.6 — Frontend: Blog Insights — Analytics Page

**Platform**: Frontend
**Complexity**: Complex
**Estimated cost**: $1.00
**Dependencies**: Task 3.3 (Blog List Page), Task 3.5 (Blog Insights Backend)

**Delivers**:
- pages/admin/blogs/BlogInsightsPage.tsx (page 4 design)
- components/insights/KpiStatCard.tsx (shared with case-study insights)
- components/insights/GeoDistributionPanel.tsx (map + country table)
- components/insights/AccessLogPanel.tsx
- components/blog/insights/EngagementTrendChart.tsx (Recharts line chart)
- components/shared/SystemStatusFooter.tsx
- features/blog/blogInsightsApi.ts (RTK Query endpoints)

**Acceptance criteria covered**:
- [AC-30], [AC-31], [AC-32], [AC-33]

**Test scenarios**:
- Frontend: 8 scenarios (render insights page, 4 KPI cards, geo map + table, access log list, engagement chart, draft blog redirect, loading/error states, data-testid attributes)
