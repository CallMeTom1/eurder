import { expect, test } from './fixtures';

test.use({ locale: 'en-US' });

test('given an English browser, when choosing Nederlands, then the page speaks Dutch and keeps it after a reload', async ({
  page,
}) => {
  await page.goto('/design-system');
  await expect(page.getByRole('button', { name: 'Theme' })).toBeVisible();

  await page.getByRole('button', { name: 'Language' }).click();
  await page.getByRole('menuitemradio', { name: 'Nederlands' }).click();

  await expect(page.getByRole('button', { name: 'Thema' })).toBeVisible();
  await expect(page.locator('html')).toHaveAttribute('lang', 'nl');
  await page.reload();
  await expect(page.getByRole('button', { name: 'Thema' })).toBeVisible();
});

test.describe('with a Belgian French browser', () => {
  test.use({ locale: 'fr-BE' });

  test('given no choice yet, when opening the app, then it speaks French', async ({ page }) => {
    await page.goto('/design-system');

    await expect(page.getByRole('button', { name: 'Thème' })).toBeVisible();
    await expect(page.locator('html')).toHaveAttribute('lang', 'fr');
  });
});
