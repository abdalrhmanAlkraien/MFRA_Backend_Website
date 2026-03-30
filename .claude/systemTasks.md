# System Tasks

**Project**: MFRA Website
**Total tasks**: 21
**Generated**: 29/03/2026 at 12:00
**Last updated**: 30/03/2026 at 20:00

---

## Phase 1 — Foundation

### Task 1.1: Project Scaffold + Docker Compose
- **Status**: ✅ Completed
- **Actual cost**: $0.80
- **Platform**: Backend + Frontend
- **Dependencies**: none
- **Complexity**: Medium
- **Est. cost**: $0.80
- **Delivers**:
  - Spring Boot 3.x project with Maven, Java 21
  - React 18 + TypeScript + Vite project
  - docker-compose.yml (PostgreSQL, Redis, Mailhog)
  - application.yml / application-dev.yml / application-test.yml
  - .env.example files for backend and frontend
  - BaseEntity, ApiResponse wrapper, GlobalExceptionHandler
  - Flyway V1__init_schema.sql (enable UUID extension)

### Task 1.2: Database + Flyway + Core Entities
- **Status**: ✅ Completed
- **Actual cost**: $0.30
- **Platform**: Backend
- **Dependencies**: 1.1
- **Complexity**: Medium
- **Est. cost**: $0.60
- **Delivers**:
  - PostgreSQL connection config
  - Flyway migration setup
  - BaseEntity abstract class
  - AuditConfig for @CreatedBy/@LastModifiedBy
  - ApiResponse<T> wrapper
  - GlobalExceptionHandler
  - V1__init_schema.sql

### Task 1.3: Security Config + JWT Auth
- **Status**: ✅ Completed
- **Actual cost**: $1.20
- **Platform**: Backend
- **Dependencies**: 1.2
- **Complexity**: Complex
- **Est. cost**: $1.20
- **Delivers**:
  - SecurityConfig.java (URL security mapping)
  - JwtUtil.java (token generation, validation, parsing)
  - JwtAuthFilter.java (OncePerRequestFilter)
  - AdminUserEntity + AdminUserRepository
  - V2__create_auth_tables.sql (admin_users table)
  - AuthController (login, logout, refresh)
  - AuthService (authenticate, logout, refresh logic)
  - LoginRequest/LoginResponse DTOs
  - Redis config for JWT blacklist + refresh tokens
  - Rate limiting service (Redis-based)
  - CORS config
  - Swagger/OpenAPI config
  - Seed default admin user on startup
- **Endpoints**:
  - POST /api/auth/login
  - POST /api/auth/refresh
  - POST /api/auth/logout
- **Tables**: admin_users
- **Tests**: 12 scenarios (login success, invalid creds, locked IP, logout, refresh, expired token, wrong role)

### Task 1.4: File Upload Service (S3)
- **Status**: ✅ Completed
- **Actual cost**: $0.40
- **Platform**: Backend
- **Dependencies**: 1.3
- **Complexity**: Medium
- **Est. cost**: $0.60
- **Delivers**:
  - S3Config.java
  - FileStorageService.java (presigned URL generation)
  - UploadController (POST /api/admin/upload/presigned-url)
  - UploadRequest/UploadResponse DTOs
  - File validation (type, size)
- **Endpoints**:
  - POST /api/admin/upload/presigned-url
- **Tests**: 6 scenarios (valid upload, wrong format, too large, unauthorized)

### Task 1.5: Auth Frontend — Login Page + AuthGuard
- **Status**: ✅ Completed
- **Actual cost**: $0.00 (merged with 1.3)
- **Platform**: Frontend
- **Dependencies**: 1.3
- **Complexity**: Medium
- **Est. cost**: $0.80
- **Delivers**:
  - Vite + React project scaffold (if not in 1.1)
  - Tailwind config with DESIGN.md tokens
  - Redux store + RTK Query base setup
  - authApi (login, logout, refresh RTK Query endpoints)
  - authSlice (token storage, user info)
  - LoginPage.tsx (page 1 design)
  - AuthGuard.tsx component
  - RoleGuard.tsx component (ADMIN vs EDITOR)
  - Token management (localStorage + auto-refresh)
  - React Router setup with public + admin routes
- **Pages**: /admin/login
- **Tests**: 8 scenarios (render, submit, validation, error states, redirect)

### Task 1.6: Admin Layout Shell (Frontend)
- **Status**: ✅ Completed
- **Actual cost**: $0.60
- **Platform**: Frontend
- **Dependencies**: 1.5
- **Complexity**: Medium
- **Est. cost**: $0.80
- **Delivers**:
  - AdminLayout.tsx (sidebar + top bar + content area)
  - Sidebar.tsx (nav items, support, logout, user profile)
  - TopBar.tsx (role badge, search placeholder, icons)
  - Responsive behavior (desktop/tablet/mobile)
  - Active nav state management
  - Logout functionality
  - All shared admin layout components
- **Pages**: Shared layout for all /admin/* pages
- **Tests**: 6 scenarios (render, nav, responsive, logout)

---

## Phase 2 — Dashboard Module

### Task 2.1: Dashboard Backend
- **Status**: ✅ Completed
- **Actual cost**: $0.60
- **Platform**: Backend
- **Dependencies**: 1.3
- **Complexity**: Medium
- **Est. cost**: $0.80
- **Delivers**:
  - DashboardController (4 endpoints)
  - DashboardService (summary, access logs, performance, security)
  - AccessLogEntity + AccessLogRepository
  - NodePerformanceEntity + NodePerformanceRepository
  - SecurityStatusEntity + SecurityStatusRepository
  - V3__create_dashboard_tables.sql
  - Dashboard DTOs
- **Endpoints**:
  - GET /api/admin/dashboard/summary
  - GET /api/admin/dashboard/access-logs
  - GET /api/admin/dashboard/node-performance
  - GET /api/admin/dashboard/security-status
- **Tables**: access_logs, node_performance, security_status
- **Tests**: 8 scenarios

### Task 2.2: Dashboard Frontend
- **Status**: ✅ Completed
- **Actual cost**: $0.60
- **Platform**: Frontend
- **Dependencies**: 1.6, 2.1
- **Complexity**: Medium
- **Est. cost**: $0.80
- **Delivers**:
  - DashboardPage.tsx (page 2 design)
  - StatCard.tsx component
  - AccessLogList.tsx component
  - NodePerformancePanel.tsx component
  - SecurityStatusPanel.tsx component
  - dashboardApi RTK Query endpoints
  - Skeleton loading states
  - Refresh Data functionality
  - EDITOR masked usernames logic
- **Pages**: /admin/dashboard
- **Tests**: 8 scenarios

---

## Phase 3 — Blog Module

### Task 3.1: Blog Backend — Entities + Admin CRUD
- **Status**: ⏳ Pending
- **Actual cost**: —
- **Platform**: Backend
- **Dependencies**: 1.3, 1.4
- **Complexity**: Complex
- **Est. cost**: $1.20
- **Delivers**:
  - BlogEntity, BlogCategoryEntity, BlogTagEntity, BlogGalleryImageEntity
  - BlogRepository, BlogCategoryRepository, BlogTagRepository
  - BlogService (CRUD, publish, feature, auto-save, slug generation)
  - BlogAdminController
  - BlogCategoryAdminController
  - BlogTagAdminController
  - V3__create_blog_tables.sql
  - All DTOs (request + response)
  - MapStruct mappers
- **Endpoints**:
  - GET /api/admin/blogs (paginated + filtered)
  - GET /api/admin/blogs/:id
  - POST /api/admin/blogs
  - PUT /api/admin/blogs/:id
  - PATCH /api/admin/blogs/:id
  - PATCH /api/admin/blogs/:id/publish
  - PATCH /api/admin/blogs/:id/order
  - DELETE /api/admin/blogs/:id
  - GET /api/admin/blogs/insights
  - GET /api/admin/blog-categories
  - GET /api/admin/blog-tags
- **Tables**: blogs, blog_categories, blog_tags, blog_tag_map, blog_gallery_images
- **Tests**: 18 scenarios

### Task 3.2: Blog Backend — Public Read Endpoints
- **Status**: ⏳ Pending
- **Actual cost**: —
- **Platform**: Backend
- **Dependencies**: 3.1
- **Complexity**: Simple
- **Est. cost**: $0.40
- **Delivers**:
  - BlogPublicController
  - Public DTOs (no drafts, no admin fields)
  - Redis caching (blog list 10min, single blog 30min)
  - Cache invalidation on write
- **Endpoints**:
  - GET /api/public/blogs (paginated, published only)
  - GET /api/public/blogs/:slug
  - GET /api/public/blog-categories
- **Tests**: 6 scenarios

### Task 3.3: Blog Frontend — Blog List Page
- **Status**: ⏳ Pending
- **Actual cost**: —
- **Platform**: Frontend
- **Dependencies**: 1.6, 3.1
- **Complexity**: Complex
- **Est. cost**: $1.00
- **Delivers**:
  - BlogListPage.tsx (page 3 design)
  - BlogTable.tsx component
  - BlogFilterTabs.tsx component
  - BlogInsightCards.tsx component
  - Pagination.tsx shared component
  - StatusBadge.tsx shared component
  - CategoryChip.tsx shared component
  - blogApi RTK Query endpoints
  - URL-synced filter state
  - Delete confirmation modal
  - Empty states
- **Pages**: /admin/blogs
- **Tests**: 10 scenarios

### Task 3.4: Blog Frontend — Blog Editor Page
- **Status**: ⏳ Pending
- **Actual cost**: —
- **Platform**: Frontend
- **Dependencies**: 3.3
- **Complexity**: Complex
- **Est. cost**: $1.20
- **Delivers**:
  - BlogEditorPage.tsx (page 5 design)
  - TipTap rich text editor setup
  - RichTextToolbar.tsx
  - PublishingSettings.tsx (right panel)
  - PostMetadata.tsx (tags + author)
  - MediaAssets.tsx (hero + gallery)
  - S3UploadCard.tsx (presigned URL upload flow)
  - AutoSaveManager hook
  - SearchPreview.tsx
  - Featured post conflict modal
- **Pages**: /admin/blogs/new, /admin/blogs/:id/edit
- **Tests**: 12 scenarios

### Task 3.5: Blog Insights Backend
- **Status**: ⏳ Pending
- **Actual cost**: —
- **Platform**: Backend
- **Dependencies**: 3.1
- **Complexity**: Medium
- **Est. cost**: $0.80
- **Delivers**:
  - BlogInsightsController (4 endpoints)
  - BlogInsightsService (aggregation queries)
  - PageViewEntity + PageViewRepository
  - V6__create_page_views_table.sql
  - PageViewTrackingService (record views from public endpoints)
  - Insights DTOs
- **Endpoints**:
  - GET /api/admin/blogs/:id/insights/summary
  - GET /api/admin/blogs/:id/insights/geo
  - GET /api/admin/blogs/:id/insights/access-log
  - GET /api/admin/blogs/:id/insights/engagement
- **Tables**: page_views
- **Tests**: 8 scenarios

### Task 3.6: Blog Insights Frontend
- **Status**: ⏳ Pending
- **Actual cost**: —
- **Platform**: Frontend
- **Dependencies**: 3.3, 3.5
- **Complexity**: Complex
- **Est. cost**: $1.00
- **Delivers**:
  - BlogInsightsPage.tsx (page 4 design)
  - KpiStatCard.tsx shared component
  - GeoDistributionPanel.tsx (map + country table)
  - AccessLogPanel.tsx
  - EngagementTrendChart.tsx (Recharts line chart)
  - SystemStatusFooter.tsx
  - blogInsightsApi RTK Query endpoints
- **Pages**: /admin/blogs/:blogId/insights
- **Tests**: 8 scenarios

---

## Phase 4 — Case Study Module

### Task 4.1: Case Study Backend — Entities + Admin CRUD
- **Status**: ⏳ Pending
- **Actual cost**: —
- **Platform**: Backend
- **Dependencies**: 1.3, 1.4
- **Complexity**: Complex
- **Est. cost**: $1.20
- **Delivers**:
  - CaseStudyEntity, CaseStudyCategoryEntity, CaseStudyTagEntity
  - CaseStudyResultEntity, CaseStudyComparisonEntity
  - All Repositories
  - CaseStudyService (CRUD, publish, feature, results, bulk actions)
  - CaseStudyAdminController
  - CaseStudyCategoryAdminController
  - CaseStudyTagAdminController
  - V4__create_case_study_tables.sql
  - All DTOs + MapStruct mappers
- **Endpoints**:
  - GET /api/admin/case-studies (paginated + filtered + searchable)
  - GET /api/admin/case-studies/:id
  - POST /api/admin/case-studies
  - PUT /api/admin/case-studies/:id
  - PATCH /api/admin/case-studies/:id
  - PATCH /api/admin/case-studies/:id/publish
  - PATCH /api/admin/case-studies/:id/unpublish
  - PATCH /api/admin/case-studies/:id/feature
  - PATCH /api/admin/case-studies/:id/results
  - DELETE /api/admin/case-studies/:id
  - POST /api/admin/case-studies/bulk
  - GET /api/admin/case-study-categories
  - GET /api/admin/case-study-industries
  - GET /api/admin/tags?type=case-study
  - POST /api/admin/tags
- **Tables**: case_studies, case_study_categories, case_study_tags, case_study_tag_map, case_study_results, case_study_comparisons
- **Tests**: 20 scenarios

### Task 4.2: Case Study Backend — Public Read Endpoints
- **Status**: ⏳ Pending
- **Actual cost**: —
- **Platform**: Backend
- **Dependencies**: 4.1
- **Complexity**: Simple
- **Est. cost**: $0.40
- **Delivers**:
  - CaseStudyPublicController
  - Public DTOs (published only, with results + comparisons)
  - Redis caching (list 10min, single 30min)
  - Cache invalidation on write
- **Endpoints**:
  - GET /api/public/case-studies
  - GET /api/public/case-studies/:slug
  - GET /api/public/case-study-categories
- **Tests**: 6 scenarios

### Task 4.3: Case Study Frontend — List Page
- **Status**: ⏳ Pending
- **Actual cost**: —
- **Platform**: Frontend
- **Dependencies**: 1.6, 4.1
- **Complexity**: Complex
- **Est. cost**: $1.00
- **Delivers**:
  - CaseStudyListPage.tsx (page 6 design)
  - CaseStudyTable.tsx
  - CaseStudyFilters.tsx (search + status + industry + category)
  - BulkActionBar.tsx (ADMIN only)
  - FeaturedStar.tsx toggle
  - caseStudyApi RTK Query endpoints
  - URL-synced filter + pagination
  - Delete + featured confirmation modals
  - Empty states
- **Pages**: /admin/case-studies
- **Tests**: 10 scenarios

### Task 4.4: Case Study Frontend — Editor Page
- **Status**: ⏳ Pending
- **Actual cost**: —
- **Platform**: Frontend
- **Dependencies**: 4.3
- **Complexity**: Complex
- **Est. cost**: $1.20
- **Delivers**:
  - CaseStudyEditorPage.tsx (page 8 design)
  - ProjectMetadata.tsx (industry, customer, country, city, tags)
  - ResultMetricCards.tsx (3 editable cards)
  - ComparisonTable.tsx (before/after, add/remove rows)
  - ArchitectureDiagramUpload.tsx
  - Reuses: TipTap editor, AutoSaveManager, PublishingSettings
- **Pages**: /admin/case-studies/new, /admin/case-studies/:id/edit
- **Tests**: 12 scenarios

### Task 4.5: Case Study Insights Backend
- **Status**: ⏳ Pending
- **Actual cost**: —
- **Platform**: Backend
- **Dependencies**: 4.1, 3.5
- **Complexity**: Complex
- **Est. cost**: $1.00
- **Delivers**:
  - CaseStudyInsightsController (6 endpoints)
  - CaseStudyInsightsService
  - InsightShareEntity + InsightShareRepository
  - V7__create_insight_shares_table.sql
  - PDF export service (using iText or OpenPDF)
  - Share link generation service
- **Endpoints**:
  - GET /api/admin/case-studies/:id/insights/summary
  - GET /api/admin/case-studies/:id/insights/engagement
  - GET /api/admin/case-studies/:id/insights/geo
  - GET /api/admin/case-studies/:id/insights/access-log
  - POST /api/admin/case-studies/:id/insights/export-pdf
  - POST /api/admin/case-studies/:id/insights/share
- **Tables**: insight_shares (page_views reused from Phase 3)
- **Tests**: 10 scenarios

### Task 4.6: Case Study Insights Frontend
- **Status**: ⏳ Pending
- **Actual cost**: —
- **Platform**: Frontend
- **Dependencies**: 4.3, 4.5
- **Complexity**: Complex
- **Est. cost**: $1.00
- **Delivers**:
  - CaseStudyInsightsPage.tsx (page 7 design)
  - EngagementBarChart.tsx (Recharts bar chart)
  - GeographicReachPanel.tsx (map + progress bars)
  - AccessLogTable.tsx (full-width table with status chips)
  - ExportPdfButton.tsx
  - ShareReportButton.tsx
  - Breadcrumb.tsx shared component
  - caseStudyInsightsApi RTK Query endpoints
- **Pages**: /admin/case-studies/:id/insights
- **Tests**: 8 scenarios

---

## Phase 5 — Integration & Polish

### Task 5.1: Async + Email Config
- **Status**: ⏳ Pending
- **Actual cost**: —
- **Platform**: Backend
- **Dependencies**: 1.2
- **Complexity**: Medium
- **Est. cost**: $0.60
- **Delivers**:
  - AsyncConfig.java (@EnableAsync, @EnableScheduling)
  - EmailConfig.java (JavaMailSender, Thymeleaf templates)
  - emailTaskExecutor thread pool
  - EmailService base class
  - Mailhog integration in docker-compose
  - Email templates directory structure
- **Note**: Prepares infrastructure for consultation + contact modules (pages 9-14)

### Task 5.2: Navigation Wiring + Route Setup + Smoke Test
- **Status**: ⏳ Pending
- **Actual cost**: —
- **Platform**: Frontend
- **Dependencies**: 2.2, 3.3, 3.4, 3.6, 4.3, 4.4, 4.6
- **Complexity**: Simple
- **Est. cost**: $0.40
- **Delivers**:
  - Complete React Router config (all routes)
  - Sidebar navigation active state synced with routes
  - 404 Not Found page
  - Redirect logic (login → dashboard, unauthenticated → login)
  - E2E smoke test across all pages

---

## Cost Summary

| Phase | Tasks | Backend | Frontend | Est. Cost |
|-------|-------|---------|----------|-----------|
| Phase 1 — Foundation | 6 | 4 | 2 | $4.80 |
| Phase 2 — Dashboard | 2 | 1 | 1 | $1.60 |
| Phase 3 — Blog | 6 | 3 | 3 | $5.60 |
| Phase 4 — Case Study | 6 | 3 | 3 | $5.80 |
| Phase 5 — Integration | 2 | 1 | 1 | $1.00 |
| **Total** | **22** | **12** | **10** | **$18.80** |

**Budget**: $20.00
**Estimated spend**: $18.80
**Buffer**: $1.20

---

## Task Dependency Chain

```
1.1 → 1.2 → 1.3 → 1.4
                 ↓
              1.5 → 1.6
                      ↓
              2.1 → 2.2
              3.1 → 3.2
              3.1 → 3.3 → 3.4
              3.1 → 3.5 → 3.6
              4.1 → 4.2
              4.1 → 4.3 → 4.4
              4.1 + 3.5 → 4.5 → 4.6
              1.2 → 5.1
              All frontend → 5.2
```
