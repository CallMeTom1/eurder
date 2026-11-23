import { expect, test } from './fixtures';

test('given the description field, when typing past 255 characters, then the counter stays at 0 in red and the field is invalid', async ({
  page,
}) => {
  await page.goto('/design-system');
  const description = page.getByLabel('Description');
  const counter = page.getByTestId('description-counter');
  await expect(counter).toHaveText('255 characters left');

  await description.fill('x'.repeat(250));
  await expect(counter).toHaveText('5 characters left');
  await description.fill('x'.repeat(256));
  await description.blur();

  await expect(counter).toHaveText('0 characters left');
  await expect(counter).toHaveCSS('color', 'rgb(186, 26, 26)');
  await expect(description).toHaveAttribute('aria-invalid', 'true');
});
