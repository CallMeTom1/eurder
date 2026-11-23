import { Component, input } from '@angular/core';
import { MatIcon } from '@angular/material/icon';

@Component({
  imports: [MatIcon],
  selector: 'app-empty-state',
  template: `
    <div class="flex flex-col items-center gap-3 py-12 text-center text-on-surface-variant">
      <mat-icon aria-hidden="true">{{ icon() }}</mat-icon>
      <p>{{ message() }}</p>
      <ng-content select="[actions]" />
    </div>
  `,
})
export class EmptyState {
  readonly message = input.required<string>();
  readonly icon = input('inventory_2');
}
