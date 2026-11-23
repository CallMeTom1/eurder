import { expect, test } from './fixtures';

const PRIMARY_LIGHT = 'rgb(69, 96, 129)';
const PRIMARY_DARK = 'rgb(173, 201, 238)';

test('given a light system, when choosing the dark theme, then the page turns dark and stays dark after a reload', async ({
  page,
}) => {
  await page.emulateMedia({ colorScheme: 'light' });
  await page.goto('/design-system');
  const primary = page.getByTestId('swatch-primary');
  await expect(primary).toHaveCSS('background-color', PRIMARY_LIGHT);

  await page.getByRole('button', { name: 'Theme' }).click();
  await page.getByRole('menuitemradio', { name: 'Dark' }).click();

  await expect(primary).toHaveCSS('background-color', PRIMARY_DARK);
  await page.reload();
  await expect(primary).toHaveCSS('background-color', PRIMARY_DARK);
});

test('given a dark choice, when choosing the system theme again, then the page follows a light system', async ({
  page,
}) => {
  await page.emulateMedia({ colorScheme: 'light' });
  await page.goto('/design-system');
  await page.getByRole('button', { name: 'Theme' }).click();
  await page.getByRole('menuitemradio', { name: 'Dark' }).click();

  await page.getByRole('button', { name: 'Theme' }).click();
  await page.getByRole('menuitemradio', { name: 'System' }).click();

  await expect(page.getByTestId('swatch-primary')).toHaveCSS('background-color', PRIMARY_LIGHT);
});
