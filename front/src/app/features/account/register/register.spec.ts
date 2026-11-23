import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MATERIAL_ANIMATIONS } from '@angular/material/core';
import { MatSelectHarness } from '@angular/material/select/testing';
import { provideRouter, Router } from '@angular/router';
import { provideApiTesting } from '@core/api/testing';
import { provideTestTranslations } from '@core/i18n/testing';
import { Register } from './register';

describe('Register', () => {
  let fixture: ComponentFixture<Register>;
  let page: HTMLElement;
  let backend: HttpTestingController;
  let navigate: ReturnType<typeof vi.spyOn>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Register],
      providers: [
        provideTestTranslations(),
        provideRouter([]),
        provideApiTesting(),
        { provide: MATERIAL_ANIMATIONS, useValue: { animationsDisabled: true } },
      ],
    }).compileComponents();
    navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    backend = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(Register);
    await fixture.whenStable();
    page = fixture.nativeElement as HTMLElement;
  });

  afterEach(() => backend.verify());

  async function refuse(status: number, body: object): Promise<void> {
    backend.expectOne('/api/customers').flush(body, { status, statusText: 'Refused' });
    await new Promise((resolve) => setTimeout(resolve));
    await fixture.whenStable();
  }

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

  function errorOf(control: string): string | undefined {
    return page
      .querySelector(`[name$=".${control}"]`)
      ?.closest('mat-form-field')
      ?.querySelector('mat-error')
      ?.textContent?.trim();
  }

  it('given an empty form, when submitting, then every required field says so and nothing is sent', async () => {
    await submit();

    expect(errorOf('firstName')).toBe('This field is required.');
    expect(errorOf('city')).toBe('This field is required.');
    backend.expectNone('/api/customers');
  });

  it('given a short password and a wrong phone number, when submitting, then the same rules as the API apply', async () => {
    fillIn();
    type('password', 'short');
    type('phoneNumber', 'call me');

    await submit();

    expect(errorOf('password')).toBe('At least 8 characters.');
    expect(errorOf('phoneNumber')).toBe('The format is not valid.');
    backend.expectNone('/api/customers');
  });

  it('given a complete form in France, when submitting, then the customer is registered and asked to log in', async () => {
    fillIn();
    const country = await TestbedHarnessEnvironment.loader(fixture).getHarness(MatSelectHarness);
    await country.clickOptions({ text: 'France' });

    await submit();
    const request = backend.expectOne({ method: 'POST', url: '/api/customers' });
    request.flush({});
    await fixture.whenStable();

    expect(request.request.body).toEqual({
      firstName: 'Jane',
      lastName: 'Doe',
      email: 'jane@doe.be',
      password: 'secret123',
      phoneNumber: '0470 12 34 56',
      address: {
        street: 'Rue Haute',
        houseNumber: '1',
        postalCode: '1000',
        city: 'Bruxelles',
        country: 'FRANCE',
      },
    });
    expect(navigate).toHaveBeenCalledWith(['/login'], { queryParams: { registered: true } });
  });

  it('given nothing chosen, when reading the country, then it is Belgium', async () => {
    const country = await TestbedHarnessEnvironment.loader(fixture).getHarness(MatSelectHarness);

    expect(await country.getValueText()).toBe('Belgium');
  });

  it('given an email already used, when submitting, then the email field says so', async () => {
    fillIn();

    await submit();
    await refuse(409, {
      status: 409,
      code: 'error.security.email-already-used',
      params: { email: 'jane@doe.be' },
    });

    expect(errorOf('email')).toBe('An account already exists for jane@doe.be.');
    expect(navigate).not.toHaveBeenCalled();
  });

  it('given a field refused by the API, when submitting, then that field shows the reason of the API', async () => {
    fillIn();

    await submit();
    await refuse(400, {
      status: 400,
      code: 'error.common.validation-failed',
      params: {},
      errors: [{ field: 'address.city', code: 'validation.too-long', params: { max: 255 } }],
    });

    expect(errorOf('city')).toBe('At most 255 characters.');
  });

  it('given a registration on its way, when waiting, then the button cannot be pressed again', async () => {
    fillIn();

    await submit();

    expect(page.querySelector<HTMLButtonElement>('button[type="submit"]')?.disabled).toBe(true);
    backend.expectOne('/api/customers');
  });
});
