import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { Role } from './current-user';
import { adminGuard, customerGuard, guestGuard } from './guards';
import { SecurityService } from './security.service';

@Component({ template: 'page' })
class Page {}

describe('Guards', () => {
  async function as(role: Role | null): Promise<RouterTestingHarness> {
    TestBed.configureTestingModule({
      providers: [
        {
          provide: SecurityService,
          useValue: {
            isAuthenticated: signal(role !== null),
            isAdmin: signal(role === 'ADMIN'),
            isCustomer: signal(role === 'CUSTOMER'),
          },
        },
        provideRouter([
          { path: 'admin', canMatch: [adminGuard], children: [{ path: '**', component: Page }] },
          { path: 'my-orders', canActivate: [customerGuard], component: Page },
          { path: 'login', canActivate: [guestGuard], component: Page },
          { path: '**', component: Page },
        ]),
      ],
    });
    return RouterTestingHarness.create();
  }

  async function landingOf(harness: RouterTestingHarness, url: string): Promise<string> {
    await harness.navigateByUrl(url);
    return TestBed.inject(Router).url;
  }

  describe('customerGuard', () => {
    it.each<[string, Role | null, string]>([
      ['an anonymous visitor', null, '/login?returnUrl=%2Fmy-orders'],
      ['a customer', 'CUSTOMER', '/my-orders'],
      ['an admin', 'ADMIN', '/'],
    ])('given %s, when opening my orders, then they land on %s', async (_, role, landing) => {
      const harness = await as(role);

      const url = await landingOf(harness, '/my-orders');

      expect(url).toBe(landing);
    });
  });

  describe('adminGuard', () => {
    it.each<[string, Role | null, string]>([
      ['an anonymous visitor', null, '/login?returnUrl=%2Fadmin%2Fcustomers'],
      ['a customer', 'CUSTOMER', '/'],
      ['an admin', 'ADMIN', '/admin/customers'],
    ])('given %s, when opening the back office, then they land on %s', async (_, role, landing) => {
      const harness = await as(role);

      const url = await landingOf(harness, '/admin/customers');

      expect(url).toBe(landing);
    });
  });

  describe('guestGuard', () => {
    it.each<[string, Role | null, string]>([
      ['an anonymous visitor', null, '/login'],
      ['a customer', 'CUSTOMER', '/'],
      ['an admin', 'ADMIN', '/'],
    ])('given %s, when opening the login, then they land on %s', async (_, role, landing) => {
      const harness = await as(role);

      const url = await landingOf(harness, '/login');

      expect(url).toBe(landing);
    });
  });
});
