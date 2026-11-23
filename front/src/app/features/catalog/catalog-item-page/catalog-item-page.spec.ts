import { HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import type { CatalogItemDto, CurrentUserDto } from '@core/api/eurder-api';
import { provideApiTesting } from '@core/api/testing';
import { provideTestTranslations } from '@core/i18n/testing';
import { SecurityService } from '@core/security/security.service';
import { BasketService } from '@features/basket/basket.service';
import { firstValueFrom } from 'rxjs';
import { routes } from '../../../app.routes';

describe('CatalogItemPage', () => {
  let harness: RouterTestingHarness;
  let backend: HttpTestingController;

  const chair: CatalogItemDto = {
    id: 'i1',
    name: 'Chair',
    description: 'Wooden chair',
    price: 49.99,
    stock: 15,
    urgency: 'STOCK_HIGH',
  };

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
    return harness.routeNativeElement!.querySelector('main') as HTMLElement;
  }

  async function open(item: CatalogItemDto): Promise<void> {
    await harness.navigateByUrl(`/catalog/${item.id}`);
    backend.expectOne({ method: 'GET', url: `/api/catalog/${item.id}` }).flush(item);
    await harness.fixture.whenStable();
  }

  async function loggedInAs(user: CurrentUserDto): Promise<void> {
    const loaded = firstValueFrom(TestBed.inject(SecurityService).loadCurrentUser());
    backend.expectOne('/api/auth/me').flush(user);
    await loaded;
  }

  function link(name: string): string | null | undefined {
    return [...page().querySelectorAll('a')]
      .find((candidate) => candidate.textContent?.trim() === name)
      ?.getAttribute('href');
  }

  it('given an item, when opening its page, then it shows its name, price, stock and description and leads back to the catalog', async () => {
    await open(chair);

    const text = page().textContent;

    expect(page().querySelector('h1')?.textContent?.trim()).toBe('Chair');
    expect(text).toContain('€49.99');
    expect(text).toContain('Wooden chair');
    expect(page().querySelector('app-stock-urgency-badge')?.textContent?.trim()).toBe('High stock');
    expect(link('Back to the catalog')).toBe('/catalog');
  });

  it('given a visitor, when opening an item, then they can add it to the basket but not edit it', async () => {
    await open(chair);

    const button = page().querySelector('app-add-to-basket-button button');

    expect(button?.textContent).toContain('Add to basket');
    expect(link('Edit')).toBeUndefined();
  });

  it('given an admin, when opening an item, then they can edit it but not order it', async () => {
    await loggedInAs({
      id: 'a',
      role: 'ADMIN',
      email: 'admin@eurder.be',
      firstName: null,
      lastName: null,
    });

    await open(chair);

    expect(link('Edit')).toBe('/admin/items/i1/edit');
    expect(page().querySelector('app-add-to-basket-button button')).toBeNull();
  });

  it('given an admin in the back office, when opening an item, then Back leads to the back office items', async () => {
    await loggedInAs({
      id: 'a',
      role: 'ADMIN',
      email: 'admin@eurder.be',
      firstName: null,
      lastName: null,
    });

    await harness.navigateByUrl('/admin/items/i1');
    backend.expectOne('/api/catalog/i1').flush(chair);
    await harness.fixture.whenStable();

    expect(page().querySelector('h1')?.textContent?.trim()).toBe('Chair');
    expect(link('Back')).toBe('/admin/items');
    expect(link('Edit')).toBe('/admin/items/i1/edit');
  });

  it('given an item, when adding it to the basket from its page, then the basket holds it', async () => {
    await open(chair);

    page().querySelector<HTMLButtonElement>('app-add-to-basket-button button')!.click();
    await harness.fixture.whenStable();

    expect(TestBed.inject(BasketService).count()).toBe(1);
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
    expect(link('Back to the catalog')).toBe('/catalog');
  });
});
