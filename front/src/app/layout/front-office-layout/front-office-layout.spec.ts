import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideTestTranslations } from '@core/i18n/testing';
import { FrontOfficeLayout } from './front-office-layout';

describe('FrontOfficeLayout', () => {
  async function layout(): Promise<HTMLElement> {
    TestBed.configureTestingModule({ providers: [provideTestTranslations(), provideRouter([])] });
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

    expect(header?.querySelector('a[href="/"]')?.textContent?.trim()).toBe('Eurder');
    expect(linksOf(header)).toContainEqual(['Log in', '/login']);
    expect(header?.querySelector('app-language-switcher')).not.toBeNull();
    expect(header?.querySelector('app-theme-toggle')).not.toBeNull();
  });

  it('given the shop, when rendered, then the pages go in its main area and the footer shows the year', async () => {
    const page = await layout();

    const main = page.querySelector('main');

    expect(main?.querySelector('router-outlet')).not.toBeNull();
    expect(page.querySelector('footer')?.textContent).toContain(
      `© ${new Date().getFullYear()} Eurder`,
    );
  });
});
