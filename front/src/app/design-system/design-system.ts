import { Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { PricePipe } from '@shared/pipes/price-pipe';
import { ConfirmationService } from '@shared/ui/confirm-dialog/confirmation.service';
import { EmptyState } from '@shared/ui/empty-state/empty-state';
import { ErrorState } from '@shared/ui/error-state/error-state';
import { LoadingState } from '@shared/ui/loading-state/loading-state';
import { Logo } from '@shared/ui/logo/logo';
import { PageHeader } from '@shared/ui/page-header/page-header';
import { LanguageSwitcher } from '@shared/ui/language-switcher/language-switcher';
import { ThemeToggle } from '@shared/ui/theme-toggle/theme-toggle';

interface Swatch {
  name: string;
  colors: string;
}

@Component({
  selector: 'app-design-system',
  imports: [
    MatButtonModule,
    EmptyState,
    ErrorState,
    LanguageSwitcher,
    LoadingState,
    Logo,
    PageHeader,
    PricePipe,
    ThemeToggle,
  ],
  templateUrl: './design-system.html',
})
export class DesignSystem {
  private readonly confirmation = inject(ConfirmationService);

  protected readonly answer = signal('');

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
