import { expect, test } from '@playwright/test';
import { createItem, logIn, registerCustomer, uniqueEmail, uniqueName } from './helpers';

test.use({ locale: 'en-US' });

test('given an item added by the admin, when a visitor browses the shop, then they find it, search it and open it', async ({
  page,
  playwright,
  baseURL,
}) => {
  const name = uniqueName('Zebra chair');
  const id = await createItem(playwright, baseURL!, {
    name,
    description: 'A chair with stripes',
    price: 49.99,
    units: 3,
  });

  await page.goto('/catalog');
  await page.getByPlaceholder('Filter by name…').fill(name.toUpperCase());

  const card = page.getByTestId('catalog-item');
  await expect(card.getByRole('heading')).toHaveText([name]);
  await expect(card).toContainText('Price: €49.99');
  await expect(card).toContainText('Low stock');
  await card.getByRole('link').click();
  await expect(page).toHaveURL(`/catalog/${id}`);
  await expect(page.getByLabel('Name')).toHaveValue(name);
  await expect(page.getByLabel('Description')).toHaveValue('A chair with stripes');
  await expect(page.getByLabel('Amount of stock')).toHaveValue('3');
});

test('given an unknown item, when opening its address, then the page says it does not exist', async ({
  page,
}) => {
  await page.goto('/catalog/3f2a9c1e-5b7d-4e8a-9c21-000000000000');

  await expect(page.getByRole('alert')).toContainText('This item does not exist.');
});

test('given a customer with an item in the basket, when placing the order, then the API registers it and the stock goes down', async ({
  page,
  playwright,
  baseURL,
  request,
}) => {
  const id = await createItem(playwright, baseURL!, {
    name: uniqueName('Order lamp'),
    description: 'A lamp to order',
    price: 29,
    units: 5,
  });
  const email = uniqueEmail('orderer');
  await registerCustomer(request, email);
  await page.goto('/login');
  await logIn(page, email, 'secret123');
  await expect(page).toHaveURL('/catalog');
  await page.goto(`/catalog/${id}`);
  await page.getByRole('main').getByRole('button', { name: 'Add to basket' }).click();
  await page.getByRole('main').getByRole('button', { name: 'Add to basket' }).click();
  await page.getByRole('link', { name: 'View basket' }).click();

  await page.getByRole('button', { name: 'Place order' }).click();
  await expect(page.getByRole('dialog', { name: 'Order placed' })).toBeVisible();
  await page.getByRole('button', { name: 'OK' }).click();

  await expect(page).toHaveURL('/catalog');
  const item = await (await request.get(`/api/catalog/${id}`)).json();
  expect(item.stock).toBe(3);
});
