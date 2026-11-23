import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { form } from '@angular/forms/signals';
import { digits, notNegative, positive, wholeNumber } from './eurder-validators';

describe('Eurder validators', () => {
  function errorKindsOf(
    price: number | null,
    rule: 'positive' | 'notNegative' | 'digits' | 'wholeNumber',
  ): string[] {
    return TestBed.runInInjectionContext(() => {
      const item = form(signal({ price }), (path) => {
        if (rule === 'positive') {
          positive(path.price);
        } else if (rule === 'notNegative') {
          notNegative(path.price);
        } else if (rule === 'wholeNumber') {
          wholeNumber(path.price);
        } else {
          digits(path.price, 8, 2);
        }
      });
      return item
        .price()
        .errors()
        .map((error) => error.kind);
    });
  }

  it.each([
    { price: 0, kinds: ['positive'] },
    { price: -1, kinds: ['positive'] },
    { price: 0.01, kinds: [] },
    { price: null, kinds: [] },
  ])(
    'given $price, when checking it is positive, then the errors are $kinds',
    ({ price, kinds }) => {
      const errors = errorKindsOf(price, 'positive');

      expect(errors).toEqual(kinds);
    },
  );

  it.each([
    { price: -1, kinds: ['notNegative'] },
    { price: 0, kinds: [] },
    { price: null, kinds: [] },
  ])(
    'given $price, when checking it is not negative, then the errors are $kinds',
    ({ price, kinds }) => {
      const errors = errorKindsOf(price, 'notNegative');

      expect(errors).toEqual(kinds);
    },
  );

  it.each([
    { price: 49.99, kinds: [] },
    { price: 12345678.5, kinds: [] },
    { price: 49.999, kinds: ['digits'] },
    { price: 123456789, kinds: ['digits'] },
    { price: null, kinds: [] },
  ])(
    'given $price, when checking at most 8 digits and 2 decimals, then the errors are $kinds',
    ({ price, kinds }) => {
      const errors = errorKindsOf(price, 'digits');

      expect(errors).toEqual(kinds);
    },
  );

  it.each([
    { price: 15, kinds: [] },
    { price: 0, kinds: [] },
    { price: 1.5, kinds: ['wholeNumber'] },
    { price: null, kinds: [] },
  ])(
    'given $price, when checking it is a whole number, then the errors are $kinds',
    ({ price, kinds }) => {
      const errors = errorKindsOf(price, 'wholeNumber');

      expect(errors).toEqual(kinds);
    },
  );

  it('given too many digits, when reading the error, then it tells the limits', () => {
    const error = TestBed.runInInjectionContext(
      () =>
        form(signal({ price: 49.999 }), (path) => digits(path.price, 8, 2))
          .price()
          .errors()[0],
    );

    expect(error).toMatchObject({ kind: 'digits', integer: 8, fraction: 2 });
  });
});
