import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { ApiProblem } from '@core/api/api-problem';
import { provideTestTranslations } from '@core/i18n/testing';
import { CurrentUser } from '@core/security/current-user';
import { SecurityService } from '@core/security/security.service';
import { Observable, of, Subject, throwError } from 'rxjs';
import { Login } from './login';

describe('Login', () => {
  let fixture: ComponentFixture<Login>;
  let page: HTMLElement;
  let login: ReturnType<typeof vi.fn>;
  let navigate: ReturnType<typeof vi.spyOn>;

  const customer: CurrentUser = {
    id: 'c',
    role: 'CUSTOMER',
    email: 'jane@doe.be',
    displayName: 'Jane Doe',
  };
  const admin: CurrentUser = {
    id: 'a',
    role: 'ADMIN',
    email: 'admin@eurder.be',
    displayName: 'admin@eurder.be',
  };

  async function open(answer: Observable<CurrentUser>, returnUrl?: string): Promise<void> {
    login = vi.fn().mockReturnValue(answer);
    TestBed.configureTestingModule({
      providers: [
        provideTestTranslations(),
        provideRouter([]),
        { provide: SecurityService, useValue: { login } },
      ],
    });
    navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    fixture = TestBed.createComponent(Login);
    if (returnUrl) {
      fixture.componentRef.setInput('returnUrl', returnUrl);
    }
    await fixture.whenStable();
    page = fixture.nativeElement as HTMLElement;
  }

  function type(label: string, value: string): void {
    const input = page.querySelector<HTMLInputElement>(`input[autocomplete="${label}"]`)!;
    input.value = value;
    input.dispatchEvent(new Event('input'));
  }

  async function submit(): Promise<void> {
    page.querySelector<HTMLButtonElement>('button[type="submit"]')!.click();
    await fixture.whenStable();
  }

  async function logInWithTheRightCredentials(): Promise<void> {
    type('username', 'jane@doe.be');
    type('current-password', 'secret123');
    await submit();
  }

  it('given a customer who just registered, when the login opens, then it confirms that the account is ready', async () => {
    await open(of(customer));

    fixture.componentRef.setInput('registered', 'true');
    await fixture.whenStable();

    expect(page.querySelector('[role="status"]')?.textContent).toContain(
      'Your account is ready. You can log in now.',
    );
  });

  it('given a usual visit, when the login opens, then there is no confirmation', async () => {
    await open(of(customer));

    const status = page.querySelector('[role="status"]');

    expect(status).toBeNull();
  });

  it('given empty fields, when submitting, then both fields say they are required and nothing is sent', async () => {
    await open(of(customer));

    await submit();

    expect(page.querySelectorAll('mat-error')).toHaveLength(2);
    expect(page.textContent).toContain('This field is required.');
    expect(login).not.toHaveBeenCalled();
  });

  it.each([
    ['a customer', customer, '/catalog'],
    ['an admin', admin, '/admin'],
  ])(
    'given %s without a return address, when logging in, then they land on %s',
    async (_, user, landing) => {
      await open(of(user));

      await logInWithTheRightCredentials();

      expect(login).toHaveBeenCalledWith('jane@doe.be', 'secret123');
      expect(navigate).toHaveBeenCalledWith(landing);
    },
  );

  it('given a return address of the app, when logging in, then they go back there', async () => {
    await open(of(customer), '/my-orders');

    await logInWithTheRightCredentials();

    expect(navigate).toHaveBeenCalledWith('/my-orders');
  });

  it('given a return address to another site, when logging in, then it is ignored', async () => {
    await open(of(customer), 'https://evil.example');

    await logInWithTheRightCredentials();

    expect(navigate).toHaveBeenCalledWith('/catalog');
  });

  it('given wrong credentials, when logging in, then the page says so and stays', async () => {
    const wrong: ApiProblem = {
      status: 401,
      code: 'error.security.invalid-credentials',
      params: {},
      errors: [],
    };
    await open(throwError(() => wrong));

    await logInWithTheRightCredentials();

    expect(page.querySelector('[role="alert"]')?.textContent).toContain('Wrong email or password.');
    expect(navigate).not.toHaveBeenCalled();
  });

  it('given a login on its way, when waiting for a refusal, then the button waits too and is usable again afterwards', async () => {
    const answer = new Subject<CurrentUser>();
    await open(answer);
    const button = () => page.querySelector<HTMLButtonElement>('button[type="submit"]')!;

    await logInWithTheRightCredentials();

    expect(button().disabled).toBe(true);
    answer.error({
      status: 401,
      code: 'error.security.invalid-credentials',
      params: {},
      errors: [],
    });
    await vi.waitFor(async () => {
      await fixture.whenStable();
      expect(button().disabled).toBe(false);
    });
  });
});
