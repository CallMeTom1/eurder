import { HttpClient } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { ApiRoutes } from '@core/api/api-routes';
import type { CatalogItemDto } from '@core/api/eurder-api';
import { map, Observable } from 'rxjs';
import { CatalogItem, toCatalogItem } from './catalog-item';

@Service()
export class CatalogService {
  private readonly http = inject(HttpClient);

  findAll(): Observable<CatalogItem[]> {
    return this.http
      .get<CatalogItemDto[]>(ApiRoutes.catalog.all)
      .pipe(map((items) => items.map(toCatalogItem)));
  }

  findOne(id: string): Observable<CatalogItem> {
    return this.http.get<CatalogItemDto>(ApiRoutes.catalog.one(id)).pipe(map(toCatalogItem));
  }
}
