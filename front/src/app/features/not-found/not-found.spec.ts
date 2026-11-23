import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { provideTestTranslations } from '@core/i18n/testing';
import { NotFound } from './not-found';

describe('NotFound', () => {
  it('given an unknown address, when opening it, then it says so and leads back to the shop', async () => {
    TestBed.configureTestingModule({
      providers: [provideTestTranslations(), provideRouter([{ path: '**', component: NotFound }])],
    });
    const harness = await RouterTestingHarness.create();

    await harness.navigateByUrl('/no-such-page');

    const page = harness.routeNativeElement!;
    expect(page.querySelector('h1')?.textContent?.trim()).toBe('Page not found');
    expect(page.textContent).toContain('This page does not exist.');
    expect(page.querySelector('a')?.getAttribute('href')).toBe('/catalog');
  });
});
