import { Component, computed, inject, input, linkedSignal, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { form, FormRoot, TreeValidationResult } from '@angular/forms/signals';
import { MatAnchor, MatButton } from '@angular/material/button';
import { Router, RouterLink } from '@angular/router';
import { isGlobalProblem } from '@core/api/api-error.interceptor';
import { ApiProblem, isApiProblem, problemOf } from '@core/api/api-problem';
import { CatalogService } from '@features/catalog/catalog.service';
import { TranslatePipe } from '@ngx-translate/core';
import { apiErrorsOf } from '@shared/forms/api-errors';
import { ErrorState } from '@shared/ui/error-state/error-state';
import { LoadingState } from '@shared/ui/loading-state/loading-state';
import { PageHeader } from '@shared/ui/page-header/page-header';
import { firstValueFrom } from 'rxjs';
import { AdminItemService } from '../admin-item.service';
import { ItemForm, itemRules } from '../item-form';
import { ItemFormFields } from '../item-form-fields/item-form-fields';

const CENTS_PER_EURO = 100;

@Component({
  imports: [
    ErrorState,
    FormRoot,
    ItemFormFields,
    LoadingState,
    MatAnchor,
    MatButton,
    PageHeader,
    RouterLink,
    TranslatePipe,
  ],
  selector: 'app-item-edit-page',
  templateUrl: './item-edit-page.html',
})
export class ItemEditPage {
  readonly id = input.required<string>();

  private readonly catalog = inject(CatalogService);
  private readonly adminItems = inject(AdminItemService);
  private readonly router = inject(Router);

  protected readonly loaded = rxResource({
    params: () => this.id(),
    stream: ({ params }) => this.catalog.getItem(params),
  });
  protected readonly loadProblem = computed(() => problemOf(this.loaded.error()));
  private readonly currentStock = computed(() =>
    this.loaded.hasValue() ? this.loaded.value().stock : 0,
  );

  protected readonly item = linkedSignal<ItemForm>(() => {
    const item = this.loaded.hasValue() ? this.loaded.value() : undefined;
    return {
      name: item?.name ?? '',
      description: item?.description ?? '',
      price: item ? item.priceInCents / CENTS_PER_EURO : null,
      stock: item?.stock ?? null,
    };
  });
  protected readonly problem = signal<ApiProblem | null>(null);
  protected readonly itemForm = form(this.item, (path) => itemRules(path, this.currentStock), {
    submission: { action: () => this.update() },
  });

  private async update(): Promise<TreeValidationResult> {
    this.problem.set(null);
    const id = this.id();
    const { name, description, price, stock } = this.item();
    const added = stock! - this.currentStock();
    try {
      await firstValueFrom(this.adminItems.updateItem(id, { name, description, price: price! }));
      if (added > 0) {
        await firstValueFrom(this.adminItems.addUnits(id, added));
      }
      await this.router.navigate(['/admin/items', id]);
      return undefined;
    } catch (error) {
      return this.errorsOf(error);
    }
  }

  private errorsOf(error: unknown): TreeValidationResult {
    if (!isApiProblem(error) || isGlobalProblem(error)) {
      return undefined;
    }
    const { onFields, unmatched } = apiErrorsOf(this.itemForm, error);
    if (error.errors.length === 0 || unmatched.length > 0) {
      this.problem.set(error);
    }
    return onFields;
  }
}
