import { expect, test } from '@playwright/test';
import { createItem, uniqueName } from './helpers';

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
  await page.getByLabel('Search an item').fill(name.toUpperCase());

  const card = page.getByTestId('catalog-item');
  await expect(card.getByRole('heading')).toHaveText([name]);
  await expect(card).toContainText('€49.99');
  await card.getByRole('link').click();
  await expect(page).toHaveURL(`/catalog/${id}`);
  await expect(page.getByRole('heading', { level: 1, name })).toBeVisible();
  await expect(page.getByText('A chair with stripes')).toBeVisible();
});

test('given an unknown item, when opening its address, then the page says it does not exist', async ({
  page,
}) => {
  await page.goto('/catalog/3f2a9c1e-5b7d-4e8a-9c21-000000000000');

  await expect(page.getByRole('alert')).toContainText('This item does not exist.');
});
