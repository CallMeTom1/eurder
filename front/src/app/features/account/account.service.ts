import { HttpClient } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { ApiRoutes } from '@core/api/api-routes';
import type { CreateCustomerDto, CustomerDto } from '@core/api/eurder-api';
import { Observable } from 'rxjs';

@Service()
export class AccountService {
  private readonly http = inject(HttpClient);

  register(registration: CreateCustomerDto): Observable<CustomerDto> {
    return this.http.post<CustomerDto>(ApiRoutes.customers.all, registration);
  }
}
