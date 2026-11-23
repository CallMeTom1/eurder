import { HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import type { CurrentUserDto } from '@core/api/eurder-api';
import { provideApiTesting } from '@core/api/testing';
import { provideTestTranslations } from '@core/i18n/testing';
import { SecurityService } from '@core/security/security.service';
import { BasketService } from '@features/basket/basket.service';
import { firstValueFrom } from 'rxjs';
import { FrontOfficeLayout } from './front-office-layout';

describe('FrontOfficeLayout', () => {
  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [FrontOfficeLayout],
      providers: [provideTestTranslations(), provideRouter([]), provideApiTesting()],
    }).compileComponents();
  });

  afterEach(() => TestBed.inject(HttpTestingController).verify());

  async function loggedInAs(user: CurrentUserDto): Promise<void> {
    const loaded = firstValueFrom(TestBed.inject(SecurityService).loadCurrentUser());
    TestBed.inject(HttpTestingController).expectOne('/api/auth/me').flush(user);
    await loaded;
  }

  async function layout(): Promise<HTMLElement> {
    const fixture = TestBed.createComponent(FrontOfficeLayout);
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  function linksOf(element: Element | null): [string | undefined, string | null][] {
    return [...(element?.querySelectorAll('a') ?? [])].map((link) => [
      link.textContent?.trim(),
      link.getAttribute('href'),
    ]);
  }

  it('given the shop, when rendered, then its main navigation leads to the catalog and the basket', async () => {
    const page = await layout();

    const navigation = page.querySelector('nav[aria-label="Main"]');

    expect(linksOf(navigation)).toEqual([
      ['Catalog', '/catalog'],
      ['Basket', '/basket'],
    ]);
  });

  it('given the shop, when rendered, then the logo leads home and the header offers to log in', async () => {
    const page = await layout();

    const header = page.querySelector('header');

    expect(header?.querySelector('a[href="/"] img')?.getAttribute('alt')).toBe('Örder');
    expect(linksOf(header)).toContainEqual(['Log in', '/login']);
    expect(header?.querySelector('app-language-switcher')).not.toBeNull();
    expect(header?.querySelector('app-theme-toggle')).not.toBeNull();
  });

  it('given the shop, when rendered, then the pages go in its main area and the footer shows the logo and the year', async () => {
    const page = await layout();

    const main = page.querySelector('main');

    expect(main?.querySelector('router-outlet')).not.toBeNull();
    expect(page.querySelector('footer')?.textContent).toContain(
      `© ${new Date().getFullYear()} Örder`,
    );
    expect(page.querySelector('footer img')?.getAttribute('alt')).toBe('Örder');
  });

  it('given items in the basket, when rendered, then the basket link shows how many', async () => {
    const basket = TestBed.inject(BasketService);
    basket.add({ id: 'i1', name: 'Chair', priceInCents: 4999 });
    basket.add({ id: 'i1', name: 'Chair', priceInCents: 4999 });
    basket.add({ id: 'i2', name: 'Lamp', priceInCents: 2900 });

    const page = await layout();

    const link = page.querySelector('nav a[href="/basket"]');
    expect(link?.getAttribute('aria-label')).toBe('Basket, 3 items');
    expect(link?.querySelector('[data-testid="basket-count"]')?.textContent?.trim()).toBe('3');
  });

  it('given an admin, when rendered, then there is no basket since an admin cannot order', async () => {
    await loggedInAs({
      id: 'a',
      role: 'ADMIN',
      email: 'admin@eurder.be',
      firstName: null,
      lastName: null,
    });

    const page = await layout();

    expect(linksOf(page.querySelector('nav[aria-label="Main"]'))).toEqual([
      ['Catalog', '/catalog'],
    ]);
  });
});
