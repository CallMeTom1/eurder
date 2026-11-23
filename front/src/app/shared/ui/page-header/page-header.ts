import { Component, input } from '@angular/core';

@Component({
  selector: 'app-page-header',
  templateUrl: './page-header.html',
  host: { class: 'flex flex-wrap items-end justify-between gap-4' },
})
export class PageHeader {
  readonly title = input.required<string>();
  readonly subtitle = input<string>();
}
