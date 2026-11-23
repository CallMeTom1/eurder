import { Component } from '@angular/core';
import { MatProgressSpinner } from '@angular/material/progress-spinner';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-loading-state',
  imports: [MatProgressSpinner, TranslatePipe],
  template: `
    <div role="status" class="flex items-center gap-3 py-8 text-on-surface-variant">
      <mat-progress-spinner mode="indeterminate" diameter="24" aria-hidden="true" />
      {{ 'common.loading' | translate }}
    </div>
  `,
})
export class LoadingState {}
