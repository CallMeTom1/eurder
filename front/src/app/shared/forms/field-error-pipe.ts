import { Pipe, PipeTransform } from '@angular/core';
import { ValidationErrors } from '@angular/forms';

export interface FieldError {
  key: string;
  params: Record<string, unknown>;
}

type ErrorTranslation = (error: Record<string, unknown>) => FieldError;

const TRANSLATIONS: Record<string, ErrorTranslation> = {
  api: (error) => ({
    key: String(error['code']),
    params: (error['params'] as Record<string, unknown>) ?? {},
  }),
  required: () => ({ key: 'validation.required', params: {} }),
  email: () => ({ key: 'validation.invalid-format', params: {} }),
  pattern: () => ({ key: 'validation.invalid-format', params: {} }),
  minlength: (error) => ({ key: 'validation.too-short', params: { min: error['requiredLength'] } }),
  maxlength: (error) => ({ key: 'validation.too-long', params: { max: error['requiredLength'] } }),
  max: (error) => ({ key: 'validation.too-large', params: { max: error['max'] } }),
  positive: () => ({ key: 'validation.must-be-positive', params: {} }),
  notNegative: () => ({ key: 'validation.must-not-be-negative', params: {} }),
  digits: (error) => ({
    key: 'validation.too-many-digits',
    params: { integer: error['integer'], fraction: error['fraction'] },
  }),
};

@Pipe({ name: 'fieldError' })
export class FieldErrorPipe implements PipeTransform {
  transform(errors: ValidationErrors | null): FieldError | null {
    const first = errors ? Object.entries(errors)[0] : undefined;
    if (!first) {
      return null;
    }
    const [name, error] = first;
    const translation = TRANSLATIONS[name];
    return translation
      ? translation(typeof error === 'object' && error !== null ? error : {})
      : { key: 'validation.invalid', params: {} };
  }
}
