import { expect, test as base } from '@playwright/test';

export const test = base.extend<{ anonymousVisitor: void }>({
  anonymousVisitor: [
    async ({ page }, use) => {
      await page.route('**/api/auth/me', (route) =>
        route.fulfill({
          status: 401,
          json: { status: 401, code: 'error.security.authentication-required', params: {} },
        }),
      );
      await use();
    },
    { auto: true },
  ],
});

export { expect };
