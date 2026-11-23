import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { AdminItemService } from './admin-item.service';

describe('AdminItemService', () => {
  let items: AdminItemService;
  let backend: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClientTesting()],
    });
    items = TestBed.inject(AdminItemService);
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  it('given the items of the API, when listing them, then each one comes with its whole stock and its price in cents', async () => {
    const listed = firstValueFrom(items.getItems());

    backend.expectOne({ method: 'GET', url: '/api/items' }).flush([
      {
        id: 'i1',
        name: 'Chair',
        description: 'Wooden chair',
        price: 49.99,
        availableUnits: { PRISTINE: 3, SLIGHTLY_DAMAGED: 1, DAMAGED: 0 },
        urgency: 'STOCK_LOW',
      },
    ]);

    expect(await listed).toEqual([
      {
        id: 'i1',
        name: 'Chair',
        description: 'Wooden chair',
        priceInCents: 4999,
        stock: 4,
        urgency: 'STOCK_LOW',
      },
    ]);
  });

  it('given a new item, when adding it, then its stock is created as pristine units and its id comes back', async () => {
    const added = firstValueFrom(
      items.addItem({ name: 'Chair', description: 'Wooden chair', price: 49.99, stock: 15 }),
    );

    const request = backend.expectOne({ method: 'POST', url: '/api/items' });
    request.flush({ id: 'i1' });

    expect(request.request.body).toEqual({
      name: 'Chair',
      description: 'Wooden chair',
      price: 49.99,
      units: { PRISTINE: 15 },
    });
    expect(await added).toBe('i1');
  });

  it('given changed details, when updating an item, then only the name, the description and the price are sent', async () => {
    const updated = firstValueFrom(
      items.updateItem('i1', { name: 'Chair', description: 'Oak chair', price: 59 }),
    );

    const request = backend.expectOne({ method: 'PUT', url: '/api/items/i1' });
    request.flush({ id: 'i1' });

    expect(request.request.body).toEqual({ name: 'Chair', description: 'Oak chair', price: 59 });
    await updated;
  });

  it('given more stock, when adding units, then pristine units are added', async () => {
    const added = firstValueFrom(items.addUnits('i1', 5));

    const request = backend.expectOne({ method: 'POST', url: '/api/items/i1/units' });
    request.flush({ id: 'i1' });

    expect(request.request.body).toEqual({ amount: 5, state: 'PRISTINE' });
    await added;
  });
});
