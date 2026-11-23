import { expect, test } from '@playwright/test';

test.use({ locale: 'fr-BE' });

test('given a French page, when leaving fields invalid, then each one shows the same message as the API in French', async ({
  page,
}) => {
  await page.goto('/design-system');
  const name = page.getByLabel('Name');
  const price = page.getByLabel('Price', { exact: true });

  await name.focus();
  await name.blur();
  await price.fill('-1');
  await price.blur();

  await expect(page.getByText('Ce champ est obligatoire.')).toBeVisible();
  await expect(page.getByText('Doit être supérieur à 0.')).toBeVisible();
  await price.fill('49.999');
  await expect(page.getByText('Au maximum 8 chiffres avant la virgule et 2 après.')).toBeVisible();
});
