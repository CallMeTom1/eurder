import { ValidationError } from '@angular/forms/signals';
import { FieldErrorPipe } from './field-error-pipe';

describe('FieldErrorPipe', () => {
  const pipe = new FieldErrorPipe();

  it.each<{
    error: { kind: string } & Record<string, unknown>;
    key: string;
    params: Record<string, unknown>;
  }>([
    { error: { kind: 'required' }, key: 'validation.required', params: {} },
    { error: { kind: 'email' }, key: 'validation.invalid-format', params: {} },
    { error: { kind: 'pattern', pattern: /^\d+$/ }, key: 'validation.invalid-format', params: {} },
    { error: { kind: 'minLength', minLength: 8 }, key: 'validation.too-short', params: { min: 8 } },
    {
      error: { kind: 'maxLength', maxLength: 255 },
      key: 'validation.too-long',
      params: { max: 255 },
    },
    { error: { kind: 'max', max: 1000 }, key: 'validation.too-large', params: { max: 1000 } },
    { error: { kind: 'min', min: 15 }, key: 'validation.too-small', params: { min: 15 } },
    { error: { kind: 'wholeNumber' }, key: 'validation.invalid-format', params: {} },
    { error: { kind: 'positive' }, key: 'validation.must-be-positive', params: {} },
    { error: { kind: 'notNegative' }, key: 'validation.must-not-be-negative', params: {} },
    {
      error: { kind: 'digits', integer: 8, fraction: 2 },
      key: 'validation.too-many-digits',
      params: { integer: 8, fraction: 2 },
    },
    {
      error: { kind: 'api', code: 'validation.too-long', params: { max: 50 } },
      key: 'validation.too-long',
      params: { max: 50 },
    },
    { error: { kind: 'somethingElse' }, key: 'validation.invalid', params: {} },
  ])('given the error $error, when translating, then the key is $key', ({ error, key, params }) => {
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
