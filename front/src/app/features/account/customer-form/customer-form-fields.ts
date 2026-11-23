import { Component, input } from '@angular/core';
import { FieldTree, FormField } from '@angular/forms/signals';
import { MatError, MatFormField, MatHint } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { MatOption, MatSelect } from '@angular/material/select';
import { addressDtoCountryValues } from '@core/api/eurder-api';
import { TranslatePipe } from '@ngx-translate/core';
import { FieldControl } from '@shared/forms/form-field/field-control';
import { FieldErrors } from '@shared/forms/form-field/field-errors';
import { FormFieldComponent } from '@shared/forms/form-field/form-field';
import { CustomerDetails } from './customer-form';

@Component({
  imports: [
    FieldControl,
    FieldErrors,
    FormFieldComponent,
    FormField,
    MatError,
    MatFormField,
    MatHint,
    MatInput,
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
