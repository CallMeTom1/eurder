import { HttpClient } from '@angular/common/http';
import { computed, inject, Service, signal } from '@angular/core';
import type { CurrentUserDto } from '@core/api/eurder-api';
import { ApiRoutes } from '@core/api/api-routes';
import { catchError, map, Observable, of, timeout } from 'rxjs';
import { CurrentUser, toCurrentUser } from './current-user';

const STARTUP_TIMEOUT_MS = 5000;

@Service()
export class SecurityService {
  private readonly http = inject(HttpClient);
  private readonly user = signal<CurrentUser | null>(null);

  readonly currentUser = this.user.asReadonly();
  readonly isAuthenticated = computed(() => this.user() !== null);
  readonly isAdmin = computed(() => this.user()?.role === 'ADMIN');
  readonly isCustomer = computed(() => this.user()?.role === 'CUSTOMER');

  loadCurrentUser(): Observable<void> {
    return this.http.get<CurrentUserDto>(ApiRoutes.auth.me).pipe(
      timeout(STARTUP_TIMEOUT_MS),
      map((dto) => this.user.set(toCurrentUser(dto))),
      catchError(() => {
        this.user.set(null);
        return of(undefined);
      }),
    );
  }
}
