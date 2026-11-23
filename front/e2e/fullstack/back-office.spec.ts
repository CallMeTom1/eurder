import { expect, test } from '@playwright/test';
import { ADMIN, createItem, logIn, registerCustomer, uniqueEmail, uniqueName } from './helpers';

test.use({ locale: 'en-US' });

test('given the admin, when creating then editing an item, then the shop shows the new details, the added stock and the picture', async ({
  page,
}) => {
  const name = uniqueName('Admin chair');
  await page.goto('/login');
  await logIn(page, ADMIN.email, ADMIN.password);
  await expect(page).toHaveURL('/admin/items');

  await page.getByRole('link', { name: 'New item' }).click();
  await page.getByLabel('Name').fill(name);
  await page.getByLabel('Description').fill('A chair made by the admin');
  await page.getByLabel('Price').fill('59.90');
  await page.getByLabel('Amount of stock').fill('3');
  await page.locator('input[type="file"]').setInputFiles('../demo/images/wooden-chair.jpg');
  await expect(page.getByTestId('picture-preview')).toBeVisible();
  await page.getByRole('button', { name: 'Create', exact: true }).click();
  await expect(page.getByRole('heading', { level: 1, name })).toBeVisible();
  await page.getByRole('link', { name: 'Edit' }).click();
  await page.getByLabel('Description').fill('An oak chair made by the admin');
  await page.getByLabel('Amount of stock').fill('12');
  await page.getByRole('button', { name: 'Update' }).click();

  await expect(page.getByText('An oak chair made by the admin')).toBeVisible();
  await expect(page.getByText('High stock')).toBeVisible();
  await expect(page.getByRole('img', { name })).toHaveAttribute('src', /^\/api\/images\/.+\.jpg$/);
});

test('given the admin, when creating then editing a customer, then the detail shows the changes', async ({
  page,
}) => {
  const email = uniqueEmail('created');
  await page.goto('/login');
  await logIn(page, ADMIN.email, ADMIN.password);
  await page.getByRole('link', { name: 'Customers' }).click();

  await page.getByRole('link', { name: 'New customer' }).click();
  await page.getByLabel('First name').fill('Carla');
  await page.getByLabel('Last name').fill('Created');
  await page.getByLabel('Email').fill(email);
  await page.getByLabel('Password', { exact: true }).fill('secret123');
  await page.getByLabel('Phone number').fill('0470 12 34 56');
  await page.getByLabel('Street').fill('Rue Haute');
  await page.getByLabel('Number', { exact: true }).fill('1');
  await page.getByLabel('Postal code').fill('1000');
  await page.getByLabel('City').fill('Bruxelles');
  await page.getByRole('button', { name: 'Create the customer' }).click();
  await expect(page.getByRole('heading', { level: 1, name: 'Customer' })).toBeVisible();
  await page.getByRole('link', { name: 'Edit' }).click();
  await page.getByLabel('City').fill('Liège');
  await page.getByRole('button', { name: 'Update' }).click();

  await expect(page.getByText('Rue Haute 1, 1000 Liège, Belgium')).toBeVisible();
  await expect(page.getByText('No order yet.')).toBeVisible();
});

test('given a customer who ordered, when they order it again and the admin opens their detail, then both orders are reported', async ({
  page,
  browser,
  playwright,
  baseURL,
  request,
}) => {
  const id = await createItem(playwright, baseURL!, {
    name: uniqueName('Report lamp'),
    description: 'A lamp to report',
    price: 29,
    units: 10,
  });
  const email = uniqueEmail('reported');
  await registerCustomer(request, email);
  await page.goto('/login');
  await logIn(page, email, 'secret123');
  await expect(page).toHaveURL('/catalog');
  await page.goto(`/catalog/${id}`);
  await page.getByRole('main').getByRole('button', { name: 'Add to basket' }).click();
  await page.goto('/basket');
  await page.getByRole('button', { name: 'Place order' }).click();
  await page.getByRole('button', { name: 'OK' }).click();

  await page.goto('/my-orders');
  await page.getByRole('button', { name: 'Order again' }).click();
  await expect(page.getByText('Order placed again.')).toBeVisible();
  await expect(page.getByTestId('eurder')).toHaveCount(2);
  const admin = await (await browser.newContext({ baseURL, locale: 'en-US' })).newPage();
  await admin.goto('/login');
  await logIn(admin, ADMIN.email, ADMIN.password);
  await expect(admin).toHaveURL('/admin/items');
  await admin.getByRole('link', { name: 'Customers' }).click();
  await expect(admin.getByRole('heading', { level: 1, name: 'Customers' })).toBeVisible();
  const customerId = await admin.evaluate(async (wanted) => {
    const response = await fetch('/api/customers');
    const list: { id: string; email: string }[] = await response.json();
    return list.find((customer) => customer.email === wanted)!.id;
  }, email);
  await admin.goto(`/admin/customers/${customerId}`);

  await expect(admin.getByTestId('eurder')).toHaveCount(2);
});
