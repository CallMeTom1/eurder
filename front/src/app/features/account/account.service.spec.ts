import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import type { CreateCustomerDto } from '@core/api/eurder-api';
import { firstValueFrom } from 'rxjs';
import { AccountService } from './account.service';

describe('AccountService', () => {
  it('given a registration, when sending it, then it posts the new customer to the API', async () => {
    TestBed.configureTestingModule({
      providers: [provideHttpClientTesting()],
    });
    const backend = TestBed.inject(HttpTestingController);
    const registration: CreateCustomerDto = {
      firstName: 'Jane',
      lastName: 'Doe',
      email: 'jane@doe.be',
      password: 'secret123',
      phoneNumber: '0470 12 34 56',
      address: {
        street: 'Rue Haute',
        houseNumber: '1',
        postalCode: '1000',
        city: 'Bruxelles',
        country: 'BELGIUM',
      },
    };

    const registering = firstValueFrom(TestBed.inject(AccountService).addCustomer(registration));
    const request = backend.expectOne('/api/customers');
    request.flush({ id: 'c1' });
    await registering;

    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(registration);
    backend.verify();
  });
});
