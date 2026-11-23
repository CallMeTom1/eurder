import { HttpClient } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { ApiRoutes } from '@core/api/api-routes';
import type { CustomerDto, ReportDto, UpdateCustomerDto } from '@core/api/eurder-api';
import { Report, toReport } from '@features/eurder/report';
import { map, Observable } from 'rxjs';

@Service()
export class AdminCustomerService {
  private readonly http = inject(HttpClient);

  getCustomers(): Observable<CustomerDto[]> {
    return this.http.get<CustomerDto[]>(ApiRoutes.customers.all);
  }

  getCustomer(id: string): Observable<CustomerDto> {
    return this.http.get<CustomerDto>(ApiRoutes.customers.one(id));
  }

  updateCustomer(id: string, customer: UpdateCustomerDto): Observable<CustomerDto> {
    return this.http.put<CustomerDto>(ApiRoutes.customers.one(id), customer);
  }

  getReport(customerId: string): Observable<Report> {
    return this.http.get<ReportDto>(ApiRoutes.eurders.reportOf(customerId)).pipe(map(toReport));
  }
}
