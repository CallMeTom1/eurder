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
import { PricePipe } from '@shared/pipes/price-pipe';
import { startingWith } from '@shared/filter/starting-with';
import { EmptyState } from '@shared/ui/empty-state/empty-state';
import { ErrorState } from '@shared/ui/error-state/error-state';
import { LoadingState } from '@shared/ui/loading-state/loading-state';
import { pageOf, Pagination } from '@shared/ui/pagination/pagination';
import { PageHeader } from '@shared/ui/page-header/page-header';
import { StockUrgencyBadge } from '@shared/ui/stock-urgency-badge/stock-urgency-badge';
import { AdminItemService } from '../admin-item.service';

const PAGE_SIZE = 10;

@Component({
  imports: [
    EmptyState,
    ErrorState,
    FormsModule,
    LoadingState,
    MatAnchor,
    MatIconAnchor,
    MatFormField,
    MatIcon,
    MatInput,
    MatLabel,
    MatSuffix,
    PageHeader,
    Pagination,
    PricePipe,
    RouterLink,
    StockUrgencyBadge,
    TranslatePipe,
  ],
  selector: 'app-admin-items-page',
  templateUrl: './admin-items-page.html',
})
export class AdminItemsPage {
  private readonly adminItems = inject(AdminItemService);

  protected readonly items = rxResource({
    stream: () => this.adminItems.getItems(),
    defaultValue: [],
  });

  protected readonly query = signal('');

  protected readonly matchingItems = computed(() =>
    startingWith(
      this.items.hasValue() ? this.items.value() : [],
      this.query(),
      (item) => item.name,
    ),
  );
  protected readonly pageIndex = linkedSignal(() => {
    this.query();
    return 0;
  });
  protected readonly pageSize = signal(PAGE_SIZE);
  protected readonly visibleItems = computed(() =>
    pageOf(this.matchingItems(), this.pageIndex(), this.pageSize()),
  );

  protected readonly problem = computed(() => problemOf(this.items.error()));
}
