import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatMenuHarness } from '@angular/material/menu/testing';
import { provideRouter, Router } from '@angular/router';
import type { CurrentUserDto } from '@core/api/eurder-api';
import { provideApiTesting } from '@core/api/testing';
import { provideTestTranslations } from '@core/i18n/testing';
import { SecurityService } from '@core/security/security.service';
import { BasketService } from '@features/basket/basket.service';
import { firstValueFrom } from 'rxjs';
import { UserMenu } from './user-menu';

describe('UserMenu', () => {
  let fixture: ComponentFixture<UserMenu>;
  let loader: HarnessLoader;
  let element: HTMLElement;
  let backend: HttpTestingController;

  const customer: CurrentUserDto = {
    id: 'c',
    role: 'CUSTOMER',
    email: 'jane@doe.be',
    firstName: 'Jane',
    lastName: 'Doe',
  };
  const admin: CurrentUserDto = {
    id: 'a',
    role: 'ADMIN',
    email: 'admin@eurder.be',
    firstName: null,
    lastName: null,
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UserMenu],
      providers: [provideTestTranslations(), provideRouter([]), provideApiTesting()],
    }).compileComponents();
    backend = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(UserMenu);
    element = fixture.nativeElement as HTMLElement;
    loader = TestbedHarnessEnvironment.loader(fixture);
  });

  afterEach(() => backend.verify());

  async function menuFor(user: CurrentUserDto | null): Promise<void> {
    const loaded = firstValueFrom(TestBed.inject(SecurityService).loadCurrentUser());
    const me = backend.expectOne('/api/auth/me');
    if (user) {
      me.flush(user);
    } else {
      me.flush({}, { status: 401, statusText: 'Unauthorized' });
    }
    await loaded;
    await fixture.whenStable();
  }

  async function itemTexts(menu: MatMenuHarness): Promise<string[]> {
    await menu.open();
    return Promise.all((await menu.getItems()).map((item) => item.getText()));
  }

  it('given an anonymous visitor, when rendered, then it offers to log in', async () => {
    await menuFor(null);

    const link = element.querySelector('a');

    expect(link?.textContent?.trim()).toBe('Log in');
    expect(link?.getAttribute('href')).toBe('/login');
  });

  it('given a customer, when opening the menu, then it shows their name, their orders and the logout', async () => {
    await menuFor(customer);
    const menu = await loader.getHarness(MatMenuHarness);

    const items = await itemTexts(menu);

    expect(await menu.getTriggerText()).toContain('Jane Doe');
    expect(items).toEqual([
      expect.stringContaining('My orders'),
      expect.stringContaining('Log out'),
    ]);
  });

  it('given an admin, when opening the menu, then it leads to the back office', async () => {
    await menuFor(admin);
    const menu = await loader.getHarness(MatMenuHarness);

    const items = await itemTexts(menu);

    expect(items).toEqual([
      expect.stringContaining('Back office'),
      expect.stringContaining('Log out'),
    ]);
  });

  it('given a logged-in user, when logging out, then the session ends and they go home', async () => {
    await menuFor(customer);
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    const menu = await loader.getHarness(MatMenuHarness);

    await menu.clickItem({ text: /Log out/ });
    backend.expectOne({ method: 'POST', url: '/api/auth/logout' }).flush(null);
    await fixture.whenStable();

    expect(TestBed.inject(SecurityService).isAuthenticated()).toBe(false);
    expect(navigate).toHaveBeenCalledWith('/');
  });

  it('given a filled basket, when logging out, then the basket is emptied', async () => {
    await menuFor(customer);
    vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    TestBed.inject(BasketService).add({
      id: 'i1',
      name: 'Chair',

      priceInCents: 4999,
    });
    const menu = await loader.getHarness(MatMenuHarness);

    await menu.clickItem({ text: /Log out/ });
    backend.expectOne('/api/auth/logout').flush(null);
    await fixture.whenStable();

    expect(TestBed.inject(BasketService).lines()).toEqual([]);
  });
});
