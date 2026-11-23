import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
  TestRequest,
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { apiErrorInterceptor } from '@core/api/api-error.interceptor';
import { ApiProblem } from '@core/api/api-problem';
import type { CurrentUserDto } from '@core/api/eurder-api';
import { provideTestTranslations } from '@core/i18n/testing';
import { firstValueFrom } from 'rxjs';
import { authInterceptor } from './auth.interceptor';
import { SecurityService } from './security.service';

describe('authInterceptor', () => {
  let http: HttpClient;
  let backend: HttpTestingController;
  let security: SecurityService;

  const admin: CurrentUserDto = {
    id: '9d92d67e-6ef3-450d-85ac-11180c8826d2',
    role: 'ADMIN',
    email: 'admin@eurder.be',
    firstName: null,
    lastName: null,
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([apiErrorInterceptor, authInterceptor])),
        provideHttpClientTesting(),
        provideTestTranslations(),
        { provide: MatSnackBar, useValue: { open: vi.fn() } },
      ],
    });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
    security = TestBed.inject(SecurityService);
  });

  afterEach(() => backend.verify());

  function unauthorized(request: TestRequest, code: string): void {
    request.flush({ status: 401, code, params: {} }, { status: 401, statusText: 'Unauthorized' });
  }

  function call(url = '/api/customers'): Promise<unknown> {
    return firstValueFrom(http.get(url)).catch((error: unknown) => error);
  }

  it('given an expired access token, when a call fails, then the session is refreshed and the call replayed once', async () => {
    const customers = call();
    unauthorized(backend.expectOne('/api/customers'), 'error.security.access-token-expired');

    const refresh = backend.expectOne('/api/auth/refresh');
    refresh.flush(admin);
    backend.expectOne('/api/customers').flush([{ id: 'c1' }]);

    expect(refresh.request.method).toBe('POST');
    expect(await customers).toEqual([{ id: 'c1' }]);
    expect(security.isAdmin()).toBe(true);
  });

  it('given three calls failing at once with an expired token, when refreshing, then only one refresh is sent', async () => {
    const calls = [call('/api/customers'), call('/api/items'), call('/api/eurders/shipping-today')];
    for (const url of ['/api/customers', '/api/items', '/api/eurders/shipping-today']) {
      unauthorized(backend.expectOne(url), 'error.security.access-token-expired');
    }

    backend.expectOne('/api/auth/refresh').flush(admin);
    for (const url of ['/api/customers', '/api/items', '/api/eurders/shipping-today']) {
      backend.expectOne(url).flush({ url });
    }

    expect(await Promise.all(calls)).toEqual([
      { url: '/api/customers' },
      { url: '/api/items' },
      { url: '/api/eurders/shipping-today' },
    ]);
  });

  it('given a first refresh is over, when the token expires again later, then a new refresh is sent', async () => {
    const first = call();
    unauthorized(backend.expectOne('/api/customers'), 'error.security.access-token-expired');
    backend.expectOne('/api/auth/refresh').flush(admin);
    backend.expectOne('/api/customers').flush([]);
    await first;

    const second = call();
    unauthorized(backend.expectOne('/api/customers'), 'error.security.access-token-expired');
    backend.expectOne('/api/auth/refresh').flush(admin);
    backend.expectOne('/api/customers').flush([]);

    expect(await second).toEqual([]);
  });

  it('given a refused refresh, when the session cannot be renewed, then nobody is logged in and the caller gets the expiry', async () => {
    const customers = call();
    unauthorized(backend.expectOne('/api/customers'), 'error.security.access-token-expired');

    unauthorized(backend.expectOne('/api/auth/refresh'), 'error.security.invalid-refresh-token');

    expect(((await customers) as ApiProblem).code).toBe('error.security.access-token-expired');
    expect(security.isAuthenticated()).toBe(false);
  });

  it('given a replayed call failing again, when it answers 401, then no second refresh is sent', async () => {
    const customers = call();
    unauthorized(backend.expectOne('/api/customers'), 'error.security.access-token-expired');
    backend.expectOne('/api/auth/refresh').flush(admin);

    unauthorized(backend.expectOne('/api/customers'), 'error.security.access-token-expired');

    expect(((await customers) as ApiProblem).status).toBe(401);
    backend.expectNone('/api/auth/refresh');
  });

  it.each([
    ['an anonymous visitor', 401, 'error.security.authentication-required'],
    ['a refused access', 403, 'error.security.forbidden'],
  ])('given %s, when a call fails, then no refresh is sent', async (_, status, code) => {
    const customers = call();

    backend
      .expectOne('/api/customers')
      .flush({ status, code, params: {} }, { status, statusText: 'Error' });

    expect(((await customers) as ApiProblem).code).toBe(code);
    backend.expectNone('/api/auth/refresh');
  });

  it('given a user the front believes logged in, when the API says nobody is logged in, then the session is over', async () => {
    const me = firstValueFrom(security.loadCurrentUser());
    backend.expectOne('/api/auth/me').flush(admin);
    await me;
    const expired = vi.fn();
    security.sessionExpired.subscribe(expired);

    const customers = call();
    unauthorized(backend.expectOne('/api/customers'), 'error.security.authentication-required');
    await customers;

    expect(security.isAuthenticated()).toBe(false);
    expect(expired).toHaveBeenCalledOnce();
    backend.expectNone('/api/auth/refresh');
  });

  it('given a failed login, when it answers 401, then no refresh is sent', async () => {
    const login = firstValueFrom(http.post('/api/auth/login', {})).catch((error: unknown) => error);

    unauthorized(backend.expectOne('/api/auth/login'), 'error.security.access-token-expired');

    expect(((await login) as ApiProblem).status).toBe(401);
    backend.expectNone('/api/auth/refresh');
  });
});
