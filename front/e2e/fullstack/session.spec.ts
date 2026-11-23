import { expect, test } from '@playwright/test';
import { ADMIN, logIn, registerCustomer, uniqueEmail } from './helpers';

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

test('given a logged-in customer, when logging out, then the session cookies are gone and a reload stays anonymous', async ({
  page,
  request,
}) => {
  const email = uniqueEmail('jane');
  await registerCustomer(request, email);
  await page.goto('/login');
  await logIn(page, email, 'secret123');
  await expect(page.getByRole('button', { name: 'Jane Doe' })).toBeVisible();

  await page.getByRole('button', { name: 'Jane Doe' }).click();
  await page.getByRole('menuitem', { name: 'Log out' }).click();

  await expect(page.getByRole('link', { name: 'Log in' })).toBeVisible();
  const cookies = (await page.context().cookies()).map((cookie) => cookie.name);
  expect(cookies).not.toContain('access_token');
  expect(cookies).not.toContain('refresh_token');
  await page.reload();
  await expect(page.getByRole('link', { name: 'Log in' })).toBeVisible();
});

test('given an expired access token, when reloading, then the session is renewed without the user noticing', async ({
  page,
  request,
}) => {
  const email = uniqueEmail('jane');
  await registerCustomer(request, email);
  await page.goto('/login');
  await logIn(page, email, 'secret123');
  await expect(page.getByRole('button', { name: 'Jane Doe' })).toBeVisible();
  await page.waitForTimeout(6000);
  const refresh = page.waitForResponse('**/api/auth/refresh');

  await page.reload();

  expect((await refresh).status()).toBe(200);
  await expect(page.getByRole('button', { name: 'Jane Doe' })).toBeVisible();
});

test('given a logout in another tab, when reloading a protected page, then the login asks again', async ({
  page,
  context,
  request,
}) => {
  const email = uniqueEmail('jane');
  await registerCustomer(request, email);
  await page.goto('/my-orders');
  await logIn(page, email, 'secret123');
  await expect(page).toHaveURL('/my-orders');
  const otherTab = await context.newPage();
  await otherTab.goto('/catalog');
  await otherTab.getByRole('button', { name: 'Jane Doe' }).click();
  await otherTab.getByRole('menuitem', { name: 'Log out' }).click();
  await expect(otherTab.getByRole('link', { name: 'Log in' })).toBeVisible();

  await page.reload();

  await expect(page).toHaveURL('/login?returnUrl=%2Fmy-orders');
});
