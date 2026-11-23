import items from './data/catalog.json';
import { expect, test } from './fixtures';

test.use({ locale: 'en-US' });

test.beforeEach(async ({ page }) => {
  await page.route('**/api/catalog', (route) => route.fulfill({ json: items }));
  await page.route('**/api/catalog/*', (route) => {
    const id = route.request().url().split('/').pop();
    const item = items.find((candidate) => candidate.id === id);
    return item ? route.fulfill({ json: item }) : route.fulfill({ status: 404 });
  });
});

test('given items for sale, when opening the shop, then each one shows its name and price', async ({
  page,
}) => {
  await page.goto('/catalog');

  const cards = page.getByTestId('catalog-item');

  await expect(cards.getByRole('heading')).toHaveText(['Armchair', 'Chair', 'Lamp']);
  await expect(cards.nth(1)).toContainText('€49.99');
});

test('given items for sale, when searching, then only the names starting with the search stay', async ({
  page,
}) => {
  await page.goto('/catalog');

  await page.getByLabel('Search an item').fill('ch');

  await expect(page.getByTestId('catalog-item').getByRole('heading')).toHaveText(['Chair']);
});

test('given an item, when clicking it, then its detail opens and leads back to the catalog', async ({
  page,
}) => {
  await page.goto('/catalog');

  await page.getByRole('link', { name: /Chair/ }).first().click();

  await expect(page).toHaveURL('/catalog/i2');
  await expect(page.getByRole('heading', { level: 1, name: 'Chair' })).toBeVisible();
  await expect(page.getByText('Wooden chair')).toBeVisible();
  await expect(page).toHaveTitle('Item · Eurder');
  await page.getByRole('link', { name: 'Back to the catalog' }).click();
  await expect(page).toHaveURL('/catalog');
});
