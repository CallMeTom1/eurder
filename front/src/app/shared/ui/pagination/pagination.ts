import { Component, inject, Injectable, input, model, Provider } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatPaginator, MatPaginatorIntl, PageEvent } from '@angular/material/paginator';
import { TranslateService } from '@ngx-translate/core';

const PAGE_SIZES = [10, 25, 50];

export function pageOf<T>(items: readonly T[], pageIndex: number, pageSize: number): T[] {
  return items.slice(pageIndex * pageSize, (pageIndex + 1) * pageSize);
}

@Injectable()
export class TranslatedPaginatorIntl extends MatPaginatorIntl {
  private readonly translate = inject(TranslateService);

  constructor() {
    super();
    this.translate.onLangChange.pipe(takeUntilDestroyed()).subscribe(() => this.translateLabels());
    this.translateLabels();
  }

  override getRangeLabel = (page: number, pageSize: number, length: number): string => {
    const start = length === 0 ? 0 : page * pageSize + 1;
    const end = Math.min((page + 1) * pageSize, length);
    return this.translate.instant('pagination.range', { start, end, length });
  };

  private translateLabels(): void {
    this.itemsPerPageLabel = this.translate.instant('pagination.itemsPerPage');
    this.nextPageLabel = this.translate.instant('pagination.next');
    this.previousPageLabel = this.translate.instant('pagination.previous');
    this.firstPageLabel = this.translate.instant('pagination.first');
    this.lastPageLabel = this.translate.instant('pagination.last');
    this.changes.next();
  }
}

export function providePaginatorTranslations(): Provider {
  return { provide: MatPaginatorIntl, useClass: TranslatedPaginatorIntl };
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
