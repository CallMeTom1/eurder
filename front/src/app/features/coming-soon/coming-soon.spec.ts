import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { provideTestTranslations } from '@core/i18n/testing';
import { ComingSoon } from './coming-soon';

describe('ComingSoon', () => {
  it('given a route titled basket, when opening it, then the heading is the translated title of the route', async () => {
    TestBed.configureTestingModule({
      providers: [
        provideTestTranslations(),
        provideRouter([{ path: 'basket', component: ComingSoon, title: 'nav.basket' }]),
      ],
    });
    const harness = await RouterTestingHarness.create();

    await harness.navigateByUrl('/basket');

    expect(harness.routeNativeElement?.querySelector('h1')?.textContent?.trim()).toBe('Basket');
    expect(harness.routeNativeElement?.textContent).toContain('This page is coming soon.');
  });
});
