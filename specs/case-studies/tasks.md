# Tasks: Case Studies Module

**Module**: case-studies
**Generated**: 29/03/2026 at 14:00
**Total tasks**: 6

---

## Task 4.1 — Backend: Case Study — Entities + Admin CRUD

**Platform**: Backend
**Complexity**: Complex
**Estimated cost**: $1.20
**Dependencies**: Task 1.3 (Security + JWT), Task 1.4 (S3 Upload)

**Delivers**:
- Flyway migration: V4__create_case_study_tables.sql (case_studies, case_study_categories, case_study_tags, case_study_tag_map, case_study_results, case_study_comparisons)
- CaseStudyEntity, CaseStudyCategoryEntity, CaseStudyTagEntity, CaseStudyResultEntity, CaseStudyComparisonEntity
- All Repositories
- CaseStudyService (CRUD, publish, unpublish, feature, results, bulk actions)
- CaseStudyAdminController
- CaseStudyCategoryAdminController
- CaseStudyTagAdminController
- All DTOs (request + response) + MapStruct mapper

**API endpoints covered**:
- GET /api/admin/case-studies (paginated + filtered + searchable)
- GET /api/admin/case-studies/{id}
- POST /api/admin/case-studies
- PUT /api/admin/case-studies/{id}
- PATCH /api/admin/case-studies/{id}
- PATCH /api/admin/case-studies/{id}/publish
- PATCH /api/admin/case-studies/{id}/unpublish
- PATCH /api/admin/case-studies/{id}/feature
- PATCH /api/admin/case-studies/{id}/results
- DELETE /api/admin/case-studies/{id}
- POST /api/admin/case-studies/bulk
- GET /api/admin/case-study-categories
- GET /api/admin/case-study-industries
- GET /api/admin/tags?type=case-study
- POST /api/admin/tags

**Acceptance criteria covered**:
- [AC-1] through [AC-17], [AC-S1], [AC-S2], [AC-S3]

**Test scenarios**:
- Backend: 20 scenarios (CRUD happy path, slug collision, featured toggle, publish validation with required fields, results save atomic, comparisons validation, bulk actions, EDITOR restrictions, soft delete, reading time, search + filter, unauthorized)

---

## Task 4.2 — Backend: Case Study — Public Read Endpoints + Caching

**Platform**: Backend
**Complexity**: Simple
**Estimated cost**: $0.40
**Dependencies**: Task 4.1

**Delivers**:
- CaseStudyPublicController
- CaseStudyPublicService (published-only, includes results + comparisons)
- Public DTOs
- Redis caching: list 10min, single 30min
- Cache invalidation on write

**API endpoints covered**:
- GET /api/public/case-studies
- GET /api/public/case-studies/{slug}
- GET /api/public/case-study-categories

**Acceptance criteria covered**:
- [AC-9], [AC-10], [AC-11]

**Test scenarios**:
- Backend: 6 scenarios (public list published only, public slug with results + comparisons, draft not visible, cache hit, cache invalidation, unknown slug 404)

---

## Task 4.3 — Frontend: Case Study — List Page

**Platform**: Frontend
**Complexity**: Complex
**Estimated cost**: $1.00
**Dependencies**: Task 1.6 (Admin Layout Shell), Task 4.1 (Case Study Backend)

**Delivers**:
- features/case-study/caseStudyApi.ts (RTK Query endpoints)
- features/case-study/types.ts
- pages/admin/case-studies/CaseStudyListPage.tsx (page 6 design)
- components/case-study/CaseStudyTable.tsx
- components/case-study/CaseStudyFilters.tsx (search + status + industry + category)
- components/case-study/BulkActionBar.tsx (ADMIN only)
- components/case-study/FeaturedStar.tsx
- URL-synced filter + pagination
- Delete + featured confirmation modals
- Empty states

**Acceptance criteria covered**:
- [AC-25], [AC-26], [AC-27], [AC-28], [AC-35], [AC-36], [AC-37]

**Test scenarios**:
- Frontend: 10 scenarios (render page, search + filters, bulk actions ADMIN only, featured toggle, pagination, EDITOR restrictions, delete modal, empty state, loading state, URL filter sync)

---

## Task 4.4 — Frontend: Case Study — Editor Page

**Platform**: Frontend
**Complexity**: Complex
**Estimated cost**: $1.20
**Dependencies**: Task 4.3 (Case Study List Page)

**Delivers**:
- pages/admin/case-studies/CaseStudyEditorPage.tsx (page 8 design)
- components/case-study/editor/ProjectMetadata.tsx (industry, customer, country, city, tags)
- components/case-study/editor/ResultMetricCards.tsx (3 editable cards)
- components/case-study/editor/ComparisonTable.tsx (before/after, add/remove rows)
- components/case-study/editor/ArchitectureDiagramUpload.tsx
- Reuses: TipTap editor, useAutoSave, PublishingSettings from blog module

**Acceptance criteria covered**:
- [AC-29], [AC-30], [AC-31], [AC-32], [AC-35], [AC-36], [AC-37]

**Test scenarios**:
- Frontend: 12 scenarios (render editor, create/edit modes, result metric cards, comparison table add/remove rows, architecture diagram upload, auto-save, publish validation, EDITOR restrictions, metadata fields, TipTap editor, loading/error states)

---

## Task 4.5 — Backend: Case Study Insights — Analytics + Export + Share

**Platform**: Backend
**Complexity**: Complex
**Estimated cost**: $1.00
**Dependencies**: Task 4.1 (Case Study Backend), Task 3.5 (Blog Insights — shared PageViewEntity)

**Delivers**:
- Flyway migration: V7__create_insight_shares_table.sql
- InsightShareEntity + InsightShareRepository
- CaseStudyInsightsService (aggregation queries)
- PdfExportService (OpenPDF)
- ShareLinkService (token generation + validation)
- CaseStudyInsightsController (6 endpoints)
- Insights DTOs + ShareLinkResponse

**API endpoints covered**:
- GET /api/admin/case-studies/{id}/insights/summary
- GET /api/admin/case-studies/{id}/insights/engagement
- GET /api/admin/case-studies/{id}/insights/geo
- GET /api/admin/case-studies/{id}/insights/access-log
- POST /api/admin/case-studies/{id}/insights/export-pdf
- POST /api/admin/case-studies/{id}/insights/share

**Acceptance criteria covered**:
- [AC-18] through [AC-24]

**Test scenarios**:
- Backend: 10 scenarios (summary KPIs, engagement bar data, geo top 5, access log 10 entries, PDF generation, share link creation, share link expiry, performance labels, draft 404, unauthorized)

---

## Task 4.6 — Frontend: Case Study Insights — Analytics Page

**Platform**: Frontend
**Complexity**: Complex
**Estimated cost**: $1.00
**Dependencies**: Task 4.3 (Case Study List Page), Task 4.5 (Case Study Insights Backend)

**Delivers**:
- pages/admin/case-studies/CaseStudyInsightsPage.tsx (page 7 design)
- components/case-study/insights/EngagementBarChart.tsx (Recharts bar chart)
- components/case-study/insights/GeographicReachPanel.tsx (map + progress bars)
- components/case-study/insights/AccessLogTable.tsx (full-width, status chips)
- components/case-study/insights/ExportPdfButton.tsx
- components/case-study/insights/ShareReportButton.tsx
- components/shared/Breadcrumb.tsx
- features/case-study/caseStudyInsightsApi.ts (RTK Query endpoints)

**Acceptance criteria covered**:
- [AC-33], [AC-34], [AC-35], [AC-36]

**Test scenarios**:
- Frontend: 8 scenarios (render insights, bar chart, geo map + progress bars, access log with status chips, export PDF download, share link copy, continent color coding, loading/error states)
