import { inject, Service } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';
import { SecurityService } from './security.service';

@Service()
export class SessionWatcher {
  constructor() {
    const router = inject(Router);
    inject(SecurityService)
      .sessionExpired.pipe(takeUntilDestroyed())
      .subscribe(() => router.navigateByUrl(router.url, { onSameUrlNavigation: 'reload' }));
  }
}
