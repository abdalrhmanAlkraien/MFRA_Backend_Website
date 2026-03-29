# Requirements: 5 — Blog Post Editor

**Design**: designs/5.png
**Spec**: designs/5.md
**Analyzed**: 29/03/2026 at 12:00
**URL**: /admin/blogs/new (create) | /admin/blogs/:id/edit (edit)
**Access**: ADMIN (full), EDITOR (restricted — no archive, no featured, no priority, no author change)
**Primary action**: Create or edit a blog post with rich text editor

---

## Design System Notes
File: designs/DESIGN.md

Tokens used on this page:
  Title input: font-display large, transparent bg
  Editor area: surface_container_lowest (recessed)
  Toolbar: surface_container_high
  Right panel cards: surface_container_high
  Publishing dropdown: surface_container_lowest
  Featured toggle: primary_container when on
  Auto-save dot: primary_container pulsing
  Media cards: surface_container bg
  Hero badge: primary_container bg
  Discard: tertiary text button
  Publish: primary gradient button

---

## Sections

| # | Section | Type | Data Source |
|---|---------|------|-------------|
| 1 | Blog Title input | Interactive | User input |
| 2 | Content Type + Category dropdowns | Interactive | API categories |
| 3 | Rich Text Toolbar | Interactive | TipTap controls |
| 4 | Rich Text Editor | Interactive | TipTap |
| 5 | Auto-save status bar | Dynamic | Save status |
| 6 | Action buttons (Discard + Publish) | Interactive | API calls |
| 7 | Media Assets (Hero + Gallery) | Interactive | S3 upload |
| 8 | Publishing Status dropdown | Interactive | Status change |
| 9 | Featured Post toggle | Interactive | ADMIN only |
| 10 | Display Priority input | Interactive | ADMIN only |
| 11 | Post Metadata (Category + Author) | Interactive | Tags + author |
| 12 | Search Preview | Dynamic | Auto-generated from title |

---

## Database Requirements

Uses `blogs`, `blog_categories`, `blog_tags`, `blog_tag_map`, `blog_gallery_images` tables defined in 3-requirements.md.

No new tables required for this page.

---

## API Endpoints

| Method | Path | Auth | Description | Cached | Rate Limited |
|--------|------|------|-------------|--------|--------------|
| GET | /api/admin/blogs/:id | ADMIN or EDITOR | Load blog for editing | No | No |
| POST | /api/admin/blogs | ADMIN or EDITOR | Create new blog | No | No |
| PATCH | /api/admin/blogs/:id | ADMIN or EDITOR | Auto-save partial update | No | No |
| PUT | /api/admin/blogs/:id | ADMIN or EDITOR | Full update | No | No |
| PATCH | /api/admin/blogs/:id/publish | ADMIN or EDITOR | Publish blog | No | No |
| DELETE | /api/admin/blogs/:id | ADMIN only | Soft delete (discard) | No | No |
| GET | /api/admin/blog-categories | ADMIN or EDITOR | Category options | No | No |
| GET | /api/admin/blog-tags | ADMIN or EDITOR | Tag autocomplete | No | No |
| POST | /api/admin/upload/presigned-url | ADMIN or EDITOR | Get S3 presigned URL | No | No |

### POST /api/admin/blogs
```
Request: { title, content, content_type, category_id, status, tag_ids }
Response: { success: true, data: { id, slug, ... } }
```

### POST /api/admin/upload/presigned-url
```
Request: { filename, content_type, folder: "blogs" }
Response: { presigned_url, public_url, key }
```

---

## Business Rules

1. Title required before publish — inline error if empty
2. Content required before publish
3. Category required before publish
4. Auto-save: debounce 3 seconds, minimum 60-second interval
5. Only one FEATURED post at a time — warn and unfeature previous
6. Slug auto-generated from title (backend), collision → append -2, -3
7. Display Priority 1 = highest, 10 = lowest
8. EDITOR cannot archive, feature, set priority, or change author
9. Images upload to S3 directly via pre-signed URL
10. Hero image optional — can publish without it
11. Maximum 10 gallery images per article
12. Create mode: first auto-save fires POST, subsequent fires PATCH
13. Reading time auto-calculated: word_count / 200

---

## Clarifications

None — design was clear.

---

## Out of Scope

- Version history
- Collaborative editing
- Scheduled publishing
- Post preview
- Custom slug editing
- SEO meta override
- Video upload
- Markdown raw mode toggle
