import { Component, inject, signal } from '@angular/core';
import { form, FormRoot, TreeValidationResult } from '@angular/forms/signals';
import { MatAnchor, MatButton } from '@angular/material/button';
import { Router, RouterLink } from '@angular/router';
import { isGlobalProblem } from '@core/api/api-error.interceptor';
import { ApiProblem, isApiProblem } from '@core/api/api-problem';
import { TranslatePipe } from '@ngx-translate/core';
import { apiErrorsOf } from '@shared/forms/api-errors';
import { ErrorState } from '@shared/ui/error-state/error-state';
import { PageHeader } from '@shared/ui/page-header/page-header';
import { firstValueFrom } from 'rxjs';
import { AdminItemService } from '../admin-item.service';
import { ItemForm, itemRules } from '../item-form';
import { ItemFormFields } from '../item-form-fields/item-form-fields';

@Component({
  imports: [
    ErrorState,
    FormRoot,
    ItemFormFields,
    MatAnchor,
    MatButton,
    PageHeader,
    RouterLink,
    TranslatePipe,
  ],
  selector: 'app-item-create-page',
  templateUrl: './item-create-page.html',
})
export class ItemCreatePage {
  private readonly adminItems = inject(AdminItemService);
  private readonly router = inject(Router);

  protected readonly item = signal<ItemForm>({
    name: '',
    description: '',
    price: null,
    stock: null,
  });
  protected readonly problem = signal<ApiProblem | null>(null);
  protected readonly itemForm = form(this.item, (path) => itemRules(path, () => 0), {
    submission: { action: () => this.create() },
  });

  private async create(): Promise<TreeValidationResult> {
    this.problem.set(null);
    const { name, description, price, stock } = this.item();
    try {
      const id = await firstValueFrom(
        this.adminItems.addItem({ name, description, price: price!, stock: stock! }),
      );
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
