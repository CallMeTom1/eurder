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
