import { HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import type { ItemOverviewDto } from '@core/api/eurder-api';
import { provideApiTesting } from '@core/api/testing';
import { provideTestTranslations } from '@core/i18n/testing';
import { providePaginatorTranslations } from '@core/i18n/paginator-intl';
import { AdminItemsPage } from './admin-items-page';

describe('AdminItemsPage', () => {
  let fixture: ComponentFixture<AdminItemsPage>;
  let page: HTMLElement;
  let backend: HttpTestingController;

  const lamp: ItemOverviewDto = {
    id: 'i2',
    name: 'Lamp',
    description: 'Desk lamp',
    price: 29,
    availableUnits: { PRISTINE: 1, SLIGHTLY_DAMAGED: 0, DAMAGED: 0 },
    urgency: 'STOCK_LOW',
    image: null,
  };
  const chair: ItemOverviewDto = {
    id: 'i1',
    name: 'Chair',
    description: 'Wooden chair',
    price: 49.99,
    availableUnits: { PRISTINE: 10, SLIGHTLY_DAMAGED: 2, DAMAGED: 0 },
    urgency: 'STOCK_HIGH',
    image: null,
  };

  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [AdminItemsPage],
      providers: [
        provideTestTranslations(),
        provideRouter([]),
        provideApiTesting(),
        providePaginatorTranslations(),
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(AdminItemsPage);
    page = fixture.nativeElement as HTMLElement;
    backend = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => backend.verify());

  async function answer(items: ItemOverviewDto[]): Promise<void> {
    backend.expectOne({ method: 'GET', url: '/api/items' }).flush(items);
    await fixture.whenStable();
  }

  function names(): string[] {
    return [...page.querySelectorAll('[data-testid="admin-item"] th')].map((name) =>
      name.textContent!.trim(),
    );
  }

  it('given the items, when the page opens, then each row shows the name, price, whole stock and urgency, the most urgent first', async () => {
    await answer([lamp, chair]);

    const chairRow = page.querySelectorAll('[data-testid="admin-item"]')[1];

    expect(page.querySelector('h1')?.textContent?.trim()).toBe('Items');
    expect(names()).toEqual(['Lamp', 'Chair']);
    expect(
      [...chairRow.querySelectorAll('td')].slice(0, 2).map((cell) => cell.textContent?.trim()),
    ).toEqual(['€49.99', '12']);
    expect(chairRow.querySelector('app-stock-urgency-badge')?.textContent?.trim()).toBe(
      'High stock',
    );
  });

  it('given an item row, when reading it, then its name leads to its detail and the pencil to its edit page', async () => {
    await answer([chair]);

    const row = page.querySelector('[data-testid="admin-item"]')!;

    expect(row.querySelector('th a')?.getAttribute('href')).toBe('/admin/items/i1');
    expect(row.querySelector('a[aria-label="Edit Chair"]')?.getAttribute('href')).toBe(
      '/admin/items/i1/edit',
    );
  });

  it('given the items, when the page opens, then it offers to create a new item', async () => {
    await answer([lamp]);

    const link = [...page.querySelectorAll('a')].find((candidate) =>
      candidate.textContent?.includes('New item'),
    );

    expect(link?.getAttribute('href')).toBe('/admin/items/new');
  });

  it('given the items, when filtering by name, then only the names starting with it stay', async () => {
    await answer([lamp, chair]);
    const input = page.querySelector<HTMLInputElement>('input[type="search"]')!;

    input.value = 'CH';
    input.dispatchEvent(new Event('input'));
    await fixture.whenStable();

    expect(names()).toEqual(['Chair']);
  });

  it('given no item, when the page opens, then it says so', async () => {
    await answer([]);

    const text = page.textContent;

    expect(text).toContain('No item yet.');
  });

  it('given more than ten items, when the page opens, then it shows the first ten and paginates the rest', async () => {
    const many = Array.from({ length: 12 }, (_, index) => ({
      ...chair,
      id: `i${index}`,
      name: `Chair ${String(index).padStart(2, '0')}`,
    }));
    await answer(many);

    const range = page.querySelector('.mat-mdc-paginator-range-label')?.textContent?.trim();

    expect(names()).toHaveLength(10);
    expect(range).toBe('1 – 10 of 12');
  });
});
