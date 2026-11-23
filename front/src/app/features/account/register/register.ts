import { Component, inject, signal } from '@angular/core';
import {
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { MatError, MatFormField, MatHint, MatLabel } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { MatOption, MatSelect } from '@angular/material/select';
import { Router, RouterLink } from '@angular/router';
import { isGlobalProblem } from '@core/api/api-error.interceptor';
import { ApiProblem, isApiProblem } from '@core/api/api-problem';
import { addressDtoCountryValues, type AddressDto } from '@core/api/eurder-api';
import { TranslatePipe } from '@ngx-translate/core';
import { applyApiErrors } from '@shared/forms/api-errors';
import { FieldErrorPipe } from '@shared/forms/field-error-pipe';
import { ErrorState } from '@shared/ui/error-state/error-state';
import { PageHeader } from '@shared/ui/page-header/page-header';
import { finalize } from 'rxjs';
import { AccountService } from '../account.service';

type Country = AddressDto['country'];

const PHONE_NUMBER = /^\+?[0-9 ./-]{6,20}$/;

function requiredText(maxLength: number, ...validators: ValidatorFn[]): FormControl<string> {
  return new FormControl('', {
    nonNullable: true,
    validators: [Validators.required, Validators.maxLength(maxLength), ...validators],
  });
}

@Component({
  imports: [
    ErrorState,
    FieldErrorPipe,
    MatButton,
    MatError,
    MatFormField,
    MatHint,
    MatInput,
    MatLabel,
    MatOption,
    MatSelect,
    PageHeader,
    ReactiveFormsModule,
    RouterLink,
    TranslatePipe,
  ],
  selector: 'app-register',
  templateUrl: './register.html',
})
export class Register {
  private readonly accounts = inject(AccountService);
  private readonly router = inject(Router);

  protected readonly countries = addressDtoCountryValues;
  protected readonly form = new FormGroup({
    firstName: requiredText(255),
    lastName: requiredText(255),
    email: requiredText(255, Validators.email),
    password: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(8), Validators.maxLength(72)],
    }),
    phoneNumber: requiredText(20, Validators.pattern(PHONE_NUMBER)),
    address: new FormGroup({
      street: requiredText(255),
      houseNumber: requiredText(20),
      postalCode: requiredText(20),
      city: requiredText(255),
      country: new FormControl<Country>('BELGIUM', {
        nonNullable: true,
        validators: Validators.required,
      }),
    }),
  });
  protected readonly submitting = signal(false);
  protected readonly problem = signal<ApiProblem | null>(null);

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.problem.set(null);
    this.accounts
      .addCustomer(this.form.getRawValue())
      .pipe(finalize(() => this.submitting.set(false)))
      .subscribe({
        next: () => this.router.navigate(['/login'], { queryParams: { registered: true } }),
        error: (error: unknown) => this.show(error),
      });
  }

  private show(error: unknown): void {
    if (!isApiProblem(error) || isGlobalProblem(error)) {
      return;
    }
    if (error.code === 'error.security.email-already-used') {
      const email = this.form.controls.email;
      email.setErrors({ api: { code: error.code, params: error.params } });
      email.markAsTouched();
      return;
    }
    const unmatched = applyApiErrors(this.form, error);
    if (error.errors.length === 0 || unmatched.length > 0) {
      this.problem.set(error);
    }
  }
}
