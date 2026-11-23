import { Component, input } from '@angular/core';
import { FieldTree, FormField } from '@angular/forms/signals';
import { MatError, MatFormField, MatHint, MatLabel, MatPrefix } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { TranslatePipe } from '@ngx-translate/core';
import { FieldErrorPipe } from '@shared/forms/field-error-pipe';
import { CharCounter } from '@shared/ui/char-counter/char-counter';
import { ItemForm } from '../item-form';

@Component({
  imports: [
    CharCounter,
    FieldErrorPipe,
    FormField,
    MatError,
    MatFormField,
    MatHint,
    MatInput,
    MatLabel,
    MatPrefix,
    TranslatePipe,
  ],
  selector: 'app-item-form-fields',
  templateUrl: './item-form-fields.html',
  host: { class: 'grid gap-x-4 gap-y-1 sm:grid-cols-[12rem_14rem_1fr]' },
})
export class ItemFormFields {
  readonly form = input.required<FieldTree<ItemForm>>();
}
