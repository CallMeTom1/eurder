import { expect, test } from './fixtures';

test.use({ locale: 'en-US' });

test('given the API is down, when opening the shop, then the shop still opens and tells something went wrong', async ({
  page,
}) => {
  await page.route('**/api/auth/me', (route) => route.abort('connectionrefused'));

  await page.goto('/catalog');

  await expect(page.getByRole('heading', { level: 1, name: 'Catalog' })).toBeVisible();
  await expect(page.getByText('Something went wrong. Please try again later.')).toBeVisible();
});

test('given a slow API, when opening the shop, then a splash shows until the shop is ready', async ({
  page,
}) => {
  await page.route('**/api/auth/me', async (route) => {
    await new Promise((resolve) => setTimeout(resolve, 1500));
    await route.fulfill({
      status: 401,
      json: { status: 401, code: 'error.security.authentication-required', params: {} },
    });
  });

  await page.goto('/catalog', { waitUntil: 'commit' });

  await expect(page.getByTestId('splash')).toBeVisible();
  await expect(page.getByRole('heading', { level: 1, name: 'Catalog' })).toBeVisible();
  await expect(page.getByTestId('splash')).toBeHidden();
});
