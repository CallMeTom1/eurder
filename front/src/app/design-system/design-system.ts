import { Component, inject, signal } from '@angular/core';
import { form, FormField, required, submit } from '@angular/forms/signals';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { TranslatePipe } from '@ngx-translate/core';
import { uniquePrefix } from '@shared/forms/field-ids';
import { apiErrorsOf } from '@shared/forms/api-errors';
import { atMost, digits, positive } from '@shared/forms/eurder-validators';
import { FieldErrorPipe } from '@shared/forms/field-error-pipe';
import { PricePipe } from '@shared/pipes/price-pipe';
import { CharCounter } from '@shared/ui/char-counter/char-counter';
import { ConfirmationService } from '@shared/ui/confirm-dialog/confirmation.service';
import { EmptyState } from '@shared/ui/empty-state/empty-state';
import { ErrorState } from '@shared/ui/error-state/error-state';
import { LanguageSwitcher } from '@shared/ui/language-switcher/language-switcher';
import { LoadingState } from '@shared/ui/loading-state/loading-state';
import { Logo } from '@shared/ui/logo/logo';
import { PageHeader } from '@shared/ui/page-header/page-header';
import { StockUrgencyBadge } from '@shared/ui/stock-urgency-badge/stock-urgency-badge';
import { ThemeToggle } from '@shared/ui/theme-toggle/theme-toggle';
import { firstValueFrom } from 'rxjs';

interface Swatch {
  name: string;
  colors: string;
}

interface DemoItem {
  name: string;
  price: number | null;
  description: string;
}

@Component({
  imports: [
    CharCounter,
    EmptyState,
    ErrorState,
    FieldErrorPipe,
    FormField,
    LanguageSwitcher,
    LoadingState,
    Logo,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    PageHeader,
    PricePipe,
    StockUrgencyBadge,
    ThemeToggle,
    TranslatePipe,
  ],
  selector: 'app-design-system',
  templateUrl: './design-system.html',
})
export class DesignSystem {
  protected readonly prefix = uniquePrefix();

  private readonly confirmation = inject(ConfirmationService);

  protected readonly answer = signal('');
  protected readonly item = signal<DemoItem>({ name: '', price: null, description: '' });
  protected readonly itemForm = form(this.item, (path) => {
    required(path.name);
    required(path.price);
    positive(path.price);
    digits(path.price, 8, 2);
    atMost(path.description, 255);
  });

  protected readonly swatches: Swatch[] = [
    { name: 'primary', colors: 'bg-primary text-on-primary' },
    { name: 'primary-container', colors: 'bg-primary-container text-on-primary-container' },
    { name: 'secondary', colors: 'bg-secondary text-on-secondary' },
    { name: 'secondary-container', colors: 'bg-secondary-container text-on-secondary-container' },
    { name: 'tertiary', colors: 'bg-tertiary text-on-tertiary' },
    { name: 'error', colors: 'bg-error text-on-error' },
    { name: 'surface', colors: 'bg-surface text-on-surface border border-outline-variant' },
    { name: 'surface-container', colors: 'bg-surface-container text-on-surface' },
  ];

  protected async simulateApiErrors(): Promise<void> {
    this.item.set({ name: 'Chair', price: 49.99, description: '' });
    await submit(this.itemForm, async () => {
      const { onFields } = apiErrorsOf(this.itemForm, {
        status: 400,
        code: 'error.common.validation-failed',
        params: {},
        errors: [
          { field: 'name', code: 'validation.too-long', params: { max: 100 } },
          { field: 'price', code: 'validation.must-be-positive', params: {} },
        ],
      });
      return onFields;
    });
  }

  protected async askToDelete(): Promise<void> {
    const confirmed = await firstValueFrom(
      this.confirmation.confirm({
        title: 'Delete the chair?',
        message: 'It cannot be undone.',
        confirmLabel: 'Delete',
      }),
    );
    this.answer.set(confirmed ? 'Confirmed' : 'Cancelled');
  }
}
