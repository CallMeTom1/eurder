import { Component, computed, input, signal } from '@angular/core';
import { FieldTree } from '@angular/forms/signals';

let fields = 0;

@Component({
  selector: 'app-form-field',
  template: `
    <label
      class="field-label"
      [class.required]="required()"
      [id]="labelId"
      [attr.for]="labelsANativeControl() ? controlId : null"
      >{{ label() }}</label
    >
    <ng-content />
  `,
  host: { class: 'field' },
})
export class FormFieldComponent {
  readonly label = input.required<string>();
  readonly field = input.required<FieldTree<unknown>>();

  readonly controlId: string;
  readonly labelId: string;

  protected readonly required = computed(() => this.field()().required());
  protected readonly labelsANativeControl = signal(true);

  constructor() {
    fields += 1;
    this.controlId = `field-${fields}`;
    this.labelId = `field-${fields}-label`;
  }

  labelsAComponent(): void {
    this.labelsANativeControl.set(false);
  }
}
