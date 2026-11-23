import { HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import type { CatalogItemDto } from '@core/api/eurder-api';
import { provideApiTesting } from '@core/api/testing';
import { provideTestTranslations } from '@core/i18n/testing';
import { CatalogPage } from './catalog-page';

describe('CatalogPage', () => {
  let fixture: ComponentFixture<CatalogPage>;
  let page: HTMLElement;
  let backend: HttpTestingController;

  const chair: CatalogItemDto = {
    id: 'i1',
    name: 'Chair',
    description: 'Wooden chair',
    price: 49.99,
    stock: 12,
    urgency: 'STOCK_HIGH',
  };
  const lamp: CatalogItemDto = {
    id: 'i2',
    name: 'Lamp',
    description: 'Desk lamp',
    price: 29,
    stock: 3,
    urgency: 'STOCK_LOW',
  };
  const armchair: CatalogItemDto = {
    id: 'i3',
    name: 'armchair',
    description: 'Leather',
    price: 150,
    stock: 3,
    urgency: 'STOCK_LOW',
  };

  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [CatalogPage],
      providers: [provideTestTranslations(), provideRouter([]), provideApiTesting()],
    }).compileComponents();
    fixture = TestBed.createComponent(CatalogPage);
    page = fixture.nativeElement as HTMLElement;
    backend = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => backend.verify());

  async function answer(items: CatalogItemDto[]): Promise<void> {
    backend.expectOne({ method: 'GET', url: '/api/catalog' }).flush(items);
    await fixture.whenStable();
  }

  function names(): string[] {
    return [...page.querySelectorAll('[data-testid="catalog-item"] h2')].map((name) =>
      name.textContent!.trim(),
    );
  }

  async function search(query: string): Promise<void> {
    const input = page.querySelector<HTMLInputElement>('input[type="search"]')!;
    input.value = query;
    input.dispatchEvent(new Event('input'));
    await fixture.whenStable();
  }

  it('given the catalog is loading, when the page opens, then it says so', () => {
    const status = page.querySelector('[role="status"]');

    expect(status?.textContent).toContain('Loading…');
    backend.expectOne('/api/catalog');
  });

  it('given items for sale, when the page opens, then each card shows the name, an image, the price and the stock urgency', async () => {
    await answer([chair, lamp]);

    const [chairCard, lampCard] = page.querySelectorAll('[data-testid="catalog-item"]');

    expect(names()).toEqual(['Chair', 'Lamp']);
    expect(chairCard.querySelector('[data-testid="item-image"]')).not.toBeNull();
    expect(chairCard.textContent).toContain('Price: €49.99');
    expect(chairCard.querySelector('app-stock-urgency-badge')?.textContent?.trim()).toBe(
      'High stock',
    );
    expect(lampCard.querySelector('app-stock-urgency-badge')?.textContent?.trim()).toBe(
      'Low stock',
    );
  });

  it('given an item for sale, when reading its card, then its name and its image lead to its detail and nothing else is a button', async () => {
    await answer([chair]);

    const card = page.querySelector('[data-testid="catalog-item"]')!;

    expect(card.querySelector('a h2')?.closest('a')?.getAttribute('href')).toBe('/catalog/i1');
    expect(
      card.querySelector('a [data-testid="item-image"]')?.closest('a')?.getAttribute('href'),
    ).toBe('/catalog/i1');
    expect(card.querySelector('button')).toBeNull();
  });

  it('given the catalog, when the page opens, then the filter invites to filter by name', async () => {
    await answer([chair]);

    const filter = page.querySelector<HTMLInputElement>('input[type="search"]');

    expect(filter?.getAttribute('placeholder')).toBe('Filter by name…');
    expect(filter?.getAttribute('aria-label')).toBe('Filter by name…');
  });

  it('given items for sale, when searching, then only the names starting with the search stay, whatever the case', async () => {
    await answer([chair, lamp, armchair]);

    await search('  AR ');

    expect(names()).toEqual(['armchair']);
  });

  it('given items for sale, when searching a word in the middle of a name, then nothing matches', async () => {
    await answer([chair, lamp, armchair]);

    await search('chair');

    expect(names()).toEqual(['Chair']);
  });

  it('given a search without match, when filtering, then the page says no item starts with it', async () => {
    await answer([chair, lamp]);

    await search('sofa');

    expect(page.textContent).toContain('No item starts with “sofa”.');
  });

  it('given an empty catalog, when the page opens, then it says nothing is for sale yet', async () => {
    await answer([]);

    const text = page.textContent;

    expect(text).toContain('No item for sale yet.');
  });

  it('given the API fails, when trying again, then the catalog loads', async () => {
    backend
      .expectOne('/api/catalog')
      .flush(
        { status: 404, code: 'error.common.route-not-found', params: {} },
        { status: 404, statusText: 'Not Found' },
      );
    await fixture.whenStable();
    expect(page.querySelector('[role="alert"]')?.textContent).toContain(
      'This page does not exist.',
    );

    page.querySelector<HTMLButtonElement>('[role="alert"] button')!.click();
    TestBed.tick();
    await answer([chair]);

    expect(names()).toEqual(['Chair']);
  });
});
