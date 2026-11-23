import { expect, test } from '@playwright/test';
import { ADMIN, logIn } from './helpers';

test.use({ locale: 'en-US' });

test('given an anonymous visitor, when opening the shop, then the API says nobody is logged in and the shop opens', async ({
  page,
}) => {
  const me = page.waitForResponse('**/api/auth/me');

  await page.goto('/catalog');

  expect((await me).status()).toBe(401);
  await expect(page.getByRole('link', { name: 'Log in' })).toBeVisible();
  expect((await page.context().cookies()).map((cookie) => cookie.name)).toContain('XSRF-TOKEN');
});

test('given the admin, when logging in from the back office address, then they land in the back office and stay after a reload', async ({
  page,
}) => {
  await page.goto('/admin/customers');
  await expect(page).toHaveURL('/login?returnUrl=%2Fadmin%2Fcustomers');

  await logIn(page, ADMIN.email, ADMIN.password);

  await expect(page).toHaveURL('/admin/customers');
  await page.reload();
  await expect(page.getByRole('heading', { level: 1, name: 'Customers' })).toBeVisible();
  await expect(page.getByRole('button', { name: ADMIN.email })).toBeVisible();
});
