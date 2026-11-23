import { toCatalogItem } from './catalog-item';

describe('toCatalogItem', () => {
  it.each([
    { price: 49.99, cents: 4999 },
    { price: 0.29, cents: 29 },
    { price: 1234.5, cents: 123450 },
    { price: 10, cents: 1000 },
  ])(
    'given a price of $price euros, when mapping, then it becomes $cents cents without any rounding error',
    ({ price, cents }) => {
      const item = toCatalogItem({ id: 'i1', name: 'Chair', description: 'Wooden chair', price });

      expect(item.priceInCents).toBe(cents);
    },
  );

  it('given an item of the API, when mapping, then keeps its id, name and description', () => {
    const item = toCatalogItem({
      id: 'i1',
      name: 'Chair',
      description: 'Wooden chair',
      price: 49.99,
    });

    expect(item).toEqual({
      id: 'i1',
      name: 'Chair',
      description: 'Wooden chair',
      priceInCents: 4999,
    });
  });
});
