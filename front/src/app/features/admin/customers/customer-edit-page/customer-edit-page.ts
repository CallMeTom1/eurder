import { Component, computed, inject, input, linkedSignal, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { form, FormRoot, TreeValidationResult } from '@angular/forms/signals';
import { MatAnchor, MatButton } from '@angular/material/button';
import { Router, RouterLink } from '@angular/router';
import { ApiProblem, isApiProblem, problemOf } from '@core/api/api-problem';
import {
  CustomerDetails,
  customerRules,
  emptyCustomer,
} from '@features/account/customer-form/customer-form';
import { CustomerFormFields } from '@features/account/customer-form/customer-form-fields';
import { TranslatePipe } from '@ngx-translate/core';
import { apiErrorOn, submissionErrorsOf } from '@shared/forms/api-errors';
import { ErrorState } from '@shared/ui/error-state/error-state';
import { LoadingState } from '@shared/ui/loading-state/loading-state';
import { PageHeader } from '@shared/ui/page-header/page-header';
import { firstValueFrom } from 'rxjs';
import { AdminCustomerService } from '../admin-customer.service';

@Component({
  imports: [
    CustomerFormFields,
    ErrorState,
    FormRoot,
    LoadingState,
    MatAnchor,
    MatButton,
    PageHeader,
    RouterLink,
    TranslatePipe,
  ],
  selector: 'app-customer-edit-page',
  templateUrl: './customer-edit-page.html',
})
export class CustomerEditPage {
  readonly id = input.required<string>();

  private readonly adminCustomers = inject(AdminCustomerService);
  private readonly router = inject(Router);

  protected readonly loaded = rxResource({
    params: () => this.id(),
    stream: ({ params }) => this.adminCustomers.getCustomer(params),
  });
  protected readonly loadProblem = computed(() => problemOf(this.loaded.error()));

  protected readonly customer = linkedSignal<CustomerDetails>(() => {
    if (!this.loaded.hasValue()) {
      return emptyCustomer();
    }
    const { firstName, lastName, email, phoneNumber, address } = this.loaded.value();
    return { firstName, lastName, email, phoneNumber, address: { ...address } };
  });
  protected readonly problem = signal<ApiProblem | null>(null);
  protected readonly customerForm = form(this.customer, (path) => customerRules(path), {
    submission: { action: () => this.update() },
  });

  private async update(): Promise<TreeValidationResult> {
    this.problem.set(null);
    try {
      await firstValueFrom(this.adminCustomers.updateCustomer(this.id(), this.customer()));
      await this.router.navigate(['/admin/customers', this.id()]);
      return undefined;
    } catch (error) {
      if (isApiProblem(error) && error.code === 'error.security.email-already-used') {
        return apiErrorOn(this.customerForm.email, error);
      }
      return submissionErrorsOf(this.customerForm, error, this.problem);
    }
  }
}
