import { SchemaPath, validate, ValidationError } from '@angular/forms/signals';

export interface DigitsValidationError extends ValidationError {
  readonly kind: 'digits';
  readonly integer: number;
  readonly fraction: number;
}

export function positive(path: SchemaPath<number | null>): void {
  validate(path, ({ value }) => {
    const number = value();
    return number !== null && number <= 0 ? { kind: 'positive' } : undefined;
  });
}

export function notNegative(path: SchemaPath<number | null>): void {
  validate(path, ({ value }) => {
    const number = value();
    return number !== null && number < 0 ? { kind: 'notNegative' } : undefined;
  });
}

export function digits(path: SchemaPath<number | null>, integer: number, fraction: number): void {
  validate(path, ({ value }) => {
    const number = value();
    if (number === null) {
      return undefined;
    }
    const [integerPart, fractionPart = ''] = Math.abs(number).toString().split('.');
    if (integerPart.length <= integer && fractionPart.length <= fraction) {
      return undefined;
    }
    const error: DigitsValidationError = { kind: 'digits', integer, fraction };
    return error;
  });
}

export function wholeNumber(path: SchemaPath<number | null>): void {
  validate(path, ({ value }) => {
    const number = value();
    return number !== null && !Number.isInteger(number) ? { kind: 'wholeNumber' } : undefined;
  });
}

export interface AtMostValidationError extends ValidationError {
  readonly kind: 'maxLength';
  readonly maxLength: number;
}

export function atMost(path: SchemaPath<string>, max: number): void {
  validate(path, ({ value }) => {
    if (value().length <= max) {
      return undefined;
    }
    const error: AtMostValidationError = { kind: 'maxLength', maxLength: max };
    return error;
  });
}
