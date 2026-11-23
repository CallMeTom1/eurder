import { HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { provideApiTesting } from '@core/api/testing';
import { provideTestTranslations } from '@core/i18n/testing';
import { BasketService } from '@features/basket/basket.service';
import { routes } from '../../../app.routes';

describe('CatalogItemPage', () => {
  let harness: RouterTestingHarness;
  let backend: HttpTestingController;

  beforeEach(async () => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideTestTranslations(),
        provideRouter(routes, withComponentInputBinding()),
        provideApiTesting(),
      ],
    });
    backend = TestBed.inject(HttpTestingController);
    harness = await RouterTestingHarness.create();
  });

  afterEach(() => backend.verify());

  function page(): HTMLElement {
    return harness.routeNativeElement as HTMLElement;
  }

  it('given an item, when opening its page, then it shows its name, description and price and leads back to the catalog', async () => {
    await harness.navigateByUrl('/catalog/i1');

    backend
      .expectOne({ method: 'GET', url: '/api/catalog/i1' })
      .flush({ id: 'i1', name: 'Chair', description: 'Wooden chair', price: 49.99 });
    await harness.fixture.whenStable();

    expect(page().querySelector('h1')?.textContent?.trim()).toBe('Chair');
    expect(page().textContent).toContain('Wooden chair');
    expect(page().textContent).toContain('€49.99');
    expect(page().querySelector('main a[href="/catalog"]')?.textContent?.trim()).toBe(
      'Back to the catalog',
    );
  });

  it('given an unknown item, when opening its page, then it says the item does not exist and leads back', async () => {
    await harness.navigateByUrl('/catalog/nope');

    backend
      .expectOne('/api/catalog/nope')
      .flush(
        { status: 404, code: 'error.item.not-found', params: { id: 'nope' } },
        { status: 404, statusText: 'Not Found' },
      );
    await harness.fixture.whenStable();

    expect(page().querySelector('[role="alert"]')?.textContent).toContain(
      'This item does not exist.',
    );
    expect(page().querySelector('main a[href="/catalog"]')).not.toBeNull();
  });

  it('given an item, when adding it to the basket from its page, then the basket holds it', async () => {
    await harness.navigateByUrl('/catalog/i1');
    backend
      .expectOne('/api/catalog/i1')
      .flush({ id: 'i1', name: 'Chair', description: 'Wooden chair', price: 49.99 });
    await harness.fixture.whenStable();

    page().querySelector<HTMLButtonElement>('main app-add-to-basket-button button')!.click();
    await harness.fixture.whenStable();

    expect(TestBed.inject(BasketService).count()).toBe(1);
  });
});
