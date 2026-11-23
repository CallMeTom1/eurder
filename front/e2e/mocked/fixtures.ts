import users from './data/users.json';
import { expect, Page, test as base } from '@playwright/test';

export const test = base.extend<{ anonymousVisitor: void }>({
  anonymousVisitor: [
    async ({ page }, use) => {
      await page.route('**/api/**', (route) => route.abort());
      await page.route('**/api/auth/me', (route) =>
        route.fulfill({
          status: 401,
          json: { status: 401, code: 'error.security.authentication-required', params: {} },
        }),
      );
      await page.route('**/api/catalog', (route) => route.fulfill({ json: [] }));
      await use();
    },
    { auto: true },
  ],
});

export { expect };

export async function loggedInAs(page: Page, role: 'ADMIN' | 'CUSTOMER'): Promise<void> {
  const user = role === 'ADMIN' ? users.admin : users.customer;
  await page.route('**/api/auth/me', (route) => route.fulfill({ json: user }));
}
