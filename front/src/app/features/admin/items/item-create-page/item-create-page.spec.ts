import { HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { provideApiTesting } from '@core/api/testing';
import { provideTestTranslations } from '@core/i18n/testing';
import { ItemCreatePage } from './item-create-page';

describe('ItemCreatePage', () => {
  let fixture: ComponentFixture<ItemCreatePage>;
  let page: HTMLElement;
  let backend: HttpTestingController;
  let navigate: ReturnType<typeof vi.spyOn>;

  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [ItemCreatePage],
      providers: [provideTestTranslations(), provideRouter([]), provideApiTesting()],
    }).compileComponents();
    navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    backend = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(ItemCreatePage);
    page = fixture.nativeElement as HTMLElement;
    await fixture.whenStable();
  });

  afterEach(() => backend.verify());

  function control(label: string): HTMLInputElement | HTMLTextAreaElement {
    const formField = [...page.querySelectorAll('mat-form-field')].find(
      (candidate) => candidate.querySelector('mat-label')?.textContent?.trim() === label,
    );
    return formField!.querySelector<HTMLInputElement | HTMLTextAreaElement>('input, textarea')!;
  }

  function type(label: string, value: string): void {
    const input = control(label);
    input.value = value;
    input.dispatchEvent(new Event('input'));
  }

  function errorOf(label: string): string | undefined {
    return control(label)
      .closest('mat-form-field')
      ?.querySelector('mat-error')
      ?.textContent?.trim();
  }

  function fillIn(): void {
    type('Name', 'Chair');
    type('Description', 'Wooden chair');
    type('Price', '49.99');
    type('Amount of stock', '15');
  }

  async function submit(): Promise<void> {
    page.querySelector<HTMLButtonElement>('button[type="submit"]')!.click();
    await fixture.whenStable();
  }

  async function settle(): Promise<void> {
    await new Promise((resolve) => setTimeout(resolve));
    await fixture.whenStable();
  }

  it('given the form, when it opens, then it is titled Create item with the price in euro and the description counter', () => {
    const counter = page.querySelector('app-char-counter');

    expect(page.querySelector('h1')?.textContent?.trim()).toBe('Create item');
    expect(control('Price').closest('mat-form-field')?.textContent).toContain('€');
    expect(counter?.textContent?.trim()).toBe('255 characters left');
  });

  it('given an empty form, when creating, then every field is required and nothing is sent', async () => {
    await submit();

    expect(errorOf('Name')).toBe('This field is required.');
    expect(errorOf('Description')).toContain('This field is required.');
    expect(errorOf('Price')).toBe('This field is required.');
    expect(errorOf('Amount of stock')).toBe('This field is required.');
    backend.expectNone('/api/items');
  });

  it('given a negative price and stock, when creating, then both are refused', async () => {
    fillIn();
    type('Price', '-1');
    type('Amount of stock', '-2');

    await submit();

    expect(errorOf('Price')).toBe('Must be greater than 0.');
    expect(errorOf('Amount of stock')).toBe('Must not be negative.');
    backend.expectNone('/api/items');
  });

  it('given a description, when typing it, then the counter shows the characters left', async () => {
    type('Description', 'Wooden chair');
    await fixture.whenStable();

    const counter = page.querySelector('app-char-counter');

    expect(counter?.textContent?.trim()).toBe('243 characters left');
  });

  it('given a complete form, when creating, then the item is sent and its detail opens', async () => {
    fillIn();

    await submit();
    const request = backend.expectOne({ method: 'POST', url: '/api/items' });
    request.flush({ id: 'i1' });
    await settle();

    expect(request.request.body).toEqual({
      name: 'Chair',
      description: 'Wooden chair',
      price: 49.99,
      units: { PRISTINE: 15 },
    });
    expect(navigate).toHaveBeenCalledWith(['/admin/items', 'i1']);
  });

  it('given a field refused by the API, when creating, then that field shows the reason', async () => {
    fillIn();

    await submit();
    backend.expectOne('/api/items').flush(
      {
        status: 400,
        code: 'error.common.validation-failed',
        params: {},
        errors: [{ field: 'name', code: 'validation.too-long', params: { max: 255 } }],
      },
      { status: 400, statusText: 'Bad Request' },
    );
    await settle();

    expect(errorOf('Name')).toBe('At most 255 characters.');
    expect(navigate).not.toHaveBeenCalled();
  });

  it('given the form, when giving up, then Cancel leads back to the items', () => {
    const cancel = [...page.querySelectorAll('a')].find(
      (link) => link.textContent?.trim() === 'Cancel',
    );

    expect(cancel?.getAttribute('href')).toBe('/admin/items');
  });
});
