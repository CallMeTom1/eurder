import {
  HttpClient,
  HttpErrorResponse,
  provideHttpClient,
  withInterceptors,
} from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { provideTestTranslations } from '@core/i18n/testing';
import { firstValueFrom } from 'rxjs';
import { apiErrorInterceptor } from './api-error.interceptor';
import { ApiProblem } from './api-problem';

describe('apiErrorInterceptor', () => {
  let http: HttpClient;
  let backend: HttpTestingController;
  let snackBar: { open: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    snackBar = { open: vi.fn() };
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([apiErrorInterceptor])),
        provideHttpClientTesting(),
        provideTestTranslations(),
        { provide: MatSnackBar, useValue: snackBar },
      ],
    });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  async function failingCall(
    answer: (request: ReturnType<HttpTestingController['expectOne']>) => void,
  ) {
    const call = firstValueFrom(http.get('/api/catalog/3f2a')).catch((error: unknown) => error);
    answer(backend.expectOne('/api/catalog/3f2a'));
    return call;
  }

  it('given an error the page can handle, when the call fails, then the page gets an ApiProblem and no snackbar shows', async () => {
    const error = await failingCall((request) =>
      request.flush(
        { status: 404, code: 'error.item.not-found', params: { id: '3f2a' } },
        { status: 404, statusText: 'Not Found' },
      ),
    );

    expect(error).toEqual<ApiProblem>({
      status: 404,
      code: 'error.item.not-found',
      params: { id: '3f2a' },
      errors: [],
    });
    expect(snackBar.open).not.toHaveBeenCalled();
  });

  it('given an expired session, when the call fails with 401, then no snackbar shows', async () => {
    await failingCall((request) =>
      request.flush(
        { status: 401, code: 'error.security.access-token-expired', params: {} },
        { status: 401, statusText: 'Unauthorized' },
      ),
    );

    expect(snackBar.open).not.toHaveBeenCalled();
  });

  it('given a server error, when the call fails, then a snackbar tells it in the language of the page', async () => {
    const error = await failingCall((request) =>
      request.flush(
        { status: 500, code: 'error.common.unexpected', params: {} },
        { status: 500, statusText: 'Error' },
      ),
    );

    expect((error as ApiProblem).code).toBe('error.common.unexpected');
    expect(snackBar.open).toHaveBeenCalledWith(
      'Something went wrong. Please try again later.',
      undefined,
      {
        duration: 6000,
      },
    );
  });

  it('given a refused access, when the call fails with 403, then a snackbar tells it', async () => {
    await failingCall((request) =>
      request.flush(
        { status: 403, code: 'error.security.invalid-csrf-token', params: {} },
        { status: 403, statusText: 'Forbidden' },
      ),
    );

    expect(snackBar.open).toHaveBeenCalledWith(
      'Your session needs to be refreshed. Reload the page and try again.',
      undefined,
      { duration: 6000 },
    );
  });

  it('given a call outside the API, when it fails, then the error is left as it is and no snackbar shows', async () => {
    const call = firstValueFrom(http.get('/i18n/fr.json')).catch((error: unknown) => error);
    backend.expectOne('/i18n/fr.json').flush('Not Found', { status: 404, statusText: 'Not Found' });

    const error = await call;

    expect(error).toBeInstanceOf(HttpErrorResponse);
    expect(snackBar.open).not.toHaveBeenCalled();
  });

  it('given the network is down, when the call fails, then the problem is unexpected and a snackbar tells it', async () => {
    const error = await failingCall((request) => request.error(new ProgressEvent('error')));

    expect(error).toEqual<ApiProblem>({
      status: 0,
      code: 'error.common.unexpected',
      params: {},
      errors: [],
    });
    expect(snackBar.open).toHaveBeenCalledOnce();
  });
});
