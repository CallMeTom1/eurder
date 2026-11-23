import { Component, inject, signal } from '@angular/core';
import { MatButton, MatIconButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { Router, RouterLink } from '@angular/router';
import { isGlobalProblem } from '@core/api/api-error.interceptor';
import { ApiProblem, isApiProblem } from '@core/api/api-problem';
import { SecurityService } from '@core/security/security.service';
import { EurderService } from '@features/eurder/eurder.service';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { PricePipe } from '@shared/pipes/price-pipe';
import { ConfirmationService } from '@shared/ui/confirm-dialog/confirmation.service';
import { EmptyState } from '@shared/ui/empty-state/empty-state';
import { ErrorState } from '@shared/ui/error-state/error-state';
import { PageHeader } from '@shared/ui/page-header/page-header';
import { firstValueFrom } from 'rxjs';
import { BasketLine, MAX_AMOUNT } from '../basket-line';
import { BasketService } from '../basket.service';

const UNAUTHORIZED = 401;

@Component({
  imports: [
    EmptyState,
    ErrorState,
    MatButton,
    MatIcon,
    MatIconButton,
    PageHeader,
    PricePipe,
    RouterLink,
    TranslatePipe,
  ],
  selector: 'app-basket-page',
  templateUrl: './basket-page.html',
})
export class BasketPage {
  private readonly security = inject(SecurityService);
  private readonly eurders = inject(EurderService);
  private readonly confirmation = inject(ConfirmationService);
  private readonly translate = inject(TranslateService);
  private readonly router = inject(Router);
  protected readonly basket = inject(BasketService);
  protected readonly maxAmount = MAX_AMOUNT;
  protected readonly placing = signal(false);
  protected readonly problem = signal<ApiProblem | null>(null);

  protected changeAmount(line: BasketLine, input: HTMLInputElement): void {
    this.basket.changeAmount(line.itemId, Number(input.value));
    input.value = String(this.basket.lines().find((kept) => kept.itemId === line.itemId)?.amount);
  }

  protected async placeOrder(): Promise<void> {
    if (!this.security.isAuthenticated()) {
      await this.logInFirst();
      return;
    }
    this.problem.set(null);
    this.placing.set(true);
    try {
      await firstValueFrom(this.eurders.addEurder(this.basket.lines()));
      this.basket.clear();
      await firstValueFrom(
        this.confirmation.inform({
          title: this.translate.instant('basket.placed.title'),
          message: this.translate.instant('basket.placed.message'),
        }),
      );
      await this.router.navigateByUrl('/catalog');
    } catch (error) {
      await this.refused(error);
    } finally {
      this.placing.set(false);
    }
  }

  private async refused(error: unknown): Promise<void> {
    if (!isApiProblem(error) || isGlobalProblem(error)) {
      return;
    }
    if (error.status === UNAUTHORIZED) {
      await this.logInFirst();
      return;
    }
    this.problem.set(error);
  }

  private async logInFirst(): Promise<void> {
    await this.router.navigate(['/login'], { queryParams: { returnUrl: '/basket' } });
  }
}
