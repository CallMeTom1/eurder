import { expect, test } from './fixtures';

test.use({ locale: 'en-US' });

test('given errors sent by the API, when applied to the form, then each field shows its own translated message', async ({
  page,
}) => {
  await page.goto('/design-system');

  await page.getByRole('button', { name: 'Simulate API errors' }).click();

  await expect(page.getByText('Must be greater than 0.')).toBeVisible();
  await expect(page.getByText('At most 100 characters.')).toBeVisible();
});

test('given errors sent by the API, when the user changes one field, then only the error of that field goes away', async ({
  page,
}) => {
  await page.goto('/design-system');
  await page.getByRole('button', { name: 'Simulate API errors' }).click();
  await expect(page.getByText('At most 100 characters.')).toBeVisible();

  await page.getByLabel('Name').fill('Table');

  await expect(page.getByText('At most 100 characters.')).toBeHidden();
  await expect(page.getByText('Must be greater than 0.')).toBeVisible();
});
