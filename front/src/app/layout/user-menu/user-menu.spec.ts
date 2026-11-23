import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { computed, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { MatMenuHarness } from '@angular/material/menu/testing';
import { provideRouter, Router } from '@angular/router';
import { provideTestTranslations } from '@core/i18n/testing';
import { CurrentUser } from '@core/security/current-user';
import { SecurityService } from '@core/security/security.service';
import { of } from 'rxjs';
import { UserMenu } from './user-menu';

describe('UserMenu', () => {
  let loader: HarnessLoader;
  let element: HTMLElement;
  let logout: ReturnType<typeof vi.fn>;

  async function menuFor(user: CurrentUser | null): Promise<void> {
    const current = signal(user);
    logout = vi.fn().mockReturnValue(of(undefined));
    TestBed.configureTestingModule({
      providers: [
        provideTestTranslations(),
        provideRouter([]),
        {
          provide: SecurityService,
          useValue: {
            currentUser: current,
            isAuthenticated: computed(() => current() !== null),
            isAdmin: computed(() => current()?.role === 'ADMIN'),
            isCustomer: computed(() => current()?.role === 'CUSTOMER'),
            logout,
          },
        },
      ],
    });
    const fixture = TestBed.createComponent(UserMenu);
    await fixture.whenStable();
    element = fixture.nativeElement as HTMLElement;
    loader = TestbedHarnessEnvironment.loader(fixture);
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
    await menuFor({ id: 'c', role: 'CUSTOMER', email: 'jane@doe.be', displayName: 'Jane Doe' });
    const menu = await loader.getHarness(MatMenuHarness);

    const items = await itemTexts(menu);

    expect(await menu.getTriggerText()).toContain('Jane Doe');
    expect(items).toEqual([
      expect.stringContaining('My orders'),
      expect.stringContaining('Log out'),
    ]);
  });

  it('given an admin, when opening the menu, then it leads to the back office', async () => {
    await menuFor({
      id: 'a',
      role: 'ADMIN',
      email: 'admin@eurder.be',
      displayName: 'admin@eurder.be',
    });
    const menu = await loader.getHarness(MatMenuHarness);

    const items = await itemTexts(menu);

    expect(items).toEqual([
      expect.stringContaining('Back office'),
      expect.stringContaining('Log out'),
    ]);
  });

  it('given a logged-in user, when logging out, then the session ends and they go home', async () => {
    await menuFor({ id: 'c', role: 'CUSTOMER', email: 'jane@doe.be', displayName: 'Jane Doe' });
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    const menu = await loader.getHarness(MatMenuHarness);

    await menu.clickItem({ text: /Log out/ });

    expect(logout).toHaveBeenCalledOnce();
    expect(navigate).toHaveBeenCalledWith('/');
  });
});
