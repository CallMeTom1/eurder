import { Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { EmptyState } from '@shared/ui/empty-state/empty-state';
import { PageHeader } from '@shared/ui/page-header/page-header';

@Component({
  imports: [EmptyState, PageHeader, TranslatePipe],
  selector: 'app-coming-soon',
  template: `
    <app-page-header [title]="title() ?? '' | translate" />
    <app-empty-state icon="construction" [message]="'page.comingSoon' | translate" />
  `,
})
export class ComingSoon {
  protected readonly title = toSignal(inject(ActivatedRoute).title);
}
