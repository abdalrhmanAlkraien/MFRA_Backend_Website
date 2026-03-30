import { useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import toast from 'react-hot-toast';
import { Eye, EyeOff } from 'lucide-react';

import { useLoginMutation } from '@/features/auth/authApi';
import { setCredentials, selectIsAuthenticated } from '@/features/auth/authSlice';
import { getErrorMessage } from '@/lib/axios';
import { Navigate } from 'react-router-dom';

const loginSchema = z.object({
  email: z.string().min(1, 'Email is required').email('Must be a valid email address'),
  password: z.string().min(1, 'Password is required'),
  rememberDevice: z.boolean(),
});

type LoginFormData = z.infer<typeof loginSchema>;

export default function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const dispatch = useDispatch();
  const isAuthenticated = useSelector(selectIsAuthenticated);
  const [showPassword, setShowPassword] = useState(false);
  const [login, { isLoading }] = useLoginMutation();

  const from = (location.state as { from?: { pathname: string } })?.from?.pathname || '/admin/dashboard';

  const form = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      email: '',
      password: '',
      rememberDevice: false,
    },
  });

  if (isAuthenticated) {
    return <Navigate to="/admin/dashboard" replace />;
  }

  const onSubmit = async (data: LoginFormData) => {
    try {
      const response = await login(data).unwrap();
      dispatch(
        setCredentials({
          user: response.user,
          accessToken: response.accessToken,
          refreshToken: response.refreshToken,
        }),
      );
      toast.success('Welcome back!');
      navigate(from, { replace: true });
    } catch (error) {
      toast.error(getErrorMessage(error));
    }
  };

  return (
    <div
      className="flex min-h-screen items-center justify-center bg-[#0d1322] px-4"
      data-testid="login-page"
    >
      <div className="w-full max-w-md">
        {/* Logo */}
        <div className="mb-8 text-center" data-testid="login-logo">
          <h1 className="text-3xl font-bold tracking-tight text-[#ff9900]">
            MFRA
          </h1>
          <p className="mt-1 text-sm uppercase tracking-widest text-[#dbc2ad]">
            Admin Panel
          </p>
        </div>

        {/* Login Card */}
        <div className="rounded-2xl bg-[#191f2f] p-8 shadow-xl">
          <h2 className="mb-6 text-xl font-semibold text-[#e8e8f0]">
            Sign in to your account
          </h2>

          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-5">
            {/* Email */}
            <div>
              <label
                htmlFor="email"
                className="mb-1.5 block text-xs font-medium uppercase tracking-widest text-[#dbc2ad]"
              >
                Email Address
              </label>
              <input
                id="email"
                type="email"
                autoComplete="email"
                data-testid="email-input"
                {...form.register('email')}
                className="block w-full rounded-lg border border-[#2a3142] bg-[#080e1d] px-4 py-3 text-[#e8e8f0] placeholder-gray-500 focus:border-[#ff9900] focus:outline-none focus:ring-1 focus:ring-[#ff9900]"
                placeholder="you@mfra.com"
              />
              {form.formState.errors.email && (
                <p
                  className="mt-1 text-sm text-red-400"
                  role="alert"
                  data-testid="email-error"
                >
                  {form.formState.errors.email.message}
                </p>
              )}
            </div>

            {/* Password */}
            <div>
              <label
                htmlFor="password"
                className="mb-1.5 block text-xs font-medium uppercase tracking-widest text-[#dbc2ad]"
              >
                Password
              </label>
              <div className="relative">
                <input
                  id="password"
                  type={showPassword ? 'text' : 'password'}
                  autoComplete="current-password"
                  data-testid="password-input"
                  {...form.register('password')}
                  className="block w-full rounded-lg border border-[#2a3142] bg-[#080e1d] px-4 py-3 pe-12 text-[#e8e8f0] placeholder-gray-500 focus:border-[#ff9900] focus:outline-none focus:ring-1 focus:ring-[#ff9900]"
                  placeholder="Enter your password"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute end-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-300"
                  aria-label={showPassword ? 'Hide password' : 'Show password'}
                  data-testid="toggle-password"
                >
                  {showPassword ? (
                    <EyeOff className="h-5 w-5" />
                  ) : (
                    <Eye className="h-5 w-5" />
                  )}
                </button>
              </div>
              {form.formState.errors.password && (
                <p
                  className="mt-1 text-sm text-red-400"
                  role="alert"
                  data-testid="password-error"
                >
                  {form.formState.errors.password.message}
                </p>
              )}
            </div>

            {/* Remember Device */}
            <div className="flex items-center gap-2">
              <input
                id="rememberDevice"
                type="checkbox"
                data-testid="remember-checkbox"
                {...form.register('rememberDevice')}
                className="h-4 w-4 rounded border-gray-600 bg-[#080e1d] text-[#ff9900] focus:ring-[#ff9900]"
              />
              <label htmlFor="rememberDevice" className="text-sm text-[#dbc2ad]">
                Remember this device
              </label>
            </div>

            {/* Submit */}
            <button
              type="submit"
              disabled={isLoading}
              data-testid="login-submit"
              className="w-full rounded-lg bg-gradient-to-r from-[#ff9900] to-[#ffb347] px-4 py-3 font-semibold text-[#0d1322] transition-opacity hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-50"
            >
              {isLoading ? 'Signing in...' : 'Sign In'}
            </button>
          </form>
        </div>

        {/* Security Notice */}
        <p className="mt-6 text-center text-xs text-gray-500" data-testid="security-notice">
          This is a secure area. Unauthorized access attempts are logged.
        </p>
      </div>
    </div>
  );
}
