import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { CatalogService } from './catalog.service';

describe('CatalogService', () => {
  let catalog: CatalogService;
  let backend: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClientTesting()],
    });
    catalog = TestBed.inject(CatalogService);
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  it('given the catalog of the API, when listing it, then the items come with their price in cents', async () => {
    const items = firstValueFrom(catalog.getItems());

    backend
      .expectOne('/api/catalog')
      .flush([{ id: 'i1', name: 'Chair', description: 'Wooden chair', price: 49.99 }]);

    expect(await items).toEqual([
      { id: 'i1', name: 'Chair', description: 'Wooden chair', priceInCents: 4999 },
    ]);
  });

  it('given an item id, when finding it, then it asks the API for that item', async () => {
    const item = firstValueFrom(catalog.getItem('i1'));

    backend
      .expectOne('/api/catalog/i1')
      .flush({ id: 'i1', name: 'Chair', description: 'Wooden chair', price: 49.99 });

    expect((await item).name).toBe('Chair');
  });
});
