# Plan: Blog Module

**Module**: blog
**Generated**: 29/03/2026 at 14:00

---

## Implementation Approach

### Package Structure
```
com.mfra.website/
└── module/
    └── blog/
        ├── entity/
        │   ├── BlogEntity.java
        │   ├── BlogCategoryEntity.java
        │   ├── BlogTagEntity.java
        │   ├── BlogGalleryImageEntity.java
        │   └── PageViewEntity.java
        ├── repository/
        │   ├── BlogRepository.java
        │   ├── BlogCategoryRepository.java
        │   ├── BlogTagRepository.java
        │   ├── BlogGalleryImageRepository.java
        │   └── PageViewRepository.java
        ├── service/
        │   ├── BlogService.java
        │   ├── BlogPublicService.java
        │   ├── BlogCategoryService.java
        │   ├── BlogInsightsService.java
        │   └── PageViewTrackingService.java
        ├── controller/
        │   ├── BlogAdminController.java
        │   ├── BlogPublicController.java
        │   ├── BlogCategoryAdminController.java
        │   ├── BlogTagAdminController.java
        │   └── BlogInsightsController.java
        ├── dto/
        │   ├── BlogCreateRequest.java
        │   ├── BlogUpdateRequest.java
        │   ├── BlogAutoSaveRequest.java
        │   ├── BlogResponse.java
        │   ├── BlogListResponse.java
        │   ├── BlogPublicResponse.java
        │   ├── BlogCategoryResponse.java
        │   ├── BlogTagResponse.java
        │   ├── BlogInsightsSummaryResponse.java
        │   ├── BlogInsightsGeoResponse.java
        │   ├── BlogInsightsAccessLogResponse.java
        │   └── BlogInsightsEngagementResponse.java
        └── mapper/
            └── BlogMapper.java
```

### Layer Decisions
- **Slug generation**: Service layer — auto-generated from title, unique collision handling
- **Reading time**: Calculated in service layer from word count (200 wpm)
- **Featured enforcement**: Service layer — un-feature previous on new feature toggle
- **Auto-save**: PATCH endpoint supports partial fields — null fields not updated
- **Cache strategy**: @Cacheable-style manual Redis on public GET endpoints, invalidate on write
- **Page views**: Tracked by PageViewTrackingService on public slug endpoint, append-only
- **Insights**: Aggregation queries on page_views table by resource_type='BLOG'
- **Auth**: Admin endpoints use @PreAuthorize per users.md rules

### Key Service Methods

**BlogService:**
- `create(request)` → validate → generate unique slug → calculate reading time → save as DRAFT → invalidate cache
- `update(id, request)` → find + validate → update fields → recalculate slug if title changed → invalidate cache
- `autoSave(id, request)` → find → update only non-null fields → save → invalidate cache
- `publish(id)` → validate required fields → set PUBLISHED + publishedAt → invalidate public cache
- `delete(id)` → soft delete → invalidate cache
- `toggleFeatured(id)` → un-feature current featured → feature this one → invalidate cache
- `updateOrder(id, newOrder)` → update display_order → invalidate cache

**BlogPublicService:**
- `list(filters, pageable)` → Redis cache → DB fallback → store in cache
- `getBySlug(slug)` → Redis cache → DB fallback → track page view → store in cache

**BlogInsightsService:**
- `getSummary(blogId)` → aggregate page_views (total, avg_read_time, bounce_rate, consultation_conversion)
- `getGeo(blogId)` → GROUP BY country, top 5
- `getAccessLog(blogId)` → recent 8 page_views
- `getEngagement(blogId, period)` → daily/weekly counts for chart

### Frontend Approach
- **State**: RTK Query — blogApi with tagTypes ['Blog', 'BlogCategory', 'BlogTag']
- **Forms**: react-hook-form + zod for blog editor validation
- **Rich text**: TipTap with StarterKit + Image + Link extensions
- **Auto-save**: Custom useAutoSave hook — debounce 3s, min interval 60s
- **Admin route guard**: All admin pages wrapped in AuthGuard
- **Charts**: Recharts for engagement trend (line chart on insights page)

---

## Cache Keys

| Key Pattern | TTL | Invalidated By |
|-------------|-----|----------------|
| blogs:public:list:{hash} | 10 min | create, update, delete, publish |
| blogs:public:{slug} | 30 min | update, delete, publish |
| blogs:categories:public | 1 hour | category create/update/delete |

---

## Dependencies

- Depends on: auth module (JWT, SecurityContext), upload module (S3 presigned URLs)
- Required config: Cache (Redis), SecurityConfig
- Flyway: V3 (blog tables) must run after V2 (admin_users FK reference)
- Flyway: V6 (page_views) must run after V3
