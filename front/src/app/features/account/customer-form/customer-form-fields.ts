import { Component, input } from '@angular/core';
import { FieldTree, FormField } from '@angular/forms/signals';
import { MatError, MatFormField, MatHint, MatLabel } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { MatOption, MatSelect } from '@angular/material/select';
import { addressDtoCountryValues } from '@core/api/eurder-api';
import { TranslatePipe } from '@ngx-translate/core';
import { FieldErrorPipe } from '@shared/forms/field-error-pipe';
import { CustomerDetails } from './customer-form';

@Component({
  imports: [
    FieldErrorPipe,
    FormField,
    MatError,
    MatFormField,
    MatHint,
    MatInput,
    MatLabel,
    MatOption,
    MatSelect,
    TranslatePipe,
  ],
  selector: 'app-customer-form-fields',
  templateUrl: './customer-form-fields.html',
  host: { class: 'grid gap-x-4 gap-y-3 sm:grid-cols-2' },
})
export class CustomerFormFields {
  readonly form = input.required<FieldTree<CustomerDetails>>();
  readonly password = input<FieldTree<string>>();

  protected readonly countries = addressDtoCountryValues;
}
