import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MATERIAL_ANIMATIONS } from '@angular/material/core';
import { MatSelectHarness } from '@angular/material/select/testing';
import { provideRouter, Router } from '@angular/router';
import { ApiProblem } from '@core/api/api-problem';
import type { CustomerDto } from '@core/api/eurder-api';
import { provideTestTranslations } from '@core/i18n/testing';
import { Observable, of, Subject, throwError } from 'rxjs';
import { AccountService } from '../account.service';
import { Register } from './register';

describe('Register', () => {
  let fixture: ComponentFixture<Register>;
  let page: HTMLElement;
  let addCustomer: ReturnType<typeof vi.fn>;
  let navigate: ReturnType<typeof vi.spyOn>;

  async function open(answer: Observable<CustomerDto>): Promise<void> {
    addCustomer = vi.fn().mockReturnValue(answer);
    TestBed.configureTestingModule({
      providers: [
        provideTestTranslations(),
        provideRouter([]),
        { provide: AccountService, useValue: { addCustomer } },
        { provide: MATERIAL_ANIMATIONS, useValue: { animationsDisabled: true } },
      ],
    });
    navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    fixture = TestBed.createComponent(Register);
    await fixture.whenStable();
    page = fixture.nativeElement as HTMLElement;
  }

  function type(control: string, value: string): void {
    const input = page.querySelector<HTMLInputElement>(`[formcontrolname="${control}"]`)!;
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
      .querySelector(`[formcontrolname="${control}"]`)
      ?.closest('mat-form-field')
      ?.querySelector('mat-error')
      ?.textContent?.trim();
  }

  it('given an empty form, when submitting, then every required field says so and nothing is sent', async () => {
    await open(of({} as CustomerDto));

    await submit();

    expect(errorOf('firstName')).toBe('This field is required.');
    expect(errorOf('city')).toBe('This field is required.');
    expect(addCustomer).not.toHaveBeenCalled();
  });

  it('given a short password and a wrong phone number, when submitting, then the same rules as the API apply', async () => {
    await open(of({} as CustomerDto));
    fillIn();
    type('password', 'short');
    type('phoneNumber', 'call me');

    await submit();

    expect(errorOf('password')).toBe('At least 8 characters.');
    expect(errorOf('phoneNumber')).toBe('The format is not valid.');
    expect(addCustomer).not.toHaveBeenCalled();
  });

  it('given a complete form in France, when submitting, then the customer is registered and asked to log in', async () => {
    await open(of({} as CustomerDto));
    fillIn();
    const country = await TestbedHarnessEnvironment.loader(fixture).getHarness(MatSelectHarness);
    await country.clickOptions({ text: 'France' });

    await submit();

    expect(addCustomer).toHaveBeenCalledWith({
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
    await open(of({} as CustomerDto));

    const country = await TestbedHarnessEnvironment.loader(fixture).getHarness(MatSelectHarness);

    expect(await country.getValueText()).toBe('Belgium');
  });

  it('given an email already used, when submitting, then the email field says so', async () => {
    const used: ApiProblem = {
      status: 409,
      code: 'error.security.email-already-used',
      params: { email: 'jane@doe.be' },
      errors: [],
    };
    await open(throwError(() => used));
    fillIn();

    await submit();

    expect(errorOf('email')).toBe('An account already exists for jane@doe.be.');
    expect(navigate).not.toHaveBeenCalled();
  });

  it('given a field refused by the API, when submitting, then that field shows the reason of the API', async () => {
    const refused: ApiProblem = {
      status: 400,
      code: 'error.common.validation-failed',
      params: {},
      errors: [{ field: 'address.city', code: 'validation.too-long', params: { max: 255 } }],
    };
    await open(throwError(() => refused));
    fillIn();

    await submit();

    expect(errorOf('city')).toBe('At most 255 characters.');
  });

  it('given a registration on its way, when waiting, then the button cannot be pressed again', async () => {
    await open(new Subject<CustomerDto>());
    fillIn();

    await submit();

    expect(page.querySelector<HTMLButtonElement>('button[type="submit"]')?.disabled).toBe(true);
  });
});
