import { Component, input } from '@angular/core';
import { MatCard } from '@angular/material/card';
import { MatIcon } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { PricePipe } from '@shared/pipes/price-pipe';

@Component({
  imports: [MatCard, MatIcon, PricePipe, RouterLink],
  selector: 'app-item-card',
  template: `
    <mat-card appearance="outlined" class="h-full">
      <a [routerLink]="link()" class="flex flex-1 flex-col gap-3 p-4 pb-3">
        @if (imageUrl(); as url) {
          <img
            data-testid="item-image"
            [src]="url"
            alt=""
            loading="lazy"
            class="aspect-[4/3] w-full rounded-md bg-surface-container object-cover"
          />
        } @else {
          <span
            aria-hidden="true"
            data-testid="item-image"
            class="flex aspect-[4/3] items-center justify-center rounded-md bg-surface-container text-on-surface-variant"
          >
            <mat-icon>image</mat-icon>
          </span>
        }
        <h2 class="[font:var(--mat-sys-title-medium)]">{{ name() }}</h2>
        <span class="text-on-surface-variant">{{ priceInCents() | price }}</span>
      </a>
      <div class="px-4 pb-4"><ng-content /></div>
    </mat-card>
  `,
  host: { class: 'block h-full' },
})
export class ItemCard {
  readonly name = input.required<string>();
  readonly priceInCents = input.required<number>();
  readonly link = input.required<string | readonly unknown[]>();
  readonly imageUrl = input<string | null>(null);
}
