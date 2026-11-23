import { expect, Page, test as base } from '@playwright/test';

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

export async function loggedInAs(page: Page, role: 'ADMIN' | 'CUSTOMER'): Promise<void> {
  const user =
    role === 'ADMIN'
      ? { email: 'admin@eurder.be', firstName: null, lastName: null }
      : { email: 'jane@doe.be', firstName: 'Jane', lastName: 'Doe' };
  await page.route('**/api/auth/me', (route) =>
    route.fulfill({ json: { id: '3f2a9c1e-5b7d-4e8a-9c21-7d4e5f6a8b90', role, ...user } }),
  );
}
