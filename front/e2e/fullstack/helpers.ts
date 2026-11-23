import { APIRequestContext, expect, Page } from '@playwright/test';

export const ADMIN = { email: 'admin@eurder.be', password: 'admin' };

const CSRF_TOKEN = 'csrf-token-of-the-full-stack-tests';

export function uniqueEmail(name: string): string {
  return `${name}.${Date.now()}.${Math.floor(Math.random() * 1_000_000)}@eurder.test`;
}

export async function registerCustomer(
  request: APIRequestContext,
  email: string,
  password = 'secret123',
): Promise<void> {
  const response = await request.post('/api/customers', {
    headers: { Cookie: `XSRF-TOKEN=${CSRF_TOKEN}`, 'X-XSRF-TOKEN': CSRF_TOKEN },
    data: {
      firstName: 'Jane',
      lastName: 'Doe',
      email,
      password,
      phoneNumber: '0470 12 34 56',
      address: {
        street: 'Rue Haute',
        houseNumber: '1',
        postalCode: '1000',
        city: 'Bruxelles',
        country: 'BELGIUM',
      },
    },
  });
  expect(response.status()).toBe(201);
}

export async function logIn(page: Page, email: string, password: string): Promise<void> {
  await page.getByLabel('Email').fill(email);
  await page.getByLabel('Password', { exact: true }).fill(password);
  await page.getByRole('button', { name: 'Log in' }).click();
}

export async function createItem(
  playwright: {
    request: { newContext: (options: { baseURL: string }) => Promise<APIRequestContext> };
  },
  baseURL: string,
  item: { name: string; description: string; price: number; units: number },
): Promise<string> {
  const admin = await playwright.request.newContext({ baseURL });
  const headers = { Cookie: `XSRF-TOKEN=${CSRF_TOKEN}`, 'X-XSRF-TOKEN': CSRF_TOKEN };
  const login = await admin.post('/api/auth/login', { headers, data: ADMIN });
  expect(login.status()).toBe(200);
  const accessToken = (await admin.storageState()).cookies.find(
    (cookie) => cookie.name === 'access_token',
  );
  const created = await admin.post('/api/items', {
    headers: { ...headers, Cookie: `${headers.Cookie}; access_token=${accessToken?.value}` },
    data: {
      name: item.name,
      description: item.description,
      price: item.price,
      units: { PRISTINE: item.units },
    },
  });
  expect(created.status()).toBe(201);
  const id: string = (await created.json()).id;
  await admin.dispose();
  return id;
}

export function uniqueName(name: string): string {
  return `${name} ${Date.now()}${Math.floor(Math.random() * 1000)}`;
}
