import { Component, computed, inject, input } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { MatAnchor } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { problemOf } from '@core/api/api-problem';
import { EurderReport } from '@features/eurder/eurder-report/eurder-report';
import { TranslatePipe } from '@ngx-translate/core';
import { ErrorState } from '@shared/ui/error-state/error-state';
import { LoadingState } from '@shared/ui/loading-state/loading-state';
import { PageHeader } from '@shared/ui/page-header/page-header';
import { AdminCustomerService } from '../admin-customer.service';

@Component({
  imports: [
    ErrorState,
    EurderReport,
    LoadingState,
    MatAnchor,
    MatIcon,
    PageHeader,
    RouterLink,
    TranslatePipe,
  ],
  selector: 'app-customer-detail-page',
  templateUrl: './customer-detail-page.html',
})
export class CustomerDetailPage {
  readonly id = input.required<string>();

  private readonly adminCustomers = inject(AdminCustomerService);

  protected readonly customer = rxResource({
    params: () => this.id(),
    stream: ({ params }) => this.adminCustomers.getCustomer(params),
  });
  protected readonly report = rxResource({
    params: () => this.id(),
    stream: ({ params }) => this.adminCustomers.getReport(params),
  });

  protected readonly problem = computed(() => problemOf(this.customer.error()));
  protected readonly reportProblem = computed(() => problemOf(this.report.error()));
}
