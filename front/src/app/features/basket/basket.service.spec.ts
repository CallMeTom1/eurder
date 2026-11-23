import { TestBed } from '@angular/core/testing';
import { CatalogItem } from '@features/catalog/catalog-item';
import { BasketService } from './basket.service';

describe('BasketService', () => {
  const chair: CatalogItem = {
    id: 'i1',
    name: 'Chair',
    description: 'Wooden chair',
    priceInCents: 4999,
  };
  const lamp: CatalogItem = {
    id: 'i2',
    name: 'Lamp',
    description: 'Desk lamp',
    priceInCents: 2900,
  };

  function basket(): BasketService {
    return TestBed.inject(BasketService);
  }

  beforeEach(() => localStorage.clear());

  it('given an empty basket, when adding an item, then it holds one of it', () => {
    basket().add(chair);

    expect(basket().lines()).toEqual([
      { itemId: 'i1', name: 'Chair', priceInCents: 4999, amount: 1 },
    ]);
  });

  it('given an item already in the basket, when adding it again, then its amount grows', () => {
    basket().add(chair);

    basket().add(chair);

    expect(basket().lines()).toHaveLength(1);
    expect(basket().lines()[0].amount).toBe(2);
  });

  it('given several lines, when counting, then the count and the total follow the amounts', () => {
    basket().add(chair);
    basket().add(chair);

    basket().add(lamp);

    expect(basket().count()).toBe(3);
    expect(basket().totalInCents()).toBe(2 * 4999 + 2900);
  });

  it('given a line, when changing its amount, then its price follows', () => {
    basket().add(lamp);

    basket().changeAmount('i2', 4);

    expect(basket().lines()[0].amount).toBe(4);
    expect(basket().totalInCents()).toBe(4 * 2900);
  });

  it.each([
    { amount: 0, why: 'zero' },
    { amount: -2, why: 'negative' },
    { amount: 1.5, why: 'not whole' },
    { amount: 1001, why: 'more than the API accepts' },
  ])(
    'given a line, when changing its amount to $amount ($why), then the amount is kept',
    ({ amount }) => {
      basket().add(lamp);

      basket().changeAmount('i2', amount);

      expect(basket().lines()[0].amount).toBe(1);
    },
  );

  it('given a line, when changing its amount to 1000, then it is the most the API accepts', () => {
    basket().add(lamp);

    basket().changeAmount('i2', 1000);

    expect(basket().lines()[0].amount).toBe(1000);
  });

  it('given 1000 of an item, when adding it again, then the amount stays at 1000', () => {
    basket().add(lamp);
    basket().changeAmount('i2', 1000);

    basket().add(lamp);

    expect(basket().lines()[0].amount).toBe(1000);
  });

  it('given two lines, when removing one, then only the other stays', () => {
    basket().add(chair);
    basket().add(lamp);

    basket().remove('i1');

    expect(
      basket()
        .lines()
        .map((line) => line.itemId),
    ).toEqual(['i2']);
  });

  it('given a filled basket, when emptying it, then nothing is left', () => {
    basket().add(chair);

    basket().clear();

    expect(basket().lines()).toEqual([]);
    expect(basket().count()).toBe(0);
  });

  it('given a filled basket, when the page reloads, then the basket is still there', () => {
    basket().add(chair);
    basket().changeAmount('i1', 3);

    TestBed.resetTestingModule();

    expect(basket().lines()).toEqual([
      { itemId: 'i1', name: 'Chair', priceInCents: 4999, amount: 3 },
    ]);
  });

  it.each([
    { stored: 'not json', why: 'broken' },
    { stored: '{"itemId":"i1"}', why: 'not a list' },
    { stored: '[{"itemId":"i1","amount":"two"}]', why: 'with a wrong line' },
  ])('given a $why stored basket, when starting, then the basket is empty', ({ stored }) => {
    localStorage.setItem('eurder.basket', stored);

    const lines = basket().lines();

    expect(lines).toEqual([]);
  });
});
