import { HttpClient } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { ApiRoutes } from '@core/api/api-routes';
import type { CreateEurderDto, EurderDto } from '@core/api/eurder-api';
import { BasketLine } from '@features/basket/basket-line';
import { Observable } from 'rxjs';

@Service()
export class EurderService {
  private readonly http = inject(HttpClient);

  addEurder(lines: readonly BasketLine[]): Observable<EurderDto> {
    const eurder: CreateEurderDto = {
      itemGroups: lines.map(({ itemId, amount }) => ({ itemId, amount })),
    };
    return this.http.post<EurderDto>(ApiRoutes.eurders.all, eurder);
  }
}
