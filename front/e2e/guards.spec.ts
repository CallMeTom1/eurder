import { expect, loggedInAs, test } from './fixtures';

test.describe('as an anonymous visitor', () => {
  test('given no account, when opening the back office, then the login asks first and remembers where to go', async ({
    page,
  }) => {
    await page.goto('/admin/customers');

    await expect(page).toHaveURL('/login?returnUrl=%2Fadmin%2Fcustomers');
  });

  test('given no account, when opening my orders, then the login asks first', async ({ page }) => {
    await page.goto('/my-orders');

    await expect(page).toHaveURL('/login?returnUrl=%2Fmy-orders');
  });
});

test.describe('as a customer', () => {
  test.beforeEach(async ({ page }) => loggedInAs(page, 'CUSTOMER'));

  test('given a customer, when opening the back office, then they stay in the shop', async ({
    page,
  }) => {
    await page.goto('/admin');

    await expect(page).toHaveURL('/catalog');
  });

  test('given a customer, when opening my orders, then the page opens', async ({ page }) => {
    await page.goto('/my-orders');

    await expect(page).toHaveURL('/my-orders');
  });

  test('given a customer, when opening the login, then they go back to the shop', async ({
    page,
  }) => {
    await page.goto('/login');

    await expect(page).toHaveURL('/catalog');
  });
});

test.describe('as an admin', () => {
  test.beforeEach(async ({ page }) => loggedInAs(page, 'ADMIN'));

  test('given an admin, when opening the back office, then it opens', async ({ page }) => {
    await page.goto('/admin');

    await expect(page).toHaveURL('/admin/items');
  });

  test('given an admin, when opening my orders, then they stay in the shop', async ({ page }) => {
    await page.goto('/my-orders');

    await expect(page).toHaveURL('/catalog');
  });
});
