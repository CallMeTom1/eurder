import { Pipe, PipeTransform } from '@angular/core';
import { ValidationError } from '@angular/forms/signals';

export interface FieldError {
  key: string;
  params: Record<string, unknown>;
}

type ErrorTranslation = (error: Record<string, unknown>) => FieldError;

const TRANSLATIONS: Record<string, ErrorTranslation> = {
  api: (error) => ({
    key: String(error['code']),
    params: (error['params'] as Record<string, unknown> | undefined) ?? {},
  }),
  required: () => ({ key: 'validation.required', params: {} }),
  email: () => ({ key: 'validation.invalid-format', params: {} }),
  pattern: () => ({ key: 'validation.invalid-format', params: {} }),
  minLength: (error) => ({ key: 'validation.too-short', params: { min: error['minLength'] } }),
  maxLength: (error) => ({ key: 'validation.too-long', params: { max: error['maxLength'] } }),
  max: (error) => ({ key: 'validation.too-large', params: { max: error['max'] } }),
  min: (error) => ({ key: 'validation.too-small', params: { min: error['min'] } }),
  wholeNumber: () => ({ key: 'validation.invalid-format', params: {} }),
  positive: () => ({ key: 'validation.must-be-positive', params: {} }),
  notNegative: () => ({ key: 'validation.must-not-be-negative', params: {} }),
  digits: (error) => ({
    key: 'validation.too-many-digits',
    params: { integer: error['integer'], fraction: error['fraction'] },
  }),
};

@Pipe({ name: 'fieldError' })
export class FieldErrorPipe implements PipeTransform {
  transform(errors: readonly ValidationError[]): FieldError | null {
    const [first] = errors;
    if (!first) {
      return null;
    }
    const translation = TRANSLATIONS[first.kind];
    return translation
      ? translation(first as unknown as Record<string, unknown>)
      : { key: 'validation.invalid', params: {} };
  }
}
