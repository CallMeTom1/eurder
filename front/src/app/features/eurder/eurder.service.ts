import { HttpClient } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { ApiRoutes } from '@core/api/api-routes';
import type {
  CreateEurderDto,
  EurderDto,
  ReportDto,
  ShippingEurderDto,
} from '@core/api/eurder-api';
import { BasketLine } from '@features/basket/basket-line';
import { map, Observable } from 'rxjs';
import { Report, toReport } from './report';

@Service()
export class EurderService {
  private readonly http = inject(HttpClient);

  addEurder(lines: readonly BasketLine[]): Observable<EurderDto> {
    const eurder: CreateEurderDto = {
      itemGroups: lines.map(({ itemId, amount }) => ({ itemId, amount })),
    };
    return this.http.post<EurderDto>(ApiRoutes.eurders.all, eurder);
  }

  getReport(): Observable<Report> {
    return this.http.get<ReportDto>(ApiRoutes.eurders.report).pipe(map(toReport));
  }

  reorder(id: string): Observable<EurderDto> {
    return this.http.post<EurderDto>(ApiRoutes.eurders.reorder(id), null);
  }

  getShippingToday(): Observable<ShippingEurderDto[]> {
    return this.http.get<ShippingEurderDto[]>(ApiRoutes.eurders.shippingToday);
  }
}
