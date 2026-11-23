import { Component, computed, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { MatCard } from '@angular/material/card';
import { MatFormField, MatLabel } from '@angular/material/form-field';
import { MatIcon } from '@angular/material/icon';
import { MatInput } from '@angular/material/input';
import { RouterLink } from '@angular/router';
import { problemOf } from '@core/api/api-problem';
import { TranslatePipe } from '@ngx-translate/core';
import { PricePipe } from '@shared/pipes/price-pipe';
import { EmptyState } from '@shared/ui/empty-state/empty-state';
import { ErrorState } from '@shared/ui/error-state/error-state';
import { LoadingState } from '@shared/ui/loading-state/loading-state';
import { PageHeader } from '@shared/ui/page-header/page-header';
import { CatalogService } from '../catalog.service';

@Component({
  imports: [
    EmptyState,
    ErrorState,
    LoadingState,
    MatCard,
    MatFormField,
    MatIcon,
    MatInput,
    MatLabel,
    PageHeader,
    PricePipe,
    RouterLink,
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

  protected readonly visibleItems = computed(() => {
    const start = this.query().trim().toLocaleLowerCase();
    const items = this.items.hasValue() ? this.items.value() : [];
    return items.filter((item) => item.name.toLocaleLowerCase().startsWith(start));
  });

  protected readonly problem = computed(() => problemOf(this.items.error()));

  protected search(event: Event): void {
    this.query.set((event.target as HTMLInputElement).value);
  }
}
