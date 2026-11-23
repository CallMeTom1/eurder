import { Component, input } from '@angular/core';
import { FieldTree } from '@angular/forms/signals';
import { TranslatePipe } from '@ngx-translate/core';
import { FieldErrorPipe } from '../field-error-pipe';

@Component({
  imports: [FieldErrorPipe, TranslatePipe],
  selector: 'app-field-errors',
  template: `
    @if (field()().errors() | fieldError; as error) {
      {{ error.key | translate: error.params }}
    }
  `,
})
export class FieldErrors {
  readonly field = input.required<FieldTree<unknown>>();
}
