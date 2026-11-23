import { Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { TranslatePipe } from '@ngx-translate/core';
import { CharCounter } from '@shared/directives/char-counter';
import { applyApiErrors } from '@shared/forms/api-errors';
import { EurderValidators } from '@shared/forms/eurder-validators';
import { FieldErrorPipe } from '@shared/forms/field-error-pipe';
import { PricePipe } from '@shared/pipes/price-pipe';
import { ConfirmationService } from '@shared/ui/confirm-dialog/confirmation.service';
import { EmptyState } from '@shared/ui/empty-state/empty-state';
import { ErrorState } from '@shared/ui/error-state/error-state';
import { LoadingState } from '@shared/ui/loading-state/loading-state';
import { Logo } from '@shared/ui/logo/logo';
import { PageHeader } from '@shared/ui/page-header/page-header';
import { LanguageSwitcher } from '@shared/ui/language-switcher/language-switcher';
import { StockUrgencyBadge } from '@shared/ui/stock-urgency-badge/stock-urgency-badge';
import { ThemeToggle } from '@shared/ui/theme-toggle/theme-toggle';

interface Swatch {
  name: string;
  colors: string;
}

@Component({
  selector: 'app-design-system',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    TranslatePipe,
    CharCounter,
    FieldErrorPipe,
    EmptyState,
    ErrorState,
    LanguageSwitcher,
    LoadingState,
    Logo,
    PageHeader,
    PricePipe,
    StockUrgencyBadge,
    ThemeToggle,
  ],
  templateUrl: './design-system.html',
})
export class DesignSystem {
  private readonly confirmation = inject(ConfirmationService);

  protected readonly answer = signal('');
  protected readonly name = new FormControl('', {
    nonNullable: true,
    validators: Validators.required,
  });
  protected readonly price = new FormControl<number | null>(null, [
    Validators.required,
    EurderValidators.positive,
    EurderValidators.digits(8, 2),
  ]);
  protected readonly description = new FormControl('', { nonNullable: true });

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

  protected simulateApiErrors(): void {
    applyApiErrors(new FormGroup({ name: this.name, price: this.price }), {
      status: 400,
      code: 'error.common.validation-failed',
      params: {},
      errors: [
        { field: 'name', code: 'validation.too-long', params: { max: 100 } },
        { field: 'price', code: 'validation.must-be-positive', params: {} },
      ],
    });
  }

  protected askToDelete(): void {
    this.confirmation
      .confirm({
        title: 'Delete the chair?',
        message: 'It cannot be undone.',
        confirmLabel: 'Delete',
      })
      .subscribe((confirmed) => this.answer.set(confirmed ? 'Confirmed' : 'Cancelled'));
  }
}
