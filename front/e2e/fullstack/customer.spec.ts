import { expect, test } from '@playwright/test';
import { logIn, registerCustomer, uniqueEmail } from './helpers';

test.use({ locale: 'en-US' });

test('given a new visitor, when registering then logging in, then the shop knows them by name', async ({
  page,
}) => {
  const email = uniqueEmail('jane');
  await page.goto('/register');

  await page.getByLabel('First name').fill('Jane');
  await page.getByLabel('Last name').fill('Doe');
  await page.getByLabel('Email').fill(email);
  await page.getByLabel('Password', { exact: true }).fill('secret123');
  await page.getByLabel('Phone number').fill('0470 12 34 56');
  await page.getByLabel('Street').fill('Rue Haute');
  await page.getByLabel('Number', { exact: true }).fill('1');
  await page.getByLabel('Postal code').fill('1000');
  await page.getByLabel('City').fill('Bruxelles');
  await page.getByRole('button', { name: 'Create my account' }).click();
  await expect(page.getByRole('status')).toHaveText('Your account is ready. You can log in now.');
  await logIn(page, email, 'secret123');

  await expect(page).toHaveURL('/catalog');
  await expect(page.getByRole('button', { name: 'Jane Doe' })).toBeVisible();
});

test('given an email already used, when registering again, then the email field says so', async ({
  page,
  request,
}) => {
  const email = uniqueEmail('jane');
  await registerCustomer(request, email);
  await page.goto('/register');

  await page.getByLabel('First name').fill('Jane');
  await page.getByLabel('Last name').fill('Doe');
  await page.getByLabel('Email').fill(email);
  await page.getByLabel('Password', { exact: true }).fill('secret123');
  await page.getByLabel('Phone number').fill('0470 12 34 56');
  await page.getByLabel('Street').fill('Rue Haute');
  await page.getByLabel('Number', { exact: true }).fill('1');
  await page.getByLabel('Postal code').fill('1000');
  await page.getByLabel('City').fill('Bruxelles');
  await page.getByRole('button', { name: 'Create my account' }).click();

  await expect(page.getByText(`An account already exists for ${email}.`)).toBeVisible();
});

test('given a wrong password, when logging in, then the page says so', async ({
  page,
  request,
}) => {
  const email = uniqueEmail('jane');
  await registerCustomer(request, email);
  await page.goto('/login');

  await logIn(page, email, 'not-the-password');

  await expect(page.getByRole('alert')).toContainText('Wrong email or password.');
});

test('given a customer, when opening the back office, then they stay in the shop and keep their orders', async ({
  page,
  request,
}) => {
  const email = uniqueEmail('jane');
  await registerCustomer(request, email);
  await page.goto('/my-orders');
  await logIn(page, email, 'secret123');
  await expect(page).toHaveURL('/my-orders');

  await page.goto('/admin');

  await expect(page).toHaveURL('/catalog');
});
