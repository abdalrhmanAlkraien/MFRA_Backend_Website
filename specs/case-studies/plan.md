# Plan: Case Studies Module

**Module**: case-studies
**Generated**: 29/03/2026 at 14:00

---

## Implementation Approach

### Package Structure
```
com.mfra.website/
└── module/
    └── casestudy/
        ├── entity/
        │   ├── CaseStudyEntity.java
        │   ├── CaseStudyCategoryEntity.java
        │   ├── CaseStudyTagEntity.java
        │   ├── CaseStudyResultEntity.java
        │   ├── CaseStudyComparisonEntity.java
        │   └── InsightShareEntity.java
        ├── repository/
        │   ├── CaseStudyRepository.java
        │   ├── CaseStudyCategoryRepository.java
        │   ├── CaseStudyTagRepository.java
        │   ├── CaseStudyResultRepository.java
        │   ├── CaseStudyComparisonRepository.java
        │   └── InsightShareRepository.java
        ├── service/
        │   ├── CaseStudyService.java
        │   ├── CaseStudyPublicService.java
        │   ├── CaseStudyCategoryService.java
        │   ├── CaseStudyInsightsService.java
        │   ├── PdfExportService.java
        │   └── ShareLinkService.java
        ├── controller/
        │   ├── CaseStudyAdminController.java
        │   ├── CaseStudyPublicController.java
        │   ├── CaseStudyCategoryAdminController.java
        │   ├── CaseStudyTagAdminController.java
        │   └── CaseStudyInsightsController.java
        ├── dto/
        │   ├── CaseStudyCreateRequest.java
        │   ├── CaseStudyUpdateRequest.java
        │   ├── CaseStudyAutoSaveRequest.java
        │   ├── CaseStudyResponse.java
        │   ├── CaseStudyListResponse.java
        │   ├── CaseStudyPublicResponse.java
        │   ├── CaseStudyResultRequest.java
        │   ├── CaseStudyResultResponse.java
        │   ├── CaseStudyComparisonRequest.java
        │   ├── CaseStudyComparisonResponse.java
        │   ├── BulkActionRequest.java
        │   ├── CaseStudyCategoryResponse.java
        │   ├── CaseStudyTagResponse.java
        │   ├── CaseStudyInsightsSummaryResponse.java
        │   ├── CaseStudyInsightsEngagementResponse.java
        │   ├── CaseStudyInsightsGeoResponse.java
        │   ├── CaseStudyInsightsAccessLogResponse.java
        │   └── ShareLinkResponse.java
        └── mapper/
            └── CaseStudyMapper.java
```

### Layer Decisions
- **Slug generation**: Service layer — auto-generated from title, unique collision handling
- **Reading time**: Calculated in service from word count (200 wpm)
- **Featured enforcement**: Service layer — un-feature previous on new feature toggle
- **Results/Comparisons**: Saved atomically via dedicated PATCH endpoint
- **Auto-save**: PATCH endpoint supports partial fields
- **Bulk actions**: Service processes list of IDs, returns success/failure per ID
- **Cache strategy**: Manual Redis on public GET endpoints, invalidate on write
- **PDF export**: OpenPDF library — server-side generation, return as byte stream
- **Share links**: UUID-based token with 7-day TTL, stored in insight_shares table
- **Auth**: Admin endpoints use @PreAuthorize per users.md rules

### Key Service Methods

**CaseStudyService:**
- `create(request)` → validate → generate unique slug → save as DRAFT → invalidate cache
- `update(id, request)` → find → update → recalculate slug/reading time → invalidate cache
- `autoSave(id, request)` → partial update → save → invalidate cache
- `publish(id)` → validate required fields (title, content, category, industry, 3 metrics) → set PUBLISHED → invalidate cache
- `unpublish(id)` → set DRAFT → clear publishedAt → invalidate cache
- `delete(id)` → soft delete → invalidate cache
- `toggleFeatured(id)` → un-feature current → feature this → invalidate cache
- `saveResults(id, request)` → replace all metrics + comparisons atomically → invalidate cache
- `bulkAction(request)` → process each ID, collect results → invalidate cache

**CaseStudyPublicService:**
- `list(filters, pageable)` → Redis cache → DB (published only, with results + comparisons)
- `getBySlug(slug)` → Redis cache → DB → track page view

**CaseStudyInsightsService:**
- `getSummary(caseStudyId)` → aggregate page_views (total, avg_read_time, conversion_rate, share_count)
- `getEngagement(caseStudyId, period)` → daily bar chart data
- `getGeo(caseStudyId)` → top 5 countries
- `getAccessLog(caseStudyId)` → 10 recent entries

**PdfExportService:**
- `exportInsightsPdf(caseStudyId)` → generate PDF with KPIs, chart data, geo → return byte[]

**ShareLinkService:**
- `createShareLink(caseStudyId, userId)` → generate token, set expiry 7 days → save → return URL
- `getSharedInsights(token)` → validate token + expiry → return insights data

### Frontend Approach
- **State**: RTK Query — caseStudyApi with tagTypes ['CaseStudy', 'CaseStudyCategory', 'CaseStudyTag']
- **Forms**: react-hook-form + zod for editor validation
- **Rich text**: Reuses TipTap setup from blog module
- **Auto-save**: Reuses useAutoSave hook from blog module
- **Results editor**: Custom ResultMetricCards and ComparisonTable components
- **Charts**: Recharts for engagement bar chart
- **PDF export**: Download from API response blob
- **Share**: Copy link to clipboard with toast notification

---

## Cache Keys

| Key Pattern | TTL | Invalidated By |
|-------------|-----|----------------|
| casestudies:public:list:{hash} | 10 min | create, update, delete, publish, bulk |
| casestudies:public:{slug} | 30 min | update, delete, publish |
| casestudies:categories:public | 1 hour | category create/update/delete |

---

## Dependencies

- Depends on: auth module (JWT), upload module (S3), blog module (PageViewEntity shared)
- Required config: Cache (Redis), SecurityConfig
- Flyway: V4 (case study tables) must run after V2 (admin_users)
- Flyway: V7 (insight_shares) must run after V4
- PDF library: OpenPDF (open-source fork of iText)
