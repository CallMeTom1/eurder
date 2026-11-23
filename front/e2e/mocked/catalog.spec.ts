import { expect, test } from './fixtures';

test.use({ locale: 'en-US' });

const items = [
  { id: 'i1', name: 'Armchair', description: 'Leather armchair', price: 150 },
  { id: 'i2', name: 'Chair', description: 'Wooden chair', price: 49.99 },
  { id: 'i3', name: 'Lamp', description: 'Desk lamp', price: 29 },
];

test.beforeEach(async ({ page }) => {
  await page.route('**/api/catalog', (route) => route.fulfill({ json: items }));
  await page.route('**/api/catalog/i2', (route) => route.fulfill({ json: items[1] }));
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
