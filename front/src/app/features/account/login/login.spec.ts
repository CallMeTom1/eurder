import { HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import type { CurrentUserDto } from '@core/api/eurder-api';
import { provideApiTesting } from '@core/api/testing';
import { provideTestTranslations } from '@core/i18n/testing';
import { Login } from './login';

describe('Login', () => {
  let fixture: ComponentFixture<Login>;
  let page: HTMLElement;
  let backend: HttpTestingController;
  let navigate: ReturnType<typeof vi.spyOn>;

  const customer: CurrentUserDto = {
    id: 'c',
    role: 'CUSTOMER',
    email: 'jane@doe.be',
    firstName: 'Jane',
    lastName: 'Doe',
  };
  const admin: CurrentUserDto = {
    id: 'a',
    role: 'ADMIN',
    email: 'admin@eurder.be',
    firstName: null,
    lastName: null,
  };
  const wrongCredentials = {
    status: 401,
    code: 'error.security.invalid-credentials',
    params: {},
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Login],
      providers: [provideTestTranslations(), provideRouter([]), provideApiTesting()],
    }).compileComponents();
    navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    backend = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(Login);
    page = fixture.nativeElement as HTMLElement;
  });

  afterEach(() => backend.verify());

  async function open(returnUrl?: string): Promise<void> {
    if (returnUrl) {
      fixture.componentRef.setInput('returnUrl', returnUrl);
    }
    await fixture.whenStable();
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

  async function answer(user: CurrentUserDto): Promise<void> {
    backend.expectOne({ method: 'POST', url: '/api/auth/login' }).flush(user);
    await fixture.whenStable();
  }

  async function refuse(): Promise<void> {
    backend
      .expectOne('/api/auth/login')
      .flush(wrongCredentials, { status: 401, statusText: 'Unauthorized' });
    await new Promise((resolve) => setTimeout(resolve));
    await fixture.whenStable();
  }

  it('given a customer who just registered, when the login opens, then it confirms that the account is ready', async () => {
    await open();

    fixture.componentRef.setInput('registered', 'true');
    await fixture.whenStable();

    expect(page.querySelector('[role="status"]')?.textContent).toContain(
      'Your account is ready. You can log in now.',
    );
  });

  it('given a usual visit, when the login opens, then there is no confirmation', async () => {
    await open();

    const status = page.querySelector('[role="status"]');

    expect(status).toBeNull();
  });

  it('given empty fields, when submitting, then both fields say they are required and nothing is sent', async () => {
    await open();

    await submit();

    expect(page.querySelectorAll('mat-error')).toHaveLength(2);
    expect(page.textContent).toContain('This field is required.');
    backend.expectNone('/api/auth/login');
  });

  it.each([
    { who: 'a customer', user: customer, landing: '/catalog' },
    { who: 'an admin', user: admin, landing: '/admin' },
  ])(
    'given $who without a return address, when logging in, then they land on $landing',
    async ({ user, landing }) => {
      await open();

      await logInWithTheRightCredentials();
      const request = backend.expectOne('/api/auth/login');
      expect(request.request.body).toEqual({ email: 'jane@doe.be', password: 'secret123' });
      request.flush(user);
      await fixture.whenStable();

      expect(navigate).toHaveBeenCalledWith(landing);
    },
  );

  it('given a return address of the app, when logging in, then they go back there', async () => {
    await open('/my-orders');

    await logInWithTheRightCredentials();
    await answer(customer);

    expect(navigate).toHaveBeenCalledWith('/my-orders');
  });

  it('given a return address to another site, when logging in, then it is ignored', async () => {
    await open('https://evil.example');

    await logInWithTheRightCredentials();
    await answer(customer);

    expect(navigate).toHaveBeenCalledWith('/catalog');
  });

  it('given wrong credentials, when logging in, then the page says so and stays', async () => {
    await open();

    await logInWithTheRightCredentials();
    await refuse();

    expect(page.querySelector('[role="alert"]')?.textContent).toContain('Wrong email or password.');
    expect(navigate).not.toHaveBeenCalled();
  });

  it('given a login on its way, when waiting for a refusal, then the button waits too and is usable again afterwards', async () => {
    await open();
    const button = () => page.querySelector<HTMLButtonElement>('button[type="submit"]')!;

    await logInWithTheRightCredentials();

    expect(button().disabled).toBe(true);
    await refuse();
    expect(button().disabled).toBe(false);
  });
});
