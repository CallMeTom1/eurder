import { Component, input, output } from '@angular/core';
import { MatButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-error-state',
  imports: [MatButton, MatIcon, TranslatePipe],
  template: `
    <div
      role="alert"
      class="flex flex-col items-start gap-3 rounded-md bg-error-container p-4 text-on-error-container"
    >
      <p class="flex items-center gap-2">
        <mat-icon aria-hidden="true">error</mat-icon>
        {{ message() }}
      </p>
      @if (retryable()) {
        <button mat-button (click)="retry.emit()">{{ 'common.retry' | translate }}</button>
      }
    </div>
  `,
})
export class ErrorState {
  readonly message = input.required<string>();
  readonly retryable = input(false);
  readonly retry = output();
}
