import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { TestBed } from '@angular/core/testing';
import { MatMenuHarness } from '@angular/material/menu/testing';
import { LanguageService } from '@core/i18n/language.service';
import { provideTestTranslations } from '@core/i18n/testing';
import { firstValueFrom } from 'rxjs';
import { LanguageSwitcher } from './language-switcher';

describe('LanguageSwitcher', () => {
  let loader: HarnessLoader;

  beforeEach(async () => {
    localStorage.clear();
    localStorage.setItem('eurder.lang', 'en');
    await TestBed.configureTestingModule({
      imports: [LanguageSwitcher],
      providers: [provideTestTranslations()],
    }).compileComponents();
    await firstValueFrom(TestBed.inject(LanguageService).start());
    loader = TestbedHarnessEnvironment.loader(TestBed.createComponent(LanguageSwitcher));
  });

  it('given the switcher, when opening its menu, then each language is named in its own language and the current one is checked', async () => {
    const menu = await loader.getHarness(MatMenuHarness.with({ triggerText: /Language/ }));

    await menu.open();

    const items = await menu.getItems();
    expect(await Promise.all(items.map((item) => item.getText()))).toEqual([
      'Français',
      'English',
      'Nederlands',
    ]);
    expect(await (await items[1].host()).getAttribute('aria-checked')).toBe('true');
  });

  it('given the switcher, when choosing Nederlands, then the language becomes Dutch and its own label follows', async () => {
    const menu = await loader.getHarness(MatMenuHarness.with({ triggerText: /Language/ }));

    await menu.clickItem({ text: 'Nederlands' });

    expect(TestBed.inject(LanguageService).language()).toBe('nl');
    expect(await menu.getTriggerText()).toContain('Taal');
  });
});
