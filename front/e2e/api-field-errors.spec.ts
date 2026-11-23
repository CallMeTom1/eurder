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
