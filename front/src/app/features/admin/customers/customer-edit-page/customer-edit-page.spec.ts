import { HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MATERIAL_ANIMATIONS } from '@angular/material/core';
import { provideRouter, Router } from '@angular/router';
import type { CustomerDto } from '@core/api/eurder-api';
import { provideApiTesting } from '@core/api/testing';
import { provideTestTranslations } from '@core/i18n/testing';
import { CustomerEditPage } from './customer-edit-page';

describe('CustomerEditPage', () => {
  let fixture: ComponentFixture<CustomerEditPage>;
  let page: HTMLElement;
  let backend: HttpTestingController;
  let navigate: ReturnType<typeof vi.spyOn>;

  const jane: CustomerDto = {
    id: 'c1',
    firstName: 'Jane',
    lastName: 'Doe',
    email: 'jane@doe.be',
    phoneNumber: '0470 12 34 56',
    address: {
      street: 'Rue Haute',
      houseNumber: '1',
      postalCode: '1000',
      city: 'Bruxelles',
      country: 'BELGIUM',
    },
  };

  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [CustomerEditPage],
      providers: [
        provideTestTranslations(),
        provideRouter([]),
        provideApiTesting(),
        { provide: MATERIAL_ANIMATIONS, useValue: { animationsDisabled: true } },
      ],
    }).compileComponents();
    navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    backend = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(CustomerEditPage);
    fixture.componentRef.setInput('id', 'c1');
    page = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
    backend.expectOne({ method: 'GET', url: '/api/customers/c1' }).flush(jane);
    await fixture.whenStable();
  });

  afterEach(() => backend.verify());

  function input(control: string): HTMLInputElement {
    return page.querySelector<HTMLInputElement>(`[name$=".${control}"]`)!;
  }

  function type(control: string, value: string): void {
    input(control).value = value;
    input(control).dispatchEvent(new Event('input'));
  }

  async function update(): Promise<void> {
    page.querySelector<HTMLButtonElement>('button[type="submit"]')!.click();
    await fixture.whenStable();
  }

  async function settle(): Promise<void> {
    await new Promise((resolve) => setTimeout(resolve));
    await fixture.whenStable();
  }

  function errorOf(control: string): string | undefined {
    return input(control)
      ?.closest('mat-form-field')
      ?.querySelector('mat-error')
      ?.textContent?.trim();
  }

  it('given a customer, when the edit page opens, then its id heads the page, the fields hold its details and there is no password', () => {
    const header = page.querySelector('app-page-header');

    expect(header?.querySelector('h1')?.textContent?.trim()).toBe('Edit customer');
    expect(header?.textContent).toContain('c1');
    expect(input('firstName').value).toBe('Jane');
    expect(input('email').value).toBe('jane@doe.be');
    expect(input('city').value).toBe('Bruxelles');
    expect(page.querySelector('[name$=".password"]')).toBeNull();
  });

  it('given new details, when updating, then they are sent and the detail of the customer opens', async () => {
    type('firstName', 'Janet');
    type('city', 'Liège');

    await update();
    const request = backend.expectOne({ method: 'PUT', url: '/api/customers/c1' });
    request.flush({ ...jane, firstName: 'Janet' });
    await settle();

    expect(request.request.body).toEqual({
      firstName: 'Janet',
      lastName: 'Doe',
      email: 'jane@doe.be',
      phoneNumber: '0470 12 34 56',
      address: { ...jane.address, city: 'Liège' },
    });
    expect(navigate).toHaveBeenCalledWith(['/admin/customers', 'c1']);
  });

  it('given a blank last name, when updating, then the same rules apply and nothing is sent', async () => {
    type('lastName', '');

    await update();

    expect(errorOf('lastName')).toBe('This field is required.');
    backend.expectNone('/api/customers/c1');
  });

  it('given the email of another account, when updating, then the email field says so', async () => {
    type('email', 'john@doe.be');

    await update();
    backend.expectOne('/api/customers/c1').flush(
      {
        status: 409,
        code: 'error.security.email-already-used',
        params: { email: 'john@doe.be' },
      },
      { status: 409, statusText: 'Conflict' },
    );
    await settle();

    expect(errorOf('email')).toBe('An account already exists for john@doe.be.');
    expect(navigate).not.toHaveBeenCalled();
  });

  it('given the form, when giving up, then Cancel leads back to the customer', () => {
    const cancel = [...page.querySelectorAll('a')].find(
      (link) => link.textContent?.trim() === 'Cancel',
    );

    expect(cancel?.getAttribute('href')).toBe('/admin/customers/c1');
  });
});
