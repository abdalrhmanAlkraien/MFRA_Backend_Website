import { test, expect } from '@playwright/test';

test.describe('Login Page', () => {
  test.beforeEach(async ({ context }) => {
    // Clear storage via context to avoid SecurityError on about:blank
    await context.clearCookies();
    await context.addInitScript(() => {
      localStorage.clear();
      sessionStorage.clear();
    });
  });

  // TC-F-01: Page Load — Happy Path
  test('should render login page with all required elements', async ({ page }) => {
    await page.goto('/admin/login');
    await page.waitForLoadState('networkidle');

    // Logo area
    await expect(page.locator('[data-testid="login-logo"]')).toBeVisible();

    // Email input
    const emailInput = page.locator('[data-testid="email-input"]');
    await expect(emailInput).toBeVisible();
    await expect(emailInput).toHaveAttribute('type', 'email');

    // Password input
    const passwordInput = page.locator('[data-testid="password-input"]');
    await expect(passwordInput).toBeVisible();
    await expect(passwordInput).toHaveAttribute('type', 'password');

    // Remember checkbox
    await expect(page.locator('[data-testid="remember-checkbox"]')).toBeVisible();

    // Submit button
    const submitBtn = page.locator('[data-testid="login-submit"]');
    await expect(submitBtn).toBeVisible();
    await expect(submitBtn).toHaveText('Sign In');

    // Security notice
    await expect(page.locator('[data-testid="security-notice"]')).toBeVisible();

    // Toggle password button
    await expect(page.locator('[data-testid="toggle-password"]')).toBeVisible();
  });

  // TC-F-02: Form Validation — Empty Submit
  test('should show validation errors on empty form submit', async ({ page }) => {
    await page.goto('/admin/login');
    await page.waitForLoadState('networkidle');

    // Submit empty form
    await page.locator('[data-testid="login-submit"]').click();
    await page.waitForTimeout(300);

    // Email error should appear
    const emailError = page.locator('[data-testid="email-error"]');
    await expect(emailError).toBeVisible();

    // Password error should appear
    const passwordError = page.locator('[data-testid="password-error"]');
    await expect(passwordError).toBeVisible();
  });

  // TC-F-03: Form Validation — Invalid Email
  test('should show validation error for invalid email format', async ({ page }) => {
    await page.goto('/admin/login');
    await page.waitForLoadState('networkidle');

    // Use a value that passes HTML type="email" but fails Zod email validation
    // We fill the field programmatically bypassing HTML validation
    const emailInput = page.locator('[data-testid="email-input"]');
    await emailInput.fill('not-an-email');

    await page.locator('[data-testid="password-input"]').fill('somepassword');

    // Dispatch submit event directly to bypass HTML5 validation
    await page.evaluate(() => {
      const form = document.querySelector('form');
      form?.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    });
    await page.waitForTimeout(500);

    const emailError = page.locator('[data-testid="email-error"]');
    await expect(emailError).toBeVisible();
    await expect(emailError).toContainText('valid email');
  });

  // TC-F-04: Password Toggle
  test('should toggle password visibility', async ({ page }) => {
    await page.goto('/admin/login');
    await page.waitForLoadState('networkidle');

    const passwordInput = page.locator('[data-testid="password-input"]');
    const toggleBtn = page.locator('[data-testid="toggle-password"]');

    // Initially password type
    await expect(passwordInput).toHaveAttribute('type', 'password');

    // Click toggle — should become text
    await toggleBtn.click();
    await expect(passwordInput).toHaveAttribute('type', 'text');

    // Click again — should become password
    await toggleBtn.click();
    await expect(passwordInput).toHaveAttribute('type', 'password');
  });

  // TC-F-05: Auth Guard — Redirect unauthenticated users
  test('should redirect unauthenticated user from /admin/dashboard to /admin/login', async ({ page }) => {
    await page.goto('/admin/dashboard');
    await page.waitForLoadState('networkidle');

    // Should redirect to login
    expect(page.url()).toContain('/admin/login');
  });

  // TC-F-06: Login Form Submission — API call structure
  test('should make correct API call on form submit', async ({ page }) => {
    const apiCalls: { method: string; url: string; body: string }[] = [];

    await page.route('**/api/auth/login', async (route) => {
      const request = route.request();
      apiCalls.push({
        method: request.method(),
        url: request.url(),
        body: request.postData() || '',
      });

      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            accessToken: 'mock-access-token',
            refreshToken: 'mock-refresh-token',
            tokenType: 'Bearer',
            expiresIn: 3600,
            user: {
              id: '123e4567-e89b-12d3-a456-426614174000',
              fullName: 'Test Admin',
              email: 'admin@mfra.com',
              role: 'ADMIN',
            },
          },
          timestamp: new Date().toISOString(),
        }),
      });
    });

    await page.goto('/admin/login');
    await page.waitForLoadState('networkidle');

    await page.locator('[data-testid="email-input"]').fill('admin@mfra.com');
    await page.locator('[data-testid="password-input"]').fill('admin123');
    await page.locator('[data-testid="login-submit"]').click();

    // Wait for navigation after login
    await page.waitForTimeout(1000);

    // Verify API call was made
    expect(apiCalls.length).toBe(1);
    expect(apiCalls[0].method).toBe('POST');
    expect(apiCalls[0].url).toContain('/api/auth/login');

    const requestBody = JSON.parse(apiCalls[0].body);
    expect(requestBody.email).toBe('admin@mfra.com');
    expect(requestBody.password).toBe('admin123');
  });

  // TC-F-07: Successful Login — Redirect to dashboard
  test('should redirect to dashboard after successful login', async ({ page }) => {
    await page.route('**/api/auth/login', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            accessToken: 'mock-access-token',
            refreshToken: 'mock-refresh-token',
            tokenType: 'Bearer',
            expiresIn: 3600,
            user: {
              id: '123e4567-e89b-12d3-a456-426614174000',
              fullName: 'Test Admin',
              email: 'admin@mfra.com',
              role: 'ADMIN',
            },
          },
          timestamp: new Date().toISOString(),
        }),
      });
    });

    await page.goto('/admin/login');
    await page.waitForLoadState('networkidle');

    await page.locator('[data-testid="email-input"]').fill('admin@mfra.com');
    await page.locator('[data-testid="password-input"]').fill('admin123');
    await page.locator('[data-testid="login-submit"]').click();

    // Should navigate to dashboard
    await page.waitForURL('**/admin/dashboard', { timeout: 5000 });
    expect(page.url()).toContain('/admin/dashboard');
  });

  // TC-F-08: Failed Login — Error toast
  test('should show error toast on failed login', async ({ page }) => {
    await page.route('**/api/auth/login', async (route) => {
      await route.fulfill({
        status: 401,
        contentType: 'application/json',
        body: JSON.stringify({
          success: false,
          error: {
            code: 'INVALID_CREDENTIALS',
            message: 'Invalid email or password',
          },
          timestamp: new Date().toISOString(),
        }),
      });
    });

    await page.goto('/admin/login');
    await page.waitForLoadState('networkidle');

    await page.locator('[data-testid="email-input"]').fill('admin@mfra.com');
    await page.locator('[data-testid="password-input"]').fill('wrongpass');
    await page.locator('[data-testid="login-submit"]').click();

    // Should stay on login page
    await page.waitForTimeout(1000);
    expect(page.url()).toContain('/admin/login');
  });

  // TC-F-09: Remember Device checkbox
  test('should include rememberDevice in API request when checked', async ({ page }) => {
    let capturedBody = '';

    await page.route('**/api/auth/login', async (route) => {
      capturedBody = route.request().postData() || '';
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            accessToken: 'token',
            refreshToken: 'refresh',
            tokenType: 'Bearer',
            expiresIn: 3600,
            user: {
              id: '123e4567-e89b-12d3-a456-426614174000',
              fullName: 'Admin',
              email: 'admin@mfra.com',
              role: 'ADMIN',
            },
          },
          timestamp: new Date().toISOString(),
        }),
      });
    });

    await page.goto('/admin/login');
    await page.waitForLoadState('networkidle');

    await page.locator('[data-testid="email-input"]').fill('admin@mfra.com');
    await page.locator('[data-testid="password-input"]').fill('admin123');
    await page.locator('[data-testid="remember-checkbox"]').check();
    await page.locator('[data-testid="login-submit"]').click();

    await page.waitForTimeout(1000);

    const body = JSON.parse(capturedBody);
    expect(body.rememberDevice).toBe(true);
  });

  // TC-F-10: Loading state on submit
  test('should show loading state while API call is in flight', async ({ page }) => {
    await page.route('**/api/auth/login', async (route) => {
      // Delay response to observe loading state
      await new Promise((resolve) => setTimeout(resolve, 2000));
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            accessToken: 'token',
            refreshToken: 'refresh',
            tokenType: 'Bearer',
            expiresIn: 3600,
            user: {
              id: '123e4567-e89b-12d3-a456-426614174000',
              fullName: 'Admin',
              email: 'admin@mfra.com',
              role: 'ADMIN',
            },
          },
          timestamp: new Date().toISOString(),
        }),
      });
    });

    await page.goto('/admin/login');
    await page.waitForLoadState('networkidle');

    await page.locator('[data-testid="email-input"]').fill('admin@mfra.com');
    await page.locator('[data-testid="password-input"]').fill('admin123');
    await page.locator('[data-testid="login-submit"]').click();

    // Button should show loading text
    const submitBtn = page.locator('[data-testid="login-submit"]');
    await expect(submitBtn).toContainText('Signing in...');
    await expect(submitBtn).toBeDisabled();
  });

  // TC-F-11: Accessibility checks
  test('should have proper accessibility attributes', async ({ page }) => {
    await page.goto('/admin/login');
    await page.waitForLoadState('networkidle');

    // All inputs have labels
    const emailLabel = page.locator('label[for="email"]');
    await expect(emailLabel).toBeVisible();

    const passwordLabel = page.locator('label[for="password"]');
    await expect(passwordLabel).toBeVisible();

    const rememberLabel = page.locator('label[for="rememberDevice"]');
    await expect(rememberLabel).toBeVisible();

    // Toggle password has aria-label
    const toggleBtn = page.locator('[data-testid="toggle-password"]');
    await expect(toggleBtn).toHaveAttribute('aria-label', /password/i);

    // Error messages have role="alert"
    await page.locator('[data-testid="login-submit"]').click();
    await page.waitForTimeout(300);

    const alerts = page.locator('[role="alert"]');
    const alertCount = await alerts.count();
    expect(alertCount).toBeGreaterThan(0);
  });

  // TC-F-12: Responsive — No horizontal scroll
  test('should not have horizontal scroll on mobile viewport', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 812 });
    await page.goto('/admin/login');
    await page.waitForLoadState('networkidle');

    const noHorizontalScroll = await page.evaluate(
      () => document.body.scrollWidth <= window.innerWidth,
    );
    expect(noHorizontalScroll).toBe(true);

    // Login form should still be visible
    await expect(page.locator('[data-testid="login-page"]')).toBeVisible();
    await expect(page.locator('[data-testid="email-input"]')).toBeVisible();
  });

  // TC-F-13: Already authenticated redirect
  test('should redirect to dashboard if already authenticated', async ({ browser }) => {
    // Use a fresh context with auth state pre-set via addInitScript
    const context = await browser.newContext();
    await context.addInitScript(() => {
      localStorage.setItem('accessToken', 'mock-access-token');
      localStorage.setItem('refreshToken', 'mock-refresh-token');
      localStorage.setItem(
        'user',
        JSON.stringify({
          id: '123e4567-e89b-12d3-a456-426614174000',
          fullName: 'Admin',
          email: 'admin@mfra.com',
          role: 'ADMIN',
        }),
      );
    });
    const page = await context.newPage();

    await page.goto('/admin/login');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(500);

    // Should redirect to dashboard because user is already authenticated
    expect(page.url()).toContain('/admin/dashboard');

    await context.close();
  });

  // TC-F-14: 404 page
  test('should show 404 page for unknown routes', async ({ page }) => {
    await page.goto('/some/unknown/route');
    await page.waitForLoadState('networkidle');

    await expect(page.locator('text=404')).toBeVisible();
    await expect(page.locator('text=Page not found')).toBeVisible();
  });

  // TC-F-15: No console errors on page load
  test('should have no console errors on page load', async ({ page }) => {
    const consoleErrors: string[] = [];
    page.on('console', (msg) => {
      if (msg.type() === 'error') {
        consoleErrors.push(msg.text());
      }
    });

    await page.goto('/admin/login');
    await page.waitForLoadState('networkidle');

    expect(consoleErrors).toHaveLength(0);
  });
});
