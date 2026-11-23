import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { PricePipe } from '@shared/pipes/price-pipe';
import { ImagePlaceholder } from '@shared/ui/image-placeholder/image-placeholder';
import {
  StockUrgency,
  StockUrgencyBadge,
} from '@shared/ui/stock-urgency-badge/stock-urgency-badge';

@Component({
  imports: [ImagePlaceholder, PricePipe, RouterLink, StockUrgencyBadge, TranslatePipe],
  selector: 'app-item-card',
  template: `
    <a [routerLink]="link()" class="flex flex-col gap-2 rounded-md">
      <h2 class="text-center [font:var(--mat-sys-title-medium)]">{{ name() }}</h2>
      <app-image-placeholder data-testid="item-image" />
    </a>
    <div class="flex items-center justify-between gap-2 text-sm">
      <span>{{ 'catalog.price' | translate: { price: (priceInCents() | price) } }}</span>
      <app-stock-urgency-badge [urgency]="urgency()" />
    </div>
  `,
  host: { class: 'flex flex-col gap-2' },
})
export class ItemCard {
  readonly name = input.required<string>();
  readonly priceInCents = input.required<number>();
  readonly urgency = input.required<StockUrgency>();
  readonly link = input.required<string | readonly unknown[]>();
}
