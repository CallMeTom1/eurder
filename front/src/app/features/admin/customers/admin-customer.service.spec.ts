import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import type { CustomerDto, UpdateCustomerDto } from '@core/api/eurder-api';
import { firstValueFrom } from 'rxjs';
import { AdminCustomerService } from './admin-customer.service';

describe('AdminCustomerService', () => {
  let customers: AdminCustomerService;
  let backend: HttpTestingController;

  const jane: CustomerDto = {
    id: 'c1',
    firstName: 'Jane',
    lastName: 'Doe',
    email: 'jane@doe.be',
    phoneNumber: '0470123456',
    address: {
      street: 'Rue Haute',
      houseNumber: '1',
      postalCode: '1000',
      city: 'Bruxelles',
      country: 'BELGIUM',
    },
  };

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClientTesting()] });
    customers = TestBed.inject(AdminCustomerService);
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  it('given the customers of the API, when listing them, then they come as the API sends them', async () => {
    const listed = firstValueFrom(customers.getCustomers());

    backend.expectOne({ method: 'GET', url: '/api/customers' }).flush([jane]);

    expect(await listed).toEqual([jane]);
  });

  it('given a customer id, when reading it, then it asks the API for that customer', async () => {
    const read = firstValueFrom(customers.getCustomer('c1'));

    backend.expectOne({ method: 'GET', url: '/api/customers/c1' }).flush(jane);

    expect(await read).toEqual(jane);
  });

  it('given changed details, when updating a customer, then they are put on its address', async () => {
    const { id, ...details } = jane;
    const changes: UpdateCustomerDto = { ...details, firstName: 'Janet' };
    const updated = firstValueFrom(customers.updateCustomer(id, changes));

    const request = backend.expectOne({ method: 'PUT', url: '/api/customers/c1' });
    request.flush({ ...jane, firstName: 'Janet' });

    expect(request.request.body).toEqual(changes);
    expect((await updated).firstName).toBe('Janet');
  });

  it('given a customer, when reading its report, then the prices come in cents', async () => {
    const report = firstValueFrom(customers.getReport('c1'));

    backend.expectOne({ method: 'GET', url: '/api/eurders/reports/c1' }).flush({
      eurders: [],
      totalPrice: 12.5,
    });

    expect(await report).toEqual({ eurders: [], totalInCents: 1250 });
  });
});
