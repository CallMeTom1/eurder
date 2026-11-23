import { Component, computed, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { MatFormField, MatSuffix } from '@angular/material/form-field';
import { MatIcon } from '@angular/material/icon';
import { MatInput } from '@angular/material/input';
import { problemOf } from '@core/api/api-problem';
import { AddToBasketButton } from '@features/basket/add-to-basket-button/add-to-basket-button';
import { TranslatePipe } from '@ngx-translate/core';
import { EmptyState } from '@shared/ui/empty-state/empty-state';
import { ErrorState } from '@shared/ui/error-state/error-state';
import { LoadingState } from '@shared/ui/loading-state/loading-state';
import { ItemCard } from '@shared/ui/item-card/item-card';
import { startingWith } from '@shared/filter/starting-with';
import { PageHeader } from '@shared/ui/page-header/page-header';
import { CatalogService } from '../catalog.service';

@Component({
  imports: [
    AddToBasketButton,
    EmptyState,
    ErrorState,
    FormsModule,
    ItemCard,
    LoadingState,
    MatFormField,
    MatIcon,
    MatInput,
    MatSuffix,
    PageHeader,
    TranslatePipe,
  ],
  selector: 'app-catalog-page',
  templateUrl: './catalog-page.html',
})
export class CatalogPage {
  private readonly catalog = inject(CatalogService);

  protected readonly items = rxResource({
    stream: () => this.catalog.getItems(),
    defaultValue: [],
  });

  protected readonly query = signal('');

  protected readonly visibleItems = computed(() =>
    startingWith(
      this.items.hasValue() ? this.items.value() : [],
      this.query(),
      (item) => item.name,
    ),
  );

  protected readonly problem = computed(() => problemOf(this.items.error()));
}
