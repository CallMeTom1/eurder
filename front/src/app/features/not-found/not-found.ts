import { Component } from '@angular/core';
import { MatAnchor } from '@angular/material/button';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { EmptyState } from '@shared/ui/empty-state/empty-state';
import { PageHeader } from '@shared/ui/page-header/page-header';

@Component({
  selector: 'app-not-found',
  imports: [EmptyState, MatAnchor, PageHeader, RouterLink, TranslatePipe],
  template: `
    <app-page-header [title]="'page.notFound' | translate" />
    <app-empty-state icon="explore_off" [message]="'error.common.route-not-found' | translate">
      <a mat-stroked-button routerLink="/catalog" actions>{{ 'nav.shop' | translate }}</a>
    </app-empty-state>
  `,
})
export class NotFound {}
