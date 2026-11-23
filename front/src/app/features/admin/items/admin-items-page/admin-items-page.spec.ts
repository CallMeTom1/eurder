import { HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import type { ItemOverviewDto } from '@core/api/eurder-api';
import { provideApiTesting } from '@core/api/testing';
import { provideTestTranslations } from '@core/i18n/testing';
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
  };
  const chair: ItemOverviewDto = {
    id: 'i1',
    name: 'Chair',
    description: 'Wooden chair',
    price: 49.99,
    availableUnits: { PRISTINE: 10, SLIGHTLY_DAMAGED: 2, DAMAGED: 0 },
    urgency: 'STOCK_HIGH',
  };

  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [AdminItemsPage],
      providers: [provideTestTranslations(), provideRouter([]), provideApiTesting()],
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
    return [...page.querySelectorAll('app-item-card h2')].map((name) => name.textContent!.trim());
  }

  it('given the items, when the page opens, then they come in the order of the API, the most urgent first, each leading to its admin detail', async () => {
    await answer([lamp, chair]);

    const firstLink = page.querySelector('app-item-card a');

    expect(page.querySelector('h1')?.textContent?.trim()).toBe('Items');
    expect(names()).toEqual(['Lamp', 'Chair']);
    expect(firstLink?.getAttribute('href')).toBe('/admin/items/i2');
    expect(page.querySelector('app-stock-urgency-badge')?.textContent?.trim()).toBe('Low stock');
  });

  it('given the items, when the page opens, then it offers to create a new item', async () => {
    await answer([lamp]);

    const link = [...page.querySelectorAll('a')].find(
      (candidate) => candidate.textContent?.trim() === 'New item',
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
});
