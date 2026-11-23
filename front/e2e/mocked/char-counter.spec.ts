import { expect, test } from './fixtures';

test('given the description field, when typing past 255 characters, then the browser stops at 255 and the counter at 0', async ({
  page,
}) => {
  await page.goto('/design-system');
  const description = page.getByLabel('Description');
  const counter = page.getByTestId('description-counter').locator('span');
  await expect(counter).toHaveText('255 characters left');

  await description.fill('x'.repeat(250));
  await expect(counter).toHaveText('5 characters left');
  await description.pressSequentially('x'.repeat(10));

  await expect(description).toHaveValue('x'.repeat(255));
  await expect(counter).toHaveText('0 characters left');
  await expect(description).toHaveAttribute('maxlength', '255');
});
