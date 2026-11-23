import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { apiErrorInterceptor } from '@core/api/api-error.interceptor';
import type { CurrentUserDto } from '@core/api/eurder-api';
import { provideTestTranslations } from '@core/i18n/testing';
import { firstValueFrom } from 'rxjs';
import { SecurityService } from './security.service';

describe('SecurityService', () => {
  let security: SecurityService;
  let backend: HttpTestingController;

  const jane: CurrentUserDto = {
    id: '3f2a9c1e-5b7d-4e8a-9c21-7d4e5f6a8b90',
    role: 'CUSTOMER',
    email: 'jane@doe.be',
    firstName: 'Jane',
    lastName: 'Doe',
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([apiErrorInterceptor])),
        provideHttpClientTesting(),
        provideTestTranslations(),
        { provide: MatSnackBar, useValue: { open: vi.fn() } },
      ],
    });
    security = TestBed.inject(SecurityService);
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    backend.verify();
    vi.useRealTimers();
  });

  it('given nothing loaded yet, when reading the state, then nobody is logged in', () => {
    const user = security.currentUser();

    expect(user).toBeNull();
    expect(security.isAuthenticated()).toBe(false);
  });

  it('given a logged-in customer, when loading the current user, then the service knows them', async () => {
    const loading = firstValueFrom(security.loadCurrentUser());
    backend.expectOne('/api/auth/me').flush(jane);

    await loading;

    expect(security.currentUser()?.displayName).toBe('Jane Doe');
    expect(security.isAuthenticated()).toBe(true);
    expect(security.isCustomer()).toBe(true);
    expect(security.isAdmin()).toBe(false);
  });

  it('given a logged-in admin, when loading the current user, then the service knows they are an admin', async () => {
    const loading = firstValueFrom(security.loadCurrentUser());
    backend
      .expectOne('/api/auth/me')
      .flush({ ...jane, role: 'ADMIN', email: 'admin@eurder.be', firstName: null, lastName: null });

    await loading;

    expect(security.isAdmin()).toBe(true);
    expect(security.isCustomer()).toBe(false);
  });

  it('given an anonymous visitor, when the API answers 401, then loading still succeeds with nobody logged in', async () => {
    const loading = firstValueFrom(security.loadCurrentUser());
    backend
      .expectOne('/api/auth/me')
      .flush(
        { status: 401, code: 'error.security.authentication-required', params: {} },
        { status: 401, statusText: 'Unauthorized' },
      );

    await expect(loading).resolves.toBeUndefined();
    expect(security.isAuthenticated()).toBe(false);
  });

  it('given the API is down, when loading the current user, then loading still succeeds with nobody logged in', async () => {
    const loading = firstValueFrom(security.loadCurrentUser());
    backend.expectOne('/api/auth/me').error(new ProgressEvent('error'));

    await expect(loading).resolves.toBeUndefined();
    expect(security.currentUser()).toBeNull();
  });

  it('given the right credentials, when logging in, then the API sets the cookies and the service knows the user', async () => {
    const login = firstValueFrom(security.login('jane@doe.be', 'secret123'));
    const request = backend.expectOne('/api/auth/login');

    request.flush(jane);

    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ email: 'jane@doe.be', password: 'secret123' });
    expect((await login).displayName).toBe('Jane Doe');
    expect(security.isCustomer()).toBe(true);
  });

  it('given wrong credentials, when logging in, then the caller gets the problem and nobody is logged in', async () => {
    const login = firstValueFrom(security.login('jane@doe.be', 'wrong')).catch(
      (error: unknown) => error,
    );

    backend
      .expectOne('/api/auth/login')
      .flush(
        { status: 401, code: 'error.security.invalid-credentials', params: {} },
        { status: 401, statusText: 'Unauthorized' },
      );

    expect(await login).toMatchObject({ code: 'error.security.invalid-credentials' });
    expect(security.isAuthenticated()).toBe(false);
  });

  it('given a logged-in user, when logging out, then the API revokes the session and nobody is logged in', async () => {
    const loading = firstValueFrom(security.loadCurrentUser());
    backend.expectOne('/api/auth/me').flush(jane);
    await loading;

    const logout = firstValueFrom(security.logout());
    const request = backend.expectOne('/api/auth/logout');
    request.flush(null, { status: 204, statusText: 'No Content' });
    await logout;

    expect(request.request.method).toBe('POST');
    expect(security.isAuthenticated()).toBe(false);
  });

  it('given the API is down, when logging out, then the user is logged out of the front anyway', async () => {
    const loading = firstValueFrom(security.loadCurrentUser());
    backend.expectOne('/api/auth/me').flush(jane);
    await loading;

    const logout = firstValueFrom(security.logout());
    backend.expectOne('/api/auth/logout').error(new ProgressEvent('error'));

    await expect(logout).resolves.toBeUndefined();
    expect(security.isAuthenticated()).toBe(false);
  });

  it('given a refused refresh, when renewing the session, then the service announces that the session expired', async () => {
    const loading = firstValueFrom(security.loadCurrentUser());
    backend.expectOne('/api/auth/me').flush(jane);
    await loading;
    const expired = vi.fn();
    security.sessionExpired.subscribe(expired);

    const refresh = firstValueFrom(security.refreshSession()).catch(() => undefined);
    backend
      .expectOne('/api/auth/refresh')
      .flush(
        { status: 401, code: 'error.security.invalid-refresh-token', params: {} },
        { status: 401, statusText: 'Unauthorized' },
      );
    await refresh;

    expect(expired).toHaveBeenCalledOnce();
    expect(security.isAuthenticated()).toBe(false);
  });

  it('given the API never answers, when 5 seconds pass, then loading gives up with nobody logged in', async () => {
    vi.useFakeTimers();
    const loading = firstValueFrom(security.loadCurrentUser());
    const request = backend.expectOne('/api/auth/me');

    await vi.advanceTimersByTimeAsync(5000);

    await expect(loading).resolves.toBeUndefined();
    expect(request.cancelled).toBe(true);
    expect(security.isAuthenticated()).toBe(false);
  });
});
