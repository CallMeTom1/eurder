// Fills a running Eurder API with a demo catalog, customers and orders.
// Usage: node demo/seed.mjs [api base url], by default http://localhost:8080/api
// Running it again skips the items, customers and orders that already exist.

import { readFile } from 'node:fs/promises';

const API = (process.argv[2] ?? 'http://localhost:8080/api').replace(/\/$/, '');
const IMAGES = new URL('./images/', import.meta.url);
const CSRF = 'demo-seed-csrf-token';
const ADMIN = { email: 'admin@eurder.be', password: 'admin' };
const CUSTOMER_PASSWORD = 'demo1234';

const ITEMS = [
  { image: 'oak-dining-table', name: 'Oak dining table', price: 649, pristine: 6, description: 'Solid oak table for six, with a natural oil finish.' },
  { image: 'wooden-chair', name: 'Library chair', price: 189, pristine: 18, description: 'Wooden chair with a high back, made for long reading sessions.' },
  { image: 'leather-armchair', name: 'Antique leather armchair', price: 890, pristine: 2, description: 'Carved wooden armchair with brown leather seat and back.' },
  { image: 'three-seat-sofa', name: 'Gondola sofa', price: 1290, pristine: 3, slightlyDamaged: 1, description: 'Curved three-seat sofa in cognac velvet.' },
  { image: 'brass-desk-lamp', name: 'Bauhaus desk lamp', price: 129, pristine: 14, description: 'Adjustable desk lamp with a lacquered shade, a design classic.' },
  { image: 'floor-lamp', name: 'Wooden floor lamp', price: 349, pristine: 7, description: 'Teak floor lamp with two adjustable arms.' },
  { image: 'bookcase', name: 'Tall bookcase', price: 459, pristine: 5, damaged: 1, description: 'Steel and wood bookcase with eight shelves.' },
  { image: 'office-chair', name: 'Ergonomic office chair', price: 329, pristine: 22, description: 'Office chair with headrest, lumbar support and adjustable arms.' },
  { image: 'coffee-table', name: 'Reclaimed wood coffee table', price: 279, pristine: 9, description: 'Low table made of reclaimed planks, each one unique.' },
  { image: 'double-bed', name: 'Upholstered double bed', price: 990, pristine: 4, description: 'Double bed with a padded headboard, 160 x 200 cm.' },
  { image: 'wardrobe', name: 'Two-door wardrobe', price: 720, pristine: 3, description: 'Solid wood wardrobe with a hanging rail and two shelves.' },
  { image: 'wool-rug', name: 'Persian wool rug', price: 1450, pristine: 1, description: 'Hand-knotted wool rug, 200 x 300 cm.' },
  { image: 'wall-mirror', name: 'Rattan wall mirror', price: 89, pristine: 25, description: 'Round mirror in a woven rattan frame, 60 cm.' },
  { image: 'ceramic-vase', name: 'White ceramic vase', price: 45, pristine: 30, slightlyDamaged: 3, description: 'Hexagonal vase in glazed white ceramic.' },
  { image: 'wall-clock', name: 'Quartz wall clock', price: 39, pristine: 12, description: 'Silent wall clock with brass numbers, 30 cm.' },
  { image: 'potted-plant', name: 'Ponytail palm', price: 59, pristine: 8, description: 'Easy-care houseplant, delivered in its grey pot.' },
  { image: 'bar-stool', name: 'Bentwood bar stool', price: 119, pristine: 16, description: 'Bentwood stool with a footrest, 75 cm high.' },
  { image: 'writing-desk', name: 'Oak writing desk', price: 399, pristine: 6, description: 'Simple oak desk, 140 x 70 cm, room for a laptop and more.' },
  { image: 'pendant-lamp', name: 'Origami pendant lamp', price: 219, pristine: 4, description: 'Pendant lamp made of folded white panels.' },
];

const CUSTOMERS = [
  { firstName: 'Emma', lastName: 'Peeters', email: 'emma.peeters@eurder.demo', phoneNumber: '+32 470 11 22 33', address: { street: 'Rue de la Loi', houseNumber: '16', postalCode: '1000', city: 'Bruxelles', country: 'BELGIUM' } },
  { firstName: 'Lucas', lastName: 'Martin', email: 'lucas.martin@eurder.demo', phoneNumber: '+33 6 12 34 56 78', address: { street: 'Rue de Rivoli', houseNumber: '42', postalCode: '75001', city: 'Paris', country: 'FRANCE' } },
  { firstName: 'Hannah', lastName: 'Schmidt', email: 'hannah.schmidt@eurder.demo', phoneNumber: '+49 151 2345 6789', address: { street: 'Unter den Linden', houseNumber: '5', postalCode: '10117', city: 'Berlin', country: 'GERMANY' } },
];

const ORDERS = {
  'emma.peeters@eurder.demo': [
    [['Oak dining table', 1], ['Library chair', 6]],
    [['White ceramic vase', 2]],
  ],
  'lucas.martin@eurder.demo': [[['Ergonomic office chair', 1], ['Oak writing desk', 1], ['Bauhaus desk lamp', 1]]],
  'hannah.schmidt@eurder.demo': [[['Gondola sofa', 1], ['Reclaimed wood coffee table', 1]]],
};

async function call(method, path, { body, token, form } = {}) {
  const headers = { Cookie: `XSRF-TOKEN=${CSRF}${token ? `; access_token=${token}` : ''}`, 'X-XSRF-TOKEN': CSRF };
  if (body) {
    headers['Content-Type'] = 'application/json';
  }
  const response = await fetch(API + path, { method, headers, body: form ?? (body && JSON.stringify(body)) });
  const text = await response.text();
  return { status: response.status, json: text ? JSON.parse(text) : null, cookies: response.headers.getSetCookie() };
}

async function logIn(email, password) {
  const { status, cookies } = await call('POST', '/auth/login', { body: { email, password } });
  if (status !== 200) {
    throw new Error(`Login of ${email} failed with ${status}`);
  }
  return cookies.find((cookie) => cookie.startsWith('access_token=')).split(';')[0].slice('access_token='.length);
}

async function uploadImage(token, name) {
  const form = new FormData();
  form.append('file', new Blob([await readFile(new URL(`${name}.jpg`, IMAGES))], { type: 'image/jpeg' }), `${name}.jpg`);
  const { status, json } = await call('POST', '/images', { token, form });
  if (status !== 201) {
    throw new Error(`Upload of ${name} failed with ${status}`);
  }
  return json.path;
}

const admin = await logIn(ADMIN.email, ADMIN.password);
const catalog = (await call('GET', '/catalog')).json;
const idsByName = new Map(catalog.map((item) => [item.name, item.id]));

for (const item of ITEMS) {
  if (idsByName.has(item.name)) {
    console.log(`= ${item.name}`);
    continue;
  }
  const units = { PRISTINE: item.pristine, SLIGHTLY_DAMAGED: item.slightlyDamaged ?? 0, DAMAGED: item.damaged ?? 0 };
  const image = await uploadImage(admin, item.image);
  const { status, json } = await call('POST', '/items', {
    token: admin,
    body: { name: item.name, description: item.description, price: item.price, units, image },
  });
  if (status !== 201) {
    throw new Error(`Creation of ${item.name} failed with ${status}: ${JSON.stringify(json)}`);
  }
  idsByName.set(item.name, json.id);
  console.log(`+ ${item.name}`);
}

for (const customer of CUSTOMERS) {
  const { status } = await call('POST', '/customers', { body: { ...customer, password: CUSTOMER_PASSWORD } });
  if (status === 409) {
    console.log(`= ${customer.email}`);
    continue;
  }
  if (status !== 201) {
    throw new Error(`Registration of ${customer.email} failed with ${status}`);
  }
  console.log(`+ ${customer.email}`);
  const token = await logIn(customer.email, CUSTOMER_PASSWORD);
  for (const lines of ORDERS[customer.email] ?? []) {
    const itemGroups = lines.map(([name, amount]) => ({ itemId: idsByName.get(name), amount }));
    const placed = await call('POST', '/eurders', { token, body: { itemGroups } });
    if (placed.status !== 201) {
      throw new Error(`Order of ${customer.email} failed with ${placed.status}: ${JSON.stringify(placed.json)}`);
    }
    console.log(`  + order of ${lines.length} line(s), ${placed.json.totalPrice} EUR`);
  }
}

console.log(`Done. Customers log in with the password ${CUSTOMER_PASSWORD}.`);
