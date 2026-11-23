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

  it('given the orders of the customer, when reading the report, then the prices come in cents', async () => {
    const report = firstValueFrom(eurders.getReport());

    backend.expectOne({ method: 'GET', url: '/api/eurders/report' }).flush({
      eurders: [],
      totalPrice: 10,
    });

    expect(await report).toEqual({ eurders: [], totalInCents: 1000 });
  });

  it('given a past order, when ordering it again, then the API is asked to reorder it', async () => {
    const reordered = firstValueFrom(eurders.reorder('e1'));

    backend
      .expectOne({ method: 'POST', url: '/api/eurders/e1/reorder' })
      .flush({ id: 'e2', itemGroups: [], totalPrice: 10 });

    expect((await reordered).id).toBe('e2');
  });

  it('given the admin, when reading what ships today, then the shipments come as the API sends them', async () => {
    const shipping = firstValueFrom(eurders.getShippingToday());

    backend.expectOne({ method: 'GET', url: '/api/eurders/shipping-today' }).flush([]);

    expect(await shipping).toEqual([]);
  });
});
