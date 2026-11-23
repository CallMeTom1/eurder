import { Component, input, model } from '@angular/core';
import { MatPaginator, PageEvent } from '@angular/material/paginator';

const PAGE_SIZES = [10, 25, 50];

export function pageOf<T>(items: readonly T[], pageIndex: number, pageSize: number): T[] {
  return items.slice(pageIndex * pageSize, (pageIndex + 1) * pageSize);
}

@Component({
  imports: [MatPaginator],
  selector: 'app-pagination',
  template: `
    <mat-paginator
      [length]="length()"
      [pageIndex]="pageIndex()"
      [pageSize]="pageSize()"
      [pageSizeOptions]="pageSizes"
      [showFirstLastButtons]="true"
      (page)="changePage($event)"
    />
  `,
})
export class Pagination {
  readonly length = input.required<number>();
  readonly pageIndex = model(0);
  readonly pageSize = model(PAGE_SIZES[0]);

  protected readonly pageSizes = PAGE_SIZES;

  protected changePage(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
  }
}
