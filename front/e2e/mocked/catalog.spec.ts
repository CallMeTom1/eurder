import items from './data/catalog.json';
import { Page } from '@playwright/test';
import { expect, loggedInAs, test } from './fixtures';

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
  await expect(page.getByText('Medium stock')).toBeVisible();
  await expect(page).toHaveTitle('Item · Örder');
  await page.getByRole('link', { name: 'Back to the catalog' }).click();
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

test('given an anonymous visitor with a basket, when placing the order, then the login comes first and brings them back to the basket', async ({
  page,
}) => {
  await addToBasket(page, 'Lamp');
  await page.goto('/basket');

  await page.getByRole('button', { name: 'Place order' }).click();

  await expect(page).toHaveURL('/login?returnUrl=%2Fbasket');
});

test('given a customer with a basket, when placing the order, then a dialog confirms it, the basket empties and the catalog opens', async ({
  page,
}) => {
  await loggedInAs(page, 'CUSTOMER');
  let sent: unknown;
  await page.route('**/api/eurders', async (route) => {
    sent = route.request().postDataJSON();
    await route.fulfill({ status: 201, json: { id: 'e1', itemGroups: [], totalPrice: 58 } });
  });
  await addToBasket(page, 'Lamp');
  await page.getByRole('main').getByRole('button', { name: 'Add to basket' }).click();
  await page.goto('/basket');

  await page.getByRole('button', { name: 'Place order' }).click();
  await expect(page.getByRole('dialog', { name: 'Order placed' })).toBeVisible();
  await page.getByRole('button', { name: 'OK' }).click();

  expect(sent).toEqual({ itemGroups: [{ itemId: 'i3', amount: 2 }] });
  await expect(page).toHaveURL('/catalog');
  await expect(page.getByRole('link', { name: 'Basket', exact: true })).toBeVisible();
});
