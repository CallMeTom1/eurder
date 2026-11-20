import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { TestBed } from '@angular/core/testing';
import { MatMenuHarness } from '@angular/material/menu/testing';
import { ThemeService } from '@core/theme/theme.service';
import { ThemeToggle } from './theme-toggle';

describe('ThemeToggle', () => {
  let loader: HarnessLoader;

  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({ imports: [ThemeToggle] }).compileComponents();
    loader = TestbedHarnessEnvironment.loader(TestBed.createComponent(ThemeToggle));
  });

  it('given the toggle, when opening its menu, then it offers the three themes and marks the current one', async () => {
    const menu = await loader.getHarness(MatMenuHarness.with({ triggerText: /theme/i }));

    await menu.open();

    const items = await menu.getItems();
    expect(await Promise.all(items.map((item) => item.getText()))).toEqual([
      expect.stringContaining('Light'),
      expect.stringContaining('Dark'),
      expect.stringContaining('System'),
    ]);
    const system = await (await items[2].host()).getAttribute('aria-checked');
    expect(system).toBe('true');
  });

  it('given the toggle, when choosing dark, then the theme service switches to dark', async () => {
    const menu = await loader.getHarness(MatMenuHarness.with({ triggerText: /theme/i }));

    await menu.clickItem({ text: /Dark/ });

    expect(TestBed.inject(ThemeService).preference()).toBe('dark');
  });
});
