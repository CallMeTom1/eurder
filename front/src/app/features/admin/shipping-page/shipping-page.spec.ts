import { HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import type { ShippingEurderDto } from '@core/api/eurder-api';
import { provideApiTesting } from '@core/api/testing';
import { provideTestTranslations } from '@core/i18n/testing';
import { ShippingPage } from './shipping-page';

describe('ShippingPage', () => {
  let fixture: ComponentFixture<ShippingPage>;
  let page: HTMLElement;
  let backend: HttpTestingController;

  const shipment: ShippingEurderDto = {
    eurderId: '3f2a9c1e-5b7d-4e8a-9c21-7d4e5f6a8b90',
    customerName: 'Jane Doe',
    address: {
      street: 'Rue Haute',
      houseNumber: '1',
      postalCode: '1000',
      city: 'Bruxelles',
      country: 'BELGIUM',
    },
    itemGroups: [
      { itemName: 'Chair', itemState: 'PRISTINE', amount: 2 },
      { itemName: 'Lamp', itemState: 'SLIGHTLY_DAMAGED', amount: 1 },
    ],
  };

  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [ShippingPage],
      providers: [provideTestTranslations(), provideRouter([]), provideApiTesting()],
    }).compileComponents();
    backend = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(ShippingPage);
    page = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
  });

  afterEach(() => backend.verify());

  async function answer(shipments: ShippingEurderDto[]): Promise<void> {
    backend.expectOne({ method: 'GET', url: '/api/eurders/shipping-today' }).flush(shipments);
    await fixture.whenStable();
  }

  it('given something to ship today, when the page opens, then each shipment shows the customer, the address and the items', async () => {
    await answer([shipment]);

    const card = page.querySelector('[data-testid="shipment"]');

    expect(page.querySelector('h1')?.textContent?.trim()).toBe('Shipping today');
    expect(card?.textContent).toContain('Jane Doe');
    expect(card?.textContent).toContain('Rue Haute 1, 1000 Bruxelles, Belgium');
    expect(card?.textContent).toContain('Chair');
    expect(card?.textContent).toContain('Slightly damaged');
    expect(card?.textContent).toContain('3f2a9c1e');
  });

  it('given nothing to ship, when the page opens, then it says so', async () => {
    await answer([]);

    const text = page.textContent;

    expect(text).toContain('Nothing ships today.');
  });
});
