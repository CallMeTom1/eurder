import { expect, test } from '@playwright/test';

test('given the app, when opening it, then the tab is titled Eurder', async ({ page }) => {
  await page.goto('/');

  await expect(page).toHaveTitle('Eurder');
});
