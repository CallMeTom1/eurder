import { Component, inject, input } from '@angular/core';
import { MatButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';
import { SecurityService } from '@core/security/security.service';
import { CatalogItem } from '@features/catalog/catalog-item';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { firstValueFrom } from 'rxjs';
import { BasketService } from '../basket.service';

const MESSAGE_DURATION = 4000;

@Component({
  imports: [MatButton, MatIcon, TranslatePipe],
  selector: 'app-add-to-basket-button',
  templateUrl: './add-to-basket-button.html',
  host: { class: 'contents' },
})
export class AddToBasketButton {
  private readonly basket = inject(BasketService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly translate = inject(TranslateService);
  private readonly router = inject(Router);
  protected readonly security = inject(SecurityService);

  readonly item = input.required<CatalogItem>();

  protected async add(): Promise<void> {
    const item = this.item();
    this.basket.add(item);
    const message = this.snackBar.open(
      this.translate.instant('basket.added', { name: item.name }),
      this.translate.instant('basket.view'),
      { duration: MESSAGE_DURATION },
    );
    const { dismissedByAction } = await firstValueFrom(message.afterDismissed());
    if (dismissedByAction) {
      await this.router.navigateByUrl('/basket');
    }
  }
}
