import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

function numberOf(control: AbstractControl): number | null {
  const value: unknown = control.value;
  return typeof value === 'number' && !Number.isNaN(value) ? value : null;
}

export const EurderValidators = {
  positive(control: AbstractControl): ValidationErrors | null {
    const value = numberOf(control);
    return value !== null && value <= 0 ? { positive: true } : null;
  },

  notNegative(control: AbstractControl): ValidationErrors | null {
    const value = numberOf(control);
    return value !== null && value < 0 ? { notNegative: true } : null;
  },

  digits(integer: number, fraction: number): ValidatorFn {
    return (control) => {
      const value = numberOf(control);
      if (value === null) {
        return null;
      }
      const [integerPart, fractionPart = ''] = Math.abs(value).toString().split('.');
      return integerPart.length > integer || fractionPart.length > fraction
        ? { digits: { integer, fraction } }
        : null;
    };
  },
};
