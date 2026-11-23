import { Component, computed, inject, input } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { MatAnchor } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { problemOf } from '@core/api/api-problem';
import { SecurityService } from '@core/security/security.service';
import { AddToBasketButton } from '@features/basket/add-to-basket-button/add-to-basket-button';
import { TranslatePipe } from '@ngx-translate/core';
import { PricePipe } from '@shared/pipes/price-pipe';
import { ErrorState } from '@shared/ui/error-state/error-state';
import { LoadingState } from '@shared/ui/loading-state/loading-state';
import { PageHeader } from '@shared/ui/page-header/page-header';
import { StockUrgencyBadge } from '@shared/ui/stock-urgency-badge/stock-urgency-badge';
import { CatalogService } from '../catalog.service';

@Component({
  imports: [
    AddToBasketButton,
    ErrorState,
    LoadingState,
    MatAnchor,
    MatIcon,
    PageHeader,
    PricePipe,
    RouterLink,
    StockUrgencyBadge,
    TranslatePipe,
  ],
  selector: 'app-catalog-item-page',
  templateUrl: './catalog-item-page.html',
})
export class CatalogItemPage {
  readonly id = input.required<string>();
  readonly back = input<string>();

  protected readonly backLink = computed(() => this.back() ?? '/catalog');
  protected readonly backLabel = computed(() => (this.back() ? 'common.back' : 'catalog.back'));

  private readonly catalog = inject(CatalogService);
  protected readonly security = inject(SecurityService);

  protected readonly item = rxResource({
    params: () => this.id(),
    stream: ({ params }) => this.catalog.getItem(params),
  });

  protected readonly problem = computed(() => problemOf(this.item.error()));
}
