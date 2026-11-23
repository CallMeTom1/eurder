import { expect, test } from './fixtures';

test('given a confirmation, when the user confirms, then the page gets a yes', async ({ page }) => {
  await page.goto('/design-system');
  await page.getByRole('button', { name: 'Delete the chair' }).click();
  const dialog = page.getByRole('alertdialog', { name: 'Delete the chair?' });

  await dialog.getByRole('button', { name: 'Delete' }).click();

  await expect(dialog).toBeHidden();
  await expect(page.getByTestId('confirm-answer')).toHaveText('Confirmed');
});

test('given a confirmation, when the user presses escape, then the page gets a no', async ({
  page,
}) => {
  await page.goto('/design-system');
  await page.getByRole('button', { name: 'Delete the chair' }).click();
  await expect(page.getByRole('alertdialog')).toBeVisible();

  await page.keyboard.press('Escape');

  await expect(page.getByRole('alertdialog')).toBeHidden();
  await expect(page.getByTestId('confirm-answer')).toHaveText('Cancelled');
});
