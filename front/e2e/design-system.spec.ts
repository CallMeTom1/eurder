import { expect, test } from '@playwright/test';

const PRIMARY_LIGHT = 'rgb(69, 96, 129)';
const PRIMARY_DARK = 'rgb(173, 201, 238)';

test('given a light system theme, when showing the design system, then Tailwind and Material share the primary color of the palette', async ({
  page,
}) => {
  await page.emulateMedia({ colorScheme: 'light' });

  await page.goto('/design-system');

  await expect(page.getByTestId('swatch-primary')).toHaveCSS('background-color', PRIMARY_LIGHT);
  await expect(page.getByRole('button', { name: 'Primary action' })).toHaveCSS(
    'background-color',
    PRIMARY_LIGHT,
  );
});

test('given a dark system theme, when showing the design system, then the same classes switch to the dark palette', async ({
  page,
}) => {
  await page.emulateMedia({ colorScheme: 'dark' });

  await page.goto('/design-system');

  await expect(page.getByTestId('swatch-primary')).toHaveCSS('background-color', PRIMARY_DARK);
  await expect(page.getByRole('button', { name: 'Primary action' })).toHaveCSS(
    'background-color',
    PRIMARY_DARK,
  );
});
