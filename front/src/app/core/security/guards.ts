import { inject } from '@angular/core';
import { CanActivateFn, CanMatchFn, Router, UrlTree } from '@angular/router';
import { SecurityService } from './security.service';

export const customerGuard: CanActivateFn = (_, state) =>
  allowed(inject(SecurityService).isCustomer(), state.url);

export const adminGuard: CanMatchFn = (_, segments) =>
  allowed(
    inject(SecurityService).isAdmin(),
    `/${segments.map((segment) => segment.path).join('/')}`,
  );

export const guestGuard: CanActivateFn = () =>
  inject(SecurityService).isAuthenticated() ? inject(Router).createUrlTree(['/']) : true;

function allowed(hasRole: boolean, requestedUrl: string): true | UrlTree {
  if (hasRole) {
    return true;
  }
  const router = inject(Router);
  return inject(SecurityService).isAuthenticated()
    ? router.createUrlTree(['/'])
    : router.createUrlTree(['/login'], { queryParams: { returnUrl: requestedUrl } });
}
