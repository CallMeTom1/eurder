import { Component, inject, signal } from '@angular/core';
import { form, FormRoot, TreeValidationResult } from '@angular/forms/signals';
import { MatAnchor, MatButton } from '@angular/material/button';
import { Router, RouterLink } from '@angular/router';
import { ApiProblem, isApiProblem } from '@core/api/api-problem';
import type { CreateCustomerDto } from '@core/api/eurder-api';
import { AccountService } from '@features/account/account.service';
import {
  customerRules,
  emptyCustomer,
  passwordRules,
} from '@features/account/customer-form/customer-form';
import { CustomerFormFields } from '@features/account/customer-form/customer-form-fields';
import { TranslatePipe } from '@ngx-translate/core';
import { apiErrorOn, submissionErrorsOf } from '@shared/forms/api-errors';
import { ErrorState } from '@shared/ui/error-state/error-state';
import { PageHeader } from '@shared/ui/page-header/page-header';
import { firstValueFrom } from 'rxjs';

@Component({
  imports: [
    CustomerFormFields,
    ErrorState,
    FormRoot,
    MatAnchor,
    MatButton,
    PageHeader,
    RouterLink,
    TranslatePipe,
  ],
  selector: 'app-customer-create-page',
  templateUrl: './customer-create-page.html',
})
export class CustomerCreatePage {
  private readonly accounts = inject(AccountService);
  private readonly router = inject(Router);

  protected readonly customer = signal<CreateCustomerDto>({ ...emptyCustomer(), password: '' });
  protected readonly problem = signal<ApiProblem | null>(null);
  protected readonly customerForm = form(
    this.customer,
    (path) => {
      customerRules(path);
      passwordRules(path.password);
    },
    { submission: { action: () => this.create() } },
  );

  private async create(): Promise<TreeValidationResult> {
    this.problem.set(null);
    try {
      const created = await firstValueFrom(this.accounts.addCustomer(this.customer()));
      await this.router.navigate(['/admin/customers', created.id]);
      return undefined;
    } catch (error) {
      if (isApiProblem(error) && error.code === 'error.security.email-already-used') {
        return apiErrorOn(this.customerForm.email, error);
      }
      return submissionErrorsOf(this.customerForm, error, this.problem);
    }
  }
}
