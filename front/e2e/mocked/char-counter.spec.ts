import { expect, test } from './fixtures';

test('given the description field, when typing past 255 characters, then the counter stays at 0 in red and the field says it is too long', async ({
  page,
}) => {
  await page.goto('/design-system');
  const description = page.getByLabel('Description');
  const counter = page.getByTestId('description-counter').locator('span');
  await expect(counter).toHaveText('255 characters left');

  await description.fill('x'.repeat(250));
  await expect(counter).toHaveText('5 characters left');
  await description.pressSequentially('x'.repeat(10));
  await description.blur();

  await expect(description).toHaveValue('x'.repeat(260));
  await expect(counter).toHaveText('0 characters left');
  await expect(counter).toHaveCSS('color', 'rgb(186, 26, 26)');
  await expect(description).toHaveAttribute('aria-invalid', 'true');
  await expect(page.getByText('At most 255 characters.')).toBeVisible();
});
