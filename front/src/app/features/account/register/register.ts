import { Component, inject, signal } from '@angular/core';
import { form, FormRoot, TreeValidationResult } from '@angular/forms/signals';
import { MatButton } from '@angular/material/button';
import { Router, RouterLink } from '@angular/router';
import { ApiProblem, isApiProblem } from '@core/api/api-problem';
import type { CreateCustomerDto } from '@core/api/eurder-api';
import { TranslatePipe } from '@ngx-translate/core';
import { apiErrorOn, submissionErrorsOf } from '@shared/forms/api-errors';
import { ErrorState } from '@shared/ui/error-state/error-state';
import { PageHeader } from '@shared/ui/page-header/page-header';
import { firstValueFrom } from 'rxjs';
import { AccountService } from '../account.service';
import { customerRules, emptyCustomer, passwordRules } from '../customer-form/customer-form';
import { CustomerFormFields } from '../customer-form/customer-form-fields';

@Component({
  imports: [
    CustomerFormFields,
    ErrorState,
    FormRoot,
    MatButton,
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

  protected readonly registration = signal<CreateCustomerDto>({ ...emptyCustomer(), password: '' });
  protected readonly problem = signal<ApiProblem | null>(null);
  protected readonly registerForm = form(
    this.registration,
    (path) => {
      customerRules(path);
      passwordRules(path.password);
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
    if (isApiProblem(error) && error.code === 'error.security.email-already-used') {
      return apiErrorOn(this.registerForm.email, error);
    }
    return submissionErrorsOf(this.registerForm, error, this.problem);
  }
}
