import { toCatalogItem } from './catalog-item';

describe('toCatalogItem', () => {
  it.each([
    [49.99, 4999],
    [0.29, 29],
    [1234.5, 123450],
    [10, 1000],
  ])(
    'given a price of %s euros, when mapping, then it becomes %s cents without any rounding error',
    (price, cents) => {
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
