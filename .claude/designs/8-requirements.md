# Requirements: 8 — Case Study Editor

**Design**: designs/8.png
**Spec**: designs/8.md
**Analyzed**: 29/03/2026 at 12:00
**URL**: /admin/case-studies/new (create) | /admin/case-studies/:id/edit (edit)
**Access**: ADMIN (full), EDITOR (restricted — no archive, no featured, no priority, no delete)
**Primary action**: Create or edit a case study with rich text, results, and metadata

---

## Design System Notes
File: designs/DESIGN.md

Tokens used on this page:
  Title input: font-display large, transparent bg
  Editor area: surface_container_lowest (recessed)
  Right panel: surface_container_high for metadata card
  Result metric cards: left border accent (primary_container / tertiary_container)
  Comparison table: MODERNIZED STATE in primary_container orange
  Architecture diagram: surface_container bg with hover overlay
  Auto-save: green dot + elapsed time
  Publish: primary gradient

---

## Sections

| # | Section | Type | Data Source |
|---|---------|------|-------------|
| 1 | Project Title input | Interactive | User input |
| 2 | Content Type + Category dropdowns | Interactive | API categories |
| 3 | Rich Text Toolbar + Editor | Interactive | TipTap |
| 4 | Auto-save status bar | Dynamic | Save status |
| 5 | Action buttons (Discard + Publish) | Interactive | API calls |
| 6 | Publishing Status dropdown | Interactive | Status change |
| 7 | Featured Case Study toggle | Interactive | ADMIN only |
| 8 | Display Priority | Interactive | ADMIN only |
| 9 | Project Metadata (Industry, Customer, Country, City, Tags) | Interactive | User input + API |
| 10 | Architecture Diagram upload | Interactive | S3 upload |
| 11 | Result Metric Cards (3 cards) | Interactive | Editable inputs |
| 12 | Before/After Comparison Table | Interactive | Editable rows |

---

## Database Requirements

Uses `case_studies`, `case_study_categories`, `case_study_tags`, `case_study_tag_map`,
`case_study_results`, `case_study_comparisons` tables defined in 6-requirements.md.

No new tables required for this page.

---

## API Endpoints

| Method | Path | Auth | Description | Cached | Rate Limited |
|--------|------|------|-------------|--------|--------------|
| GET | /api/admin/case-studies/:id | ADMIN or EDITOR | Load for editing | No | No |
| POST | /api/admin/case-studies | ADMIN or EDITOR | Create new | No | No |
| PATCH | /api/admin/case-studies/:id | ADMIN or EDITOR | Auto-save partial | No | No |
| PUT | /api/admin/case-studies/:id | ADMIN or EDITOR | Full update | No | No |
| PATCH | /api/admin/case-studies/:id/publish | ADMIN or EDITOR | Publish | No | No |
| DELETE | /api/admin/case-studies/:id | ADMIN only | Soft delete | No | No |
| GET | /api/admin/case-study-categories | ADMIN or EDITOR | Category options | No | No |
| GET | /api/admin/case-study-industries | ADMIN or EDITOR | Industry options | No | No |
| GET | /api/admin/tags?type=case-study | ADMIN or EDITOR | Tag autocomplete | No | No |
| POST | /api/admin/tags | ADMIN or EDITOR | Create new tag | No | No |
| PATCH | /api/admin/case-studies/:id/results | ADMIN or EDITOR | Save results section | No | No |
| POST | /api/admin/upload/presigned-url | ADMIN or EDITOR | S3 presigned URL | No | No |

### PATCH /api/admin/case-studies/:id/results
```
Request: {
  metric_cards: [{ label, value, description, color_variant }],
  comparison_rows: [{ metric, legacy_value, modernized_value }]
}
```

---

## Business Rules

1. Title required before publish
2. Content required before publish
3. Category required before publish
4. Industry required before publish
5. All 3 metric cards must be filled before publish
6. Comparison table: all columns per row must be filled (but table is optional)
7. Architecture diagram optional
8. Only one Featured case study at a time
9. EDITOR cannot archive, feature, set priority, or delete
10. Content type defaults to "Case Study" — read-only in this editor
11. Auto-save: debounce 3 seconds, min 60-second interval, elapsed time display
12. Architecture diagram max 10 MB, S3 folder: case-studies/diagrams/
13. Tags shared across case studies
14. Customer name supports anonymized text ("Major European Bank")

---

## Clarifications

None — design was clear.

---

## Out of Scope

- Multiple architecture diagrams
- Video embeds
- Collaborative editing
- Scheduled publishing
- Version history
- Custom slug editing
- More than 3 metric cards
- CSV import
