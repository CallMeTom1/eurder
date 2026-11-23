import { SlicePipe } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { MatCard } from '@angular/material/card';
import { problemOf } from '@core/api/api-problem';
import { EurderService } from '@features/eurder/eurder.service';
import { TranslatePipe } from '@ngx-translate/core';
import { EmptyState } from '@shared/ui/empty-state/empty-state';
import { ErrorState } from '@shared/ui/error-state/error-state';
import { LoadingState } from '@shared/ui/loading-state/loading-state';
import { PageHeader } from '@shared/ui/page-header/page-header';

@Component({
  imports: [EmptyState, ErrorState, LoadingState, MatCard, PageHeader, SlicePipe, TranslatePipe],
  selector: 'app-shipping-page',
  templateUrl: './shipping-page.html',
})
export class ShippingPage {
  private readonly eurders = inject(EurderService);

  protected readonly shipments = rxResource({
    stream: () => this.eurders.getShippingToday(),
    defaultValue: [],
  });
  protected readonly problem = computed(() => problemOf(this.shipments.error()));
}
