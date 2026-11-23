import { inject, Injectable, Provider } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatPaginatorIntl } from '@angular/material/paginator';
import { TranslateService } from '@ngx-translate/core';

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
