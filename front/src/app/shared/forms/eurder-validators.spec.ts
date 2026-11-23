import { FormControl } from '@angular/forms';
import { EurderValidators } from './eurder-validators';

describe('EurderValidators', () => {
  it.each([
    [0, { positive: true }],
    [-1, { positive: true }],
    [0.01, null],
    [null, null],
  ])('given %s, when checking it is positive, then the errors are %o', (value, errors) => {
    const control = new FormControl<number | null>(value);

    const result = EurderValidators.positive(control);

    expect(result).toEqual(errors);
  });

  it.each([
    [-1, { notNegative: true }],
    [0, null],
    [null, null],
  ])('given %s, when checking it is not negative, then the errors are %o', (value, errors) => {
    const control = new FormControl<number | null>(value);

    const result = EurderValidators.notNegative(control);

    expect(result).toEqual(errors);
  });

  it.each([
    [49.99, null],
    [12345678.5, null],
    [49.999, { digits: { integer: 8, fraction: 2 } }],
    [123456789, { digits: { integer: 8, fraction: 2 } }],
    [null, null],
  ])(
    'given %s, when checking at most 8 digits and 2 decimals, then the errors are %o',
    (value, errors) => {
      const control = new FormControl<number | null>(value);

      const result = EurderValidators.digits(8, 2)(control);

      expect(result).toEqual(errors);
    },
  );
});
