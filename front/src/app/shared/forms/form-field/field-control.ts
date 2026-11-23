import { Directive, ElementRef, inject } from '@angular/core';
import { MatInput } from '@angular/material/input';
import { MatSelect } from '@angular/material/select';
import { FormFieldComponent } from './form-field';

@Directive({ selector: '[appFieldControl]' })
export class FieldControl {
  constructor() {
    const formField = inject(FormFieldComponent);
    const input = inject(MatInput, { optional: true, self: true });
    const select = inject(MatSelect, { optional: true, self: true });
    if (input) {
      input.id = formField.controlId;
    } else if (select) {
      select.ariaLabelledby = formField.labelId;
      formField.labelsAComponent();
    } else {
      inject<ElementRef<HTMLElement>>(ElementRef).nativeElement.id = formField.controlId;
    }
  }
}
