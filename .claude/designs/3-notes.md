### Note 3
**Category**: MISSING
**Priority**: High
**Status**: ✅ Done — implemented 30/03/2026

**What was done**:
Created AdminErrorBoundary component and added errorElement to all admin routes in router.tsx.

**Files changed**:
  - src/components/AdminErrorBoundary.tsx — NEW: error boundary with Go back + Reload buttons
  - src/app/router.tsx — added errorElement to AuthGuard wrapper, AdminLayout wrapper, and all 14 child routes

**Problem**:
When a page crashes, React Router shows the raw browser error page:
"Unexpected Application Error! Hey developer 👋"
This is the default Remix/React Router error boundary — not our UI.
Users see a broken page with no way to recover.

**Expected behavior**:
Every admin route must have an errorElement that:
- Shows a clean error screen matching the MFRA design (dark navy, surface colors)
- Shows the error message in a readable way
- Has a "Go back" button and a "Reload page" button
- Does NOT show the raw stack trace to the user

**Implementation**:
Add errorElement to the router config:
```tsx
// src/router/AdminRouter.tsx

import { createBrowserRouter } from 'react-router-dom';
import AdminErrorBoundary from '@/components/AdminErrorBoundary';

const router = createBrowserRouter([
  {
    path: '/admin',
    element: <AdminLayout />,
    errorElement: <AdminErrorBoundary />,   // ← add this
    children: [
      {
        path: 'blogs',
        element: <BlogListPage />,
        errorElement: <AdminErrorBoundary />,   // ← add to each route too
      },
      // ... all other admin routes
    ],
  },
]);
```

Create the AdminErrorBoundary component:
```tsx
// src/components/AdminErrorBoundary.tsx

import { useRouteError, useNavigate } from 'react-router-dom';

export default function AdminErrorBoundary() {
  const error = useRouteError() as Error;
  const navigate = useNavigate();

  return (
    <div
      data-testid="error-boundary"
      className="flex flex-col items-center justify-center min-h-screen bg-surface"
    >
      <div className="bg-surface-high rounded-xl p-8 max-w-lg w-full mx-4">
        <h1 className="font-display text-2xl text-onsurface mb-2">
          Something went wrong
        </h1>
        <p className="font-body text-sm text-onsurface-variant mb-6">
          {error?.message ?? 'An unexpected error occurred.'}
        </p>
        <div className="flex gap-3">
          <button
            onClick={() => navigate(-1)}
            className="px-4 py-2 rounded-md bg-surface-highest
                       font-body text-sm text-onsurface"
          >
            Go back
          </button>
          <button
            onClick={() => window.location.reload()}
            className="px-4 py-2 rounded-md font-body text-sm text-white"
            style={{ background: 'linear-gradient(45deg, #ffc082, #ff9900)' }}
          >
            Reload page
          </button>
        </div>
      </div>
    </div>
  );
}
```

**Scope**:
- [x] Frontend change needed — add errorElement to all admin routes
- [x] Frontend change needed — create AdminErrorBoundary component