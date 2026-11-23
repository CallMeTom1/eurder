import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { TranslateService } from '@ngx-translate/core';
import { catchError, throwError } from 'rxjs';
import { ApiProblem, toApiProblem } from './api-problem';
import { API } from './api-routes';

const SNACK_BAR_DURATION = 6000;

export const apiErrorInterceptor: HttpInterceptorFn = (request, next) => {
  if (!request.url.startsWith(`${API}/`)) {
    return next(request);
  }
  const snackBar = inject(MatSnackBar);
  const translate = inject(TranslateService);
  return next(request).pipe(
    catchError((error: unknown) => {
      if (!(error instanceof HttpErrorResponse)) {
        return throwError(() => error);
      }
      const problem = toApiProblem(error);
      if (isGlobalProblem(problem)) {
        snackBar.open(translate.instant(problem.code, problem.params), undefined, {
          duration: SNACK_BAR_DURATION,
        });
      }
      return throwError(() => problem);
    }),
  );
};

export function isGlobalProblem(problem: ApiProblem): boolean {
  return problem.status === 0 || problem.status === 403 || problem.status >= 500;
}
