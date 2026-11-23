import { Component, inject, signal } from '@angular/core';
import {
  email,
  form,
  FormField,
  FormRoot,
  maxLength,
  minLength,
  pattern,
  required,
  SchemaPath,
  TreeValidationResult,
} from '@angular/forms/signals';
import { MatButton } from '@angular/material/button';
import { MatError, MatFormField, MatHint, MatLabel } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { MatOption, MatSelect } from '@angular/material/select';
import { Router, RouterLink } from '@angular/router';
import { isGlobalProblem } from '@core/api/api-error.interceptor';
import { ApiProblem, isApiProblem } from '@core/api/api-problem';
import { addressDtoCountryValues, type CreateCustomerDto } from '@core/api/eurder-api';
import { TranslatePipe } from '@ngx-translate/core';
import { apiErrorOn, apiErrorsOf } from '@shared/forms/api-errors';
import { FieldErrorPipe } from '@shared/forms/field-error-pipe';
import { ErrorState } from '@shared/ui/error-state/error-state';
import { PageHeader } from '@shared/ui/page-header/page-header';
import { firstValueFrom } from 'rxjs';
import { AccountService } from '../account.service';

const PHONE_NUMBER = /^\+?[0-9 ./-]{6,20}$/;

function requiredText(path: SchemaPath<string>, length: number): void {
  required(path);
  maxLength(path, length);
}

@Component({
  imports: [
    ErrorState,
    FieldErrorPipe,
    FormField,
    FormRoot,
    MatButton,
    MatError,
    MatFormField,
    MatHint,
    MatInput,
    MatLabel,
    MatOption,
    MatSelect,
    PageHeader,
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
  protected readonly registration = signal<CreateCustomerDto>({
    firstName: '',
    lastName: '',
    email: '',
    password: '',
    phoneNumber: '',
    address: { street: '', houseNumber: '', postalCode: '', city: '', country: 'BELGIUM' },
  });
  protected readonly problem = signal<ApiProblem | null>(null);
  protected readonly registerForm = form(
    this.registration,
    (path) => {
      requiredText(path.firstName, 255);
      requiredText(path.lastName, 255);
      requiredText(path.email, 255);
      email(path.email);
      required(path.password);
      minLength(path.password, 8);
      maxLength(path.password, 72);
      required(path.phoneNumber);
      pattern(path.phoneNumber, PHONE_NUMBER);
      requiredText(path.address.street, 255);
      requiredText(path.address.houseNumber, 20);
      requiredText(path.address.postalCode, 20);
      requiredText(path.address.city, 255);
      required(path.address.country);
    },
    { submission: { action: () => this.register() } },
  );

  private async register(): Promise<TreeValidationResult> {
    this.problem.set(null);
    try {
      await firstValueFrom(this.accounts.addCustomer(this.registration()));
      await this.router.navigate(['/login'], { queryParams: { registered: true } });
      return undefined;
    } catch (error) {
      return this.errorsOf(error);
    }
  }

  private errorsOf(error: unknown): TreeValidationResult {
    if (!isApiProblem(error) || isGlobalProblem(error)) {
      return undefined;
    }
    if (error.code === 'error.security.email-already-used') {
      return apiErrorOn(this.registerForm.email, error);
    }
    const { onFields, unmatched } = apiErrorsOf(this.registerForm, error);
    if (error.errors.length === 0 || unmatched.length > 0) {
      this.problem.set(error);
    }
    return onFields;
  }
}
