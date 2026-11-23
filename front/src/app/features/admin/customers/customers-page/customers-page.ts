import { Component, computed, inject, linkedSignal, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { MatAnchor, MatIconAnchor } from '@angular/material/button';
import { MatFormField, MatLabel, MatSuffix } from '@angular/material/form-field';
import { MatIcon } from '@angular/material/icon';
import { MatInput } from '@angular/material/input';
import { RouterLink } from '@angular/router';
import { problemOf } from '@core/api/api-problem';
import { TranslatePipe } from '@ngx-translate/core';
import { startingWith } from '@shared/filter/starting-with';
import { EmptyState } from '@shared/ui/empty-state/empty-state';
import { ErrorState } from '@shared/ui/error-state/error-state';
import { LoadingState } from '@shared/ui/loading-state/loading-state';
import { PageHeader } from '@shared/ui/page-header/page-header';
import { pageOf, Pagination } from '@shared/ui/pagination/pagination';
import { AdminCustomerService } from '../admin-customer.service';

const PAGE_SIZE = 10;

@Component({
  imports: [
    EmptyState,
    ErrorState,
    FormsModule,
    LoadingState,
    MatAnchor,
    MatFormField,
    MatIcon,
    MatIconAnchor,
    MatInput,
    MatLabel,
    MatSuffix,
    PageHeader,
    Pagination,
    RouterLink,
    TranslatePipe,
  ],
  selector: 'app-customers-page',
  templateUrl: './customers-page.html',
})
export class CustomersPage {
  private readonly adminCustomers = inject(AdminCustomerService);

  protected readonly customers = rxResource({
    stream: () => this.adminCustomers.getCustomers(),
    defaultValue: [],
  });

  protected readonly query = signal('');
  protected readonly matchingCustomers = computed(() =>
    startingWith(
      this.customers.hasValue() ? this.customers.value() : [],
      this.query(),
      (customer) => customer.lastName,
    ),
  );
  protected readonly pageIndex = linkedSignal(() => {
    this.query();
    return 0;
  });
  protected readonly pageSize = signal(PAGE_SIZE);
  protected readonly visibleCustomers = computed(() =>
    pageOf(this.matchingCustomers(), this.pageIndex(), this.pageSize()),
  );

  protected readonly problem = computed(() => problemOf(this.customers.error()));
}
