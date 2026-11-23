import { computed, Directive, input, signal } from '@angular/core';
import { AbstractControl, NG_VALIDATORS, ValidationErrors, Validator } from '@angular/forms';

@Directive({
  selector: '[appCharCounter]',
  exportAs: 'charCounter',
  providers: [{ provide: NG_VALIDATORS, useExisting: CharCounter, multi: true }],
})
export class CharCounter implements Validator {
  readonly max = input.required<number>({ alias: 'appCharCounter' });

  private readonly length = signal(0);

  readonly remaining = computed(() => Math.max(0, this.max() - this.length()));
  readonly exceeded = computed(() => this.length() > this.max());

  validate(control: AbstractControl<string | null>): ValidationErrors | null {
    const length = control.value?.length ?? 0;
    this.length.set(length);
    return length > this.max()
      ? { maxlength: { requiredLength: this.max(), actualLength: length } }
      : null;
  }
}
