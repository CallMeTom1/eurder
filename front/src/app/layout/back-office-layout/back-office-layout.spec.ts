import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideTestTranslations } from '@core/i18n/testing';
import { BackOfficeLayout } from './back-office-layout';

describe('BackOfficeLayout', () => {
  it('given the back office, when rendered, then its navigation leads to the admin pages and back to the shop', async () => {
    await TestBed.configureTestingModule({
      imports: [BackOfficeLayout],
      providers: [provideTestTranslations(), provideRouter([])],
    }).compileComponents();
    const fixture = TestBed.createComponent(BackOfficeLayout);
    await fixture.whenStable();

    const navigation = (fixture.nativeElement as HTMLElement).querySelector(
      'nav[aria-label="Back office"]',
    );

    const links = [...(navigation?.querySelectorAll('a') ?? [])].map((link) => [
      link.textContent?.trim(),
      link.getAttribute('href'),
    ]);
    expect(links).toEqual([
      ['Items', '/admin/items'],
      ['Customers', '/admin/customers'],
      ['Shipping today', '/admin/shipping'],
      ['Back to the shop', '/catalog'],
    ]);
  });

  it('given the back office, when rendered, then the pages go in its main area', async () => {
    await TestBed.configureTestingModule({
      imports: [BackOfficeLayout],
      providers: [provideTestTranslations(), provideRouter([])],
    }).compileComponents();
    const fixture = TestBed.createComponent(BackOfficeLayout);
    await fixture.whenStable();

    const main = (fixture.nativeElement as HTMLElement).querySelector('main');

    expect(main?.querySelector('router-outlet')).not.toBeNull();
  });
});
