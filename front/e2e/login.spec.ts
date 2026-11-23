import { Page } from '@playwright/test';
import { expect, loggedInAs, test } from './fixtures';

test.use({ locale: 'en-US' });

const jane = {
  id: '3f2a9c1e-5b7d-4e8a-9c21-7d4e5f6a8b90',
  role: 'CUSTOMER',
  email: 'jane@doe.be',
  firstName: 'Jane',
  lastName: 'Doe',
};

async function apiAccepts(page: Page, user: object): Promise<void> {
  await page.route('**/api/auth/login', (route) => route.fulfill({ json: user }));
}

async function logIn(page: Page): Promise<void> {
  await page.getByLabel('Email').fill('jane@doe.be');
  await page.getByLabel('Password').fill('secret123');
  await page.getByRole('button', { name: 'Log in' }).click();
}

test('given a customer in the shop, when logging in from the header, then the header shows their name', async ({
  page,
}) => {
  await apiAccepts(page, jane);
  await page.goto('/catalog');

  await page.getByRole('link', { name: 'Log in' }).click();
  await logIn(page);

  await expect(page).toHaveURL('/catalog');
  await expect(page.getByRole('button', { name: 'Jane Doe' })).toBeVisible();
});

test('given a protected page, when logging in, then the visitor goes back to it', async ({
  page,
}) => {
  await apiAccepts(page, jane);
  await page.goto('/my-orders');
  await expect(page).toHaveURL('/login?returnUrl=%2Fmy-orders');

  await logIn(page);

  await expect(page).toHaveURL('/my-orders');
  await expect(page.getByRole('heading', { level: 1, name: 'My orders' })).toBeVisible();
});

test('given wrong credentials, when logging in, then the page says so', async ({ page }) => {
  await page.route('**/api/auth/login', (route) =>
    route.fulfill({
      status: 401,
      json: { status: 401, code: 'error.security.invalid-credentials', params: {} },
    }),
  );
  await page.goto('/login');

  await logIn(page);

  await expect(page.getByRole('alert')).toContainText('Wrong email or password.');
  await expect(page).toHaveURL('/login');
});

test('given a logged-in customer, when logging out, then the session ends and the header offers to log in', async ({
  page,
}) => {
  await loggedInAs(page, 'CUSTOMER');
  let loggedOut = false;
  await page.route('**/api/auth/logout', (route) => {
    loggedOut = true;
    return route.fulfill({ status: 204 });
  });
  await page.goto('/my-orders');

  await page.getByRole('button', { name: 'Jane Doe' }).click();
  await page.getByRole('menuitem', { name: 'Log out' }).click();

  await expect(page).toHaveURL('/catalog');
  await expect(page.getByRole('link', { name: 'Log in' })).toBeVisible();
  expect(loggedOut).toBe(true);
});

test('given an admin in the shop, when opening their menu, then it leads to the back office', async ({
  page,
}) => {
  await loggedInAs(page, 'ADMIN');
  await page.goto('/catalog');

  await page.getByRole('button', { name: 'admin@eurder.be' }).click();
  await page.getByRole('menuitem', { name: 'Back office' }).click();

  await expect(page).toHaveURL('/admin/items');
});
