import { HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import type { CatalogItemDto } from '@core/api/eurder-api';
import { provideApiTesting } from '@core/api/testing';
import { provideTestTranslations } from '@core/i18n/testing';
import { ItemEditPage } from './item-edit-page';

describe('ItemEditPage', () => {
  let fixture: ComponentFixture<ItemEditPage>;
  let page: HTMLElement;
  let backend: HttpTestingController;
  let navigate: ReturnType<typeof vi.spyOn>;

  const chair: CatalogItemDto = {
    id: 'i1',
    name: 'Chair',
    description: 'Wooden chair',
    price: 49.99,
    stock: 15,
    urgency: 'STOCK_HIGH',
    image: 'images/3f2a9c1e-5b7d-4e8a-9c21-7d4e5f6a8b90.jpg',
  };

  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [ItemEditPage],
      providers: [provideTestTranslations(), provideRouter([]), provideApiTesting()],
    }).compileComponents();
    navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    backend = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(ItemEditPage);
    fixture.componentRef.setInput('id', 'i1');
    page = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
    backend.expectOne({ method: 'GET', url: '/api/catalog/i1' }).flush(chair);
    await fixture.whenStable();
  });

  afterEach(() => backend.verify());

  function control(label: string): HTMLInputElement | HTMLTextAreaElement {
    const field = [...page.querySelectorAll('.field')].find(
      (candidate) => candidate.querySelector('.field-label')?.textContent?.trim() === label,
    );
    return field!.querySelector<HTMLInputElement | HTMLTextAreaElement>('input, textarea')!;
  }

  function type(label: string, value: string): void {
    const input = control(label);
    input.value = value;
    input.dispatchEvent(new Event('input'));
  }

  async function update(): Promise<void> {
    page.querySelector<HTMLButtonElement>('button[type="submit"]')!.click();
    await fixture.whenStable();
  }

  async function settle(): Promise<void> {
    await new Promise((resolve) => setTimeout(resolve));
    await fixture.whenStable();
  }

  it('given an item, when the edit page opens, then its id heads the page and the fields hold its values', () => {
    const header = page.querySelector('app-page-header');

    expect(header?.querySelector('h1')?.textContent?.trim()).toBe('Edit item');
    expect(header?.textContent).toContain('i1');
    expect(control('Name').value).toBe('Chair');
    expect(control('Description').value).toBe('Wooden chair');
    expect(control('Price').value).toBe('49.99');
    expect(control('Amount of stock').value).toBe('15');
  });

  it('given new details and more stock, when updating, then the details are sent, the missing units added and the detail opens', async () => {
    type('Description', 'Oak chair');
    type('Amount of stock', '20');

    await update();
    const put = backend.expectOne({ method: 'PUT', url: '/api/items/i1' });
    expect(put.request.body).toEqual({
      name: 'Chair',
      description: 'Oak chair',
      price: 49.99,
      image: 'images/3f2a9c1e-5b7d-4e8a-9c21-7d4e5f6a8b90.jpg',
    });
    put.flush({ id: 'i1' });
    await settle();
    const units = backend.expectOne({ method: 'POST', url: '/api/items/i1/units' });
    expect(units.request.body).toEqual({ amount: 5, state: 'PRISTINE' });
    units.flush({ id: 'i1' });
    await settle();

    expect(navigate).toHaveBeenCalledWith(['/admin/items', 'i1']);
  });

  it('given the same stock, when updating, then no unit is added', async () => {
    type('Price', '59');

    await update();
    backend.expectOne({ method: 'PUT', url: '/api/items/i1' }).flush({ id: 'i1' });
    await settle();

    backend.expectNone('/api/items/i1/units');
    expect(navigate).toHaveBeenCalledWith(['/admin/items', 'i1']);
  });

  it('given a lower stock, when updating, then the stock cannot go down and nothing is sent', async () => {
    type('Amount of stock', '10');

    await update();

    expect(
      control('Amount of stock')
        .closest('mat-form-field')
        ?.querySelector('mat-error')
        ?.textContent?.trim(),
    ).toBe('At least 15.');
    backend.expectNone('/api/items/i1');
  });

  it('given the form, when giving up, then Cancel leads back to the item', () => {
    const cancel = [...page.querySelectorAll('a')].find(
      (link) => link.textContent?.trim() === 'Cancel',
    );

    expect(cancel?.getAttribute('href')).toBe('/admin/items/i1');
  });
});
