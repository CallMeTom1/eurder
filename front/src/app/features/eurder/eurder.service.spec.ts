import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { EurderService } from './eurder.service';

describe('EurderService', () => {
  let eurders: EurderService;
  let backend: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClientTesting()],
    });
    eurders = TestBed.inject(EurderService);
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  it('given basket lines, when ordering them, then only the item ids and the amounts are sent', async () => {
    const placed = firstValueFrom(
      eurders.addEurder([
        { itemId: 'i1', name: 'Chair', priceInCents: 4999, amount: 2 },
        { itemId: 'i2', name: 'Lamp', priceInCents: 2900, amount: 1 },
      ]),
    );

    const request = backend.expectOne({ method: 'POST', url: '/api/eurders' });
    request.flush({ id: 'e1', itemGroups: [], totalPrice: 128.98 });

    expect(request.request.body).toEqual({
      itemGroups: [
        { itemId: 'i1', amount: 2 },
        { itemId: 'i2', amount: 1 },
      ],
    });
    expect(await placed).toEqual({ id: 'e1', itemGroups: [], totalPrice: 128.98 });
  });
});
