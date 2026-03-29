# [Page Name]

> Copy this file, rename it to match your image number (e.g. 1.md, 2.md),
> and fill in each section below. Place it next to the image file.
>
> Example:
>   designs/1.png   ← your design image
>   designs/1.md    ← this file filled in
>
> Public or admin access is defined in the "User Access" section below —
> not by folder structure.

---

## Business Description

What does this page do?
What problem does it solve for the user?
What is the primary goal of this page?

[Write 2–5 sentences here]

---

## User Access

Who can see and use this page:

- [ ] PUBLIC — any visitor, no login required
- [ ] ADMIN — logged-in admin user only
- [ ] EDITOR — logged-in editor user only
- [ ] ADMIN + EDITOR — both roles

---

## Permissions

What the user on this page can do:

**View:**
- [describe what data is visible on this page]

**Create:**
- [describe what the user can submit or create — or write "None"]

**Edit:**
- [describe what can be modified — or write "None"]

**Delete:**
- [describe what can be removed — or write "None"]

**Other actions:**
- [publish, unpublish, reorder, export, toggle, upload, etc. — or write "None"]

---

## Related Pages

Pages that link to or from this page:

- [page name or image number] → [why it is related, e.g. "clicking a card opens this page"]
- [page name or image number] → [why it is related]

Write "None" if this page is standalone.

---

## Notes

Any additional context the agent needs to know:

**Special behavior:**
- [anything not obvious from the design]

**Business rules:**
- [rules the agent needs to enforce that may not be visible in the design]

**Edge cases:**
- [unusual situations the page must handle]

**Out of scope:**
- [things that look like they might be on this page but should NOT be implemented]
- [features deferred to a future version]

---

## Example — Filled In

Here is an example of a correctly filled template:

```
# Blog List Page

## Business Description
This page shows all published blog articles. Visitors can read articles
about AWS, migration, and AI topics. It is the main content discovery
page for the public website.

## User Access
- [x] PUBLIC — any visitor, no login required

## Permissions
View:   All published blog articles, category filter bar
Create: None
Edit:   None
Delete: None
Other:  None

## Related Pages
- Blog Article page (2.md) → clicking a blog card opens the article detail
- Admin Blog List (admin/1.md) → admin manages blogs there

## Notes
Special behavior:
  - Filter bar is sticky — stays visible when user scrolls down
  - Selecting a filter updates the URL param (?category=migration)
    so the filtered view can be bookmarked

Business rules:
  - Only PUBLISHED blogs are shown — drafts are never visible
  - Featured article shown at the top in a larger card format
  - Regular articles shown in a 3-column grid below

Edge cases:
  - If a selected category has no published blogs, show empty state
  - If API fails, show error message — do not show blank page

Out of scope:
  - User comments — not in this version
  - Blog search bar — deferred to next version
  - View count tracking — deferred to next version
```