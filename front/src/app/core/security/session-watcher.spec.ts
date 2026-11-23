import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { Subject } from 'rxjs';
import { adminGuard, customerGuard } from './guards';
import { SecurityService } from './security.service';
import { SessionWatcher } from './session-watcher';

@Component({ template: 'page' })
class Page {}

describe('SessionWatcher', () => {
  let expired: Subject<void>;
  let role: ReturnType<typeof signal<'ADMIN' | 'CUSTOMER' | null>>;
  let harness: RouterTestingHarness;

  beforeEach(async () => {
    expired = new Subject<void>();
    role = signal<'ADMIN' | 'CUSTOMER' | null>(null);
    TestBed.configureTestingModule({
      providers: [
        {
          provide: SecurityService,
          useValue: {
            sessionExpired: expired.asObservable(),
            isAuthenticated: () => role() !== null,
            isAdmin: () => role() === 'ADMIN',
            isCustomer: () => role() === 'CUSTOMER',
          },
        },
        provideRouter([
          { path: 'admin', canMatch: [adminGuard], children: [{ path: '**', component: Page }] },
          {
            path: 'my-orders',
            canActivate: [customerGuard],
            runGuardsAndResolvers: 'always',
            component: Page,
          },
          { path: '**', component: Page },
        ]),
      ],
    });
    harness = await RouterTestingHarness.create();
    TestBed.inject(SessionWatcher);
  });

  async function sessionExpiresOn(url: string, loggedInAs: 'ADMIN' | 'CUSTOMER'): Promise<string> {
    role.set(loggedInAs);
    await harness.navigateByUrl(url);
    role.set(null);

    expired.next();
    await harness.fixture.whenStable();

    return TestBed.inject(Router).url;
  }

  it('given an admin adding an item, when their session expires, then they are sent to the login and back afterwards', async () => {
    const url = await sessionExpiresOn('/admin/items/new', 'ADMIN');

    expect(url).toBe('/login?returnUrl=%2Fadmin%2Fitems%2Fnew');
  });

  it('given a customer on their orders, when their session expires, then they are sent to the login', async () => {
    const url = await sessionExpiresOn('/my-orders', 'CUSTOMER');

    expect(url).toBe('/login?returnUrl=%2Fmy-orders');
  });

  it('given a customer in the public catalog, when their session expires, then they stay where they are', async () => {
    const url = await sessionExpiresOn('/catalog', 'CUSTOMER');

    expect(url).toBe('/catalog');
  });
});
