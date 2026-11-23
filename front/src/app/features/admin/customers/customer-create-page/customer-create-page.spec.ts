import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MATERIAL_ANIMATIONS } from '@angular/material/core';
import { MatSelectHarness } from '@angular/material/select/testing';
import { provideRouter, Router } from '@angular/router';
import { provideApiTesting } from '@core/api/testing';
import { provideTestTranslations } from '@core/i18n/testing';
import { CustomerCreatePage } from './customer-create-page';

describe('CustomerCreatePage', () => {
  let fixture: ComponentFixture<CustomerCreatePage>;
  let page: HTMLElement;
  let backend: HttpTestingController;
  let navigate: ReturnType<typeof vi.spyOn>;

  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [CustomerCreatePage],
      providers: [
        provideTestTranslations(),
        provideRouter([]),
        provideApiTesting(),
        { provide: MATERIAL_ANIMATIONS, useValue: { animationsDisabled: true } },
      ],
    }).compileComponents();
    navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    backend = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(CustomerCreatePage);
    page = fixture.nativeElement as HTMLElement;
    await fixture.whenStable();
  });

  afterEach(() => backend.verify());

  function type(control: string, value: string): void {
    const input = page.querySelector<HTMLInputElement>(`[name$=".${control}"]`)!;
    input.value = value;
    input.dispatchEvent(new Event('input'));
  }

  function fillIn(): void {
    type('firstName', 'Jane');
    type('lastName', 'Doe');
    type('email', 'jane@doe.be');
    type('password', 'secret123');
    type('phoneNumber', '0470 12 34 56');
    type('street', 'Rue Haute');
    type('houseNumber', '1');
    type('postalCode', '1000');
    type('city', 'Bruxelles');
  }

  async function submit(): Promise<void> {
    page.querySelector<HTMLButtonElement>('button[type="submit"]')!.click();
    await fixture.whenStable();
  }

  async function settle(): Promise<void> {
    await new Promise((resolve) => setTimeout(resolve));
    await fixture.whenStable();
  }

  function errorOf(control: string): string | undefined {
    return page
      .querySelector(`[name$=".${control}"]`)
      ?.closest('mat-form-field')
      ?.querySelector('mat-error')
      ?.textContent?.trim();
  }

  it('given the form, when it opens, then it asks for every detail and an initial password, Belgium chosen', async () => {
    const country = await TestbedHarnessEnvironment.loader(fixture).getHarness(MatSelectHarness);

    expect(page.querySelector('h1')?.textContent?.trim()).toBe('New customer');
    expect(page.querySelector('[name$=".password"]')).not.toBeNull();
    expect(await country.getValueText()).toBe('Belgium');
  });

  it('given an empty form, when creating, then the required fields say so and nothing is sent', async () => {
    await submit();

    expect(errorOf('firstName')).toBe('This field is required.');
    expect(errorOf('password')).toBe('This field is required.');
    backend.expectNone('/api/customers');
  });

  it('given a complete form, when creating, then the customer is sent and its detail opens', async () => {
    fillIn();

    await submit();
    const request = backend.expectOne({ method: 'POST', url: '/api/customers' });
    request.flush({ id: 'c1' });
    await settle();

    expect(request.request.body).toMatchObject({
      firstName: 'Jane',
      email: 'jane@doe.be',
      password: 'secret123',
    });
    expect(navigate).toHaveBeenCalledWith(['/admin/customers', 'c1']);
  });

  it('given an email already used, when creating, then the email field says so', async () => {
    fillIn();

    await submit();
    backend.expectOne('/api/customers').flush(
      {
        status: 409,
        code: 'error.security.email-already-used',
        params: { email: 'jane@doe.be' },
      },
      { status: 409, statusText: 'Conflict' },
    );
    await settle();

    expect(errorOf('email')).toBe('An account already exists for jane@doe.be.');
  });

  it('given the form, when giving up, then Cancel leads back to the customers', () => {
    const cancel = [...page.querySelectorAll('a')].find(
      (link) => link.textContent?.trim() === 'Cancel',
    );

    expect(cancel?.getAttribute('href')).toBe('/admin/customers');
  });
});
