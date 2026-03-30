import { createBrowserRouter, Navigate } from 'react-router-dom';
import { AuthGuard } from '@/components/guards/AuthGuard';
import LoginPage from '@/features/auth/pages/LoginPage';

// Placeholder for future pages
function PlaceholderPage({ title }: { title: string }) {
  return (
    <div className="flex min-h-screen items-center justify-center bg-[#0d1322]">
      <div className="text-center">
        <h1 className="text-2xl font-bold text-[#e8e8f0]">{title}</h1>
        <p className="mt-2 text-[#dbc2ad]">Coming soon</p>
      </div>
    </div>
  );
}

export const router = createBrowserRouter([
  // Admin login — no layout
  {
    path: '/admin/login',
    element: <LoginPage />,
  },

  // Admin protected routes
  {
    element: <AuthGuard />,
    children: [
      {
        path: '/admin',
        element: <Navigate to="/admin/dashboard" replace />,
      },
      {
        path: '/admin/dashboard',
        element: <PlaceholderPage title="Dashboard" />,
      },
      {
        path: '/admin/blogs',
        element: <PlaceholderPage title="Blogs" />,
      },
      {
        path: '/admin/case-studies',
        element: <PlaceholderPage title="Case Studies" />,
      },
    ],
  },

  // Root redirect
  {
    path: '/',
    element: <Navigate to="/admin/login" replace />,
  },

  // 404
  {
    path: '*',
    element: (
      <div className="flex min-h-screen items-center justify-center bg-[#0d1322]">
        <div className="text-center">
          <h1 className="text-6xl font-bold text-[#ff9900]">404</h1>
          <p className="mt-2 text-[#dbc2ad]">Page not found</p>
        </div>
      </div>
    ),
  },
]);
