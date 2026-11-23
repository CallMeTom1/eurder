import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MATERIAL_ANIMATIONS } from '@angular/material/core';
import { MatDialogHarness } from '@angular/material/dialog/testing';
import { provideRouter, Router } from '@angular/router';
import type { CurrentUserDto } from '@core/api/eurder-api';
import { provideApiTesting } from '@core/api/testing';
import { provideTestTranslations } from '@core/i18n/testing';
import { SecurityService } from '@core/security/security.service';
import { firstValueFrom } from 'rxjs';
import { BasketService } from '../basket.service';
import { BasketPage } from './basket-page';

describe('BasketPage', () => {
  let fixture: ComponentFixture<BasketPage>;
  let page: HTMLElement;
  let basket: BasketService;
  let backend: HttpTestingController;
  let navigate: ReturnType<typeof vi.spyOn>;
  let documentLoader: HarnessLoader;

  const customer: CurrentUserDto = {
    id: 'c',
    role: 'CUSTOMER',
    email: 'jane@doe.be',
    firstName: 'Jane',
    lastName: 'Doe',
  };

  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [BasketPage],
      providers: [
        provideTestTranslations(),
        provideRouter([]),
        provideApiTesting(),
        { provide: MATERIAL_ANIMATIONS, useValue: { animationsDisabled: true } },
      ],
    }).compileComponents();
    basket = TestBed.inject(BasketService);
    backend = TestBed.inject(HttpTestingController);
    navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
  });

  afterEach(() => backend.verify());

  async function loggedIn(): Promise<void> {
    const loaded = firstValueFrom(TestBed.inject(SecurityService).loadCurrentUser());
    backend.expectOne('/api/auth/me').flush(customer);
    await loaded;
  }

  function placeOrderButton(): HTMLButtonElement {
    return page.querySelector<HTMLButtonElement>('[data-testid="place-order"]')!;
  }

  async function placeOrder(): Promise<void> {
    placeOrderButton().click();
    await fixture.whenStable();
  }

  async function settle(): Promise<void> {
    await new Promise((resolve) => setTimeout(resolve));
    await fixture.whenStable();
  }

  async function open(): Promise<void> {
    fixture = TestBed.createComponent(BasketPage);
    page = fixture.nativeElement as HTMLElement;
    documentLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    await fixture.whenStable();
  }

  function fill(): void {
    basket.add({ id: 'i1', name: 'Chair', priceInCents: 4999 });
    basket.add({ id: 'i2', name: 'Lamp', priceInCents: 2900 });
    basket.changeAmount('i2', 3);
  }

  function rows(): string[][] {
    return [...page.querySelectorAll('tbody tr')].map((row) =>
      [...row.querySelectorAll('th, td')]
        .slice(0, 4)
        .map((cell) => cell.querySelector('input')?.value ?? cell.textContent!.trim()),
    );
  }

  function amountOf(name: string): HTMLInputElement {
    return page.querySelector<HTMLInputElement>(`input[aria-label="Amount of ${name}"]`)!;
  }

  async function changeAmount(name: string, value: string): Promise<void> {
    const input = amountOf(name);
    input.value = value;
    input.dispatchEvent(new Event('change'));
    await fixture.whenStable();
  }

  it('given an empty basket, when opening it, then it says so and leads to the catalog', async () => {
    await open();

    const link = page.querySelector('a[href="/catalog"]');

    expect(page.textContent).toContain('Your basket is empty.');
    expect(link?.textContent?.trim()).toBe('Browse the catalog');
    expect(page.querySelector('table')).toBeNull();
  });

  it('given a filled basket, when opening it, then each line shows its name, amount, unit price and price, and the total', async () => {
    fill();

    await open();

    expect(rows()).toEqual([
      ['Chair', '1', '€49.99', '€49.99'],
      ['Lamp', '3', '€29.00', '€87.00'],
    ]);
    expect(page.querySelector('tfoot')?.textContent).toContain('€136.99');
    expect(page.querySelector('tbody a')?.getAttribute('href')).toBe('/catalog/i1');
  });

  it('given a line, when raising its amount, then its price and the total follow', async () => {
    fill();
    await open();

    await changeAmount('Chair', '2');

    expect(rows()[0]).toEqual(['Chair', '2', '€49.99', '€99.98']);
    expect(page.querySelector('tfoot')?.textContent).toContain('€186.98');
  });

  it.each([{ value: '0' }, { value: '-1' }, { value: '' }, { value: '1.5' }])(
    'given a line, when typing the amount $value, then the amount goes back to the one of the basket',
    async ({ value }) => {
      fill();
      await open();

      await changeAmount('Lamp', value);

      expect(amountOf('Lamp').value).toBe('3');
      expect(basket.lines()[1].amount).toBe(3);
    },
  );

  it('given a line, when removing it, then it leaves the basket and the total', async () => {
    fill();
    await open();

    page.querySelector<HTMLButtonElement>('button[aria-label="Remove Chair"]')!.click();
    await fixture.whenStable();

    expect(rows().map(([name]) => name)).toEqual(['Lamp']);
    expect(page.querySelector('tfoot')?.textContent).toContain('€87.00');
  });

  it('given the last line, when removing it, then the basket says it is empty', async () => {
    basket.add({ id: 'i1', name: 'Chair', priceInCents: 4999 });
    await open();

    page.querySelector<HTMLButtonElement>('button[aria-label="Remove Chair"]')!.click();
    await fixture.whenStable();

    expect(page.textContent).toContain('Your basket is empty.');
  });

  it('given a filled basket, when opening it, then it offers to place the order', async () => {
    fill();

    await open();

    expect(page.querySelector('button[type="button"]:not([aria-label])')?.textContent?.trim()).toBe(
      'Place order',
    );
  });

  it('given an anonymous visitor, when placing the order, then they are asked to log in first and come back to the basket', async () => {
    fill();
    await open();

    await placeOrder();

    expect(navigate).toHaveBeenCalledWith(['/login'], { queryParams: { returnUrl: '/basket' } });
    backend.expectNone('/api/eurders');
  });

  it('given a customer, when placing the order, then the item ids and amounts are sent and the button waits', async () => {
    await loggedIn();
    fill();
    await open();

    await placeOrder();

    const request = backend.expectOne({ method: 'POST', url: '/api/eurders' });
    expect(request.request.body).toEqual({
      itemGroups: [
        { itemId: 'i1', amount: 1 },
        { itemId: 'i2', amount: 3 },
      ],
    });
    expect(placeOrderButton().disabled).toBe(true);
  });

  it('given a placed order, when the API registers it, then a dialog says so, the basket is emptied and OK leads to the catalog', async () => {
    await loggedIn();
    fill();
    await open();
    await placeOrder();

    backend.expectOne('/api/eurders').flush({ id: 'e1', itemGroups: [], totalPrice: 136.99 });
    await settle();
    const dialog = await documentLoader.getHarness(MatDialogHarness);

    expect(await dialog.getTitleText()).toBe('Order placed');
    expect(await dialog.getContentText()).toBe(
      'Your order has been successfully placed. Thank you!',
    );
    expect(basket.lines()).toEqual([]);
    await (await dialog.getHarness(MatButtonHarness.with({ text: 'OK' }))).click();
    await settle();
    expect(TestBed.inject(Router).navigateByUrl).toHaveBeenCalledWith('/catalog');
  });

  it('given an order refused by the API, when placing it, then the page says why and keeps the basket', async () => {
    await loggedIn();
    fill();
    await open();
    await placeOrder();

    backend
      .expectOne('/api/eurders')
      .flush(
        { status: 404, code: 'error.item.not-found', params: { id: 'i2' } },
        { status: 404, statusText: 'Not Found' },
      );
    await settle();

    expect(page.querySelector('[role="alert"]')?.textContent).toContain(
      'This item does not exist.',
    );
    expect(basket.count()).toBe(4);
    expect(placeOrderButton().disabled).toBe(false);
  });

  it('given a session lost on the way, when placing the order, then they are asked to log in again and come back to the basket', async () => {
    await loggedIn();
    fill();
    await open();
    await placeOrder();

    backend
      .expectOne('/api/eurders')
      .flush(
        { status: 401, code: 'error.security.authentication-required', params: {} },
        { status: 401, statusText: 'Unauthorized' },
      );
    await settle();

    expect(navigate).toHaveBeenCalledWith(['/login'], { queryParams: { returnUrl: '/basket' } });
    expect(basket.count()).toBe(4);
  });
});
