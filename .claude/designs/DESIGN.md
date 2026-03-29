# Design System Specification: MFRA Cloud Intelligence

> This file is project-specific. The AI agent reads this before writing
> any frontend component, page, or Tailwind class. It is the single source
> of truth for all visual decisions in this project.
>
> When this file exists in designs/DESIGN.md, the agent must read it
> before Step 3 (Extract UI Elements) in the page analysis workflow,
> and before Step 5 (Implementation) in the task execution workflow.

---

## 1. Overview & Creative North Star

**Creative North Star: "The Architectural Void"**

The UI is a vast, dark digital space where information is "illuminated" rather
than just displayed. Intentional asymmetry, deep layering, and high-contrast
typography. The experience should feel like a premium command center: silent,
powerful, and precise.

---

## 2. Colors & Surface Hierarchy

### Color Tokens

| Token | Hex | Usage |
|---|---|---|
| `surface` | `#0d1322` | Base layer — page background |
| `surface_container_lowest` | `#080e1d` | Recessed/inset elements |
| `surface_container_low` | `#151b2b` | Subtle sections |
| `surface_container` | `#191f2f` | Primary content areas, cards |
| `surface_container_high` | `#242a3a` | Elevated cards |
| `surface_container_highest` | `#2f3445` | Active / floating elements |
| `surface_variant` | `#1e2435` | Glassmorphism base (60% opacity) |
| `primary` | `#ffc082` | Primary accent — light orange |
| `primary_container` | `#ff9900` | AWS Orange — buttons, logo |
| `primary_fixed_dim` | `#e8a500` | Tertiary text actions |
| `on_primary_fixed` | `#ffffff` | Text on primary button |
| `on_surface` | `#e8e8f0` | Primary text — near white |
| `on_surface_variant` | `#dbc2ad` | Secondary text — warm gray |
| `outline_variant` | `#554434` | Ghost border (15–20% opacity only) |
| `secondary_container` | `#1e3a4a` | Icon housing background |
| `tertiary_container` | `#38bdf8` | "In Progress" status chips |
| `error` | `#f87171` | Error states |

### Tailwind Custom Classes

Map tokens to Tailwind config (`tailwind.config.ts`):

```typescript
// tailwind.config.ts
export default {
  theme: {
    extend: {
      colors: {
        surface: {
          DEFAULT:  '#0d1322',
          lowest:   '#080e1d',
          low:      '#151b2b',
          base:     '#191f2f',
          high:     '#242a3a',
          highest:  '#2f3445',
          variant:  '#1e2435',
        },
        primary: {
          DEFAULT:  '#ffc082',
          container:'#ff9900',
          dim:      '#e8a500',
        },
        onsurface: {
          DEFAULT:  '#e8e8f0',
          variant:  '#dbc2ad',
        },
        outline: {
          variant:  '#554434',
        },
        secondary: {
          container:'#1e3a4a',
        },
        tertiary: {
          container:'#38bdf8',
        },
      },
      fontFamily: {
        display: ['Manrope', 'sans-serif'],   // headlines + big numbers
        body:    ['Inter', 'sans-serif'],      // body text + card titles
        label:   ['Space Grotesk', 'sans-serif'], // labels, tags, metadata
      },
      backdropBlur: {
        glass: '12px',
      },
    },
  },
}
```

---

## 3. The No-Line Rule — Critical

**Traditional 1px solid borders are strictly prohibited for sectioning.**

Boundaries must be defined through background color shifts only.

```tsx
// ✅ Correct — color shift creates the boundary
<div className="bg-surface">
  <div className="bg-surface-base rounded-xl p-6">
    <div className="bg-surface-high rounded-lg p-4">
      Elevated card content
    </div>
  </div>
</div>

// ❌ Wrong — never use border for sectioning
<div className="border border-gray-700 rounded-xl p-6">
```

**Ghost Border exception** — only when accessibility requires a visible edge:
```tsx
// Use only when color shift is insufficient for accessibility
<div className="rounded-xl p-6" style={{ border: '1px solid rgba(85, 68, 52, 0.18)' }}>
```

---

## 4. Typography System

Three fonts, each with a specific role:

| Role | Font | Tailwind class | Size | Weight | Usage |
|---|---|---|---|---|---|
| Display | Manrope | `font-display` | `text-5xl` / `3.5rem` | `font-bold` | Big numbers, hero headlines |
| Headline | Manrope | `font-display` | `text-2xl` / `1.75rem` | `font-medium` | Section headers |
| Title | Inter | `font-body` | `text-lg` / `1.125rem` | `font-medium` | Card titles, nav items |
| Body | Inter | `font-body` | `text-sm` / `0.875rem` | `font-normal` | Technical text, descriptions |
| Label | Space Grotesk | `font-label` | `text-xs` / `0.75rem` | `font-medium` | Tags, metadata, status chips |

```tsx
// Display — large stats and impact metrics
<span className="font-display text-5xl font-bold tracking-tight text-onsurface">
  1,284
</span>

// Headline — section title
<h2 className="font-display text-2xl font-medium text-onsurface">
  Global Intelligence Overview
</h2>

// Label — technical metadata, status tags
<span className="font-label text-xs font-medium text-onsurface-variant uppercase tracking-widest">
  ADMIN CONTROL CENTER
</span>
```

**Letter spacing rules:**
- Display numbers: `tracking-tight` (-0.02em)
- Labels and metadata: `tracking-widest` (0.1em) — the "technical console" feel
- Headlines: `tracking-normal`

---

## 5. Glassmorphism

Use for: navigation bars, dropdowns, floating modals, hover cards.

```tsx
// Glass element pattern
<div
  className="rounded-xl p-4"
  style={{
    background: 'rgba(30, 36, 53, 0.6)',   // surface_variant at 60%
    backdropFilter: 'blur(12px)',
    WebkitBackdropFilter: 'blur(12px)',
  }}
>
```

**Tailwind + inline style hybrid is required** — Tailwind does not have
a built-in `bg-opacity` that works with `backdrop-filter` in all browsers.

---

## 6. Elevation & Depth — Tonal Layering

Never use drop shadows for depth. Use background color steps:

```tsx
// Recessed — content sinks into the page
<div className="bg-surface-low rounded-xl p-6">
  <div className="bg-surface-lowest rounded-lg p-4">
    Recessed content
  </div>
</div>

// Elevated — content rises above the page
<div className="bg-surface">
  <div className="bg-surface-base rounded-xl p-6">
    <div className="bg-surface-high rounded-lg p-4">
      Elevated card
    </div>
  </div>
</div>
```

**Ambient shadow** — use only for floating modals and popovers:
```tsx
style={{ boxShadow: '0 20px 60px rgba(8, 14, 29, 0.06)' }}
```

---

## 7. Component Patterns

### Primary Button
```tsx
<button
  className="w-full rounded-md px-6 py-3 font-body text-base font-semibold text-white"
  style={{
    background: 'linear-gradient(45deg, #ffc082, #ff9900)',
  }}
>
  Login to Console →
</button>
```

### Secondary Button
```tsx
<button
  className="rounded-md px-6 py-3 bg-surface-highest font-body text-sm font-medium text-primary"
  style={{ border: '1px solid rgba(255, 192, 130, 0.2)' }}
>
  Secondary Action
</button>
```

### Tertiary / Text Button
```tsx
<button className="font-body text-sm text-primary-dim hover:text-primary transition-colors">
  Cancel
</button>
```

### Input Field
```tsx
<div className="space-y-1">
  <label className="font-label text-xs font-medium text-onsurface-variant uppercase tracking-widest">
    Email Address
  </label>
  <input
    className="w-full rounded-md bg-surface-lowest px-4 py-3 font-body text-sm
               text-onsurface placeholder:text-onsurface-variant/40
               outline-none transition-all
               focus:ring-1 focus:ring-primary/20"
  />
</div>
```

### Card
```tsx
// Standard card — color shift only, no border
<div className="bg-surface-base rounded-xl p-6">
  <h3 className="font-body text-lg font-medium text-onsurface">Card Title</h3>
  <p className="font-body text-sm text-onsurface-variant mt-1">Secondary text</p>
</div>
```

### Status Chip
```tsx
// In Progress
<span className="font-label text-xs font-medium px-3 py-1 rounded-full
                 bg-tertiary-container/20 text-tertiary-container">
  In Progress
</span>

// Warning / Active
<span className="font-label text-xs font-medium px-3 py-1 rounded-full
                 bg-primary-container/20 text-primary-container">
  Warning
</span>
```

### Icon Housing (AWS-style)
```tsx
<div className="flex h-10 w-10 items-center justify-center rounded-full
               bg-secondary-container/40">
  <Icon className="h-5 w-5 text-primary" />
</div>
```

### Navigation Item
```tsx
// Active
<div className="flex items-center gap-3 rounded-lg px-4 py-2.5
               bg-surface-highest text-onsurface">
  <Icon className="h-5 w-5 text-primary" />
  <span className="font-body text-sm font-medium">Dashboard</span>
</div>

// Inactive
<div className="flex items-center gap-3 rounded-lg px-4 py-2.5
               text-onsurface-variant hover:bg-surface-high transition-colors cursor-pointer">
  <Icon className="h-5 w-5" />
  <span className="font-body text-sm">Blogs</span>
</div>
```

---

## 8. Spacing Rules

Use generous spacing. When in doubt, add more:

| Context | Tailwind | Rem |
|---|---|---|
| Section padding | `p-12` | 3rem |
| Card padding | `p-6` | 1.5rem |
| Inner card spacing | `p-4` | 1rem |
| Gap between cards | `gap-6` | 1.5rem |
| List item spacing | `space-y-4` | 1rem |
| Tight list items | `space-y-2` | 0.5rem |

---

## 9. Responsive Behavior

All pages are mobile-first. The design is dark-mode-only (never toggle):

| Breakpoint | Width | Layout changes |
|---|---|---|
| Mobile | < 768px | Single column, sidebar hidden/drawer, card full-width |
| Tablet | 768px–1279px | Two columns, sidebar collapses to icons |
| Desktop | 1280px+ | Full sidebar expanded, 3-column grids |

```tsx
// Responsive grid example
<div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">

// Responsive sidebar
<aside className="hidden md:flex md:w-16 lg:w-60 flex-col bg-surface-base">

// Cards that stack on mobile
<div className="flex flex-col gap-4 lg:flex-row">
```

---

## 10. Do's and Don'ts

### Do
- Use `surface` color palette — never pure black `#000000`
- Use `on_surface_variant` (`#dbc2ad`) for secondary text — reduces eye strain
- Use `font-display` (Manrope) for large numbers and headlines
- Use `font-label` (Space Grotesk) for tags, metadata, uppercase labels
- Use tonal layering for depth — background color shifts, not shadows
- Use glassmorphism for floating UI (nav bar, dropdowns, modals)
- Use generous negative space — `p-12` on sections, `p-6` on cards

### Don't
- Never use `border` for sectioning — use color shifts
- Never use `#000000` — always use `surface` (#0d1322) as darkest
- Never use standard `drop-shadow` — use tonal layering or ambient shadow only
- Never use sharp corners — minimum `rounded-md` (0.375rem), prefer `rounded-xl`
- Never use white text for everything — secondary text uses `on_surface_variant`
- Never use `border-gray-*` Tailwind colors — use the `outline_variant` token
- Never mix font families randomly — each font has a fixed role above

---

## 11. Google Fonts Import

Add to `index.html` or global CSS:

```html
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700;800&family=Inter:wght@300;400;500;600&family=Space+Grotesk:wght@400;500;600;700&display=swap" rel="stylesheet">
```

```css
/* globals.css */
:root {
  --font-display: 'Manrope', sans-serif;
  --font-body:    'Inter', sans-serif;
  --font-label:   'Space Grotesk', sans-serif;
}

body {
  background-color: #0d1322;
  color: #e8e8f0;
  font-family: var(--font-body);
  -webkit-font-smoothing: antialiased;
}
```