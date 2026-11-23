import { Page } from '@playwright/test';
import { expect, test } from './fixtures';

test.use({ locale: 'en-US' });

async function fillIn(page: Page): Promise<void> {
  await page.getByLabel('First name').fill('Jane');
  await page.getByLabel('Last name').fill('Doe');
  await page.getByLabel('Email').fill('jane@doe.be');
  await page.getByLabel('Password', { exact: true }).fill('secret123');
  await page.getByLabel('Phone number').fill('0470 12 34 56');
  await page.getByLabel('Street').fill('Rue Haute');
  await page.getByLabel('Number', { exact: true }).fill('1');
  await page.getByLabel('Postal code').fill('1000');
  await page.getByLabel('City').fill('Bruxelles');
}

test('given a new visitor, when registering in France, then the API gets the customer and the login confirms the account', async ({
  page,
}) => {
  let sent: unknown;
  await page.route('**/api/customers', (route) => {
    sent = route.request().postDataJSON();
    return route.fulfill({ status: 201, json: { id: 'c1' } });
  });
  await page.goto('/catalog');
  await page.getByRole('link', { name: 'Log in' }).click();
  await page.getByRole('link', { name: 'Create an account' }).click();

  await fillIn(page);
  await page.getByLabel('Country').click();
  await page.getByRole('option', { name: 'France' }).click();
  await page.getByRole('button', { name: 'Create my account' }).click();

  await expect(page).toHaveURL('/login?registered=true');
  await expect(page.getByRole('status')).toHaveText('Your account is ready. You can log in now.');
  expect(sent).toMatchObject({
    email: 'jane@doe.be',
    address: { city: 'Bruxelles', country: 'FRANCE' },
  });
});

test('given an email already used, when registering, then the email field says so', async ({
  page,
}) => {
  await page.route('**/api/customers', (route) =>
    route.fulfill({
      status: 409,
      json: {
        status: 409,
        code: 'error.security.email-already-used',
        params: { email: 'jane@doe.be' },
      },
    }),
  );
  await page.goto('/register');

  await fillIn(page);
  await page.getByRole('button', { name: 'Create my account' }).click();

  await expect(page.getByText('An account already exists for jane@doe.be.')).toBeVisible();
  await expect(page).toHaveURL('/register');
});
