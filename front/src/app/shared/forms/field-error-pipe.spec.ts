import { ValidationErrors } from '@angular/forms';
import { FieldErrorPipe } from './field-error-pipe';

describe('FieldErrorPipe', () => {
  const pipe = new FieldErrorPipe();

  it.each<[ValidationErrors, string, Record<string, unknown>]>([
    [{ required: true }, 'validation.required', {}],
    [{ email: true }, 'validation.invalid-format', {}],
    [{ pattern: { requiredPattern: '^\d+$' } }, 'validation.invalid-format', {}],
    [{ minlength: { requiredLength: 8, actualLength: 3 } }, 'validation.too-short', { min: 8 }],
    [
      { maxlength: { requiredLength: 255, actualLength: 300 } },
      'validation.too-long',
      { max: 255 },
    ],
    [{ max: { max: 1000, actual: 1001 } }, 'validation.too-large', { max: 1000 }],
    [{ positive: true }, 'validation.must-be-positive', {}],
    [{ notNegative: true }, 'validation.must-not-be-negative', {}],
    [
      { digits: { integer: 8, fraction: 2 } },
      'validation.too-many-digits',
      { integer: 8, fraction: 2 },
    ],
    [{ somethingElse: true }, 'validation.invalid', {}],
  ])('given the errors %o, when translating, then the key is %s', (errors, key, params) => {
    const error = pipe.transform(errors);

    expect(error).toEqual({ key, params });
  });

  it('given an error of the API, when translating, then the key and the params are those of the API', () => {
    const error = pipe.transform({ api: { code: 'validation.too-long', params: { max: 50 } } });

    expect(error).toEqual({ key: 'validation.too-long', params: { max: 50 } });
  });

  it('given several errors, when translating, then only the first one is shown', () => {
    const error = pipe.transform({ required: true, maxlength: { requiredLength: 5 } });

    expect(error?.key).toBe('validation.required');
  });

  it('given a valid field, when translating, then there is nothing to show', () => {
    const error = pipe.transform(null);

    expect(error).toBeNull();
  });
});
