import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { API, ApiRoutes } from '@core/api/api-routes';
import { catchError, switchMap, throwError } from 'rxjs';
import { SecurityService } from './security.service';

const TOKEN_ENDPOINTS: readonly string[] = [
  ApiRoutes.auth.login,
  ApiRoutes.auth.refresh,
  ApiRoutes.auth.logout,
];

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  if (!request.url.startsWith(`${API}/`) || TOKEN_ENDPOINTS.includes(request.url)) {
    return next(request);
  }
  const security = inject(SecurityService);
  return next(request).pipe(
    catchError((error: unknown) => {
      if (isMissingSession(error)) {
        security.sessionLost();
      }
      if (!isExpiredAccessToken(error)) {
        return throwError(() => error);
      }
      return security.refreshSession().pipe(
        catchError(() => throwError(() => error)),
        switchMap(() => next(request)),
      );
    }),
  );
};

function isExpiredAccessToken(error: unknown): boolean {
  return isUnauthorizedWith(error, 'error.security.access-token-expired');
}

function isMissingSession(error: unknown): boolean {
  return isUnauthorizedWith(error, 'error.security.authentication-required');
}

function isUnauthorizedWith(error: unknown, code: string): boolean {
  return (
    error instanceof HttpErrorResponse &&
    error.status === 401 &&
    (error.error as { code?: unknown } | null)?.code === code
  );
}
