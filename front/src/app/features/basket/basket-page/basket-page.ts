import { Component, inject } from '@angular/core';
import { MatButton, MatIconButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { PricePipe } from '@shared/pipes/price-pipe';
import { EmptyState } from '@shared/ui/empty-state/empty-state';
import { PageHeader } from '@shared/ui/page-header/page-header';
import { BasketLine, MAX_AMOUNT } from '../basket-line';
import { BasketService } from '../basket.service';

@Component({
  imports: [
    EmptyState,
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
  protected readonly basket = inject(BasketService);
  protected readonly maxAmount = MAX_AMOUNT;

  protected changeAmount(line: BasketLine, input: HTMLInputElement): void {
    this.basket.changeAmount(line.itemId, Number(input.value));
    input.value = String(this.basket.lines().find((kept) => kept.itemId === line.itemId)?.amount);
  }
}
