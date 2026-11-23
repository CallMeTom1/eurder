import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { form } from '@angular/forms/signals';
import { digits, notNegative, positive } from './eurder-validators';

describe('Eurder validators', () => {
  function errorKindsOf(
    price: number | null,
    rule: 'positive' | 'notNegative' | 'digits',
  ): string[] {
    return TestBed.runInInjectionContext(() => {
      const item = form(signal({ price }), (path) => {
        if (rule === 'positive') {
          positive(path.price);
        } else if (rule === 'notNegative') {
          notNegative(path.price);
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
    [0, ['positive']],
    [-1, ['positive']],
    [0.01, []],
    [null, []],
  ])('given %s, when checking it is positive, then the errors are %o', (price, kinds) => {
    const errors = errorKindsOf(price, 'positive');

    expect(errors).toEqual(kinds);
  });

  it.each([
    [-1, ['notNegative']],
    [0, []],
    [null, []],
  ])('given %s, when checking it is not negative, then the errors are %o', (price, kinds) => {
    const errors = errorKindsOf(price, 'notNegative');

    expect(errors).toEqual(kinds);
  });

  it.each([
    [49.99, []],
    [12345678.5, []],
    [49.999, ['digits']],
    [123456789, ['digits']],
    [null, []],
  ])(
    'given %s, when checking at most 8 digits and 2 decimals, then the errors are %o',
    (price, kinds) => {
      const errors = errorKindsOf(price, 'digits');

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
