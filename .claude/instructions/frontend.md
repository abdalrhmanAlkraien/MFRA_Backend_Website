# Frontend Coding Instructions

> This file is reusable across all projects.
> The AI agent must read this file before writing any frontend code.
> Project-specific details (routes, pages, features) are in `.claude/CLAUDE.md`.

---

## Step 0 — Check for Project Design System

Before writing any component, the agent checks:

```
Does designs/DESIGN.md exist?
  YES → Read it fully before writing a single Tailwind class
        It defines: color tokens, typography, components, spacing, do's and don'ts
        All color and font decisions in this task come from that file
  NO  → Use standard Tailwind defaults (gray, white, indigo palette)
```

**If `designs/DESIGN.md` exists — it overrides all default color and typography
choices in this file.** The agent never uses generic `gray-*`, `white`, or
`indigo-*` Tailwind classes when a project design system is defined.

**Tailwind config must be updated** when a design system is present:
- Verify `tailwind.config.ts` has the custom color tokens from `DESIGN.md`
- Verify font families from `DESIGN.md` are added to the config
- Verify Google Fonts are imported in `index.html` or `globals.css`
- If not present — add them as the first step of the frontend task

---

## Stack

- **Framework**: React 18 + TypeScript
- **Build Tool**: Vite
- **Routing**: React Router v6
- **Server State**: RTK Query (Redux Toolkit)
- **Client State**: Redux Toolkit slices
- **Styling**: Tailwind CSS
- **Forms**: React Hook Form + Zod
- **Rich Text**: TipTap (for content editors)
- **HTTP Client**: Axios (inside RTK Query only)
- **Notifications**: React Hot Toast
- **Icons**: Lucide React

---

## Architecture Layers

```
User Interface Layer
(Pages, Layouts, Components)
         ↓
Presentation Logic Layer
(Custom Hooks — useBlogs, useAuth, useCaseStudies)
         ↓
State Management Layer
Server State: RTK Query  |  Client State: Redux Slice
         ↓
API Layer
(features/*/api.ts — typed API functions)
         ↓
HTTP Client Layer
(lib/axios.ts — interceptors, auth headers, error handling)
         ↓
Backend REST API
```

---

## Project Structure

```
src/
├── app/
│   ├── store.ts                    ← Redux store setup
│   └── router.tsx                  ← React Router config
├── components/
│   ├── ui/                         ← Shared primitive components
│   │   ├── Button.tsx
│   │   ├── Input.tsx
│   │   ├── Card.tsx
│   │   ├── Modal.tsx
│   │   ├── Badge.tsx
│   │   ├── Skeleton.tsx
│   │   └── Spinner.tsx
│   └── layout/
│       ├── PublicLayout.tsx        ← Header + Footer + Outlet
│       ├── AdminLayout.tsx         ← Sidebar + Header + Outlet
│       └── AuthGuard.tsx           ← Wraps protected routes
├── features/
│   └── <feature-name>/
│       ├── api.ts                  ← RTK Query API slice
│       ├── types.ts                ← TypeScript types
│       ├── hooks.ts                ← Custom hooks
│       ├── components/             ← Feature-specific components
│       │   ├── BlogCard.tsx
│       │   └── BlogFilter.tsx
│       └── pages/
│           ├── BlogListPage.tsx
│           └── BlogDetailPage.tsx
├── hooks/
│   └── useDebounce.ts              ← Shared hooks
├── lib/
│   ├── axios.ts                    ← Axios instance + interceptors
│   └── utils.ts                    ← Shared utilities
├── types/
│   └── index.ts                    ← Global shared types
└── main.tsx
```

---

## Data Flow Patterns

### Reading Data — Query Pattern

```
User Action
    ↓
Component calls RTK Query hook (useGetBlogsQuery)
    ↓
RTK Query checks cache — hit → return cached data
    ↓ miss
RTK Query calls API endpoint
    ↓
Axios interceptor adds auth headers
    ↓
Request sent to backend
    ↓
Response received (ApiResponse<T>)
    ↓
RTK Query caches result
    ↓
Component re-renders with data
```

**Example:**
```typescript
// features/blog/api.ts
export const blogApi = createApi({
  reducerPath: 'blogApi',
  baseQuery: axiosBaseQuery(),
  tagTypes: ['Blog', 'BlogCategory'],
  endpoints: (builder) => ({
    getBlogs: builder.query<PagedResponse<BlogResponse>, BlogFilters>({
      query: (filters) => ({
        url: '/public/blogs',
        method: 'GET',
        params: filters,
      }),
      providesTags: ['Blog'],
    }),
    getBlogBySlug: builder.query<BlogResponse, string>({
      query: (slug) => ({
        url: `/public/blogs/${slug}`,
        method: 'GET',
      }),
      providesTags: (result, error, slug) => [{ type: 'Blog', id: slug }],
    }),
  }),
});

export const { useGetBlogsQuery, useGetBlogBySlugQuery } = blogApi;

// Component usage
const { data, isLoading, error } = useGetBlogsQuery({ category: 'migration', page: 0 });
```

---

### Writing Data — Mutation Pattern

```
User Action (e.g., Publish Blog)
    ↓
Component calls mutation hook (usePublishBlogMutation)
    ↓
RTK Query calls API endpoint
    ↓
Backend processes and returns updated data
    ↓
onSuccess → invalidate related tags
    ↓
Related queries refetch automatically
    ↓
UI updates with fresh data + toast notification
```

**Example:**
```typescript
// features/blog/api.ts
publishBlog: builder.mutation<BlogResponse, string>({
  query: (id) => ({
    url: `/admin/blogs/${id}/publish`,
    method: 'PATCH',
  }),
  invalidatesTags: ['Blog'],    // Auto-refetch all blog queries
}),

// Component usage
const [publishBlog, { isLoading }] = usePublishBlogMutation();

const handlePublish = async () => {
  try {
    await publishBlog(blogId).unwrap();
    toast.success('Blog published successfully');
  } catch (error) {
    toast.error(getErrorMessage(error));
  }
};
```

---

## State Management Strategy

### Decision Tree

```
Is it data from backend?
├─ YES → RTK Query (useGetXQuery / useCreateXMutation)
│
├─ NO → Is it shared across multiple components?
│  ├─ YES → Redux slice (auth, ui preferences)
│  │
│  └─ NO → useState in component
```

### Server State — RTK Query

**Use for everything from the API:**
```typescript
// ✅ Correct — server data via RTK Query
const { data: blogs, isLoading } = useGetBlogsQuery({ page: 0, size: 10 });

// ❌ Wrong — server data in useState
const [blogs, setBlogs] = useState([]);
useEffect(() => { fetchBlogs().then(setBlogs) }, []);
```

**RTK Query tag invalidation rules:**
```typescript
// Tags must be invalidated on every write
createBlog: builder.mutation({
  invalidatesTags: ['Blog'],            // Invalidate all blog lists
}),
updateBlog: builder.mutation({
  invalidatesTags: (result, error, { id }) => [{ type: 'Blog', id }],
}),
deleteBlog: builder.mutation({
  invalidatesTags: ['Blog'],
}),
```

### Client State — Redux Slice

**Use only for UI state that doesn't come from backend:**
```typescript
// features/auth/authSlice.ts
interface AuthState {
  user: AdminUser | null;
  token: string | null;
  isAuthenticated: boolean;
}

const authSlice = createSlice({
  name: 'auth',
  initialState: { user: null, token: null, isAuthenticated: false },
  reducers: {
    setCredentials: (state, action) => {
      state.user = action.payload.user;
      state.token = action.payload.token;
      state.isAuthenticated = true;
    },
    logout: (state) => {
      state.user = null;
      state.token = null;
      state.isAuthenticated = false;
    },
  },
});
```

**Use Redux slice for:**
- Auth state (user, token, isAuthenticated)
- Global UI preferences (theme, sidebar open/closed)
- Language selection

**Never use Redux slice for:**
- Blog posts, case studies, testimonials → RTK Query
- Form state → React Hook Form
- Component-specific UI → useState

### Local State — useState

**Use for component-specific temporary UI:**
```typescript
// ✅ Correct use of useState
const [isFilterOpen, setIsFilterOpen] = useState(false);
const [selectedTab, setSelectedTab] = useState('all');
const [searchInput, setSearchInput] = useState('');

// ❌ Wrong — API data in useState
const [blogs, setBlogs] = useState([]);
```

---

## HTTP Client Setup

```typescript
// lib/axios.ts
import axios from 'axios';
import { store } from '@/app/store';
import { logout } from '@/features/auth/authSlice';

export const axiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,   // Never hardcode URLs
  timeout: 10000,
});

// Request interceptor — auto-add auth token
axiosInstance.interceptors.request.use((config) => {
  const token = store.getState().auth.token;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Response interceptor — handle auth expiry
axiosInstance.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      store.dispatch(logout());
      window.location.href = '/admin/login';
    }
    return Promise.reject(error);
  }
);

// RTK Query base query using axios
export const axiosBaseQuery = (): BaseQueryFn => async ({ url, method, data, params }) => {
  try {
    const result = await axiosInstance({ url, method, data, params });
    return { data: result.data.data };   // Unwrap ApiResponse<T>
  } catch (error) {
    if (axios.isAxiosError(error)) {
      return {
        error: {
          status: error.response?.status,
          message: error.response?.data?.error?.message || error.message,
          fields: error.response?.data?.error?.fields,
        },
      };
    }
    return { error: { message: 'An unexpected error occurred' } };
  }
};

// Utility to extract error message
export function getErrorMessage(error: unknown): string {
  if (typeof error === 'object' && error !== null && 'message' in error) {
    return (error as { message: string }).message;
  }
  return 'An unexpected error occurred';
}
```

---

## Component Pattern

### Component File Structure

Every component follows this exact order:

```typescript
// 1. Imports — grouped
import { useState } from 'react';

import { useGetBlogsQuery } from '@/features/blog/api';
import { useDebounce } from '@/hooks/useDebounce';

import { BlogCard } from './BlogCard';
import { BlogFilter } from './BlogFilter';
import { Skeleton } from '@/components/ui/Skeleton';
import { EmptyState } from '@/components/ui/EmptyState';

import type { BlogFilters } from '../types';

// 2. Types — component-specific only
interface BlogListProps {
  initialCategory?: string;
}

// 3. Component
export function BlogList({ initialCategory }: BlogListProps) {
  // 3a. Local state
  const [filters, setFilters] = useState<BlogFilters>({
    category: initialCategory,
    page: 0,
    size: 9,
  });

  // 3b. Derived / computed
  const debouncedSearch = useDebounce(filters.search, 300);

  // 3c. RTK Query hooks
  const { data, isLoading, error } = useGetBlogsQuery({
    ...filters,
    search: debouncedSearch,
  });

  // 3d. Event handlers
  const handleFilterChange = (newFilters: Partial<BlogFilters>) => {
    setFilters((prev) => ({ ...prev, ...newFilters, page: 0 }));
  };

  // 3e. Loading state — always handle
  if (isLoading) {
    return (
      <div className="grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-3">
        {Array.from({ length: 6 }).map((_, i) => (
          <Skeleton key={i} className="h-64 rounded-xl" />
        ))}
      </div>
    );
  }

  // 3f. Error state — always handle
  if (error) {
    return (
      <EmptyState
        title="Failed to load blogs"
        description={getErrorMessage(error)}
        action={{ label: 'Try again', onClick: () => window.location.reload() }}
      />
    );
  }

  // 3g. Empty state — always handle
  if (!data?.data?.length) {
    return (
      <EmptyState
        title="No articles found"
        description="Try adjusting your filters"
      />
    );
  }

  // 3h. Main render
  return (
    <div>
      <BlogFilter filters={filters} onChange={handleFilterChange} />
      <div className="grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-3">
        {data.data.map((blog) => (
          <BlogCard key={blog.id} blog={blog} />
        ))}
      </div>
    </div>
  );
}
```

**Component rules:**
- Always handle loading, error, and empty states — no exceptions
- One component per file — never multiple exports in one file
- Props interface defined above the component — never inline
- No business logic in JSX — extract to handlers above return
- Max 300 lines per component file — split if larger

---

## Form Pattern

```typescript
// features/consultation/components/ConsultationForm.tsx
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useSubmitConsultationMutation } from '../api';

// Zod schema — must match backend validation exactly
const consultationSchema = z.object({
  fullName: z.string().min(1, 'Full name is required').max(150),
  jobTitle: z.string().min(1, 'Job title is required').max(150),
  companyName: z.string().min(1, 'Company name is required').max(200),
  workEmail: z.string().email('Must be a valid email address'),
  phone: z.string().min(1, 'Phone number is required').max(50),
  country: z.string().min(1, 'Country is required'),
  companySize: z.enum(['1-10', '11-50', '51-200', '200+']),
  currentInfrastructure: z.enum(['On-Premise', 'AWS', 'GCP', 'Azure', 'Mixed']),
  servicesInterested: z.array(z.string()).min(1, 'Select at least one service'),
  projectTimeline: z.string().min(1, 'Project timeline is required'),
  challengeDescription: z.string().min(10, 'Please describe your challenge'),
});

type ConsultationFormData = z.infer<typeof consultationSchema>;

export function ConsultationForm() {
  const [submit, { isLoading }] = useSubmitConsultationMutation();

  const form = useForm<ConsultationFormData>({
    resolver: zodResolver(consultationSchema),
    defaultValues: {
      servicesInterested: [],
    },
  });

  const onSubmit = async (data: ConsultationFormData) => {
    try {
      await submit(data).unwrap();
      toast.success('Request submitted! We will contact you within 24 hours.');
      form.reset();
    } catch (error) {
      // Handle field-level errors from backend
      const apiError = error as ApiError;
      if (apiError.fields) {
        Object.entries(apiError.fields).forEach(([field, message]) => {
          form.setError(field as keyof ConsultationFormData, { message });
        });
      } else {
        toast.error(getErrorMessage(error));
      }
    }
  };

  return (
    <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-6">
      {/* Full Name */}
      <div>
        <label htmlFor="fullName" className="block text-sm font-medium text-gray-700">
          Full Name *
        </label>
        <input
          id="fullName"
          {...form.register('fullName')}
          className="mt-1 block w-full rounded-lg border border-gray-300 px-4 py-3"
        />
        {form.formState.errors.fullName && (
          <p className="mt-1 text-sm text-red-600" role="alert">
            {form.formState.errors.fullName.message}
          </p>
        )}
      </div>

      {/* Submit */}
      <button
        type="submit"
        disabled={isLoading}
        className="w-full rounded-lg bg-orange-500 px-6 py-4 text-white font-semibold
                   hover:bg-orange-600 disabled:opacity-50 disabled:cursor-not-allowed"
      >
        {isLoading ? 'Submitting...' : 'Book Free Consultation'}
      </button>
    </form>
  );
}
```

**Form rules:**
- Zod schema must match backend validation rules exactly
- Always use `zodResolver` — never manual validation
- Show inline field errors below each input with `role="alert"`
- Disable submit button during submission — prevent double submit
- Handle backend field-level errors via `form.setError`
- Show loading state during submission — never leave button static

---

## Auth Guard Pattern

```typescript
// components/layout/AuthGuard.tsx
import { useSelector } from 'react-redux';
import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { selectIsAuthenticated } from '@/features/auth/authSlice';

export function AuthGuard() {
  const isAuthenticated = useSelector(selectIsAuthenticated);
  const location = useLocation();

  if (!isAuthenticated) {
    // Preserve intended destination for post-login redirect
    return <Navigate to="/admin/login" state={{ from: location }} replace />;
  }

  return <Outlet />;
}

// app/router.tsx usage
<Route element={<AuthGuard />}>
  <Route path="/admin/dashboard" element={<DashboardPage />} />
  <Route path="/admin/blogs" element={<BlogListPage />} />
</Route>
```

---

## TypeScript Rules

### No `any` — Ever

```typescript
// ❌ Wrong
const data: any = response.data;
const handleClick = (e: any) => {};

// ✅ Correct
const data: BlogResponse = response.data;
const handleClick = (e: React.MouseEvent<HTMLButtonElement>) => {};
```

### Explicit Return Types on Functions

```typescript
// ❌ Wrong
function formatDate(date) {
  return new Date(date).toLocaleDateString();
}

// ✅ Correct
function formatDate(date: string): string {
  return new Date(date).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  });
}
```

### Type Imports

```typescript
// ✅ Correct — use 'type' keyword for type-only imports
import type { BlogResponse, CaseStudyResponse } from '@/types';
import { useGetBlogsQuery } from '@/features/blog/api';
```

### Union Types for Status Fields

```typescript
// ✅ Correct — union types
type BlogStatus = 'DRAFT' | 'PUBLISHED';
type ToolCategory = 'AWS_SERVICE' | 'LANGUAGE' | 'FRAMEWORK' | 'DEVOPS';
type ConsultationStatus = 'NEW' | 'REVIEWED' | 'CONTACTED' | 'CLOSED';

// ❌ Wrong — enums
enum BlogStatus { DRAFT = 'DRAFT', PUBLISHED = 'PUBLISHED' }
```

### Response Types — Match Backend Exactly

```typescript
// types/index.ts

export interface ApiResponse<T> {
  success: boolean;
  data: T;
  timestamp: string;
  error?: ApiError;
}

export interface ApiError {
  code: string;
  message: string;
  fields?: Record<string, string>;
}

export interface PagedResponse<T> {
  data: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface BlogResponse {
  id: string;                 // UUID as string
  title: string;
  slug: string;
  summary: string;
  content: string;
  coverImageUrl: string | null;
  status: 'DRAFT' | 'PUBLISHED';
  readingTimeMins: number;
  publishedAt: string | null; // ISO-8601
  createdAt: string;
  updatedAt: string;
  category: BlogCategoryResponse;
  tags: BlogTagResponse[];
}
```

---

## Styling Rules

### Tailwind Utility Classes Only

```typescript
// ✅ Correct
<div className="flex items-center justify-between rounded-xl bg-white p-6 shadow-sm
                border border-gray-100 hover:shadow-md transition-shadow">

// ❌ Wrong — inline styles
<div style={{ display: 'flex', padding: '24px' }}>

// ❌ Wrong — CSS modules (use Tailwind instead)
<div className={styles.card}>
```

### RTL Support — Logical Properties Always

```typescript
// ❌ Wrong — breaks Arabic RTL layout
<div className="ml-4 pl-6 text-left border-l">

// ✅ Correct — works in both LTR and RTL
<div className="ms-4 ps-6 text-start border-s">
```

**Mapping:**
| Physical | Logical | Use Case |
|---|---|---|
| `ml-*` | `ms-*` | margin-inline-start |
| `mr-*` | `me-*` | margin-inline-end |
| `pl-*` | `ps-*` | padding-inline-start |
| `pr-*` | `pe-*` | padding-inline-end |
| `text-left` | `text-start` | text alignment |
| `text-right` | `text-end` | text alignment |
| `border-l` | `border-s` | border side |
| `border-r` | `border-e` | border side |
| `rounded-l-*` | `rounded-s-*` | border radius |

### Mobile First — Always

```typescript
// ✅ Correct — mobile first, scale up
<div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">

// ❌ Wrong — desktop first
<div className="grid grid-cols-3 md:grid-cols-2 grid-cols-1">
```

**Breakpoints:**
- Default: 0px+ — mobile
- `sm`: 640px+ — large mobile
- `md`: 768px+ — tablet
- `lg`: 1024px+ — desktop
- `xl`: 1280px+ — large desktop

---

## Naming Conventions

| Type | Convention | Example |
|---|---|---|
| Component | PascalCase | `BlogCard.tsx`, `CaseStudyFilter.tsx` |
| Page | PascalCase + Page suffix | `BlogListPage.tsx`, `BlogDetailPage.tsx` |
| Hook | camelCase + use prefix | `useBlogs.ts`, `useAuth.ts` |
| API slice | camelCase | `api.ts` inside feature folder |
| Redux slice | camelCase | `authSlice.ts` |
| Utility | camelCase | `formatDate.ts`, `generateSlug.ts` |
| Types file | `types.ts` per feature | |
| CSS class | Tailwind only — no custom class names | |

---

## File Size Limits

| File Type | Max Lines | Action if Exceeded |
|---|---|---|
| Page component | 200 lines | Extract sections to sub-components |
| Feature component | 150 lines | Split into smaller components |
| Custom hook | 100 lines | Split into multiple hooks |
| API slice | 200 lines | Split by sub-feature |
| Utility file | 100 lines | Group related utils together |

---

## Import Order

```typescript
// 1. React
import { useState, useEffect } from 'react';

// 2. Third-party libraries
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Link, useNavigate } from 'react-router-dom';
import { useSelector } from 'react-redux';

// 3. Internal — RTK Query hooks
import { useGetBlogsQuery, usePublishBlogMutation } from '@/features/blog/api';

// 4. Internal — components
import { BlogCard } from '@/features/blog/components/BlogCard';
import { Button } from '@/components/ui/Button';
import { Skeleton } from '@/components/ui/Skeleton';

// 5. Internal — utilities and hooks
import { useDebounce } from '@/hooks/useDebounce';
import { formatDate, getErrorMessage } from '@/lib/utils';

// 6. Types
import type { BlogResponse, BlogFilters } from '../types';

// 7. Relative imports
import { BlogFilter } from './BlogFilter';
```

---

## Routing Pattern

```typescript
// app/router.tsx
import { createBrowserRouter } from 'react-router-dom';
import { AuthGuard } from '@/components/layout/AuthGuard';
import { PublicLayout } from '@/components/layout/PublicLayout';
import { AdminLayout } from '@/components/layout/AdminLayout';

export const router = createBrowserRouter([
  // Public routes
  {
    element: <PublicLayout />,
    children: [
      { path: '/', element: <HomePage /> },
      { path: '/about', element: <AboutPage /> },
      { path: '/services', element: <ServicesPage /> },
      { path: '/case-studies', element: <CaseStudiesPage /> },
      { path: '/case-studies/:slug', element: <CaseStudyDetailPage /> },
      { path: '/blog', element: <BlogPage /> },
      { path: '/blog/:slug', element: <BlogDetailPage /> },
      { path: '/free-consultation', element: <ConsultationPage /> },
      { path: '/contact', element: <ContactPage /> },
    ],
  },
  // Admin auth route — no layout
  { path: '/admin/login', element: <AdminLoginPage /> },
  // Admin protected routes
  {
    element: <AuthGuard />,
    children: [
      {
        element: <AdminLayout />,
        children: [
          { path: '/admin', element: <Navigate to="/admin/dashboard" replace /> },
          { path: '/admin/dashboard', element: <AdminDashboardPage /> },
          { path: '/admin/blogs', element: <AdminBlogListPage /> },
          { path: '/admin/blogs/new', element: <AdminBlogEditorPage /> },
          { path: '/admin/blogs/:id/edit', element: <AdminBlogEditorPage /> },
          { path: '/admin/case-studies', element: <AdminCaseStudyListPage /> },
          { path: '/admin/consultations', element: <AdminConsultationPage /> },
          { path: '/admin/settings', element: <AdminSettingsPage /> },
        ],
      },
    ],
  },
  // 404
  { path: '*', element: <NotFoundPage /> },
]);
```

---

## Environment Variables

```typescript
// ✅ Correct — always from env, never hardcoded
const apiUrl = import.meta.env.VITE_API_BASE_URL;

// ❌ Wrong — hardcoded URL
const apiUrl = 'http://localhost:8080';
```

**.env.development:**
```
VITE_API_BASE_URL=http://localhost:8080/api
VITE_APP_NAME=MFRA
```

**.env.production:**
```
VITE_API_BASE_URL=https://api.mfra.com/api
VITE_APP_NAME=MFRA
```

---

## Performance Rules

### Images — Always Lazy Load

```typescript
// ✅ Correct
<img
  src={blog.coverImageUrl}
  alt={blog.title}
  loading="lazy"
  className="w-full h-48 object-cover rounded-t-xl"
/>

// ❌ Wrong — no loading attribute
<img src={blog.coverImageUrl} alt={blog.title} />
```

### Dynamic Imports — For Heavy Components

```typescript
// For rich text editor, charts, or large libraries
const BlogEditor = lazy(() => import('@/features/blog/components/BlogEditor'));

// Usage with suspense
<Suspense fallback={<Skeleton className="h-96" />}>
  <BlogEditor />
</Suspense>
```

### RTK Query Cache — Stale Times

```typescript
// Fast-changing data (real-time or frequently updated)
keepUnusedDataFor: 30,      // 30 seconds — consultation requests, messages

// Moderate data (updated occasionally)
keepUnusedDataFor: 300,     // 5 minutes — blogs, case studies

// Slow-changing data (rarely updated)
keepUnusedDataFor: 3600,    // 1 hour — tools, stats, settings, testimonials
```

---

## Accessibility Rules

- All `<img>` must have meaningful `alt` text — never empty for content images
- All form inputs must have `<label>` associated via `htmlFor` / `id`
- All error messages must have `role="alert"`
- All icon-only buttons must have `aria-label`
- All modals must trap focus and have `role="dialog"`
- Keyboard navigation must work — tab through all interactive elements

```typescript
// ✅ Correct — accessible button
<button
  onClick={handleDelete}
  aria-label="Delete blog post"
  className="p-2 text-red-500 hover:text-red-700"
>
  <TrashIcon className="h-5 w-5" aria-hidden="true" />
</button>

// ✅ Correct — accessible form field
<div>
  <label htmlFor="email" className="block text-sm font-medium">
    Email Address *
  </label>
  <input
    id="email"
    type="email"
    aria-required="true"
    aria-describedby={errors.email ? 'email-error' : undefined}
    {...register('email')}
    className="mt-1 block w-full rounded-lg border px-4 py-3"
  />
  {errors.email && (
    <p id="email-error" role="alert" className="mt-1 text-sm text-red-600">
      {errors.email.message}
    </p>
  )}
</div>
```

---

## ✅ Always Do This

1. **RTK Query for all server data** — never useState for API data
2. **Explicit TypeScript types** — no `any`, no implicit types
3. **Handle all states** — loading, error, empty, success — always
4. **Logical CSS properties** — `ms-*` not `ml-*`, `text-start` not `text-left`
5. **Mobile first responsive** — start with mobile breakpoint, scale up
6. **Env variables** — `import.meta.env.VITE_*` for all config values
7. **Zod schema matches backend** — validation rules must be identical
8. **Disable submit during loading** — prevent double submissions
9. **Invalidate RTK Query tags on mutation** — keep UI in sync
10. **Auth headers via interceptor** — never manually add Authorization header
11. **Lazy load images** — `loading="lazy"` on all non-critical images
12. **`role="alert"` on errors** — accessibility for screen readers
13. **One component per file** — no multiple exports in one file
14. **Max 300 lines per component** — split if larger

---

## ❌ Never Do This

1. **Never use `any` type** — always explicit types
2. **Never store server data in Redux** — RTK Query handles server state
3. **Never hardcode API URLs** — always `import.meta.env.VITE_*`
4. **Never skip error/loading states** — always handle all states
5. **Never use margin-left/right** — always logical properties for RTL
6. **Never add auth headers manually** — use the axios interceptor
7. **Never put business logic in JSX** — extract to handlers above return
8. **Never use inline styles** — Tailwind classes only
9. **Never hardcode strings** — prepare for i18n from day one
10. **Never exceed 300 lines per component** — split into smaller pieces
11. **Never skip form validation** — always Zod + React Hook Form
12. **Never use enum** — use union types instead
13. **Never store JWT in localStorage** — use httpOnly cookie or Redux in-memory
14. **Never forget `aria-label` on icon buttons** — accessibility is mandatory