import items from './data/catalog.json';
import { Page } from '@playwright/test';
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

async function addToBasket(page: Page, name: string): Promise<void> {
  const item = items.find((candidate) => candidate.name === name)!;
  await page.goto(`/catalog/${item.id}`);
  await page.getByRole('main').getByRole('button', { name: 'Add to basket' }).click();
  await expect(page.getByText(`${name} added to the basket`)).toBeVisible();
}

test('given items for sale, when opening the shop, then each one shows its name, its price and its stock urgency', async ({
  page,
}) => {
  await page.goto('/catalog');

  const cards = page.getByTestId('catalog-item');

  await expect(cards.getByRole('heading')).toHaveText(['Armchair', 'Chair', 'Lamp']);
  await expect(cards.nth(1)).toContainText('Price: €49.99');
  await expect(cards.nth(1)).toContainText('Medium stock');
  await expect(cards.nth(2)).toContainText('Low stock');
});

test('given items for sale, when searching, then only the names starting with the search stay', async ({
  page,
}) => {
  await page.goto('/catalog');

  await page.getByPlaceholder('Filter by name…').fill('ch');

  await expect(page.getByTestId('catalog-item').getByRole('heading')).toHaveText(['Chair']);
});

test('given an item, when clicking it, then its detail opens and leads back to the catalog', async ({
  page,
}) => {
  await page.goto('/catalog');

  await page.getByRole('link', { name: /Chair/ }).first().click();

  await expect(page).toHaveURL('/catalog/i2');
  await expect(page.getByRole('heading', { level: 1, name: 'Item' })).toBeVisible();
  await expect(page.getByLabel('Name')).toHaveValue('Chair');
  await expect(page.getByLabel('Description')).toHaveValue('Wooden chair');
  await expect(page.getByLabel('Amount of stock')).toHaveValue('7');
  await expect(page).toHaveTitle('Item · Örder');
  await page.getByRole('link', { name: 'Back', exact: true }).click();
  await expect(page).toHaveURL('/catalog');
});

test('given items for sale, when adding some to the basket, then the header counts them, even after a reload', async ({
  page,
}) => {
  await addToBasket(page, 'Chair');
  await page.getByRole('main').getByRole('button', { name: 'Add to basket' }).click();
  await addToBasket(page, 'Lamp');

  await page.reload();

  await expect(page.getByRole('link', { name: 'Basket, 3 items' })).toBeVisible();
});

test('given an item just added, when following the message, then the basket opens', async ({
  page,
}) => {
  await addToBasket(page, 'Lamp');

  await page.getByRole('button', { name: 'View basket' }).click();

  await expect(page).toHaveURL('/basket');
});

test('given items in the basket, when changing an amount and removing a line, then the prices and the total follow', async ({
  page,
}) => {
  await addToBasket(page, 'Chair');
  await addToBasket(page, 'Lamp');
  await page.getByRole('link', { name: 'Basket, 2 items' }).click();

  await page.getByLabel('Amount of Lamp').fill('3');
  await page.getByLabel('Amount of Lamp').press('Tab');
  await expect(page.getByRole('row', { name: /Lamp/ })).toContainText('€87.00');
  await expect(page.getByRole('row', { name: /Total/ })).toContainText('€136.99');
  await page.getByRole('button', { name: 'Remove Chair' }).click();

  await expect(page.getByRole('row', { name: /Total/ })).toContainText('€87.00');
  await expect(page.getByRole('link', { name: 'Basket, 3 items' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Place order' })).toBeVisible();
});
