import { ValidationError } from '@angular/forms/signals';
import { FieldErrorPipe } from './field-error-pipe';

describe('FieldErrorPipe', () => {
  const pipe = new FieldErrorPipe();

  it.each<[{ kind: string } & Record<string, unknown>, string, Record<string, unknown>]>([
    [{ kind: 'required' }, 'validation.required', {}],
    [{ kind: 'email' }, 'validation.invalid-format', {}],
    [{ kind: 'pattern', pattern: /^\d+$/ }, 'validation.invalid-format', {}],
    [{ kind: 'minLength', minLength: 8 }, 'validation.too-short', { min: 8 }],
    [{ kind: 'maxLength', maxLength: 255 }, 'validation.too-long', { max: 255 }],
    [{ kind: 'max', max: 1000 }, 'validation.too-large', { max: 1000 }],
    [{ kind: 'positive' }, 'validation.must-be-positive', {}],
    [{ kind: 'notNegative' }, 'validation.must-not-be-negative', {}],
    [
      { kind: 'digits', integer: 8, fraction: 2 },
      'validation.too-many-digits',
      { integer: 8, fraction: 2 },
    ],
    [
      { kind: 'api', code: 'validation.too-long', params: { max: 50 } },
      'validation.too-long',
      { max: 50 },
    ],
    [{ kind: 'somethingElse' }, 'validation.invalid', {}],
  ])('given the error %o, when translating, then the key is %s', (error, key, params) => {
    const translated = pipe.transform([error as ValidationError]);

    expect(translated).toEqual({ key, params });
  });

  it('given several errors, when translating, then only the first one is shown', () => {
    const errors = [{ kind: 'required' }, { kind: 'maxLength', maxLength: 5 }] as ValidationError[];

    const translated = pipe.transform(errors);

    expect(translated?.key).toBe('validation.required');
  });

  it('given a valid field, when translating, then there is nothing to show', () => {
    const translated = pipe.transform([]);

    expect(translated).toBeNull();
  });
});
