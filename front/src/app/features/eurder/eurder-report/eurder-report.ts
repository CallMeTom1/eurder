import { Component, input, output } from '@angular/core';
import { MatButton } from '@angular/material/button';
import { MatCard } from '@angular/material/card';
import { MatIcon } from '@angular/material/icon';
import { TranslatePipe } from '@ngx-translate/core';
import { PricePipe } from '@shared/pipes/price-pipe';
import { EmptyState } from '@shared/ui/empty-state/empty-state';
import { Report } from '../report';

@Component({
  imports: [EmptyState, MatButton, MatCard, MatIcon, PricePipe, TranslatePipe],
  selector: 'app-eurder-report',
  templateUrl: './eurder-report.html',
  host: { class: 'flex flex-col gap-4' },
})
export class EurderReport {
  readonly report = input.required<Report>();
  readonly canReorder = input(false);
  readonly reorder = output<string>();
}
