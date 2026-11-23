import { expect, test } from './fixtures';

test.use({ locale: 'en-US' });

test('given the shop, when opening the root, then it shows the catalog with a translated tab title', async ({
  page,
}) => {
  await page.goto('/');

  await expect(page).toHaveURL('/catalog');
  await expect(page).toHaveTitle('Catalog · Eurder');
  await expect(page.getByRole('heading', { level: 1, name: 'Catalog' })).toBeVisible();
});

test('given the shop, when following its navigation, then each page opens in the same layout', async ({
  page,
}) => {
  await page.goto('/catalog');
  const navigation = page.getByRole('navigation', { name: 'Main' });

  await navigation.getByRole('link', { name: 'Basket' }).click();

  await expect(page).toHaveURL('/basket');
  await expect(page.getByRole('heading', { level: 1, name: 'Basket' })).toBeVisible();
  await expect(navigation.getByRole('link', { name: 'Basket' })).toHaveAttribute(
    'aria-current',
    'page',
  );
});

test('given the back office, when opening it, then it shows the items in the admin layout', async ({
  page,
}) => {
  await page.goto('/admin');

  await expect(page).toHaveURL('/admin/items');
  await expect(page).toHaveTitle('Items · Eurder');
  const navigation = page.getByRole('navigation', { name: 'Back office' });
  await navigation.getByRole('link', { name: 'Customers' }).click();
  await expect(page.getByRole('heading', { level: 1, name: 'Customers' })).toBeVisible();
  await navigation.getByRole('link', { name: 'Back to the shop' }).click();
  await expect(page).toHaveURL('/catalog');
});

test('given an unknown address, when opening it, then the page says it does not exist', async ({
  page,
}) => {
  await page.goto('/no-such-page');

  await expect(page.getByRole('heading', { level: 1, name: 'Page not found' })).toBeVisible();
  await expect(page.getByText('This page does not exist.')).toBeVisible();
});

test.describe('in Dutch', () => {
  test.use({ locale: 'nl-BE' });

  test('given a Dutch browser, when opening the catalog, then the tab title is in Dutch', async ({
    page,
  }) => {
    await page.goto('/catalog');

    await expect(page).toHaveTitle('Catalogus · Eurder');
  });
});
