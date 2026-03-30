# Frontend Testing Instructions

> This file is reusable across all projects.
> The AI agent must read this file before writing or executing any frontend test.
> Project-specific URLs, credentials, and routes are in `.claude/CLAUDE.md`.

---

## Testing Stack

- **E2E Testing**: Playwright — the ONLY frontend testing tool
- **Build Check**: `npm run build` — must pass before any Playwright run
- **Browser**: Chromium (default)
- **Screenshots**: Captured after EVERY scenario — mandatory

---

## The Non-Negotiable Rule

**Generate the test plan file FIRST. Execute tests SECOND. Write results THIRD.**

The test plan file must exist BEFORE Playwright runs a single scenario.
If Playwright cannot run (server won't start, build fails), the test plan
file must still be created and updated with the failure reason.

A task with no test plan file = a task that cannot be reviewed or audited.
It will always be treated as incomplete.

---

## Test Plan File Location and Naming

```
.claude/tests/Task X.Y - Frontend Test Plan.md
```

This file serves as both the test plan AND the test results — results are
written back into the same file after execution.

---

## Mandatory Testing Workflow

```
Step 1 — Read context
  Read: specs/<module>/spec.md        ← acceptance criteria → scenarios
  Read: designs/[N]-requirements.md   ← page-specific rules → edge cases
  Read: .claude/systemTasks.md        ← current task ID

Step 2 — Write the Playwright spec file
  Create: tests/e2e/<module>/<page-name>.spec.ts
  Fill in: all test cases — TC-F-01 through TC-F-NN
  DO NOT run Playwright yet

  Verify the file exists before continuing:
  ls tests/e2e/<module>/
  → Must show: <page-name>.spec.ts with content
  → If empty or missing → write it now before continuing

Step 3 — Generate test plan file (documentation)
  Create: .claude/tests/Task X.Y - Frontend Test Plan.md
  This documents the same scenarios as the spec file
  Status at top: ⏳ NOT EXECUTED

Step 4 — Build check
  Run: npm run build
  If build fails → write failure into plan file → fix → rebuild
  Do not run Playwright until build is clean: 0 TypeScript errors

Step 5 — Start servers
  Run: npm run dev  (frontend at localhost:5173)
  Confirm: backend running at localhost:8080

Step 6 — Run Playwright
  npx playwright test tests/e2e/<module>/<page-name>.spec.ts
  Or: npx playwright test (run all spec files for this task)
  Each test captures a screenshot automatically

Step 7 — Write results back into plan file
  For each scenario row in the plan file:
    → Fill in: ✅ PASS or ❌ FAIL
    → Fill in: screenshot filename
    → Fill in: any error message if failed
  Update Final Status section at top
  Update systemTasks.md — exactly once
```

**An empty `tests/e2e/` directory = no tests were written = task is NOT complete.**
**Write the .spec.ts file (Step 2) before running Playwright (Step 6).**
**Tasks cannot be marked complete without all scenarios passing — no exceptions.**

---

## SpecKit File Reference

SpecKit generates these files — always read them before writing tests:

| SpecKit File | Location | What to Extract |
|---|---|---|
| Module spec | `specs/<module>/spec.md` | Acceptance criteria → one scenario each |
| Implementation plan | `specs/<module>/plan.md` | Technical approach → what to verify |
| Task breakdown | `specs/<module>/tasks.md` | Scope of current task → what is in/out |
| Task tracker | `.claude/systemTasks.md` | Current task ID and status |

**Example for blog module:**
```
specs/blog/spec.md      ← read acceptance criteria
specs/blog/plan.md      ← read implementation decisions
specs/blog/tasks.md     ← read what this specific task covers
.claude/systemTasks.md  ← confirm current task and mark complete
```

---

## File Naming Convention

| File Type | Location | Format | Example |
|---|---|---|---|
| SpecKit Spec | `specs/<module>/` | `spec.md` | `specs/blog/spec.md` |
| SpecKit Tasks | `specs/<module>/` | `tasks.md` | `specs/blog/tasks.md` |
| **Frontend Test Plan** | `.claude/tests/` | `Task X.Y - Frontend Test Plan.md` | `Task 7.1 - Frontend Test Plan.md` |
| **Backend Test Plan** | `.claude/tests/` | `Task X.Y - Backend Test Plan.md` | `Task 7.1 - Backend Test Plan.md` |
| **Mobile Test Plan** | `.claude/tests/` | `Task X.Y - Mobile Test Plan.md` | `Task 7.1 - Mobile Test Plan.md` |
| Screenshots | `.claude/screenshots/` | `test-X.Y-FNN-[label].png` | `test-7.1-F01-page-load.png` |

**One test plan file per platform per task. Results written back into the same file.**

---

## Test Generation Process

After completing a task implementation, generate test scenarios automatically:

### Step 1 — Read Current Task
```
Read .claude/systemTasks.md
  → find current task ID (e.g. Task 7.1)
  → find module name (e.g. blog)

Read specs/<module>/spec.md
  → extract acceptance criteria
  → each criterion becomes one test scenario

Read specs/<module>/tasks.md
  → confirm what this task covers
  → identify APIs, UI elements, edge cases in scope
```

### Step 2 — Generate Test File
```
For each acceptance criterion in spec.md → create one Scenario
For each API endpoint in scope → create one Network Check
For each form in scope → create one Validation Scenario
For each error case in spec.md → create one Error Handling Scenario
Always add → Loading State Scenario
Always add → Regression Smoke Tests
Save to .claude/Tests/<module>/Task X.Y.md
```

### Step 3 — Execute Tests
```
Start dev server if not running: npm run dev
Run each scenario using Playwright
Capture screenshot after each scenario
Record pass/fail per scenario
Save results to .claude/processed/Task X.Y - Test Results.md
Update .claude/systemTasks.md — exactly once
```

---

## Test Scenario File Template

Every test file follows this exact structure:

```markdown
# Test Scenarios: Task X.Y — [Task Name]

**Task Definition**: `.claude/Phases/PhaseX/Task X.Y.md`
**Generated**: [Timestamp]
**Status**: ⏳ Not Executed

---

## Prerequisites

- [ ] Dev server running: `npm run dev` at http://localhost:5173
- [ ] Backend API running: http://localhost:8080
- [ ] Previous tasks completed: [list task IDs]
- [ ] Test admin user exists: admin@mfra.com / admin123
- [ ] Clean browser state: Redux store cleared, no stale cache

---

## Environment

- **Frontend URL**: http://localhost:5173
- **API URL**: http://localhost:8080/api
- **Browser**: Chromium
- **Viewport**: 1280x720 (desktop default)

---

## Scenarios

### Scenario 1: [Primary Functionality — Happy Path]

**Purpose**: [What this tests — one sentence]
**Acceptance Criterion**: [Which criterion from task definition this covers]

**Steps**:
1. [Step]
2. [Step]
3. Verify [expected result]

**Playwright Code**:
```javascript
await playwright_navigate({ url: 'http://localhost:5173/...' });
await page.waitForLoadState('networkidle');
await playwright_screenshot({ name: 'test-X.Y-scenario-1-loaded' });
// assertions...
```

**Expected Results**:
- ✅ [Outcome 1]
- ✅ [Outcome 2]
- ✅ No console errors
- ✅ Correct API call made

**Result**: [ ] Pass / [ ] Fail
**Notes**:

---

### Scenario 2: [Form Validation / Error Handling]
[Same structure]

### Scenario 3: [Auth / Access Control]
[Same structure]

### Scenario 4: [Loading State]
[Same structure]

### Scenario 5: [Empty State]
[Same structure]

---

## Inspection Checklist

### Network
- [ ] Correct endpoint called: [METHOD /api/...]
- [ ] Correct request headers: Authorization Bearer token
- [ ] Correct request body structure
- [ ] Correct response status: [200/201/etc]
- [ ] Response data matches what UI displays

### Console
- [ ] No errors on page load
- [ ] No errors during interactions
- [ ] No unhandled promise rejections
- [ ] No React warnings

### Redux Store
- [ ] Correct state after action
- [ ] Auth token present for protected pages
- [ ] RTK Query cache populated after fetch

### Accessibility
- [ ] All inputs have labels
- [ ] Error messages have role="alert"
- [ ] Icon buttons have aria-label
- [ ] Keyboard tab navigation works

### Responsive
- [ ] Desktop (1280x720) ✅
- [ ] Tablet (768x1024) ✅
- [ ] Mobile (375x812) ✅

---

## Regression Smoke Tests

Quick checks that previous functionality still works:

- [ ] Home page loads at http://localhost:5173
- [ ] Public API calls succeed (no CORS errors)
- [ ] Admin login page loads at http://localhost:5173/admin/login
- [ ] TypeScript build passes: `npm run build`

---

## Execution Log

**Started**: [Timestamp]
**Completed**: [Timestamp]
**Duration**: [Minutes]

| Scenario | Status | Duration | Notes |
|---|---|---|---|
| 1. Primary functionality | ⏳ | — | — |
| 2. Form validation | ⏳ | — | — |
| 3. Error handling | ⏳ | — | — |
| 4. Loading state | ⏳ | — | — |
| 5. Empty state | ⏳ | — | — |
| Network checks | ⏳ | — | — |
| Regression | ⏳ | — | — |

**Total**: 0 / 0 passed
**Final Status**: ⏳ Not Executed
```

---

## Scenario Types — Write One of Each Per Task

### Type 1 — Happy Path (always required)

Tests the primary success flow:

```javascript
// Example: Blog list loads and displays cards
await playwright_navigate({ url: 'http://localhost:5173/blog' });
await page.waitForLoadState('networkidle');

// Verify cards rendered
const cards = page.locator('[data-testid="blog-card"]');
const count = await cards.count();
console.log('Blog cards rendered:', count > 0 ? `✅ ${count} cards` : '❌ None');

// Verify filter bar present
const filterBar = page.locator('[data-testid="blog-filter"]');
const filterVisible = await filterBar.isVisible();
console.log('Filter bar visible:', filterVisible ? '✅' : '❌');

await playwright_screenshot({ name: 'test-X.Y-scenario-1-blog-list' });
```

---

### Type 2 — Form Submission (required for any form)

Tests valid submit, invalid submit, and backend error handling:

```javascript
// PART A — Valid submission
await playwright_navigate({ url: 'http://localhost:5173/free-consultation' });

await playwright_fill({ selector: '[name="fullName"]', value: 'Ahmed Al-Rashid' });
await playwright_fill({ selector: '[name="workEmail"]', value: 'ahmed@company.com' });
await playwright_fill({ selector: '[name="companyName"]', value: 'TechCorp KSA' });
await playwright_fill({ selector: '[name="phone"]', value: '+966501234567' });

await playwright_screenshot({ name: 'test-X.Y-scenario-2-form-filled' });
await playwright_click({ selector: 'button[type="submit"]' });

// Wait for success state
await page.waitForSelector('[data-testid="success-message"]', { timeout: 5000 });
const successMsg = await page.locator('[data-testid="success-message"]').textContent();
console.log('Success message shown:', successMsg ? '✅' : '❌');

// PART B — Empty form validation
await playwright_navigate({ url: 'http://localhost:5173/free-consultation' });
await playwright_click({ selector: 'button[type="submit"]' });
await page.waitForTimeout(300);

const errors = await page.locator('[role="alert"]').count();
console.log('Validation errors shown:', errors > 0 ? `✅ ${errors} errors` : '❌ None');

await playwright_screenshot({ name: 'test-X.Y-scenario-2-validation-errors' });
```

---

### Type 3 — Auth Guard (required for all admin pages)

Tests that protected routes redirect unauthenticated users:

```javascript
// Clear Redux store to simulate logged-out state
await page.evaluate(() => {
  localStorage.clear();
  sessionStorage.clear();
});

// Try to access protected route
await playwright_navigate({ url: 'http://localhost:5173/admin/dashboard' });
await page.waitForLoadState('networkidle');

// Must redirect to login
const currentUrl = page.url();
console.log('Redirected to login:', currentUrl.includes('/admin/login') ? '✅' : `❌ Stayed at ${currentUrl}`);

await playwright_screenshot({ name: 'test-X.Y-scenario-3-auth-redirect' });

// Now login and verify access
await playwright_fill({ selector: '[name="email"]', value: 'admin@mfra.com' });
await playwright_fill({ selector: '[name="password"]', value: 'admin123' });
await playwright_click({ selector: 'button[type="submit"]' });

await page.waitForURL('**/admin/dashboard', { timeout: 5000 });
console.log('Dashboard accessible after login:', '✅');

await playwright_screenshot({ name: 'test-X.Y-scenario-3-after-login' });
```

---

### Type 4 — Loading State + RTK Query Crash Guard (required for ALL components)

Tests two things simultaneously:
1. That loading skeletons appear while the API is in flight
2. That the component does NOT crash when `data` is `undefined`

The crash guard is the most important part. A `TypeError: Cannot read properties
of undefined` means the component accessed `data.X` before the API responded.
This test catches it before it reaches production.

```javascript
// ── Collect JS errors — crash shows up here ──────────────────
const jsErrors = [];
page.on('pageerror', err => jsErrors.push(err.message));
page.on('console', msg => {
  if (msg.type() === 'error') jsErrors.push(msg.text());
});

// ── Delay the API to force the "data=undefined" render ───────
await page.route('**/api/**', async (route) => {
  await new Promise(resolve => setTimeout(resolve, 1000)); // 1s — exposes the crash
  await route.continue();
});

await playwright_navigate({ url: 'http://localhost:5173/[route]' });

// ── Check for crash IMMEDIATELY (before API responds) ────────
await page.waitForTimeout(200); // let the first render attempt happen

const crashes = jsErrors.filter(e =>
  e.includes('Cannot read properties of undefined') ||
  e.includes('Cannot read properties of null') ||
  e.includes('is not a function') ||
  e.includes('TypeError')
);

if (crashes.length > 0) {
  console.log('❌ COMPONENT CRASHED during loading state:');
  console.log('  Error:', crashes[0]);
  console.log('  Fix: add data?.X ?? [] guards — read instructions/frontend.md Step 0b');
} else {
  console.log('✅ No crash during loading state (data=undefined handled correctly)');
}

// ── Check skeleton is visible ─────────────────────────────────
const skeleton = page.locator('[data-testid="skeleton"], [data-testid="skeleton-row"]');
const skeletonVisible = await skeleton.isVisible().catch(() => false);
console.log('Loading skeleton shown:', skeletonVisible ? '✅' : '⚠️ Skeleton not found');

await playwright_screenshot({ name: 'test-X.Y-scenario-4-loading' });

// ── Wait for API to respond ───────────────────────────────────
await page.waitForLoadState('networkidle');

// ── Skeleton removed after load ───────────────────────────────
const skeletonGone = !(await skeleton.isVisible().catch(() => false));
console.log('Skeleton removed after load:', skeletonGone ? '✅' : '❌');

// ── No new crashes after data arrived ────────────────────────
const postLoadCrashes = jsErrors.filter(e => e.includes('TypeError'));
console.log('No crashes after data loaded:', postLoadCrashes.length === 0 ? '✅' : '❌ ' + postLoadCrashes[0]);

await playwright_screenshot({ name: 'test-X.Y-scenario-4-loaded' });
```

**Also add this as a separate scenario — RTK Query crash on unexpected shapes:**

```javascript
// Scenario 4b — Component handles null/empty API response without crashing

const jsErrors4b = [];
page.on('pageerror', err => jsErrors4b.push(err.message));

// Test 1: API returns { success: true, data: null }
await page.route('**/api/**', async route => {
  await route.fulfill({
    status: 200,
    contentType: 'application/json',
    body: JSON.stringify({ success: true, data: null }),
  });
});
await playwright_navigate({ url: 'http://localhost:5173/[route]' });
await page.waitForLoadState('networkidle');

const crashOnNull = jsErrors4b.filter(e => e.includes('Cannot read properties of null'));
console.log('No crash on null data:', crashOnNull.length === 0 ? '✅' : '❌ ' + crashOnNull[0]);

// Test 2: API returns empty object {}
await page.route('**/api/**', async route => {
  await route.fulfill({
    status: 200,
    contentType: 'application/json',
    body: JSON.stringify({}),
  });
});
await page.reload();
await page.waitForLoadState('networkidle');

const crashOnEmpty = jsErrors4b.filter(e => e.includes('TypeError'));
console.log('No crash on empty response:', crashOnEmpty.length === 0 ? '✅' : '❌ ' + crashOnEmpty[0]);

await playwright_screenshot({ name: 'test-X.Y-scenario-4b-crash-guard' });
```

---

### Type 5 — Empty State (required for all list components)

Tests that empty state UI shows when no data is returned:

```javascript
// Mock API to return empty array
await page.route('**/api/public/case-studies**', async (route) => {
  await route.fulfill({
    status: 200,
    contentType: 'application/json',
    body: JSON.stringify({
      success: true,
      data: [],
      page: 0,
      size: 10,
      totalElements: 0,
      totalPages: 0,
    }),
  });
});

await playwright_navigate({ url: 'http://localhost:5173/case-studies' });
await page.waitForLoadState('networkidle');

// Verify empty state shown
const emptyState = page.locator('[data-testid="empty-state"]');
const emptyVisible = await emptyState.isVisible();
console.log('Empty state shown:', emptyVisible ? '✅' : '❌');

await playwright_screenshot({ name: 'test-X.Y-scenario-5-empty-state' });
```

---

### Type 6 — Error State (required for all API-connected components)

Tests that error UI shows when API fails:

```javascript
// Mock API to return 500 error
await page.route('**/api/public/blogs**', async (route) => {
  await route.fulfill({
    status: 500,
    contentType: 'application/json',
    body: JSON.stringify({ success: false, error: { message: 'Server error' } }),
  });
});

await playwright_navigate({ url: 'http://localhost:5173/blog' });
await page.waitForLoadState('networkidle');

// Verify error state shown
const errorState = page.locator('[data-testid="error-state"]');
const errorVisible = await errorState.isVisible();
console.log('Error state shown:', errorVisible ? '✅' : '❌');

// Verify no raw error message leaked to user
const rawError = await page.locator('text=500').isVisible();
console.log('Raw error not shown to user:', !rawError ? '✅' : '❌');

await playwright_screenshot({ name: 'test-X.Y-scenario-6-error-state' });
```

---

### Type 7 — Filter / Search (required for all filterable lists)

Tests that filter interactions update the displayed content:

```javascript
await playwright_navigate({ url: 'http://localhost:5173/case-studies' });
await page.waitForSelector('[data-testid="case-study-card"]');

// Count initial cards
const initialCount = await page.locator('[data-testid="case-study-card"]').count();
console.log('Initial cards:', initialCount);

// Click Migration filter
await playwright_click({ selector: '[data-testid="filter-migration"]' });
await page.waitForLoadState('networkidle');

// Verify URL updated
const url = page.url();
console.log('URL updated with filter:', url.includes('migration') ? '✅' : '⚠️');

// Verify cards changed
const filteredCount = await page.locator('[data-testid="case-study-card"]').count();
console.log('Cards after filter:', filteredCount, filteredCount !== initialCount ? '✅ Changed' : '⚠️ Same');

// Verify all shown cards have correct category badge
const badges = await page.locator('[data-testid="category-badge"]').allTextContents();
const allCorrect = badges.every(b => b.toLowerCase().includes('migration'));
console.log('All cards match filter:', allCorrect ? '✅' : '❌');

await playwright_screenshot({ name: 'test-X.Y-scenario-7-filtered' });
```

---

## Inspection Code Blocks

### Network Monitoring — Add to Every Scenario

```javascript
// Add at start of scenario — before navigation
const networkLog = [];
const consoleErrors = [];

page.on('request', req => {
  if (req.url().includes('/api/')) {
    networkLog.push({
      method: req.method(),
      url: req.url(),
      hasAuth: !!req.headers()['authorization'],
    });
  }
});

page.on('response', res => {
  if (res.url().includes('/api/')) {
    networkLog.push({
      url: res.url(),
      status: res.status(),
    });
  }
});

page.on('console', msg => {
  if (msg.type() === 'error') {
    consoleErrors.push(msg.text());
  }
});

// Add at end of scenario — after all actions
console.log('\n📡 Network Log:');
networkLog.forEach(entry => {
  const status = entry.status;
  const icon = status >= 200 && status < 300 ? '✅' : '❌';
  console.log(`  ${icon} ${entry.method || ''} ${entry.url} ${status || ''}`);
});

console.log('\n🖥 Console Errors:', consoleErrors.length === 0 ? '✅ None' : `❌ ${consoleErrors.length}`);
if (consoleErrors.length > 0) console.log(consoleErrors);
```

---

### Redux Store Validation

```javascript
// Read Redux store state from browser
const reduxState = await page.evaluate(() => {
  // RTK Query cache and auth slice
  const state = window.__REDUX_STORE__?.getState();
  return {
    isAuthenticated: state?.auth?.isAuthenticated,
    hasToken: !!state?.auth?.token,
    userEmail: state?.auth?.user?.email,
  };
});

console.log('Redux auth state:', JSON.stringify(reduxState, null, 2));
console.log('Is authenticated:', reduxState?.isAuthenticated ? '✅' : '❌');
console.log('Has token:', reduxState?.hasToken ? '✅' : '❌');
```

---

### Responsive Testing

```javascript
// Test all three viewports per scenario
const viewports = [
  { name: 'Desktop', width: 1280, height: 720 },
  { name: 'Tablet', width: 768, height: 1024 },
  { name: 'Mobile', width: 375, height: 812 },
];

for (const viewport of viewports) {
  await page.setViewportSize({ width: viewport.width, height: viewport.height });
  await page.waitForTimeout(300);

  const bodyVisible = await page.locator('body').isVisible();
  const noHorizontalScroll = await page.evaluate(() =>
    document.body.scrollWidth <= window.innerWidth
  );

  console.log(`${viewport.name}:`,
    bodyVisible && noHorizontalScroll ? '✅' : '❌',
    `(${viewport.width}x${viewport.height})`
  );

  await playwright_screenshot({
    name: `test-X.Y-responsive-${viewport.name.toLowerCase()}`
  });
}
```

---

### Accessibility Check

```javascript
// Run after page load in every scenario
const a11yIssues = await page.evaluate(() => {
  const issues = [];

  // Check all images have alt text
  document.querySelectorAll('img').forEach(img => {
    if (!img.alt) issues.push(`Missing alt: ${img.src}`);
  });

  // Check all inputs have labels
  document.querySelectorAll('input, select, textarea').forEach(input => {
    const id = input.id;
    const label = id ? document.querySelector(`label[for="${id}"]`) : null;
    const ariaLabel = input.getAttribute('aria-label');
    if (!label && !ariaLabel) issues.push(`Missing label: ${input.name || input.type}`);
  });

  // Check error messages have role="alert"
  document.querySelectorAll('.error, .text-red-600').forEach(el => {
    if (!el.getAttribute('role')) issues.push(`Missing role="alert" on: ${el.textContent}`);
  });

  // Check icon buttons have aria-label
  document.querySelectorAll('button').forEach(btn => {
    const hasText = btn.textContent?.trim().length > 0;
    const hasAriaLabel = btn.getAttribute('aria-label');
    if (!hasText && !hasAriaLabel) issues.push('Icon button missing aria-label');
  });

  return issues;
});

console.log('Accessibility issues:', a11yIssues.length === 0 ? '✅ None' : `❌ ${a11yIssues.length}`);
if (a11yIssues.length > 0) a11yIssues.forEach(i => console.log('  ❌', i));
```

---

## Test Results File Template

```markdown
# Test Results: Task X.Y — [Task Name]

**Task**: X.Y — [Task Name]
**Test File**: `.claude/Tests/TestX/Task X.Y.md`
**Execution Date**: [Timestamp]
**Duration**: [X minutes Y seconds]

---

## Summary

[✅ ALL TESTS PASSED | ❌ X TESTS FAILED]

- Total Scenarios: X
- Passed: X
- Failed: X
- Pass Rate: X%
- Console Errors: None / X found
- Network Errors: None / X found

---

## Scenario Results

### Scenario 1: [Name]
**Status**: ✅ PASSED / ❌ FAILED
**Duration**: Xm Ys

**Results**:
- ✅ [Outcome verified]
- ✅ [Outcome verified]

**Screenshot**: `test-X.Y-scenario-1.png`

---

[Repeat per scenario]

---

## Network Analysis

| Request | Status | Auth Header | Notes |
|---|---|---|---|
| GET /api/public/blogs | 200 ✅ | No | Public endpoint |
| POST /api/admin/blogs | 201 ✅ | Bearer ✅ | Admin endpoint |

---

## Console Analysis

**Errors**: 0 ✅
**Warnings**: 0 ✅

---

## Responsive Results

| Viewport | Status | Notes |
|---|---|---|
| Desktop 1280x720 | ✅ | — |
| Tablet 768x1024 | ✅ | — |
| Mobile 375x812 | ✅ | — |

---

## Accessibility Results

**Issues Found**: 0 ✅

---

## Issues Found & Fixed

| Issue | Severity | Fix Applied | Time |
|---|---|---|---|
| [Issue] | High/Med/Low | [Fix] | X min |

---

## Regression Results

| Check | Status |
|---|---|
| Home page loads | ✅ |
| TypeScript build passes | ✅ |
| Admin login works | ✅ |
| Public API accessible | ✅ |

---

## Final Verdict

✅ TASK X.Y COMPLETED / ❌ TASK X.Y FAILED — FIX REQUIRED

**Ready for Next Task**: Yes / No
```

---

## Regression Smoke Test — Run After Every Task

```javascript
// Standard regression suite — run at end of every task test
async function runRegressionTests(page) {
  console.log('\n🔁 REGRESSION SMOKE TESTS');
  console.log('─────────────────────────');

  const checks = [];

  // 1. Home page loads
  await playwright_navigate({ url: 'http://localhost:5173' });
  await page.waitForLoadState('networkidle');
  const homeLoads = await page.locator('body').isVisible();
  checks.push({ name: 'Home page loads', pass: homeLoads });

  // 2. Public API accessible
  const apiCheck = await page.evaluate(async () => {
    try {
      const res = await fetch('http://localhost:8080/api/public/stats');
      return res.ok;
    } catch { return false; }
  });
  checks.push({ name: 'Public API accessible', pass: apiCheck });

  // 3. Admin login page loads
  await playwright_navigate({ url: 'http://localhost:5173/admin/login' });
  const loginLoads = await page.locator('input[type="email"]').isVisible();
  checks.push({ name: 'Admin login page loads', pass: loginLoads });

  // 4. TypeScript build passes
  // Checked separately via: npm run build
  // Agent must run: npm run build → verify 0 errors before marking task complete

  // 5. No console errors on home page
  const consoleErrors = [];
  page.on('console', msg => {
    if (msg.type() === 'error') consoleErrors.push(msg.text());
  });
  await playwright_navigate({ url: 'http://localhost:5173' });
  await page.waitForLoadState('networkidle');
  checks.push({ name: 'No console errors on home', pass: consoleErrors.length === 0 });

  // Report
  console.log('');
  checks.forEach(c => {
    console.log(`  ${c.pass ? '✅' : '❌'} ${c.name}`);
  });

  const allPassed = checks.every(c => c.pass);
  console.log(`\nRegression: ${allPassed ? '✅ ALL PASSED' : '❌ FAILURES FOUND'}`);
  return allPassed;
}
```

---

## Quality Gates — Cannot Mark Task Complete Unless

**Build:**
- ✅ `npm run build` → 0 TypeScript errors
- ✅ `npm run lint` → 0 lint errors

**Scenarios:**
- ✅ All scenarios pass (100% pass rate)
- ✅ Happy path works end-to-end
- ✅ Form validation shows inline errors
- ✅ Loading state shown during API calls
- ✅ Error state shown when API fails
- ✅ Empty state shown when no data
- ✅ Auth guard redirects unauthenticated users

**Network:**
- ✅ Correct API endpoints called
- ✅ Admin endpoints include Authorization header
- ✅ Public endpoints called without auth header
- ✅ No 4xx or 5xx errors in happy path

**Console:**
- ✅ Zero console errors on page load
- ✅ Zero console errors during interactions
- ✅ Zero unhandled promise rejections

**Accessibility:**
- ✅ All inputs have labels
- ✅ All error messages have `role="alert"`
- ✅ All icon buttons have `aria-label`
- ✅ Keyboard tab navigation works

**Responsive:**
- ✅ Desktop (1280x720) — no broken layout
- ✅ Tablet (768x1024) — no horizontal scroll
- ✅ Mobile (375x812) — no horizontal scroll

**Regression:**
- ✅ Home page still loads
- ✅ Admin login still works
- ✅ Public API still accessible
- ✅ Previous pages not broken

---

## data-testid Convention

Every interactive or verifiable UI element must have a `data-testid` attribute. Playwright selects elements by `data-testid` — never by CSS class or text content (which change with design updates).

**Naming pattern: `[feature]-[element]-[modifier]`**

```typescript
// ✅ Correct
<div data-testid="blog-card" />
<div data-testid="blog-filter" />
<button data-testid="blog-publish-btn" />
<p data-testid="blog-card-title" />
<span data-testid="category-badge" />
<div data-testid="empty-state" />
<div data-testid="error-state" />
<div data-testid="skeleton" />
<form data-testid="consultation-form" />
<div data-testid="success-message" />

// ❌ Wrong — fragile selectors
page.locator('.blog-card')           // CSS class can change
page.locator('text=Read More')       // Text can change
page.locator('div > div > h3')       // Structure can change
```

**Required data-testid elements per component type:**

| Component Type | Required data-testid |
|---|---|
| List page | `[feature]-card`, `[feature]-filter`, `empty-state`, `error-state`, `skeleton` |
| Detail page | `[feature]-title`, `[feature]-content`, `[feature]-meta` |
| Form | `[feature]-form`, `success-message`, field error IDs |
| Admin table | `[feature]-row`, `[feature]-edit-btn`, `[feature]-delete-btn` |
| Admin editor | `[feature]-editor`, `[feature]-publish-btn`, `[feature]-save-btn` |
| Navigation | `navbar`, `sidebar`, `[link-name]-link` |

---

## ✅ Always Do This

1. Generate test file before executing — never test without a written plan
2. One scenario per acceptance criterion — traceability is mandatory
3. Screenshot after every scenario — visual evidence required
4. Monitor network requests in every scenario — API calls must be verified
5. Monitor console errors in every scenario — zero tolerance
6. Test all three viewports — desktop, tablet, mobile
7. Run regression suite after every task — never skip
8. Use `data-testid` for all selectors — never CSS classes or text
9. Mock API responses for empty/error states — never depend on real data
10. Record exact results in test results file — every scenario documented

---

## ❌ Never Do This

1. Mark task complete without all scenarios passing
2. Skip loading/error/empty state scenarios — they are mandatory
3. Use CSS class or text selectors in Playwright — always `data-testid`
4. Skip the regression suite — previous functionality must be verified
5. Ignore console errors — zero console errors is a hard requirement
6. Skip the network inspection — every API call must be verified
7. Test only happy path — error and edge cases are mandatory
8. Skip responsive testing — mobile must work on every task
9. Skip accessibility check — aria labels and roles are mandatory
10. Test with stale browser state — always start clean