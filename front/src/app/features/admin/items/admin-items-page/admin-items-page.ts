import { Component, computed, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { MatAnchor } from '@angular/material/button';
import { MatFormField, MatSuffix } from '@angular/material/form-field';
import { MatIcon } from '@angular/material/icon';
import { MatInput } from '@angular/material/input';
import { RouterLink } from '@angular/router';
import { problemOf } from '@core/api/api-problem';
import { TranslatePipe } from '@ngx-translate/core';
import { startingWith } from '@shared/filter/starting-with';
import { EmptyState } from '@shared/ui/empty-state/empty-state';
import { ErrorState } from '@shared/ui/error-state/error-state';
import { ItemCard } from '@shared/ui/item-card/item-card';
import { LoadingState } from '@shared/ui/loading-state/loading-state';
import { PageHeader } from '@shared/ui/page-header/page-header';
import { AdminItemService } from '../admin-item.service';

@Component({
  imports: [
    EmptyState,
    ErrorState,
    FormsModule,
    ItemCard,
    LoadingState,
    MatAnchor,
    MatFormField,
    MatIcon,
    MatInput,
    MatSuffix,
    PageHeader,
    RouterLink,
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

  protected readonly visibleItems = computed(() =>
    startingWith(
      this.items.hasValue() ? this.items.value() : [],
      this.query(),
      (item) => item.name,
    ),
  );

  protected readonly problem = computed(() => problemOf(this.items.error()));
}
