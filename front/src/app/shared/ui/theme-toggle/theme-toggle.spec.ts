import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { TestBed } from '@angular/core/testing';
import { MatMenuHarness } from '@angular/material/menu/testing';
import { Language, LanguageService } from '@core/i18n/language.service';
import { provideTestTranslations } from '@core/i18n/testing';
import { ThemeService } from '@core/theme/theme.service';
import { firstValueFrom } from 'rxjs';
import { ThemeToggle } from './theme-toggle';

describe('ThemeToggle', () => {
  async function toggleIn(language: Language): Promise<HarnessLoader> {
    localStorage.clear();
    localStorage.setItem('eurder.lang', language);
    await TestBed.configureTestingModule({
      imports: [ThemeToggle],
      providers: [provideTestTranslations()],
    }).compileComponents();
    await firstValueFrom(TestBed.inject(LanguageService).start());
    return TestbedHarnessEnvironment.loader(TestBed.createComponent(ThemeToggle));
  }

  async function itemTexts(menu: MatMenuHarness): Promise<string[]> {
    const items = await menu.getItems();
    return Promise.all(items.map((item) => item.getText()));
  }

  it('given the toggle, when opening its menu, then it offers the three themes and marks the current one', async () => {
    const loader = await toggleIn('en');
    const menu = await loader.getHarness(MatMenuHarness.with({ triggerText: /Theme/ }));

    await menu.open();

    expect(await itemTexts(menu)).toEqual([
      expect.stringContaining('Light'),
      expect.stringContaining('Dark'),
      expect.stringContaining('System'),
    ]);
    const system = (await menu.getItems())[2];
    expect(await (await system.host()).getAttribute('aria-checked')).toBe('true');
  });

  it('given a French page, when opening the menu, then the themes are named in French', async () => {
    const loader = await toggleIn('fr');
    const menu = await loader.getHarness(MatMenuHarness.with({ triggerText: /Thème/ }));

    await menu.open();

    expect(await itemTexts(menu)).toEqual([
      expect.stringContaining('Clair'),
      expect.stringContaining('Sombre'),
      expect.stringContaining('Système'),
    ]);
  });

  it('given the toggle, when choosing dark, then the theme service switches to dark', async () => {
    const loader = await toggleIn('en');
    const menu = await loader.getHarness(MatMenuHarness.with({ triggerText: /Theme/ }));

    await menu.clickItem({ text: /Dark/ });

    expect(TestBed.inject(ThemeService).preference()).toBe('dark');
  });
});
