import { Component, computed, input } from '@angular/core';
import type { ItemOverviewDto } from '@core/api/eurder-api';
import { TranslatePipe } from '@ngx-translate/core';

export type StockUrgency = ItemOverviewDto['urgency'];

type Tone = 'success' | 'warning' | 'error';

const TONES: Record<StockUrgency, Tone> = {
  STOCK_HIGH: 'success',
  STOCK_MEDIUM: 'warning',
  STOCK_LOW: 'error',
};

const COLORS: Record<Tone, string> = {
  success: 'bg-success-container text-on-success-container',
  warning: 'bg-warning-container text-on-warning-container',
  error: 'bg-error-container text-on-error-container',
};

@Component({
  imports: [TranslatePipe],
  selector: 'app-stock-urgency-badge',
  template: `
    <span
      class="inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-sm font-medium"
      [class]="colors()"
      [attr.data-tone]="tone()"
    >
      <span aria-hidden="true" class="size-2 rounded-full bg-current"></span>
      {{ 'stock.urgency.' + urgency() | translate }}
    </span>
  `,
})
export class StockUrgencyBadge {
  readonly urgency = input.required<StockUrgency>();

  protected readonly tone = computed(() => TONES[this.urgency()]);
  protected readonly colors = computed(() => COLORS[this.tone()]);
}
